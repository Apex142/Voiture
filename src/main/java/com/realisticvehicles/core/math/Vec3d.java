package com.realisticvehicles.core.math;

/**
 * Immutable 3D double-precision vector in right-handed SI coordinates (meters).
 * Local vehicle convention: +X = Left, +Y = Up, +Z = Forward.
 */
public record Vec3d(double x, double y, double z) {
    public static final Vec3d ZERO = new Vec3d(0.0, 0.0, 0.0);
    public static final Vec3d UNIT_X = new Vec3d(1.0, 0.0, 0.0);
    public static final Vec3d UNIT_Y = new Vec3d(0.0, 1.0, 0.0);
    public static final Vec3d UNIT_Z = new Vec3d(0.0, 0.0, 1.0);

    public Vec3d add(Vec3d other) {
        return new Vec3d(x + other.x, y + other.y, z + other.z);
    }

    public Vec3d addScaled(Vec3d other, double scale) {
        return new Vec3d(x + other.x * scale, y + other.y * scale, z + other.z * scale);
    }

    public Vec3d sub(Vec3d other) {
        return new Vec3d(x - other.x, y - other.y, z - other.z);
    }

    public Vec3d scale(double scalar) {
        return new Vec3d(x * scalar, y * scalar, z * scalar);
    }

    public Vec3d negate() {
        return new Vec3d(-x, -y, -z);
    }

    public double dot(Vec3d other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vec3d cross(Vec3d other) {
        return new Vec3d(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        );
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public Vec3d normalize() {
        double len = length();
        if (len < MathUtil.EPSILON || !Double.isFinite(len)) {
            return ZERO;
        }
        return scale(1.0 / len);
    }

    public Vec3d clampLength(double maxLength) {
        double lenSq = lengthSquared();
        if (lenSq <= maxLength * maxLength) {
            return this;
        }
        return normalize().scale(maxLength);
    }

    public boolean isFinite() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }

    public Vec3d sanitize() {
        return isFinite() ? this : ZERO;
    }

    public Vec3d lerp(Vec3d target, double t) {
        double clamped = MathUtil.clamp(t, 0.0, 1.0);
        return new Vec3d(
            x + (target.x - x) * clamped,
            y + (target.y - y) * clamped,
            z + (target.z - z) * clamped
        );
    }
}
