package com.realisticvehicles.render.glb;

import java.util.List;

/**
 * Represents one articulated wheel assembly centered around its geometric hub (0,0,0).
 */
public record GlbWheelPart(
    float hubX,
    float hubY,
    float hubZ,
    List<GlbMeshPrimitive> primitives
) {
}
