package com.realisticvehicles.render.api;

import com.realisticvehicles.core.math.Vec3d;

/**
 * Snapshot of a single wheel's visual state decoupled from physics internals.
 */
public record WheelRenderState(
    Vec3d localHubOffset,
    double radius,
    double width,
    double steerAngleRadians,
    double spinAngleRadians,
    boolean grounded
) {
}
