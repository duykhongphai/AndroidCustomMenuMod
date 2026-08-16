#include "menufreefire_features.h"

#include <android/log.h>
#include <dlfcn.h>
#include <link.h>
#include <sys/uio.h>
#include <unistd.h>
#include <thread>
#include <atomic>
#include <mutex>
#include <cstring>
#include <algorithm>
#include <cmath>

#define LOG_TAG "OnyxMenuFreeFire"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace onyx::menufreefire::features {

// ============================================================================
// 1. Offsets – Từ phân tích dump.cs và Transform
// ============================================================================
struct Offsets {
    // GameFacade (có thể tìm bằng pattern)
    uintptr_t GAMEFACADE_CURRENT_MATCH_RVA = 0x7295AD8;

    // Match registry (EMKJHAJNPDH)
    size_t MATCH_LOCAL_PLAYER = 0xD8;
    size_t MATCH_PLAYER_DICT = 0x128;

    // Player
    size_t PLAYER_CACHED_TRANSFORM = 0x58;
    size_t PLAYER_PRIDATA_POOL = 0x70;
    size_t PLAYER_DEAD_FLAG = 0x7C;
    size_t PLAYER_USER_ID = 0x390;
    size_t PLAYER_TEAM_INDEX = 0x3C8;
    size_t PLAYER_NICKNAME = 0x428;
    size_t PLAYER_HEAD_NODE = 0x638;
    size_t PLAYER_CHEST_NODE = 0x648;

    // PRIData slots (ushort)
    int PRIDATA_CUR_HP = 0;
    int PRIDATA_MAX_HP = 1;

    // Transform.position (Unity)
    size_t TRANSFORM_POSITION = 0x30; // thử 0x30, nếu sai đổi 0x38

    // System.String
    size_t STRING_LENGTH = 0x10;
    size_t STRING_CHARS = 0x14;

    // Dictionary<BHGGAEEHJCO, Player*>
    size_t DICT_BUCKETS = 0x18;
    size_t DICT_ENTRIES = 0x20;
    size_t DICT_COUNT = 0x28;

    // Entry struct (size = 0x28)
    size_t ENTRY_HASHCODE = 0x0;
    size_t ENTRY_NEXT = 0x4;
    size_t ENTRY_KEY = 0x8;     // BHGGAEEHJCO (0x18 bytes)
    size_t ENTRY_VALUE = 0x20;  // Player*
    size_t ENTRY_SIZE = 0x28;
};

static Offsets g_offsets;

// ============================================================================
// 2. Memory reader – đọc từ tiến trình hiện tại (injected)
// ============================================================================
class MemoryReader {
public:
    static bool read(uintptr_t addr, void* out, size_t size) {
        // Vì đã inject vào tiến trình game, có thể đọc trực tiếp bằng memcpy
        // nhưng để an toàn, dùng process_vm_readv với pid của chính mình
        pid_t pid = getpid();
        struct iovec local = { out, size };
        struct iovec remote = { (void*)addr, size };
        return process_vm_readv(pid, &local, 1, &remote, 1, 0) == (ssize_t)size;
    }

    template<typename T>
    static T read(uintptr_t addr) {
        T val = {};
        read(addr, &val, sizeof(T));
        return val;
    }

    template<typename T>
    static bool write(uintptr_t addr, const T& value) {
        pid_t pid = getpid();
        struct iovec local = { (void*)&value, sizeof(T) };
        struct iovec remote = { (void*)addr, sizeof(T) };
        return process_vm_writev(pid, &local, 1, &remote, 1, 0) == (ssize_t)sizeof(T);
    }
};

