package com.realisticvehicles.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyConfig;
import com.realisticvehicles.physics.body.RigidBodyIntegrator;
import com.realisticvehicles.physics.body.RigidBodyState;
import com.realisticvehicles.physics.drivetrain.EngineConfig;
import com.realisticvehicles.physics.drivetrain.TransmissionConfig;
import com.realisticvehicles.physics.drivetrain.VehicleEngine;
import com.realisticvehicles.physics.drivetrain.VehicleTransmission;
import com.realisticvehicles.physics.simulation.VehicleConfig;
import com.realisticvehicles.physics.simulation.VehiclePresetFactory;
import com.realisticvehicles.physics.steering.AckermannSteeringSolver;
import com.realisticvehicles.physics.suspension.WheelState;
import com.realisticvehicles.physics.tires.BrushTireModel;
import com.realisticvehicles.physics.tires.TireContext;
import com.realisticvehicles.physics.tires.TireForces;
import com.realisticvehicles.physics.world.SurfaceProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

final class VehicleSubsystemsTest {

    @Test
    void heavierRigidBodyAcceleratesSlowerUnderEqualForce() {
        RigidBodyIntegrator integrator = new RigidBodyIntegrator();
        RigidBodyConfig light = RigidBodyConfig.builder().mass(1000.0).damping(0.0, 0.0).build();
        RigidBodyConfig heavy = RigidBodyConfig.builder().mass(2000.0).damping(0.0, 0.0).build();

        RigidBodyState sLight = new RigidBodyState(Vec3d.ZERO, Quat4d.IDENTITY);
        RigidBodyState sHeavy = new RigidBodyState(Vec3d.ZERO, Quat4d.IDENTITY);

        for (int i = 0; i < 60; i++) {
            sLight.addForce(new Vec3d(0.0, 0.0, 4000.0));
            sHeavy.addForce(new Vec3d(0.0, 0.0, 4000.0));
            integrator.integrate(sLight, light, 1.0 / 60.0);
            integrator.integrate(sHeavy, heavy, 1.0 / 60.0);
        }
        assertEquals(4.0, sLight.linearVelocity().z(), 0.05);
        assertEquals(2.0, sHeavy.linearVelocity().z(), 0.05);
    }

    @Test
    void ackermannInnerWheelSteersMoreThanOuterWheel() {
        VehicleConfig sedan = VehiclePresetFactory.createSedan();
        List<WheelState> wheels = sedan.wheels().stream().map(WheelState::new).toList();
        AckermannSteeringSolver solver = new AckermannSteeringSolver();

        for (int i = 0; i < 120; i++) {
            solver.updateSteering(1.0, 5.0, sedan.steering(), wheels, 1.0 / 120.0);
        }
        double fl = wheels.get(0).steerAngle();
        double fr = wheels.get(1).steerAngle();
        assertTrue(fl > fr && fr > 0.0, "Left turn must have FL > FR > 0");
        assertEquals(0.0, wheels.get(2).steerAngle(), 1e-9);
    }

    @Test
    void brushTireModelRespectsSurfaceGripAndFrictionCircle() {
        BrushTireModel model = new BrushTireModel();
        TireContext asphalt = new TireContext(3600.0, 12.0, 2.0, 35.0, 0.34, 1.25, 1600.0, 0.0, 1.15, SurfaceProperties.ASPHALT, 1.0 / 120.0);
        TireContext ice = new TireContext(3600.0, 12.0, 2.0, 35.0, 0.34, 1.25, 1600.0, 0.0, 1.15, SurfaceProperties.ICE, 1.0 / 120.0);

        TireForces fAsphalt = model.calculate(asphalt);
        TireForces fIce = model.calculate(ice);

        double magAsphalt = Math.hypot(fAsphalt.longitudinalForce(), fAsphalt.lateralForce());
        double magIce = Math.hypot(fIce.longitudinalForce(), fIce.lateralForce());
        assertTrue(magAsphalt > magIce * 3.0, "Asphalt must provide far higher grip than ice");
        assertTrue(magIce <= 3600.0 * 1.15 * SurfaceProperties.ICE.gripMultiplier() + 1e-3);
    }

    @Test
    void engineAndTransmissionShiftAndEngageReverse() {
        VehicleEngine engine = new VehicleEngine(EngineConfig.standardSedan());
        VehicleTransmission trans = new VehicleTransmission(TransmissionConfig.sixSpeedAutomatic());

        trans.updateAutomatic(1200.0, 0.0, 0.0, 1.0, 0.05);
        assertEquals(-1, trans.currentGear(), "Brake from rest engages reverse");

        trans.updateAutomatic(1500.0, 0.0, 1.0, 0.0, 0.05);
        assertEquals(1, trans.currentGear(), "Throttle from rest engages 1st gear");

        for (int i = 0; i < 50; i++) {
            trans.updateAutomatic(6000.0, 12.0, 1.0, 0.0, 0.02);
        }
        assertTrue(trans.currentGear() >= 2, "High RPM under throttle upshifts");
        assertTrue(engine.calculateTorque(1.0) > 150.0);
    }
}
