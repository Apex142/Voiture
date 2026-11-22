package com.realisticvehicles.physics.drivetrain;

/**
 * Open differential splitting torque 50/50 regardless of wheel speed difference.
 */
public final class OpenDifferential implements DifferentialStrategy {
    @Override
    public AxleTorqueSplit split(double totalAxleTorque, double leftWheelOmega, double rightWheelOmega) {
        double half = totalAxleTorque * 0.5;
        return new AxleTorqueSplit(half, half);
    }
}
