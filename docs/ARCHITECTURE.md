# Kiến trúc UI engine

Nebula tách giao diện dùng chung khỏi nội dung menu và hành vi riêng của từng ứng dụng.

## Luồng hoạt động

```text
AndroidManifest của ứng dụng host
    │ metadata MENU_PROVIDER
    ▼
MenuProviderLoader
    │ tạo
    ├── MenuProfile ──────────────┐
    └── FeatureBridge             │
                                  ▼
MenuOverlayService ──► MenuOverlayController
                                  │
                                  ▼
                           ModernMenuView
                                  │
                    render profile và phát sự kiện
                                  │
                                  ▼
                           FeatureBridge
```

## Module `menu-core`

`menu-core` chứa toàn bộ phần ổn định của engine:

- `engine`: đọc metadata, tạo provider và cung cấp profile dự phòng.
- `model`: các model bất biến cho profile, tab, section, hero, metric, option và control.
- `overlay`: foreground service, `WindowManager`, kéo thả và renderer động.
- `storage`: lưu giá trị control theo profile và lưu vị trí bubble toàn cục.
- `ui`: màu sắc, typography, drawable helper và custom View.

Module không tham chiếu tới app demo hoặc mã nguồn game. Notification tự tìm launcher activity của package host.

Engine không tham chiếu resource ID của host. Notification dùng icon framework Android và chuỗi nội bộ, vì vậy DEX của engine có thể dùng trong bài kiểm thử Apktool mà không phải sửa hằng số `R`.

## Hợp đồng của ứng dụng host

Ứng dụng cung cấp một class implement `MenuProvider`:

```java
public interface MenuProvider {
    MenuProfile createProfile(Context context);
    FeatureBridge createBridge(Context context);
}
```

`MenuProfile` mô tả những gì cần hiển thị. `FeatureBridge` nhận ID và giá trị khi người dùng tương tác. Logic riêng của ứng dụng chỉ được đặt trong bridge hoặc các service/controller do ứng dụng sở hữu.

## Hợp đồng renderer

`ModernMenuView` duyệt toàn bộ profile và render theo loại control:

- `TOGGLE` gọi `FeatureBridge.onToggleChanged`.
- `SLIDER` gọi `FeatureBridge.onValueChanged`.
- `PALETTE` gọi `FeatureBridge.onChoiceChanged`.
- `ACTION` gọi `FeatureBridge.onAction`.

Do đó thêm hoặc xóa control chỉ thay đổi provider, không làm thay đổi engine.

## Vòng đời overlay

`MenuOverlayService` sở hữu một `MenuOverlayController`. Controller chỉ gắn một cửa sổ tại một thời điểm:

- Trạng thái thu gọn: bubble 68 dp.
- Trạng thái mở: control center responsive.
- Trạng thái dừng: view được gỡ khỏi `WindowManager`.

Header chỉ giữ nút `—` để chuyển từ trạng thái mở sang bubble. UI engine không hiển thị nút `×` và nút thu gọn không gọi `stopService`.

Service được khai báo `android:stopWithTask="true"` và trả về `START_NOT_STICKY`:

- Vuốt task khỏi Recent Apps sẽ dừng service và gỡ bubble.
- Process bị hệ thống dừng sẽ không làm service tự khởi động lại.
- Nhấn Home không xóa task, vì vậy bubble vẫn tồn tại khi ứng dụng chỉ chạy nền.
- Ứng dụng host vẫn có thể gọi `stopService` để dừng chủ động.

## Khôi phục process

Engine không dùng đăng ký provider bằng biến static. Android có thể tạo lại foreground service mà không mở lại activity, nên tên provider được lưu trong manifest. Service có thể khôi phục profile trực tiếp từ application context.

## R8

Provider được nạp bằng reflection. `menu-core/consumer-rules.pro` giữ mọi class implement `MenuProvider` và constructor công khai không tham số. Khi dùng AAR qua Gradle, rules này được nhập tự động.

Trong workflow Apktool thủ công, phải giữ nguyên tên class provider trong DEX/smali vì consumer rules không còn tham gia quá trình build host.

## Module `apktool-payload`

Payload độc lập gồm:

- `NebulaBootstrap`: kiểm tra quyền và khởi động engine.
- `NebulaPermissionActivity`: mở trang cấp quyền overlay rồi quay lại khởi động service.
- `StandaloneMenuProvider`: profile demo chỉ thao tác trạng thái UI.
- `StandaloneFeatureBridge`: chỉ ghi sự kiện vào Logcat.

Payload không có resource riêng và không chứa logic game. Khi build cùng `menu-core`, hai `classes.jar` có thể được D8 thành một DEX rồi chuyển sang smali.
