# Nebula Android Custom Menu

Nebula is a modern, zero-dependency floating menu template for Android. It is built with classic Android Views and Canvas so the UI layer stays small, portable, and easy to customize.

The repository ships as a complete Android Studio project with:

- A draggable floating bubble and expandable control center.
- Three polished menu sections: Core, Visual, and System.
- Custom animated toggle and slider components.
- A code-drawn brand mark and animated nebula background.
- Persisted controls, palette choice, and overlay position.
- A replaceable `FeatureBridge` boundary.
- A harmless demo bridge that only writes events to Logcat.
- No Compose, Material Components, networking, analytics, or ads.

> Use this project only with apps you own or are explicitly authorized to test. It does not include hooking, memory patching, anti-cheat bypasses, or game-specific modifications.

## Preview the project

1. Open the repository in Android Studio.
2. Let Gradle sync and run the `app` configuration on Android 6.0 or newer.
3. Tap **Launch overlay**.
4. Grant Android's **Display over other apps** permission.
5. Tap the floating Nebula bubble to expand the menu.

You can also build from a terminal:

```powershell
./gradlew.bat assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

```text
app/src/main/java/com/nguyen/androidcustommenumod/
├── MainActivity.java                 # Showcase and permission flow
├── bridge/
│   ├── FeatureBridge.java            # Replaceable behavior boundary
│   └── DemoFeatureBridge.java        # Logcat-only sample
├── overlay/
│   ├── MenuOverlayService.java       # Foreground service lifecycle
│   ├── MenuOverlayController.java    # WindowManager and drag behavior
│   └── ModernMenuView.java           # Menu composition and controls
├── storage/
│   └── PreferenceStore.java          # Local UI preferences
└── ui/
    ├── BrandMarkView.java            # Canvas-drawn identity
    ├── Design.java                   # Tokens and drawable helpers
    ├── ModernSlider.java             # Custom slider
    ├── ModernToggle.java             # Custom animated switch
    └── NebulaBackgroundView.java     # Animated landing background
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the event flow and extension points.

## Customize the look

The central palette lives in `ui/Design.java`. Start with these tokens:

```java
public static final int ACCENT = Color.rgb(124, 92, 255);
public static final int CYAN = Color.rgb(45, 226, 230);
public static final int SURFACE = Color.rgb(16, 21, 34);
public static final int TEXT = Color.rgb(244, 247, 251);
```

Menu content is composed in `overlay/ModernMenuView.java`. The helper methods `addToggleCard`, `addSliderCard`, and `addActionButton` keep new controls visually consistent.

## Connect behavior you own

Implement `FeatureBridge` and pass your implementation to `MenuOverlayController`:

```java
public final class MyAppBridge implements FeatureBridge {
    @Override
    public void onToggleChanged(String featureId, boolean enabled) {
        // Call an authorized feature in your own app.
    }

    @Override
    public void onValueChanged(String featureId, float value) {
        // Update your own app's setting.
    }

    @Override
    public void onAction(String actionId) {
        // Run an authorized local action.
    }
}
```

Keep UI code independent of business or native code. That makes the design previewable and prevents app-specific logic from spreading through the view layer.

## Requirements

- Android Studio compatible with Android Gradle Plugin 9.3.
- JDK 17 or newer.
- Android SDK 37.
- Android 6.0 (API 23) or newer device/emulator.

## License

MIT — see [LICENSE](LICENSE).
