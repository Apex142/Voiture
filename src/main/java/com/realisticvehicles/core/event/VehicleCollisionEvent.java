package com.realisticvehicles.core.event;

import com.realisticvehicles.core.math.Vec3d;

/**
 * Published whenever a vehicle chassis collides with a solid surface.
 */
public record VehicleCollisionEvent(
    Vec3d contactPoint,
    Vec3d contactNormal,
    double normalImpulse,
    double closingSpeed
) {
}
