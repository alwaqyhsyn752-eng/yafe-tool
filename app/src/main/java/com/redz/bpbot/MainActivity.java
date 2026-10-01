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