// ============================================================================
// 3. Cấu trúc dữ liệu player (mở rộng)
// ============================================================================
struct PlayerDataInternal {
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
// 4. Trạng thái hack toàn cục
// ============================================================================
static uintptr_t s_matchBase = 0;
static uintptr_t s_localPlayer = 0;
static std::vector<PlayerDataInternal> s_players;
static std::mutex s_mutex;
static std::atomic<bool> s_running = false;
static std::thread s_updateThread;
static std::atomic<bool> s_espEnabled = false;
static std::atomic<bool> s_aimbotEnabled = false;
static std::atomic<float> s_rotationSpeed = 5.0f;

// ============================================================================
// 5. Hàm đọc nickname
// ============================================================================
std::string readUnityString(uintptr_t strPtr) {
    if (!strPtr) return "";
    int32_t len = MemoryReader::read<int32_t>(strPtr + g_offsets.STRING_LENGTH);
    if (len <= 0 || len > 256) return "";
    std::vector<char> buf(len + 1, 0);
    if (!MemoryReader::read(strPtr + g_offsets.STRING_CHARS, buf.data(), len))
        return "";
    buf[len] = '\0';
    return std::string(buf.data(), len);
}

// ============================================================================
// 6. Đọc bone position từ node
// ============================================================================
Vector3 getBoneWorldPos(uintptr_t playerAddr, size_t nodeOffset) {
    Vector3 pos = {0,0,0};
    uintptr_t node = MemoryReader::read<uintptr_t>(playerAddr + nodeOffset);
    if (!node) return pos;
    uintptr_t transform = MemoryReader::read<uintptr_t>(node + 0x10); // ITransformNode->transform
    if (!transform) return pos;
    pos = MemoryReader::read<Vector3>(transform + g_offsets.TRANSFORM_POSITION);
    return pos;
}

// ============================================================================
// 7. Đọc vị trí từ Transform
// ============================================================================
Vector3 getTransformPosition(uintptr_t transform) {
    if (!transform) return {0,0,0};
    return MemoryReader::read<Vector3>(transform + g_offsets.TRANSFORM_POSITION);
}

// ============================================================================
// 8. Duyệt Dictionary player
// ============================================================================
std::vector<uintptr_t> iteratePlayerDictionary(uintptr_t dictPtr) {
    std::vector<uintptr_t> result;
    if (!dictPtr) return result;

    uintptr_t bucketsPtr = MemoryReader::read<uintptr_t>(dictPtr + g_offsets.DICT_BUCKETS);
    uintptr_t entriesPtr = MemoryReader::read<uintptr_t>(dictPtr + g_offsets.DICT_ENTRIES);
    int count = MemoryReader::read<int>(dictPtr + g_offsets.DICT_COUNT);
    if (!bucketsPtr || !entriesPtr || count <= 0) return result;

    for (int i = 0; i < count; ++i) {
        uintptr_t entryAddr = entriesPtr + i * g_offsets.ENTRY_SIZE;
        uintptr_t player = MemoryReader::read<uintptr_t>(entryAddr + g_offsets.ENTRY_VALUE);
        if (player != 0) {
            result.push_back(player);
        }
    }
    return result;
}

// ============================================================================
// 9. Cập nhật danh sách player (chạy trong thread)
// ============================================================================
void updatePlayers() {
    if (!s_matchBase) return;

    // Đọc local player
    s_localPlayer = MemoryReader::read<uintptr_t>(s_matchBase + g_offsets.MATCH_LOCAL_PLAYER);
    if (!s_localPlayer) return;

    // Đọc dictionary
    uintptr_t dictPtr = MemoryReader::read<uintptr_t>(s_matchBase + g_offsets.MATCH_PLAYER_DICT);
    std::vector<uintptr_t> playerAddrs = iteratePlayerDictionary(dictPtr);
    if (std::find(playerAddrs.begin(), playerAddrs.end(), s_localPlayer) == playerAddrs.end())
        playerAddrs.push_back(s_localPlayer);

    std::vector<PlayerDataInternal> newPlayers;
    for (uintptr_t addr : playerAddrs) {
        PlayerDataInternal p;
        p.address = addr;
        p.userId = MemoryReader::read<uint64_t>(addr + g_offsets.PLAYER_USER_ID);
        p.teamIndex = MemoryReader::read<int>(addr + g_offsets.PLAYER_TEAM_INDEX);
        p.isDead = MemoryReader::read<bool>(addr + g_offsets.PLAYER_DEAD_FLAG);

        uintptr_t nickPtr = MemoryReader::read<uintptr_t>(addr + g_offsets.PLAYER_NICKNAME);
        p.nickname = readUnityString(nickPtr);

        uintptr_t pool = MemoryReader::read<uintptr_t>(addr + g_offsets.PLAYER_PRIDATA_POOL);
        if (pool) {
            p.curHP = (float)MemoryReader::read<uint16_t>(pool + 0);
            p.maxHP = (float)MemoryReader::read<uint16_t>(pool + 2);
        } else {
            p.curHP = p.maxHP = 0;
        }

        uintptr_t transform = MemoryReader::read<uintptr_t>(addr + g_offsets.PLAYER_CACHED_TRANSFORM);
        p.position = getTransformPosition(transform);
        p.headPos = getBoneWorldPos(addr, g_offsets.PLAYER_HEAD_NODE);
        p.chestPos = getBoneWorldPos(addr, g_offsets.PLAYER_CHEST_NODE);

        newPlayers.push_back(p);
    }

    std::lock_guard<std::mutex> lock(s_mutex);
    s_players = std::move(newPlayers);
}

// ============================================================================
// 10. Vòng lặp cập nhật (chạy liên tục)
// ============================================================================
void updateLoop() {
    while (s_running) {
        if (s_espEnabled.load() || s_aimbotEnabled.load()) {
            updatePlayers();
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(16));
    }
}

// ============================================================================
// 11. Khởi tạo hack
// ============================================================================
void initializeHack(uintptr_t il2cppBase) {
    LOGI("Initializing Free Fire hack...");
    // Tìm match base bằng cách gọi CurrentMatch() nếu có thể
    // Hoặc scan pattern để tìm instance GameFacade
    // Ở đây giả định bạn đã có matchBase từ bên ngoài
    // Nếu chưa có, bạn cần scan pattern hoặc dùng offset static
    // Vì không có static field, ta để s_matchBase = 0 và sẽ tìm sau
    LOGI("Hack initialized with il2cpp base: 0x%lx", il2cppBase);
    s_running = true;
    s_updateThread = std::thread(updateLoop);
}

// ============================================================================
// 12. Lấy danh sách player cho UI
// ============================================================================
std::vector<PlayerData> getPlayers() {
    std::lock_guard<std::mutex> lock(s_mutex);
    std::vector<PlayerData> result;
    result.reserve(s_players.size());
    for (const auto& p : s_players) {
        result.push_back({
            p.address,
            p.userId,
            p.teamIndex,
            p.nickname,
            p.isDead,
            p.curHP,
            p.maxHP,
            p.position,
            p.headPos,
            p.chestPos
        });
    }
    return result;
}

// ============================================================================
// 13. Các hàm toggle (gọi từ JNI)
// ============================================================================
void onEspEnabledChanged(bool enabled) noexcept {
    s_espEnabled = enabled;
    LOGI("ESP %s", enabled ? "ON" : "OFF");
}

void onTracerLineChanged(bool enabled) noexcept {
    LOGI("TracerLine %s", enabled ? "ON" : "OFF");
}

void onEspBoxChanged(bool enabled) noexcept {
    LOGI("EspBox %s", enabled ? "ON" : "OFF");
}

void onHealthBarChanged(bool enabled) noexcept {
    LOGI("HealthBar %s", enabled ? "ON" : "OFF");
}

void onAimbotEnabledChanged(bool enabled) noexcept {
    s_aimbotEnabled = enabled;
    LOGI("Aimbot %s", enabled ? "ON" : "OFF");
}

void onSkipKnockChanged(bool enabled) noexcept {
    LOGI("SkipKnock %s", enabled ? "ON" : "OFF");
}

void onSilentAimChanged(bool enabled) noexcept {
    LOGI("SilentAim %s", enabled ? "ON" : "OFF");
}

void onLegitAimChanged(bool enabled) noexcept {
    LOGI("LegitAim %s", enabled ? "ON" : "OFF");
}

void onDragAimAssistChanged(bool enabled) noexcept {
    LOGI("DragAimAssist %s", enabled ? "ON" : "OFF");
}

void onRotationEnabledChanged(bool enabled) noexcept {
    LOGI("Rotation %s", enabled ? "ON" : "OFF");
}

void onBypassEmulatorDetectChanged(bool enabled) noexcept {
    LOGI("BypassEmulator %s", enabled ? "ON" : "OFF");
    // TODO: Hook các hàm kiểm tra emulator
}

void onRotationSpeedChanged(float speed) noexcept {
    s_rotationSpeed = speed;
    LOGI("RotationSpeed %.2f", speed);
}

void applyDefaults() noexcept {
    onEspEnabledChanged(false);
    onTracerLineChanged(false);
    onEspBoxChanged(false);
    onHealthBarChanged(false);
    onAimbotEnabledChanged(false);
    onSkipKnockChanged(false);
    onSilentAimChanged(false);
    onLegitAimChanged(false);
    onDragAimAssistChanged(false);
    onRotationEnabledChanged(false);
    onBypassEmulatorDetectChanged(false);
    onRotationSpeedChanged(5.0f);
}

} // namespace onyx::menufreefire::features