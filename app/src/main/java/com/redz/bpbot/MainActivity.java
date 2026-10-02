package com.redz.bpbot;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 1001;
    private static final int REQ_CAPTURE = 1002;

    private MediaProjectionManager projectionMgr;
    private TextView status;
    private Intent pendingCapture;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        projectionMgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 100, 40, 40);

        TextView title = new TextView(this);
        title.setText("yafe-tool  \u2014  8 Ball Aim Assist");
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(14);
        status.setPadding(0, 30, 0, 30);
        root.addView(status);

        addBtn(root, "1. Grant Overlay Permission", v -> askOverlay());
        addBtn(root, "2. Grant Screen Capture", v -> askCapture());
        addBtn(root, "3. Start Overlay Service", v -> startOverlay());
        addBtn(root, "4. Stop Overlay Service", v -> stopOverlay());

        setContentView(root);

        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            status.setText("Status: overlay permission required");
        } else {
            status.setText("Status: ready");
        }
    }

    private void addBtn(LinearLayout p, String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        p.addView(b);
    }

    private void askOverlay() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQ_OVERLAY);
        } else {
            toast("Overlay already granted");
        }
    }

    private void askCapture() {
        if (projectionMgr == null) {
            toast("MediaProjection unavailable");
            return;
        }
        startActivityForResult(projectionMgr.createScreenCaptureIntent(), REQ_CAPTURE);
    }

    private void startOverlay() {
        if (pendingCapture == null) {
            toast("Grant screen capture first");
            askCapture();
            return;
        }
        Intent svc = new Intent(this, com.redz.bpbot.service.OverlayService.class);
        svc.setAction(com.redz.bpbot.service.OverlayService.ACTION_START);
        svc.putExtra(com.redz.bpbot.service.OverlayService.EXTRA_RESULT_DATA, pendingCapture);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
        else startService(svc);
        status.setText("Status: service starting");
    }

    private void stopOverlay() {
        Intent svc = new Intent(this, com.redz.bpbot.service.OverlayService.class);
        stopService(svc);
        status.setText("Status: service stopped");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OVERLAY) {
            if (Build.VERSION.SDK_INT >= 23 && Settings.canDrawOverlays(this)) {
                status.setText("Status: overlay granted");
            } else {
                status.setText("Status: overlay denied");
            }
        } else if (req == REQ_CAPTURE) {
            if (res == RESULT_OK && data != null) {
                pendingCapture = data;
                status.setText("Status: capture granted, starting");
                startOverlay();
            } else {
                status.setText("Status: capture denied");
            }
        }
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}
