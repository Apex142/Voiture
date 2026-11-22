package com.realisticvehicles.render.glb;

import java.util.List;

/**
 * Complete parsed 3D vehicle model split into chassis, 4 wheels, and 4 hinged panels (doors, hood, trunk).
 */
public record GlbModel(
    List<GlbMeshPrimitive> chassisPrimitives,
    GlbWheelPart frontLeftWheel,
    GlbWheelPart frontRightWheel,
    GlbWheelPart rearLeftWheel,
    GlbWheelPart rearRightWheel,
    GlbHingedPart leftDoor,
    GlbHingedPart rightDoor,
    GlbHingedPart hood,
    GlbHingedPart trunk,
    List<GlbMaterial> materials,
    float minX, float minY, float minZ,
    float maxX, float maxY, float maxZ
) {
    public int totalTriangles() {
        int count = sumTris(chassisPrimitives);
        for (GlbWheelPart w : List.of(frontLeftWheel, frontRightWheel, rearLeftWheel, rearRightWheel)) {
            count += sumTris(w.primitives());
        }
        for (GlbHingedPart h : List.of(leftDoor, rightDoor, hood, trunk)) {
            count += sumTris(h.primitives());
        }
        return count;
    }

    private static int sumTris(List<GlbMeshPrimitive> list) {
        int c = 0;
        for (GlbMeshPrimitive p : list) {
            c += p.triangleCount();
        }
        return c;
    }
}
