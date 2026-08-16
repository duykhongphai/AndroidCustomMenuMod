package com.nguyen.nebulapayload;

import android.content.Context;

import com.nguyen.nebulamenu.bridge.FeatureBridge;
import com.nguyen.nebulamenu.engine.MenuProvider;
import com.nguyen.nebulamenu.model.MenuControl;
import com.nguyen.nebulamenu.model.MenuHero;
import com.nguyen.nebulamenu.model.MenuOption;
import com.nguyen.nebulamenu.model.MenuProfile;
import com.nguyen.nebulamenu.model.MenuSection;
import com.nguyen.nebulamenu.model.MenuTab;

public final class StandaloneMenuProvider implements MenuProvider {
    @Override
    public MenuProfile createProfile(Context context) {
        return MenuProfile.builder("apktool_demo", "NEBULA")
                .subtitle("APKTOOL PROFILE")
                .version("v1.0.0")
                .footer("UI ENGINE · LOGCAT BRIDGE")
                .tab(MenuTab.builder("core", "CORE")
                        .hero(MenuHero.builder("Engine online", "Your menu payload\nis running.")
                                .description("Controls in this profile only store UI state and emit Logcat events.")
                                .metric("04", "CONTROL TYPES")
                                .metric("01", "PROFILE")
                                .build())
                        .section(MenuSection.builder("Demo controls")
                                .meta("SAFE PROFILE")
                                .control(MenuControl.toggle(
                                        "demo_toggle",
                                        "Demo toggle",
                                        "Persists a local boolean value",
                                        true
                                ))
                                .control(MenuControl.slider(
                                        "demo_intensity",
                                        "Interface intensity",
                                        "Adjust the local preview value",
                                        0f,
                                        1f,
                                        0.68f
                                ))
                                .control(MenuControl.action(
                                        "demo_action",
                                        "PING LOGCAT BRIDGE"
                                ))
                                .build())
                        .build())
                .tab(MenuTab.builder("visual", "VISUAL")
                        .hero(MenuHero.builder("Visual system", "Choose a profile\naccent.")
                                .description("Palette choices are persisted by the UI engine.")
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
                        .section(MenuSection.builder("Motion")
                                .meta("LOCAL")
                                .control(MenuControl.toggle(
                                        "reduced_motion",
                                        "Reduced motion",
                                        "Use calmer interface transitions",
                                        false
                                ))
                                .build())
                        .build())
                .build();
    }

    @Override
    public FeatureBridge createBridge(Context context) {
        return new StandaloneFeatureBridge();
    }
}
