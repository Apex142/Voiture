package com.realisticvehicles.physics.drivetrain;

import java.util.List;

/**
 * Transmission gear ratios and automatic shift thresholds.
 */
public record TransmissionConfig(
    List<Double> forwardGearRatios,
    double reverseGearRatio,
    double finalDriveRatio,
    double drivetrainEfficiency,
    double upshiftRpm,
    double downshiftRpm,
    double shiftCooldownSeconds
) {
    public static TransmissionConfig sixSpeedAutomatic() {
        return new TransmissionConfig(
            List.of(3.45, 2.18, 1.52, 1.15, 0.92, 0.76),
            -3.25,
            3.70,
            0.88,
            5700.0,
            2100.0,
            0.35
        );
    }

    public static TransmissionConfig sportSixSpeed() {
        return new TransmissionConfig(
            List.of(3.60, 2.35, 1.68, 1.26, 1.00, 0.82),
            -3.40,
            3.95,
            0.90,
            6800.0,
            2600.0,
            0.22
        );
    }
}
