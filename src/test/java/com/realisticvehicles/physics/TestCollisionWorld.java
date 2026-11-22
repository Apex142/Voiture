package com.realisticvehicles.physics;

import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.world.CollisionBox;
import com.realisticvehicles.physics.world.CollisionWorld;
import com.realisticvehicles.physics.world.RayHit;
import com.realisticvehicles.physics.world.SurfaceProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure analytical CollisionWorld for deterministic physics unit and integration tests.
 */
public final class TestCollisionWorld implements CollisionWorld {
    private final double groundHeight;
    private final double slopeAngleRad;
    private final double slopeStartZ;
    private final SurfaceProperties surface;
    private final List<CollisionBox> boxes;

    public TestCollisionWorld(double groundHeight, SurfaceProperties surface) {
        this(groundHeight, 0.0, Double.POSITIVE_INFINITY, surface, List.of());
    }

    public TestCollisionWorld(
        double groundHeight,
        double slopeAngleRad,
        double slopeStartZ,
        SurfaceProperties surface,
        List<CollisionBox> boxes
    ) {
        this.groundHeight = groundHeight;
        this.slopeAngleRad = slopeAngleRad;
        this.slopeStartZ = slopeStartZ;
        this.surface = surface;
        this.boxes = List.copyOf(boxes);
    }

    @Override
    public Optional<RayHit> raycast(Vec3d origin, Vec3d direction, double maxDistance) {
        Vec3d dir = direction.normalize();
        RayHit best = intersectGroundPlane(origin, dir, maxDistance);
        for (CollisionBox box : boxes) {
            RayHit boxHit = intersectBox(origin, dir, maxDistance, box);
            if (boxHit != null && (best == null || boxHit.distance() < best.distance())) {
                best = boxHit;
            }
        }
        return Optional.ofNullable(best);
    }

    @Override
    public List<CollisionBox> queryBoxes(CollisionBox bounds) {
        List<CollisionBox> result = new ArrayList<>();
        CollisionBox groundSlab = new CollisionBox(
            bounds.minX() - 2.0, groundHeight - 2.0, bounds.minZ() - 2.0,
            bounds.maxX() + 2.0, groundHeight, bounds.maxZ() + 2.0
        );
        if (slopeAngleRad == 0.0 && groundSlab.intersects(bounds)) {
            result.add(groundSlab);
        }
        for (CollisionBox box : boxes) {
            if (box.intersects(bounds)) {
                result.add(box);
            }
        }
        return result;
    }

    private RayHit intersectGroundPlane(Vec3d origin, Vec3d dir, double maxDistance) {
        double tanSlope = Math.tan(slopeAngleRad);
        Vec3d normal = slopeAngleRad == 0.0
            ? Vec3d.UNIT_Y
            : new Vec3d(0.0, Math.cos(slopeAngleRad), -Math.sin(slopeAngleRad)).normalize();

        double bestDist = Double.POSITIVE_INFINITY;
        Vec3d bestPoint = null;
        Vec3d bestNormal = Vec3d.UNIT_Y;

        if (Math.abs(dir.y()) > 1e-8) {
            double tFlat = (groundHeight - origin.y()) / dir.y();
            if (tFlat >= 0.0 && tFlat <= maxDistance) {
                Vec3d p = origin.addScaled(dir, tFlat);
                if (p.z() <= slopeStartZ) {
                    bestDist = tFlat;
                    bestPoint = p;
                }
            }
        }
        if (slopeAngleRad != 0.0) {
            double denom = dir.y() - tanSlope * dir.z();
            if (Math.abs(denom) > 1e-8) {
                double num = groundHeight + tanSlope * (origin.z() - slopeStartZ) - origin.y();
                double tSlope = num / denom;
                if (tSlope >= 0.0 && tSlope <= maxDistance && tSlope < bestDist) {
                    Vec3d p = origin.addScaled(dir, tSlope);
                    if (p.z() >= slopeStartZ) {
                        bestDist = tSlope;
                        bestPoint = p;
                        bestNormal = normal;
                    }
                }
            }
        }
        return bestPoint != null ? new RayHit(bestPoint, bestNormal, bestDist, surface) : null;
    }

    private RayHit intersectBox(Vec3d origin, Vec3d dir, double maxDistance, CollisionBox box) {
        double tMin = 0.0;
        double tMax = maxDistance;
        Vec3d hitNormal = Vec3d.UNIT_Y;

        double[] orig = { origin.x(), origin.y(), origin.z() };
        double[] d = { dir.x(), dir.y(), dir.z() };
        double[] bMin = { box.minX(), box.minY(), box.minZ() };
        double[] bMax = { box.maxX(), box.maxY(), box.maxZ() };
        Vec3d[] negNormals = { new Vec3d(-1, 0, 0), new Vec3d(0, -1, 0), new Vec3d(0, 0, -1) };
        Vec3d[] posNormals = { new Vec3d(1, 0, 0), new Vec3d(0, 1, 0), new Vec3d(0, 0, 1) };

        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(d[axis]) < 1e-9) {
                if (orig[axis] < bMin[axis] || orig[axis] > bMax[axis]) {
                    return null;
                }
            } else {
                double inv = 1.0 / d[axis];
                double t1 = (bMin[axis] - orig[axis]) * inv;
                double t2 = (bMax[axis] - orig[axis]) * inv;
                Vec3d n1 = negNormals[axis];
                if (t1 > t2) {
                    double tmp = t1;
                    t1 = t2;
                    t2 = tmp;
                    n1 = posNormals[axis];
                }
                if (t1 > tMin) {
                    tMin = t1;
                    hitNormal = n1;
                }
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) {
                    return null;
                }
            }
        }
        return new RayHit(origin.addScaled(dir, tMin), hitNormal, tMin, surface);
    }
}
