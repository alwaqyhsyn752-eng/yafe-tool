package com.redz.bpbot.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

public final class TableConfig {

    private static final String PREF = "yafe_config";
    private static final String K_L = "left";
    private static final String K_T = "top";
    private static final String K_R = "right";
    private static final String K_B = "bottom";
    private static final String K_SET = "set";

    public float left, top, right, bottom;
    public boolean isSet;

    public static TableConfig load(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        TableConfig t = new TableConfig();
        t.left   = p.getFloat(K_L, 0);
        t.top    = p.getFloat(K_T, 0);
        t.right  = p.getFloat(K_R, 0);
        t.bottom = p.getFloat(K_B, 0);
        t.isSet  = p.getBoolean(K_SET, false);
        return t;
    }

    public void save(Context c) {
        SharedPreferences.Editor e =
                c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        e.putFloat(K_L, left);
        e.putFloat(K_T, top);
        e.putFloat(K_R, right);
        e.putFloat(K_B, bottom);
        e.putBoolean(K_SET, true);
        e.apply();
    }

    public void clear(Context c) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().clear().apply();
        isSet = false;
    }

    public float width()  { return right - left; }
    public float height() { return bottom - top; }
    public float centerX() { return (left + right) / 2f; }
    public float centerY() { return (top + bottom) / 2f; }

    public List<PointF> pockets() {
        List<PointF> p = new ArrayList<>();
        float pw = width() * 0.035f;
        float ph = height() * 0.050f;
        p.add(new PointF(left + pw, top + ph));
        p.add(new PointF(centerX(), top + ph * 0.5f));
        p.add(new PointF(right - pw, top + ph));
        p.add(new PointF(left + pw, bottom - ph));
        p.add(new PointF(centerX(), bottom - ph * 0.5f));
        p.add(new PointF(right - pw, bottom - ph));
        return p;
    }

    public float pocketRadius() {
        return Math.max(width() * 0.035f, height() * 0.05f) * 1.6f;
    }

    public float ballRadius() {
        return Math.max(8f, width() / 62f);
    }
}
