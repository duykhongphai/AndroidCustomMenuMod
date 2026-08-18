# Inject Onyx menu vào APK được phép kiểm thử

Workflow nhận hai đầu vào kéo-thả: APK host và APK/folder menu donor. Nếu không
chọn donor, script build và dùng `app-debug.apk` mặc định. Nó không chép
`MainActivity`/resource của app donor vào APK host.

Script chỉ copy các thành phần sau:

- `onyx-core`: engine, model, overlay, storage và custom view.
- `MenuProvider` đã chọn và các class cùng package với provider.
- Các thư viện `libonyx_*.so` có ABI tương thích nếu donor dùng native bridge.

Chỉ sử dụng với APK bạn sở hữu hoặc được cho phép kiểm thử. Drag mode hiển thị
cảnh báo nhưng không hỏi confirm lặp lại; advanced mode vẫn yêu cầu cờ
`-IHavePermission`. Script luôn tạo APK output mới, không tự cài đặt và không
bypass signature/integrity check.

## Files

- `scripts/inject-demo-menu.bat`: wrapper dễ gọi từ CMD/PowerShell.
- `scripts/inject-demo-menu.ps1`: implementation xử lý manifest, smali và signing.

## Yêu cầu

- Script tự tìm JDK từ `-JavaHome`, `JAVA_HOME`, JDK 21 mặc định, Android
  Studio JBR rồi mới tới `java.exe` trong `PATH`. Nếu vẫn thiếu, script cho phép
  kéo folder JDK vào cửa sổ.
- Script tự tìm toolkit từ `-ToolkitRoot`, biến `ONYX_APK_TOOLKIT` hoặc
  `D:\APK_Toolkit_by_0xd00d`. Có thể truyền cả toolkit root hoặc trực tiếp folder
  `6 - Resources`; nếu vẫn thiếu, script yêu cầu kéo folder vào cửa sổ.
- Toolkit cần có:
  - `6 - Resources\apktool.jar`
  - `6 - Resources\zipalign.exe`
  - `6 - Resources\apksigner.jar`
  - `6 - Resources\ApkToolkit_Key.pk8`
  - `6 - Resources\ApkToolkit_Certificate.pem`
- Android SDK/NDK cần thiết để build `app-debug.apk` donor.

## Cách dễ nhất: kéo thả

### Kéo APK host hoặc folder host lên file BAT

Trong File Explorer, kéo một trong hai thứ sau thả trực tiếp lên
`scripts\inject-demo-menu.bat`:

- Một file `.apk`.
- Một folder chứa APK, ví dụ folder `release` của Android/LibGDX.

Nếu folder chỉ có một APK, script chọn tự động. Nếu có nhiều APK, script liệt kê
đường dẫn, dung lượng và yêu cầu nhập số tương ứng.

Sau đó BAT yêu cầu kéo một APK/folder menu donor. Nhấn Enter để dùng built-in
Onyx demo. Nếu donor chứa nhiều `MenuProvider`, script liệt kê class để bạn chọn.
Toolkit và JDK được tự tìm.

### Double-click BAT rồi kéo folder vào cửa sổ

Bạn cũng có thể double-click `inject-demo-menu.bat`, sau đó lần lượt:

1. Kéo APK/folder host vào cửa sổ và nhấn Enter.
2. Kéo APK/folder menu donor vào cửa sổ và nhấn Enter; hoặc chỉ nhấn Enter để
   dùng demo mặc định.

### Truyền thêm folder toolkit

Nếu toolkit không nằm ở vị trí mặc định:
đối số thứ hai là menu source và đối số thứ ba là toolkit:

```bat
scripts\inject-demo-menu.bat "C:\MyApps\release" "C:\MyMenus\preview-app" "E:\MyApkToolkit"
```

Đối số toolkit chấp nhận toolkit root hoặc folder chứa trực tiếp
`apktool.jar`, `zipalign.exe`, `apksigner.jar` và test key.

