package com.realisticvehicles.physics.drivetrain;

/**
 * Strategy interface splitting axle drive torque between left and right wheels.
 */
public interface DifferentialStrategy {
    record AxleTorqueSplit(double leftTorque, double rightTorque) {
    }

    AxleTorqueSplit split(double totalAxleTorque, double leftWheelOmega, double rightWheelOmega);
}
