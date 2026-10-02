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

    /**
     * كشف الطاولة بطريقة أكثر تحفظاً:
     * - يقصر البحث على المنطقة الوسطى من الشاشة (يتجنب الـ UI)
     * - يبني histogram للأعمدة والصفوف ليجد الحدود الحقيقية للقماش
     */
    public static Table detect(Bitmap bmp, Table prev) {
        Table t = new Table();
        if (bmp == null) return t;

        int w = bmp.getWidth();
        int h = bmp.getHeight();

        // قص منطقة البحث: 10%-90% أفقياً، 18%-88% رأسياً
        // هذا يستبعد HUD اللعبة (اللاعبون/الأزرار) والجزء السفلي
        int searchLeft   = (int) (w * 0.05f);
        int searchRight  = (int) (w * 0.95f);
        int searchTop    = (int) (h * 0.18f);
        int searchBottom = (int) (h * 0.88f);

        int step = 3;

        // Histogram للأعمدة (X) والصفوف (Y)
        int[] colHits = new int[w];
        int[] rowHits = new int[h];

        for (int y = searchTop; y < searchBottom; y += step) {
            for (int x = searchLeft; x < searchRight; x += step) {
                if (isFelt(bmp.getPixel(x, y))) {
                    colHits[x]++;
                    rowHits[y]++;
                }
            }
        }

        // عتبة: عمود/صف يُعدّ "داخل الطاولة" إذا تجاوز 25% من أقصى قيمة
        int maxCol = 0, maxRow = 0;
        for (int x = searchLeft; x < searchRight; x++) if (colHits[x] > maxCol) maxCol = colHits[x];
        for (int y = searchTop; y < searchBottom; y++) if (rowHits[y] > maxRow) maxRow = rowHits[y];
        if (maxCol < 5 || maxRow < 5) return t;

        int colThresh = (int) (maxCol * 0.35f);
        int rowThresh = (int) (maxRow * 0.35f);

        int minX = -1, maxX = -1, minY = -1, maxY = -1;
        for (int x = searchLeft; x < searchRight; x++) {
            if (colHits[x] >= colThresh) { minX = x; break; }
        }
        for (int x = searchRight - 1; x >= searchLeft; x--) {
            if (colHits[x] >= colThresh) { maxX = x; break; }
        }
        for (int y = searchTop; y < searchBottom; y++) {
            if (rowHits[y] >= rowThresh) { minY = y; break; }
        }
        for (int y = searchBottom - 1; y >= searchTop; y--) {
            if (rowHits[y] >= rowThresh) { maxY = y; break; }
        }

        if (minX < 0 || maxX < 0 || minY < 0 || maxY < 0) return t;
        if (maxX - minX < w * 0.40f) return t;
        if (maxY - minY < h * 0.30f) return t;

        t.bounds.set(minX, minY, maxX, maxY);
        t.detected = true;

        float left = t.bounds.left, right = t.bounds.right;
        float top = t.bounds.top, bottom = t.bounds.bottom;
        float cx = (left + right) / 2f;

        // نصف قطر الجيب بحسب نسبة من عرض/ارتفاع الطاولة
        float pw = (right - left) * 0.032f;
        float ph = (bottom - top) * 0.048f;
        float pr = Math.max(pw, ph);

        // الجيوب الستة: 4 زوايا + 2 وسط
        t.pockets.add(new PointF(left  + pw * 0.2f, top    + ph * 0.2f));
        t.pockets.add(new PointF(cx,                top    + ph * 0.1f));
        t.pockets.add(new PointF(right - pw * 0.2f, top    + ph * 0.2f));
        t.pockets.add(new PointF(left  + pw * 0.2f, bottom - ph * 0.2f));
        t.pockets.add(new PointF(cx,                bottom - ph * 0.1f));
        t.pockets.add(new PointF(right - pw * 0.2f, bottom - ph * 0.2f));

        t.pocketRadius = pr * 1.9f;
        t.ballRadius = (right - left) / 62f;
        if (t.ballRadius < 6f) t.ballRadius = 6f;
        if (t.ballRadius > 20f) t.ballRadius = 20f;

        // كشف الكرة البيضاء داخل حدود الطاولة فقط
        t.cueBall = findCueBall(bmp, t.bounds, t.ballRadius);

        return t;
    }

    private static boolean isFelt(int c) {
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        // القماش الأزرق لـ 8 Ball Pool:
        // R منخفض نسبياً، B مرتفع، G أعلى من R بقليل
        boolean blue = (b > 95) && (b > r + 25) && (g > r + 5) && (b >= g - 40);
        // تركوازي (بعض النسخ)
        boolean teal = (g > 85 && b > 85 && g > r + 30 && b > r + 25);
        return blue || teal;
    }

    /**
     * ابحث عن الكرة البيضاء داخل مساحة الطاولة (وليس خارجها).
     * المنطقة: الثلث السفلي الأيسر من الطاولة.
     */
    private static PointF findCueBall(Bitmap bmp, RectF table, float ballR) {
        int xStart = (int) (table.left + table.width() * 0.05f);
        int xEnd   = (int) (table.left + table.width() * 0.55f);
        int yStart = (int) (table.bottom - table.height() * 0.45f);
        int yEnd   = (int) (table.bottom - 3);

        long sx = 0, sy = 0;
        int found = 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = 0, maxY = 0;
        int step = 2;

        for (int y = yStart; y < yEnd; y += step) {
            if (y < 0 || y >= bmp.getHeight()) continue;
            for (int x = xStart; x < xEnd; x += step) {
                if (x < 0 || x >= bmp.getWidth()) continue;
                int c = bmp.getPixel(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                // أبيض نقي، مع تشديد أكبر
                if (r > 230 && g > 230 && b > 230
                        && Math.abs(r - g) < 15 && Math.abs(g - b) < 15) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                    sx += x; sy += y; found++;
                }
            }
        }

        if (found < 6) return null;
        float bw = maxX - minX, bh = maxY - minY;
        // يجب أن تكون بحجم كرة معقول (ليس أكبر من 3 أضعاف نصف قطر الكرة)
        if (bw > ballR * 4 || bh > ballR * 4) return null;
        if (bw < 2 || bh < 2) return null;
        return new PointF((float) sx / found, (float) sy / found);
    }
}
