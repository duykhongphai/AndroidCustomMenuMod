#include "menufreefire_state.h"

#include "menufreefire_features.h"

#include <algorithm>
#include <cmath>
#include <iomanip>
#include <sstream>

namespace onyx::menufreefire {
namespace {

constexpr float kMinimumRotationSpeed = 1.0F;
constexpr float kMaximumRotationSpeed = 10.0F;

enum class ToggleHandler {
    EspEnabled,
    TracerLine,
    EspBox,
    HealthBar,
    AimbotEnabled,
    SkipKnock,
    SilentAim,
    LegitAim,
    DragAimAssist,
    RotationEnabled,
    BypassEmulatorDetect
};

void appendBoolean(std::ostringstream& output, const bool value) {
    output << (value ? "true" : "false");
}

void dispatchToggleHandler(const ToggleHandler handler, const bool enabled) noexcept {
    switch (handler) {
        case ToggleHandler::EspEnabled:
            features::onEspEnabledChanged(enabled);
            break;
        case ToggleHandler::TracerLine:
            features::onTracerLineChanged(enabled);
            break;
        case ToggleHandler::EspBox:
            features::onEspBoxChanged(enabled);
            break;
        case ToggleHandler::HealthBar:
            features::onHealthBarChanged(enabled);
            break;
        case ToggleHandler::AimbotEnabled:
            features::onAimbotEnabledChanged(enabled);
            break;
        case ToggleHandler::SkipKnock:
            features::onSkipKnockChanged(enabled);
            break;
        case ToggleHandler::SilentAim:
            features::onSilentAimChanged(enabled);
            break;
        case ToggleHandler::LegitAim:
            features::onLegitAimChanged(enabled);
            break;
        case ToggleHandler::DragAimAssist:
            features::onDragAimAssistChanged(enabled);
            break;
        case ToggleHandler::RotationEnabled:
            features::onRotationEnabledChanged(enabled);
            break;
        case ToggleHandler::BypassEmulatorDetect:
            features::onBypassEmulatorDetectChanged(enabled);
            break;
    }
}

}  // kết thúc namespace nội bộ

bool FeatureState::setToggle(const std::string_view featureId, const bool enabled) {
    bool* destination = nullptr;
    ToggleHandler handler = ToggleHandler::EspEnabled;

    std::unique_lock lock(mutex_);
    if (featureId == "esp_enabled") {
        destination = &config_.espEnabled;
        handler = ToggleHandler::EspEnabled;
    } else if (featureId == "tracer_line") {
        destination = &config_.tracerLine;
        handler = ToggleHandler::TracerLine;
    } else if (featureId == "esp_box") {
        destination = &config_.espBox;
        handler = ToggleHandler::EspBox;
    } else if (featureId == "health_bar") {
        destination = &config_.healthBar;
        handler = ToggleHandler::HealthBar;
    } else if (featureId == "aimbot_enabled") {
        destination = &config_.aimbotEnabled;
        handler = ToggleHandler::AimbotEnabled;
    } else if (featureId == "skip_knock") {
        destination = &config_.skipKnock;
        handler = ToggleHandler::SkipKnock;
    } else if (featureId == "silent_aim") {
        destination = &config_.silentAim;
        handler = ToggleHandler::SilentAim;
    } else if (featureId == "legit_aim") {
        destination = &config_.legitAim;
        handler = ToggleHandler::LegitAim;
    } else if (featureId == "drag_aim_assist") {
        destination = &config_.dragAimAssist;
        handler = ToggleHandler::DragAimAssist;
    } else if (featureId == "rotation_enabled") {
        destination = &config_.rotationEnabled;
        handler = ToggleHandler::RotationEnabled;
    } else if (featureId == "bypass_emulator_detect") {
        destination = &config_.bypassEmulatorDetect;
        handler = ToggleHandler::BypassEmulatorDetect;
    } else {
        return false;
    }

    if (*destination != enabled) {
        *destination = enabled;
        ++revision_;
    }
    lock.unlock();
    dispatchToggleHandler(handler, enabled);
    return true;
}

bool FeatureState::setValue(const std::string_view featureId, const float value) {
    if (featureId != "rotation_speed" || !std::isfinite(value)) {
        return false;
    }

    const float normalized = std::clamp(
            value,
            kMinimumRotationSpeed,
            kMaximumRotationSpeed
    );
    {
        std::lock_guard lock(mutex_);
        if (config_.rotationSpeed != normalized) {
            config_.rotationSpeed = normalized;
            ++revision_;
        }
    }
    features::onRotationSpeedChanged(normalized);
    return true;
}

void FeatureState::reset() {
    {
        std::lock_guard lock(mutex_);
        config_ = Config{};
        ++revision_;
    }
    features::applyDefaults();
}

std::string FeatureState::snapshot() const {
    std::lock_guard lock(mutex_);

    const bool tracerEffective = config_.espEnabled && config_.tracerLine;
    const bool boxEffective = config_.espEnabled && config_.espBox;
    const bool healthEffective = config_.espEnabled && config_.healthBar;
    const bool aimbotEffective = config_.aimbotEnabled;
    const bool rotationEffective = config_.rotationEnabled;
    const int espPrimitiveCount = static_cast<int>(tracerEffective)
            + static_cast<int>(boxEffective)
            + static_cast<int>(healthEffective);

    std::string_view aimMode = "off";
    if (aimbotEffective) {
        if (config_.silentAim) {
            aimMode = "silent";
        } else if (config_.legitAim) {
            aimMode = "legit";
        } else {
            aimMode = "standard";
        }
    }

    std::ostringstream output;
    output << std::fixed << std::setprecision(2);
    output << "{\"revision\":" << revision_ << ",\"toggles\":{";
    output << "\"esp_enabled\":";
    appendBoolean(output, config_.espEnabled);
    output << ",\"tracer_line\":";
    appendBoolean(output, config_.tracerLine);
    output << ",\"esp_box\":";
    appendBoolean(output, config_.espBox);
    output << ",\"health_bar\":";
    appendBoolean(output, config_.healthBar);
    output << ",\"aimbot_enabled\":";
    appendBoolean(output, config_.aimbotEnabled);
    output << ",\"skip_knock\":";
    appendBoolean(output, config_.skipKnock);
    output << ",\"silent_aim\":";
    appendBoolean(output, config_.silentAim);
    output << ",\"legit_aim\":";
    appendBoolean(output, config_.legitAim);
    output << ",\"drag_aim_assist\":";
    appendBoolean(output, config_.dragAimAssist);
    output << ",\"rotation_enabled\":";
    appendBoolean(output, config_.rotationEnabled);
    output << ",\"bypass_emulator_detect\":";
    appendBoolean(output, config_.bypassEmulatorDetect);
    output << "},\"values\":{\"rotation_speed\":" << config_.rotationSpeed << "}";
    output << ",\"effective\":{\"esp_primitive_count\":" << espPrimitiveCount;
    output << ",\"aim_mode\":\"" << aimMode << "\"";
    output << ",\"skip_knock\":";
    appendBoolean(output, aimbotEffective && config_.skipKnock);
    output << ",\"drag_aim_assist\":";
    appendBoolean(output, aimbotEffective && config_.dragAimAssist);
    output << ",\"rotation_speed\":"
           << (rotationEffective ? config_.rotationSpeed : 0.0F) << "}}";
    return output.str();
}

FeatureState& featureState() {
    static FeatureState state;
    return state;
}

}  // kết thúc namespace onyx::menufreefire
