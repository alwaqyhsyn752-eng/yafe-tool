package com.redz.bpbot.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.RectF;
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
import androidx.core.app.NotificationCompat;

import com.redz.bpbot.physics.PhysicsEngine;
import com.redz.bpbot.ui.CustomOverlayView;
import com.redz.bpbot.vision.AutoTableDetector;

import java.nio.ByteBuffer;

public final class OverlayService extends Service {

    public static final String EXTRA_RESULT_CODE = "rc";
    public static final String EXTRA_RESULT_DATA = "rd";
    private static final String CHANNEL_ID = "yafe-tool";
    private static final int    NOTIF_ID    = 1107;

    private WindowManager wm;
    private CustomOverlayView overlay;
    private WindowManager.LayoutParams overlayLp;

    private MediaProjection projection;
    private VirtualDisplay  vDisplay;
    private ImageReader     reader;

    private int screenW, screenH, screenDpi;
    private final Handler loop = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;

    @Override public void onCreate() {
        super.onCreate();
        startForegroundInternal();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        buildOverlay();

        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenW = dm.widthPixels; screenH = dm.heightPixels; screenDpi = dm.densityDpi;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        int rc = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent rd = intent.getParcelableExtra(EXTRA_RESULT_DATA);
        if (rc == 0 || rd == null) return START_STICKY;

        MediaProjectionManager mgr =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mgr != null) {
            projection = mgr.getMediaProjection(rc, rd);
            startCapture();
        }
        return START_STICKY;
    }

    private void startForegroundInternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "yafe-tool", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("yafe-tool")
                .setContentText("Aim overlay running")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();
        startForeground(NOTIF_ID, n);
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

        try { wm.addView(overlay, overlayLp); }
        catch (Exception ignored) {}
    }

    private void startCapture() {
        if (projection == null || running) return;

        reader = ImageReader.newInstance(screenW, screenH, PixelFormat.RGBA_8888, 2);
        vDisplay = projection.createVirtualDisplay(
                "yafe-capture", screenW, screenH, screenDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(), null, loop);

        running = true;
        reader.setOnImageAvailableListener(r -> {
            Image img = null;
            try {
                img = r.acquireLatestImage();
                if (img == null) return;
                Bitmap bmp = toBitmap(img);
                if (bmp != null) processFrame(bmp);
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
        int rowStride   = planes[0].getRowStride();
        int rowPad      = rowStride - pixelStride * screenW;

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
            AutoTableDetector.Table t = AutoTableDetector.detect(bmp, null);
            if (t.detected && overlay != null) {
                overlay.setTable(t.bounds, t.pockets, t.ballRadius, t.pocketRadius);

                PointF cue = new PointF(t.bounds.centerX(), t.bounds.bottom - t.ballRadius * 2f);
                PointF dir = new PointF(0f, -1f);

                PhysicsEngine.Trace tr = PhysicsEngine.trace(
                        cue, dir, new RectF(t.bounds), t.pockets,
                        t.ballRadius, t.pocketRadius);
                overlay.updateFromTrace(tr);
            } else if (overlay != null) {
                overlay.clear();
            }
        } catch (Exception ignored) {
        } finally {
            bmp.recycle();
        }
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        running = false;
        if (vDisplay != null) { vDisplay.release(); vDisplay = null; }
        if (projection != null) { projection.stop(); projection = null; }
        if (reader != null) { reader.close(); reader = null; }
        if (overlay != null && wm != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
}
