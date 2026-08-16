#pragma once

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <functional>
#include <limits>
#include <memory>
#include <new>
#include <optional>
#include <type_traits>
#include <utility>

namespace onyx::demo::offset {

/** Overflow-checked helpers for composing byte offsets. */
struct OffsetMath final {
    [[nodiscard]] static std::optional<std::size_t> add(
            const std::size_t left,
            const std::size_t right
    ) {
        if (right > std::numeric_limits<std::size_t>::max() - left) {
            return std::nullopt;
        }
        return left + right;
    }

    [[nodiscard]] static std::optional<std::size_t> multiply(
            const std::size_t left,
            const std::size_t right
    ) {
        if (left != 0 && right > std::numeric_limits<std::size_t>::max() / left) {
            return std::nullopt;
        }
        return left * right;
    }

    /** Computes first + index * stride + field without wrapping size_t. */
    [[nodiscard]] static std::optional<std::size_t> element(
            const std::size_t first,
            const std::size_t index,
            const std::size_t stride,
            const std::size_t field = 0
    ) {
        const auto indexBytes = multiply(index, stride);
        if (!indexBytes) {
            return std::nullopt;
        }
        const auto elementBase = add(first, *indexBytes);
        if (!elementBase) {
            return std::nullopt;
        }
        return add(*elementBase, field);
    }
};

/**
 * A bounds-checked view over a live object or array owned by this process.
 *
 * Bounds and alignment can be checked here; the caller must still use the exact
 * field type and an offset that points to a live subobject of that type.
 */
class OwnedMemoryView final {
public:
    template <typename Owner>
    [[nodiscard]] static OwnedMemoryView fromObject(Owner& owner) {
        static_assert(!std::is_pointer_v<Owner>, "Pass the object, not an object pointer");
        return OwnedMemoryView(
                reinterpret_cast<std::byte*>(std::addressof(owner)),
                sizeof(Owner)
        );
    }

    template <typename Element, std::size_t Count>
    [[nodiscard]] static OwnedMemoryView fromArray(Element (&elements)[Count]) {
        return OwnedMemoryView(
                reinterpret_cast<std::byte*>(elements),
                sizeof(Element) * Count
        );
    }

    [[nodiscard]] std::size_t size() const {
        return size_;
    }

    [[nodiscard]] bool empty() const {
        return base_ == nullptr || size_ == 0;
    }

    [[nodiscard]] bool contains(
            const std::size_t offset,
            const std::size_t byteCount
    ) const {
        return base_ != nullptr && offset <= size_ && byteCount <= size_ - offset;
    }

    template <typename Value>
    [[nodiscard]] bool isAligned(const std::size_t offset) const {
        if (!contains(offset, sizeof(Value))) {
            return false;
        }
        const auto address = reinterpret_cast<std::uintptr_t>(base_ + offset);
        return address % alignof(Value) == 0;
    }

    template <typename Value>
    [[nodiscard]] Value* pointerAt(const std::size_t offset) {
        static_assert(!std::is_void_v<Value>, "Value must have a concrete type");
        if (!isAligned<Value>(offset)) {
            return nullptr;
        }
        return std::launder(reinterpret_cast<Value*>(base_ + offset));
    }

    template <typename Value>
    [[nodiscard]] const Value* pointerAt(const std::size_t offset) const {
        static_assert(!std::is_void_v<Value>, "Value must have a concrete type");
        if (!isAligned<Value>(offset)) {
            return nullptr;
        }
        return std::launder(reinterpret_cast<const Value*>(base_ + offset));
    }

    template <typename Value>
    [[nodiscard]] std::optional<Value> read(const std::size_t offset) const {
        static_assert(std::is_copy_constructible_v<Value>, "Value must be copyable");
        const Value* value = pointerAt<Value>(offset);
        if (value == nullptr) {
            return std::nullopt;
        }
        return *value;
    }

    template <typename Value>
    [[nodiscard]] Value readOr(
            const std::size_t offset,
            Value fallback
    ) const {
        const auto value = read<Value>(offset);
        return value ? *value : std::move(fallback);
    }

    template <typename Value>
    bool write(const std::size_t offset, const Value& value) {
        static_assert(std::is_copy_assignable_v<Value>, "Value must be assignable");
        Value* destination = pointerAt<Value>(offset);
        if (destination == nullptr) {
            return false;
        }
        *destination = value;
        return true;
    }

    template <typename Value, typename Updater>
    bool update(const std::size_t offset, Updater&& updater) {
        Value* value = pointerAt<Value>(offset);
        if (value == nullptr) {
            return false;
        }
        std::invoke(std::forward<Updater>(updater), *value);
        return true;
    }

    template <typename Number>
    bool tryAdd(const std::size_t offset, const Number delta) {
        static_assert(std::is_arithmetic_v<Number>, "Number must be arithmetic");
        static_assert(!std::is_same_v<std::remove_cv_t<Number>, bool>, "Use toggleBool for bool");
        Number* value = pointerAt<Number>(offset);
        if (value == nullptr) {
            return false;
        }

        if constexpr (std::is_floating_point_v<Number>) {
            const Number result = *value + delta;
            if (!std::isfinite(result)) {
                return false;
            }
            *value = result;
            return true;
        } else if constexpr (std::is_unsigned_v<Number>) {
            if (*value > std::numeric_limits<Number>::max() - delta) {
                return false;
            }
        } else {
            if (delta > 0 && *value > std::numeric_limits<Number>::max() - delta) {
                return false;
            }
            if (delta < 0 && *value < std::numeric_limits<Number>::min() - delta) {
                return false;
            }
        }

        *value = static_cast<Number>(*value + delta);
        return true;
    }

