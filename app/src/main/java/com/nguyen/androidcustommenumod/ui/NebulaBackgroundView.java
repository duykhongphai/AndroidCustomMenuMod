package com.nguyen.androidcustommenumod.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public final class NebulaBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] stars = {
            0.08f, 0.12f, 0.18f, 0.31f, 0.27f, 0.08f, 0.35f, 0.22f,
            0.43f, 0.13f, 0.52f, 0.28f, 0.62f, 0.09f, 0.72f, 0.21f,
            0.83f, 0.12f, 0.93f, 0.29f, 0.13f, 0.49f, 0.31f, 0.58f,
            0.47f, 0.44f, 0.67f, 0.55f, 0.81f, 0.46f, 0.91f, 0.62f,
            0.07f, 0.73f, 0.23f, 0.86f, 0.39f, 0.75f, 0.58f, 0.91f,
            0.71f, 0.78f, 0.86f, 0.91f, 0.96f, 0.76f
    };
    private ValueAnimator animator;
    private float phase;
    private Shader backgroundShader;
    private Shader purpleGlowShader;
    private Shader cyanGlowShader;

    public NebulaBackgroundView(Context context) {
        this(context, null);
    }

    public NebulaBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(12000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            phase = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        backgroundShader = new LinearGradient(
                0,
                0,
                width,
                height,
                new int[]{0xFF080B13, 0xFF0B1020, 0xFF080B13},
                null,
                Shader.TileMode.CLAMP
        );
        purpleGlowShader = new RadialGradient(
                width * 0.82f,
                height * 0.14f,
                width * 0.6f,
                new int[]{0x387C5CFF, 0x087C5CFF, 0x007C5CFF},
                null,
                Shader.TileMode.CLAMP
        );
        cyanGlowShader = new RadialGradient(
                width * 0.06f,
                height * 0.62f,
                width * 0.48f,
                new int[]{0x202DE2E6, 0x052DE2E6, 0x002DE2E6},
                null,
                Shader.TileMode.CLAMP
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }

        paint.setAlpha(255);
        paint.setShader(backgroundShader);
        canvas.drawRect(0, 0, width, height, paint);

        float pulse = 0.82f + 0.18f * (float) Math.sin(phase * Math.PI * 2);
        paint.setAlpha(Math.round(255 * pulse));
        paint.setShader(purpleGlowShader);
        canvas.drawCircle(width * 0.82f, height * 0.14f, width * 0.6f, paint);

        paint.setAlpha(Math.round(255 * (1.82f - pulse)));
        paint.setShader(cyanGlowShader);
        canvas.drawCircle(width * 0.06f, height * 0.62f, width * 0.48f, paint);

        paint.setAlpha(255);
        paint.setShader(null);
        paint.setStrokeWidth(1f);
        paint.setColor(0x0FFFFFFF);
        int grid = Design.dp(getContext(), 40);
        for (int x = 0; x < width; x += grid) {
            canvas.drawLine(x, 0, x, height, paint);
        }
        for (int y = 0; y < height; y += grid) {
            canvas.drawLine(0, y, width, y, paint);
        }

        for (int index = 0; index < stars.length; index += 2) {
            float twinkle = 0.35f + 0.65f * Math.abs((float) Math.sin(
                    phase * Math.PI * 2 + index * 0.61f
            ));
            paint.setColor((Math.round(50 * twinkle) << 24) | 0x00FFFFFF);
            canvas.drawCircle(
                    stars[index] * width,
                    stars[index + 1] * height,
                    Design.dp(getContext(), 1.1f),
                    paint
            );
        }
    }
}
