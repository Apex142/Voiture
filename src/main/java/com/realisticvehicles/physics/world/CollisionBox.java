package com.realisticvehicles.physics.world;

import com.realisticvehicles.core.math.Vec3d;

/**
 * Axis-aligned solid box in pure physics world coordinates (meters).
 */
public record CollisionBox(
    double minX,
    double minY,
    double minZ,
    double maxX,
    double maxY,
    double maxZ
) {
    public boolean contains(Vec3d point) {
        return point.x() >= minX && point.x() <= maxX
            && point.y() >= minY && point.y() <= maxY
            && point.z() >= minZ && point.z() <= maxZ;
    }

    public boolean intersects(CollisionBox other) {
        return maxX > other.minX && minX < other.maxX
            && maxY > other.minY && minY < other.maxY
            && maxZ > other.minZ && minZ < other.maxZ;
    }

    public Vec3d closestPoint(Vec3d point) {
        return new Vec3d(
            Math.max(minX, Math.min(point.x(), maxX)),
            Math.max(minY, Math.min(point.y(), maxY)),
            Math.max(minZ, Math.min(point.z(), maxZ))
        );
    }

    public CollisionBox expand(double margin) {
        return new CollisionBox(
            minX - margin, minY - margin, minZ - margin,
            maxX + margin, maxY + margin, maxZ + margin
        );
    }
}
