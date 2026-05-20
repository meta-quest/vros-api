/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package horizonos.os.sdk.sample.stereovideo

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import horizonos.view.SurfaceControlExt
import horizonos.view.SurfaceViewExt

/**
 * Demonstrates the smallest VideoView-based stereo composition flow.
 *
 * VideoView extends SurfaceView, so this Activity can apply stereo composition directly to the
 * VideoView while the standard Android widget handles media playback and aspect-ratio-preserving
 * layout.
 */
class StereoVideoActivity : AppCompatActivity() {

  private lateinit var mVideoView: VideoView
  private lateinit var mCompositionInfo: TextView
  // Tracks the stereo composition mode currently applied to the VideoView.
  // Mono is the default mode until the user selects side-by-side or top-bottom content.
  private var mStereoType: Int = SurfaceControlExt.STEREO_COMPOSITION_MONO
  private var mCurrentUri: Uri? = null
  private var mSavedPosition: Int = 0
  private var mVideoWidth: Int = 0
  private var mVideoHeight: Int = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_stereo_video_view)

    mVideoView = findViewById(R.id.video_view)
    mCompositionInfo = findViewById(R.id.composition_info)
    val mediaController = MediaController(this)
    mediaController.setAnchorView(mVideoView)
    mVideoView.setMediaController(mediaController)

    mVideoView.setOnPreparedListener { mp ->
      mVideoWidth = mp.videoWidth
      mVideoHeight = mp.videoHeight
      updateCompositionInfo()
    }

    val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.stereo_type_radio_group)
    toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (!isChecked) return@addOnButtonCheckedListener
      StereoVideoResources.forButtonId(checkedId)?.let {
        selectVideoSource(it, startPlayback = true)
      }
    }

    // Match the default selected toggle without loading the video twice on startup.
    selectInitialVideoSource(StereoVideoResources.mono)
  }

  private fun selectInitialVideoSource(source: StereoVideoResources.VideoSource) {
    mStereoType = source.stereoType
    // This is the key Horizon OS JSDK API demonstrated by the sample: it tells
    // Horizon OS how to map regions of the VideoView buffer to each eye.
    SurfaceViewExt.setStereoComposition(mVideoView, mStereoType)
    mCurrentUri = StereoVideoResources.uriFor(this, source)
  }

  private fun selectVideoSource(
      source: StereoVideoResources.VideoSource,
      startPlayback: Boolean,
  ) {
    mStereoType = source.stereoType
    // Keep the composition mode in sync with the selected video layout.
    SurfaceViewExt.setStereoComposition(mVideoView, mStereoType)
    val videoUri = StereoVideoResources.uriFor(this, source)
    mCurrentUri = videoUri
    if (startPlayback) {
      mSavedPosition = 0
    }
    mVideoView.setVideoURI(videoUri)
    if (startPlayback) {
      mVideoView.start()
    }
  }

  private fun updateCompositionInfo() {
    val mode = StereoVideoResources.stereoModeName(mStereoType)
    val file =
        mCurrentUri?.let { StereoVideoResources.fileNameFromUri(this, it) }
            ?: getString(R.string.composition_info_none)
    mCompositionInfo.text =
        getString(
            R.string.composition_info_format,
            mode,
            file,
            mVideoWidth,
            mVideoHeight,
            mVideoView.width,
            mVideoView.height,
        )
  }

  override fun onStop() {
    mSavedPosition = mVideoView.currentPosition
    super.onStop()
  }

  override fun onStart() {
    super.onStart()
    val uri = mCurrentUri ?: return
    // Reapply the stereo composition mode after lifecycle transitions because
    // the underlying surface can be recreated.
    SurfaceViewExt.setStereoComposition(mVideoView, mStereoType)
    mVideoView.setVideoURI(uri)
    mVideoView.seekTo(mSavedPosition)
    mVideoView.start()
  }
}
