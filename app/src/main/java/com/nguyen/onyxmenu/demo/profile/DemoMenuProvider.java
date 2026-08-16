package com.nguyen.onyxmenu.demo.profile;

import android.content.Context;

import com.nguyen.onyxmenu.bridge.FeatureBridge;
import com.nguyen.onyxmenu.engine.MenuProvider;
import com.nguyen.onyxmenu.model.MenuControl;
import com.nguyen.onyxmenu.model.MenuHero;
import com.nguyen.onyxmenu.model.MenuOption;
import com.nguyen.onyxmenu.model.MenuProfile;
import com.nguyen.onyxmenu.model.MenuSection;
import com.nguyen.onyxmenu.model.MenuTab;

public final class DemoMenuProvider implements MenuProvider {
    @Override
    public MenuProfile createProfile(Context context) {
        return MenuProfile.builder("onyx_demo", "ONYX")
                .subtitle("CONTROL CENTER")
                .version("v1.1.0")
                .footer("DEMO BRIDGE · PROFILE DRIVEN")
                .tab(createCoreTab())
                .tab(createVisualTab())
                .tab(createSystemTab())
                .build();
    }

    @Override
    public FeatureBridge createBridge(Context context) {
        return new DemoFeatureBridge(context);
    }

    private MenuTab createCoreTab() {
        return MenuTab.builder("core", "CORE")
                .hero(MenuHero.builder("Live workspace", "Everything, right where\nyou need it.")
                        .description("A polished overlay shell powered by a replaceable profile.")
                        .metric("06", "MODULES")
                        .metric("1.1", "BUILD")
                        .build())
                .section(MenuSection.builder("Quick controls")
                        .meta("DEMO BRIDGE")
                        .control(MenuControl.toggle(
                                "focus_mode",
                                "Focus mode",
                                "Quiet, distraction-free profile",
                                true
                        ))
                        .control(MenuControl.toggle(
                                "safe_overlay",
                                "Safe overlay",
                                "Keeps the UI touch-friendly",
                                true
                        ))
                        .control(MenuControl.toggle(
                                "edge_glow",
                                "Edge glow",
                                "Subtle accent around active cards",
                                false
                        ))
                        .control(MenuControl.slider(
                                "intensity",
                                "Interface intensity",
                                "Animation and glow strength",
                                0f,
                                1f,
                                0.68f
                        ))
                        .build())
                .build();
    }

    private MenuTab createVisualTab() {
        return MenuTab.builder("visual", "VISUAL")
                .hero(MenuHero.builder("Visual system", "Built to feel native,\nnot generic.")
                        .description("Tune color, atmosphere and motion without changing engine code.")
                        .build())
                .section(MenuSection.builder("Accent palette")
                        .meta("5 PRESETS")
                        .control(MenuControl.palette(
                                "accent_palette",
                                "Accent palette",
                                "purple",
                                MenuOption.color("purple", "Purple", 0xFF7C5CFF),
                                MenuOption.color("cyan", "Cyan", 0xFF2DE2E6),
                                MenuOption.color("pink", "Pink", 0xFFFF6B9C),
                                MenuOption.color("amber", "Amber", 0xFFFFB454),
                                MenuOption.color("mint", "Mint", 0xFF54E6A5)
                        ))
                        .build())
                .section(MenuSection.builder("Atmosphere")
                        .meta("LIVE PREVIEW")
                        .control(MenuControl.toggle(
                                "ambient_light",
                                "Ambient light",
                                "Soft color bloom behind surfaces",
                                true
                        ))
                        .control(MenuControl.toggle(
                                "particle_field",
                                "Particle field",
                                "Adds depth to the background",
                                true
                        ))
                        .control(MenuControl.toggle(
                                "reduced_motion",
                                "Reduced motion",
                                "Uses simpler menu transitions",
                                false
                        ))
                        .control(MenuControl.slider(
                                "surface_opacity",
                                "Surface opacity",
                                "Balance contrast and depth",
                                0f,
                                1f,
                                0.92f
                        ))
                        .build())
                .build();
    }

    private MenuTab createSystemTab() {
        return MenuTab.builder("system", "SYSTEM")
                .hero(MenuHero.builder("Local profile", "A bridge you can\nreplace safely.")
                        .description("Controls call Java, JNI and a process-local C++ state.")
                        .build())
                .section(MenuSection.builder("Behavior")
                        .meta("PREFERENCES")
                        .control(MenuControl.toggle(
                                "haptic_feedback",
                                "Haptic feedback",
                                "Small response on interactions",
                                true
                        ))
                        .control(MenuControl.toggle(
                                "auto_collapse",
                                "Auto collapse",
                                "Return to the bubble after launch",
                                false
                        ))
                        .control(MenuControl.slider(
                                "animation_speed",
                                "Animation speed",
                                "Controls interface transitions",
                                0f,
                                1f,
                                0.72f
                        ))
                        .build())
                .section(MenuSection.builder("Bridge actions")
                        .meta("JNI · C++ · LOCAL ONLY")
                        .control(MenuControl.action("ping_demo_bridge", "PING DEMO BRIDGE"))
                        .control(MenuControl.action("run_owned_offset_lab", "RUN OWNED OFFSET LAB"))
                        .control(MenuControl.dangerAction("clear_demo_state", "CLEAR DEMO STATE"))
                        .build())
                .build();
    }
}
