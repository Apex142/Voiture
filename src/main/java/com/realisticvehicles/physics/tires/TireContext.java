package com.realisticvehicles.physics.tires;

import com.realisticvehicles.physics.world.SurfaceProperties;

/**
 * Inputs required by any TireModel strategy to compute wheel contact forces.
 */
public record TireContext(
    double normalLoad,
    double longitudinalVelocity,
    double lateralVelocity,
    double wheelAngularVelocity,
    double wheelRadius,
    double wheelInertia,
    double driveTorque,
    double brakeTorque,
    double tireGripFactor,
    SurfaceProperties surface,
    double dt
) {
}
