package com.realisticvehicles.core.math;

/**
 * Immutable unit quaternion representing 3D orientation (local-to-world rotation).
 */
public record Quat4d(double x, double y, double z, double w) {
    public static final Quat4d IDENTITY = new Quat4d(0.0, 0.0, 0.0, 1.0);

    public static Quat4d fromAxisAngle(Vec3d axis, double radians) {
        Vec3d normAxis = axis.normalize();
        if (normAxis.lengthSquared() < MathUtil.EPSILON) {
            return IDENTITY;
        }
        double half = radians * 0.5;
        double s = Math.sin(half);
        return new Quat4d(normAxis.x() * s, normAxis.y() * s, normAxis.z() * s, Math.cos(half)).normalize();
    }

    public static Quat4d fromYaw(double yawRadians) {
        return fromAxisAngle(Vec3d.UNIT_Y, yawRadians);
    }

    public Quat4d normalize() {
        double normSq = x * x + y * y + z * z + w * w;
        if (normSq < MathUtil.EPSILON || !Double.isFinite(normSq)) {
            return IDENTITY;
        }
        double inv = 1.0 / Math.sqrt(normSq);
        return new Quat4d(x * inv, y * inv, z * inv, w * inv);
    }

    public Quat4d conjugate() {
        return new Quat4d(-x, -y, -z, w);
    }

    public Quat4d multiply(Quat4d o) {
        return new Quat4d(
            w * o.x + x * o.w + y * o.z - z * o.y,
            w * o.y - x * o.z + y * o.w + z * o.x,
            w * o.z + x * o.y - y * o.x + z * o.w,
            w * o.w - x * o.x - y * o.y - z * o.z
        );
    }

    /** Rotates a vector from local vehicle space into world space. */
    public Vec3d rotate(Vec3d v) {
        Vec3d qv = new Vec3d(x, y, z);
        Vec3d t = qv.cross(v).scale(2.0);
        return v.add(t.scale(w)).add(qv.cross(t));
    }

    /** Rotates a vector from world space into local vehicle space. */
    public Vec3d inverseRotate(Vec3d v) {
        return conjugate().rotate(v);
    }

    /** Integrates orientation using world-space angular velocity omega (rad/s). */
    public Quat4d integrate(Vec3d worldOmega, double dt) {
        double speed = worldOmega.length();
        if (speed < MathUtil.EPSILON || dt <= 0.0) {
            return this;
        }
        Quat4d delta = fromAxisAngle(worldOmega.scale(1.0 / speed), speed * dt);
        return delta.multiply(this).normalize();
    }

    public Vec3d forward() {
        return rotate(Vec3d.UNIT_Z);
    }

    public Vec3d up() {
        return rotate(Vec3d.UNIT_Y);
    }

    public Vec3d left() {
        return rotate(Vec3d.UNIT_X);
    }

    /** Yaw angle in radians around +Y (0 faces +Z, +pi/2 faces +X / Left). */
    public double yawRadians() {
        Vec3d fwd = forward();
        return Math.atan2(fwd.x(), fwd.z());
    }

    /** Pitch angle in radians (positive = nose down around local +X). */
    public double pitchRadians() {
        Vec3d fwd = forward();
        double horiz = Math.hypot(fwd.x(), fwd.z());
        return Math.atan2(-fwd.y(), horiz);
    }

    /** Roll angle in radians around local +Z. */
    public double rollRadians() {
        Vec3d localLeft = left();
        Vec3d localUp = up();
        return Math.atan2(localLeft.y(), localUp.y());
    }

    public boolean isFinite() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && Double.isFinite(w);
    }

    public Quat4d slerp(Quat4d target, double t) {
        double clamped = MathUtil.clamp(t, 0.0, 1.0);
        double dot = x * target.x + y * target.y + z * target.z + w * target.w;
        Quat4d end = dot < 0.0 ? new Quat4d(-target.x, -target.y, -target.z, -target.w) : target;
        dot = Math.abs(dot);
        if (dot > 0.9995) {
            return new Quat4d(
                x + (end.x - x) * clamped,
                y + (end.y - y) * clamped,
                z + (end.z - z) * clamped,
                w + (end.w - w) * clamped
            ).normalize();
        }
        double theta0 = Math.acos(MathUtil.clamp(dot, -1.0, 1.0));
        double theta = theta0 * clamped;
        double sinTheta = Math.sin(theta);
        double sinTheta0 = Math.sin(theta0);
        double s0 = Math.cos(theta) - dot * sinTheta / sinTheta0;
        double s1 = sinTheta / sinTheta0;
        return new Quat4d(
            s0 * x + s1 * end.x,
            s0 * y + s1 * end.y,
            s0 * z + s1 * end.z,
            s0 * w + s1 * end.w
        ).normalize();
    }
}
