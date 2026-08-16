# Hướng dẫn tùy chỉnh menu

Tài liệu này mô tả cách đổi nội dung, xử lý sự kiện và giao diện mà không sửa engine.

## File cần chỉnh

Khi chạy app demo:

- Khai báo menu: `app/src/main/java/com/nguyen/onyxmenu/demo/profile/DemoMenuProvider.java`
- Xử lý sự kiện: `app/src/main/java/com/nguyen/onyxmenu/demo/profile/DemoFeatureBridge.java`

Khi build payload Apktool:

- Khai báo menu: `apktool-payload/src/main/java/com/nguyen/onyxpayload/StandaloneMenuProvider.java`
- Xử lý sự kiện: `apktool-payload/src/main/java/com/nguyen/onyxpayload/StandaloneFeatureBridge.java`

Không nên thêm logic riêng vào `ModernMenuView` hoặc `MenuOverlayController`.

## Quy tắc đặt ID

Mỗi control trong một profile phải có ID duy nhất:

```text
show_grid
overlay_opacity
export_report
accent_palette
```

Nên dùng chữ thường, số và dấu gạch dưới. Không đổi ID sau khi đã phát hành nếu muốn giữ lại giá trị trong `SharedPreferences`.

Engine sẽ từ chối profile có tab ID hoặc control ID bị trùng.

## Toggle

```java
MenuControl.toggle(
        "show_grid",
        "Lưới căn chỉnh",
        "Hiện đường căn chỉnh bố cục",
        false
)
```

Tham số cuối là trạng thái mặc định.

Bridge nhận sự kiện:

```java
@Override
public void onToggleChanged(String id, boolean enabled) {
    if ("show_grid".equals(id)) {
        layoutDebugger.setGridVisible(enabled);
    }
}
```

## Slider

```java
MenuControl.slider(
        "overlay_opacity",
        "Độ trong suốt",
        "Điều chỉnh độ trong suốt của bề mặt",
        0f,
        1f,
        0.9f
)
```

Ba số cuối lần lượt là giá trị nhỏ nhất, lớn nhất và mặc định.

- Khoảng `0..1` được hiển thị dưới dạng phần trăm.
- Khoảng khác được hiển thị với một chữ số thập phân.
- Giá trị mặc định phải nằm trong khoảng hợp lệ.

Bridge:

```java
@Override
public void onValueChanged(String id, float value) {
    if ("overlay_opacity".equals(id)) {
        overlaySettings.setOpacity(value);
    }
}
```

## Nút action

Nút bình thường:

```java
MenuControl.action(
        "export_report",
        "XUẤT BÁO CÁO"
)
```

Nút có màu cảnh báo:

```java
MenuControl.dangerAction(
        "clear_local_data",
        "XÓA DỮ LIỆU CỤC BỘ"
)
```

Renderer chỉ phát sự kiện. Bridge quyết định hành động có chạy hay không:

```java
@Override
public void onAction(String id) {
    if ("export_report".equals(id)) {
        reportExporter.export();
    }
}
```

Nếu hành động nguy hiểm, ứng dụng host phải tự thêm xác nhận và kiểm tra quyền.

## Palette màu

```java
MenuControl.palette(
        "accent_palette",
        "Màu chủ đạo",
        "purple",
        MenuOption.color("purple", "Tím", 0xFF7C5CFF),
        MenuOption.color("cyan", "Xanh cyan", 0xFF2DE2E6),
        MenuOption.color("pink", "Hồng", 0xFFFF6B9C)
)
```

Tham số thứ ba là ID option mặc định. ID này phải tồn tại trong danh sách option.

Bridge:

```java
@Override
public void onChoiceChanged(String id, String optionId) {
    if ("accent_palette".equals(id)) {
        overlaySettings.setAccent(optionId);
    }
}
```

## Tạo section

```java
MenuSection.builder("Công cụ phát triển")
        .meta("CỤC BỘ")
        .control(MenuControl.toggle(...))
        .control(MenuControl.slider(...))
        .control(MenuControl.action(...))
        .build()
```

`meta` là dòng chữ nhỏ bên phải tiêu đề section và có thể bỏ qua.

## Tạo hero

```java
MenuHero.builder(
        "Bộ công cụ",
        "Điều khiển dành cho\nbản debug."
)
        .description("Các chức năng dành cho môi trường được cho phép kiểm thử.")
        .metric("08", "CÔNG CỤ")
        .metric("1.0", "PHIÊN BẢN")
        .build()
```

