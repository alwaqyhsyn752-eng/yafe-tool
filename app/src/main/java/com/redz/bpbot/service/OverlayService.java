package com.redz.bpbot.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.redz.bpbot.physics.PhysicsEngine;
import com.redz.bpbot.ui.CustomOverlayView;
import com.redz.bpbot.vision.AutoTableDetector;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class OverlayService extends Service {

    public static final String ACTION_START = "com.redz.bpbot.START";
    public static final String EXTRA_RESULT_DATA = "rd";

    private static final String CHANNEL_ID = "yafe-tool";
    private static final int NOTIF_ID = 1107;

    /** ألوان الخطوط الثانوية — تطابق الصورة: أحمر/أزرق/أصفر/أخضر/برتقالي/بنفسجي */
    private static final int[] SECONDARY_COLORS = {
            0xFFFF3B30, // red
            0xFF42A5F5, // blue
            0xFFFFEB3B, // yellow
            0xFF66BB6A, // green
            0xFFFF9800, // orange
            0xFFAB47BC  // purple
    };

    private WindowManager wm;
    private CustomOverlayView overlay;
    private WindowManager.LayoutParams overlayLp;

    private MediaProjection projection;
    private VirtualDisplay vDisplay;
    private ImageReader reader;

    private int screenW, screenH, screenDpi;
    private final Handler loop = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;
    private volatile boolean busy = false;
    private int frameTick = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenW = dm.widthPixels;
        screenH = dm.heightPixels;
        screenDpi = dm.densityDpi;
        startForegroundInternal();
        buildOverlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        Intent data = intent.getParcelableExtra(EXTRA_RESULT_DATA);
        if (data == null) return START_STICKY;

        MediaProjectionManager mgr =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mgr == null) return START_STICKY;

        if (projection != null) {
            try { projection.stop(); } catch (Exception ignored) {}
            projection = null;
        }

        try {
            projection = mgr.getMediaProjection(Activity.RESULT_OK, data);
        } catch (Exception e) {
            return START_STICKY;
        }
        if (projection == null) return START_STICKY;

        if (Build.VERSION.SDK_INT >= 34) {
            try {
                projection.registerCallback(new MediaProjection.Callback() {
                    @Override public void onStop() { }
                }, loop);
            } catch (Exception ignored) {}
        }

        startCapture();
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

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIF_ID, n);
        }
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

    private void startCapture() {
        if (projection == null || running) return;
        reader = ImageReader.newInstance(screenW, screenH, PixelFormat.RGBA_8888, 2);
        try {
            vDisplay = projection.createVirtualDisplay(
                    "yafe-capture", screenW, screenH, screenDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.getSurface(), null, loop);
        } catch (Exception e) {
            return;
        }
        running = true;
        reader.setOnImageAvailableListener(r -> {
            Image img = null;
            try {
                img = r.acquireLatestImage();
                if (img == null) return;
                if (busy) return;
                Bitmap bmp = toBitmap(img);
                if (bmp != null) {
                    busy = true;
                    processFrame(bmp);
                    busy = false;
                }
            } catch (Exception ignored) {
            } finally {
                if (img != null) img.close();
            }
        }, loop);
    }

    private Bitmap toBitmap(Image img) {
        Image.Plane[] planes = img.getPlanes();
        if (planes.length == 0) return null;
        ByteBuffer buf = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPad = rowStride - pixelStride * screenW;
        Bitmap bmp = Bitmap.createBitmap(
                screenW + rowPad / pixelStride, screenH, Bitmap.Config.ARGB_8888);
        buf.rewind();
        bmp.copyPixelsFromBuffer(buf);
        Bitmap cropped = Bitmap.createBitmap(bmp, 0, 0, screenW, screenH);
        if (cropped != bmp) bmp.recycle();
        return cropped;
    }

    private void processFrame(Bitmap bmp) {
        try {
            // خفّف الحمل — عالج كل إطار ثالث
            frameTick++;
            if (frameTick % 3 != 0) return;

            AutoTableDetector.Table t = AutoTableDetector.detect(bmp, null);
            if (!t.detected || overlay == null) {
                if (overlay != null) overlay.clear();
                return;
            }

            PointF cue = t.cueBall != null ? t.cueBall
                    : new PointF(t.bounds.centerX(),
                                 t.bounds.bottom - t.ballRadius * 3f);

            // خزّن الطاولة في الـ overlay
            overlay.setTable(t.bounds, t.pockets, cue,
                    t.ballRadius, t.pocketRadius);

            List<CustomOverlayView.AimPath> paths = new ArrayList<>();
            List<Integer> glows = new ArrayList<>();

            // 1) خط أبيض أساسي — من الكرة البيضاء إلى مركز الكتلة/الحواف
            PhysicsEngine.Trace white = PhysicsEngine.trace(
                    cue, 0f, -1f, t.bounds, t.pockets,
                    t.ballRadius, t.pocketRadius, 0);
            if (white.path.size() >= 2) {
                paths.add(new CustomOverlayView.AimPath(
                        white.path, Color.WHITE, true, -1));
            }

            // 2) لكل جيب: حاول العثور على مسار بانك واحد أو اثنين
            for (int i = 0; i < t.pockets.size(); i++) {
                PhysicsEngine.Trace tr = PhysicsEngine.traceToPocket(
                        cue, i, t.bounds, t.pockets,
                        t.ballRadius, t.pocketRadius, 2);
                if (tr != null && tr.path.size() >= 2) {
                    int color = SECONDARY_COLORS[i % SECONDARY_COLORS.length];
                    boolean isPrimary = tr.bounces <= 1;
                    paths.add(new CustomOverlayView.AimPath(
                            tr.path, color, false, i));
                    if (tr.bounces <= 1 && !glows.contains(i)) {
                        glows.add(i);
                    }
                }
            }

            overlay.setPaths(paths, glows);

        } catch (Exception ignored) {
        } finally {
            bmp.recycle();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        running = false;
        if (vDisplay != null) {
            try { vDisplay.release(); } catch (Exception ignored) {}
            vDisplay = null;
        }
        if (projection != null) {
            try { projection.stop(); } catch (Exception ignored) {}
            projection = null;
        }
        if (reader != null) { reader.close(); reader = null; }
        if (overlay != null && wm != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
}
