package com.realisticvehicles.physics.steering;

import com.realisticvehicles.core.math.MathUtil;
import com.realisticvehicles.physics.suspension.WheelState;
import java.util.List;

/**
 * Computes Ackermann front-wheel steering angles with speed-sensitive authority.
 * Positive steer input turns LEFT (+X in vehicle local space).
 */
public final class AckermannSteeringSolver {
    private double currentSteerAngle;

    public double currentSteerAngle() {
        return currentSteerAngle;
    }

    public void setCurrentSteerAngle(double angleRadians) {
        this.currentSteerAngle = MathUtil.sanitize(angleRadians);
    }

    public void updateSteering(
        double steerInput,
        double speedMps,
        SteeringConfig config,
        List<WheelState> wheels,
        double dt
    ) {
        double clampedInput = MathUtil.clamp(steerInput, -1.0, 1.0);
        double speedRatio = MathUtil.clamp(Math.abs(speedMps) / config.highSpeedReferenceMps(), 0.0, 1.0);
        double authority = MathUtil.lerp(1.0, config.highSpeedSteerFactor(), speedRatio * speedRatio);
        double targetAngle = clampedInput * config.maxSteerAngleRadians() * authority;

        double rate = Math.abs(targetAngle) < Math.abs(currentSteerAngle)
            ? config.returnRateRadPerSec()
            : config.steerRateRadPerSec();
        currentSteerAngle = MathUtil.moveTowards(currentSteerAngle, targetAngle, rate * dt);

        applyAckermann(config, wheels);
    }

    private void applyAckermann(SteeringConfig config, List<WheelState> wheels) {
        double centerAngle = currentSteerAngle;
        if (Math.abs(centerAngle) < 1e-4) {
            for (WheelState wheel : wheels) {
                if (wheel.config().steerable()) {
                    wheel.setSteerAngle(0.0);
                }
            }
            return;
        }
        double turnRadius = config.wheelbase() / Math.tan(Math.abs(centerAngle));
        double halfTrack = config.trackWidth() * 0.5;
        double innerAngle = Math.atan2(config.wheelbase(), Math.max(0.25, turnRadius - halfTrack));
        double outerAngle = Math.atan2(config.wheelbase(), turnRadius + halfTrack);
        double sign = Math.signum(centerAngle);

        for (WheelState wheel : wheels) {
            if (!wheel.config().steerable()) {
                wheel.setSteerAngle(0.0);
                continue;
            }
            boolean isLeftWheel = wheel.config().mountPoint().x() > 0.0;
            boolean isInner = (sign > 0.0 && isLeftWheel) || (sign < 0.0 && !isLeftWheel);
            wheel.setSteerAngle(sign * (isInner ? innerAngle : outerAngle));
        }
    }
}
