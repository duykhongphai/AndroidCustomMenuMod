# Onyx Android Menu Engine

Onyx là UI engine menu nổi dành cho Android. Phần giao diện, vòng đời overlay và renderer nằm trong module `onyx-core`; mỗi ứng dụng chỉ cần cung cấp một `MenuProvider` để khai báo menu và một `FeatureBridge` để xử lý sự kiện.

Giao diện chạy hoàn toàn bằng Android Views và Canvas, không phụ thuộc Compose, Material Components, mạng, quảng cáo hoặc thư viện UI bên thứ ba.

> Chỉ sử dụng dự án với ứng dụng bạn sở hữu hoặc được cho phép kiểm thử. Dự án không chứa hook, sửa bộ nhớ, bypass anti-cheat hoặc logic gian lận trong trò chơi.

## Tính năng

- Bubble nổi có thể kéo và ghi nhớ vị trí.
- Control center có tab, section, hero, metric và footer động.
- Bốn loại control: toggle, slider, action và palette màu.
- Toggle, slider, logo và nền động được vẽ bằng Canvas.
- Cấu hình được lưu riêng theo `profileId`.
- Nạp `MenuProvider` từ manifest và tự dùng profile dự phòng nếu cấu hình sai.
- Có consumer rules cho R8 để giữ nguyên provider được gọi bằng reflection.
- Có payload riêng, không phụ thuộc resource ID, dành cho kiểm thử bằng Apktool.
- Header chỉ có nút `—` để thu gọn menu về bubble; không có nút `×`.

## Cấu trúc repository

```text
AndroidCustomMenuMod/
├── onyx-core/                         # UI engine tái sử dụng
│   └── src/main/java/com/nguyen/onyxmenu/
│       ├── bridge/FeatureBridge.java
│       ├── engine/                    # Tìm và nạp MenuProvider
│       ├── model/                     # MenuProfile, tab, section, control
│       ├── overlay/                   # Service, WindowManager, renderer
│       ├── storage/                   # SharedPreferences theo profile
│       └── ui/                        # Design token và custom View
├── app/                               # Ứng dụng preview
│   └── src/main/java/com/nguyen/onyxmenu/demo/profile/
│       ├── DemoMenuProvider.java      # Khai báo menu demo
│       └── DemoFeatureBridge.java     # Nhận sự kiện demo
├── apktool-payload/                   # Payload độc lập, không dùng host R
│   └── src/main/java/com/nguyen/onyxpayload/
│       ├── OnyxBootstrap.java
│       ├── OnyxPermissionActivity.java
│       ├── StandaloneMenuProvider.java
│       └── StandaloneFeatureBridge.java
└── docs/
    ├── ARCHITECTURE.md
    ├── CUSTOMIZATION.md
    ├── APKTOOL_COMPATIBILITY.md
    ├── HUONG_DAN_APKTOOL.md
    └── APKTOOL_INJECTION_REPORT.md
```

## Thêm một nút mới

Thêm control vào [DemoMenuProvider.java](app/src/main/java/com/nguyen/onyxmenu/demo/profile/DemoMenuProvider.java):

```java
.control(MenuControl.toggle(
        "show_grid",
        "Lưới căn chỉnh",
        "Hiện đường căn chỉnh bố cục",
        false
))
```

Xử lý cùng ID trong bridge:

```java
@Override
public void onToggleChanged(String featureId, boolean enabled) {
    if ("show_grid".equals(featureId)) {
        layoutDebugger.setGridVisible(enabled);
    }
}
```

Không cần sửa `ModernMenuView`, `MenuOverlayController` hoặc bất kỳ custom View nào.

Đọc [hướng dẫn tùy chỉnh menu](docs/CUSTOMIZATION.md) để xem đầy đủ toggle, slider, action, palette, tab, section, theme và nhiều profile.

## Đăng ký profile

Ứng dụng host khai báo provider trong `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.nguyen.onyxmenu.MENU_PROVIDER"
    android:value="com.example.myapp.MyMenuProvider" />
```

Provider phải:

- Có constructor công khai không tham số.
- Implement `MenuProvider`.
- Trả về `MenuProfile` và `FeatureBridge` khác `null`.
- Có tên class khớp chính xác với manifest.

Nếu provider bị thiếu hoặc tạo profile lỗi, engine hiển thị profile dự phòng thay vì làm ứng dụng crash.

## Build

Mở repository bằng Android Studio hoặc chạy PowerShell:

```powershell
$env:JAVA_HOME = "C:\duong-dan\toi\jdk-21"
./gradlew.bat testDebugUnitTest assembleDebug lintDebug
./gradlew.bat assembleRelease
```

File đầu ra:

- APK demo: `app/build/outputs/apk/debug/app-debug.apk`
- AAR engine: `onyx-core/build/outputs/aar/onyx-core-release.aar`
- AAR payload: `apktool-payload/build/outputs/aar/apktool-payload-release.aar`

Yêu cầu:

- Android Gradle Plugin 9.3.
- Gradle 9.7.
- JDK 17 trở lên.
- Android SDK 37.
- Engine hỗ trợ từ Android 5.0, API 21.
- Luồng cấp quyền overlay hoạt động từ Android 6.0, API 23.

## Dùng với Apktool

Kiến trúc engine vẫn hoạt động khi nhúng vào APK bạn sở hữu, nhưng Apktool không đọc trực tiếp Java source hoặc AAR. Cần build engine và payload thành DEX, chuyển DEX thành smali, merge manifest, gọi bootstrap, rebuild, align và ký APK.

Đọc theo thứ tự:

1. [Khả năng tương thích và giới hạn](docs/APKTOOL_COMPATIBILITY.md)
2. [Hướng dẫn nhúng bằng Apktool từng bước](docs/HUONG_DAN_APKTOOL.md)
3. [Báo cáo lần nhúng đã kiểm chứng](docs/APKTOOL_INJECTION_REPORT.md)

Tích hợp AAR tại source luôn đáng tin cậy hơn Apktool vì Gradle tự xử lý manifest, DEX, resource và R8.

## Hành vi đóng và dừng menu

- Nút `—`: thu gọn menu về bubble.
- Chạm bubble: mở lại menu.
- Nút `STOP` trong app demo: dừng `MenuOverlayService` hoàn toàn.
- Ứng dụng host cũng có thể gọi `stopService(new Intent(context, MenuOverlayService.class))`.
- Vuốt ứng dụng khỏi Recent Apps: Android dừng service và bubble cùng task.
- Service dùng `START_NOT_STICKY`, nên không tự sống lại sau khi process bị hệ thống dừng.
- Nhấn Home chỉ đưa ứng dụng xuống nền, task chưa bị xóa nên bubble vẫn tồn tại.

## Giấy phép

MIT — xem [LICENSE](LICENSE).
