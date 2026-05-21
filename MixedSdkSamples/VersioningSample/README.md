<!--
  Copyright (c) Meta Platforms, Inc. and affiliates.

  This source code is licensed under the MIT license found in the
  LICENSE file in the root directory of this source tree.
-->

# Horizon OS SDK versioning using Horizon OS JSDK and Support Library

This sample demonstrates a simple Android Studio project using APIs from the Horizon OS SDK to query the Horizon OS SDK version available on a Quest device. Specifically, APIs from both the [Horizon OS Java Software Development Kit (JSDK)](https://developers.meta.com/horizon/documentation/android-apps/horizon-os-jsdk) and Support Library are shown.

This sample uses Kotlin, but the same APIs are easily accessed via Java.

See the [Horizon OS SDK versioning documentation](https://developers.meta.com/horizon/documentation/android-apps/horizon-os-sdk-versioning/) for more information.

## Sample project setup

This project can be imported directly into Android Studio. After launching Android Studio, select **File -> New -> Import Project**, select this project's root directory (`VersioningSample`), and click **Open**.

## Running the sample

After building the sample and installing it onto a Quest device, launch it. It will look like the following screenshot:

![Versioning Sample Screenshot](documentation/versioning-sample-screenshot.jpg)

### Querying the Horizon OS SDK version

When you run the sample, you will see a `TextView` displaying a string containing the version of Horizon OS SDK available on the device, such as `204`. This value is fetched from [`Build.HorizonOsSdk.getVersion()`](https://developers.meta.com/horizon/reference/horizon-os-jsdk/latest/classhorizonos_os_build), which is available via `import horizonos.os.Build`.

In your app, you can use the version returned by `horizonos.os.Build.HorizonOsSdk.getVersion()` to make a single build of your app compatible with multiple versions of the Horizon OS SDK simultaneously.

### Comparing the Horizon OS SDK version to a specific version

You will also see a `TextView` comparing the version of the Horizon OS SDK available on the device to the version specified in `SDK_VERSION_TO_COMPARE_WITH_ISATORABOVE` within `app/src/main/java/horizonos/os/sdk/sample/versioning/MainActivity.kt`. The comparison is executed by passing the value of `SDK_VERSION_TO_COMPARE_WITH_ISATORABOVE` to `HorizonOsSdkVersion.isAtOrAbove()`, which is available via `import horizonosx.os.HorizonOsSdkVersion`. See the [Horizon OS SDK versioning documentation](https://developers.meta.com/horizon/documentation/android-apps/horizon-os-sdk-versioning/#handling-versioning-with-horizon-os-support-library) for more information.

Change the value of `SDK_VERSION_TO_COMPARE_WITH_ISATORABOVE` to compare against a different version.

In your app, you can use `HorizonOsSdkVersion.isAtOrAbove()` to easily adjust your app's behavior based on the version of the Horizon OS SDK available on the device.

---

_Java is a registered trademark of Oracle and/or its affiliates._
