#include <android/log.h>
#include <jni.h>

#include <string>

#include "menufreefire_state.h"

namespace {

constexpr char kLogTag[] = "OnyxMenuFreeFire";

/** Quản lý vòng đời chuỗi UTF-8 được mượn từ JVM. */
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

void logRejectedId(const char* operation, const std::string& featureId) {
    __android_log_print(
            ANDROID_LOG_WARN,
            kLogTag,
            "Đã từ chối %s có ID không hợp lệ: %s",
            operation,
            featureId.c_str()
    );
}

}  // kết thúc namespace nội bộ

extern "C" JNIEXPORT jboolean JNICALL
Java_com_nguyen_onyxpayload_nativebridge_MenuFreeFireRuntime_nativeSetToggle(
        JNIEnv* environment,
        jclass,
        jstring featureId,
        jboolean enabled
) {
    const std::string id = UtfChars(environment, featureId).value();
    const bool accepted = onyx::menufreefire::featureState().setToggle(
            id,
            enabled == JNI_TRUE
    );
    if (!accepted) {
        logRejectedId("công tắc", id);
    }
    return accepted ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_nguyen_onyxpayload_nativebridge_MenuFreeFireRuntime_nativeSetValue(
        JNIEnv* environment,
        jclass,
        jstring featureId,
        jfloat value
) {
    const std::string id = UtfChars(environment, featureId).value();
    const bool accepted = onyx::menufreefire::featureState().setValue(id, value);
    if (!accepted) {
        logRejectedId("giá trị", id);
    }
    return accepted ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_nguyen_onyxpayload_nativebridge_MenuFreeFireRuntime_nativeReset(
        JNIEnv*,
        jclass
) {
    onyx::menufreefire::featureState().reset();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_nguyen_onyxpayload_nativebridge_MenuFreeFireRuntime_nativeSnapshot(
        JNIEnv* environment,
        jclass
) {
    const std::string snapshot = onyx::menufreefire::featureState().snapshot();
    return environment->NewStringUTF(snapshot.c_str());
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM*, void*) {
    __android_log_print(
            ANDROID_LOG_INFO,
            kLogTag,
            "Đã nạp libonyx_menufreefire"
    );
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL
Java_com_nguyen_onyxmenu_demo_nativebridge_NativeDemoRuntime_nativeInitializeHack(
        JNIEnv* env,
        jclass,
        jlong il2cppBase
) {
    onyx::menufreefire::features::initializeHack((uintptr_t)il2cppBase);
}