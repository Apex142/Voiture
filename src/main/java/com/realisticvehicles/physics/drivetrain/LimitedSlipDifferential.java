package com.realisticvehicles.physics.drivetrain;

import com.realisticvehicles.core.math.MathUtil;

/**
 * Limited-Slip Differential (LSD) transferring torque away from a spinning wheel
 * toward the slower wheel with traction.
 */
public final class LimitedSlipDifferential implements DifferentialStrategy {
    private final double lockingFactor;
    private final double preloadNm;

    public LimitedSlipDifferential(double lockingFactor, double preloadNm) {
        this.lockingFactor = MathUtil.clamp(lockingFactor, 0.0, 1.0);
        this.preloadNm = Math.max(0.0, preloadNm);
    }

    @Override
    public AxleTorqueSplit split(double totalAxleTorque, double leftWheelOmega, double rightWheelOmega) {
        double half = totalAxleTorque * 0.5;
        double speedDiff = leftWheelOmega - rightWheelOmega;
        double biasMagnitude = Math.min(
            Math.abs(half) * lockingFactor,
            Math.abs(speedDiff) * 35.0 * lockingFactor + preloadNm * 0.5
        );
        double transfer = Math.signum(speedDiff) * biasMagnitude;
        return new AxleTorqueSplit(half - transfer, half + transfer);
    }
}
