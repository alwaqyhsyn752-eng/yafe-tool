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
        int yTop = (int) (h * 0.10f);
        int yBot = (int) (h * 0.92f);

        int step = 4;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = 0, maxY = 0;
        int count = 0;

        for (int y = yTop; y < yBot; y += step) {
            for (int x = 0; x < w; x += step) {
                int c = bmp.getPixel(x, y);
                if (isFelt(c)) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                    count++;
                }
            }
        }

        if (count < 200 || maxX - minX < w * 0.30f || maxY - minY < h * 0.25f) {
            return t;
        }

        t.bounds.set(minX, minY, maxX, maxY);
        t.detected = true;

        float left = t.bounds.left, right = t.bounds.right;
        float top = t.bounds.top, bottom = t.bounds.bottom;
        float cx = (left + right) / 2f;

        float pw = (right - left) * 0.030f;
        float ph = (bottom - top) * 0.045f;
        float pr = Math.max(pw, ph);

        t.pockets.add(new PointF(left,  top));
        t.pockets.add(new PointF(cx,    top));
        t.pockets.add(new PointF(right, top));
        t.pockets.add(new PointF(left,  bottom));
        t.pockets.add(new PointF(cx,    bottom));
        t.pockets.add(new PointF(right, bottom));

        t.pocketRadius = pr * 1.8f;
        t.ballRadius = (right - left) / 60f;
        if (t.ballRadius < 5f) t.ballRadius = 5f;

        t.cueBall = findCueBall(bmp, t.bounds, t.ballRadius);
        return t;
    }

    private static boolean isFelt(int c) {
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        // القماش الأزرق لـ 8 Ball Pool: أزرق تركوازي متوسط السطوع
        boolean blue = (b > 90) && (b > r + 20) && (g > r + 10) && (b >= g - 30);
        boolean teal = (g > 80 && b > 80 && g > r + 30 && b > r + 20);
        return blue || teal;
    }

    private static PointF findCueBall(Bitmap bmp, RectF table, float ballR) {
        // المنطقة السفلية اليسرى من الطاولة (موضع الكرة البيضاء)
        int xStart = (int) (table.left + table.width() * 0.10f);
        int xEnd   = (int) (table.left + table.width() * 0.45f);
        int yStart = (int) (table.bottom - table.height() * 0.35f);
        int yEnd   = (int) table.bottom;

        long sx = 0, sy = 0;
        int found = 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = 0, maxY = 0;
        int step = 3;

        for (int y = yStart; y < yEnd; y += step) {
            if (y < 0 || y >= bmp.getHeight()) continue;
            for (int x = xStart; x < xEnd; x += step) {
                if (x < 0 || x >= bmp.getWidth()) continue;
                int c = bmp.getPixel(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                if (r > 220 && g > 220 && b > 220
                        && Math.abs(r - g) < 20 && Math.abs(g - b) < 20) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                    sx += x; sy += y; found++;
                }
            }
        }

        if (found < 8) return null;
        float bw = maxX - minX, bh = maxY - minY;
        if (bw > ballR * 8 || bh > ballR * 8) return null;
        if (bw < 3 || bh < 3) return null;
        return new PointF((float) sx / found, (float) sy / found);
    }
}
