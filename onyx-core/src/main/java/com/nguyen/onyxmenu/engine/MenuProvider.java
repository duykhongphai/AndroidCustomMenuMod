package com.nguyen.onyxmenu.engine;

import android.content.Context;

import com.nguyen.onyxmenu.bridge.FeatureBridge;
import com.nguyen.onyxmenu.model.MenuProfile;

public interface MenuProvider {
    MenuProfile createProfile(Context context);

    FeatureBridge createBridge(Context context);
}
