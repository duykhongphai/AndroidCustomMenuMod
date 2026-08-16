package com.nguyen.onyxmenu.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public final class Design {
    public static final int INK = Color.rgb(8, 11, 19);
    public static final int INK_SOFT = Color.rgb(12, 16, 27);
    public static final int SURFACE = Color.rgb(16, 21, 34);
    public static final int SURFACE_HIGH = Color.rgb(23, 29, 45);
    public static final int SURFACE_HOVER = Color.rgb(29, 36, 55);
    public static final int TEXT = Color.rgb(244, 247, 251);
    public static final int MUTED = Color.rgb(140, 150, 170);
    public static final int SUBTLE = Color.rgb(91, 101, 123);
    public static final int DIVIDER = Color.rgb(39, 47, 67);
    public static final int ACCENT = Color.rgb(124, 92, 255);
    public static final int ACCENT_LIGHT = Color.rgb(167, 139, 255);
    public static final int CYAN = Color.rgb(45, 226, 230);
    public static final int SUCCESS = Color.rgb(84, 230, 165);
    public static final int DANGER = Color.rgb(255, 100, 124);

    private Design() {
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static TextView text(Context context, String value, float sizeSp, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    public static TextView title(Context context, String value, float sizeSp) {
        TextView view = text(context, value, sizeSp, TEXT);
        view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return view;
    }

    public static TextView label(Context context, String value) {
        TextView view = text(context, value.toUpperCase(Locale.ROOT), 10, MUTED);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setLetterSpacing(0.14f);
        return view;
    }

    public static GradientDrawable rounded(Context context, int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    public static GradientDrawable outlined(
            Context context,
            int color,
            int strokeColor,
            float radiusDp,
            float strokeDp
    ) {
        GradientDrawable drawable = rounded(context, color, radiusDp);
        drawable.setStroke(Math.max(1, dp(context, strokeDp)), strokeColor);
        return drawable;
    }

    public static GradientDrawable gradient(
            Context context,
            int startColor,
            int endColor,
            float radiusDp
    ) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor}
        );
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    public static Drawable ripple(int rippleColor, Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, null);
    }

    public static LinearLayout vertical(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static LinearLayout horizontal(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    public static LinearLayout.LayoutParams rowParams(int width, int height, float weight) {
        return new LinearLayout.LayoutParams(width, height, weight);
    }

    public static void margins(View view, int leftDp, int topDp, int rightDp, int bottomDp) {
        Context context = view.getContext();
        ViewGroup.LayoutParams raw = view.getLayoutParams();
        if (raw instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) raw;
            params.setMargins(
                    dp(context, leftDp),
                    dp(context, topDp),
                    dp(context, rightDp),
                    dp(context, bottomDp)
            );
            view.setLayoutParams(params);
        }
    }

    public static TextView iconButton(Context context, String glyph, String description) {
        TextView button = text(context, glyph, 18, MUTED);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(description);
        button.setBackground(ripple(
                0x337C5CFF,
                outlined(context, SURFACE_HIGH, DIVIDER, 12, 1)
        ));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    public static TextView pill(Context context, String value, int foreground, int background) {
        TextView pill = text(context, value, 10, foreground);
        pill.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        pill.setGravity(Gravity.CENTER);
        pill.setLetterSpacing(0.08f);
        pill.setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6));
        pill.setBackground(rounded(context, background, 99));
        return pill;
    }
}
