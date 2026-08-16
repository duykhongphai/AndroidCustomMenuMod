package com.nguyen.androidcustommenumod.bridge;

import android.util.Log;

/** A harmless bridge used by the sample app. It only writes events to Logcat. */
public final class DemoFeatureBridge implements FeatureBridge {
    private static final String TAG = "NebulaDemoBridge";

    @Override
    public void onToggleChanged(String featureId, boolean enabled) {
        Log.i(TAG, featureId + " = " + enabled);
    }

    @Override
    public void onValueChanged(String featureId, float value) {
        Log.i(TAG, featureId + " = " + value);
    }

    @Override
    public void onAction(String actionId) {
        Log.i(TAG, "Action: " + actionId);
    }
}
