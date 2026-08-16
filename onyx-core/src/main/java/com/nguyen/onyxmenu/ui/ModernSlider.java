package com.nguyen.onyxmenu.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public final class ModernSlider extends View {
    public interface OnValueChangeListener {
        void onValueChanged(float value, boolean fromUser);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float value = 0.5f;
    private OnValueChangeListener listener;
    private Shader progressShader;

    public ModernSlider(Context context) {
        this(context, null);
    }

    public ModernSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = Design.dp(getContext(), 220);
        int height = Design.dp(getContext(), 34);
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        float edge = Design.dp(getContext(), 10);
        progressShader = new LinearGradient(
                edge,
                0,
                Math.max(edge + 1, width - edge),
                0,
                Design.ACCENT,
                Design.CYAN,
                Shader.TileMode.CLAMP
        );
    }

    public float getValue() {
        return value;
    }

    public void setValue(float newValue) {
        value = Math.max(0f, Math.min(1f, newValue));
        invalidate();
    }

    public void setOnValueChangeListener(OnValueChangeListener value) {
        listener = value;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN
                || event.getAction() == MotionEvent.ACTION_MOVE
                || event.getAction() == MotionEvent.ACTION_UP) {
            getParent().requestDisallowInterceptTouchEvent(true);
            float edge = Design.dp(getContext(), 10);
            setValue((event.getX() - edge) / Math.max(1f, getWidth() - edge * 2f));
            if (listener != null) {
                listener.onValueChanged(value, true);
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                performClick();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float edge = Design.dp(getContext(), 10);
        float centerY = getHeight() / 2f;
        float trackHeight = Design.dp(getContext(), 5);
        float end = getWidth() - edge;

        paint.setColor(Design.SURFACE_HOVER);
        canvas.drawRoundRect(edge, centerY - trackHeight / 2f, end, centerY + trackHeight / 2f,
                trackHeight, trackHeight, paint);

        float thumbX = edge + (end - edge) * value;
        paint.setShader(progressShader);
        canvas.drawRoundRect(edge, centerY - trackHeight / 2f, thumbX, centerY + trackHeight / 2f,
                trackHeight, trackHeight, paint);

        paint.setShader(null);
        paint.setColor(Design.TEXT);
        paint.setShadowLayer(Design.dp(getContext(), 7), 0, 0, 0x887C5CFF);
        canvas.drawCircle(thumbX, centerY, Design.dp(getContext(), 7), paint);
        paint.clearShadowLayer();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Design.dp(getContext(), 2));
        paint.setColor(Design.ACCENT);
        canvas.drawCircle(thumbX, centerY, Design.dp(getContext(), 9), paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
