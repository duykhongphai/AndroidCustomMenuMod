# Hướng dẫn nhúng menu bằng Apktool

Tài liệu này dùng PowerShell và cấu trúc toolkit sau:

```text
D:\APK_Toolkit_by_0xd00d\
├── 1 - Decompiled\
├── 2 - Compiled\
├── 3 - Extracted\
└── 6 - Resources\
    ├── apktool.jar
    ├── baksmali.jar
    ├── zipalign.exe
    ├── apksigner.jar
    ├── ApkToolkit_Key.pk8
    └── ApkToolkit_Certificate.pem
```

Chỉ thực hiện với APK bạn sở hữu hoặc được phép kiểm thử. Luôn giữ bản gốc và dùng thư mục/output mới.

## 1. Chuẩn bị biến đường dẫn

Mở PowerShell:

```powershell
$repo = "C:\Users\nguye\Documents\GitHub\AndroidCustomMenuMod"
$toolkit = "D:\APK_Toolkit_by_0xd00d"
$apk = "C:\duong-dan\toi\app-release.apk"

$decoded = "$toolkit\1 - Decompiled\MyApp-Onyx"
$payloadWork = "$toolkit\3 - Extracted\OnyxPayload"
$unsigned = "$toolkit\2 - Compiled\MyApp-Onyx-unsigned.apk"
$aligned = "$toolkit\2 - Compiled\MyApp-Onyx-aligned.apk"
$signed = "$toolkit\2 - Compiled\MyApp-Onyx-signed.apk"
```

Các đường dẫn output nên chưa tồn tại để tránh ghi đè nhầm dữ liệu.

## 2. Build engine và payload

```powershell
Set-Location $repo

$env:JAVA_HOME = "C:\duong-dan\toi\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

.\gradlew.bat :onyx-core:assembleRelease :apktool-payload:assembleRelease
```

Kết quả:

```text
onyx-core\build\outputs\aar\onyx-core-release.aar
apktool-payload\build\outputs\aar\apktool-payload-release.aar
```

## 3. Lấy `classes.jar` từ hai AAR

```powershell
$sevenZip = "$toolkit\6 - Resources\7z.exe"
$coreAar = "$repo\onyx-core\build\outputs\aar\onyx-core-release.aar"
$payloadAar = "$repo\apktool-payload\build\outputs\aar\apktool-payload-release.aar"

New-Item -ItemType Directory -Path "$payloadWork\core" -Force | Out-Null
New-Item -ItemType Directory -Path "$payloadWork\payload" -Force | Out-Null

& $sevenZip e $coreAar classes.jar "-o$payloadWork\core" -y
& $sevenZip e $payloadAar classes.jar "-o$payloadWork\payload" -y
```

Sau bước này phải có:

```text
$payloadWork\core\classes.jar
$payloadWork\payload\classes.jar
```

## 4. Compile hai JAR thành DEX

Chọn D8 trong Android SDK:

```powershell
$d8 = "C:\Android\build-tools\36.0.0\d8.bat"
$androidJar = "C:\Android\platforms\android-36\android.jar"
$dexOut = "$payloadWork\dex"

New-Item -ItemType Directory -Path $dexOut -Force | Out-Null

& $d8 `
    --release `
    --min-api 21 `
    --lib $androidJar `
    --output $dexOut `
    "$payloadWork\core\classes.jar" `
    "$payloadWork\payload\classes.jar"
```

Kiểm tra:

```powershell
Get-Item "$dexOut\classes.dex"
```

## 5. Chuyển payload DEX thành smali

```powershell
$baksmali = "$toolkit\6 - Resources\baksmali.jar"
$smaliOut = "$payloadWork\smali"

java -jar $baksmali d "$dexOut\classes.dex" -o $smaliOut

Get-ChildItem $smaliOut -Filter "*.smali" -Recurse | Measure-Object
```

Phải thấy hai package:

```text
$smaliOut\com\nguyen\onyxmenu
$smaliOut\com\nguyen\onyxpayload
```

Payload chuẩn không được tham chiếu `R` của host:

```powershell
rg "Lcom/nguyen/onyxmenu/R;|Lcom/nguyen/onyxpayload/R;" $smaliOut
```

Không có kết quả là đúng.

## 6. Decode APK host

```powershell
$apktool = "$toolkit\6 - Resources\apktool.jar"

java -Xmx6g -jar $apktool d $apk -o $decoded
```

