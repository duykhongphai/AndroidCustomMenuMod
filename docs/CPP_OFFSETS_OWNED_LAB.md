# Hiểu offset trong C++ bằng object do app sở hữu

Lab này chỉ thao tác trên `DemoActor` và `DemoFunctionTable` được tạo trong
chính native library của app preview. Nó không tìm module, scan process, hook
hoặc truy cập ứng dụng khác.

Code chạy được nằm tại:

- `app/src/main/cpp/owned_offset_helpers.h`
- `app/src/main/cpp/owned_offset_lab.cpp`
- `app/src/main/cpp/owned_offset_lab.h`
- JNI entry `nativeRunOwnedOffsetLab` trong `app/src/main/cpp/jni_bridge.cpp`

## Bộ helper có sẵn

`owned_offset_helpers.h` là header-only và cung cấp hai nhóm API:

| Nhóm | Helper | Mục đích |
|---|---|---|
| Offset math | `OffsetMath::add` | Cộng hai offset, từ chối `size_t` overflow |
| Offset math | `OffsetMath::multiply` | Tính stride, từ chối overflow |
| Offset math | `OffsetMath::element` | Tính `first + index * stride + field` |
| View | `fromObject`, `fromArray` | Tạo view có base và kích thước rõ ràng |
| Validation | `size`, `empty`, `contains`, `isAligned` | Kiểm tra vùng và alignment |
| Field | `pointerAt`, `read`, `readOr`, `write`, `update` | Truy cập field có bounds/alignment check |
| Number | `tryAdd`, `trySubtract`, `clamp` | Toán số học có kiểm tra overflow/range |
| Boolean | `toggleBool` | Đảo field `bool` |
| Bit flags | `setBits`, `clearBits`, `toggleBits`, `hasAllBits` | Xử lý mask trên field số nguyên |
| Array | `elementAt`, `readElement`, `writeElement` | Truy cập phần tử bằng index/stride |
| Function table | `functionAt` | Đọc function pointer không-null từ slot tin cậy |

Ví dụ sử dụng:

```cpp
auto memory = OwnedMemoryView::fromObject(actor);

auto health = memory.read<std::int32_t>(healthOffset);
memory.write<std::int32_t>(healthOffset, 75);
memory.tryAdd<std::int32_t>(healthOffset, 5);
memory.clamp<std::int32_t>(healthOffset, 0, 100);
memory.toggleBool(shieldOffset);
```

Helper kiểm tra được bounds, alignment và một số overflow; nó không thể tự xác
minh rằng một offset thực sự thuộc đúng field type. Contract đó vẫn phải đến từ
struct/header của app. API cố ý không có module lookup, arbitrary raw address,
pointer-chain traversal, memory protection change hoặc code patching.

## Offset của field là gì?

Offset chỉ là khoảng cách tính bằng byte từ đầu object tới một field:

```text
field address = object base address + field offset
```

Ví dụ object thuộc source của mình:

```cpp
struct DemoActor {
    std::int32_t health;
    float speed;
};

constexpr std::size_t healthOffset = offsetof(DemoActor, health);
DemoActor actor{100, 1.0F};

auto* base = reinterpret_cast<std::byte*>(&actor);
auto* health = reinterpret_cast<std::int32_t*>(base + healthOffset);
std::int32_t value = *health;  // đọc
*health = 75;                  // ghi
```

Cần đồng thời biết đúng bốn thứ:

1. Base address của một object còn sống và hợp lệ.
2. Offset đúng với chính layout/phiên bản đó.
3. Kiểu field chính xác, ví dụ `int32_t`, `float` hay pointer.
4. Alignment và lifetime của object.

Chỉ có con số offset mà không có base và type thì chưa đọc được gì. Dùng sai
type/offset có thể đọc padding, làm hỏng object hoặc gây undefined behavior.
`offsetof` chỉ nên dùng với standard-layout type; lab có `static_assert` để kiểm
tra điều này.

## Dùng if, for và while

Sau khi có reference/pointer hợp lệ, nó là một biến C++ bình thường:

