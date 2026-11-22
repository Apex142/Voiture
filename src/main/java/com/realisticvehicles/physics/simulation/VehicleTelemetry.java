package com.realisticvehicles.physics.simulation;

/**
 * Real-time simulation telemetry snapshot for HUD, diagnostics, and tests.
 */
public record VehicleTelemetry(
    String presetId,
    double speedMps,
    double speedKmh,
    double engineRpm,
    int gear,
    double steerAngleDeg,
    int groundedWheels,
    boolean braking,
    boolean reversing,
    boolean sleeping
) {
}
