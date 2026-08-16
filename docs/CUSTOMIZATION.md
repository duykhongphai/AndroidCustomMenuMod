# Customization

All menu-specific content belongs in a `MenuProvider`. The demo implementation is `app/src/main/java/com/nguyen/androidcustommenumod/profile/DemoMenuProvider.java`.

## Toggle

```java
MenuControl.toggle(
        "show_grid",
        "Layout grid",
        "Show alignment guides",
        false
)
```

The fourth argument is the default state.

## Slider

```java
MenuControl.slider(
        "overlay_opacity",
        "Overlay opacity",
        "Controls surface transparency",
        0f,
        1f,
        0.9f
)
```

Arguments after the description are minimum, maximum, and default value. A `0..1` range is displayed as a percentage; other ranges are displayed as decimal values.

## Action

```java
MenuControl.action("export_report", "EXPORT REPORT")
```

For a destructive-looking secondary action:

```java
MenuControl.dangerAction("clear_local_data", "CLEAR LOCAL DATA")
```

The engine only emits an event. The bridge decides whether the action does anything and should apply any required confirmation or authorization.

## Color palette

```java
MenuControl.palette(
        "accent_palette",
        "Accent palette",
        "purple",
        MenuOption.color("purple", "Purple", 0xFF7C5CFF),
        MenuOption.color("cyan", "Cyan", 0xFF2DE2E6)
)
```

The third argument is the default option ID.

## Add a section

```java
MenuSection.builder("Developer tools")
        .meta("LOCAL")
        .control(MenuControl.toggle(...))
        .control(MenuControl.action(...))
        .build()
```

## Add a tab

```java
MenuTab.builder("developer", "DEV")
        .hero(MenuHero.builder("Toolkit", "Developer controls")
                .description("Controls available in authorized debug builds.")
                .metric("08", "TOOLS")
                .build())
        .section(...)
        .build()
```

Attach the tab to `MenuProfile.Builder` with `.tab(...)`.

## Handle events

Use stable IDs in the bridge:

```java
public final class MyFeatureBridge implements FeatureBridge {
    @Override
    public void onToggleChanged(String id, boolean enabled) {
        if ("show_grid".equals(id)) {
            layoutDebugger.setGridVisible(enabled);
        }
    }

    @Override
    public void onValueChanged(String id, float value) {
        if ("overlay_opacity".equals(id)) {
            overlaySettings.setOpacity(value);
        }
    }

    @Override
    public void onChoiceChanged(String id, String optionId) {
        if ("accent_palette".equals(id)) {
            overlaySettings.setAccent(optionId);
        }
    }

    @Override
    public void onAction(String id) {
        if ("export_report".equals(id)) {
            reportExporter.export();
        }
    }
}
```

## Multiple purposes

Create one provider per purpose rather than copying the engine:

```text
profile/
├── DeveloperMenuProvider.java
├── DesignerMenuProvider.java
└── AccessibilityMenuProvider.java
```

Each application or product flavor selects its provider through manifest metadata. This avoids long-lived Git branches and allows every profile to receive engine fixes automatically.

For fully separate repositories, publish or copy `menu-core-release.aar` and implement the provider in each consumer project.
