package com.nguyen.androidcustommenumod.overlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.nguyen.androidcustommenumod.bridge.FeatureBridge;
import com.nguyen.androidcustommenumod.storage.PreferenceStore;
import com.nguyen.androidcustommenumod.ui.BrandMarkView;
import com.nguyen.androidcustommenumod.ui.Design;

public final class MenuOverlayController {
    private final Context context;
    private final WindowManager windowManager;
    private final FeatureBridge bridge;
    private final PreferenceStore preferences;
    private View attachedView;
    private WindowManager.LayoutParams attachedParams;
    private boolean expanded;

    public MenuOverlayController(Context context, FeatureBridge bridge) {
        this.context = context;
        this.bridge = bridge;
        this.preferences = new PreferenceStore(context);
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void show() {
        showBubble(
                preferences.getInt("bubble_x", Design.dp(context, 18)),
                preferences.getInt("bubble_y", Design.dp(context, 160))
        );
    }

    public void destroy() {
        removeAttachedView();
    }

    private void showBubble(int x, int y) {
        removeAttachedView();
        expanded = false;

        int size = Design.dp(context, 68);
        FrameLayout bubble = new FrameLayout(context);
        bubble.setContentDescription("Open Nebula control center");
        bubble.setElevation(Design.dp(context, 18));
        GradientDrawable background = Design.gradient(context, 0xF21A2030, 0xF20D1220, 23);
        background.setStroke(Design.dp(context, 1), 0xAA7C5CFF);
        bubble.setBackground(background);

        BrandMarkView mark = new BrandMarkView(context);
        FrameLayout.LayoutParams markParams = new FrameLayout.LayoutParams(
                Design.dp(context, 48),
                Design.dp(context, 48),
                Gravity.CENTER
        );
        bubble.addView(mark, markParams);

        View online = new View(context);
        GradientDrawable onlineBackground = new GradientDrawable();
        onlineBackground.setShape(GradientDrawable.OVAL);
        onlineBackground.setColor(Design.SUCCESS);
        onlineBackground.setStroke(Design.dp(context, 2), Design.INK_SOFT);
        online.setBackground(onlineBackground);
        FrameLayout.LayoutParams onlineParams = new FrameLayout.LayoutParams(
                Design.dp(context, 12),
                Design.dp(context, 12),
                Gravity.END | Gravity.BOTTOM
        );
        onlineParams.setMargins(0, 0, Design.dp(context, 5), Design.dp(context, 5));
        bubble.addView(online, onlineParams);

        WindowManager.LayoutParams params = createParams(size, size, x, y);
        attach(bubble, params);
        attachDragBehavior(bubble, params, true);
    }

    private void showMenu(int x, int y) {
        removeAttachedView();
        expanded = true;

        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        int width = Math.min(Design.dp(context, 370), screenWidth - Design.dp(context, 20));
        int height = Math.min(Design.dp(context, 580), screenHeight - Design.dp(context, 56));
        int menuX = clamp(x, Design.dp(context, 8), Math.max(Design.dp(context, 8), screenWidth - width - Design.dp(context, 8)));
        int menuY = clamp(y, Design.dp(context, 16), Math.max(Design.dp(context, 16), screenHeight - height - Design.dp(context, 16)));

        ModernMenuView menu = new ModernMenuView(context, bridge, preferences);
        menu.setOnCollapseListener(() -> showBubble(attachedParams.x, attachedParams.y));
        menu.setOnCloseListener(() -> context.stopService(new Intent(context, MenuOverlayService.class)));

        WindowManager.LayoutParams params = createParams(width, height, menuX, menuY);
        attach(menu, params);
        attachDragBehavior(menu.getDragHandle(), params, false);
    }

    private WindowManager.LayoutParams createParams(int width, int height, int x, int y) {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width,
                height,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = x;
        params.y = y;
        return params;
    }

    private void attach(View view, WindowManager.LayoutParams params) {
        if (!Settings.canDrawOverlays(context)) {
            context.stopService(new Intent(context, MenuOverlayService.class));
            return;
        }
        try {
            windowManager.addView(view, params);
            attachedView = view;
            attachedParams = params;
        } catch (RuntimeException error) {
            context.stopService(new Intent(context, MenuOverlayService.class));
        }
    }

    private void removeAttachedView() {
        if (attachedView == null) {
            return;
        }
        try {
            windowManager.removeViewImmediate(attachedView);
        } catch (RuntimeException ignored) {
            // The system may already have detached the overlay.
        }
        attachedView = null;
        attachedParams = null;
    }

    private void attachDragBehavior(
            View handle,
            WindowManager.LayoutParams params,
            boolean openOnTap
    ) {
        int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        handle.setOnTouchListener(new View.OnTouchListener() {
            private float downRawX;
            private float downRawY;
            private int startX;
            private int startY;
            private boolean dragged;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (attachedView == null || attachedParams != params) {
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    startX = params.x;
                    startY = params.y;
                    dragged = false;
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    float deltaX = event.getRawX() - downRawX;
                    float deltaY = event.getRawY() - downRawY;
                    if (Math.abs(deltaX) > touchSlop || Math.abs(deltaY) > touchSlop) {
                        dragged = true;
                    }
                    int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
                    int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
                    params.x = clamp(startX + Math.round(deltaX), 0, Math.max(0, screenWidth - params.width));
                    params.y = clamp(startY + Math.round(deltaY), 0, Math.max(0, screenHeight - params.height));
                    try {
                        windowManager.updateViewLayout(attachedView, params);
                    } catch (RuntimeException ignored) {
                        return false;
                    }
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    preferences.putInt("bubble_x", params.x);
                    preferences.putInt("bubble_y", params.y);
                    if (!dragged) {
                        view.performClick();
                        if (openOnTap) {
                            showMenu(params.x, params.y);
                        }
                    }
                    return true;
                }
                return event.getAction() == MotionEvent.ACTION_CANCEL;
            }
        });
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
