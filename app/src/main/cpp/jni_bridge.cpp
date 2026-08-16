#include <android/log.h>
#include <jni.h>
#include <string>
#include "menufreefire_features.h"

#define LOG_TAG "OnyxBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace {
class UtfChars final {
    JNIEnv* env_;
    jstring str_;
    const char* chars_ = nullptr;
public:
    UtfChars(JNIEnv* env, jstring str) : env_(env), str_(str) {
        if (str) chars_ = env->GetStringUTFChars(str, nullptr);
    }
    ~UtfChars() { if (chars_) env_->ReleaseStringUTFChars(str_, chars_); }
    std::string value() const { return chars_ ? std::string(chars_) : std::string(); }
};
} // namespace

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    LOGI("JNI_OnLoad called");
    onyx::menufreefire::features::setJavaVM(vm);
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL
Java_com_nguyen_onyxpayload_nativebridge_MenuFreeFireRuntime_nativeInitializeHack(
        JNIEnv* env,
        jclass,
        jlong il2cppBase
) {
    onyx::menufreefire::features::initializeHack((uintptr_t)il2cppBase);
}

extern "C" JNIEXPORT void JNICALL
Java_com_nguyen_onyxpayload_nativebridge_MenuFreeFireRuntime_nativeRegisterCallback(
        JNIEnv* env,
        jclass,
        jobject obj
) {
    onyx::menufreefire::features::registerCallback(env, obj);
}

extern "C" JNIEXPORT void JNICALL
Java_com_nguyen_onyxpayload_nativebridge_MenuFreeFireRuntime_nativeSetScreenSize(
        JNIEnv*,
        jclass,
        jint width,
        jint height
) {
    onyx::menufreefire::features::setScreenSize(width, height);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_nguyen_onyxpayload_nativebridge_MenuFreeFireRuntime_nativeSetToggle(
        JNIEnv* env,
        jclass,
        jstring featureId,
        jboolean enabled
) {
    // Implement logic to call appropriate toggle functions based on featureId
    // For now, we just log and return true
    LOGI("Toggle %s = %d", UtfChars(env, featureId).value().c_str(), enabled);
    return JNI_TRUE;
}