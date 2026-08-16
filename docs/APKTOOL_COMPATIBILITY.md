# Khả năng tương thích với Apktool

UI engine có thể được nhúng bằng Apktool vào APK bạn sở hữu hoặc được cho phép sửa đổi. Việc chuyển sang kiến trúc profile không làm mất khả năng này.

## Apktool làm được gì

Apktool có thể:

- Decode và rebuild manifest/resource.
- Chuyển DEX sang smali và smali trở lại DEX.
- Giữ lại assets, native library và phần lớn file chưa biết.

Apktool không thể:

- Compile Java source.
- Dùng trực tiếp AAR như Gradle.
- Tự merge dependency hoặc consumer ProGuard rules.
- Giữ nguyên chữ ký APK sau khi rebuild.

Vì vậy phải build `menu-core` và `apktool-payload` trước, chuyển `classes.jar` thành DEX bằng D8 rồi chuyển DEX thành smali.

## Hợp đồng bắt buộc của APK cuối

APK sau khi rebuild phải có đủ:

- Các class đã compile trong package `com.nguyen.nebulamenu`.
- Các class payload trong package `com.nguyen.nebulapayload`.
- `StandaloneMenuProvider` và `StandaloneFeatureBridge`.
- `NebulaPermissionActivity`.
- `MenuOverlayService`.
- Metadata `com.nguyen.nebulamenu.MENU_PROVIDER` trỏ tới đúng provider.
- Quyền overlay, foreground service, special-use và notification.
- Một lời gọi `NebulaBootstrap.launch(Context)` từ điểm khởi động do bạn kiểm soát.

Thiếu một thành phần có thể dẫn tới:

- Chỉ hiện profile dự phòng.
- Không mở trang cấp quyền.
- Không khởi động foreground service.
- Không xuất hiện bubble.
- Crash do không tìm thấy class.

## Vì sao payload ít xung đột

- Không có thư viện runtime bên thứ ba.
- Giao diện được tạo bằng code.
- Logo được vẽ bằng Canvas.
- Engine và payload không tham chiếu `R` của app host.
- Tên resource riêng của engine đã được loại bỏ khỏi payload thủ công.
- Provider được chọn bằng một metadata duy nhất.
- Payload có thể đặt trong một multidex folder mới.

## Phiên bản Android

- Engine compile với min SDK 21.
- `NebulaBootstrap` không chạy overlay trên API thấp hơn 23 vì Android chưa có luồng `Settings.canDrawOverlays` hiện đại.
- Từ API 23 trở lên, lần chạy đầu mở trang “Hiển thị trên ứng dụng khác”.
- Từ API 26 trở lên, bootstrap dùng `startForegroundService`.
- APK target SDK mới cần khai báo foreground service type `specialUse` và permission tương ứng.
- Service dùng `stopWithTask=true` và `START_NOT_STICKY`, nên biến mất khi task host bị xóa và không tự khởi động lại.

## Giới hạn quan trọng

### Chữ ký

Rebuild làm mất chữ ký gốc. Nếu điện thoại đang cài APK cùng package nhưng ký bằng key khác, Android báo `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

Giải pháp hợp lệ:

- Ký bằng đúng release key của ứng dụng bạn sở hữu; hoặc
- Gỡ bản đang cài rồi cài bản test.

Test key trong `APK_Toolkit_by_0xd00d` chỉ phù hợp kiểm thử cục bộ.

### Kiểm tra integrity

Ứng dụng tự kiểm tra signature, checksum hoặc file DEX có thể từ chối APK đã rebuild. UI engine không bypass các cơ chế đó.

### Multidex

Phải chọn một folder mới như `smali_classes3`, `smali_classes4` dựa trên số DEX hiện có. Không copy payload đè vào package/class đã tồn tại.

### Resource

Payload hiện tại không tham chiếu resource của host nên không cần merge `public.xml` hoặc sửa ID. Nếu tự thêm XML, PNG hoặc string resource vào payload, bạn phải xử lý resource merge và xung đột tên.

### R8 và obfuscation

Trong Gradle, consumer rules giữ tên provider. Trong workflow Apktool, class provider đã được compile trước và metadata phải dùng đúng tên class trong smali. Không được đổi tên provider sau khi D8/baksmali.

## Nên dùng AAR khi nào

Nếu có source của ứng dụng, nên thêm:

```kotlin
dependencies {
    implementation(files("libs/menu-core-release.aar"))
}
```

Gradle sẽ xử lý manifest, DEX, resource và R8 an toàn hơn.

Chỉ dùng Apktool khi cần kiểm thử APK đã build hoặc không thể chạy lại build source. Quy trình đầy đủ nằm tại [HUONG_DAN_APKTOOL.md](HUONG_DAN_APKTOOL.md).

## Bảng lỗi nhanh

| Hiện tượng | Nguyên nhân thường gặp | Cách kiểm tra |
|---|---|---|
| Không mở trang cấp quyền | Chưa gọi bootstrap | Decompile launcher và tìm `NebulaBootstrap` |
| Có quyền nhưng không có bubble | Service/permission bị thiếu | Kiểm tra manifest và Logcat |
| Hiện profile dự phòng | Metadata/provider sai | So sánh class name đầy đủ |
| `ClassNotFoundException` | Copy thiếu smali hoặc sai multidex | Dùng `apkanalyzer dex packages` |
| Không cài đè được | Chữ ký khác bản đang cài | Kiểm tra bằng `apksigner verify` |
| Build lỗi duplicate class | Payload copy đè package đã có | Dùng một folder DEX mới và kiểm tra class |
| Build lỗi resource | Tự thêm resource có ID/tên xung đột | Giữ payload resource-free |
