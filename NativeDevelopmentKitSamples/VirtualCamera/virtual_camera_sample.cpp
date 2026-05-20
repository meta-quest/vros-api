/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

/**
 * @file virtual_camera_sample.cpp
 * @brief Minimal virtual camera publisher example (non-engine, EGL/GLES NDK).
 *
 * This sample registers a virtual camera that renders a scrolling vertical
 * bar using OpenGL ES 2.0 via EGL, demonstrating the full publisher lifecycle:
 *
 *   1. Create a VirtualCameraManager.
 *   2. Define input stream configurations.
 *   3. Set up callbacks for stream open, capture request, and stream close.
 *   4. Register the virtual camera.
 *   5. Create an EGL surface from the ANativeWindow and render with GLES.
 *   6. Tear down on exit.
 *
 * Build:
 *   Link against libhzos, libEGL, libGLESv2, libnativewindow, and liblog.
 *
 * Permission:
 *   Declare horizonos.permission.CREATE_VIRTUAL_CAMERA in your manifest.
 */

#include <horizonos/virtualcamera/VirtualCamera.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <android/hardware_buffer.h>
#include <android/log.h>
#include <android/native_window.h>
#include <dlfcn.h>
#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>
#include <thread>
#include <unordered_map>

#define LOG_TAG "VirtualCameraSample"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ---------------------------------------------------------------------------
// GLES helpers
// ---------------------------------------------------------------------------

static const char* kVertexShaderSource = R"(
    attribute vec4 aPosition;
    void main() {
        gl_Position = aPosition;
    }
)";

static const char* kFragmentShaderSource = R"(
    precision mediump float;
    uniform float uBarCenter;  // normalized x position [0, 1]
    uniform float uBarWidth;   // normalized half-width
    uniform vec2  uResolution;
    void main() {
        float nx = gl_FragCoord.x / uResolution.x;
        float dist = abs(nx - uBarCenter);
        if (dist < uBarWidth) {
            gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);  // white bar
        } else {
            gl_FragColor = vec4(0.0, 0.5, 0.5, 1.0);  // dark teal background
        }
    }
)";

static GLuint compileShader(GLenum type, const char* source) {
  GLuint shader = glCreateShader(type);
  glShaderSource(shader, 1, &source, nullptr);
  glCompileShader(shader);

  GLint compiled = 0;
  glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
  if (!compiled) {
    GLchar log[512];
    glGetShaderInfoLog(shader, sizeof(log), nullptr, log);
    LOGE("Shader compile error: %s", log);
    glDeleteShader(shader);
    return 0;
  }
  return shader;
}

static GLuint createProgram() {
  GLuint vs = compileShader(GL_VERTEX_SHADER, kVertexShaderSource);
  GLuint fs = compileShader(GL_FRAGMENT_SHADER, kFragmentShaderSource);
  if (vs == 0 || fs == 0) {
    if (vs)
      glDeleteShader(vs);
    if (fs)
      glDeleteShader(fs);
    return 0;
  }

  GLuint program = glCreateProgram();
  glAttachShader(program, vs);
  glAttachShader(program, fs);
  glLinkProgram(program);

  GLint linked = 0;
  glGetProgramiv(program, GL_LINK_STATUS, &linked);
  if (!linked) {
    GLchar log[512];
    glGetProgramInfoLog(program, sizeof(log), nullptr, log);
    LOGE("Program link error: %s", log);
    glDeleteProgram(program);
    program = 0;
  }

  glDeleteShader(vs);
  glDeleteShader(fs);
  return program;
}

// Full-screen quad (two triangles, CCW).
static const GLfloat kQuadVertices[] = {
    -1.0f,
    -1.0f,
    1.0f,
    -1.0f,
    -1.0f,
    1.0f,
    1.0f,
    1.0f,
};

// ---------------------------------------------------------------------------
// EGL context management
// ---------------------------------------------------------------------------

