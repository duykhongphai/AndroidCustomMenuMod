#pragma once

namespace onyx::menufreefire::features {

/** Điểm mở rộng được gọi khi toggle Bật ESP thay đổi. */
void onEspEnabledChanged(bool enabled) noexcept;

/** Điểm mở rộng được gọi khi toggle Đường Kẻ thay đổi. */
void onTracerLineChanged(bool enabled) noexcept;

/** Điểm mở rộng được gọi khi toggle Khung ESP thay đổi. */
void onEspBoxChanged(bool enabled) noexcept;

/** Điểm mở rộng được gọi khi toggle Thanh Máu thay đổi. */
void onHealthBarChanged(bool enabled) noexcept;

/** Điểm mở rộng được gọi khi toggle Bật Aimbot thay đổi. */
void onAimbotEnabledChanged(bool enabled) noexcept;

/** Điểm mở rộng được gọi khi toggle Bỏ qua Knock thay đổi. */
void onSkipKnockChanged(bool enabled) noexcept;

/** Điểm mở rộng được gọi khi toggle Aim Silent thay đổi. */
void onSilentAimChanged(bool enabled) noexcept;

/** Điểm mở rộng được gọi khi toggle Aim Legit thay đổi. */
void onLegitAimChanged(bool enabled) noexcept;

/** Điểm mở rộng được gọi khi toggle Hỗ trợ kéo tâm thay đổi. */
void onDragAimAssistChanged(bool enabled) noexcept;

/** Điểm mở rộng được gọi khi toggle Xoay thay đổi. */
void onRotationEnabledChanged(bool enabled) noexcept;

/** Điểm mở rộng được gọi khi toggle tương thích giả lập thay đổi. */
void onBypassEmulatorDetectChanged(bool enabled) noexcept;

/** Điểm mở rộng được gọi khi slider tốc độ xoay thay đổi. */
void onRotationSpeedChanged(float speed) noexcept;

/** Áp dụng các giá trị mặc định qua toàn bộ điểm mở rộng. */
void applyDefaults() noexcept;

}  // kết thúc namespace onyx::menufreefire::features
