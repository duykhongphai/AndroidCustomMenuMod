#pragma once

#include <jni.h>
#include <cstdint>

namespace onyx::menufreefire::features {

void initializeHack(uintptr_t il2cppBase);
void setMatchBase(uintptr_t matchBase);
void registerCallback(JNIEnv* env, jobject obj);
void setScreenSize(int width, int height);
void setJavaVM(JavaVM* vm);

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

} // namespace onyx::menufreefire::features