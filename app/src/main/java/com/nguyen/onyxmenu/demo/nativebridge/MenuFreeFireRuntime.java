package com.nguyen.onyxpayload.nativebridge;

import android.content.Context;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.graphics.PixelFormat;

import com.nguyen.onyxpayload.overlay.OverlayView;

public class MenuFreeFireRuntime {
    private static OverlayView overlayView;
    private static boolean overlayAdded = false;

    public static void initOverlay(Context context) {
        if (overlayAdded) return;
        overlayView = new OverlayView(context);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                LayoutParams.TYPE_APPLICATION_OVERLAY,
                LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        wm.addView(overlayView, params);
        overlayAdded = true;

        // Gọi native để đăng ký callback
        nativeRegisterCallback(overlayView);
        // Truyền kích thước màn hình (có thể lấy từ DisplayMetrics)
        nativeSetScreenSize(wm.getDefaultDisplay().getWidth(), wm.getDefaultDisplay().getHeight());
    }

    // Callback từ native
    public void drawESP(int count, float[] data) {
        if (overlayView != null) {
            overlayView.drawESP(count, data);
        }
    }

    // Native methods
    public static native void nativeInitializeHack(long il2cppBase);
    public static native void nativeRegisterCallback(Object obj);
    public static native void nativeSetScreenSize(int width, int height);
    public static native boolean nativeSetToggle(String featureId, boolean enabled);
}