Hero có thể không có metric. Khi có metric, renderer sử dụng card gradient và các ô thống kê phía dưới.

## Tạo tab

```java
MenuTab.builder("developer", "DEV")
        .hero(MenuHero.builder(...).build())
        .section(MenuSection.builder(...).build())
        .build()
```

Sau đó thêm tab vào profile:

```java
MenuProfile.builder("developer_profile", "ONYX")
        .subtitle("DEVELOPER TOOLS")
        .version("v1.0.0")
        .footer("LOCAL DEBUG PROFILE")
        .tab(coreTab)
        .tab(developerTab)
        .build();
```

## Tạo provider hoàn chỉnh

```java
public final class MyMenuProvider implements MenuProvider {
    @Override
    public MenuProfile createProfile(Context context) {
        return MenuProfile.builder("my_profile", "ONYX")
                .subtitle("MY CONTROL CENTER")
                .version("v1.0.0")
                .footer("LOCAL PROFILE")
                .tab(createMainTab())
                .build();
    }

    @Override
    public FeatureBridge createBridge(Context context) {
        return new MyFeatureBridge();
    }
}
```

Đăng ký provider trong manifest:

```xml
<meta-data
    android:name="com.nguyen.onyxmenu.MENU_PROVIDER"
    android:value="com.example.MyMenuProvider" />
```

## Nhiều mục đích khác nhau

Tạo một provider cho mỗi mục đích, không copy engine:

```text
profile/
├── DeveloperMenuProvider.java
├── DesignerMenuProvider.java
├── AccessibilityMenuProvider.java
└── DiagnosticsMenuProvider.java
```

Mỗi ứng dụng hoặc product flavor chọn provider tương ứng qua manifest. Cách này giúp mọi profile tự nhận bản sửa lỗi và nâng cấp giao diện từ `onyx-core`.

Không nên tạo một Git branch dài hạn cho mỗi bộ menu vì sẽ khó merge bản sửa engine.

## Đổi màu và phong cách chung

Các design token nằm trong:

```text
onyx-core/src/main/java/com/nguyen/onyxmenu/ui/Design.java
```

Ví dụ:

```java
public static final int ACCENT = Color.rgb(124, 92, 255);
public static final int CYAN = Color.rgb(45, 226, 230);
public static final int SURFACE = Color.rgb(16, 21, 34);
public static final int TEXT = Color.rgb(244, 247, 251);
```

Sau khi đổi design token, build lại AAR hoặc payload rồi tái nhúng APK.

## Đổi logo

Logo được vẽ bằng Canvas trong:

```text
onyx-core/src/main/java/com/nguyen/onyxmenu/ui/BrandMarkView.java
```

Không cần thêm PNG hoặc resource vào APK host. Điều này giúp workflow Apktool ít bị xung đột resource ID.

## Hành vi nút trên header

Hiện tại:

- `—` gọi callback thu gọn.
- Không hiển thị nút `×`.
- Bubble vẫn tồn tại và có thể mở lại menu.
- Vuốt ứng dụng khỏi Recent Apps sẽ dừng service vì `stopWithTask=true`.
- Service không tự khởi động lại vì dùng `START_NOT_STICKY`.
- Ứng dụng host vẫn có thể dừng ngay bằng `stopService`.

Nếu muốn đổi icon nhưng giữ hành vi, sửa glyph `—` trong `ModernMenuView.createHeader` và không thay callback.

## Lưu cấu hình

Khóa lưu được tạo theo mẫu:

```text
profile:<profileId>:<controlId>
```

Hai profile có thể dùng cùng một control ID mà không ghi đè giá trị của nhau. Vị trí bubble được lưu toàn cục để không thay đổi khi chuyển profile.

## Checklist sau khi tùy chỉnh

1. Mọi tab ID và control ID đều duy nhất.
2. ID trong bridge khớp chính xác với provider.
3. Slider có khoảng hợp lệ.
4. Palette có option mặc định hợp lệ.
5. Provider có constructor công khai không tham số.
6. Tên provider trong manifest đúng package đầy đủ.
7. Chạy `testDebugUnitTest`, `assembleDebug`, `lintDebug` và `assembleRelease`.
8. Nếu dùng Apktool, build lại DEX/smali payload trước khi rebuild APK host.
