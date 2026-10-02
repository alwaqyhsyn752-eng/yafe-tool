package com.redz.bpbot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public final class CustomOverlayView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowInner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pocketPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RectF tableBounds;
    private List<PointF> pockets;
    private PointF cueBall;
    private float ballRadius = 12f;
    private float pocketRadius = 22f;
    private int glowPocketIndex = -1;

    private List<PointF> pathPoints;
    private List<Integer> segmentColors;

    public CustomOverlayView(Context c) { super(c); init(); }
    public CustomOverlayView(Context c, AttributeSet a) { super(c, a); init(); }
    public CustomOverlayView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        setWillNotDraw(false);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        glowOuter.setStyle(Paint.Style.FILL);
        glowOuter.setColor(0x5500FF66);

        glowInner.setStyle(Paint.Style.FILL);
        glowInner.setColor(0xFF00FF66);

        pocketPaint.setStyle(Paint.Style.STROKE);
        pocketPaint.setStrokeWidth(3f);
        pocketPaint.setColor(0x99FFFFFF);

        ghostPaint.setStyle(Paint.Style.STROKE);
        ghostPaint.setStrokeWidth(3f);
        ghostPaint.setColor(0xEEFFFFFF);

        cuePaint.setStyle(Paint.Style.STROKE);
        cuePaint.setStrokeWidth(3f);
        cuePaint.setColor(0xFFFFFFFF);
    }

    public void setTable(RectF bounds, List<PointF> pockets, PointF cue,
                         float ballR, float pocketR) {
        this.tableBounds = bounds;
        this.pockets = pockets;
        this.cueBall = cue;
        this.ballRadius = ballR;
        this.pocketRadius = pocketR;
        postInvalidate();
    }

    public void setPath(List<PointF> pts, List<Integer> colors, int glowIdx) {
        this.pathPoints = pts;
        this.segmentColors = colors;
        this.glowPocketIndex = glowIdx;
        postInvalidate();
    }

    public void clear() {
        tableBounds = null;
        pockets = null;
        cueBall = null;
        pathPoints = null;
        segmentColors = null;
        glowPocketIndex = -1;
        postInvalidate();
    }

    private int colorForSegment(int idx) {
        switch (idx) {
            case 0: return 0xFFFFFFFF;
            case 1: return 0xFF42A5F5;
            case 2: return 0xFFFFA726;
            case 3: return 0xFF66BB6A;
            case 4: return 0xFFFFEE58;
            case 5: return 0xFFAB47BC;
            case 6: return 0xFFFF7043;
            default: return 0xFFEF5350;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (tableBounds == null) return;

        if (pockets != null) {
            for (int i = 0; i < pockets.size(); i++) {
                PointF p = pockets.get(i);
                if (i == glowPocketIndex) {
                    canvas.drawCircle(p.x, p.y, pocketRadius * 2.2f, glowOuter);
                    canvas.drawCircle(p.x, p.y, pocketRadius * 1.2f, glowInner);
                } else {
                    canvas.drawCircle(p.x, p.y, pocketRadius, pocketPaint);
                }
            }
        }

        if (pathPoints != null && segmentColors != null
                && pathPoints.size() >= 2) {
            int segs = Math.min(pathPoints.size() - 1, segmentColors.size());
            for (int i = 0; i < segs; i++) {
                linePaint.setColor(colorForSegment(segmentColors.get(i)));
                linePaint.setStrokeWidth(i == 0 ? 9f : 6f);
                PointF a = pathPoints.get(i);
                PointF b = pathPoints.get(i + 1);
                canvas.drawLine(a.x, a.y, b.x, b.y, linePaint);
            }

            PointF ghost = pathPoints.get(1);
            if (ghost != null && segs > 1) {
                canvas.drawCircle(ghost.x, ghost.y, ballRadius * 1.6f, ghostPaint);
            }
        }

        if (cueBall != null) {
            canvas.drawCircle(cueBall.x, cueBall.y, ballRadius * 1.5f, cuePaint);
        }
    }
}
