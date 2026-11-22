package com.realisticvehicles.physics.simulation;

import com.realisticvehicles.core.math.MathUtil;

/**
 * Immutable driver control command (Command pattern).
 * steer: +1.0 = full LEFT (Q), -1.0 = full RIGHT (D).
 */
public record VehicleInput(
    double throttle,
    double brake,
    double steer,
    boolean handbrake
) {
    public static final VehicleInput NEUTRAL = new VehicleInput(0.0, 0.0, 0.0, false);

    public VehicleInput {
        throttle = MathUtil.clamp(throttle, 0.0, 1.0);
        brake = MathUtil.clamp(brake, 0.0, 1.0);
        steer = MathUtil.clamp(steer, -1.0, 1.0);
    }
}
