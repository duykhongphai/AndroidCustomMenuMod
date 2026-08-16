#include "native_state.h"

#include <iomanip>
#include <sstream>

namespace onyx::demo {
namespace {

std::string jsonString(const std::string& value) {
    std::ostringstream stream;
    stream << '"';
    for (const unsigned char character : value) {
        switch (character) {
            case '"':
                stream << "\\\"";
                break;
            case '\\':
                stream << "\\\\";
                break;
            case '\b':
                stream << "\\b";
                break;
            case '\f':
                stream << "\\f";
                break;
            case '\n':
                stream << "\\n";
                break;
            case '\r':
                stream << "\\r";
                break;
            case '\t':
                stream << "\\t";
                break;
            default:
                if (character < 0x20) {
                    stream << "\\u"
                           << std::hex << std::setw(4) << std::setfill('0')
                           << static_cast<int>(character)
                           << std::dec << std::setfill(' ');
                } else {
                    stream << character;
                }
        }
    }
    stream << '"';
    return stream.str();
}

template <typename Value, typename Formatter>
void appendMap(
        std::ostringstream& stream,
        const std::map<std::string, Value>& values,
        Formatter formatter
) {
    stream << '{';
    bool first = true;
    for (const auto& [key, value] : values) {
        if (!first) {
            stream << ',';
        }
        first = false;
        stream << jsonString(key) << ':';
        formatter(stream, value);
    }
    stream << '}';
}

}  // namespace

void NativeState::setToggle(const std::string& featureId, const bool enabled) {
    std::lock_guard lock(mutex_);
    toggles_[featureId] = enabled;
    ++revision_;
}

void NativeState::setValue(const std::string& featureId, const float value) {
    std::lock_guard lock(mutex_);
    values_[featureId] = value;
    ++revision_;
}

void NativeState::setChoice(
        const std::string& featureId,
        const std::string& optionId
) {
    std::lock_guard lock(mutex_);
    choices_[featureId] = optionId;
    ++revision_;
}

void NativeState::performAction(const std::string& actionId) {
    std::lock_guard lock(mutex_);
    if (actionId == "clear_demo_state") {
        resetLocked();
    }
    ++actions_[actionId];
    ++revision_;
}

void NativeState::reset() {
    std::lock_guard lock(mutex_);
    resetLocked();
    ++revision_;
}

void NativeState::resetLocked() {
    toggles_.clear();
    values_.clear();
    choices_.clear();
    actions_.clear();
}

std::string NativeState::snapshot() const {
    std::lock_guard lock(mutex_);
    std::ostringstream stream;
    stream << std::setprecision(4);
    stream << "{\"revision\":" << revision_ << ",\"toggles\":";
    appendMap(stream, toggles_, [](std::ostringstream& output, const bool value) {
        output << (value ? "true" : "false");
    });
    stream << ",\"values\":";
    appendMap(stream, values_, [](std::ostringstream& output, const float value) {
        output << value;
    });
    stream << ",\"choices\":";
    appendMap(stream, choices_, [](std::ostringstream& output, const std::string& value) {
        output << jsonString(value);
    });
    stream << ",\"actions\":";
    appendMap(stream, actions_, [](std::ostringstream& output, const std::uint64_t value) {
        output << value;
    });
    stream << '}';
    return stream.str();
}

NativeState& nativeState() {
    static NativeState instance;
    return instance;
}

}  // namespace onyx::demo
