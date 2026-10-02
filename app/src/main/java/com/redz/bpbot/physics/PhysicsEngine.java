package com.redz.bpbot.physics;

import android.graphics.PointF;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

public final class PhysicsEngine {

    private static final float EPS = 1e-3f;
    private static final float MAX_RAY = 8000f;
    private static final float POCKET_CAPTURE = 0.95f;

    public static final class Trace {
        public final List<PointF> path = new ArrayList<>();
        public final List<PointF> bouncePoints = new ArrayList<>();
        public PointF ghost;
        public int pocketIndex = -1;
        public boolean reached = false;
        public int bounces = 0;
        public float angle;
    }

    public static Trace trace(PointF start, float dirX, float dirY,
                              RectF bounds, List<PointF> pockets,
                              float ballR, float pocketR, int maxBounces) {
        Trace tr = new Trace();
        tr.path.add(new PointF(start.x, start.y));
        tr.angle = (float) Math.atan2(dirY, dirX);

        float dx = dirX, dy = dirY;
        float n = (float) Math.hypot(dx, dy);
        if (n < EPS) return tr;
        dx /= n; dy /= n;

        float x = start.x, y = start.y, remaining = MAX_RAY;
        float lW = bounds.left + ballR;
        float rW = bounds.right - ballR;
        float tW = bounds.top + ballR;
        float bW = bounds.bottom - ballR;

        for (int b = 0; b <= maxBounces && remaining > EPS; b++) {
            int hit = firstPocket(x, y, dx, dy, pockets, pocketR, remaining);
            if (hit >= 0) {
                PointF p = pockets.get(hit);
                tr.path.add(new PointF(p.x, p.y));
                tr.pocketIndex = hit;
                tr.reached = true;
                tr.bounces = b;
                return tr;
            }

            float tL = (dx < -EPS) ? (lW - x) / dx : Float.MAX_VALUE;
            float tR = (dx > EPS) ? (rW - x) / dx : Float.MAX_VALUE;
            float tT = (dy < -EPS) ? (tW - y) / dy : Float.MAX_VALUE;
            float tB = (dy > EPS) ? (bW - y) / dy : Float.MAX_VALUE;

            float tHit = Math.min(Math.min(tL, tR), Math.min(tT, tB));
            if (tHit >= remaining || tHit == Float.MAX_VALUE) {
                x += dx * remaining; y += dy * remaining;
                tr.path.add(new PointF(x, y));
                tr.bounces = b;
                return tr;
            }

            x += dx * tHit; y += dy * tHit;
            remaining -= tHit;
            tr.path.add(new PointF(x, y));
            tr.bouncePoints.add(new PointF(x, y));
            if (b == 0) tr.ghost = new PointF(x, y);

            if (tHit == tL || tHit == tR) dx = -dx;
            if (tHit == tT || tHit == tB) dy = -dy;
        }
        tr.bounces = maxBounces;
        return tr;
    }

    public static List<Trace> raycastAll(PointF start, RectF bounds,
                                         List<PointF> pockets,
                                         float ballR, float pocketR,
                                         int rayCount, int maxBounces) {
        List<Trace> results = new ArrayList<>();
        boolean[] pocketHit = new boolean[pockets.size()];
        float angularStep = (float) (2 * Math.PI / rayCount);

        for (int i = 0; i < rayCount; i++) {
            float a = i * angularStep;
            float dx = (float) Math.cos(a);
            float dy = (float) Math.sin(a);
            Trace tr = trace(start, dx, dy, bounds, pockets,
                    ballR, pocketR, maxBounces);
            if (tr.reached) {
                if (tr.bounces <= 1 || !pocketHit[tr.pocketIndex]) {
                    results.add(tr);
                    if (tr.bounces <= 1) pocketHit[tr.pocketIndex] = true;
                }
            }
        }
        return results;
    }

    private static int firstPocket(float px, float py, float dx, float dy,
                                   List<PointF> pockets, float r, float maxT) {
        float bestT = Float.MAX_VALUE;
        int bestI = -1;
        float rCap = r * POCKET_CAPTURE;

        for (int i = 0; i < pockets.size(); i++) {
            PointF c = pockets.get(i);
            float ox = px - c.x, oy = py - c.y;
            float a = dx * dx + dy * dy;
            float bq = 2f * (ox * dx + oy * dy);
            float cq = ox * ox + oy * oy - rCap * rCap;
            float disc = bq * bq - 4f * a * cq;
            if (disc < 0f) continue;
            float sq = (float) Math.sqrt(disc);
            float t1 = (-bq - sq) / (2f * a);
            float t2 = (-bq + sq) / (2f * a);
            float t = (t1 > EPS) ? t1 : (t2 > EPS ? t2 : -1f);
            if (t > EPS && t < bestT && t <= maxT) { bestT = t; bestI = i; }
        }
        return bestI;
    }
}
