package com.nguyen.onyxpayload.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class OverlayView extends SurfaceView implements SurfaceHolder.Callback {
    private Paint paint;
    private float[] playerData;
    private int numPlayers;
    private int myTeam = 0;

    public OverlayView(Context context) {
        super(context);
        getHolder().addCallback(this);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setTextSize(30);
    }

    public void drawESP(int count, float[] data) {
        this.numPlayers = count;
        this.playerData = data;
        postInvalidate();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {}

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (playerData == null || numPlayers == 0) return;

        int screenW = getWidth();
        int screenH = getHeight();

        for (int i = 0; i < numPlayers; i++) {
            int idx = i * 9;
            int team = (int)playerData[idx];
            float hp = playerData[idx + 1];
            float maxHp = playerData[idx + 2];
            float sx = playerData[idx + 3];
            float sy = playerData[idx + 4];
            float hx = playerData[idx + 5];
            float hy = playerData[idx + 6];
            float cx = playerData[idx + 7];
            float cy = playerData[idx + 8];

            if (sx < 0 || sx > screenW || sy < 0 || sy > screenH) continue;

            float height = Math.abs(sy - hy);
            float width = height * 0.5f;
            float left = sx - width / 2;
            float top = hy;
            float right = sx + width / 2;
            float bottom = sy;

            paint.setColor(team == myTeam ? Color.GREEN : Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(left, top, right, bottom, paint);

            float hpPercent = Math.min(hp / maxHp, 1.0f);
            paint.setColor(hpPercent > 0.5f ? Color.GREEN : Color.RED);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(left, top - 20, left + (right - left) * hpPercent, top - 5, paint);
        }
    }
}