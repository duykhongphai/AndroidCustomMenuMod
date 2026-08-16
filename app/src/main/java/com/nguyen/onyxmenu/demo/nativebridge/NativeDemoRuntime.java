package com.nguyen.onyxmenu.demo.nativebridge;

/**
 * Small Java API over the process-local C++ demo state.
 *
 * The native library is packaged inside this app. It does not access another
 * application's process and contains no hook or memory-patching behavior.
 */
public final class NativeDemoRuntime {
    static {
        System.loadLibrary("onyx_demo_native");
    }

    private NativeDemoRuntime() {
    }

    public static void setToggle(String featureId, boolean enabled) {
        nativeSetToggle(requireId(featureId), enabled);
    }

    public static void setValue(String featureId, float value) {
        nativeSetValue(requireId(featureId), value);
    }

    public static void setChoice(String featureId, String optionId) {
        nativeSetChoice(requireId(featureId), requireId(optionId));
    }

    public static void performAction(String actionId) {
        nativePerformAction(requireId(actionId));
    }

    public static void reset() {
        nativeReset();
    }

    public static String snapshot() {
        return nativeSnapshot();
    }

    /** Runs an offset exercise over a C++ struct owned by this demo process. */
    public static String runOwnedOffsetLab() {
        return nativeRunOwnedOffsetLab();
    }

    private static String requireId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Native demo IDs must not be blank");
        }
        return value;
    }

    private static native void nativeSetToggle(String featureId, boolean enabled);

    private static native void nativeSetValue(String featureId, float value);

    private static native void nativeSetChoice(String featureId, String optionId);

    private static native void nativePerformAction(String actionId);

    private static native void nativeReset();

    private static native String nativeSnapshot();

    private static native String nativeRunOwnedOffsetLab();
}
