package com.nguyen.nebulapayload;

import android.util.Log;

import com.nguyen.nebulamenu.bridge.FeatureBridge;

public final class StandaloneFeatureBridge implements FeatureBridge {
    private static final String TAG = "NebulaPayload";

    @Override
    public void onToggleChanged(String featureId, boolean enabled) {
        Log.i(TAG, featureId + " = " + enabled);
    }

    @Override
    public void onValueChanged(String featureId, float value) {
        Log.i(TAG, featureId + " = " + value);
    }

    @Override
    public void onChoiceChanged(String featureId, String optionId) {
        Log.i(TAG, featureId + " = " + optionId);
    }

    @Override
    public void onAction(String actionId) {
        Log.i(TAG, "Action: " + actionId);
    }
}
