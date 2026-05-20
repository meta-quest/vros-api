# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# This source code is licensed under the MIT license found in the
# LICENSE file in the root directory of this source tree.

cmake_minimum_required(VERSION 3.22.1)

project("<project name here>")

# The horizon-os-nsdk package is provided by the Horizon OS NSDK AAR via Prefab.
# When building inside Android Studio with Gradle, Prefab generates the
# CMake config files automatically and find_package works out of the box.
#
# For standalone builds with the NDK toolchain, point CMAKE_FIND_ROOT_PATH
# to the extracted Prefab directory from the AAR.
find_package(horizon-os-nsdk REQUIRED CONFIG)

add_executable(virtual_camera_sample virtual_camera_sample.cpp)

target_link_libraries(virtual_camera_sample
    horizon-os-nsdk::hzos
    EGL
    GLESv2
    nativewindow
    log
)
