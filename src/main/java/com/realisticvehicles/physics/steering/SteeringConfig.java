package com.realisticvehicles.physics.steering;

/**
 * Steering geometry and speed-sensitivity parameters.
 */
public record SteeringConfig(
    double maxSteerAngleRadians,
    double steerRateRadPerSec,
    double returnRateRadPerSec,
    double highSpeedSteerFactor,
    double highSpeedReferenceMps,
    double wheelbase,
    double trackWidth
) {
    public static SteeringConfig standardSedan() {
        return new SteeringConfig(
            Math.toRadians(34.0),
            Math.toRadians(150.0),
            Math.toRadians(210.0),
            0.32,
            32.0,
            2.76,
            1.68
        );
    }
}
