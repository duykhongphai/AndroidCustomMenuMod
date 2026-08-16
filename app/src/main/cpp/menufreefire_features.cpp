// ============================================================================
// menufreefire_features.cpp – Full internal hack logic with ESP
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
#include <dlfcn.h>
#include <jni.h>

#define LOG_TAG "FFHack"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace onyx::menufreefire::features {

// ============================================================================
// 1. Offsets (từ phân tích dump.cs)
// ============================================================================
struct Offsets {
    uintptr_t GAMEFACADE_CURRENT_MATCH_RVA = 0x7295AD8;

    size_t MATCH_LOCAL_PLAYER = 0xD8;
    size_t MATCH_PLAYER_DICT = 0x128;

    size_t PLAYER_CACHED_TRANSFORM = 0x58;
    size_t PLAYER_PRIDATA_POOL = 0x70;
    size_t PLAYER_DEAD_FLAG = 0x7C;
    size_t PLAYER_USER_ID = 0x390;
    size_t PLAYER_TEAM_INDEX = 0x3C8;
    size_t PLAYER_NICKNAME = 0x428;
    size_t PLAYER_HEAD_NODE = 0x638;
    size_t PLAYER_CHEST_NODE = 0x648;

    int PRIDATA_CUR_HP = 0;
    int PRIDATA_MAX_HP = 1;

    size_t TRANSFORM_POSITION = 0x30; // có thể là 0x38

    size_t STRING_LENGTH = 0x10;
    size_t STRING_CHARS = 0x14;

    size_t DICT_BUCKETS = 0x18;
    size_t DICT_ENTRIES = 0x20;
    size_t DICT_COUNT = 0x28;

    size_t ENTRY_HASHCODE = 0x0;
    size_t ENTRY_NEXT = 0x4;
    size_t ENTRY_KEY = 0x8;
    size_t ENTRY_VALUE = 0x20;
    size_t ENTRY_SIZE = 0x28;
};

Offsets g_offsets;
uintptr_t g_il2cppBase = 0;
uintptr_t g_matchBase = 0;
bool g_initialized = false;

// ============================================================================
// 2. Memory reader (internal)
// ============================================================================
static pid_t s_pid = 0;

bool readMemory(uintptr_t addr, void* out, size_t size) {
    if (s_pid == 0) s_pid = getpid();
    struct iovec local = { out, size };
    struct iovec remote = { (void*)addr, size };
    return process_vm_readv(s_pid, &local, 1, &remote, 1, 0) == (ssize_t)size;
}

template<typename T>
T readMemory(uintptr_t addr) {
    T val = {};
    readMemory(addr, &val, sizeof(T));
    return val;
}

// ============================================================================
// 3. Cấu trúc dữ liệu
// ============================================================================
struct Vector3 { float x, y, z; };
struct Vector2 { float x, y; };

struct Matrix4x4 {
    float m[4][4];
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
    Vector2 screenPos;
    Vector2 headScreenPos;
    Vector2 chestScreenPos;
    bool isVisible;
};

// ============================================================================
// 4. World-to-screen
// ============================================================================
uintptr_t g_viewMatrixAddr = 0;
uintptr_t g_projMatrixAddr = 0;
int g_screenWidth = 1080;
int g_screenHeight = 2400;

bool worldToScreen(const Vector3& worldPos, Vector2& screenPos) {
    if (!g_viewMatrixAddr || !g_projMatrixAddr) return false;

    Matrix4x4 view = readMemory<Matrix4x4>(g_viewMatrixAddr);
    Matrix4x4 proj = readMemory<Matrix4x4>(g_projMatrixAddr);

    float clipX = worldPos.x * view.m[0][0] + worldPos.y * view.m[1][0] + worldPos.z * view.m[2][0] + view.m[3][0];
    float clipY = worldPos.x * view.m[0][1] + worldPos.y * view.m[1][1] + worldPos.z * view.m[2][1] + view.m[3][1];
    float clipZ = worldPos.x * view.m[0][2] + worldPos.y * view.m[1][2] + worldPos.z * view.m[2][2] + view.m[3][2];
    float clipW = worldPos.x * view.m[0][3] + worldPos.y * view.m[1][3] + worldPos.z * view.m[2][3] + view.m[3][3];

    float ndcX = (clipX * proj.m[0][0] + clipY * proj.m[1][0] + clipZ * proj.m[2][0] + clipW * proj.m[3][0]) / clipW;
    float ndcY = (clipX * proj.m[0][1] + clipY * proj.m[1][1] + clipZ * proj.m[2][1] + clipW * proj.m[3][1]) / clipW;
    float ndcZ = (clipX * proj.m[0][2] + clipY * proj.m[1][2] + clipZ * proj.m[2][2] + clipW * proj.m[3][2]) / clipW;

    if (ndcZ < 0.001f || ndcZ > 1.0f) return false;

    screenPos.x = (g_screenWidth / 2.0f) * (ndcX + 1.0f);
    screenPos.y = (g_screenHeight / 2.0f) * (1.0f - ndcY);

    return true;
}

