package com.nguyen.onyxmenu.demo.profile;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.nguyen.onyxmenu.bridge.FeatureBridge;
import com.nguyen.onyxmenu.demo.nativebridge.NativeDemoRuntime;

/** Routes menu events through JNI into the harmless process-local C++ demo state. */
public final class DemoFeatureBridge implements FeatureBridge {
    private static final String TAG = "OnyxDemoBridge";
    private final Context applicationContext;

    public DemoFeatureBridge(Context context) {
        applicationContext = context.getApplicationContext();
    }

    @Override
    public void onToggleChanged(String featureId, boolean enabled) {
        NativeDemoRuntime.setToggle(featureId, enabled);
        logSnapshot("toggle " + featureId + " = " + enabled);
    }

    @Override
    public void onValueChanged(String featureId, float value) {
        NativeDemoRuntime.setValue(featureId, value);
        logSnapshot("value " + featureId + " = " + value);
    }

    @Override
    public void onChoiceChanged(String featureId, String optionId) {
        NativeDemoRuntime.setChoice(featureId, optionId);
        logSnapshot("choice " + featureId + " = " + optionId);
    }

    @Override
    public void onAction(String actionId) {
        NativeDemoRuntime.performAction(actionId);
        String snapshot = NativeDemoRuntime.snapshot();
        Log.i(TAG, "action " + actionId + " -> " + snapshot);

        String message;
        if ("run_owned_offset_lab".equals(actionId)) {
            message = "Owned offset lab: " + NativeDemoRuntime.runOwnedOffsetLab();
        } else if ("clear_demo_state".equals(actionId)) {
            message = "C++ state cleared: " + snapshot;
        } else {
            message = "C++ snapshot: " + snapshot;
        }
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show();
    }

    private static void logSnapshot(String event) {
        Log.i(TAG, event + " -> " + NativeDemoRuntime.snapshot());
    }
}
