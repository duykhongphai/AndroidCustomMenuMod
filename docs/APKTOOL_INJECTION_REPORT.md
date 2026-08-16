# Báo cáo nhúng UI engine bằng Apktool

Ngày thực hiện: 16/08/2026

Phạm vi: kiểm thử tương thích UI engine trên APK LibGDX do người dùng sở hữu. Profile được nhúng chỉ lưu trạng thái UI; bridge là no-op, không chứa hook hoặc logic thay đổi trò chơi.

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

- Build từ `onyx-core` và `apktool-payload`.
- Hai `classes.jar` được compile bằng D8 với `--min-api 21`.
- DEX payload: 62.700 byte.
- Số file smali: 54.
- Package engine: `com.nguyen.onyxmenu`.
- Package payload: `com.nguyen.onyxpayload`.
- Profile đang chọn: `menufreefire` qua `MenuFreeFireProvider`.
- Không còn package, class hoặc chuỗi thương hiệu cũ trong payload.
- Không có tham chiếu tới `R` của APK host.

## Thay đổi đã nhúng

- Thêm payload dưới dạng `classes3.dex`.
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
D:\APK_Toolkit_by_0xd00d\2 - Compiled\Hiep250x6-Onyx-menufreefire-v7-signed.apk
```

SHA-256:

```text
19595FA88D6AAB04C74FD87D30DD8FB3E6897E0C48664DF9C6D718D71EC4999E
```

Kích thước: 266.241.124 byte.

APK preview độc lập để kiểm tra giao diện nhanh:

```text
D:\APK_Toolkit_by_0xd00d\2 - Compiled\Onyx-menufreefire-preview-debug.apk
```

SHA-256 preview: `25F214316F636984A8A1BF362E1F8D6354F1F0EC669CD7486553CF91B243B918`.

## Kết quả xác minh

- Apktool rebuild: đạt.
- Zipalign: đạt.
- Chữ ký APK v1: đạt.
- Chữ ký APK v2: đạt.
- Chữ ký APK v3: đạt.
- Package/min SDK/target SDK: giữ nguyên.
- Provider, bootstrap, activity cấp quyền, service và renderer: tìm thấy trong DEX cuối.
- DEX cuối có đúng 9 nhãn toggle của profile `menufreefire`; bridge chỉ chuyển tiếp tới default no-op và không gọi Log/native.
- Lời gọi bootstrap: tìm thấy trong bytecode launcher của APK cuối.
- Manifest APK cuối xác nhận `android:stopWithTask="true"`.
- Bytecode APK cuối xác nhận `onStartCommand` trả về `START_NOT_STICKY` (`2`).
- Bytecode header chỉ còn một nút có mô tả `Collapse menu` và không còn ký tự `×`.
- Lint `app`, `onyx-core`, `apktool-payload`: 0 issue.
- Unit test: 3 test model và 1 test profile `menufreefire` đạt, 0 failure.
- Kiểm thử trên thiết bị: chưa thực hiện vì không có thiết bị/emulator ADB kết nối.

APK được ký bằng test certificate của toolkit. Certificate này chỉ phù hợp kiểm thử cục bộ, không phải chữ ký phát hành chính thức.
