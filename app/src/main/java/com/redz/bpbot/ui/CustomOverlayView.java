package com.redz.bpbot.ui;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.redz.bpbot.physics.PhysicsEngine;

import java.util.ArrayList;
import java.util.List;

public final class CustomOverlayView extends View {

<<<<<<< HEAD
    private static final int   MAIN_COLOR   = 0xFFFFFFFF;
=======
    private static final int   MAIN_COLOR = 0xFFFFFFFF;
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
    private static final int[] BOUNCE_COLORS = {
            0xFF4FC3F7, 0xFFFF9800, 0xFFFFEB3B,
            0xFF00E676, 0xFF9C27B0, 0xFFFF1744
    };

<<<<<<< HEAD
    private final Paint mainPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bouncePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pocketDot    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint framePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float density = 1f;

=======
    private final Paint mainPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bouncePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pocketDot   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint framePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float density = 1f;
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
    private List<PointF>  path = new ArrayList<>();
    private List<Integer> segColor = new ArrayList<>();
    private PointF ghostCenter;
    private List<PointF> pockets = new ArrayList<>();
    private int targetPocketIdx = -1;
<<<<<<< HEAD
    private float ballR = 12f;
    private float pocketR = 26f;
=======
    private float ballR = 12f, pocketR = 26f;
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
    private RectF tableBounds = new RectF();

    public CustomOverlayView(Context c) { super(c); init(); }
    public CustomOverlayView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public CustomOverlayView(Context c, @Nullable AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        density = getResources().getDisplayMetrics().density;

        mainPaint.setStyle(Paint.Style.STROKE);
        mainPaint.setStrokeCap(Paint.Cap.ROUND);
        mainPaint.setStrokeJoin(Paint.Join.ROUND);
        mainPaint.setStrokeWidth(5f * density);
        mainPaint.setColor(MAIN_COLOR);
        mainPaint.setMaskFilter(new BlurMaskFilter(2.5f * density, BlurMaskFilter.Blur.NORMAL));

        bouncePaint.setStyle(Paint.Style.STROKE);
        bouncePaint.setStrokeCap(Paint.Cap.ROUND);
        bouncePaint.setStrokeJoin(Paint.Join.ROUND);
        bouncePaint.setStrokeWidth(4f * density);
        bouncePaint.setAlpha(210);
        bouncePaint.setMaskFilter(new BlurMaskFilter(2f * density, BlurMaskFilter.Blur.NORMAL));

        ghostPaint.setStyle(Paint.Style.STROKE);
        ghostPaint.setStrokeWidth(2.5f * density);
        ghostPaint.setColor(0xCCFFFFFF);

        pocketDot.setStyle(Paint.Style.FILL);
        pocketDot.setColor(0xFFFF3D00);

        glowPaint.setStyle(Paint.Style.FILL);

        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(1.5f * density);
        framePaint.setColor(0x66FFFFFF);
    }

<<<<<<< HEAD
    public void setTable(RectF bounds, List<PointF> pk, float br, float pr) {
        this.tableBounds = (bounds != null) ? new RectF(bounds) : new RectF();
        this.pockets = (pk != null) ? new ArrayList<>(pk) : new ArrayList<>();
        this.ballR = br > 0 ? br : this.ballR;
        this.pocketR = pr > 0 ? pr : this.pocketR;
=======
    public void setTable(RectF b, List<PointF> pk, float br, float pr) {
        tableBounds = (b != null) ? new RectF(b) : new RectF();
        pockets = (pk != null) ? new ArrayList<>(pk) : new ArrayList<>();
        ballR = br > 0 ? br : ballR;
        pocketR = pr > 0 ? pr : pocketR;
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
        postInvalidateOnAnimation();
    }

    public void updateFromTrace(PhysicsEngine.Trace tr) {
        if (tr == null) { clear(); return; }
<<<<<<< HEAD
        this.path     = new ArrayList<>(tr.path);
        this.segColor = new ArrayList<>(tr.segmentColor);
        this.ghostCenter = (tr.ghostBallCenter != null)
                ? new PointF(tr.ghostBallCenter.x, tr.ghostBallCenter.y) : null;
        this.targetPocketIdx = tr.pocketIndex;
=======
        path = new ArrayList<>(tr.path);
        segColor = new ArrayList<>(tr.segmentColor);
        ghostCenter = (tr.ghostBallCenter != null)
                ? new PointF(tr.ghostBallCenter.x, tr.ghostBallCenter.y) : null;
        targetPocketIdx = tr.pocketIndex;
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
        postInvalidateOnAnimation();
    }

    public void clear() {
        path.clear(); segColor.clear();
        ghostCenter = null; targetPocketIdx = -1;
        postInvalidateOnAnimation();
    }

<<<<<<< HEAD
    @Override
    protected void onDraw(Canvas c) {
=======
    @Override protected void onDraw(Canvas c) {
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
        super.onDraw(c);
        if (!tableBounds.isEmpty()) c.drawRect(tableBounds, framePaint);
        drawPockets(c);
        drawOverlayGraphics(c);
    }

    private void drawPockets(Canvas c) {
        for (int i = 0; i < pockets.size(); i++) {
            PointF pk = pockets.get(i);
<<<<<<< HEAD
            boolean isHit = (i == targetPocketIdx);
            if (isHit) {
=======
            if (i == targetPocketIdx) {
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
                float r = pocketR * 3.4f;
                RadialGradient grad = new RadialGradient(
                        pk.x, pk.y, r,
                        new int[]{0xE600FF88, 0x6600FF88, 0x0000FF88},
                        new float[]{0f, 0.45f, 1f},
                        Shader.TileMode.CLAMP);
                glowPaint.setShader(grad);
                c.drawCircle(pk.x, pk.y, r, glowPaint);
                glowPaint.setShader(null);
            }
            c.drawCircle(pk.x, pk.y, 5f * density, pocketDot);
        }
    }

    private void drawOverlayGraphics(Canvas c) {
        if (path.size() < 2) return;
<<<<<<< HEAD

        for (int i = 0; i < path.size() - 1; i++) {
            PointF a = path.get(i);
            PointF b = path.get(i + 1);
            if (a == null || b == null) continue;

            int col = (segColor.size() > i) ? segColor.get(i) : 0;
            if (col <= 1) {
                c.drawLine(a.x, a.y, b.x, b.y, mainPaint);
            } else {
=======
        for (int i = 0; i < path.size() - 1; i++) {
            PointF a = path.get(i), b = path.get(i + 1);
            if (a == null || b == null) continue;
            int col = (segColor.size() > i) ? segColor.get(i) : 0;
            if (col <= 1) c.drawLine(a.x, a.y, b.x, b.y, mainPaint);
            else {
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
                bouncePaint.setColor(BOUNCE_COLORS[Math.min(col - 2, BOUNCE_COLORS.length - 1)]);
                c.drawLine(a.x, a.y, b.x, b.y, bouncePaint);
            }
        }
<<<<<<< HEAD

        if (ghostCenter != null) {
            c.drawCircle(ghostCenter.x, ghostCenter.y, ballR, ghostPaint);
        }
=======
        if (ghostCenter != null) c.drawCircle(ghostCenter.x, ghostCenter.y, ballR, ghostPaint);
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
    }
}
