package com.redz.8bpbot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.view.View;
import android.view.WindowManager;

public class OverlayView extends View {
    private final WindowManager windowManager;
    private final WindowManager.LayoutParams params;
    private final Paint paint;
    private float[] currentLine = new float[]{0, 0, 0, 0};

    public OverlayView(Context context) {
        super(context);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );

        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
    }

    public void show() {
        windowManager.addView(this, params);
    }

    public void hide() {
        windowManager.removeView(this);
    }

    public void updateLine(float[] coords) {
        this.currentLine = coords;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (currentLine != null && currentLine.length == 4) {
            canvas.drawLine(currentLine[0], currentLine[1], currentLine[2], currentLine[3], paint);
        }
    }
}
