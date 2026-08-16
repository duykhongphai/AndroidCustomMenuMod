# Architecture

Nebula separates presentation, overlay lifecycle, preferences, and behavior. This keeps the visual layer reusable without coupling it to a specific application.

## Event flow

```text
MainActivity
    │ grants overlay permission and starts
    ▼
MenuOverlayService
    │ owns foreground-service lifecycle
    ▼
MenuOverlayController
    │ attaches bubble/menu through WindowManager
    ▼
ModernMenuView
    │ emits semantic IDs and values
    ▼
FeatureBridge
    │
    └── DemoFeatureBridge → Logcat only
```

## Presentation

`Design` contains colors, density conversion, text factories, and drawable factories. Custom controls depend on those tokens instead of duplicating visual values.

`ModernMenuView` composes the full menu programmatically. This avoids XML resource-name collisions and third-party UI dependencies. Each user-facing setting has a stable semantic ID such as `focus_mode` or `surface_opacity`.

## Overlay lifecycle

`MenuOverlayService` starts as a foreground service and owns a `MenuOverlayController`. The controller attaches exactly one `WindowManager` view at a time:

- Collapsed state: a 68 dp draggable brand bubble.
- Expanded state: a responsive control center clamped to the visible display.
- Destroyed state: the attached view is removed immediately.

The Android manifest declares the overlay and foreground-service permissions. Notification permission is optional for starting the overlay but is requested so users can see its ongoing notification normally.

## Behavior bridge

`FeatureBridge` is intentionally small. Replace `DemoFeatureBridge` only when integrating the UI with an app or test environment you control.

Avoid placing behavior directly in click listeners. Stable bridge IDs make it possible to unit test business logic separately, change the menu layout later, or provide a different implementation for a debug build.

## Data storage

`PreferenceStore` uses one private `SharedPreferences` file. It stores menu values and the bubble position. Restoring defaults preserves position, so a user does not unexpectedly lose where they placed the overlay.
