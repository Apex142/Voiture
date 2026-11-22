package com.realisticvehicles.physics.world;

import com.realisticvehicles.core.math.Vec3d;

/**
 * Result of a raycast query against the physical collision world.
 */
public record RayHit(
    Vec3d point,
    Vec3d normal,
    double distance,
    SurfaceProperties surface
) {
}
