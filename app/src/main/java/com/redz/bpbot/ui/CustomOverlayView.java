package com.redz.bpbot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public final class CustomOverlayView extends View {

    /** مسار واحد ملوّن مع إمكانية توهج الجيب */
    public static final class AimPath {
        public final List<PointF> points;
        public final int color;
        public final boolean primary;
        public final int pocketIndex;
        public AimPath(List<PointF> p, int c, boolean prim, int pi) {
            points = p; color = c; primary = prim; pocketIndex = pi;
        }
    }

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
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
        glowOuter.setColor(0x6600FF66);

        glowInner.setStyle(Paint.Style.FILL);
        glowInner.setColor(0xFF00FF66);

        pocketPaint.setStyle(Paint.Style.STROKE);
        pocketPaint.setStrokeWidth(3f);
        pocketPaint.setColor(0x88FFFFFF);

        ghostPaint.setStyle(Paint.Style.STROKE);
        ghostPaint.setStrokeWidth(3f);
        ghostPaint.setColor(0xEEFFFFFF);

        cuePaint.setStyle(Paint.Style.STROKE);
        cuePaint.setStrokeWidth(3f);
        cuePaint.setColor(0xFFFFFFFF);

        dotPaint.setStyle(Paint.Style.FILL);
    }

    public void setTable(RectF bounds, List<PointF> p, PointF cue,
                         float ballR, float pocketR) {
        this.tableBounds = bounds;
        if (p != null) this.pockets = p; else this.pockets = new ArrayList<>();
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

        // 1) رسم الجيوب (مع توهج أخضر لمن استُهدف)
        for (int i = 0; i < pockets.size(); i++) {
            PointF p = pockets.get(i);
            if (glowingPockets.contains(i)) {
                canvas.drawCircle(p.x, p.y, pocketRadius * 2.4f, glowOuter);
                canvas.drawCircle(p.x, p.y, pocketRadius * 1.2f, glowInner);
            } else {
                canvas.drawCircle(p.x, p.y, pocketRadius, pocketPaint);
            }
        }

        // 2) رسم كل المسارات
        for (AimPath ap : paths) {
            if (ap.points == null || ap.points.size() < 2) continue;
            linePaint.setColor(ap.color);
            linePaint.setStrokeWidth(ap.primary ? 10f : 6f);
            linePaint.setAlpha(ap.primary ? 255 : 200);

            for (int i = 0; i < ap.points.size() - 1; i++) {
                PointF a = ap.points.get(i);
                PointF b = ap.points.get(i + 1);
                canvas.drawLine(a.x, a.y, b.x, b.y, linePaint);
            }

            // دائرة عند نقطة ghost (أول ارتداد/اصطدام)
            if (ap.primary && ap.points.size() >= 2) {
                PointF g = ap.points.get(1);
                canvas.drawCircle(g.x, g.y, ballRadius * 1.7f, ghostPaint);
            }

            // نقاط عند كل ارتداد
            dotPaint.setColor(ap.color);
            for (int i = 1; i < ap.points.size() - 1; i++) {
                PointF p = ap.points.get(i);
                canvas.drawCircle(p.x, p.y, 7f, dotPaint);
            }
        }

        // 3) دائرة الكرة البيضاء
        if (cueBall != null) {
            canvas.drawCircle(cueBall.x, cueBall.y, ballRadius * 1.5f, cuePaint);
        }
    }
}