    template <typename Number>
    bool trySubtract(const std::size_t offset, const Number delta) {
        static_assert(std::is_arithmetic_v<Number>, "Number must be arithmetic");
        static_assert(!std::is_same_v<std::remove_cv_t<Number>, bool>, "Use toggleBool for bool");
        Number* value = pointerAt<Number>(offset);
        if (value == nullptr) {
            return false;
        }

        if constexpr (std::is_floating_point_v<Number>) {
            const Number result = *value - delta;
            if (!std::isfinite(result)) {
                return false;
            }
            *value = result;
            return true;
        } else if constexpr (std::is_unsigned_v<Number>) {
            if (*value < delta) {
                return false;
            }
        } else {
            if (delta > 0 && *value < std::numeric_limits<Number>::min() + delta) {
                return false;
            }
            if (delta < 0 && *value > std::numeric_limits<Number>::max() + delta) {
                return false;
            }
        }

        *value = static_cast<Number>(*value - delta);
        return true;
    }

    template <typename Number>
    bool clamp(
            const std::size_t offset,
            const Number minimum,
            const Number maximum
    ) {
        static_assert(std::is_arithmetic_v<Number>, "Number must be arithmetic");
        static_assert(!std::is_same_v<std::remove_cv_t<Number>, bool>, "Use toggleBool for bool");
        if (maximum < minimum) {
            return false;
        }
        return update<Number>(offset, [&](Number& value) {
            value = std::clamp(value, minimum, maximum);
        });
    }

    bool toggleBool(const std::size_t offset) {
        return update<bool>(offset, [](bool& value) {
            value = !value;
        });
    }

    template <typename Integer>
    bool setBits(const std::size_t offset, const Integer mask) {
        static_assert(std::is_integral_v<Integer>, "Integer must be integral");
        static_assert(!std::is_same_v<std::remove_cv_t<Integer>, bool>, "Use toggleBool for bool");
        return update<Integer>(offset, [&](Integer& value) {
            value = static_cast<Integer>(value | mask);
        });
    }

    template <typename Integer>
    bool clearBits(const std::size_t offset, const Integer mask) {
        static_assert(std::is_integral_v<Integer>, "Integer must be integral");
        static_assert(!std::is_same_v<std::remove_cv_t<Integer>, bool>, "Use toggleBool for bool");
        return update<Integer>(offset, [&](Integer& value) {
            value = static_cast<Integer>(value & static_cast<Integer>(~mask));
        });
    }

    template <typename Integer>
    bool toggleBits(const std::size_t offset, const Integer mask) {
        static_assert(std::is_integral_v<Integer>, "Integer must be integral");
        static_assert(!std::is_same_v<std::remove_cv_t<Integer>, bool>, "Use toggleBool for bool");
        return update<Integer>(offset, [&](Integer& value) {
            value = static_cast<Integer>(value ^ mask);
        });
    }

    template <typename Integer>
    [[nodiscard]] bool hasAllBits(
            const std::size_t offset,
            const Integer mask
    ) const {
        static_assert(std::is_integral_v<Integer>, "Integer must be integral");
        static_assert(!std::is_same_v<std::remove_cv_t<Integer>, bool>, "Use toggleBool for bool");
        const auto value = read<Integer>(offset);
        return value && ((*value & mask) == mask);
    }

    template <typename Element>
    [[nodiscard]] Element* elementAt(
            const std::size_t first,
            const std::size_t index,
            const std::size_t stride = sizeof(Element)
    ) {
        if (stride < sizeof(Element)) {
            return nullptr;
        }
        const auto offset = OffsetMath::element(first, index, stride);
        return offset ? pointerAt<Element>(*offset) : nullptr;
    }

    template <typename Element>
    [[nodiscard]] const Element* elementAt(
            const std::size_t first,
            const std::size_t index,
            const std::size_t stride = sizeof(Element)
    ) const {
        if (stride < sizeof(Element)) {
            return nullptr;
        }
        const auto offset = OffsetMath::element(first, index, stride);
        return offset ? pointerAt<Element>(*offset) : nullptr;
    }

    template <typename Element>
    [[nodiscard]] std::optional<Element> readElement(
            const std::size_t first,
            const std::size_t index,
            const std::size_t stride = sizeof(Element)
    ) const {
        const Element* value = elementAt<Element>(first, index, stride);
        return value == nullptr ? std::nullopt : std::optional<Element>(*value);
    }

    template <typename Element>
    bool writeElement(
            const std::size_t first,
            const std::size_t index,
            const Element& value,
            const std::size_t stride = sizeof(Element)
    ) {
        Element* destination = elementAt<Element>(first, index, stride);
        if (destination == nullptr) {
            return false;
        }
        *destination = value;
        return true;
    }

    template <typename FunctionPointer>
    [[nodiscard]] std::optional<FunctionPointer> functionAt(
            const std::size_t slotOffset
    ) const {
        static_assert(std::is_pointer_v<FunctionPointer>, "Expected a function pointer");
        static_assert(
                std::is_function_v<std::remove_pointer_t<FunctionPointer>>,
                "Expected a function pointer"
        );
        const auto function = read<FunctionPointer>(slotOffset);
        if (!function || *function == nullptr) {
            return std::nullopt;
        }
        return function;
    }

private:
    OwnedMemoryView(std::byte* base, const std::size_t size)
        : base_(base), size_(size) {
    }

    std::byte* base_;
    std::size_t size_;
};

}  // namespace onyx::demo::offset
