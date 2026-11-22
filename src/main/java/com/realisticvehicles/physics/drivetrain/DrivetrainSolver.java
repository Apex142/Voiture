package com.realisticvehicles.physics.drivetrain;

import com.realisticvehicles.core.math.MathUtil;
import com.realisticvehicles.physics.suspension.WheelState;
import java.util.List;

/**
 * Coordinates engine, transmission, differential, ABS/TCS, service brakes, and handbrake.
 */
public final class DrivetrainSolver {
    private static final double RAD_PER_SEC_TO_RPM = 60.0 / (2.0 * Math.PI);
    private final double maxServiceBrakeTorqueNm;
    private final double handbrakeTorqueNm;
    private final DifferentialStrategy differential;
    private final AbsTcsController absTcs;

    public DrivetrainSolver(
        double maxServiceBrakeTorqueNm,
        double handbrakeTorqueNm,
        DifferentialStrategy differential,
        AbsTcsController absTcs
    ) {
        this.maxServiceBrakeTorqueNm = maxServiceBrakeTorqueNm;
        this.handbrakeTorqueNm = handbrakeTorqueNm;
        this.differential = differential;
        this.absTcs = absTcs;
    }

    public DrivetrainOutput solve(
        VehicleEngine engine,
        VehicleTransmission transmission,
        List<WheelState> wheels,
        double forwardSpeed,
        double throttleInput,
        double brakeInput,
        boolean handbrake,
        double dt
    ) {
        double throttle = MathUtil.clamp(throttleInput, 0.0, 1.0);
        double brake = MathUtil.clamp(brakeInput, 0.0, 1.0);

        transmission.updateAutomatic(engine.currentRpm(), forwardSpeed, throttle, brake, dt);
        boolean inReverseGear = transmission.currentGear() < 0;
        double effectiveThrottle = inReverseGear ? brake : throttle;
        double effectiveBrake = inReverseGear
            ? (forwardSpeed < -0.35 ? throttle : 0.0)
            : (forwardSpeed > 0.35 ? brake : (throttle < 0.05 ? brake : 0.0));

        double totalRatio = transmission.activeTotalRatio();
        double avgDrivenOmega = averageDrivenWheelOmega(wheels);
        double coupledEngineRpm = Math.abs(avgDrivenOmega * totalRatio) * RAD_PER_SEC_TO_RPM;
        engine.updateRpm(coupledEngineRpm, effectiveThrottle, dt);

        double crankTorque = engine.calculateTorque(effectiveThrottle);
        double totalDriveTorque = handbrake
            ? 0.0
            : crankTorque * totalRatio * transmission.config().drivetrainEfficiency();

        double[] driveTorques = computeDriveTorques(wheels, totalDriveTorque, forwardSpeed);
        double[] brakeTorques = computeBrakeTorques(wheels, effectiveBrake, handbrake, forwardSpeed);

        boolean isBraking = effectiveBrake > 0.05 || handbrake;
        boolean activelyReversing = inReverseGear && (brake > 0.05 || forwardSpeed < -0.15);
        return new DrivetrainOutput(driveTorques, brakeTorques, isBraking, activelyReversing);
    }

    private double[] computeDriveTorques(List<WheelState> wheels, double totalTorque, double speed) {
        double[] out = new double[wheels.size()];
        boolean frontDriven = wheels.size() >= 2 && (wheels.get(0).config().driven() || wheels.get(1).config().driven());
        boolean rearDriven = wheels.size() >= 4 && (wheels.get(2).config().driven() || wheels.get(3).config().driven());
        double axleShare = (frontDriven && rearDriven) ? totalTorque * 0.5 : totalTorque;

        if (frontDriven) {
            DifferentialStrategy.AxleTorqueSplit front = differential.split(
                axleShare, wheels.get(0).angularVelocity(), wheels.get(1).angularVelocity()
            );
            out[0] = wheels.get(0).config().driven() ? absTcs.modulateDriveTorque(wheels.get(0), front.leftTorque(), speed) : 0.0;
            out[1] = wheels.get(1).config().driven() ? absTcs.modulateDriveTorque(wheels.get(1), front.rightTorque(), speed) : 0.0;
        }
        if (rearDriven) {
            DifferentialStrategy.AxleTorqueSplit rear = differential.split(
                axleShare, wheels.get(2).angularVelocity(), wheels.get(3).angularVelocity()
            );
            out[2] = wheels.get(2).config().driven() ? absTcs.modulateDriveTorque(wheels.get(2), rear.leftTorque(), speed) : 0.0;
            out[3] = wheels.get(3).config().driven() ? absTcs.modulateDriveTorque(wheels.get(3), rear.rightTorque(), speed) : 0.0;
        }
        return out;
    }

    private double[] computeBrakeTorques(List<WheelState> wheels, double brake, boolean handbrake, double speed) {
        double[] out = new double[wheels.size()];
        for (int i = 0; i < wheels.size(); i++) {
            WheelState w = wheels.get(i);
            double service = absTcs.modulateBrakeTorque(w, brake * maxServiceBrakeTorqueNm * w.config().brakeBias(), speed);
            double rearHandbrake = (handbrake && w.config().handbrakeAffected()) ? handbrakeTorqueNm : 0.0;
            out[i] = service + rearHandbrake;
        }
        return out;
    }

    private double averageDrivenWheelOmega(List<WheelState> wheels) {
        double sum = 0.0;
        int count = 0;
        for (WheelState wheel : wheels) {
            if (wheel.config().driven()) {
                sum += wheel.angularVelocity();
                count++;
            }
        }
        return count > 0 ? sum / count : 0.0;
    }
}
