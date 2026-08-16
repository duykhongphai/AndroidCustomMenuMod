LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := onyx_demo_native
LOCAL_SRC_FILES := \
    jni_bridge.cpp \
    native_state.cpp \
    owned_offset_lab.cpp
LOCAL_CPPFLAGS := -std=c++17 -Wall -Wextra -Werror
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)
