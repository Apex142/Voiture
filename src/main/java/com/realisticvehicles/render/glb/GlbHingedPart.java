package com.realisticvehicles.render.glb;

import java.util.List;

/**
 * Represents an articulated hinged body panel (door, hood, or trunk) centered around its hinge pivot.
 */
public record GlbHingedPart(
    float pivotX,
    float pivotY,
    float pivotZ,
    List<GlbMeshPrimitive> primitives
) {
    public static final GlbHingedPart EMPTY = new GlbHingedPart(0, 0, 0, List.of());
}
