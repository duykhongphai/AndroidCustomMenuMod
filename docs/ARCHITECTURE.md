# Architecture

Nebula separates reusable presentation from app-specific menu definitions and behavior.

## Runtime flow

```text
Host application manifest
    │ MENU_PROVIDER metadata
    ▼
MenuProviderLoader
    │ creates
    ├── MenuProfile ──────────────┐
    └── FeatureBridge             │
                                  ▼
MenuOverlayService ──► MenuOverlayController
                                  │
                                  ▼
                           ModernMenuView
                                  │
                   renders profile and emits events
                                  │
                                  ▼
                           FeatureBridge
```

## `menu-core`

The Android library owns all stable engine behavior:

- `engine`: discovers the host provider from manifest metadata and supplies a fallback.
- `model`: immutable profile, tab, section, hero, metric, option, and control definitions.
- `overlay`: owns the foreground service, `WindowManager`, drag behavior, and dynamic renderer.
- `storage`: namespaces settings by profile ID and persists the bubble position globally.
- `ui`: contains design tokens and custom Android Views.

`menu-core` does not reference the demo app. Its notification opens the host package's launcher activity dynamically.

The engine also avoids host resource IDs: its notification uses an Android framework icon and internal text constants. This lets compiled engine code move between authorized APK test builds without patching generated `R` references.

## `apktool-payload`

The standalone payload module contains:

- `NebulaBootstrap`: starts the engine or opens the overlay permission flow.
- `NebulaPermissionActivity`: returns from Android settings and starts the service.
- `StandaloneMenuProvider`: a safe profile containing UI-only demo controls.
- `StandaloneFeatureBridge`: writes events to Logcat and does not modify host behavior.

The module deliberately contains no application resources or game-specific code.

## Host contract

A host supplies one class implementing `MenuProvider`:

```java
public interface MenuProvider {
    MenuProfile createProfile(Context context);
    FeatureBridge createBridge(Context context);
}
```

The profile is declarative and immutable after construction. The bridge receives semantic control IDs and is the only place app-specific behavior belongs.

## Renderer contract

`ModernMenuView` renders every tab and section by iterating the profile model. Adding or removing a control therefore changes only the provider. The engine recognizes four control types:

- `TOGGLE` → `FeatureBridge.onToggleChanged`
- `SLIDER` → `FeatureBridge.onValueChanged`
- `PALETTE` → `FeatureBridge.onChoiceChanged`
- `ACTION` → `FeatureBridge.onAction`

## Process recreation

Static registration is intentionally avoided. Android can recreate a foreground service without reopening the host activity, so the provider class name is stored in merged manifest metadata. The engine can reconstruct both profile and bridge directly from the application context.

## R8

Provider discovery uses reflection. `menu-core/consumer-rules.pro` keeps every `MenuProvider` implementation and its public no-argument constructor. These rules are automatically included when the host consumes the AAR through Gradle.
