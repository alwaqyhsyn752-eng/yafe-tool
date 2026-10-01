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