// ============================================================================
// 5. JNI Callback
// ============================================================================
static JavaVM* g_vm = nullptr;
static jobject g_javaObj = nullptr;
static jclass g_javaClass = nullptr;
static jmethodID g_drawEspMethod = nullptr;

void setJavaVM(JavaVM* vm) {
    g_vm = vm;
}

void registerCallback(JNIEnv* env, jobject obj) {
    if (g_javaObj) env->DeleteGlobalRef(g_javaObj);
    g_javaObj = env->NewGlobalRef(obj);
    g_javaClass = (jclass)env->NewGlobalRef(env->GetObjectClass(obj));
    g_drawEspMethod = env->GetMethodID(g_javaClass, "drawESP", "(I[F)V");
}

void setScreenSize(int width, int height) {
    g_screenWidth = width;
    g_screenHeight = height;
}

void sendPlayersToJava(const std::vector<PlayerData>& players) {
    if (!g_vm || !g_javaObj || !g_drawEspMethod) return;

    JNIEnv* env;
    g_vm->AttachCurrentThread(&env, nullptr);

    int numPlayers = players.size();
    jfloatArray arr = env->NewFloatArray(numPlayers * 9);
    jfloat* elements = env->GetFloatArrayElements(arr, nullptr);

    for (int i = 0; i < numPlayers; ++i) {
        const auto& p = players[i];
        int idx = i * 9;
        elements[idx + 0] = (float)p.teamIndex;
        elements[idx + 1] = p.curHP;
        elements[idx + 2] = p.maxHP;
        elements[idx + 3] = p.screenPos.x;
        elements[idx + 4] = p.screenPos.y;
        elements[idx + 5] = p.headScreenPos.x;
        elements[idx + 6] = p.headScreenPos.y;
        elements[idx + 7] = p.chestScreenPos.x;
        elements[idx + 8] = p.chestScreenPos.y;
    }

    env->ReleaseFloatArrayElements(arr, elements, 0);
    env->CallVoidMethod(g_javaObj, g_drawEspMethod, numPlayers, arr);
}

// ============================================================================
// 6. Đọc dữ liệu game
// ============================================================================
std::string readUnityString(uintptr_t strPtr) {
    if (!strPtr) return "";
    int32_t len = readMemory<int32_t>(strPtr + g_offsets.STRING_LENGTH);
    if (len <= 0 || len > 256) return "";
    std::vector<char> buf(len + 1, 0);
    if (!readMemory(strPtr + g_offsets.STRING_CHARS, buf.data(), len)) return "";
    buf[len] = '\0';
    return std::string(buf.data(), len);
}

Vector3 getBoneWorldPos(uintptr_t playerAddr, size_t nodeOffset) {
    Vector3 pos = {0,0,0};
    uintptr_t node = readMemory<uintptr_t>(playerAddr + nodeOffset);
    if (!node) return pos;
    uintptr_t transform = readMemory<uintptr_t>(node + 0x10);
    if (!transform) return pos;
    pos = readMemory<Vector3>(transform + g_offsets.TRANSFORM_POSITION);
    return pos;
}

Vector3 getTransformPosition(uintptr_t transform) {
    if (!transform) return {0,0,0};
    return readMemory<Vector3>(transform + g_offsets.TRANSFORM_POSITION);
}

std::vector<uintptr_t> iteratePlayerDictionary(uintptr_t dictPtr) {
    std::vector<uintptr_t> result;
    if (!dictPtr) return result;

    uintptr_t bucketsPtr = readMemory<uintptr_t>(dictPtr + g_offsets.DICT_BUCKETS);
    uintptr_t entriesPtr = readMemory<uintptr_t>(dictPtr + g_offsets.DICT_ENTRIES);
    int count = readMemory<int>(dictPtr + g_offsets.DICT_COUNT);
    if (!bucketsPtr || !entriesPtr || count <= 0) return result;

    for (int i = 0; i < count; ++i) {
        uintptr_t entryAddr = entriesPtr + i * g_offsets.ENTRY_SIZE;
        uintptr_t player = readMemory<uintptr_t>(entryAddr + g_offsets.ENTRY_VALUE);
        if (player != 0) {
            result.push_back(player);
        }
    }
    return result;
}

// ============================================================================
// 7. Cập nhật danh sách player
// ============================================================================
static std::vector<PlayerData> s_players;
static std::mutex s_mutex;
static std::atomic<bool> s_running = false;
static std::thread s_updateThread;

