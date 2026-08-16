package com.nguyen.onyxmenu.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public final class BrandMarkView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path bolt = new Path();
    private Shader outerShader;
    private Shader boltShader;
    private float centerX;
    private float centerY;
    private float markRadius;

    public BrandMarkView(Context context) {
        this(context, null);
    }

    public BrandMarkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setContentDescription("Onyx logo");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = Design.dp(getContext(), 42);
        setMeasuredDimension(resolveSize(desired, widthMeasureSpec), resolveSize(desired, heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        centerX = width / 2f;
        centerY = height / 2f;
        markRadius = Math.min(width, height) * 0.47f;
        outerShader = new RadialGradient(
                centerX - markRadius * 0.35f,
                centerY - markRadius * 0.4f,
                markRadius * 1.45f,
                new int[]{Design.ACCENT_LIGHT, Design.ACCENT, 0xFF4930B8},
                null,
                Shader.TileMode.CLAMP
        );
        boltShader = new RadialGradient(
                centerX,
                centerY,
                markRadius,
                new int[]{0xFFFFFFFF, Design.CYAN},
                null,
                Shader.TileMode.CLAMP
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = centerX;
        float cy = centerY;
        float radius = markRadius;

        paint.setShader(outerShader);
        paint.setShadowLayer(Design.dp(getContext(), 8), 0, Design.dp(getContext(), 3), 0x667C5CFF);
        canvas.drawCircle(cx, cy, radius, paint);

        paint.clearShadowLayer();
        paint.setShader(null);
        paint.setColor(Design.INK_SOFT);
        canvas.drawCircle(cx, cy, radius * 0.64f, paint);

        bolt.reset();
        bolt.moveTo(cx - radius * 0.46f, cy + radius * 0.38f);
        bolt.lineTo(cx - radius * 0.08f, cy - radius * 0.55f);
        bolt.lineTo(cx + radius * 0.08f, cy - radius * 0.12f);
        bolt.lineTo(cx + radius * 0.5f, cy - radius * 0.32f);
        bolt.lineTo(cx + radius * 0.12f, cy + radius * 0.58f);
        bolt.lineTo(cx - radius * 0.04f, cy + radius * 0.15f);
        bolt.close();

        paint.setShader(boltShader);
        canvas.drawPath(bolt, paint);
        paint.setShader(null);
    }
}
