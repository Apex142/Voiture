package com.realisticvehicles.physics.drivetrain;

/**
 * Engine physical curve and redline configuration.
 */
public record EngineConfig(
    double idleRpm,
    double peakTorqueRpm,
    double redlineRpm,
    double maxTorqueNm,
    double engineBrakingNm
) {
    public static EngineConfig standardSedan() {
        return new EngineConfig(850.0, 4200.0, 6500.0, 290.0, 55.0);
    }

    public static EngineConfig sportCoupe() {
        return new EngineConfig(950.0, 5000.0, 7600.0, 460.0, 80.0);
    }
}
