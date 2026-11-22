package com.realisticvehicles.physics.suspension;

import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyState;
import java.util.List;

/**
 * Computes anti-roll bar (sway bar) restoring forces across left/right axle pairs
 * to control body roll and lateral load transfer.
 */
public final class AntiRollBarSolver {

    public void applyAxleAntiRoll(
        RigidBodyState body,
        List<WheelState> wheels,
        double frontStiffness,
        double rearStiffness
    ) {
        if (wheels.size() < 4) {
            return;
        }
        applyPair(body, wheels.get(0), wheels.get(1), frontStiffness);
        applyPair(body, wheels.get(2), wheels.get(3), rearStiffness);
    }

    private void applyPair(RigidBodyState body, WheelState left, WheelState right, double stiffness) {
        if (stiffness <= 0.0 || (!left.grounded() && !right.grounded())) {
            return;
        }
        double travelDiff = left.compression() - right.compression();
        double antiRollForce = travelDiff * stiffness;
        Vec3d up = body.orientation().up();

        if (left.grounded()) {
            Vec3d leftMount = body.localToWorldPoint(left.config().mountPoint());
            body.addForceAtPoint(up.scale(-antiRollForce), leftMount);
        }
        if (right.grounded()) {
            Vec3d rightMount = body.localToWorldPoint(right.config().mountPoint());
            body.addForceAtPoint(up.scale(antiRollForce), rightMount);
        }
    }
}
