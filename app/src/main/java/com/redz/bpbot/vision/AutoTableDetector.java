package com.redz.bpbot.vision;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

public final class AutoTableDetector {

    public static final class Table {
        public boolean detected;
        public final RectF bounds = new RectF();
        public final List<PointF> pockets = new ArrayList<>();
        public PointF cueBall;
        public float ballRadius = 12f;
        public float pocketRadius = 22f;
    }

    public static Table detect(Bitmap bmp, Table prev) {
        Table t = new Table();
        if (bmp == null) return t;

        int w = bmp.getWidth();
        int h = bmp.getHeight();

        // ═══ 1) عيّنة لون القماش المرجعي من مركز الشاشة ═══
        int refR, refG, refB;
        {
            int cx = w / 2, cy = h / 2;
            long sr = 0, sg = 0, sb = 0;
            int n = 0;
            for (int y = cy - 40; y <= cy + 40; y += 8) {
                for (int x = cx - 40; x <= cx + 40; x += 8) {
                    if (x < 0 || y < 0 || x >= w || y >= h) continue;
                    int c = bmp.getPixel(x, y);
                    sr += (c >> 16) & 0xFF;
                    sg += (c >> 8) & 0xFF;
                    sb += c & 0xFF;
                    n++;
                }
            }
            refR = (int) (sr / Math.max(1, n));
            refG = (int) (sg / Math.max(1, n));
            refB = (int) (sb / Math.max(1, n));
        }

        // لو المركز ليس قماشاً (أزرق)، استخدم اللون النموذجي لـ 8 Ball Pool
        if (refB < 100 || refB <= refR + 20) {
            refR = 70; refG = 165; refB = 210;
        }

        // ═══ 2) مسح الصورة — قبول فقط ما يقارب اللون المرجعي ═══
        int yTop = (int) (h * 0.10f);
        int yBot = (int) (h * 0.95f);
        int step = 4;

        int[] colHits = new int[w];
        int[] rowHits = new int[h];
        int total = 0;

        final int TOL = 60;

        for (int y = yTop; y < yBot; y += step) {
            for (int x = 0; x < w; x += step) {
                int c = bmp.getPixel(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                if (Math.abs(r - refR) < TOL
                        && Math.abs(g - refG) < TOL
                        && Math.abs(b - refB) < TOL) {
                    colHits[x]++;
                    rowHits[y]++;
                    total++;
                }
            }
        }

        if (total < 500) return t;

        // ═══ 3) أكبر نطاق متصل أفقي (يتجاهل الشوائب المنعزلة) ═══
        int maxCol = 0, maxRow = 0;
        for (int x = 0; x < w; x++) if (colHits[x] > maxCol) maxCol = colHits[x];
        for (int y = 0; y < h; y++) if (rowHits[y] > maxRow) maxRow = rowHits[y];
        if (maxCol < 5 || maxRow < 5) return t;

        int colThresh = (int) (maxCol * 0.30f);
        int rowThresh = (int) (maxRow * 0.30f);

        int bestMinX = -1, bestMaxX = -1, curStart = -1, bestLen = 0;
        for (int x = 0; x < w; x++) {
            if (colHits[x] >= colThresh) {
                if (curStart < 0) curStart = x;
            } else if (curStart >= 0) {
                int len = x - curStart;
                if (len > bestLen) { bestLen = len; bestMinX = curStart; bestMaxX = x - 1; }
                curStart = -1;
            }
        }
        if (curStart >= 0) {
            int len = w - curStart;
            if (len > bestLen) { bestLen = len; bestMinX = curStart; bestMaxX = w - 1; }
        }

        int bestMinY = -1, bestMaxY = -1;
        curStart = -1; bestLen = 0;
        for (int y = 0; y < h; y++) {
            if (rowHits[y] >= rowThresh) {
                if (curStart < 0) curStart = y;
            } else if (curStart >= 0) {
                int len = y - curStart;
                if (len > bestLen) { bestLen = len; bestMinY = curStart; bestMaxY = y - 1; }
                curStart = -1;
            }
        }
        if (curStart >= 0) {
            int len = h - curStart;
            if (len > bestLen) { bestLen = len; bestMinY = curStart; bestMaxY = h - 1; }
        }

        if (bestMinX < 0 || bestMinY < 0) return t;
        if (bestMaxX - bestMinX < w * 0.40f) return t;
        if (bestMaxY - bestMinY < h * 0.25f) return t;

        t.bounds.set(bestMinX, bestMinY, bestMaxX, bestMaxY);
        t.detected = true;

        // ═══ 4) الجيوب الستة ═══
        float left = t.bounds.left, right = t.bounds.right;
        float top = t.bounds.top, bottom = t.bounds.bottom;
        float cx = (left + right) / 2f;
        float pw = (right - left) * 0.035f;
        float ph = (bottom - top) * 0.050f;

        t.pockets.add(new PointF(left,  top));
        t.pockets.add(new PointF(cx,    top));
        t.pockets.add(new PointF(right, top));
        t.pockets.add(new PointF(left,  bottom));
        t.pockets.add(new PointF(cx,    bottom));
        t.pockets.add(new PointF(right, bottom));

        t.pocketRadius = Math.max(pw, ph) * 1.6f;
        t.ballRadius = (right - left) / 62f;
        if (t.ballRadius < 6f)  t.ballRadius = 6f;
        if (t.ballRadius > 22f) t.ballRadius = 22f;

        t.cueBall = findCueBall(bmp, t.bounds, t.ballRadius);
        return t;
    }

    private static PointF findCueBall(Bitmap bmp, RectF table, float ballR) {
        // ابحث في النصف الأيسر من الطاولة (حيث توضع الكرة البيضاء افتراضياً)
        int xStart = (int) (table.left + table.width() * 0.05f);
        int xEnd   = (int) (table.left + table.width() * 0.55f);
        int yStart = (int) (table.top + table.height() * 0.15f);
        int yEnd   = (int) (table.bottom - table.height() * 0.15f);

        int step = 2;
        long sx = 0, sy = 0;
        int found = 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = 0, maxY = 0;

        // نطلب أولاً مجموعات متجاورة (ليس بكسلات متفرقة)
        for (int y = yStart; y < yEnd; y += step) {
            if (y < 0 || y >= bmp.getHeight()) continue;
            for (int x = xStart; x < xEnd; x += step) {
                if (x < 0 || x >= bmp.getWidth()) continue;
                int c = bmp.getPixel(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                if (r > 225 && g > 225 && b > 225
                        && Math.abs(r - g) < 18
                        && Math.abs(g - b) < 18
                        && Math.abs(r - b) < 18) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                    sx += x; sy += y; found++;
                }
            }
        }

        if (found < 12) return null;
        float bw = maxX - minX, bh = maxY - minY;
        if (bw > ballR * 4 || bh > ballR * 4) return null;
        if (bw < 3 || bh < 3) return null;
        return new PointF((float) sx / found, (float) sy / found);
    }
}
