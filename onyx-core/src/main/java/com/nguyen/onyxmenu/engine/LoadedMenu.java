package com.nguyen.onyxmenu.engine;

import com.nguyen.onyxmenu.bridge.FeatureBridge;
import com.nguyen.onyxmenu.model.MenuProfile;

import java.util.Objects;

public final class LoadedMenu {
    private final MenuProfile profile;
    private final FeatureBridge bridge;

    LoadedMenu(MenuProfile profile, FeatureBridge bridge) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public MenuProfile getProfile() {
        return profile;
    }

    public FeatureBridge getBridge() {
        return bridge;
    }
}
