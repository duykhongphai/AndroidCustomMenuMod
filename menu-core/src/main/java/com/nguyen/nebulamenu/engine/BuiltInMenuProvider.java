package com.nguyen.nebulamenu.engine;

import android.content.Context;

import com.nguyen.nebulamenu.bridge.FeatureBridge;
import com.nguyen.nebulamenu.model.MenuControl;
import com.nguyen.nebulamenu.model.MenuHero;
import com.nguyen.nebulamenu.model.MenuProfile;
import com.nguyen.nebulamenu.model.MenuSection;
import com.nguyen.nebulamenu.model.MenuTab;

final class BuiltInMenuProvider implements MenuProvider {
    @Override
    public MenuProfile createProfile(Context context) {
        return MenuProfile.builder("nebula_fallback", "NEBULA")
                .subtitle("UI ENGINE")
                .footer("FALLBACK PROFILE")
                .tab(MenuTab.builder("core", "CORE")
                        .hero(MenuHero.builder("Ready", "Your UI engine is active.")
                                .description("Register a MenuProvider to replace this fallback profile.")
                                .metric("01", "PROFILE")
                                .metric("4", "CONTROL TYPES")
                                .build())
                        .section(MenuSection.builder("Engine status")
                                .meta("LOCAL")
                                .control(MenuControl.toggle(
                                        "engine_enabled",
                                        "UI engine",
                                        "Dynamic profile rendering is available",
                                        true
                                ))
                                .build())
                        .build())
                .build();
    }

    @Override
    public FeatureBridge createBridge(Context context) {
        return new FeatureBridge() {
        };
    }
}
