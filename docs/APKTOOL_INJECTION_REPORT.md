# Báo cáo nhúng UI engine bằng Apktool

Ngày thực hiện: 16/08/2026

Phạm vi: kiểm thử tương thích UI engine trên APK LibGDX do người dùng sở hữu. Profile được nhúng chỉ lưu trạng thái UI và ghi sự kiện vào Logcat; không chứa hook hoặc logic thay đổi trò chơi.

## APK đầu vào

```text
C:\Users\nguye\Documents\GitHub\LibGDXProjects\Hiep250x6 Native\android\build\outputs\apk\release\android-release-unsigned.apk
```

Thông tin:

- Package: `com.monkey.nso`
- Launcher: `com.monkey.nso.android.AndroidLauncher`
- Min SDK: 21
- Target SDK: 36
- Kích thước: 265.210.001 byte
- Apktool: 2.11.0

SHA-256 APK gốc:

```text
675008A9A2159F22C0DD537B9102A5436FD777BC1D78897F712B6DA838FD797C
```

## Payload

- Build từ `menu-core` và `apktool-payload`.
- Hai `classes.jar` được compile bằng D8 với `--min-api 21`.
- DEX payload: 61.748 byte.
- Số file smali: 53.
- Package engine: `com.nguyen.nebulamenu`.
- Package payload: `com.nguyen.nebulapayload`.
- Không có tham chiếu tới `R` của APK host.

## Thay đổi đã nhúng

- Thêm payload dưới dạng `classes3.dex`.
- Thêm quyền overlay và foreground service.
- Đăng ký `StandaloneMenuProvider` bằng metadata `MENU_PROVIDER`.
- Đăng ký `NebulaPermissionActivity`.
- Đăng ký `MenuOverlayService` với type `specialUse`.
- Thêm lời gọi `NebulaBootstrap.launch(Context)` ngay sau `super.onCreate` của launcher.
- Giữ nguyên APK gốc và toàn bộ source game.
- Nút `—` và `×` đều thu gọn menu về bubble.

## APK đầu ra hiện tại

```text
D:\APK_Toolkit_by_0xd00d\2 - Compiled\Hiep250x6-Nebula-UIEngine-v3-signed.apk
```

SHA-256:

```text
60D4081AC1851F0E665A668010F3830FDF7B6FBBA1BFDCEDDA8F47188C6D944D
```

Kích thước: 266.241.124 byte.

## Kết quả xác minh

- Apktool rebuild: đạt.
- Zipalign: đạt.
- Chữ ký APK v1: đạt.
- Chữ ký APK v2: đạt.
- Chữ ký APK v3: đạt.
- Package/min SDK/target SDK: giữ nguyên.
- Provider, bootstrap, activity cấp quyền, service và renderer: tìm thấy trong DEX cuối.
- Lời gọi bootstrap: tìm thấy trong bytecode launcher của APK cuối.
- Lint `app`, `menu-core`, `apktool-payload`: 0 issue.
- Unit test model: 3 test đạt, 0 failure.
- Kiểm thử trên thiết bị: chưa thực hiện vì không có thiết bị/emulator ADB kết nối.

APK được ký bằng test certificate của toolkit. Certificate này chỉ phù hợp kiểm thử cục bộ, không phải chữ ký phát hành chính thức.
