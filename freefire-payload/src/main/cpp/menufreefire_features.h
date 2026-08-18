#pragma once

namespace onyx::menufreefire::features {

// UI handlers for the standalone preview. They only emit Logcat events.
void onEspEnabledChanged(bool enabled) noexcept;
void onTracerLineChanged(bool enabled) noexcept;
void onEspBoxChanged(bool enabled) noexcept;
void onHealthBarChanged(bool enabled) noexcept;
void onAimbotEnabledChanged(bool enabled) noexcept;
void onSkipKnockChanged(bool enabled) noexcept;
void onSilentAimChanged(bool enabled) noexcept;
void onLegitAimChanged(bool enabled) noexcept;
void onDragAimAssistChanged(bool enabled) noexcept;
void onRotationEnabledChanged(bool enabled) noexcept;
void onBypassEmulatorDetectChanged(bool enabled) noexcept;
void onRotationSpeedChanged(float speed) noexcept;
void applyDefaults() noexcept;

}  // namespace onyx::menufreefire::features