Không dùng `-f` nếu chưa chắc thư mục đích có dữ liệu quan trọng hay không.

## 7. Xác định DEX folder mới

```powershell
Get-ChildItem $decoded -Directory -Filter "smali*" | Select-Object Name
```

Ví dụ APK có:

```text
smali
smali_classes2
```

Thì tạo payload ở `smali_classes3`:

```powershell
$payloadSmaliDestination = "$decoded\smali_classes3"
New-Item -ItemType Directory -Path $payloadSmaliDestination -Force | Out-Null
Copy-Item -LiteralPath "$smaliOut\com" -Destination $payloadSmaliDestination -Recurse
```

Không copy vào folder có class trùng tên.

### 7.1. Chép native library của payload

`MenuFreeFireFeatureBridge` cần `libonyx_menufreefire.so` tương ứng với ABI của APK host. Giải nén thư mục `jni` từ AAR payload và chép từng ABI vào `lib` của thư mục đã decode:

```powershell
$nativeWork = "$payloadWork\native"
& $sevenZip x $payloadAar "jni\*" "-o$nativeWork" -y

Get-ChildItem "$nativeWork\jni" -Directory | ForEach-Object {
    $destination = Join-Path "$decoded\lib" $_.Name
    New-Item -ItemType Directory -Path $destination -Force | Out-Null
    Copy-Item -Path "$($_.FullName)\*" -Destination $destination -Force
}

Get-ChildItem "$decoded\lib" -Filter "libonyx_menufreefire.so" -Recurse
```

Chỉ giữ các ABI mà APK host thực sự phân phối. Với APK split theo ABI, native library phải nằm trong split tương ứng thay vì APK base.

## 8. Tìm launcher activity

Có thể dùng `apkanalyzer`:

```powershell
$apkanalyzer = "C:\Android\cmdline-tools\latest\bin\apkanalyzer.bat"
& $apkanalyzer manifest print $apk |
    Select-String "android.intent.action.MAIN|android.intent.category.LAUNCHER|<activity" -Context 0,3
```

Hoặc mở `$decoded\AndroidManifest.xml` và tìm activity chứa:

```xml
<action android:name="android.intent.action.MAIN" />
<category android:name="android.intent.category.LAUNCHER" />
```

Sau đó tìm file smali:

```powershell
Get-ChildItem $decoded -Filter "TenLauncher.smali" -Recurse
```

## 9. Sửa manifest

Thêm các permission dưới thẻ `<manifest>`:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Thêm vào bên trong `<application>`:

```xml
<meta-data
    android:name="com.nguyen.onyxmenu.MENU_PROVIDER"
    android:value="com.nguyen.onyxpayload.MenuFreeFireProvider" />

<activity
    android:name="com.nguyen.onyxpayload.OnyxPermissionActivity"
    android:excludeFromRecents="true"
    android:exported="false"
    android:theme="@android:style/Theme.Translucent.NoTitleBar" />

<service
    android:name="com.nguyen.onyxmenu.overlay.MenuOverlayService"
    android:exported="false"
    android:foregroundServiceType="specialUse"
    android:stopWithTask="true">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="User-initiated customizable overlay for an app owned by the user" />
</service>
```

Không thêm trùng permission, activity, service hoặc metadata đã tồn tại.

## 10. Gọi bootstrap từ launcher

Mở method:

```smali
.method protected onCreate(Landroid/os/Bundle;)V
```

Ngay sau lời gọi `invoke-super ...->onCreate`, thêm:

```smali
invoke-static {p0}, Lcom/nguyen/onyxpayload/OnyxBootstrap;->launch(Landroid/content/Context;)V
```

Ví dụ:

```smali
.method protected onCreate(Landroid/os/Bundle;)V
    .locals 1

    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    invoke-static {p0}, Lcom/nguyen/onyxpayload/OnyxBootstrap;->launch(Landroid/content/Context;)V

    # Phần code gốc tiếp tục ở đây.
```

Lời gọi chỉ dùng `p0`, nên thường không cần tăng `.locals`.

Nếu launcher không kế thừa `Context`/`Activity`, phải chọn một activity khác do bạn kiểm soát.

## 11. Rebuild APK

```powershell
java -Xmx6g -jar $apktool b $decoded -o $unsigned
```

Trong log phải thấy payload được build thành DEX mới, ví dụ:

```text
Smaling smali_classes3 folder into classes3.dex
```

## 12. Zipalign

