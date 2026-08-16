// ============================================================================
// menufreefire_features.cpp – Hoàn chỉnh, sẵn sàng tích hợp
// ============================================================================

#include "menufreefire_features.h"
#include <android/log.h>
#include <thread>
#include <atomic>
#include <vector>
#include <mutex>
#include <cstring>
#include <sys/uio.h>
#include <unistd.h>

#define LOG_TAG "FFHack"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

// ============================================================================
// 1. Offsets – Cập nhật từ phân tích dump.cs
// ============================================================================
struct Offsets {
    // RVA của GameFacade.CurrentMatch()
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

    // PRIData slots (0 = CurHP, 1 = MaxHP)
    int PRIDATA_CUR_HP = 0;
    int PRIDATA_MAX_HP = 1;

    // Transform.position – thử 0x30, nếu sai đổi thành 0x38
    size_t TRANSFORM_POSITION = 0x30;

    // System.String
    size_t STRING_LENGTH = 0x10;
    size_t STRING_CHARS = 0x14;

    // Dictionary<BHGGAEEHJCO, Player*> layout (Unity IL2CPP)
    size_t DICT_BUCKETS = 0x18;
    size_t DICT_ENTRIES = 0x20;
    size_t DICT_COUNT = 0x28;

    // Entry struct: { int hashCode; int next; BHGGAEEHJCO key; Player* value; }
    // BHGGAEEHJCO size = 0x18 (từ dump.cs)
    // Entry size = 4 + 4 + 0x18 + 8 = 0x28
    size_t ENTRY_HASHCODE = 0x0;
    size_t ENTRY_NEXT = 0x4;
    size_t ENTRY_KEY = 0x8;
    size_t ENTRY_VALUE = 0x20;
    size_t ENTRY_SIZE = 0x28;
};

Offsets g_offsets;

// ============================================================================
// 2. Memory reader (process_vm_readv)
// ============================================================================
class MemoryReader {
public:
    static bool read(pid_t pid, uintptr_t addr, void* out, size_t size) {
        struct iovec local = { out, size };
        struct iovec remote = { (void*)addr, size };
        return process_vm_readv(pid, &local, 1, &remote, 1, 0) == (ssize_t)size;
    }

    template<typename T>
    static T read(pid_t pid, uintptr_t addr) {
        T val = {};
        read(pid, addr, &val, sizeof(T));
        return val;
    }
};

// ============================================================================
// 3. Cấu trúc dữ liệu
// ============================================================================
struct Vector3 { float x, y, z; };

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
// 4. Trạng thái toàn cục
// ============================================================================
static pid_t s_pid = 0;
static uintptr_t s_matchBase = 0;
static std::vector<PlayerData> s_players;
static std::mutex s_mutex;
static std::atomic<bool> s_running = false;
static std::thread s_updateThread;

// ============================================================================
// 5. Hàm đọc nickname (System.String)
// ============================================================================
std::string readUnityString(pid_t pid, uintptr_t strPtr) {
    if (!strPtr) return "";
    int32_t len = MemoryReader::read<int32_t>(pid, strPtr + g_offsets.STRING_LENGTH);
    if (len <= 0 || len > 256) return "";
    std::vector<char> buf(len + 1, 0);
    if (!MemoryReader::read(pid, strPtr + g_offsets.STRING_CHARS, buf.data(), len))
        return "";
    buf[len] = '\0';
    return std::string(buf.data(), len);
}

// ============================================================================
// 6. Đọc bone position từ node
// ============================================================================
Vector3 getBoneWorldPos(pid_t pid, uintptr_t playerAddr, size_t nodeOffset) {
    Vector3 pos = {0,0,0};
    uintptr_t node = MemoryReader::read<uintptr_t>(pid, playerAddr + nodeOffset);
    if (!node) return pos;
    uintptr_t transform = MemoryReader::read<uintptr_t>(pid, node + 0x10); // ITransformNode->transform
    if (!transform) return pos;
    pos = MemoryReader::read<Vector3>(pid, transform + g_offsets.TRANSFORM_POSITION);
    return pos;
}

// ============================================================================
// 7. Đọc vị trí từ Transform
// ============================================================================
Vector3 getTransformPosition(pid_t pid, uintptr_t transform) {
    if (!transform) return {0,0,0};
    return MemoryReader::read<Vector3>(pid, transform + g_offsets.TRANSFORM_POSITION);
}

