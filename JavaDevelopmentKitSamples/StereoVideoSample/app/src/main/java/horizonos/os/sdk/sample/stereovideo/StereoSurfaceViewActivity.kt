/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package horizonos.os.sdk.sample.stereovideo

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.MediaController
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import horizonos.view.SurfaceControlExt
import horizonos.view.SurfaceViewExt

/**
 * Demonstrates stereo composition with a SurfaceView managed by MediaPlayer.
 *
 * This Activity shows the lower-level path where the app owns sizing decisions. It computes the
 * per-eye display dimensions from the encoded packed video dimensions before resizing the
 * SurfaceView.
 */
class StereoSurfaceViewActivity : AppCompatActivity(), MediaController.MediaPlayerControl {

  private lateinit var mSurfaceView: SurfaceView
  private lateinit var mCompositionInfo: TextView
  private lateinit var mMediaController: MediaController
  private var mMediaPlayer: MediaPlayer? = null
  // Tracks the stereo composition mode currently applied to the SurfaceView.
  // Mono is the default mode until the user selects side-by-side or top-bottom content.
  private var mStereoType: Int = SurfaceControlExt.STEREO_COMPOSITION_MONO
  private var mCurrentUri: Uri? = null
  private var mSavedPosition: Int = 0
  private var mVideoWidth: Int = 0
  private var mVideoHeight: Int = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_stereo_surface_view)

    mSurfaceView = findViewById(R.id.surface_view)
    mCompositionInfo = findViewById(R.id.composition_info)
    mMediaController = MediaController(this)
    mMediaController.setAnchorView(mSurfaceView)
    mMediaController.setMediaPlayer(this)
    mSurfaceView.holder.addCallback(
        object : SurfaceHolder.Callback {
          override fun surfaceCreated(holder: SurfaceHolder) {
            // Resume the previous video if available, otherwise play mono by default
            val uri =
                mCurrentUri
                    ?: StereoVideoResources.uriFor(
                        this@StereoSurfaceViewActivity,
                        StereoVideoResources.mono,
                    )
            // Reapply the stereo composition mode because the underlying
            // surface can be recreated during lifecycle transitions.
            SurfaceViewExt.setStereoComposition(mSurfaceView, mStereoType)
            playVideo(uri)
          }

          override fun surfaceChanged(
              holder: SurfaceHolder,
              format: Int,
              width: Int,
              height: Int,
          ) {}

          override fun surfaceDestroyed(holder: SurfaceHolder) {
            mSavedPosition = mMediaPlayer?.currentPosition ?: 0
            mMediaPlayer?.release()
            mMediaPlayer = null
          }
        }
    )

    val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.stereo_type_radio_group)
    toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (!isChecked) return@addOnButtonCheckedListener
      StereoVideoResources.forButtonId(checkedId)?.let(::selectVideoSource)
    }
  }

  private fun selectVideoSource(source: StereoVideoResources.VideoSource) {
    mStereoType = source.stereoType
    // This is the key Horizon OS JSDK API demonstrated by the sample: it tells
    // Horizon OS how to map regions of the SurfaceView buffer to each eye.
    SurfaceViewExt.setStereoComposition(mSurfaceView, mStereoType)
    mSavedPosition = 0
    playVideo(StereoVideoResources.uriFor(this, source))
  }

  private fun playVideo(uri: Uri) {
    mMediaPlayer?.release()
    mCurrentUri = uri
    mMediaPlayer =
        MediaPlayer().apply {
          setDataSource(this@StereoSurfaceViewActivity, uri)
          setSurface(mSurfaceView.holder.surface)
          setOnPreparedListener { mp -> onVideoPrepared(mp) }
          prepareAsync()
        }
  }

  private fun onVideoPrepared(mp: MediaPlayer) {
    val videoWidth = mp.videoWidth
    val videoHeight = mp.videoHeight
    mVideoWidth = videoWidth
    mVideoHeight = videoHeight
    if (videoWidth <= 0 || videoHeight <= 0 || mSurfaceView.width <= 0) {
      return
    }

    // The video file contains the packed stereo content. Adjust dimensions to
    // represent a single eye's content for the correct display aspect ratio.
    val displayWidth =
        when (mStereoType) {
          SurfaceControlExt.STEREO_COMPOSITION_SIDE_BY_SIDE -> videoWidth / 2
          else -> videoWidth
        }
    val displayHeight =
        when (mStereoType) {
          SurfaceControlExt.STEREO_COMPOSITION_TOP_BOTTOM -> videoHeight / 2
          else -> videoHeight
        }
    val surfaceViewHeight = (mSurfaceView.width * displayHeight.toFloat() / displayWidth).toInt()

    val containerParams = mSurfaceView.layoutParams
    containerParams.height = surfaceViewHeight
    mSurfaceView.layoutParams = containerParams

    mSurfaceView.post { updateCompositionInfo() }
    mp.start()
    if (mSavedPosition > 0) {
      mp.seekTo(mSavedPosition)
      mSavedPosition = 0
    }
    mMediaController.show()
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
            mSurfaceView.width,
            mSurfaceView.height,
        )
  }

  override fun onTouchEvent(event: android.view.MotionEvent?): Boolean {
    mMediaController.show()
    return super.onTouchEvent(event)
  }

  // MediaPlayerControl implementation
  override fun start() {
    mMediaPlayer?.start()
  }

  override fun pause() {
    mMediaPlayer?.pause()
  }

  override fun getDuration(): Int = mMediaPlayer?.duration ?: 0

  override fun getCurrentPosition(): Int = mMediaPlayer?.currentPosition ?: 0

  override fun seekTo(pos: Int) {
    mMediaPlayer?.seekTo(pos)
  }

  override fun isPlaying(): Boolean = mMediaPlayer?.isPlaying ?: false

  override fun getBufferPercentage(): Int = 0

  override fun canPause(): Boolean = true

  override fun canSeekBackward(): Boolean = true

  override fun canSeekForward(): Boolean = true

  override fun getAudioSessionId(): Int = mMediaPlayer?.audioSessionId ?: 0

  override fun onDestroy() {
    super.onDestroy()
    mMediaPlayer?.release()
    mMediaPlayer = null
  }
}
