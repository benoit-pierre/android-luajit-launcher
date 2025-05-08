cmake_minimum_required(VERSION 3.16.3)
project(android-luajit-launcher LANGUAGES C CXX)

find_package(PkgConfig REQUIRED)
pkg_check_modules(LuaJIT luajit REQUIRED IMPORTED_TARGET)
pkg_check_modules(libarchive libarchive REQUIRED IMPORTED_TARGET)

add_library(7z)
target_link_libraries(7z PRIVATE PkgConfig::libarchive android log)
target_sources(7z PRIVATE jni/unarchive.cpp)

add_library(ioctl SHARED)
target_sources(ioctl PRIVATE jni/ioctl/ioctl.cpp)

add_library(luajit-launcher SHARED)
target_include_directories(luajit-launcher PUBLIC jni/android_native_app_glue)
target_link_libraries(luajit-launcher PRIVATE PkgConfig::LuaJIT android log)
target_link_options(luajit-launcher PRIVATE -u ANativeActivity_onCreate)
target_sources(luajit-launcher PRIVATE jni/android_native_app_glue/android_native_app_glue.c jni/jni_helper.c jni/main.c)

install(TARGETS 7z)
install(TARGETS ioctl)
install(TARGETS luajit-launcher)
