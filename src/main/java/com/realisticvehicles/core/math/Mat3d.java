package com.realisticvehicles.core.math;

/**
 * Diagonal 3x3 inertia tensor helper transformed via unit quaternion orientation.
 */
public record Mat3d(double diagX, double diagY, double diagZ) {

    public static Mat3d solidBoxInertia(double mass, double widthX, double heightY, double lengthZ) {
        double factor = mass / 12.0;
        double ix = factor * (heightY * heightY + lengthZ * lengthZ);
        double iy = factor * (widthX * widthX + lengthZ * lengthZ);
        double iz = factor * (widthX * widthX + heightY * heightY);
        return new Mat3d(ix, iy, iz);
    }

    public Mat3d inverse() {
        return new Mat3d(
            MathUtil.safeDivide(1.0, diagX, 0.0),
            MathUtil.safeDivide(1.0, diagY, 0.0),
            MathUtil.safeDivide(1.0, diagZ, 0.0)
        );
    }

    /** Multiplies world-space vector by R * diag * R^T. */
    public Vec3d multiplyWorld(Quat4d orientation, Vec3d worldVector) {
        Vec3d local = orientation.inverseRotate(worldVector);
        Vec3d scaled = new Vec3d(local.x() * diagX, local.y() * diagY, local.z() * diagZ);
        return orientation.rotate(scaled);
    }
}
