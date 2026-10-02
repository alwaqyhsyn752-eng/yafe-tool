package com.redz.bpbot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class BotService extends Service {
    private ScreenCapture screenCapture;
    private OverlayView overlayView;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new Notification.Builder(this, "8bp_channel")
                .setContentTitle("8BP Bot Running")
                .setContentText("Overlay dan deteksi aktif")
                .smallIcon(android.R.drawable.ic_menu_compass)
                .build();
        startForeground(1, notification);

        overlayView = new OverlayView(this);
        overlayView.show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int resultCode = intent.getIntExtra("code", 0);
            Intent data = intent.getParcelableExtra("data");
            screenCapture = new ScreenCapture(this, resultCode, data, overlayView);
            screenCapture.startCapture();
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel("8bp_channel", "Bot Service Channel", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenCapture != null) screenCapture.stopCapture();
        if (overlayView != null) overlayView.hide();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
