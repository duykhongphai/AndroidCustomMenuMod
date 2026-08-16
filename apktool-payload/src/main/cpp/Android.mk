LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := onyx_menufreefire
LOCAL_SRC_FILES := \
    jni_bridge.cpp \
    menufreefire_features.cpp \
    menufreefire_state.cpp
LOCAL_CPPFLAGS := -std=c++17 -Wall -Wextra -Werror
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)
