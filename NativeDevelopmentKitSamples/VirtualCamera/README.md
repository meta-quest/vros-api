# Virtual Camera Publisher sample

This sample demonstrates the full publisher lifecycle of the Virtual Camera
Publisher API using plain C++ with EGL/GLES rendering. No engine dependency is
required.

## What it does

The sample registers a virtual camera named **"Sample Virtual Camera"** with
Horizon OS. When a consumer app (such as the system Camera app) opens the
camera, the sample renders a white vertical bar scrolling left-to-right across
a dark teal background at approximately 30 FPS using OpenGL ES 2.0.

No rendering occurs until a consumer opens the camera. When the consumer
closes the camera, rendering stops automatically.

The sample supports two output resolutions:

| Resolution | Format | Max FPS |
|---|---|---|
| 1920 x 1080 | RGBA_8888 | 30 |
| 1280 x 720 | RGBA_8888 | 30 |

## Prerequisites

- A physical Meta Quest device running Horizon OS v204 or later
- [Android Studio](https://developer.android.com/studio) with the Android NDK
  and CMake 3.18.1 or later installed (through SDK Manager, then SDK Tools)

## Build with Android Studio

The Horizon OS NSDK is distributed as an AAR containing header files and a stub
shared library (`libhzos.meta.so`). The stub provides symbol definitions for
the linker at build time only. The real implementation is supplied by the OS
on the device. Don’t package the stub into your APK.

### Step 1: Create a Native C++ project

In Android Studio, select **File**, then **New**, then **New Project**, then **Phone and Tablet**, then **Native C++**. Set the
minimum SDK to **API 34** (`android-34`). You can leave the rest of the options at their default
values, or configure them to fit your needs.

> **Note:** Android Studio is used here as a build environment only. The sample
> is a standalone native executable (`add_executable` in CMake), not an APK with
> an Activity. You can push the compiled binary to the device either through `adb`
> or using Android Studio rather than installing it as an app.

### Step 2: Add the Maven Central repository

In your project’s `settings.gradle.kts`, make sure `mavenCentral()` is listed
under `dependencyResolutionManagement.repositories`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()   // Required for Horizon OS NSDK
    }
}
```

### Step 3: Add the Horizon OS NSDK Gradle dependency

Add the Horizon OS NSDK to your version catalog (`gradle/libs.versions.toml`):

```toml
[versions]
horizon-os-nsdk = "204"

[libraries]
horizon-os-nsdk = { group = "com.meta.horizonos", name = "horizon-os-nsdk", version.ref = "horizon-os-nsdk" }
```

In `app/build.gradle.kts`, add the dependency and enable Prefab:

```kotlin
android {
    buildFeatures {
        prefab = true
    }

    defaultConfig {
        ndk {
            abiFilters.clear()
            abiFilters += listOf("arm64-v8a")
        }
    }

    packaging {
        jniLibs {
            excludes += listOf("**/libhzos.meta.so")
        }
    }
}

dependencies {
    implementation(libs.horizon.os.nsdk)
}
```

The `packaging` block makes sure the stub `libhzos.meta.so` is **not** bundled
into the APK. The real library is provided by the OS on the device.

### Step 4: Add the source and CMake files

Copy [`virtual_camera_sample.cpp`](virtual_camera_sample.cpp) and
[`CMakeLists.txt`](CMakeLists.txt) into your project’s `app/src/main/cpp/`
directory. Update the `project()` name in `CMakeLists.txt` to match your
project. With Prefab enabled, Gradle exposes the Horizon OS NSDK to CMake
automatically. The `find_package(horizon-os-nsdk REQUIRED CONFIG)` call in the CMakeLists
resolves without extra configuration. The sample links against
`horizon-os-nsdk::hzos`, which provides both the stub library and header files
(headers are exported transitively).

### Step 5: Update the manifest

Add the `horizonos` XML namespace, declare the Horizon OS SDK version, and add the
Virtual Camera permission to your `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:horizonos="http://schemas.horizonos/sdk">

    <horizonos:uses-horizonos-sdk
        horizonos:minSdkVersion="204"
        horizonos:targetSdkVersion="204" />

    <uses-permission android:name="horizonos.permission.CREATE_VIRTUAL_CAMERA" />

    <application ...>
        ...
    </application>
