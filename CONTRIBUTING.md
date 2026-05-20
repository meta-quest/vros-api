<!--
  Copyright (c) Meta Platforms, Inc. and affiliates.

  This source code is licensed under the MIT license found in the
  LICENSE file in the root directory of this source tree.
-->

# Contributing to vros-api

Thanks for your interest in contributing to the Horizon OS SDK samples!

Before contributing, please review our [Code of Conduct](CODE_OF_CONDUCT.md).

## How pull requests work

This repository is a public mirror of an internal Meta source-of-truth repository.

Because of that, pull requests cannot be merged directly. Instead:

1. Fork the repository and create a feature branch from `main`.
2. Make your changes in a focused commit series.
3. Open a pull request with a clear description of what changed and why.
4. A maintainer will import your PR into the internal review system, where it is reviewed and (if approved) landed.
5. Once the internal change lands, the sync will publish your commit to GitHub and your pull request will be closed automatically.

You will not see your PR being directly merged on GitHub, but your changes will appear in the repository once the sync runs.

## Sample structure

Each sample lives in its own directory under one of the four top-level sample groups:

- `JavaDevelopmentKitSamples/<name>/` — Horizon OS Java Development Kit (JDK) samples, typically in Java and/or Kotlin.
- `NativeDevelopmentKitSamples/<name>/` — Horizon OS Native Development Kit (NDK) samples, typically in C and/or C++.
- `SupportLibrarySamples/<name>/` — Horizon OS Support Library samples, typically in Java and/or Kotlin.
- `MixedSdkSamples/<name>/` — Samples that combine two or more Horizon OS SDK packages.

A sample directory should contain:

- A `README.md` describing what APIs/capabilities the sample demonstrates, and how to build and run it
- Source code files
- Supplemental assets, such as icon images in the form of Android resources
- Build configuration:
  - Native: `CMakeLists.txt` resolving `horizon-os-nsdk` via Prefab
  - JVM: `build.gradle.kts` declaring the `horizon-os-jsdk` dependency

## Writing guidelines

- Keep each sample minimal and focused.
- Include a clear, paragraph-style file-header comment explaining what the sample demonstrates and how it works.
- Use the standard MIT license header (see existing files for the exact wording).
- Prefer modern Android idioms (Kotlin + Coroutines for JVM, modern C++ with `std::unique_ptr` / `std::lock_guard` for native).
- Do not hardcode internal-only identifiers, build tags, or paths.

## Reporting issues

Open a GitHub issue. Please include:

- The sample directory name.
- A clear description of the problem.
- Steps to reproduce, expected behavior, and actual behavior.
- Device build version (`adb shell getprop ro.build.fingerprint`).

## Contributor License Agreement

To accept your pull request, we need you to submit a [Contributor License Agreement](https://code.facebook.com/cla). You only need to complete this once for any Meta open source project: <https://code.facebook.com/cla>.

## License

By contributing, you agree that your contributions will be licensed under the LICENSE file in the root of this repository.
