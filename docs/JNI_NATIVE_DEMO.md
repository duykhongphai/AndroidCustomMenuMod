# Demo Java → JNI → C++ → native state

Demo trong module `app` cho thấy một menu Java có thể gọi C++ mà không hook,
inject hoặc truy cập bộ nhớ của ứng dụng khác.

## Luồng sự kiện

```text
ModernToggle / ModernSlider
        │ featureId + value
        ▼
DemoFeatureBridge.java
        │ NativeDemoRuntime.set...
        ▼
JNI method trong jni_bridge.cpp
        │
        ▼
NativeState C++ (process-local, có mutex)
        │
        ├── snapshot JSON trả về Java
        └── log với tag OnyxNativeDemo
```

`System.loadLibrary("onyx_demo_native")` nạp `libonyx_demo_native.so` được đóng
gói trong APK. JNI chuyển `String`, `boolean` và `float` của Java thành dữ liệu
C++, sau đó `NativeState` lưu chúng trong các map. `snapshot()` đi ngược lại
qua JNI và hiển thị JSON trên màn hình app preview.

Các file chính:

- `native-demo-bridge/src/main/java/com/nguyen/onyxmenu/nativebridge/NativeDemoRuntime.java`:
  Java API và khai báo native methods.
- `app/src/main/java/com/nguyen/onyxmenu/demo/profile/DemoFeatureBridge.java`:
  adapter từ UI engine sang Java API của JNI.
- `native-demo-bridge/src/main/cpp/jni_bridge.cpp`: chuyển đổi kiểu dữ liệu JNI.
- `native-demo-bridge/src/main/cpp/native_state.h` và `native_state.cpp`: state C++ giả lập.
- `native-demo-bridge/src/main/cpp/owned_offset_lab.cpp`: field offset, vòng lặp và function table an toàn.
- `native-demo-bridge/src/main/cpp/Android.mk`: build shared library bằng Android NDK.

## Chạy thử

1. Build và cài module `app`.
2. Bật/tắt `Ambient interface` hoặc kéo `Interface intensity` trong preview.
3. Quan sát JSON trong card `JNI → C++ → NATIVE STATE`.
4. Mở overlay; các control trong profile cũng đi qua cùng bridge.
5. Bấm `PING DEMO BRIDGE` để hiện snapshot hoặc `CLEAR DEMO STATE` để reset.
6. Bấm `RUN OWNED OFFSET LAB` để chạy ví dụ `base + offset` trên C++ object của demo.

Phần giải thích chi tiết nằm trong
[Hiểu offset trong C++ bằng object do app sở hữu](CPP_OFFSETS_OWNED_LAB.md).

Có thể lọc log native bằng:

```powershell
adb logcat -s OnyxNativeDemo OnyxDemoBridge
```

Native state tồn tại cho tới khi process bị kết thúc hoặc được reset. Nó khác
với `SharedPreferences`: state demo không tự tồn tại sau khi Android tạo process
mới.

## Có dùng cho nhiều app được không?

Có, nếu các app đó tích hợp SDK/source này một cách hợp lệ. Nên giữ ba lớp:

```text
onyx-core                 UI và FeatureBridge dùng chung
host-app bridge           ánh xạ feature ID của từng app
host-app native library   C++ state/logic thuộc chính app đó
```

UI engine không cần biết C++, tên thư viện hoặc cách host xử lý feature. Mỗi app
có thể cung cấp một `MenuProvider` và một `FeatureBridge` khác nhau. Nếu nhiều app
dùng đúng cùng native API, phần Java + `.so` có thể được đóng thành một Android
Library (AAR) dùng chung; cần đóng gói đủ ABI mà các app hỗ trợ.

Giới hạn quan trọng: Android cô lập process. Thư viện JNI do app A nạp chỉ truy
cập state trong process A. Một overlay chạy ở app A không thể dùng bridge này để
đổi native state của app B. Để app B sử dụng bridge, app B phải chủ động tích hợp
và nạp thư viện trong process của chính nó.

## Đổi feature cho app sở hữu

1. Khai báo control và ID trong `MenuProvider` của app.
2. Ánh xạ ID trong `FeatureBridge` của app.
3. Mở rộng Java API và JNI contract nếu cần kiểu dữ liệu mới.
4. Cài đặt logic C++ chỉ trên state/API thuộc app đó.
5. Giữ class JNI khi bật R8, giống rule trong `app/proguard-rules.pro`.

Không đặt secret hoặc quyết định bảo mật quan trọng trong native state. C++ chỉ
làm reverse engineering khó hơn; dữ liệu có giá trị vẫn cần được backend xác
thực.
