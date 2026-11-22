package com.realisticvehicles.physics.drivetrain;

/**
 * Per-wheel drive and brake torque commands produced by the drivetrain solver.
 */
public record DrivetrainOutput(
    double[] wheelDriveTorques,
    double[] wheelBrakeTorques,
    boolean braking,
    boolean reversing
) {
}