### Validate folder bằng drag mode

```bat
scripts\inject-demo-menu.bat "C:\MyApps\release" "C:\MyMenus\preview-app" --validate-only
```

Lệnh này tự chọn APK trong folder nhưng chỉ kiểm tra môi trường, không decode
hoặc sửa APK.

## Dùng với APK Hiep250x6 Native

Mở PowerShell tại repository:

```powershell
Set-Location "C:\Users\nguye\Documents\GitHub\AndroidCustomMenuMod"

.\scripts\inject-demo-menu.bat `
    -HostPath "C:\Users\nguye\Documents\GitHub\LibGDXProjects\Hiep250x6 Native\android\build\outputs\apk\release" `
    -MenuSource "C:\Users\nguye\Documents\GitHub\AndroidCustomMenuMod\app" `
    -IHavePermission
```

Mặc định, APK được tạo cạnh APK nguồn với tên:

```text
android-release-unsigned-onyx-menu-test-signed.apk
```

Thư mục trung gian có dạng:

```text
onyx-menu-inject-work-YYYYMMDD-HHMMSS
```

Script không xóa thư mục này để bạn có thể kiểm tra manifest/smali đã thay đổi.

## Chọn output và work directory

```powershell
.\scripts\inject-demo-menu.bat `
    -HostPath "C:\MyApps\sample.apk" `
    -MenuSource "C:\MyMenus\preview-app" `
    -OutputApk "C:\MyApps\sample-onyx-test.apk" `
    -WorkRoot "C:\Temp\sample-onyx-work" `
    -IHavePermission
```

`OutputApk` và `WorkRoot` phải chưa tồn tại. Script từ chối ghi đè để tránh làm
mất APK hoặc dữ liệu đã decode.

## Chọn menu/provider

`-MenuSource` nhận:

- APK donor đã build.
- Folder module/app chứa `build\outputs\apk\debug\*.apk`.
- Folder lớn hơn có APK nằm bên dưới; nếu có nhiều APK script cho chọn.

Donor cần chứa một class triển khai `com.nguyen.onyxmenu.engine.MenuProvider`.
Provider có thể nằm trong namespace riêng; provider và bridge/helper riêng nên
nằm cùng package để được copy cùng nhau.

Nếu donor có nhiều menu, script hỏi số. Có thể chọn thẳng class:

```powershell
.\scripts\inject-demo-menu.bat `
    -HostPath "C:\MyApps\sample.apk" `
    -MenuSource "C:\MyMenus\preview.apk" `
    -ProviderClass "com.nguyen.onyxmenu.profiles.preview.MyMenuProvider" `
    -IHavePermission
```

## Bỏ qua bước build donor mặc định

Nếu [app-debug.apk](../app/build/outputs/apk/debug/app-debug.apk) đã được build
đúng phiên bản mong muốn:

```powershell
.\scripts\inject-demo-menu.bat `
    -HostPath "C:\MyApps\sample.apk" `
    -SkipBuild `
    -IHavePermission
```

Nếu không dùng `-SkipBuild`, script tự chạy:

```powershell
.\gradlew.bat :app:assembleDebug
```

Việc auto-build chỉ áp dụng khi không truyền `-MenuSource`. Với menu donor bên
ngoài, hãy build APK donor trước rồi kéo APK/folder build vào BAT.

## Chỉ kiểm tra môi trường

Để kiểm tra APK, Java, toolkit và donor mà không decode hoặc sửa file:

```powershell
.\scripts\inject-demo-menu.bat `
    -HostPath "C:\MyApps\sample.apk" `
    -MenuSource "C:\MyMenus\preview-app" `
    -SkipBuild `
    -ValidateOnly `
    -IHavePermission
```

## Script làm gì?

