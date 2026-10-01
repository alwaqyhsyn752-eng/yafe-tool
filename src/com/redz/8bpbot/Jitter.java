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