```cpp
auto memory = OwnedMemoryView::fromObject(actor);
auto* health = memory.pointerAt<std::int32_t>(healthOffset);

if (health != nullptr && *health < 50) {
    memory.tryAdd<std::int32_t>(healthOffset, 10);
}

for (int step = 0; step < 3; ++step) {
    memory.tryAdd<std::int32_t>(healthOffset, 5);
}

int guard = 0;
while (health != nullptr && *health < 100 && guard < 20) {
    memory.tryAdd<std::int32_t>(healthOffset, 1);
    ++guard;
}
```

Offset không thay đổi cú pháp của vòng lặp. Nó chỉ là cách lấy ra pointer hoặc
reference trước khi chạy logic. `while` nên có giới hạn nếu giá trị có thể bị
thay đổi bởi thread khác.

Nếu có nhiều object do app sở hữu, nên duyệt container hợp lệ của app:

```cpp
for (DemoActor& actor : actors) {
    auto memory = OwnedMemoryView::fromObject(actor);
    auto* health = memory.pointerAt<std::int32_t>(healthOffset);
    if (health != nullptr) {
        // xử lý *health
    }
}
```

Đừng duyệt các địa chỉ số ngẫu nhiên để thử tìm object; đó không phải cách quản
lý object an toàn.

## Offset của field khác địa chỉ hàm

Có ba khái niệm thường bị gọi chung là “offset”:

- Field offset: khoảng cách từ đầu object đến field.
- Function-table offset: khoảng cách từ đầu API table đến một slot chứa function
  pointer.
- Code RVA: khoảng cách từ base của native module đến machine code của hàm.

Lab minh họa function-table vì đây là contract ổn định cho plugin/app do mình sở
hữu:

```cpp
using TickFunction = void (*)(DemoActor*, float);

struct DemoFunctionTable {
    std::uint32_t version;
    TickFunction tick;
};

constexpr std::size_t tickOffset = offsetof(DemoFunctionTable, tick);
DemoFunctionTable api{1, &tickActor};

auto table = OwnedMemoryView::fromObject(api);
auto tick = table.functionAt<TickFunction>(tickOffset);
if (tick) {
    (*tick)(&actor, 0.25F);
}
```

Với code RVA, chỉ có offset vẫn chưa đủ để gọi. Cần đúng native module, đúng
binary version, đúng ABI, kiểu trả về, toàn bộ tham số và calling convention.
Gọi một địa chỉ với signature sai là undefined behavior và thường crash. Với
phần mềm mình sở hữu, exported function, JNI method, callback hoặc versioned
function table đáng tin cậy hơn hard-code code RVA.

## “Thêm code vào hàm” nghĩa là gì?

Offset không biến machine code đã build trở lại thành nơi có thể chèn `if` hay
`for` như source C++. Khi sở hữu source, dùng wrapper hoặc callback:

```cpp
void tickWithChecks(DemoActor* actor, float deltaSeconds) {
    // before
    if (actor == nullptr) {
        return;
    }

    tickActor(actor, deltaSeconds);

    // after
    actor->health = std::max(actor->health, 0);
}

api.tick = &tickWithChecks;
```

Đây là cùng ý tưởng “chạy trước/sau một hàm”, nhưng có type safety, dễ debug và
không phụ thuộc địa chỉ machine code.

## Chạy lab

Trong app preview, bấm `RUN SAFE C++ OFFSET LAB`, hoặc mở overlay và chọn
`RUN OWNED OFFSET LAB`. C++ trả về báo cáo tương tự:

```json
{
  "actor_size": 20,
  "offsets": {"health": 0, "speed": 4, "tick_count": 8, "flags": 12, "shield": 16},
  "function_table": {"tick_slot": 8, "calls": 2},
  "result": {"health": 46, "speed": 2.50, "flags": 5, "shield": true, "while_steps": 4, "array_middle": 10},
  "helper_checks": {"passed": 10, "total": 10}
}
```

Con số padding/offset có thể khác nếu struct, compiler hoặc ABI thay đổi. Vì thế
lab tự tính bằng `offsetof` thay vì chép một offset hard-code từ binary khác.
