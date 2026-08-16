package com.nguyen.onyxmenu.engine;

import android.content.Context;

import com.nguyen.onyxmenu.bridge.FeatureBridge;
import com.nguyen.onyxmenu.model.MenuControl;
import com.nguyen.onyxmenu.model.MenuHero;
import com.nguyen.onyxmenu.model.MenuProfile;
import com.nguyen.onyxmenu.model.MenuSection;
import com.nguyen.onyxmenu.model.MenuTab;

final class BuiltInMenuProvider implements MenuProvider {
    @Override
    public MenuProfile createProfile(Context context) {
        return MenuProfile.builder("onyx_fallback", "ONYX")
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
