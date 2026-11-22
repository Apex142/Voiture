package com.realisticvehicles.physics.tires;

import com.realisticvehicles.core.math.MathUtil;

/**
 * Pacejka Magic Formula (simplified combined-slip) TireModel strategy.
 * Formula: y(x) = D * sin(C * atan(B*x - E*(B*x - atan(B*x))))
 */
public final class PacejkaTireModel implements TireModel {
    private static final double GRAVITY = 9.81;
    private static final double B_LONG = 10.5;
    private static final double C_LONG = 1.65;
    private static final double E_LONG = 0.97;
    private static final double B_LAT = 8.2;
    private static final double C_LAT = 1.38;
    private static final double E_LAT = 0.92;
    private static final double LOW_SPEED_V_REF = 1.4;
    private static final double STOP_RELAXATION = 0.55;

    @Override
    public TireForces calculate(TireContext ctx) {
        if (ctx.normalLoad() <= 1e-3 || ctx.wheelRadius() <= 1e-3 || ctx.dt() <= 0.0) {
            return new TireForces(0.0, 0.0, 0.0, 0.0, freeSpin(ctx));
        }

        double mu = Math.max(0.05, ctx.tireGripFactor() * ctx.surface().gripMultiplier());
        double peakForce = mu * ctx.normalLoad();
        double effMass = ctx.normalLoad() / GRAVITY;
        double vLong = ctx.longitudinalVelocity();
        double vLat = ctx.lateralVelocity();
        double vRef = Math.max(Math.abs(vLong), LOW_SPEED_V_REF);

        double wheelSpeed = ctx.wheelAngularVelocity() * ctx.wheelRadius();
        double rawSlipRatio = MathUtil.clamp((wheelSpeed - vLong) / vRef, -1.5, 1.5);
        double slipAngle = Math.atan2(vLat, vRef);

        double driveForce = ctx.driveTorque() / ctx.wheelRadius();
        double rollingForce = -ctx.surface().rollingResistance() * ctx.normalLoad() * Math.tanh(vLong / 0.25);
        double maxBrakeForce = Math.max(0.0, ctx.brakeTorque()) / ctx.wheelRadius();
        double longStopCap = effMass * Math.abs(vLong) / ctx.dt() * STOP_RELAXATION;
        double brakeForce = Math.abs(vLong) > 1e-4
            ? -Math.signum(vLong) * Math.min(maxBrakeForce, longStopCap)
            : 0.0;

        double pacejkaLong = magicFormula(rawSlipRatio, B_LONG, C_LONG, peakForce, E_LONG);
        double requestedLong = driveForce + rollingForce + brakeForce;
        double fx = Math.abs(requestedLong) < Math.abs(pacejkaLong)
            ? requestedLong
            : Math.copySign(Math.min(Math.abs(requestedLong), peakForce), requestedLong);

        double fy = -magicFormula(slipAngle, B_LAT, C_LAT, peakForce, E_LAT);
        double latStopCap = effMass * Math.abs(vLat) / ctx.dt() * STOP_RELAXATION;
        if (Math.abs(fy) > latStopCap) {
            fy = -Math.signum(vLat) * latStopCap;
        }

        double combined = Math.hypot(fx, fy);
        if (combined > peakForce && combined > 1e-6) {
            double scale = peakForce / combined;
            fx *= scale;
            fy *= scale;
        }

        double nextOmega = computeNextOmega(ctx, fx, peakForce, maxBrakeForce);
        double nextSlipRatio = MathUtil.clamp((nextOmega * ctx.wheelRadius() - vLong) / vRef, -2.0, 2.0);
        return new TireForces(
            MathUtil.sanitize(fx),
            MathUtil.sanitize(fy),
            MathUtil.sanitize(nextSlipRatio),
            MathUtil.sanitize(slipAngle),
            MathUtil.sanitize(nextOmega)
        );
    }

    private double magicFormula(double x, double b, double c, double d, double e) {
        double bx = b * x;
        return d * Math.sin(c * Math.atan(bx - e * (bx - Math.atan(bx))));
    }

    private double computeNextOmega(TireContext ctx, double fx, double peakForce, double maxBrakeForce) {
        if (maxBrakeForce > peakForce * 1.05 && Math.abs(ctx.driveTorque()) < 1e-2) {
            return MathUtil.moveTowards(ctx.wheelAngularVelocity(), 0.0, 95.0 * ctx.dt());
        }
        double roadOmega = ctx.longitudinalVelocity() / ctx.wheelRadius();
        double netTorque = ctx.driveTorque() - fx * ctx.wheelRadius();
        double spinDelta = (netTorque / Math.max(ctx.wheelInertia(), 0.2)) * ctx.dt();
        double targetOmega = roadOmega + MathUtil.clamp(spinDelta, -30.0, 30.0);
        return MathUtil.lerp(ctx.wheelAngularVelocity(), targetOmega, MathUtil.clamp(ctx.dt() * 35.0, 0.0, 1.0));
    }

    private double freeSpin(TireContext ctx) {
        double omega = ctx.wheelAngularVelocity() + (ctx.driveTorque() / Math.max(ctx.wheelInertia(), 0.2)) * ctx.dt();
        double brakeDecel = (Math.max(0.0, ctx.brakeTorque()) / Math.max(ctx.wheelInertia(), 0.2)) * ctx.dt();
        return MathUtil.clamp(MathUtil.moveTowards(omega, 0.0, brakeDecel + 2.0 * ctx.dt()), -250.0, 250.0);
    }
}
