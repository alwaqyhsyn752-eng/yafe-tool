package com.redz.bpbot;

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
