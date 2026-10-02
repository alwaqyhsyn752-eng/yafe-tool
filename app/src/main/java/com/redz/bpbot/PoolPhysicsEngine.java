package com.redz.bpbot;

import android.graphics.PointF;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;

public class PoolPhysicsEngine {

    public static class Segment {
        public float x1, y1, x2, y2;
        public int   bounceIndex;
        public Segment(float x1, float y1, float x2, float y2, int b) {
            this.x1 = x1; this.y1 = y1;
            this.x2 = x2; this.y2 = y2;
            this.bounceIndex = b;
        }
        public float length() {
            return (float) Math.hypot(x2 - x1, y2 - y1);
        }
    }

    public static class Trace {
        public List<Segment> segments = new ArrayList<>();
        public boolean reachedPocket = false;
        public int     pocketIndex    = -1;
        public float   pocketHitX, pocketHitY;
        public float   totalLength;

        public void addLength(Segment s) { totalLength += s.length(); }
    }

    public static class PocketHit {
        public boolean isTargeting  = false;
        public int     pocketIndex  = -1;
        public float   hitX, hitY;
    }

    private RectF    table;         
    private PointF[] pockets;       
    private float    ballRadius;    
    private float    pocketRadius;  
    private int      maxBounces;    

    public PoolPhysicsEngine(RectF table, PointF[] pockets, float ballRadius, float pocketRadius, int maxBounces) {
        this.table        = table;
        this.pockets      = pockets;
        this.ballRadius   = ballRadius;
        this.pocketRadius = pocketRadius;
        this.maxBounces   = maxBounces;
    }

    public void setTable(RectF table, PointF[] pockets) {
        this.table   = table;
        this.pockets = pockets;
    }

    public void setRadii(float ballRadius, float pocketRadius) {
        this.ballRadius   = ballRadius;
        this.pocketRadius = pocketRadius;
    }

    public void setMaxBounces(int n) { this.maxBounces = n; }

    public Trace trace(float startX, float startY, float dirX, float dirY) {
        Trace result = new Trace();
        if (table == null || pockets == null || pockets.length == 0) return result;

        float len = (float) Math.hypot(dirX, dirY);
        if (len < 1e-6f) return result;
        float dx = dirX / len;
        float dy = dirY / len;

        final float lx = table.left   + ballRadius;
        final float rx = table.right  - ballRadius;
        final float ty = table.top    + ballRadius;
        final float by = table.bottom - ballRadius;

        if (lx >= rx || ty >= by) return result;

        float cx = startX;
        float cy = startY;

        for (int bounce = 0; bounce <= maxBounces; bounce++) {
            float tWall = Float.MAX_VALUE;
            int   wall  = 0; 

            if (dx < -1e-6f) {
                float t = (lx - cx) / dx;
                if (t > 1e-4f && t < tWall) { tWall = t; wall = 1; }
            }
            if (dx >  1e-6f) {
                float t = (rx - cx) / dx;
                if (t > 1e-4f && t < tWall) { tWall = t; wall = 2; }
            }
            if (dy < -1e-6f) {
                float t = (ty - cy) / dy;
                if (t > 1e-4f && t < tWall) { tWall = t; wall = 3; }
            }
            if (dy >  1e-6f) {
                float t = (by - cy) / dy;
                if (t > 1e-4f && t < tWall) { tWall = t; wall = 4; }
            }

            if (wall == 0) break;   

            float ex = cx + dx * tWall;
            float ey = cy + dy * tWall;

            PocketEntry pe = firstPocketAlongRay(cx, cy, dx, dy, tWall);

            if (pe != null) {
                float px = cx + dx * pe.t;
                float py = cy + dy * pe.t;
                Segment seg = new Segment(cx, cy, px, py, bounce);
                result.segments.add(seg);
                result.addLength(seg);
                result.reachedPocket = true;
                result.pocketIndex   = pe.index;
                result.pocketHitX    = px;
                result.pocketHitY    = py;
                return result;
            }

            Segment seg = new Segment(cx, cy, ex, ey, bounce);
            result.segments.add(seg);
            result.addLength(seg);

            if (wall == 1 || wall == 2) dx = -dx;
            else                        dy = -dy;

            cx = ex + dx * 0.5f;
            cy = ey + dy * 0.5f;
        }
        return result;
    }

    public Trace traceAngle(float startX, float startY, double angleRad) {
        return trace(startX, startY, (float) Math.cos(angleRad), (float) Math.sin(angleRad));
    }

    public PocketHit checkTargeting(float startX, float startY, float dirX, float dirY) {
        Trace t = trace(startX, startY, dirX, dirY);
        PocketHit h = new PocketHit();
        if (t.reachedPocket) {
            h.isTargeting = true;
            h.pocketIndex = t.pocketIndex;
            h.hitX = t.pocketHitX;
            h.hitY = t.pocketHitY;
        }
        return h;
    }

    public int targetingPocketIndex(float startX, float startY, float dirX, float dirY) {
        Trace t = trace(startX, startY, dirX, dirY);
        return t.reachedPocket ? t.pocketIndex : -1;
    }

    public PocketHit checkTargetingAngle(float startX, float startY, double angleRad) {
        return checkTargeting(startX, startY, (float) Math.cos(angleRad), (float) Math.sin(angleRad));
    }

    private static class PocketEntry {
        int   index;
        float t;   
    }

    private PocketEntry firstPocketAlongRay(float ox, float oy, float dx, float dy, float tMax) {
        PocketEntry best = null;
        float bestT = tMax;
        final float r2 = pocketRadius * pocketRadius;

        for (int i = 0; i < pockets.length; i++) {
            PointF p = pockets[i];
            if (p == null) continue;

            float fx = ox - p.x;
            float fy = oy - p.y;

            float b = fx * dx + fy * dy;                 
            float c = fx * fx + fy * fy - r2;            

            float disc = b * b - c;
            if (disc < 0f) continue;

            float sq = (float) Math.sqrt(disc);
            float t1 = -b - sq;
            float t2 = -b + sq;

            float t;
            if (t1 > 1e-4f)      t = t1;
            else if (t2 > 1e-4f) t = t2;   
            else                 continue; 

            if (t < bestT) {
                bestT = t;
                if (best == null) best = new PocketEntry();
                best.index = i;
                best.t     = t;
            }
        }
        return best;
    }
}
