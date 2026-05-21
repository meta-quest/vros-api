<!--
  Copyright (c) Meta Platforms, Inc. and affiliates.

  This source code is licensed under the MIT license found in the
  LICENSE file in the root directory of this source tree.
-->

# vros-api — Horizon OS SDK Samples

This repository is a collection of code samples that demonstrate the capabilities of the **Horizon OS SDK** for Meta Quest devices.

The Horizon OS SDK consists of three complementary packages:

- **[Horizon OS JSDK](https://developers.meta.com/horizon/documentation/android-apps/horizon-os-jsdk/)** — Java APIs for Horizon OS features, akin to the Android SDK. Distributed as the `horizon-os-jsdk` Android library (AAR).
- **[Horizon OS NSDK](https://developers.meta.com/horizon/documentation/native/horizon-os-nsdk/horizon-os-nsdk-overview/)** — Native C APIs for Horizon OS features, akin to the Android NDK. Distributed as the `horizon-os-nsdk` Android library (AAR) with Prefab-packaged headers and shared libraries.
- **Horizon OS Support Library** — A "static" library that provides a more convenient and compatible API for common Horizon OS features, akin to AndroidX/Jetpack.

## Repository Layout

| Directory | Contents |
|---|---|
| `JavaDevelopmentKitSamples/` | Single-API samples for the Horizon OS JSDK (Java/Kotlin). |
| `NativeDevelopmentKitSamples/` | Single-API samples for the Horizon OS NSDK (native C/C++). |
| `SupportLibrarySamples/` | Single-API samples for the Horizon OS Support Library. |
| `MixedSdkSamples/` | End-to-end samples that combine multiple Horizon OS SDK packages. |

## Requirements

To build and run these samples, you will need:

- A Meta Quest device running Horizon OS.
- Mac, Linux, or Windows host with:
  - [Android Studio](https://developer.android.com/studio) (latest stable release recommended).
  - Android SDK platform-tools (`adb`, `fastboot`).
  - [Native samples only] Android NDK (installed via Android Studio's SDK Manager), which is required by the `horizon-os-nsdk` Prefab package.
  - JDK 17 or newer.

Additional per-sample requirements (specific Horizon OS feature flags, system permissions, device builds) are documented in each sample's own `README.md`.

## Getting Started

Find a sample you want to try, then follow the instructions in that sample's `README.md`.

## Reporting Issues

File a [new issue](https://github.com/meta-quest/vros-api/issues/new/choose) — the bug report and sample request templates will guide you.

## Contributing

We welcome contributions. See [CONTRIBUTING.md](CONTRIBUTING.md) for the full process.

This repository is a public mirror of an internal Meta source-of-truth repository. Your contribution will appear in this repository once the internal review lands. The project Code of Conduct is [here](CODE_OF_CONDUCT.md).

## License

The vros-api samples are licensed under the **MIT License**, as found in the [LICENSE](LICENSE) file.
