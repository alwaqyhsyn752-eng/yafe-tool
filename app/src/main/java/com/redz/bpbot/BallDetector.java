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
