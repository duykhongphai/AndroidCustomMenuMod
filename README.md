# Nebula Android Menu Engine

Nebula is a profile-driven floating UI engine for Android. The visual system and overlay lifecycle live in the reusable `menu-core` module; each host app supplies only a `MenuProvider` and a `FeatureBridge`.

The production UI has no Compose, Material Components, networking, analytics, or third-party runtime dependencies.

> Use this project only with apps you own or are explicitly authorized to test. It does not include hooking, memory patching, anti-cheat bypasses, or game-specific modifications.

## What is included

- Draggable floating bubble and expandable control center.
- Dynamic tabs, sections, hero content, metrics, and controls.
- Toggle, slider, action, and color-palette control types.
- Custom Canvas toggle, slider, logo, and animated background.
- Profile-scoped persistent values and a global bubble position.
- Manifest-discovered `MenuProvider` with a safe fallback profile.
- Consumer R8 rules that preserve provider implementations.
- A demo app whose bridge only writes events to Logcat.

## Project structure

```text
AndroidCustomMenuMod/
├── menu-core/                         # Reusable Android library
│   └── src/main/java/com/nguyen/nebulamenu/
│       ├── bridge/FeatureBridge.java
│       ├── engine/                    # Provider discovery and loading
│       ├── model/                     # Profile schema
│       ├── overlay/                   # Service, WindowManager, renderer
│       ├── storage/                   # Profile-scoped preferences
│       └── ui/                        # Visual tokens and custom views
├── app/                               # Showcase host app
│   └── src/main/java/com/nguyen/androidcustommenumod/
│       ├── MainActivity.java
│       └── profile/
│           ├── DemoMenuProvider.java  # Edit this to add/remove controls
│           └── DemoFeatureBridge.java # Handle control events here
└── docs/
    ├── ARCHITECTURE.md
    ├── CUSTOMIZATION.md
    └── APKTOOL_COMPATIBILITY.md
```

## Add a control

Add a control to a section in `DemoMenuProvider`:

```java
.control(MenuControl.toggle(
        "show_grid",
        "Layout grid",
        "Show alignment guides",
        false
))
```

Then handle the same stable ID in the bridge:

```java
@Override
public void onToggleChanged(String featureId, boolean enabled) {
    if ("show_grid".equals(featureId)) {
        layoutDebugger.setGridVisible(enabled);
    }
}
```

No edit to `ModernMenuView`, `MenuOverlayController`, or any visual component is required.

See [docs/CUSTOMIZATION.md](docs/CUSTOMIZATION.md) for every control type and multi-profile guidance.

## Register a profile

The host app points the engine at a provider class through manifest metadata:

```xml
<meta-data
    android:name="com.nguyen.nebulamenu.MENU_PROVIDER"
    android:value="com.example.myapp.MyMenuProvider" />
```

The class must have a public no-argument constructor and implement `MenuProvider`. If loading fails, the engine displays a fallback profile instead of crashing.

## Build

Open the repository in Android Studio or run:

```powershell
./gradlew.bat testDebugUnitTest assembleDebug lintDebug
./gradlew.bat assembleRelease
```

Outputs:

- Demo APK: `app/build/outputs/apk/debug/app-debug.apk`
- Reusable AAR: `menu-core/build/outputs/aar/menu-core-release.aar`

Requirements:

- Android Gradle Plugin 9.3.
- Gradle 9.7.
- JDK 17 or newer.
- Android SDK 37.
- Android 6.0 (API 23) or newer.

## Apktool compatibility

The engine architecture remains compatible with an Apktool-based workflow for an APK you own, but Apktool cannot consume an AAR or Java source directly. The compiled classes, manifest declarations, and small set of resources must be present in the final APK.

Source-level/AAR integration is strongly preferred because Gradle performs manifest, resource, DEX, and R8 merging safely. See [docs/APKTOOL_COMPATIBILITY.md](docs/APKTOOL_COMPATIBILITY.md) for the compatibility contract and limitations.

## License

MIT — see [LICENSE](LICENSE).
