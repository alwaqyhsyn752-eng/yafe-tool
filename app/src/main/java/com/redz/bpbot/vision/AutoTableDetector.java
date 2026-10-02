package com.redz.bpbot.vision;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

public final class AutoTableDetector {

    private static final int   STRIDE          = 6;
    private static final float FELT_MIN_RATIO  = 0.28f;
    private static final float HUE_MIN         = 150f;
    private static final float HUE_MAX         = 260f;
    private static final float SAT_MIN         = 0.35f;
    private static final float VAL_MIN         = 0.18f;
    private static final float POCKET_R_RATIO  = 0.045f;

    public static final class Table {
        public RectF bounds      = new RectF();
        public final List<PointF> pockets = new ArrayList<>();
        public boolean detected  = false;
        public int     scanWidth = 0;
        public int     scanHeight = 0;
        public float   ballRadius   = 12f;
        public float   pocketRadius = 26f;
    }

    public static Table detect(Bitmap frame, Rect scanRegion) {
        Table t = new Table();
        if (frame == null || frame.isRecycled()) return t;

        int x0 = scanRegion != null ? Math.max(0, scanRegion.left)   : 0;
        int y0 = scanRegion != null ? Math.max(0, scanRegion.top)    : 0;
        int x1 = scanRegion != null ? Math.min(frame.getWidth(),  scanRegion.right)  : frame.getWidth();
        int y1 = scanRegion != null ? Math.min(frame.getHeight(), scanRegion.bottom) : frame.getHeight();

        if (x1 - x0 < 32 || y1 - y0 < 32) return t;

        int rowHits[] = new int[frame.getHeight()];
        int colHits[] = new int[frame.getWidth()];
        int feltCount = 0;
        int totalSampled = 0;
<<<<<<< HEAD

=======
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
        int[] pixelRow = new int[frame.getWidth()];

        for (int y = y0; y < y1; y += STRIDE) {
            frame.getPixels(pixelRow, 0, frame.getWidth(), 0, y, frame.getWidth(), 1);
            for (int x = x0; x < x1; x += STRIDE) {
                totalSampled++;
                int p = pixelRow[x];
                if (isFelt(p)) { feltCount++; rowHits[y]++; colHits[x]++; }
            }
        }

        if (totalSampled == 0) return t;
<<<<<<< HEAD
        float ratio = feltCount / (float) totalSampled;
        if (ratio < FELT_MIN_RATIO) return t;
=======
        if (feltCount / (float) totalSampled < FELT_MIN_RATIO) return t;
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))

        int top    = firstAbove(rowHits, 1, y0, y1);
        int bottom = lastAbove(rowHits, 1, y0, y1);
        int left   = firstAbove(colHits, 1, x0, x1);
        int right  = lastAbove(colHits, 1, x0, x1);

        if (top < 0 || bottom <= top || left < 0 || right <= left) return t;

        t.bounds.set(left, top, right, bottom);
        t.scanWidth  = x1 - x0;
        t.scanHeight = y1 - y0;
        computePockets(t);
        t.detected = true;
        return t;
    }

    private static void computePockets(Table t) {
        RectF b = t.bounds;
        float cy = b.centerY();
<<<<<<< HEAD
        float pocketR = Math.min(b.width(), b.height()) * POCKET_R_RATIO;

        t.pockets.clear();
        t.pockets.add(new PointF(b.left  + pocketR, b.top    + pocketR));
        t.pockets.add(new PointF(b.right - pocketR, b.top    + pocketR));
        t.pockets.add(new PointF(b.left  + pocketR, b.bottom - pocketR));
        t.pockets.add(new PointF(b.right - pocketR, b.bottom - pocketR));
        t.pockets.add(new PointF(b.left  + pocketR, cy));
        t.pockets.add(new PointF(b.right - pocketR, cy));

        t.pocketRadius = pocketR;
=======
        float pr = Math.min(b.width(), b.height()) * POCKET_R_RATIO;

        t.pockets.clear();
        t.pockets.add(new PointF(b.left  + pr, b.top    + pr));
        t.pockets.add(new PointF(b.right - pr, b.top    + pr));
        t.pockets.add(new PointF(b.left  + pr, b.bottom - pr));
        t.pockets.add(new PointF(b.right - pr, b.bottom - pr));
        t.pockets.add(new PointF(b.left  + pr, cy));
        t.pockets.add(new PointF(b.right - pr, cy));

        t.pocketRadius = pr;
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
        t.ballRadius   = Math.min(b.width(), b.height()) * 0.018f;
    }

    private static boolean isFelt(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8)  & 0xFF;
        int b =  color        & 0xFF;
<<<<<<< HEAD

        float[] hsv = new float[3];
        android.graphics.Color.RGBToHSV(r, g, b, hsv);
        float h = hsv[0], s = hsv[1], v = hsv[2];

        if (v < VAL_MIN || s < SAT_MIN) return false;
        return (h >= HUE_MIN && h <= HUE_MAX);
    }

    private static int firstAbove(int[] arr, int th, int from, int to) {
        for (int i = from; i < to; i++) if (arr[i] >= th) return i;
        return -1;
    }
    private static int lastAbove(int[] arr, int th, int from, int to) {
        for (int i = to - 1; i >= from; i--) if (arr[i] >= th) return i;
=======
        float[] hsv = new float[3];
        android.graphics.Color.RGBToHSV(r, g, b, hsv);
        if (hsv[2] < VAL_MIN || hsv[1] < SAT_MIN) return false;
        return (hsv[0] >= HUE_MIN && hsv[0] <= HUE_MAX);
    }

    private static int firstAbove(int[] a, int th, int from, int to) {
        for (int i = from; i < to; i++) if (a[i] >= th) return i;
        return -1;
    }
    private static int lastAbove(int[] a, int th, int from, int to) {
        for (int i = to - 1; i >= from; i--) if (a[i] >= th) return i;
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
        return -1;
    }
}
