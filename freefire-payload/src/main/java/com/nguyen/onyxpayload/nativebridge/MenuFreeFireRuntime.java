package com.nguyen.onyxpayload.nativebridge;

import android.util.Log;

/** API Java tối thiểu của trạng thái C++ dành cho profile menufreefire. */
public final class MenuFreeFireRuntime {
    private static final String TAG = "OnyxMenuFreeFire";
    private static final boolean AVAILABLE = loadNativeLibrary();

    private MenuFreeFireRuntime() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static boolean setToggle(String featureId, boolean enabled) {
        return AVAILABLE && nativeSetToggle(requireId(featureId), enabled);
    }

    public static boolean setValue(String featureId, float value) {
        return AVAILABLE && nativeSetValue(requireId(featureId), value);
    }

    public static void reset() {
        if (AVAILABLE) {
            nativeReset();
        }
    }

    public static String snapshot() {
        return AVAILABLE ? nativeSnapshot() : "{\"native_available\":false}";
    }

    private static boolean loadNativeLibrary() {
        try {
            System.loadLibrary("onyx_menufreefire");
            return true;
        } catch (UnsatisfiedLinkError error) {
            Log.e(TAG, "Không thể nạp libonyx_menufreefire", error);
            return false;
        }
    }

    private static String requireId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ID tính năng không được để trống");
        }
        return value;
    }

    private static native boolean nativeSetToggle(String featureId, boolean enabled);

    private static native boolean nativeSetValue(String featureId, float value);

    private static native void nativeReset();

    private static native String nativeSnapshot();
}
