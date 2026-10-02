package com.redz.bpbot;

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
