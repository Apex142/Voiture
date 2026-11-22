package com.realisticvehicles.core.math;

/**
 * Scalar math utilities and numerical stability guards for physics simulation.
 */
public final class MathUtil {
    public static final double EPSILON = 1e-9;

    private MathUtil() {
    }

    public static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(min, Math.min(max, value));
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * clamp(t, 0.0, 1.0);
    }

    public static double moveTowards(double current, double target, double maxDelta) {
        double diff = target - current;
        if (Math.abs(diff) <= maxDelta) {
            return target;
        }
        return current + Math.copySign(maxDelta, diff);
    }

    public static double safeDivide(double numerator, double denominator, double fallback) {
        if (!Double.isFinite(numerator) || !Double.isFinite(denominator) || Math.abs(denominator) < EPSILON) {
            return fallback;
        }
        double result = numerator / denominator;
        return Double.isFinite(result) ? result : fallback;
    }

    public static boolean isFinite(double value) {
        return Double.isFinite(value);
    }

    public static double sanitize(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