</manifest>
```

The [`<horizonos:uses-horizonos-sdk>`](https://developers.meta.com/horizon/documentation/native/native-manifest-config) element declares which Horizon OS versions your
app supports. `minSdkVersion` is the lowest version required. `targetSdkVersion`
is the version you designed and tested against. See the [Horizon OS SDK versioning documentation](https://developers.meta.com/horizon/essentials/horizon-os-sdk-versioning) for more information.

The `CREATE_VIRTUAL_CAMERA` permission is a `normal`-level permission. No
runtime prompt is needed.

### Step 6: Build and deploy

Connect your Quest device, select it as the deployment target, and click
**Run** (or **Build**, then **Make Project**). The APK excludes the stub library and
deploys directly to your connected Quest device.

Since the sample is a native executable (not an APK with an Activity), you
can also push the binary directly through adb:

```bash
adb push app\build\intermediates\cxx\Debug\<hash>\obj\arm64-v8a\virtual_camera_sample /data/local/tmp/
adb shell chmod +x /data/local/tmp/virtual_camera_sample
adb shell /data/local/tmp/virtual_camera_sample
```

Replace `<hash>` with the build hash found inside `app\build\intermediates\cxx\Debug\`.

> **Note:** The `chmod +x` step is necessary because pushed files don’t have the execute
> permission set by default.

### Alternative: Standalone cross-compile

If you’re working outside Android Studio, you can cross-compile directly using CMake
and the Android NDK toolchain file.

First, extract the Horizon OS NSDK headers and stub library from the AAR (optionally, change its
extension to .ZIP so tools more easily recognize it):

```
horizon-os-nsdk-<version>.aar (extract as ZIP)
└── prefab/
    └── modules/
        ├── hzos_headers/include/horizonos/...
        └── hzos/libs/android.arm64-v8a/libhzos.meta.so
```

Then configure and build:

```bash
cmake -B build \
    -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-34 \
    -DCMAKE_FIND_ROOT_PATH=<path-to-extracted-prefab>

