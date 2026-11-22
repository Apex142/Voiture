package com.realisticvehicles.minecraft;

import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyConfig;

/**
 * Factory and layout updater for a 15-cell contoured 3D grid of square PartEntity hitboxes.
 * Square horizontal cells (width ~0.68m) hug the exact rotated car silhouette at any yaw angle (0°, 45°, 90°)
 * without protruding corners, while remaining smoothly climbable.
 */
public final class VehiclePartLayout {
    public static final int PART_COUNT = 15;

    private VehiclePartLayout() {
    }

    public static VehiclePartEntity[] createInitialParts(VehicleEntity parent, RigidBodyConfig body) {
        String[] names = {
            "front_nose", "front_nose", "front_nose",
            "front_hood", "front_hood", "front_hood",
            "windshield", "windshield", "windshield",
            "cabin", "cabin", "cabin",
            "rear_trunk", "rear_trunk", "rear_trunk"
        };
        VehiclePartEntity[] parts = new VehiclePartEntity[PART_COUNT];
        for (int i = 0; i < PART_COUNT; i++) {
            parts[i] = new VehiclePartEntity(parent, names[i], Vec3d.ZERO, 0.68F, 0.8F);
        }
        configureParts(parts, body);
        return parts;
    }

    public static void configureParts(VehiclePartEntity[] parts, RigidBodyConfig body) {
        if (parts == null || parts.length < PART_COUNT) {
            return;
        }
        double l = body.length();
        double w = body.width();
        double bottomY = -0.635;

        double[] rowZ = { l * 0.37, l * 0.19, l * 0.01, -l * 0.18, -l * 0.37 };
        float[] rowHeights = { 0.64F, 1.00F, 1.36F, 1.65F, 1.08F };
        double[] colX = { w * 0.27, 0.0, -w * 0.27 };
        float cellW = (float) (w * 0.30);

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 3; c++) {
                int idx = r * 3 + c;
                // Slightly lower outer roof edges on cabin row for rounded coupe profile
                float h = (r == 3 && c != 1) ? rowHeights[r] - 0.14F : rowHeights[r];
                parts[idx].reconfigure(new Vec3d(colX[c], bottomY, rowZ[r]), cellW, h);
            }
        }
    }

    public static void updatePartTransforms(
        VehiclePartEntity[] parts,
        Vec3d comPos,
        Quat4d orientation,
        boolean snapOld
    ) {
        if (parts == null) {
            return;
        }
        for (VehiclePartEntity part : parts) {
            Vec3d world = comPos.add(orientation.rotate(part.localOffset()));
            if (snapOld || (part.xOld == 0.0 && part.yOld == 0.0 && part.zOld == 0.0)) {
                part.xOld = world.x();
                part.yOld = world.y();
                part.zOld = world.z();
                part.xo = world.x();
                part.yo = world.y();
                part.zo = world.z();
            } else {
                part.xOld = part.getX();
                part.yOld = part.getY();
                part.zOld = part.getZ();
                part.xo = part.getX();
                part.yo = part.getY();
                part.zo = part.getZ();
            }
            part.setPos(world.x(), world.y(), world.z());
        }
    }
}