void updatePlayers() {
    if (!g_matchBase) return;

    uintptr_t localPlayer = readMemory<uintptr_t>(g_matchBase + g_offsets.MATCH_LOCAL_PLAYER);
    if (!localPlayer) return;

    uintptr_t dictPtr = readMemory<uintptr_t>(g_matchBase + g_offsets.MATCH_PLAYER_DICT);
    std::vector<uintptr_t> playerAddrs = iteratePlayerDictionary(dictPtr);
    if (std::find(playerAddrs.begin(), playerAddrs.end(), localPlayer) == playerAddrs.end())
        playerAddrs.push_back(localPlayer);

    std::vector<PlayerData> newPlayers;
    for (uintptr_t addr : playerAddrs) {
        PlayerData p;
        p.address = addr;
        p.userId = readMemory<uint64_t>(addr + g_offsets.PLAYER_USER_ID);
        p.teamIndex = readMemory<int>(addr + g_offsets.PLAYER_TEAM_INDEX);
        p.isDead = readMemory<bool>(addr + g_offsets.PLAYER_DEAD_FLAG);

        uintptr_t nickPtr = readMemory<uintptr_t>(addr + g_offsets.PLAYER_NICKNAME);
        p.nickname = readUnityString(nickPtr);

        uintptr_t pool = readMemory<uintptr_t>(addr + g_offsets.PLAYER_PRIDATA_POOL);
        if (pool) {
            p.curHP = (float)readMemory<uint16_t>(pool + 0);
            p.maxHP = (float)readMemory<uint16_t>(pool + 2);
        } else {
            p.curHP = p.maxHP = 0;
        }

        uintptr_t transform = readMemory<uintptr_t>(addr + g_offsets.PLAYER_CACHED_TRANSFORM);
        p.position = getTransformPosition(transform);
        p.headPos = getBoneWorldPos(addr, g_offsets.PLAYER_HEAD_NODE);
        p.chestPos = getBoneWorldPos(addr, g_offsets.PLAYER_CHEST_NODE);

        p.isVisible = worldToScreen(p.position, p.screenPos);
        if (p.isVisible) {
            worldToScreen(p.headPos, p.headScreenPos);
            worldToScreen(p.chestPos, p.chestScreenPos);
        }

        newPlayers.push_back(p);
    }

    std::lock_guard<std::mutex> lock(s_mutex);
    s_players = std::move(newPlayers);
    sendPlayersToJava(s_players);
}

void updateLoop() {
    while (s_running) {
        updatePlayers();
        std::this_thread::sleep_for(std::chrono::milliseconds(16));
    }
}

// ============================================================================
// 8. Khởi tạo
// ============================================================================
void initializeHack(uintptr_t il2cppBase) {
    if (g_initialized) return;
    g_il2cppBase = il2cppBase;
    g_initialized = true;
    LOGI("Hack initialized, il2cpp base: 0x%lx", il2cppBase);
    // TODO: tìm match base và view/proj matrix
}

void setMatchBase(uintptr_t matchBase) {
    g_matchBase = matchBase;
    LOGI("Match base set to 0x%lx", matchBase);
}

void startUpdating() {
    if (s_running) return;
    if (!g_matchBase) {
        LOGE("Cannot start updating: match base not set");
        return;
    }
    s_running = true;
    s_updateThread = std::thread(updateLoop);
}

void stopUpdating() {
    s_running = false;
    if (s_updateThread.joinable())
        s_updateThread.join();
}

// ============================================================================
// 9. Các hàm toggle
// ============================================================================
void onEspEnabledChanged(bool enabled) noexcept {
    LOGI("ESP %s", enabled ? "ON" : "OFF");
    if (enabled) {
        startUpdating();
    } else {
        stopUpdating();
    }
}

void onTracerLineChanged(bool enabled) noexcept { LOGI("TracerLine %s", enabled ? "ON" : "OFF"); }
void onEspBoxChanged(bool enabled) noexcept { LOGI("EspBox %s", enabled ? "ON" : "OFF"); }
void onHealthBarChanged(bool enabled) noexcept { LOGI("HealthBar %s", enabled ? "ON" : "OFF"); }
void onAimbotEnabledChanged(bool enabled) noexcept { LOGI("Aimbot %s", enabled ? "ON" : "OFF"); }
void onSkipKnockChanged(bool enabled) noexcept { LOGI("SkipKnock %s", enabled ? "ON" : "OFF"); }
void onSilentAimChanged(bool enabled) noexcept { LOGI("SilentAim %s", enabled ? "ON" : "OFF"); }
void onLegitAimChanged(bool enabled) noexcept { LOGI("LegitAim %s", enabled ? "ON" : "OFF"); }
void onDragAimAssistChanged(bool enabled) noexcept { LOGI("DragAimAssist %s", enabled ? "ON" : "OFF"); }
void onRotationEnabledChanged(bool enabled) noexcept { LOGI("Rotation %s", enabled ? "ON" : "OFF"); }
void onBypassEmulatorDetectChanged(bool enabled) noexcept { LOGI("BypassEmulator %s", enabled ? "ON" : "OFF"); }
void onRotationSpeedChanged(float speed) noexcept { LOGI("RotationSpeed %.2f", speed); }
void applyDefaults() noexcept {}

} // namespace onyx::menufreefire::features