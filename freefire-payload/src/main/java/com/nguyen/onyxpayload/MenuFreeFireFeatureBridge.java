package com.nguyen.onyxpayload;

import android.content.Context;
import android.util.Log;

import com.nguyen.onyxmenu.bridge.FeatureBridge;
import com.nguyen.onyxmenu.storage.PreferenceStore;
import com.nguyen.onyxpayload.nativebridge.MenuFreeFireRuntime;

/** Đồng bộ control menufreefire với trạng thái C++ trong cùng tiến trình. */
public final class MenuFreeFireFeatureBridge implements FeatureBridge {
    private static final String TAG = "OnyxMenuFreeFire";
    private static final String PROFILE_ID = "menufreefire";
    private static final String[] TOGGLE_IDS = {
            "esp_enabled",
            "tracer_line",
            "esp_box",
            "health_bar",
            "aimbot_enabled",
            "skip_knock",
            "silent_aim",
            "legit_aim",
            "drag_aim_assist",
            "rotation_enabled",
            "bypass_emulator_detect"
    };

    public MenuFreeFireFeatureBridge(Context context) {
        MenuFreeFireRuntime.reset();
        if (context != null && MenuFreeFireRuntime.isAvailable()) {
            restorePersistedState(new PreferenceStore(context));
        }
        Log.i(TAG, "Khởi tạo bridge: " + MenuFreeFireRuntime.snapshot());
    }

    @Override
    public void onToggleChanged(String featureId, boolean enabled) {
        if (!MenuFreeFireRuntime.setToggle(featureId, enabled)) {
            Log.w(TAG, "Từ chối công tắc: " + featureId);
            return;
        }
        logSnapshot("Công tắc " + featureId + " = " + enabled);
    }

    @Override
    public void onValueChanged(String featureId, float value) {
        if (!MenuFreeFireRuntime.setValue(featureId, value)) {
            Log.w(TAG, "Từ chối giá trị: " + featureId + " = " + value);
            return;
        }
        logSnapshot("Giá trị " + featureId + " = " + value);
    }

    private static void restorePersistedState(PreferenceStore preferences) {
        for (String id : TOGGLE_IDS) {
            MenuFreeFireRuntime.setToggle(
                    id,
                    preferences.getBoolean(preferences.key(PROFILE_ID, id), false)
            );
        }
        MenuFreeFireRuntime.setValue(
                "rotation_speed",
                preferences.getFloat(
                        preferences.key(PROFILE_ID, "rotation_speed"),
                        5f
                )
        );
    }

    private static void logSnapshot(String event) {
        Log.i(TAG, event + " -> " + MenuFreeFireRuntime.snapshot());
    }
}
