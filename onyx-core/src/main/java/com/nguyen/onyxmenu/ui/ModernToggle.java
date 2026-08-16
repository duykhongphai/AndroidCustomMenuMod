package com.nguyen.onyxmenu.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public final class ModernToggle extends View {
    public interface OnCheckedChangeListener {
        void onCheckedChanged(boolean checked);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean checked;
    private float progress;
    private OnCheckedChangeListener listener;
    private ValueAnimator animator;

    public ModernToggle(Context context) {
        this(context, null);
    }

    public ModernToggle(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        setFocusable(true);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = Design.dp(getContext(), 50);
        int height = Design.dp(getContext(), 28);
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean value) {
        setChecked(value, false);
    }

    public void setChecked(boolean value, boolean animate) {
        checked = value;
        float target = value ? 1f : 0f;
        if (!animate) {
            progress = target;
            invalidate();
            return;
        }
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(progress, target);
        animator.setDuration(180);
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener value) {
        listener = value;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        setChecked(!checked, true);
        if (listener != null) {
            listener.onCheckedChanged(checked);
        }
        sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float height = getHeight();
        float width = getWidth();
        float radius = height / 2f;

        paint.setColor(blend(Design.SURFACE_HOVER, Design.ACCENT, progress));
        canvas.drawRoundRect(0, 0, width, height, radius, radius, paint);

        float thumbRadius = height * 0.36f;
        float start = radius;
        float end = width - radius;
        float thumbX = start + (end - start) * progress;
        paint.setColor(Design.TEXT);
        paint.setShadowLayer(Design.dp(getContext(), 5), 0, Design.dp(getContext(), 2), 0x66000000);
        canvas.drawCircle(thumbX, radius, thumbRadius, paint);
        paint.clearShadowLayer();
    }

    private static int blend(int from, int to, float amount) {
        float inverse = 1f - amount;
        return android.graphics.Color.rgb(
                Math.round(android.graphics.Color.red(from) * inverse + android.graphics.Color.red(to) * amount),
                Math.round(android.graphics.Color.green(from) * inverse + android.graphics.Color.green(to) * amount),
                Math.round(android.graphics.Color.blue(from) * inverse + android.graphics.Color.blue(to) * amount)
        );
    }
}
