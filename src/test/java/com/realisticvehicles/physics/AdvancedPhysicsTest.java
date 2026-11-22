package com.realisticvehicles.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.collision.VehicleToVehicleCollider;
import com.realisticvehicles.physics.drivetrain.AbsTcsController;
import com.realisticvehicles.physics.drivetrain.DifferentialStrategy.AxleTorqueSplit;
import com.realisticvehicles.physics.drivetrain.LimitedSlipDifferential;
import com.realisticvehicles.physics.drivetrain.OpenDifferential;
import com.realisticvehicles.physics.simulation.VehicleInput;
import com.realisticvehicles.physics.simulation.VehiclePresetFactory;
import com.realisticvehicles.physics.simulation.VehicleSimulation;
import com.realisticvehicles.physics.suspension.WheelState;
import com.realisticvehicles.physics.tires.PacejkaTireModel;
import com.realisticvehicles.physics.tires.TireContext;
import com.realisticvehicles.physics.tires.TireForces;
import com.realisticvehicles.physics.world.SurfaceProperties;
import org.junit.jupiter.api.Test;

final class AdvancedPhysicsTest {

    @Test
    void pacejkaTireProducesRealisticPeakAndLateralGrip() {
        PacejkaTireModel pacejka = new PacejkaTireModel();
        TireContext asphalt = new TireContext(3900.0, 15.0, 2.5, 45.0, 0.34, 1.25, 1400.0, 0.0, 1.2, SurfaceProperties.ASPHALT, 1.0 / 120.0);
        TireContext ice = new TireContext(3900.0, 15.0, 2.5, 45.0, 0.34, 1.25, 1400.0, 0.0, 1.2, SurfaceProperties.ICE, 1.0 / 120.0);

        TireForces fA = pacejka.calculate(asphalt);
        TireForces fI = pacejka.calculate(ice);
        assertTrue(Math.abs(fA.lateralForce()) > Math.abs(fI.lateralForce()) * 3.0);
        assertTrue(Math.hypot(fA.longitudinalForce(), fA.lateralForce()) <= 3900.0 * 1.2 * 1.05 + 1.0);
    }

    @Test
    void lsdTransfersTorqueAwayFromSpinningWheelUnlikeOpenDiff() {
        OpenDifferential open = new OpenDifferential();
        LimitedSlipDifferential lsd = new LimitedSlipDifferential(0.8, 100.0);

        AxleTorqueSplit openSplit = open.split(1000.0, 80.0, 20.0);
        AxleTorqueSplit lsdSplit = lsd.split(1000.0, 80.0, 20.0);

        assertEquals(500.0, openSplit.leftTorque(), 1e-6);
        assertEquals(500.0, openSplit.rightTorque(), 1e-6);
        assertTrue(lsdSplit.rightTorque() > lsdSplit.leftTorque(), "LSD must bias torque to slower gripping wheel");
    }

    @Test
    void absReducesBrakeTorqueWhenWheelLocksUp() {
        AbsTcsController controller = new AbsTcsController(true, true);
        WheelState lockedWheel = new WheelState(VehiclePresetFactory.createSedan().wheels().get(0));
        lockedWheel.updateContact(true, 0.2, 3500.0, Vec3d.ZERO, Vec3d.UNIT_Y, SurfaceProperties.ICE);
        lockedWheel.updateSlip(-0.65, 0.0);

        double modulated = controller.modulateBrakeTorque(lockedWheel, 2000.0, 18.0);
        assertTrue(modulated < 1200.0, "ABS must modulate brake pressure on lockup");
    }

    @Test
    void hardCorneringTransfersLoadAndInducesRoll() {
        TestCollisionWorld flat = new TestCollisionWorld(0.0, SurfaceProperties.ASPHALT);
        VehicleSimulation slk = new VehicleSimulation(
            VehiclePresetFactory.createSlk55Amg(), new Vec3d(0.0, 0.72, 0.0), Quat4d.IDENTITY
        );
        slk.setInput(new VehicleInput(1.0, 0.0, 0.0, false));
        for (int i = 0; i < 40; i++) {
            slk.step(0.05, flat);
        }
        slk.setInput(new VehicleInput(0.7, 0.0, 1.0, false));
        for (int i = 0; i < 25; i++) {
            slk.step(0.05, flat);
        }
        double leftLoad = slk.wheels().get(0).normalForce() + slk.wheels().get(2).normalForce();
        double rightLoad = slk.wheels().get(1).normalForce() + slk.wheels().get(3).normalForce();
        assertTrue(rightLoad > leftLoad + 150.0,
            String.format("Left=%.1f Right=%.1f Grounded=%d Speed=%.1f Roll=%.1f",
                leftLoad, rightLoad, slk.telemetry().groundedWheels(), slk.telemetry().speedMps(),
                Math.toDegrees(slk.body().orientation().rollRadians())));
    }

    @Test
    void vehicleToVehicleCollisionConservesMomentum() {
        VehicleSimulation a = new VehicleSimulation(VehiclePresetFactory.createSedan(), new Vec3d(0.0, 0.7, 0.0), Quat4d.IDENTITY);
        VehicleSimulation b = new VehicleSimulation(VehiclePresetFactory.createSedan(), new Vec3d(0.0, 0.7, 3.2), Quat4d.IDENTITY);
        a.body().setLinearVelocity(new Vec3d(0.0, 0.0, 12.0));
        b.body().setLinearVelocity(Vec3d.ZERO);

        boolean hit = VehicleToVehicleCollider.resolvePair(a, b);
        assertTrue(hit);
        assertTrue(b.body().linearVelocity().z() > 4.0, "Stationary vehicle B must gain forward momentum");
        assertTrue(a.body().linearVelocity().z() < 8.0, "Impacting vehicle A must lose momentum");
    }
}
