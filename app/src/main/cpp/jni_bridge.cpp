#include <android/log.h>
#include <jni.h>

#include <string>

#include "native_state.h"
#include "owned_offset_lab.h"

namespace {

constexpr char kLogTag[] = "OnyxNativeDemo";

class UtfChars final {
public:
    UtfChars(JNIEnv* environment, jstring value)
        : environment_(environment), value_(value) {
        if (value_ != nullptr) {
            characters_ = environment_->GetStringUTFChars(value_, nullptr);
        }
    }

    ~UtfChars() {
        if (characters_ != nullptr) {
            environment_->ReleaseStringUTFChars(value_, characters_);
        }
    }

    UtfChars(const UtfChars&) = delete;
    UtfChars& operator=(const UtfChars&) = delete;

    [[nodiscard]] std::string value() const {
        return characters_ == nullptr ? std::string() : std::string(characters_);
    }

private:
    JNIEnv* environment_;
    jstring value_;
    const char* characters_ = nullptr;
};

void logSnapshot() {
    const std::string snapshot = onyx::demo::nativeState().snapshot();
    __android_log_print(ANDROID_LOG_INFO, kLogTag, "%s", snapshot.c_str());
}

}  // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_nguyen_onyxmenu_demo_nativebridge_NativeDemoRuntime_nativeSetToggle(
        JNIEnv* environment,
        jclass,
        jstring featureId,
        jboolean enabled
) {
    onyx::demo::nativeState().setToggle(
            UtfChars(environment, featureId).value(),
            enabled == JNI_TRUE
    );
    logSnapshot();
}

extern "C" JNIEXPORT void JNICALL
Java_com_nguyen_onyxmenu_demo_nativebridge_NativeDemoRuntime_nativeSetValue(
        JNIEnv* environment,
        jclass,
        jstring featureId,
        jfloat value
) {
    onyx::demo::nativeState().setValue(UtfChars(environment, featureId).value(), value);
    logSnapshot();
}

extern "C" JNIEXPORT void JNICALL
Java_com_nguyen_onyxmenu_demo_nativebridge_NativeDemoRuntime_nativeSetChoice(
        JNIEnv* environment,
        jclass,
        jstring featureId,
        jstring optionId
) {
    onyx::demo::nativeState().setChoice(
            UtfChars(environment, featureId).value(),
            UtfChars(environment, optionId).value()
    );
    logSnapshot();
}

extern "C" JNIEXPORT void JNICALL
Java_com_nguyen_onyxmenu_demo_nativebridge_NativeDemoRuntime_nativePerformAction(
        JNIEnv* environment,
        jclass,
        jstring actionId
) {
    onyx::demo::nativeState().performAction(UtfChars(environment, actionId).value());
    logSnapshot();
}

extern "C" JNIEXPORT void JNICALL
Java_com_nguyen_onyxmenu_demo_nativebridge_NativeDemoRuntime_nativeReset(
        JNIEnv*,
        jclass
) {
    onyx::demo::nativeState().reset();
    logSnapshot();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_nguyen_onyxmenu_demo_nativebridge_NativeDemoRuntime_nativeSnapshot(
        JNIEnv* environment,
        jclass
) {
    const std::string snapshot = onyx::demo::nativeState().snapshot();
    return environment->NewStringUTF(snapshot.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_nguyen_onyxmenu_demo_nativebridge_NativeDemoRuntime_nativeRunOwnedOffsetLab(
        JNIEnv* environment,
        jclass
) {
    const std::string report = onyx::demo::runOwnedOffsetLab();
    __android_log_print(ANDROID_LOG_INFO, kLogTag, "offset lab: %s", report.c_str());
    return environment->NewStringUTF(report.c_str());
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM*, void*) {
    __android_log_print(ANDROID_LOG_INFO, kLogTag, "libonyx_demo_native loaded");
    return JNI_VERSION_1_6;
}
