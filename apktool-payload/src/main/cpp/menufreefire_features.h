#pragma once

#include <cstdint>
#include <vector>
#include <string>
#include <mutex>

namespace onyx::menufreefire::features {

// ============================================================================
// Cấu trúc dữ liệu xuất ra cho ESP/Aimbot
// ============================================================================
struct Vector3 {
    float x, y, z;
};

struct PlayerData {
    uintptr_t address;
    uint64_t userId;
    int teamIndex;
    std::string nickname;
    bool isDead;
    float curHP, maxHP;
    Vector3 position;
    Vector3 headPos;
    Vector3 chestPos;
};

// ============================================================================
// API dành cho UI (gọi từ Java qua JNI)
// ============================================================================
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

// ============================================================================
// Hàm khởi tạo hack (gọi từ JNI_OnLoad hoặc bootstrap)
// ============================================================================
void initializeHack(uintptr_t il2cppBase);

// ============================================================================
// Lấy danh sách player hiện tại (cho ESP/Aimbot)
// ============================================================================
std::vector<PlayerData> getPlayers();

} // namespace onyx::menufreefire::features