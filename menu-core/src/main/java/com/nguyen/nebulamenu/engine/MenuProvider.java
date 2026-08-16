package com.nguyen.nebulamenu.engine;

import android.content.Context;

import com.nguyen.nebulamenu.bridge.FeatureBridge;
import com.nguyen.nebulamenu.model.MenuProfile;

public interface MenuProvider {
    MenuProfile createProfile(Context context);

    FeatureBridge createBridge(Context context);
}
