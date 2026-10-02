package com.redz.bpbot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class AimOverlayView extends View {

    private static final int COLOR_TABLE_EDGE    = 0x8000FF64; 
    private static final int COLOR_POCKET_IDLE   = 0xC800C85A; 
    private static final int COLOR_POCKET_INNER  = 0x8CB4FFC8; 
    private static final int COLOR_POCKET_DOT    = 0xB400DC5A;
    private static final int COLOR_POCKET_HIT    = 0xFF00FF5A; 
    private static final int COLOR_POCKET_HIT_DOT= 0xFF96FFAA;
    private static final int COLOR_AIM_GLOW      = 0x78FFFFFF;
    private static final int COLOR_AIM_CORE      = 0xFFFFFFFF;
    private static final int COLOR_CUE_RING      = 0xFFFFFFFF;
    private static final int COLOR_TARGET_RING   = 0xFF00C8FF;
    private static final int COLOR_GHOST_RING    = 0xFFFFEB00;
    private static final int COLOR_GHOST_FILL    = 0x46FFFF00;
    private static final int COLOR_IMPACT_DOT    = 0xFFFF5050;

    private static final int[] BOUNCE_COLORS = {
            0xFF2196F3, 
            0xFFFF9800, 
            0xFF4CAF50, 
            0xFFFFEB3B, 
            0xFF9C27B0  
    };

    private final Paint tableOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pocketOuter  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pocketInner  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pocketGlow   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pocketDot    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint aimGlow      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint aimCore      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cueRing      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetRing   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostRing    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostFill    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint impactDot    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RectF    tableBounds;
    private PointF[] pockets;
    private float    ballRadius   = 22f;
    private float    pocketRadius = 32f;

    private PoolPhysicsEngine.Trace trace;
    private int     targetedPocket   = -1;
    private PointF  cueBallCenter;
    private PointF  targetBallCenter;
    private PointF  ghostBallCenter;
    private PointF  aimStart;
    private PointF  aimEnd;
    private int     highlightPocket  = -1;   

    public AimOverlayView(Context c) { super(c); initPaints(); }

    public AimOverlayView(Context c, AttributeSet a) {
        super(c, a);
        initPaints();
    }

    public AimOverlayView(Context c, AttributeSet a, int s) {
        super(c, a, s);
        initPaints();
    }

    private void initPaints() {
        tableOutline.setStyle(Paint.Style.STROKE);
        tableOutline.setStrokeWidth(3f);
        tableOutline.setColor(COLOR_TABLE_EDGE);

        pocketOuter.setStyle(Paint.Style.STROKE);
        pocketOuter.setStrokeWidth(5f);
        pocketOuter.setColor(COLOR_POCKET_IDLE);

        pocketInner.setStyle(Paint.Style.STROKE);
        pocketInner.setStrokeWidth(2f);
        pocketInner.setColor(COLOR_POCKET_INNER);

        pocketGlow.setStyle(Paint.Style.FILL);

        pocketDot.setStyle(Paint.Style.FILL);
        pocketDot.setColor(COLOR_POCKET_DOT);

        segmentPaint.setStyle(Paint.Style.STROKE);
        segmentPaint.setStrokeCap(Paint.Cap.ROUND);

        aimGlow.setStyle(Paint.Style.STROKE);
        aimGlow.setStrokeCap(Paint.Cap.ROUND);
        aimGlow.setStrokeWidth(14f);
        aimGlow.setColor(COLOR_AIM_GLOW);

        aimCore.setStyle(Paint.Style.STROKE);
        aimCore.setStrokeCap(Paint.Cap.ROUND);
        aimCore.setStrokeWidth(5f);
        aimCore.setColor(COLOR_AIM_CORE);

        cueRing.setStyle(Paint.Style.STROKE);
        cueRing.setStrokeWidth(5f);
        cueRing.setColor(COLOR_CUE_RING);

        targetRing.setStyle(Paint.Style.STROKE);
        targetRing.setStrokeWidth(5f);
        targetRing.setColor(COLOR_TARGET_RING);

        ghostRing.setStyle(Paint.Style.STROKE);
        ghostRing.setStrokeWidth(4f);
        ghostRing.setColor(COLOR_GHOST_RING);

        ghostFill.setStyle(Paint.Style.FILL);
        ghostFill.setColor(COLOR_GHOST_FILL);

        impactDot.setStyle(Paint.Style.FILL);
        impactDot.setColor(COLOR_IMPACT_DOT);

        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void setTable(RectF table, PointF[] pockets) {
        this.tableBounds = table;
        this.pockets     = pockets;
    }

    public void setRadii(float ballRadius, float pocketRadius) {
        this.ballRadius   = ballRadius;
        this.pocketRadius = pocketRadius;
    }

    public void setTrace(PoolPhysicsEngine.Trace trace) {
        this.trace = trace;
    }

    public void setHighlightPocket(int index) {
        this.targetedPocket = index;
    }

    public void setCueBall(PointF c)      { this.cueBallCenter = c; }
    public void setTargetBall(PointF c)   { this.targetBallCenter = c; }
    public void setGhostBall(PointF c)    { this.ghostBallCenter = c; }

    public void setAimSegment(PointF from, PointF to) {
        this.aimStart = from;
        this.aimEnd   = to;
    }

    public void clear() {
        trace = null;
        targetedPocket = -1;
        cueBallCenter = null;
        targetBallCenter = null;
        ghostBallCenter = null;
        aimStart = null;
        aimEnd = null;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (tableBounds != null) {
            canvas.drawRect(tableBounds, tableOutline);
        }

        drawPockets(canvas);

        if (trace != null && trace.segments != null) {
            drawTrace(canvas);
        }

        if (aimStart != null && aimEnd != null) {
            canvas.drawLine(aimStart.x, aimStart.y, aimEnd.x, aimEnd.y, aimGlow);
            canvas.drawLine(aimStart.x, aimStart.y, aimEnd.x, aimEnd.y, aimCore);
            drawArrow(canvas, aimStart.x, aimStart.y, aimEnd.x, aimEnd.y,
                    COLOR_AIM_CORE, 240);
        }

        if (ghostBallCenter != null) {
            canvas.drawCircle(ghostBallCenter.x, ghostBallCenter.y,
                    ballRadius + 4f, ghostRing);
            canvas.drawCircle(ghostBallCenter.x, ghostBallCenter.y,
                    ballRadius, ghostFill);
        }

        if (cueBallCenter != null) {
            canvas.drawCircle(cueBallCenter.x, cueBallCenter.y,
                    ballRadius + 6f, cueRing);
        }

        if (targetBallCenter != null) {
            canvas.drawCircle(targetBallCenter.x, targetBallCenter.y,
                    ballRadius + 8f, targetRing);
        }
    }

    private void drawPockets(Canvas canvas) {
        if (pockets == null) return;

        final int chosen = (targetedPocket >= 0) ? targetedPocket : highlightPocket;

        for (int i = 0; i < pockets.length; i++) {
            PointF p = pockets[i];
            if (p == null) continue;

            final boolean isTarget = (i == chosen);

            if (isTarget) {
                final float outerR = pocketRadius + 30f;
                RadialGradient rg = new RadialGradient(
                        p.x, p.y, outerR,
                        new int[] {
                                Color.argb(230, 0, 255, 100),
                                Color.argb(140, 0, 220,  80),
                                Color.argb(  0, 0, 200,  60)
                        },
                        new float[] { 0f, 0.55f, 1f },
                        Shader.TileMode.CLAMP);
                pocketGlow.setShader(rg);
                canvas.drawCircle(p.x, p.y, outerR, pocketGlow);
                pocketGlow.setShader(null);

                pocketOuter.setColor(COLOR_POCKET_HIT);
                pocketOuter.setStrokeWidth(8f);
                canvas.drawCircle(p.x, p.y, pocketRadius, pocketOuter);

                pocketInner.setColor(COLOR_POCKET_INNER);
                canvas.drawCircle(p.x, p.y, pocketRadius - 8f, pocketInner);

                pocketDot.setColor(COLOR_POCKET_HIT_DOT);
                canvas.drawCircle(p.x, p.y, 12f, pocketDot);
            } else {
                pocketOuter.setColor(COLOR_POCKET_IDLE);
                pocketOuter.setStrokeWidth(5f);
                canvas.drawCircle(p.x, p.y, pocketRadius, pocketOuter);

                pocketInner.setColor(COLOR_POCKET_INNER);
                canvas.drawCircle(p.x, p.y, pocketRadius - 8f, pocketInner);

                pocketDot.setColor(COLOR_POCKET_DOT);
                canvas.drawCircle(p.x, p.y, 6f, pocketDot);
            }
        }
    }

    private void drawTrace(Canvas canvas) {
        for (int i = 0; i < trace.segments.size(); i++) {
            PoolPhysicsEngine.Segment s = trace.segments.get(i);
            int color = BOUNCE_COLORS[s.bounceIndex % BOUNCE_COLORS.length];
            int alpha = Math.max(80, 230 - s.bounceIndex * 35);
            int argb  = (color & 0x00FFFFFF) | (alpha << 24);

            segmentPaint.setColor(argb);
            segmentPaint.setStrokeWidth(Math.max(3f, 5f - s.bounceIndex * 0.5f));

            canvas.drawLine(s.x1, s.y1, s.x2, s.y2, segmentPaint);

            if (s.length() > 30f) {
                drawArrow(canvas, s.x1, s.y1, s.x2, s.y2, argb, alpha);
            }
        }

        if (trace.reachedPocket) {
            canvas.drawCircle(trace.pocketHitX, trace.pocketHitY, 9f, impactDot);
        }
    }

    private void drawArrow(Canvas canvas, float x1, float y1, float x2, float y2, int color, int alpha) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len < 24f) return;

        float nx  = dx / len;
        float ny  = dy / len;
        float head = 18f;
        float wing = 10f;

        float hx = x2 - nx * head;
        float hy = y2 - ny * head;

        Path p = new Path();
        p.moveTo(x2, y2);
        p.lineTo(hx - ny * wing, hy + nx * wing);
        p.lineTo(hx + ny * wing, hy - nx * wing);
        p.close();

        arrowPaint.setColor((color & 0x00FFFFFF) | (alpha << 24));
        canvas.drawPath(p, arrowPaint);
    }
}
