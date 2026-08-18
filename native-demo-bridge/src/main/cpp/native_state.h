#pragma once

#include <cstdint>
#include <map>
#include <mutex>
#include <string>

namespace onyx::demo {

/**
 * Process-local state used only by the JNI sample.
 *
 * It deliberately has no memory scanning, hooking, injection, or access to
 * another process. Every app that packages this library gets its own instance.
 */
class NativeState final {
public:
    void setToggle(const std::string& featureId, bool enabled);
    void setValue(const std::string& featureId, float value);
    void setChoice(const std::string& featureId, const std::string& optionId);
    void performAction(const std::string& actionId);
    void reset();

    [[nodiscard]] std::string snapshot() const;

private:
    void resetLocked();

    mutable std::mutex mutex_;
    std::map<std::string, bool> toggles_;
    std::map<std::string, float> values_;
    std::map<std::string, std::string> choices_;
    std::map<std::string, std::uint64_t> actions_;
    std::uint64_t revision_ = 0;
};

NativeState& nativeState();

}  // namespace onyx::demo
