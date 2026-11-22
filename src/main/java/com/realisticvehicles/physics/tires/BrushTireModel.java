package com.realisticvehicles.physics.tires;

import com.realisticvehicles.core.math.MathUtil;

/**
 * Realistic brush tire model with Kamm friction circle and low-speed stabilization.
 */
public final class BrushTireModel implements TireModel {
    private static final double GRAVITY = 9.81;
    private static final double CORNERING_STIFFNESS_PER_N = 8.0;
    private static final double LOW_SPEED_V_REF = 1.5;
    private static final double STOP_RELAXATION = 0.55;

    @Override
    public TireForces calculate(TireContext ctx) {
        if (ctx.normalLoad() <= 1e-3 || ctx.wheelRadius() <= 1e-3 || ctx.dt() <= 0.0) {
            double freeOmega = updateFreeWheelSpin(ctx);
            return new TireForces(0.0, 0.0, 0.0, 0.0, freeOmega);
        }

        double mu = Math.max(0.05, ctx.tireGripFactor() * ctx.surface().gripMultiplier());
        double maxGrip = mu * ctx.normalLoad();
        double effectiveMass = ctx.normalLoad() / GRAVITY;
        double vLong = ctx.longitudinalVelocity();
        double vLat = ctx.lateralVelocity();

        double driveForce = ctx.driveTorque() / ctx.wheelRadius();
        double rollingForce = -ctx.surface().rollingResistance() * ctx.normalLoad() * Math.tanh(vLong / 0.25);

        double maxBrakeForce = Math.max(0.0, ctx.brakeTorque()) / ctx.wheelRadius();
        double longStopCap = effectiveMass * Math.abs(vLong) / ctx.dt() * STOP_RELAXATION;
        double brakeForce = Math.abs(vLong) > 1e-4
            ? -Math.signum(vLong) * Math.min(maxBrakeForce, longStopCap)
            : 0.0;

        double rawLong = driveForce + rollingForce + brakeForce;
        if (Math.abs(ctx.driveTorque()) < 1e-3 && maxBrakeForce > 1e-3 && Math.abs(vLong) <= 0.02) {
            rawLong = brakeForce;
        }

        double vRef = Math.max(Math.abs(vLong), LOW_SPEED_V_REF);
        double slipAngle = Math.atan2(vLat, vRef);
        double corneringStiffness = CORNERING_STIFFNESS_PER_N * ctx.normalLoad() * ctx.surface().gripMultiplier();
        double rawLat = -maxGrip * Math.tanh((corneringStiffness * slipAngle) / Math.max(maxGrip, 1.0));

        double latStopCap = effectiveMass * Math.abs(vLat) / ctx.dt() * STOP_RELAXATION;
        if (Math.abs(rawLat) > latStopCap) {
            rawLat = -Math.signum(vLat) * latStopCap;
        }

        double fx = rawLong;
        double fy = rawLat;
        double combined = Math.hypot(fx, fy);
        if (combined > maxGrip && combined > 1e-6) {
            double scale = maxGrip / combined;
            fx *= scale;
            fy *= scale;
        }

        double nextOmega = computeNextWheelOmega(ctx, fx, maxGrip, maxBrakeForce);
        double slipRatio = MathUtil.clamp((nextOmega * ctx.wheelRadius() - vLong) / vRef, -2.0, 2.0);
        return new TireForces(
            MathUtil.sanitize(fx),
            MathUtil.sanitize(fy),
            MathUtil.sanitize(slipRatio),
            MathUtil.sanitize(slipAngle),
            MathUtil.sanitize(nextOmega)
        );
    }

    private double computeNextWheelOmega(
        TireContext ctx,
        double appliedLongForce,
        double maxGrip,
        double maxBrakeForce
    ) {
        if (maxBrakeForce > maxGrip * 1.05 && Math.abs(ctx.driveTorque()) < 1e-2) {
            return MathUtil.moveTowards(ctx.wheelAngularVelocity(), 0.0, 90.0 * ctx.dt());
        }
        double roadOmega = ctx.longitudinalVelocity() / ctx.wheelRadius();
        double netTorque = ctx.driveTorque() - appliedLongForce * ctx.wheelRadius();
        double wheelSpinDelta = (netTorque / Math.max(ctx.wheelInertia(), 0.2)) * ctx.dt();
        double targetOmega = roadOmega + MathUtil.clamp(wheelSpinDelta, -25.0, 25.0);
        return MathUtil.lerp(ctx.wheelAngularVelocity(), targetOmega, MathUtil.clamp(ctx.dt() * 35.0, 0.0, 1.0));
    }

    private double updateFreeWheelSpin(TireContext ctx) {
        double omega = ctx.wheelAngularVelocity();
        double driveAcc = ctx.driveTorque() / Math.max(ctx.wheelInertia(), 0.2);
        omega += driveAcc * ctx.dt();
        double brakeDecel = (Math.max(0.0, ctx.brakeTorque()) / Math.max(ctx.wheelInertia(), 0.2)) * ctx.dt();
        omega = MathUtil.moveTowards(omega, 0.0, brakeDecel + 2.0 * ctx.dt());
        return MathUtil.clamp(omega, -250.0, 250.0);
    }
}
