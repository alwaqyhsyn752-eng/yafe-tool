package com.redz.bpbot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public final class CustomOverlayView extends View {

    public static final class AimPath {
        public final List<PointF> points;
        public final int color;
        public final int bounces;
        public final int pocketIndex;
        public AimPath(List<PointF> p, int c, int b, int pi) {
            points = p; color = c; bounces = b; pocketIndex = pi;
        }
    }

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowMid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowInner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pocketPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RectF tableBounds;
    private List<PointF> pockets = new ArrayList<>();
    private PointF cueBall;
    private float ballRadius = 12f;
    private float pocketRadius = 22f;

    private final List<AimPath> paths = new ArrayList<>();
    private final List<Integer> glowingPockets = new ArrayList<>();

    public CustomOverlayView(Context c) { super(c); init(); }
    public CustomOverlayView(Context c, AttributeSet a) { super(c, a); init(); }
    public CustomOverlayView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        setWillNotDraw(false);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        glowOuter.setStyle(Paint.Style.FILL);
        glowOuter.setColor(0x4000FF66);
        glowMid.setStyle(Paint.Style.FILL);
        glowMid.setColor(0x8000FF66);
        glowInner.setStyle(Paint.Style.FILL);
        glowInner.setColor(0xFF00FF66);

        pocketPaint.setStyle(Paint.Style.STROKE);
        pocketPaint.setStrokeWidth(3f);
        pocketPaint.setColor(0x66FFFFFF);

        ghostPaint.setStyle(Paint.Style.STROKE);
        ghostPaint.setStrokeWidth(4f);
        ghostPaint.setColor(0xFFFFFFFF);

        cuePaint.setStyle(Paint.Style.STROKE);
        cuePaint.setStrokeWidth(4f);
        cuePaint.setColor(0xFFFFFFFF);

        dotPaint.setStyle(Paint.Style.FILL);
    }

    public void setTable(RectF bounds, List<PointF> p, PointF cue,
                         float ballR, float pocketR) {
        this.tableBounds = bounds;
        this.pockets = (p != null) ? p : new ArrayList<>();
        this.cueBall = cue;
        this.ballRadius = ballR;
        this.pocketRadius = pocketR;
        postInvalidate();
    }

    public void setPaths(List<AimPath> newPaths, List<Integer> glow) {
        paths.clear();
        if (newPaths != null) paths.addAll(newPaths);
        glowingPockets.clear();
        if (glow != null) glowingPockets.addAll(glow);
        postInvalidate();
    }

    public void clear() {
        tableBounds = null;
        pockets.clear();
        cueBall = null;
        paths.clear();
        glowingPockets.clear();
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (tableBounds == null) return;

        for (AimPath ap : paths) {
            if (ap.points == null || ap.points.size() < 2) continue;
            linePaint.setColor(ap.color);
            float w = 8f - Math.min(4f, ap.bounces * 1.2f);
            linePaint.setStrokeWidth(w);
            linePaint.setAlpha(230);

            for (int i = 0; i < ap.points.size() - 1; i++) {
                PointF a = ap.points.get(i);
                PointF b = ap.points.get(i + 1);
                canvas.drawLine(a.x, a.y, b.x, b.y, linePaint);
            }

            dotPaint.setColor(ap.color);
            for (int i = 1; i < ap.points.size() - 1; i++) {
                PointF p = ap.points.get(i);
                canvas.drawCircle(p.x, p.y, 6f, dotPaint);
            }
        }

        for (int i = 0; i < pockets.size(); i++) {
            PointF p = pockets.get(i);
            if (glowingPockets.contains(i)) {
                canvas.drawCircle(p.x, p.y, pocketRadius * 2.8f, glowOuter);
                canvas.drawCircle(p.x, p.y, pocketRadius * 1.8f, glowMid);
                canvas.drawCircle(p.x, p.y, pocketRadius * 1.0f, glowInner);
            } else {
                canvas.drawCircle(p.x, p.y, pocketRadius, pocketPaint);
            }
        }

        if (cueBall != null) {
            canvas.drawCircle(cueBall.x, cueBall.y, ballRadius * 1.6f, cuePaint);
            canvas.drawCircle(cueBall.x, cueBall.y, ballRadius * 0.5f, cuePaint);
        }
    }
}
