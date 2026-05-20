/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package horizonos.os.sdk.sample.stereovideo

import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import horizonos.view.SurfaceControlExt

/** Defines the sample video resources and their matching stereo composition modes. */
internal object StereoVideoResources {

  data class VideoSource(
      @RawRes val rawResId: Int,
      val stereoType: Int,
  )

  val mono =
      VideoSource(
          rawResId = R.raw.synthetic_mono,
          stereoType = SurfaceControlExt.STEREO_COMPOSITION_MONO,
      )

  val sideBySide =
      VideoSource(
          rawResId = R.raw.synthetic_side_by_side,
          stereoType = SurfaceControlExt.STEREO_COMPOSITION_SIDE_BY_SIDE,
      )

  val topBottom =
      VideoSource(
          rawResId = R.raw.synthetic_top_bottom,
          stereoType = SurfaceControlExt.STEREO_COMPOSITION_TOP_BOTTOM,
      )

  fun forButtonId(checkedId: Int): VideoSource? =
      when (checkedId) {
        R.id.radio_none -> mono
        R.id.radio_side_by_side -> sideBySide
        R.id.radio_top_bottom -> topBottom
        else -> null
      }

  fun uriFor(context: Context, source: VideoSource): Uri =
      Uri.parse("android.resource://${context.packageName}/${source.rawResId}")

  fun stereoModeName(type: Int): String =
      when (type) {
        SurfaceControlExt.STEREO_COMPOSITION_MONO -> "MONO"
        SurfaceControlExt.STEREO_COMPOSITION_SIDE_BY_SIDE -> "SIDE_BY_SIDE"
        SurfaceControlExt.STEREO_COMPOSITION_TOP_BOTTOM -> "TOP_BOTTOM"
        else -> "UNKNOWN"
      }

  fun fileNameFromUri(context: Context, uri: Uri): String {
    try {
      val resId = uri.lastPathSegment?.toIntOrNull()
      if (resId != null) {
        return context.resources.getResourceEntryName(resId)
      }
    } catch (_: Exception) {}
    return uri.lastPathSegment ?: uri.toString()
  }
}
