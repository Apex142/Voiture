package com.realisticvehicles.physics.tires;

/**
 * Forces and kinematic telemetry returned by a TireModel evaluation.
 */
public record TireForces(
    double longitudinalForce,
    double lateralForce,
    double slipRatio,
    double slipAngle,
    double nextWheelAngularVelocity
) {
    public static final TireForces ZERO = new TireForces(0.0, 0.0, 0.0, 0.0, 0.0);
}