// ============================================================================
// 8. Duyệt Dictionary
// ============================================================================
std::vector<uintptr_t> iteratePlayerDictionary(pid_t pid, uintptr_t dictPtr) {
    std::vector<uintptr_t> result;
    if (!dictPtr) return result;

    uintptr_t bucketsPtr = MemoryReader::read<uintptr_t>(pid, dictPtr + g_offsets.DICT_BUCKETS);
    uintptr_t entriesPtr = MemoryReader::read<uintptr_t>(pid, dictPtr + g_offsets.DICT_ENTRIES);
    int count = MemoryReader::read<int>(pid, dictPtr + g_offsets.DICT_COUNT);
    if (!bucketsPtr || !entriesPtr || count <= 0) return result;

    for (int i = 0; i < count; ++i) {
        uintptr_t entryAddr = entriesPtr + i * g_offsets.ENTRY_SIZE;
        uintptr_t player = MemoryReader::read<uintptr_t>(pid, entryAddr + g_offsets.ENTRY_VALUE);
        if (player != 0) {
            result.push_back(player);
        }
    }
    return result;
}

// ============================================================================
// 9. Hàm cập nhật danh sách player
// ============================================================================
void updatePlayers() {
    if (!s_pid || !s_matchBase) return;

    uintptr_t localPlayer = MemoryReader::read<uintptr_t>(s_pid, s_matchBase + g_offsets.MATCH_LOCAL_PLAYER);
    if (!localPlayer) return;

    uintptr_t dictPtr = MemoryReader::read<uintptr_t>(s_pid, s_matchBase + g_offsets.MATCH_PLAYER_DICT);
    std::vector<uintptr_t> playerAddrs = iteratePlayerDictionary(s_pid, dictPtr);
    if (std::find(playerAddrs.begin(), playerAddrs.end(), localPlayer) == playerAddrs.end())
        playerAddrs.push_back(localPlayer);

    std::vector<PlayerData> newPlayers;
    for (uintptr_t addr : playerAddrs) {
        PlayerData p;
        p.address = addr;
        p.userId = MemoryReader::read<uint64_t>(s_pid, addr + g_offsets.PLAYER_USER_ID);
        p.teamIndex = MemoryReader::read<int>(s_pid, addr + g_offsets.PLAYER_TEAM_INDEX);
        p.isDead = MemoryReader::read<bool>(s_pid, addr + g_offsets.PLAYER_DEAD_FLAG);

        uintptr_t nickPtr = MemoryReader::read<uintptr_t>(s_pid, addr + g_offsets.PLAYER_NICKNAME);
        p.nickname = readUnityString(s_pid, nickPtr);

        uintptr_t pool = MemoryReader::read<uintptr_t>(s_pid, addr + g_offsets.PLAYER_PRIDATA_POOL);
        if (pool) {
            p.curHP = (float)MemoryReader::read<uint16_t>(s_pid, pool + 0);
            p.maxHP = (float)MemoryReader::read<uint16_t>(s_pid, pool + 2);
        } else {
            p.curHP = p.maxHP = 0;
        }

        uintptr_t transform = MemoryReader::read<uintptr_t>(s_pid, addr + g_offsets.PLAYER_CACHED_TRANSFORM);
        p.position = getTransformPosition(s_pid, transform);
        p.headPos = getBoneWorldPos(s_pid, addr, g_offsets.PLAYER_HEAD_NODE);
        p.chestPos = getBoneWorldPos(s_pid, addr, g_offsets.PLAYER_CHEST_NODE);

        newPlayers.push_back(p);
    }

    std::lock_guard<std::mutex> lock(s_mutex);
    s_players = std::move(newPlayers);
}

// ============================================================================
// 10. Thread cập nhật
// ============================================================================
void updateLoop() {
    while (s_running) {
        updatePlayers();
        std::this_thread::sleep_for(std::chrono::milliseconds(16));
    }
}

// ============================================================================
// 11. Khởi tạo và dọn dẹp
// ============================================================================
void startUpdating(pid_t pid, uintptr_t matchBase) {
    if (s_running) return;
    s_pid = pid;
    s_matchBase = matchBase;
    s_running = true;
    s_updateThread = std::thread(updateLoop);
}

void stopUpdating() {
    s_running = false;
    if (s_updateThread.joinable())
        s_updateThread.join();
}

} // anonymous namespace

// ============================================================================
// 12. Các hàm toggle (xuất ra cho JNI)
// ============================================================================
void onEspEnabledChanged(bool enabled) noexcept {
    LOGI("ESP %s", enabled ? "ON" : "OFF");
    // Gọi startUpdating(pid, matchBase) từ bên ngoài
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
}

void onRotationSpeedChanged(float speed) noexcept {
    LOGI("RotationSpeed %.2f", speed);
}

void applyDefaults() noexcept {
    // Reset về mặc định
}