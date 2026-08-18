#include "menufreefire_features.h"

#include <android/log.h>

#define LOG_TAG "OnyxMenuFreeFire"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace onyx::menufreefire::features {

void onEspEnabledChanged(const bool enabled) noexcept {
    LOGI("esp_enabled = %s", enabled ? "true" : "false");
}

void onTracerLineChanged(const bool enabled) noexcept {
    LOGI("tracer_line = %s", enabled ? "true" : "false");
}

void onEspBoxChanged(const bool enabled) noexcept {
    LOGI("esp_box = %s", enabled ? "true" : "false");
}

void onHealthBarChanged(const bool enabled) noexcept {
    LOGI("health_bar = %s", enabled ? "true" : "false");
}

void onAimbotEnabledChanged(const bool enabled) noexcept {
    LOGI("aimbot_enabled = %s", enabled ? "true" : "false");
}

void onSkipKnockChanged(const bool enabled) noexcept {
    LOGI("skip_knock = %s", enabled ? "true" : "false");
}

void onSilentAimChanged(const bool enabled) noexcept {
    LOGI("silent_aim = %s", enabled ? "true" : "false");
}

void onLegitAimChanged(const bool enabled) noexcept {
    LOGI("legit_aim = %s", enabled ? "true" : "false");
}

void onDragAimAssistChanged(const bool enabled) noexcept {
    LOGI("drag_aim_assist = %s", enabled ? "true" : "false");
}

void onRotationEnabledChanged(const bool enabled) noexcept {
    LOGI("rotation_enabled = %s", enabled ? "true" : "false");
}

void onBypassEmulatorDetectChanged(const bool enabled) noexcept {
    LOGI("bypass_emulator_detect = %s", enabled ? "true" : "false");
}

void onRotationSpeedChanged(const float speed) noexcept {
    LOGI("rotation_speed = %.2f", speed);
}

void applyDefaults() noexcept {
    LOGI("native preview state reset to defaults");
}

}  // namespace onyx::menufreefire::features
