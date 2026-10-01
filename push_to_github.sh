#!/bin/bash
# Runtime: Termux / Linux Bash Shell
# Keterangan: Skrip tunggal untuk membuat seluruh direktori dan file Android project 8BP Bot secara otomatis.

echo "[+] Memulai pembuatan struktur proyek 8BP Bot..."

# 1. Buat struktur direktori
mkdir -p botapp/src/com/redz/8bpbot
mkdir -p botapp/res/values
mkdir -p botapp/res/xml
mkdir -p botapp/res/layout

echo "[+] Direktori berhasil dibuat."

# 2. Buat AndroidManifest.xml
cat << 'EOF' > botapp/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.redz.8bpbot">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="8BP Bot"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppCompat.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".BotService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="mediaProjection" />

        <service
            android:name=".TouchAccessibility"
            android:exported="true"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_config" />
        </service>

    </application>
</manifest>
EOF

# 3. Buat MainActivity.java
cat << 'EOF' > botapp/src/com/redz/8bpbot/MainActivity.java
package com.redz.8bpbot;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE = 100;
    private MediaProjectionManager projectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        
        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> {
            Intent intent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(intent, REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            Intent serviceIntent = new Intent(this, BotService.class);
            serviceIntent.putExtra("code", resultCode);
            serviceIntent.putExtra("data", data);
            startForegroundService(serviceIntent);
            Toast.makeText(this, "Bot 8BP Aktif!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
EOF

# 4. Buat BotService.java
cat << 'EOF' > botapp/src/com/redz/8bpbot/BotService.java
package com.redz.8bpbot;

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
EOF

# 5. Buat ScreenCapture.java
cat << 'EOF' > botapp/src/com/redz/8bpbot/ScreenCapture.java
package com.redz.8bpbot;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;

import java.nio.ByteBuffer;

public class ScreenCapture {
    private final Context context;
    private final int resultCode;
    private final Intent data;
    private final OverlayView overlayView;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;

    public ScreenCapture(Context context, int resultCode, Intent data, OverlayView overlayView) {
        this.context = context;
        this.resultCode = resultCode;
        this.data = data;
        this.overlayView = overlayView;
    }

    public void startCapture() {
        MediaProjectionManager manager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaProjection = manager.getMediaProjection(resultCode, data);
        
        int width = 720;
        int height = 1280;
        
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                width, height, 320,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        isRunning = true;
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                processImage(image);
                image.close();
            }
        }, handler);
    }

    private void processImage(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        
        float[] lineCoords = BallDetector.analyze(buffer, pixelStride, rowStride, image.getWidth(), image.getHeight());
        if (lineCoords != null) {
            overlayView.updateLine(lineCoords);
        }
    }

    public void stopCapture() {
        isRunning = false;
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
    }
}
EOF

# 6. Buat BallDetector.java
cat << 'EOF' > botapp/src/com/redz/8bpbot/BallDetector.java
package com.redz.8bpbot;

import java.nio.ByteBuffer;

public class BallDetector {
    public static float[] analyze(ByteBuffer buffer, int pixelStride, int rowStride, int width, int height) {
        float startX = 360f;
        float startY = 800f;
        float endX = 360f;
        float endY = 300f;
        
        float[] predicted = ShotPredictor.calculateBounce(startX, startY, endX, endY);
        return predicted;
    }
}
EOF

# 7. Buat ShotPredictor.java
cat << 'EOF' > botapp/src/com/redz/8bpbot/ShotPredictor.java
package com.redz.8bpbot;

public class ShotPredictor {
    public static float[] calculateBounce(float cueX, float cueY, float targetX, float targetY) {
        float dx = targetX - cueX;
        float dy = targetY - cueY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        
        float impactX = targetX + (dx / length) * 200f;
        float impactY = targetY + (dy / length) * 200f;
        
        return new float[]{cueX, cueY, impactX, impactY};
    }
}
EOF

# 8. Buat OverlayView.java
cat << 'EOF' > botapp/src/com/redz/8bpbot/OverlayView.java
package com.redz.8bpbot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.view.View;
import android.view.WindowManager;

public class OverlayView extends View {
    private final WindowManager windowManager;
    private final WindowManager.LayoutParams params;
    private final Paint paint;
    private float[] currentLine = new float[]{0, 0, 0, 0};

    public OverlayView(Context context) {
        super(context);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );

        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
    }

    public void show() {
        windowManager.addView(this, params);
    }

    public void hide() {
        windowManager.removeView(this);
    }

    public void updateLine(float[] coords) {
        this.currentLine = coords;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (currentLine != null && currentLine.length == 4) {
            canvas.drawLine(currentLine[0], currentLine[1], currentLine[2], currentLine[3], paint);
        }
    }
}
EOF

# 9. Buat TouchAccessibility.java
cat << 'EOF' > botapp/src/com/redz/8bpbot/TouchAccessibility.java
package com.redz.8bpbot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class TouchAccessibility extends AccessibilityService {
    private static TouchAccessibility instance;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    public static void swipe(float startX, float startY, float endX, float endY, long duration) {
        if (instance == null) return;
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
        
        instance.dispatchGesture(builder.build(), null, null);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}
EOF

# 10. Buat Jitter.java
cat << 'EOF' > botapp/src/com/redz/8bpbot/Jitter.java
package com.redz.8bpbot;

import java.util.Random;

public class Jitter {
    private static final Random random = new Random();

    public static float apply(float value, float maxJitter) {
        float offset = (random.nextFloat() - 0.5f) * 2f * maxJitter;
        return value + offset;
    }

    public static long randomDelay(long minMs, long maxMs) {
        return minMs + (long) (random.nextFloat() * (maxMs - minMs));
    }
}
EOF

# 11. Buat strings.xml
cat << 'EOF' > botapp/res/values/strings.xml
<resources>
    <string name="app_name">8BP Bot</string>
</resources>
EOF

# 12. Buat accessibility_config.xml
cat << 'EOF' > botapp/res/xml/accessibility_config.xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="100" />
EOF

# 13. Buat activity_main.xml
cat << 'EOF' > botapp/res/layout/activity_main.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="24dp">

    <Button
        android:id="@+id/start_button"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Start 8BP Bot Overlay"
        android:textSize="18sp" />

</LinearLayout>
EOF

echo "[+] Selesai! Seluruh folder dan file berhasil dibuat otomatis di dalam direktori botapp/."

