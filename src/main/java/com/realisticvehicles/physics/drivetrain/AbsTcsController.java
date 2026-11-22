package com.realisticvehicles.physics.drivetrain;

import com.realisticvehicles.core.math.MathUtil;
import com.realisticvehicles.physics.suspension.WheelState;

/**
 * Electronic ABS (Anti-lock Braking System) and TCS (Traction Control System).
 */
public final class AbsTcsController {
    private static final double ABS_SLIP_THRESHOLD = -0.22;
    private static final double TCS_SLIP_THRESHOLD = 0.25;
    private static final double MIN_ACTIVE_SPEED_MPS = 2.5;

    private final boolean absEnabled;
    private final boolean tcsEnabled;

    public AbsTcsController(boolean absEnabled, boolean tcsEnabled) {
        this.absEnabled = absEnabled;
        this.tcsEnabled = tcsEnabled;
    }

    public double modulateBrakeTorque(WheelState wheel, double requestedBrakeTorque, double vehicleSpeedMps) {
        if (!absEnabled || Math.abs(vehicleSpeedMps) < MIN_ACTIVE_SPEED_MPS || !wheel.grounded()) {
            return requestedBrakeTorque;
        }
        if (wheel.slipRatio() < ABS_SLIP_THRESHOLD) {
            double excess = Math.abs(wheel.slipRatio() - ABS_SLIP_THRESHOLD);
            double factor = MathUtil.clamp(1.0 - excess * 1.8, 0.20, 1.0);
            return requestedBrakeTorque * factor;
        }
        return requestedBrakeTorque;
    }

    public double modulateDriveTorque(WheelState wheel, double requestedDriveTorque, double vehicleSpeedMps) {
        if (!tcsEnabled || Math.abs(vehicleSpeedMps) < MIN_ACTIVE_SPEED_MPS || !wheel.grounded()) {
            return requestedDriveTorque;
        }
        if (wheel.slipRatio() > TCS_SLIP_THRESHOLD && requestedDriveTorque > 0.0) {
            double excess = wheel.slipRatio() - TCS_SLIP_THRESHOLD;
            double factor = MathUtil.clamp(1.0 - excess * 1.6, 0.25, 1.0);
            return requestedDriveTorque * factor;
        }
        return requestedDriveTorque;
    }
}
