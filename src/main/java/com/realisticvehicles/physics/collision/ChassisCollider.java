package com.realisticvehicles.physics.collision;

import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyConfig;
import com.realisticvehicles.physics.body.RigidBodyState;
import com.realisticvehicles.physics.world.CollisionBox;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a dense contoured 3D hull matching the real vehicle mesh silhouette
 * (true lower bumper/skirt bottom at y = -0.40m so bodywork never sinks into blocks).
 */
public final class ChassisCollider {
    private static final double SKIRT_BOTTOM_Y = -0.40;
    private static final double BUMPER_TIP_Y = -0.35;
    private static final double BROADPHASE_MARGIN = 0.60;

    private final List<Vec3d> localSamplePoints;

    public ChassisCollider(RigidBodyConfig bodyConfig) {
        double hx = bodyConfig.width() * 0.49;
        double hz = bodyConfig.length() * 0.49;
        double hyHood = 0.22;
        double hyRoof = 0.98;

        List<Vec3d> pts = new ArrayList<>(76);
        double[] xFracs = { -1.0, -0.65, -0.32, 0.0, 0.32, 0.65, 1.0 };
        double[] zFracs = { -1.0, -0.72, -0.44, -0.15, 0.15, 0.44, 0.72, 1.0 };

        // Dense front and rear bumper perimeter (lower chin + mid grille + hood line)
        for (double xf : xFracs) {
            double curveZ = hz * (1.0 - 0.08 * xf * xf);
            double x = hx * 0.90 * xf;
            for (double y : new double[] { BUMPER_TIP_Y, -0.10, hyHood }) {
                pts.add(new Vec3d(x, y, curveZ));
                pts.add(new Vec3d(x, y, -curveZ));
            }
        }

        // Dense left/right rocker skirts, doors, fenders, and underbody rails
        for (double zf : zFracs) {
            double z = hz * 0.92 * zf;
            for (double side : new double[] { -1.0, 1.0 }) {
                pts.add(new Vec3d(side * hx, SKIRT_BOTTOM_Y, z));
                pts.add(new Vec3d(side * hx, -0.08, z));
                pts.add(new Vec3d(side * hx * 0.96, hyHood, z));
            }
            pts.add(new Vec3d(0.0, SKIRT_BOTTOM_Y, z));
        }

        // Cabin pillars & roof contour
        double hxRoof = hx * 0.68;
        for (double xf : new double[] { -1.0, 0.0, 1.0 }) {
            for (double zf : new double[] { -0.38, -0.10, 0.18 }) {
                pts.add(new Vec3d(hxRoof * xf, hyRoof, hz * zf));
            }
        }
        this.localSamplePoints = List.copyOf(pts);
    }

    public List<Vec3d> worldSamplePoints(RigidBodyState body) {
        List<Vec3d> worldPoints = new ArrayList<>(localSamplePoints.size());
        for (Vec3d local : localSamplePoints) {
            worldPoints.add(body.localToWorldPoint(local));
        }
        return worldPoints;
    }

    public CollisionBox broadphaseBounds(List<Vec3d> worldPoints) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3d p : worldPoints) {
            minX = Math.min(minX, p.x());
            minY = Math.min(minY, p.y());
            minZ = Math.min(minZ, p.z());
            maxX = Math.max(maxX, p.x());
            maxY = Math.max(maxY, p.y());
            maxZ = Math.max(maxZ, p.z());
        }
        return new CollisionBox(minX, minY, minZ, maxX, maxY, maxZ).expand(BROADPHASE_MARGIN);
    }
}
