#include "owned_offset_lab.h"

#include "owned_offset_helpers.h"

#include <cstddef>
#include <cstdint>
#include <iomanip>
#include <limits>
#include <sstream>
#include <type_traits>

namespace onyx::demo {
namespace {

/** A normal C++ object whose layout belongs to this demo. */
struct DemoActor final {
    std::int32_t health = 38;
    float speed = 1.0F;
    std::uint32_t tickCount = 0;
    std::uint32_t flags = 0;
    bool shieldEnabled = false;
};

using TickFunction = void (*)(DemoActor* actor, float deltaSeconds);

/**
 * A stable function-table pattern used by plugin APIs owned by an app.
 * The offset below points to a function-pointer slot, not arbitrary code.
 */
struct DemoFunctionTable final {
    std::uint32_t version = 1;
    TickFunction tick = nullptr;
};

static_assert(std::is_standard_layout_v<DemoActor>);
static_assert(std::is_standard_layout_v<DemoFunctionTable>);

constexpr std::size_t kHealthOffset = offsetof(DemoActor, health);
constexpr std::size_t kSpeedOffset = offsetof(DemoActor, speed);
constexpr std::size_t kTickCountOffset = offsetof(DemoActor, tickCount);
constexpr std::size_t kFlagsOffset = offsetof(DemoActor, flags);
constexpr std::size_t kShieldOffset = offsetof(DemoActor, shieldEnabled);
constexpr std::size_t kTickSlotOffset = offsetof(DemoFunctionTable, tick);

void tickActor(DemoActor* actor, const float deltaSeconds) {
    if (actor == nullptr) {
        return;
    }
    ++actor->tickCount;
    actor->health -= 3;
    actor->speed += deltaSeconds;
}

}  // namespace

std::string runOwnedOffsetLab() {
    DemoActor actor;
    offset::OwnedMemoryView actorMemory = offset::OwnedMemoryView::fromObject(actor);

    // Checked pointers to live fields through object base + known-good offset.
    auto* health = actorMemory.pointerAt<std::int32_t>(kHealthOffset);
    auto* speed = actorMemory.pointerAt<float>(kSpeedOffset);

    // if/else is ordinary C++ after a value has been obtained from the field.
    if (health != nullptr && *health < 50) {
        actorMemory.write<bool>(kShieldOffset, false);
        actorMemory.toggleBool(kShieldOffset);
    } else {
        actorMemory.write<bool>(kShieldOffset, false);
    }

    // Arithmetic helpers reject invalid addresses and numeric overflow.
    for (int healStep = 0; healStep < 3; ++healStep) {
        actorMemory.tryAdd<std::int32_t>(kHealthOffset, 5);
    }
    actorMemory.trySubtract<std::int32_t>(kHealthOffset, 1);
    actorMemory.clamp<std::int32_t>(kHealthOffset, 0, 100);

    // The guard demonstrates why a bounded loop is safer around mutable state.
    int speedSteps = 0;
    while (speed != nullptr && *speed < 2.0F && speedSteps < 16) {
        actorMemory.tryAdd<float>(kSpeedOffset, 0.25F);
        ++speedSteps;
    }

    actorMemory.setBits<std::uint32_t>(kFlagsOffset, 0x03U);
    actorMemory.clearBits<std::uint32_t>(kFlagsOffset, 0x02U);
    actorMemory.toggleBits<std::uint32_t>(kFlagsOffset, 0x04U);

    DemoFunctionTable api;
    api.tick = &tickActor;
    const auto apiMemory = offset::OwnedMemoryView::fromObject(api);
    const auto tick = apiMemory.functionAt<TickFunction>(kTickSlotOffset);
    if (tick) {
        for (int call = 0; call < 2; ++call) {
            (*tick)(&actor, 0.25F);
        }
    }

    std::int32_t samples[] = {4, 8, 12};
    auto sampleMemory = offset::OwnedMemoryView::fromArray(samples);
    sampleMemory.writeElement<std::int32_t>(0, 1, 10);

    std::int32_t numericLimits[] = {
            std::numeric_limits<std::int32_t>::max(),
            std::numeric_limits<std::int32_t>::min()
    };
    auto limitMemory = offset::OwnedMemoryView::fromArray(numericLimits);

    int checks = 0;
    int passed = 0;
    const auto check = [&](const bool condition) {
        ++checks;
        if (condition) {
            ++passed;
        }
    };
    check(actorMemory.contains(kHealthOffset, sizeof(std::int32_t)));
    check(!actorMemory.read<std::int32_t>(actorMemory.size()).has_value());
    check(actorMemory.pointerAt<std::int32_t>(1) == nullptr);
    check(actorMemory.hasAllBits<std::uint32_t>(kFlagsOffset, 0x01U));
    check(sampleMemory.readElement<std::int32_t>(0, 1).value_or(0) == 10);
    check(!sampleMemory.readElement<std::int32_t>(0, 99).has_value());
    check(!offset::OffsetMath::element(
            std::numeric_limits<std::size_t>::max(),
            2,
            8
    ).has_value());
    check(!limitMemory.tryAdd<std::int32_t>(0, 1));
    check(!limitMemory.trySubtract<std::int32_t>(sizeof(std::int32_t), 1));
    check(tick.has_value());

    const auto healthValue = actorMemory.readOr<std::int32_t>(kHealthOffset, -1);
    const auto speedValue = actorMemory.readOr<float>(kSpeedOffset, -1.0F);
    const auto tickCount = actorMemory.readOr<std::uint32_t>(kTickCountOffset, 0);
    const auto flags = actorMemory.readOr<std::uint32_t>(kFlagsOffset, 0);
    const auto shield = actorMemory.readOr<bool>(kShieldOffset, false);
    std::ostringstream output;
    output << std::fixed << std::setprecision(2)
           << "{\"actor_size\":" << sizeof(DemoActor)
           << ",\"offsets\":{\"health\":" << kHealthOffset
           << ",\"speed\":" << kSpeedOffset
           << ",\"tick_count\":" << kTickCountOffset
           << ",\"flags\":" << kFlagsOffset
           << ",\"shield\":" << kShieldOffset
           << "},\"function_table\":{\"tick_slot\":" << kTickSlotOffset
           << ",\"calls\":" << tickCount
           << "},\"result\":{\"health\":" << healthValue
           << ",\"speed\":" << speedValue
           << ",\"flags\":" << flags
           << ",\"shield\":" << (shield ? "true" : "false")
           << ",\"while_steps\":" << speedSteps
           << ",\"array_middle\":" << samples[1]
           << "},\"helper_checks\":{\"passed\":" << passed
           << ",\"total\":" << checks
           << "}}";
    return output.str();
}

}  // namespace onyx::demo
