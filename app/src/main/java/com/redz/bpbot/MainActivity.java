package com.redz.bpbot;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.redz.bpbot.service.OverlayService;

public final class MainActivity extends Activity {

<<<<<<< HEAD
    private static final int REQ_OVERLAY  = 1001;
    private static final int REQ_CAPTURE  = 1002;
=======
    private static final int REQ_OVERLAY = 1001;
    private static final int REQ_CAPTURE = 1002;
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))

    private MediaProjectionManager projectionMgr;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        projectionMgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 80, 40, 40);

        Button permBtn = new Button(this);
        permBtn.setText("1. Grant Overlay Permission");
        permBtn.setOnClickListener(v -> askOverlayPermission());
        root.addView(permBtn);

        Button startBtn = new Button(this);
        startBtn.setText("2. Start Overlay Service");
        startBtn.setOnClickListener(v -> askCapturePermission());
        root.addView(startBtn);

        setContentView(root);

<<<<<<< HEAD
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            askOverlayPermission();
        }
=======
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) askOverlayPermission();
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
    }

    private void askOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
<<<<<<< HEAD
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQ_OVERLAY);
        } else {
            Toast.makeText(this, "Overlay already granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void askCapturePermission() {
        if (projectionMgr == null) return;
        startActivityForResult(projectionMgr.createScreenCaptureIntent(), REQ_CAPTURE);
=======
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
        } else Toast.makeText(this, "Overlay already granted", Toast.LENGTH_SHORT).show();
    }

    private void askCapturePermission() {
        if (projectionMgr != null)
            startActivityForResult(projectionMgr.createScreenCaptureIntent(), REQ_CAPTURE);
>>>>>>> 4f92f72 (yafe-tool: full stack (Detector + Physics + Overlay + Service + Manifest))
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_CAPTURE && res == RESULT_OK && data != null) {
            Intent svc = new Intent(this, OverlayService.class);
            svc.putExtra(OverlayService.EXTRA_RESULT_CODE, res);
            svc.putExtra(OverlayService.EXTRA_RESULT_DATA, data);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
            else startService(svc);
            Toast.makeText(this, "Service starting...", Toast.LENGTH_SHORT).show();
        }
    }
}