cmake --build build
```

The binary is produced at `build/virtual_camera_sample`.

## How the sample works

The source is in [`virtual_camera_sample.cpp`](virtual_camera_sample.cpp).
This section walks through how the sample uses each API call. For a conceptual
overview of the API, see the [Virtual Camera Publisher](https://developers.meta.com/horizon/reference/horizon-os-nsdk/latest/virtual_camera_8h) reference documentation.

### Binder thread pool

The Virtual Camera API delivers callbacks ([`onStreamConfigured`](#callbacks-onstreamconfigured-onstreamclosed),
[`onStreamClosed`](#callbacks-onstreamconfigured-onstreamclosed), and `onProcessCaptureRequest`) through Android’s binder IPC
mechanism. Your process must have a binder thread pool running to receive
them. Without a running pool, the camera service times out when a consumer opens the
camera and the stream never starts.

If your code runs inside an APK (Activity, Service, or NativeActivity), the
Android runtime starts the binder thread pool automatically and no extra
setup is needed. If you run the sample as a **standalone native binary**
pushed through `adb` (as described in the deploy instructions), you must start
the thread pool yourself before creating the `HzVirtualCameraManager`. The
sample does this at the top of `main()` by loading `libbinder_ndk.so` at
runtime and calling [`ABinderProcess_startThreadPool`](https://developer.android.com/ndk/reference/group/ndk-binder#abinderprocess_startthreadpool).

### Permission

The app must declare `horizonos.permission.CREATE_VIRTUAL_CAMERA` in its
`AndroidManifest.xml`. This is a `normal`-level permission. No runtime prompt
is needed, but the call to `HzVirtualCamera_create` returns
`HZ_VIRTUAL_CAMERA_STATUS_PERMISSION_DENIED` if it’s missing.

### Manager and configuration (main)

The `main()` function creates the manager with `HzVirtualCameraManager_create`, then
defines two input stream configurations (1080p and 720p) using
`HzVirtualCameraInputConfiguration_init`. You must initialize each struct
before populating it. Skipping this causes undefined behavior from garbage values
in unset fields.

The camera configuration sets:

- `inputStreamConfigs` and `inputStreamConfigsCount`: the two resolutions
- `onStreamConfigured` and `onStreamClosed`: required callbacks (see below)
- `onProcessCaptureRequest`: optional, and a no-op in this sample since it uses
  a continuous render loop
- `cameraSource`: `HZ_VIRTUAL_CAMERA_SOURCE_APP_DEFINED` (the only source
  available to third-party apps)
- `lensFacing`: `HZ_VIRTUAL_CAMERA_LENS_FACING_EXTERNAL`
- `cameraName`: "Sample Virtual Camera" (exposed to consumers through a vendor tag)
- `clientData`: a pointer to the app’s state, passed to every callback

The `HzVirtualCamera_create` function registers the camera with the system. This call can
block for up to 5 seconds if the virtual camera service is still starting.
After registration, `HzVirtualCamera_getCameraId` retrieves the ID that
consumers use to open the camera.

### Callbacks (onStreamConfigured, onStreamClosed)

The `onStreamConfigured` callback fires when a consumer opens the camera. The sample:

1. Calls [`ANativeWindow_setBuffersGeometry`](https://developer.android.com/ndk/reference/group/a-native-window#anativewindow_setbuffersgeometry) to match the requested dimensions.
2. Stores the window, dimensions, and format in a `StreamState` struct.
3. Spawns a dedicated render thread (`renderLoop`).

The [`ANativeWindow*`](https://developer.android.com/ndk/reference/group/a-native-window) ownership transfers to the app at this point. Buffers
submitted to it must exactly match the reported width, height, and format. A
mismatch produces silently corrupted or black output, not a crash.

The `onStreamClosed` callback fires when the consumer closes the camera. The sample
signals the render thread to stop, joins it, then calls
[`ANativeWindow_release`](https://developer.android.com/ndk/reference/group/a-native-window#anativewindow_release) to return ownership of the window.

Both callbacks run on a binder thread. The sample uses `std::mutex`
to protect the shared `streams` map. It unlocks before joining the render
thread to avoid deadlock.

### Rendering (renderLoop)

Each render thread owns its own EGL context created from the [`ANativeWindow`](https://developer.android.com/ndk/reference/group/a-native-window).
The render loop draws a full-screen quad with a fragment shader that produces a
scrolling white bar on a teal background. Each frame is posted through
[`eglSwapBuffers`](https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglSwapBuffers.xhtml) at approximately 30 FPS (`sleep_for(33ms)`).

### Tear down

The sample signals all active render threads to stop, then calls
`HzVirtualCamera_destroy` (which triggers `onStreamClosed` for any active
streams) followed by `HzVirtualCameraManager_destroy`.

## Deploy and run

Push the binary to a connected Quest device and launch it:

```bash
adb root
adb push build/virtual_camera_sample /data/local/tmp/
adb shell chmod +x /data/local/tmp/virtual_camera_sample
```

The sample logs to Android’s logcat, not to stdout. Start logcat in the
background before launching the sample so you can see logs inline:

```bash
adb shell
logcat -s VirtualCameraSample:* &
/data/local/tmp/virtual_camera_sample
```

You should see log output like:

```
Binder thread pool started
VirtualCameraManager created
Virtual camera registered with ID: 43
Waiting for a consumer to open the camera...
Press Enter to exit.
```

The sample blocks on `getchar()` and stays alive until you press Enter.

## Testing

**Note: Quest Link isn’t supported.**
Testing over Meta Quest Link isn’t supported for this feature. You need to
deploy and run the binary directly on a physical device through adb.

Once the sample is running and you see "Waiting for a consumer to open the
camera..." in the logs:

1. Press the **Meta button** to open the Universal Menu.
2. Open the **Camera app**.
3. Tap **Camera Settings**, then **Camera View**.
4. Select **Sample Virtual Camera** from the list.

You should see a white vertical bar scrolling left-to-right across a teal
background in the camera preview.

### Verify registration

Confirm the camera registered successfully by checking logcat:

```bash
adb logcat -s VirtualCameraSample
```

Or inspect the camera service directly:

```bash
adb shell
dumpsys media.camera | grep -A10 "virtual/<ID>"
```

Replace `<ID>` with the camera ID printed in the log output. The output shows
the registered stream configurations, lens facing, and vendor tags.

## Cleanup

Remove the sample binary:

```bash
adb shell rm -f /data/local/tmp/virtual_camera_sample
```
