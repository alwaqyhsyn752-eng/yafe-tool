package com.redz.bpbot.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.redz.bpbot.physics.PhysicsEngine;
import com.redz.bpbot.ui.CustomOverlayView;
import com.redz.bpbot.util.TableConfig;

import java.util.ArrayList;
import java.util.List;

public final class OverlayService extends Service {

    public static final String ACTION_START = "com.redz.bpbot.START";
    public static final String EXTRA_RESULT_DATA = "rd";

    private static final String CHANNEL_ID = "yafe-tool";
    private static final int NOTIF_ID = 1107;

    private static final int[] COLORS = {
            0xFFFF3B30, 0xFF42A5F5, 0xFFFFEB3B, 0xFF66BB6A,
            0xFFFF9800, 0xFFAB47BC, 0xFFEF5350, 0xFF29B6F6,
            0xFF9CCC65, 0xFFFFCA28, 0xFFEC407A, 0xFF26C6DA
    };

    private WindowManager wm;
    private CustomOverlayView overlay;
    private WindowManager.LayoutParams overlayLp;
    private TableConfig config;

    private final Handler loop = new Handler(Looper.getMainLooper());
    private Runnable drawer;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        config = TableConfig.load(this);
        startForegroundInternal();
        buildOverlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        config = TableConfig.load(this);
        if (!config.isSet) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startDrawing();
        return START_STICKY;
    }

    private void startForegroundInternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "yafe-tool", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        Notification n = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("yafe-tool")
                .setContentText("Aim overlay active")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF_ID, n,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else {
                startForeground(NOTIF_ID, n);
            }
        } catch (Exception ignored) {}
    }

    private void buildOverlay() {
        overlay = new CustomOverlayView(this);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        overlayLp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        overlayLp.gravity = Gravity.TOP | Gravity.START;
        try { wm.addView(overlay, overlayLp); } catch (Exception ignored) {}
    }

    private void startDrawing() {
        if (drawer != null) loop.removeCallbacks(drawer);
        drawer = new Runnable() {
            @Override public void run() {
                try { drawOnce(); } catch (Exception ignored) {}
                loop.postDelayed(this, 500);
            }
        };
        loop.post(drawer);
    }

    private void drawOnce() {
        if (!config.isSet || overlay == null) return;

        RectF bounds = new RectF(
                config.left, config.top, config.right, config.bottom);
        List<PointF> pockets = config.pockets();
        float br = config.ballRadius();
        float pr = config.pocketRadius();

        PointF cue = new PointF(
                bounds.left + bounds.width() * 0.25f,
                bounds.centerY());

        overlay.setTable(bounds, pockets, cue, br, pr);

        List<PhysicsEngine.Trace> traces = PhysicsEngine.raycastAll(
                cue, bounds, pockets, br, pr, 72, 2);

        List<CustomOverlayView.AimPath> paths = new ArrayList<>();
        List<Integer> glows = new ArrayList<>();

        PhysicsEngine.Trace white = PhysicsEngine.trace(
                cue, 1f, 0f, bounds, pockets, br, pr, 0);
        if (white.path.size() >= 2) {
            paths.add(new CustomOverlayView.AimPath(
                    white.path, 0xFFFFFFFF, 0, -1));
        }

        int ci = 0;
        for (PhysicsEngine.Trace tr : traces) {
            if (tr.bounces == 0 && Math.abs(tr.angle) < 0.15f) continue;
            int col = COLORS[ci % COLORS.length]; ci++;
            paths.add(new CustomOverlayView.AimPath(
                    tr.path, col, tr.bounces, tr.pocketIndex));
            if (tr.pocketIndex >= 0 && !glows.contains(tr.pocketIndex)) {
                glows.add(tr.pocketIndex);
            }
        }

        overlay.setPaths(paths, glows);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        if (drawer != null) loop.removeCallbacks(drawer);
        if (overlay != null && wm != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
}
