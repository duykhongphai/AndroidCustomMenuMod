# Báo cáo nhúng UI engine bằng Apktool

Ngày thực hiện: 16/08/2026

Phạm vi: kiểm thử tương thích UI engine và native state cục bộ trên APK LibGDX do người dùng sở hữu. Bridge JNI chỉ lưu cấu hình trong tiến trình và xuất snapshot chẩn đoán; toggle `Bypass Emulator Detect` không triển khai bypass thật, payload không chứa hook hoặc logic thay đổi trò chơi.

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

- Build từ `onyx-core` và `freefire-payload`.
- Hai `classes.jar` được compile bằng D8 với `--min-api 21`.
- DEX payload: 65.504 byte.
- Số file smali: 55.
- Package engine: `com.nguyen.onyxmenu`.
- Package payload: `com.nguyen.onyxpayload`.
- Profile đang chọn: `menufreefire` qua `MenuFreeFireProvider`.
- Native runtime: `libonyx_menufreefire.so` cho `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
- Không còn package, class hoặc chuỗi thương hiệu cũ trong payload.
- Không có tham chiếu tới `R` của APK host.

## Thay đổi đã nhúng

- Thêm payload dưới dạng `classes3.dex`.
- Thêm native state JNI dưới dạng `libonyx_menufreefire.so` cho bốn ABI của host.
- Thêm quyền overlay và foreground service.
- Đăng ký `MenuFreeFireProvider` bằng metadata `MENU_PROVIDER`.
- Đăng ký `OnyxPermissionActivity`.
- Đăng ký `MenuOverlayService` với type `specialUse`.
- Service bám theo task bằng `stopWithTask=true` và `START_NOT_STICKY`.
- Thêm lời gọi `OnyxBootstrap.launch(Context)` ngay sau `super.onCreate` của launcher.
- Giữ nguyên APK gốc và toàn bộ source game.
- Header chỉ có nút `—` để thu gọn; nút `×` đã được loại bỏ.
- Vuốt game khỏi Recent Apps sẽ dừng service và xóa bubble.

## APK đầu ra hiện tại

```text
D:\APK_Toolkit_by_0xd00d\2 - Compiled\Hiep250x6-Onyx-menufreefire-v9-signed.apk
```

SHA-256:

```text
1EC4D1E27C4F77712F8F4CB23B3BACA41E8E139996A1A0720478036A014B4E18
```

Kích thước: 267.326.889 byte.

## Kết quả xác minh

- Apktool rebuild: đạt.
- Zipalign: đạt.
- Chữ ký APK v1: đạt.
- Chữ ký APK v2: đạt.
- Chữ ký APK v3: đạt.
- Package/min SDK/target SDK: giữ nguyên.
- Provider, bootstrap, activity cấp quyền, service và renderer: tìm thấy trong DEX cuối.
- DEX cuối có bốn nhóm `ESP`, `AIMBOT`, `XOAY`, `BYPASS`, đúng 11 toggle và một slider `Tốc độ xoay` với min/max/default là 1/10/5.
- `MenuFreeFireFeatureBridge` khôi phục cả `bypass_emulator_detect`; native state của bốn ABI chấp nhận ID này và chỉ ghi nó vào snapshot cấu hình.
- Lời gọi bootstrap: tìm thấy trong bytecode launcher của APK cuối.
- Manifest APK cuối xác nhận `android:stopWithTask="true"`.
- Bytecode APK cuối xác nhận `onStartCommand` trả về `START_NOT_STICKY` (`2`).
- Bytecode header chỉ còn một nút có mô tả `Collapse menu` và không còn ký tự `×`.
- Lint `app`, `onyx-core`, `freefire-payload`: 0 issue.
- Unit test: 3 test model và 1 test profile `menufreefire` đạt, 0 failure.
- Kiểm thử trên thiết bị: chưa thực hiện vì không có thiết bị/emulator ADB kết nối.

APK được ký bằng test certificate của toolkit. Certificate này chỉ phù hợp kiểm thử cục bộ, không phải chữ ký phát hành chính thức.
