/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "horizonos.os.sdk.sample.versioning"
  compileSdk = 34

  defaultConfig {
    applicationId = "horizonos.os.sdk.sample.versioning"
    minSdk = 34
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes { release {} }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions { jvmTarget = "11" }
}

dependencies {
  // Take dependencies on Horizon OS JSDK and Support Library
  compileOnly(libs.horizonos.jsdk)
  implementation(libs.horizonosx.core)
}