struct EglState {
  EGLDisplay display = EGL_NO_DISPLAY;
  EGLContext context = EGL_NO_CONTEXT;
  EGLSurface surface = EGL_NO_SURFACE;
  EGLConfig config = nullptr;
};

static bool initEgl(EglState* egl, ANativeWindow* window) {
  egl->display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
  if (egl->display == EGL_NO_DISPLAY) {
    LOGE("eglGetDisplay failed");
    return false;
  }
  if (!eglInitialize(egl->display, nullptr, nullptr)) {
    LOGE("eglInitialize failed");
    return false;
  }

  const EGLint configAttribs[] = {
      EGL_RENDERABLE_TYPE,
      EGL_OPENGL_ES2_BIT,
      EGL_SURFACE_TYPE,
      EGL_WINDOW_BIT,
      EGL_RED_SIZE,
      8,
      EGL_GREEN_SIZE,
      8,
      EGL_BLUE_SIZE,
      8,
      EGL_ALPHA_SIZE,
      8,
      EGL_RECORDABLE_ANDROID,
      1,
      EGL_NONE};
  EGLint numConfigs = 0;
  if (!eglChooseConfig(egl->display, configAttribs, &egl->config, 1, &numConfigs) ||
      numConfigs == 0) {
    LOGE("eglChooseConfig failed");
    return false;
  }

  const EGLint contextAttribs[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
  egl->context = eglCreateContext(egl->display, egl->config, EGL_NO_CONTEXT, contextAttribs);
  if (egl->context == EGL_NO_CONTEXT) {
    LOGE("eglCreateContext failed");
    return false;
  }

  egl->surface = eglCreateWindowSurface(egl->display, egl->config, window, nullptr);
  if (egl->surface == EGL_NO_SURFACE) {
    LOGE("eglCreateWindowSurface failed");
    return false;
  }

  if (!eglMakeCurrent(egl->display, egl->surface, egl->surface, egl->context)) {
    LOGE("eglMakeCurrent failed");
    return false;
  }

  return true;
}

static void teardownEgl(EglState* egl) {
  if (egl->display == EGL_NO_DISPLAY) {
    return;
  }
  eglMakeCurrent(egl->display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
  if (egl->surface != EGL_NO_SURFACE) {
    eglDestroySurface(egl->display, egl->surface);
  }
  if (egl->context != EGL_NO_CONTEXT) {
    eglDestroyContext(egl->display, egl->context);
  }
  eglTerminate(egl->display);
  *egl = {};
}

// ---------------------------------------------------------------------------
// Stream state — one per active stream
// ---------------------------------------------------------------------------

struct StreamState {
  ANativeWindow* window;
  int32_t width;
  int32_t height;
  AHardwareBuffer_Format format;
  std::thread renderThread;
  std::atomic<bool> running{false};
};

// ---------------------------------------------------------------------------
// Application state
// ---------------------------------------------------------------------------

struct AppState {
  std::mutex streamsMutex;
  std::unordered_map<int32_t, std::unique_ptr<StreamState>> streams;
};

// ---------------------------------------------------------------------------
// Rendering — EGL/GLES scrolling bar
// ---------------------------------------------------------------------------

static void renderLoop(StreamState* stream) {
  LOGI("Render thread started for stream (w=%d h=%d)", stream->width, stream->height);

  // Each render thread owns its own EGL context.
  EglState egl;
  if (!initEgl(&egl, stream->window)) {
    LOGE("EGL init failed, render thread exiting");
    return;
  }

  GLuint program = createProgram();
  if (program == 0) {
    LOGE("Shader program creation failed");
    teardownEgl(&egl);
    return;
  }

  GLint aPosition = glGetAttribLocation(program, "aPosition");
  GLint uBarCenter = glGetUniformLocation(program, "uBarCenter");
  GLint uBarWidth = glGetUniformLocation(program, "uBarWidth");
  GLint uResolution = glGetUniformLocation(program, "uResolution");

  glViewport(0, 0, stream->width, stream->height);
  glUseProgram(program);
  glVertexAttribPointer(aPosition, 2, GL_FLOAT, GL_FALSE, 0, kQuadVertices);
  glEnableVertexAttribArray(aPosition);
  glUniform2f(uResolution, static_cast<float>(stream->width), static_cast<float>(stream->height));
  // Bar is 20 pixels wide, expressed as half-width in normalized coords.
  glUniform1f(uBarWidth, 10.0f / static_cast<float>(stream->width));

  float barPos = 0.0f;
  const float barSpeed = 4.0f / static_cast<float>(stream->width);

  while (stream->running.load(std::memory_order_relaxed)) {
    glClear(GL_COLOR_BUFFER_BIT);

    glUniform1f(uBarCenter, barPos);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    if (!eglSwapBuffers(egl.display, egl.surface)) {
      LOGE("eglSwapBuffers failed: 0x%x", eglGetError());
      break;
    }

    barPos += barSpeed;
    if (barPos > 1.0f) {
      barPos = 0.0f;
    }

    // Target ~30 FPS. This does not account for render time, so actual FPS
    // will be slightly below 30. For production use, consider timestamp-based
    // pacing.
    std::this_thread::sleep_for(std::chrono::milliseconds(33));
  }

  glDeleteProgram(program);
  teardownEgl(&egl);
  LOGI("Render thread exiting");
}

// ---------------------------------------------------------------------------
// Callbacks
// ---------------------------------------------------------------------------

static void onStreamConfigured(
    int32_t streamId,
    ANativeWindow* window,
    int32_t width,
    int32_t height,
    AHardwareBuffer_Format format,
    void* clientData) {
  LOGI("onStreamConfigured: streamId=%d %dx%d", streamId, width, height);
  auto* app = static_cast<AppState*>(clientData);

  // Set the buffer geometry to match the requested dimensions.
  ANativeWindow_setBuffersGeometry(window, width, height, format);

  std::lock_guard<std::mutex> lock(app->streamsMutex);
  auto stream = std::make_unique<StreamState>();
  stream->window = window;
  stream->width = width;
  stream->height = height;
  stream->format = format;
  stream->running.store(true, std::memory_order_relaxed);
  StreamState* streamPtr = stream.get();
  app->streams[streamId] = std::move(stream);
  streamPtr->renderThread = std::thread(renderLoop, streamPtr);
}

static void onStreamClosed(int32_t streamId, void* clientData) {
  LOGI("onStreamClosed: streamId=%d", streamId);
  auto* app = static_cast<AppState*>(clientData);

  std::unique_lock<std::mutex> lock(app->streamsMutex);
  auto it = app->streams.find(streamId);
  if (it == app->streams.end()) {
    return;
  }

  // Extract ownership so the pointer stays valid after we unlock
  // (and the map can rehash freely from other callbacks).
  std::unique_ptr<StreamState> stream = std::move(it->second);
  app->streams.erase(it);

  stream->running.store(false, std::memory_order_relaxed);
  ANativeWindow* window = stream->window;

  // Unlock before joining to avoid deadlock.
  lock.unlock();
  if (stream->renderThread.joinable()) {
    stream->renderThread.join();
  }

  // Release ownership of the window as required by the API contract.
  ANativeWindow_release(window);
}

static void onCaptureRequest(int32_t streamId, int32_t frameId, void* clientData) {
  // Optional: react to individual capture requests.
  // This sample relies on the continuous render loop instead.
  (void)streamId;
  (void)frameId;
  (void)clientData;
}

// ---------------------------------------------------------------------------
// Main entry point
// ---------------------------------------------------------------------------

/**
 * Run the virtual camera publisher until the user presses Enter.
 *
 * In a real app you would tie the lifetime to your Activity or Service
 * rather than blocking on stdin.
 */
int main() {
  // Start the binder thread pool so we can receive callbacks from the
  // VirtualCameraService. APKs get this automatically from the Android
  // runtime; standalone binaries must start it explicitly.
  void* binderLib = dlopen("libbinder_ndk.so", RTLD_NOW);
  if (binderLib) {
    auto startPool =
        reinterpret_cast<void (*)()>(dlsym(binderLib, "ABinderProcess_startThreadPool"));
    if (startPool) {
      startPool();
      LOGI("Binder thread pool started");
    } else {
      LOGE("Failed to find ABinderProcess_startThreadPool: %s", dlerror());
    }
  } else {
    LOGE("Failed to load libbinder_ndk.so: %s", dlerror());
  }

  AppState app;

  // --- 1. Create manager ---------------------------------------------------
  HzVirtualCameraManagerHandle manager = nullptr;
  HzVirtualCameraStatus status = HzVirtualCameraManager_create(&manager);
  if (status != HZ_VIRTUAL_CAMERA_STATUS_OK) {
    LOGE("Failed to create VirtualCameraManager: %d", status);
    return 1;
  }
  LOGI("VirtualCameraManager created");

  // --- 2. Define input stream configurations --------------------------------
  HzVirtualCameraInputConfiguration cfg1080p;
  HzVirtualCameraInputConfiguration_init(&cfg1080p, HZ_VIRTUAL_CAMERA_INPUT_CONFIGURATION_LATEST);
  cfg1080p.width = 1920;
  cfg1080p.height = 1080;
  cfg1080p.format = AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM;
  cfg1080p.maxFps = 30;

  HzVirtualCameraInputConfiguration cfg720p;
  HzVirtualCameraInputConfiguration_init(&cfg720p, HZ_VIRTUAL_CAMERA_INPUT_CONFIGURATION_LATEST);
  cfg720p.width = 1280;
  cfg720p.height = 720;
  cfg720p.format = AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM;
  cfg720p.maxFps = 30;

  const HzVirtualCameraInputConfiguration* inputConfigs[] = {&cfg1080p, &cfg720p};

  // --- 3. Build camera configuration ----------------------------------------
  HzVirtualCameraConfiguration config;
  HzVirtualCameraConfiguration_init(&config, HZ_VIRTUAL_CAMERA_CONFIGURATION_LATEST);

  config.inputStreamConfigs = inputConfigs;
  config.inputStreamConfigsCount = 2;
  config.onStreamConfigured = onStreamConfigured;
  config.onStreamClosed = onStreamClosed;
  config.onProcessCaptureRequest = onCaptureRequest;
  config.cameraSource = HZ_VIRTUAL_CAMERA_SOURCE_APP_DEFINED;
  config.lensFacing = HZ_VIRTUAL_CAMERA_LENS_FACING_EXTERNAL;
  config.cameraName = "Sample Virtual Camera";
  config.clientData = &app;

  // --- 4. Register virtual camera -------------------------------------------
  HzVirtualCameraHandle camera = nullptr;
  status = HzVirtualCamera_create(manager, &config, &camera);
  if (status != HZ_VIRTUAL_CAMERA_STATUS_OK) {
    LOGE("Failed to register virtual camera: %d", status);
    HzVirtualCameraManager_destroy(manager);
    return 1;
  }

  const char* cameraId = nullptr;
  HzVirtualCamera_getCameraId(camera, &cameraId);
  if (cameraId) {
    LOGI("Virtual camera registered with ID: %s", cameraId);
  } else {
    LOGI("Virtual camera registered (ID unavailable)");
  }
  LOGI("Waiting for a consumer to open the camera...");
  LOGI("Press Enter to exit.");

  // --- 5. Run until user exits ----------------------------------------------
  getchar();

  // --- 6. Tear down ---------------------------------------------------------
  LOGI("Shutting down...");

  // Stop all active render threads before destroying the camera.
  {
    std::lock_guard<std::mutex> lock(app.streamsMutex);
    for (auto& [id, stream] : app.streams) {
      stream->running.store(false, std::memory_order_relaxed);
    }
  }
  // Destroying the camera triggers onStreamClosed for any active streams.
  HzVirtualCamera_destroy(camera);
  HzVirtualCameraManager_destroy(manager);

  LOGI("Done.");
  return 0;
}