1. Xác minh APK, Java, Apktool, zipalign, apksigner và test key.
2. Build `app-debug.apk` nếu dùng donor mặc định và không có `-SkipBuild`.
3. Decode host APK và donor APK bằng Apktool.
4. Tìm các `MenuProvider` Onyx trong donor và chọn menu.
5. Tạo một `smali_classesN` mới trong host.
6. Copy engine, package của provider và native demo nếu donor có; không copy app
   donor hoặc `R`.
7. Thêm permission, provider đã chọn và `MenuOverlayService` vào manifest.
8. Tìm activity có `MAIN` + `LAUNCHER`.
9. Thêm hai local register mới và gọi `MenuOverlayService` sau `invoke-super onCreate`.
10. Rebuild, zipalign, ký bằng test certificate và verify APK.

Với donor mặc định, manifest dùng:

```text
com.nguyen.onyxmenu.demo.profile.DemoMenuProvider
```

Với donor khác, metadata dùng chính provider bạn chọn.

### Donor MenuFreeFire preview

Module `freefire-payload` là application preview và tạo APK bằng:

```powershell
.\gradlew.bat :freefire-payload:assembleDebug
```

Sau đó kéo folder `freefire-payload` vào ô `Menu source`, hoặc truyền trực tiếp:

```text
freefire-payload\build\outputs\apk\debug\freefire-payload-debug.apk
```

Provider được chọn là `com.nguyen.onyxpayload.MenuFreeFireProvider`; thư viện
`libonyx_menufreefire.so` chỉ quản lý native state và ghi sự kiện control ra
Logcat.

## Cấp quyền overlay

Script không inject activity xin quyền để giữ payload nhỏ. Sau khi cài APK test:

1. Mở `Settings` → `Apps` → `Special app access`.
2. Chọn `Display over other apps`.
3. Bật quyền cho package của APK host.
4. Đóng và mở lại app.

Có thể dùng ADB trên thiết bị kiểm thử:

```powershell
& "D:\APK_Toolkit_by_0xd00d\6 - Resources\adb.exe" `
    shell appops set TEN_PACKAGE android:system_alert_window allow
```

Thay `TEN_PACKAGE` bằng package trong `AndroidManifest.xml` của host.

## Các giới hạn cố ý

- Không tự cài APK lên thiết bị.
- Không gỡ app cũ.
- Không bypass kiểm tra signature hoặc integrity.
- Không scan/patch memory hoặc hook native code của host.
- Không ghi đè APK đầu vào, output hoặc work directory cũ.

APK được ký bằng test certificate nên không thể update trực tiếp app đã cài bằng
certificate khác. Gỡ bản cũ trên thiết bị test hoặc ký lại bằng key thuộc quyền
sở hữu của bạn.

## Lỗi thường gặp

### `Output already exists`

Chọn `-OutputApk` mới. Script cố ý không ghi đè.

### `WorkRoot already exists`

Chọn thư mục mới hoặc bỏ `-WorkRoot` để script tự tạo tên timestamp.

### `Launcher has no onCreate(Bundle)`

Launcher không override method chuẩn. Patch thủ công một Activity do bạn kiểm
soát hoặc tích hợp AAR tại source của app.

### `Launcher uses .registers`

Automatic patcher chỉ sửa method dùng `.locals`. Trường hợp `.registers` cần
patch thủ công để tránh làm thay đổi vị trí parameter register.

### `more than 13 locals`

Smali `invoke-*` dạng thường chỉ mã hóa được register nhỏ. Script dừng thay vì
tạo bytecode không hợp lệ. Chọn Activity bootstrap đơn giản hơn hoặc patch bằng
`/range` thủ công.

### `no compatible ABI`

Host và donor không có ABI chung. Build donor thêm ABI phù hợp hoặc dùng source
integration. Không đổi tên `.so` của ABI này thành ABI khác.

### APK build/ký được nhưng không chạy

Một số app kiểm tra certificate/integrity hoặc dựa vào asset packaging đặc biệt.
Script không bypass các kiểm tra đó. Với app có source, tích hợp AAR qua Gradle
đáng tin cậy hơn Apktool.