```powershell
$zipalign = "$toolkit\6 - Resources\zipalign.exe"

& $zipalign -f -p 4 $unsigned $aligned
& $zipalign -c -v 4 $aligned
```

Dòng cuối phải báo verification successful.

## 13. Ký APK test

```powershell
$apksigner = "$toolkit\6 - Resources\apksigner.jar"
$key = "$toolkit\6 - Resources\ApkToolkit_Key.pk8"
$cert = "$toolkit\6 - Resources\ApkToolkit_Certificate.pem"

java -jar $apksigner sign `
    --key $key `
    --cert $cert `
    --out $signed `
    $aligned
```

Đây là test key của toolkit. Bản phát hành thật phải dùng release key thuộc quyền sở hữu của bạn.

## 14. Xác minh APK cuối

```powershell
& $zipalign -c 4 $signed
java -jar $apksigner verify --verbose --print-certs $signed
```

Kiểm tra manifest:

```powershell
& $apkanalyzer manifest print $signed |
    Select-String "MENU_PROVIDER|OnyxPermissionActivity|MenuOverlayService|SYSTEM_ALERT_WINDOW"
```

Kiểm tra class:

```powershell
& $apkanalyzer dex packages $signed |
    Select-String "OnyxBootstrap|MenuFreeFireProvider|ModernMenuView"
```

Kiểm tra lời gọi trong launcher:

```powershell
& $apkanalyzer dex code `
    --class "com.example.MyLauncher" `
    --method "onCreate(Landroid/os/Bundle;)V" `
    $signed |
    Select-String "OnyxBootstrap"
```

## 15. Chạy thử

1. Cài APK đã ký.
2. Mở ứng dụng.
3. Android mở trang “Hiển thị trên ứng dụng khác”.
4. Bật quyền cho ứng dụng.
5. Quay lại ứng dụng.
6. Bubble Onyx xuất hiện.
7. Chạm bubble để mở menu.
8. Chạm `—` để thu gọn về bubble; menu không có nút `×`.
9. Vuốt ứng dụng khỏi Recent Apps để kiểm tra bubble và service cùng biến mất.

Nếu thiết bị/OEM không quay lại activity đúng cách, đóng và mở lại ứng dụng sau khi đã cấp quyền.

## 16. Tùy chỉnh menu rồi tái nhúng

Sửa:

```text
apktool-payload/src/main/java/com/nguyen/onyxpayload/MenuFreeFireProvider.java
```

Profile này dùng `MenuFreeFireFeatureBridge` để chuyển control qua JNI tới trạng thái cấu hình C++ trong cùng tiến trình. Khi đổi hoặc thêm ID control, phải cập nhật đồng thời `MenuFreeFireFeatureBridge.java` và `src/main/cpp/menufreefire_state.cpp`.

Sau đó lặp lại từ bước build AAR, D8 và baksmali. Không chỉ rebuild thư mục Apktool cũ vì smali payload sẽ không tự cập nhật từ Java source.

## Lỗi thường gặp

### `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

APK trên thiết bị có cùng package nhưng khác certificate. Gỡ bản cũ hoặc ký bằng đúng release key.

### `ClassNotFoundException`

Payload smali chưa được copy đủ, chọn sai multidex folder hoặc metadata trỏ sai class.

### Hiện profile dự phòng

`MenuFreeFireProvider` không được tìm thấy hoặc tạo profile lỗi. Kiểm tra Logcat tag `OnyxMenuEngine`.

### Không có bubble

Kiểm tra:

- Quyền overlay đã bật.
- Service tồn tại trong manifest.
- Foreground-service permissions đầy đủ.
- Launcher thật sự gọi bootstrap.
- Logcat tag `OnyxMenuEngine` để kiểm tra việc nạp profile.
- Logcat tag `OnyxMenuFreeFire` để kiểm tra việc nạp `.so`, ID bị từ chối và snapshot native.
- `lib/<abi>/libonyx_menufreefire.so` tồn tại trong APK đã build.

### Apktool build lỗi duplicate class

Bạn đã copy payload vào DEX folder có class cùng tên. Dùng folder `smali_classesN` mới và không copy nhiều lần.

### APK build được nhưng ứng dụng từ chối chạy

Ứng dụng có thể kiểm tra signature/integrity. UI engine không bypass cơ chế này. Với source của chính bạn, hãy tích hợp AAR ở source level hoặc tắt kiểm tra đó trong bản debug do bạn sở hữu.
