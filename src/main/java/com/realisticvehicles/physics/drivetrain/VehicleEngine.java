package com.realisticvehicles.physics.drivetrain;

import com.realisticvehicles.core.math.MathUtil;

/**
 * Calculates engine torque output and smooth RPM response from throttle and load.
 */
public final class VehicleEngine {
    private final EngineConfig config;
    private double currentRpm;

    public VehicleEngine(EngineConfig config) {
        this.config = config;
        this.currentRpm = config.idleRpm();
    }

    public double currentRpm() {
        return currentRpm;
    }

    public void setCurrentRpm(double rpm) {
        this.currentRpm = MathUtil.clamp(rpm, config.idleRpm(), config.redlineRpm());
    }

    public EngineConfig config() {
        return config;
    }

    public void updateRpm(double wheelDrivenRpm, double throttle, double dt) {
        double clampedThrottle = MathUtil.clamp(throttle, 0.0, 1.0);
        double minTarget = config.idleRpm() + clampedThrottle * 1400.0;
        double targetRpm = Math.max(minTarget, Math.abs(wheelDrivenRpm));
        targetRpm = MathUtil.clamp(targetRpm, config.idleRpm(), config.redlineRpm());
        double response = clampedThrottle > 0.05 ? 12.0 : 7.0;
        currentRpm = MathUtil.lerp(currentRpm, targetRpm, MathUtil.clamp(response * dt, 0.0, 1.0));
    }

    /** Evaluates net crankshaft torque (N*m) using a realistic parabolic torque curve. */
    public double calculateTorque(double throttle) {
        double clampedThrottle = MathUtil.clamp(throttle, 0.0, 1.0);
        if (currentRpm >= config.redlineRpm() * 0.995) {
            return -config.engineBrakingNm();
        }
        double peakRpm = config.peakTorqueRpm();
        double halfSpan = Math.max(1000.0, config.redlineRpm() - config.idleRpm()) * 0.55;
        double normalized = (currentRpm - peakRpm) / halfSpan;
        double curveFactor = MathUtil.clamp(1.0 - 0.35 * normalized * normalized, 0.55, 1.0);
        double positiveTorque = config.maxTorqueNm() * curveFactor * clampedThrottle;
        double braking = config.engineBrakingNm() * (1.0 - clampedThrottle)
            * MathUtil.clamp((currentRpm - config.idleRpm()) / 3000.0, 0.0, 1.0);
        return positiveTorque - braking;
    }
}
