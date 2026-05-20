/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root
 * directory of this source tree.
 */
package horizonos.os.sdk.sample.versioning

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import horizonos.os.Build
import horizonosx.os.HorizonOsSdkVersion

class MainActivity : Activity() {

  companion object {
    // The Horizon OS SDK version to compare against when calling isAtOrAbove().
    // Change this value here to compare against a different SDK version.
    private const val SDK_VERSION_TO_COMPARE_WITH_ISATORABOVE = 204
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(
        ScrollView(this).apply {
          addView(
              LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )

                fun createTextView(content: String) =
                    TextView(context).apply {
                      layoutParams =
                          ViewGroup.LayoutParams(
                              ViewGroup.LayoutParams.MATCH_PARENT,
                              ViewGroup.LayoutParams.WRAP_CONTENT,
                          )
                      textSize = 18f
                      setPadding(32, 32, 32, 32)
                      text = content
                    }

                addView(createTextView(createGetVersionOutput()))
                addView(createTextView(createIsAtOrAboveOutput()))
              }
          )
        }
    )
  }

  private fun createGetVersionOutput(): String {
    // Query the currently available Horizon OS SDK version via Build.HorizonOsSdk.getVersion()
    val sdkVersion: Int = Build.HorizonOsSdk.getVersion()
    return getString(R.string.sdk_version_message, sdkVersion)
  }

  private fun createIsAtOrAboveOutput(): String {
    // Use HorizonOsSdkVersion.isAtOrAbove() to compare the currently available Horizon OS SDK
    // version with SDK_VERSION_TO_COMPARE_WITH_ISATORABOVE
    val sdkVersion: Int = Build.HorizonOsSdk.getVersion()
    val isAtOrAbove: Boolean =
        HorizonOsSdkVersion.isAtOrAbove(SDK_VERSION_TO_COMPARE_WITH_ISATORABOVE)

    val resultString = getString(if (isAtOrAbove) R.string.yes else R.string.no)
    return getString(
        R.string.isatorabove_comparison_message,
        sdkVersion,
        SDK_VERSION_TO_COMPARE_WITH_ISATORABOVE,
        resultString,
    )
  }
}
