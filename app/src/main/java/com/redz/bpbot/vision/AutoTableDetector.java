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

        int yTop = (int) (h * 0.08f);
        int yBot = (int) (h * 0.94f);

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

        if (count < 200 || maxX - minX < w * 0.25f || maxY - minY < h * 0.20f) {
            return t;
        }

        t.bounds.set(minX, minY, maxX, maxY);
        t.detected = true;

        float left = t.bounds.left, right = t.bounds.right;
        float top = t.bounds.top, bottom = t.bounds.bottom;
        float cx = (left + right) / 2f;

        float pw = (right - left) * 0.022f;
        float ph = (bottom - top) * 0.035f;
        float pr = Math.max(pw, ph);

        t.pockets.add(new PointF(left, top));
        t.pockets.add(new PointF(cx, top));
        t.pockets.add(new PointF(right, top));
        t.pockets.add(new PointF(left, bottom));
        t.pockets.add(new PointF(cx, bottom));
        t.pockets.add(new PointF(right, bottom));

        t.pocketRadius = pr * 1.7f;
        t.ballRadius = (right - left) / 55f;

        t.cueBall = findCueBall(bmp, t.bounds, t.ballRadius);
        return t;
    }

    private static boolean isFelt(int c) {
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;

        boolean blue = (b > 80) && (b > r + 15) && (b >= g - 10);
        boolean green = (g > 70) && (g > r + 20) && (b < g + 30);
        boolean teal = (g > 65 && b > 65 && Math.abs(g - b) < 55 && g > r + 25);

        return blue || green || teal;
    }

    private static PointF findCueBall(Bitmap bmp, RectF table, float ballR) {
        int cx = (int) table.centerX();
        int startY = (int) (table.bottom - (table.height() * 0.35f));
        int endY = (int) table.bottom;
        int step = 3;

        long sx = 0, sy = 0;
        int found = 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = 0, maxY = 0;

        for (int y = startY; y < endY; y += step) {
            if (y < 0 || y >= bmp.getHeight()) continue;
            for (int x = (int) table.left; x < table.right; x += step) {
                if (x < 0 || x >= bmp.getWidth()) continue;
                int c = bmp.getPixel(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                if (r > 215 && g > 215 && b > 215
                        && Math.abs(r - g) < 25 && Math.abs(g - b) < 25) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                    sx += x; sy += y; found++;
                }
            }
        }

        if (found < 10) return null;
        float w = maxX - minX, h = maxY - minY;
        if (w > ballR * 6 || h > ballR * 6) return null;
        if (w < 3 || h < 3) return null;
        return new PointF((float) sx / found, (float) sy / found);
    }
}
