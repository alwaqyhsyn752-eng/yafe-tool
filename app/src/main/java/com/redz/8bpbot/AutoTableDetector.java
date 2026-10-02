package com.redz.8bpbot;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;

public class AutoTableDetector {

    private static final int SAMPLE_STRIDE = 8;
    private static final float ROW_MIN_DENSITY = 0.40f;
    private static final float COL_MIN_DENSITY = 0.40f;
    private static final float MIN_WIDTH_FRAC  = 0.30f;
    private static final float MIN_HEIGHT_FRAC = 0.20f;
    private static final float MIN_TOTAL_COVERAGE = 0.05f;

    public static class Result {
        public boolean found;
        public RectF   tableBounds = new RectF();   
        public PointF[] pockets    = new PointF[6];
        public float   feltRatio;                   

        public float left()   { return tableBounds.left;   }
        public float top()    { return tableBounds.top;    }
        public float right()  { return tableBounds.right;  }
        public float bottom() { return tableBounds.bottom; }

        public PointF[] corners() {
            return new PointF[] {
                    new PointF(tableBounds.left,  tableBounds.top),
                    new PointF(tableBounds.right, tableBounds.top),
                    new PointF(tableBounds.right, tableBounds.bottom),
                    new PointF(tableBounds.left,  tableBounds.bottom)
            };
        }
    }

    public Result detect(Bitmap frame) {
        Result r = new Result();
        if (frame == null || frame.getWidth() < 100 || frame.getHeight() < 100) {
            return r;
        }

        final int w = frame.getWidth();
        final int h = frame.getHeight();

        final int cols = w / SAMPLE_STRIDE;
        final int rows = h / SAMPLE_STRIDE;
        final boolean[] mask = new boolean[cols * rows];
        int feltCount = 0;

        for (int ry = 0; ry < rows; ry++) {
            for (int rx = 0; rx < cols; rx++) {
                int px = rx * SAMPLE_STRIDE;
                int py = ry * SAMPLE_STRIDE;
                boolean felt = isFeltColor(frame.getPixel(px, py));
                mask[ry * cols + rx] = felt;
                if (felt) feltCount++;
            }
        }

        final int totalSamples = cols * rows;
        r.feltRatio = totalSamples > 0 ? (float) feltCount / totalSamples : 0f;

        if (feltCount < totalSamples * MIN_TOTAL_COVERAGE) {
            r.found = false;
            return r;
        }

        int topRow    = -1;
        int bottomRow = -1;

        for (int ry = 0; ry < rows; ry++) {
            int cnt  = 0;
            int base = ry * cols;
            for (int rx = 0; rx < cols; rx++) {
                if (mask[base + rx]) cnt++;
            }
            float density = (float) cnt / cols;
            if (density >= ROW_MIN_DENSITY) {
                if (topRow < 0) topRow = ry;
                bottomRow = ry;
            }
        }

        int leftCol  = -1;
        int rightCol = -1;

        if (topRow >= 0 && bottomRow > topRow) {
            final int span = bottomRow - topRow + 1;
            for (int rx = 0; rx < cols; rx++) {
                int cnt = 0;
                for (int ry = topRow; ry <= bottomRow; ry++) {
                    if (mask[ry * cols + rx]) cnt++;
                }
                float density = (float) cnt / span;
                if (density >= COL_MIN_DENSITY) {
                    if (leftCol < 0) leftCol = rx;
                    rightCol = rx;
                }
            }
        }

        if (topRow < 0 || bottomRow < 0 || leftCol < 0 || rightCol < 0) {
            r.found = false;
            return r;
        }

        float left   = leftCol   * SAMPLE_STRIDE;
        float top    = topRow    * SAMPLE_STRIDE;
        float right  = rightCol  * SAMPLE_STRIDE + SAMPLE_STRIDE;
        float bottom = bottomRow * SAMPLE_STRIDE + SAMPLE_STRIDE;

        if ((right - left)  < w * MIN_WIDTH_FRAC || (bottom - top) < h * MIN_HEIGHT_FRAC) {
            r.found = false;
            return r;
        }

        r.tableBounds.set(left, top, right, bottom);
        r.found = true;

        computePockets(r);
        return r;
    }

    private boolean isFeltColor(int c) {
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8)  & 0xFF;
        int b =  c        & 0xFF;

        boolean green = g > 80 && g < 210 && g > r + 25 && g > b + 15;
        boolean blue = b > 100 && b < 230 && b > r + 30 && b > g + 10 && r < 140;
        boolean teal = g > 100 && b > 110 && Math.abs(g - b) < 60 && g > r + 30 && b > r + 40 && r < 130;

        return green || blue || teal;
    }

    private void computePockets(Result r) {
        final float l  = r.tableBounds.left;
        final float t  = r.tableBounds.top;
        final float rr = r.tableBounds.right;
        final float bb = r.tableBounds.bottom;

        final float w = rr - l;
        final float h = bb - t;
        final float midX = l + w / 2f;

        final float ix = w * 0.045f;
        final float iy = h * 0.045f;

        r.pockets[0] = new PointF(l + ix,    t + iy);   
        r.pockets[1] = new PointF(midX,      t + 2f);   
        r.pockets[2] = new PointF(rr - ix,   t + iy);   
        r.pockets[3] = new PointF(l + ix,    bb - iy);  
        r.pockets[4] = new PointF(midX,      bb - 2f);  
        r.pockets[5] = new PointF(rr - ix,   bb - iy);  
    }
}
