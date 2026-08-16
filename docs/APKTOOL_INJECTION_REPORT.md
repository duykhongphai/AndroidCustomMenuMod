# Apktool injection report

Date: 2026-08-16

Scope: authorized UI-engine compatibility test against the user's own LibGDX APK. The injected standalone profile only persists UI state and writes bridge events to Logcat.

## Inputs

- APK: `android-release-unsigned.apk`
- Package: `com.monkey.nso`
- Launcher: `com.monkey.nso.android.AndroidLauncher`
- Minimum SDK: 21
- Target SDK: 36
- Apktool: 2.11.0
- Payload DEX: 62,288 bytes, 54 smali classes

Original APK SHA-256:

```text
675008A9A2159F22C0DD537B9102A5436FD777BC1D78897F712B6DA838FD797C
```

## Injected contract

- Added payload as `classes3.dex`.
- Added overlay and foreground-service permissions.
- Registered `StandaloneMenuProvider` through `MENU_PROVIDER` metadata.
- Registered `NebulaPermissionActivity` and `MenuOverlayService`.
- Added a call to `NebulaBootstrap.launch(Context)` immediately after the launcher's superclass `onCreate` call.
- Left the original APK and source tree unchanged.

## Output

```text
D:\APK_Toolkit_by_0xd00d\2 - Compiled\Hiep250x6-Nebula-UIEngine-v2-signed.apk
```

Final APK SHA-256:

```text
4CD8CE82194095339A18341A7D62C2816419B3AEF5C5CDF1B0564F8D5ADA6167
```

Final size: 266,241,124 bytes.

## Verification

- Apktool rebuild: passed.
- Zipalign verification: passed.
- APK signature verification: v1, v2, and v3 passed.
- Package/minimum SDK/target SDK preserved.
- Provider, bootstrap, permission activity, service, and menu renderer found in final DEX files.
- Launcher bootstrap call found in the final APK's decompiled bytecode.
- Runtime device test: not run because no ADB device or emulator was connected.

The APK is signed with the toolkit test certificate. It is suitable for local testing but is not a production release signature.
