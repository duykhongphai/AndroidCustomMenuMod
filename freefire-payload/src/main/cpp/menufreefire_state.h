#pragma once

#include <cstdint>
#include <mutex>
#include <string>
#include <string_view>

namespace onyx::menufreefire {

/** Trạng thái cấu hình native thuộc tiến trình đang nạp payload. */
class FeatureState final {
public:
    /** Cập nhật một công tắc đã khai báo; trả về false nếu ID không hợp lệ. */
    bool setToggle(std::string_view featureId, bool enabled);

    /** Cập nhật một giá trị số đã khai báo; trả về false nếu ID hoặc số không hợp lệ. */
    bool setValue(std::string_view featureId, float value);

    /** Khôi phục toàn bộ giá trị mặc định. */
    void reset();

    /** Xuất ảnh chụp JSON ổn định để kiểm tra bridge và chẩn đoán. */
    [[nodiscard]] std::string snapshot() const;

private:
    struct Config final {
        bool espEnabled = false;
        bool tracerLine = false;
        bool espBox = false;
        bool healthBar = false;
        bool aimbotEnabled = false;
        bool skipKnock = false;
        bool silentAim = false;
        bool legitAim = false;
        bool dragAimAssist = false;
        bool rotationEnabled = false;
        bool bypassEmulatorDetect = false;
        float rotationSpeed = 5.0F;
    };

    mutable std::mutex mutex_;
    Config config_;
    std::uint64_t revision_ = 0;
};

/** Trả về thể hiện duy nhất của trạng thái native trong tiến trình. */
FeatureState& featureState();

}  // kết thúc namespace onyx::menufreefire
