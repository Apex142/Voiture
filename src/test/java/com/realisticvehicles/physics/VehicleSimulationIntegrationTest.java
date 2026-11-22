package com.realisticvehicles.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.realisticvehicles.core.event.SimpleEventBus;
import com.realisticvehicles.core.event.VehicleCollisionEvent;
import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyConfig;
import com.realisticvehicles.physics.simulation.VehicleConfig;
import com.realisticvehicles.physics.simulation.VehicleInput;
import com.realisticvehicles.physics.simulation.VehiclePresetFactory;
import com.realisticvehicles.physics.simulation.VehicleSimulation;
import com.realisticvehicles.physics.tires.BrushTireModel;
import com.realisticvehicles.physics.world.CollisionBox;
import com.realisticvehicles.physics.world.SurfaceProperties;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class VehicleSimulationIntegrationTest {

    private static final double TICK_DT = 0.05;

    private static void stepSeconds(VehicleSimulation sim, TestCollisionWorld world, double seconds) {
        int ticks = (int) Math.round(seconds / TICK_DT);
        for (int i = 0; i < ticks; i++) {
            sim.step(TICK_DT, world);
            assertTrue(sim.body().position().isFinite(), "Position must stay finite");
            assertTrue(sim.body().linearVelocity().isFinite(), "Velocity must stay finite");
            assertTrue(sim.body().orientation().isFinite(), "Orientation must stay finite");
        }
    }

    @Test
    void vehicleSettlesAndRemainsStableAtRest() {
        TestCollisionWorld flat = new TestCollisionWorld(0.0, SurfaceProperties.ASPHALT);
        VehicleSimulation sim = new VehicleSimulation(
            VehiclePresetFactory.createSedan(), new Vec3d(0.0, 0.85, 0.0), Quat4d.IDENTITY
        );

        stepSeconds(sim, flat, 3.0);
        Vec3d settledPos = sim.body().position();
        assertEquals(4, sim.telemetry().groundedWheels(), "All 4 wheels must contact ground");
        assertTrue(settledPos.y() > 0.45 && settledPos.y() < 0.85, "Chassis rides at realistic height");
        assertTrue(sim.telemetry().speedMps() < 0.02, "Vehicle must not drift or bounce at rest");

        stepSeconds(sim, flat, 3.0);
        assertEquals(settledPos.y(), sim.body().position().y(), 0.01, "Rest height stays stable");
    }

    @Test
    void vehicleAcceleratesBrakesAndReversesRealistically() {
        TestCollisionWorld flat = new TestCollisionWorld(0.0, SurfaceProperties.ASPHALT);
        VehicleSimulation sim = new VehicleSimulation(
            VehiclePresetFactory.createSedan(), new Vec3d(0.0, 0.72, 0.0), Quat4d.IDENTITY
        );
        stepSeconds(sim, flat, 1.0);

        sim.setInput(new VehicleInput(1.0, 0.0, 0.0, false));
        stepSeconds(sim, flat, 4.0);
        double topSpeed = sim.body().forwardSpeed();
        assertTrue(topSpeed > 15.0, "Sedan must accelerate strongly (>54 km/h in 4s), got: " + topSpeed);
        assertTrue(sim.body().position().z() > 25.0);

        sim.setInput(new VehicleInput(0.0, 1.0, 0.0, false));
        stepSeconds(sim, flat, 2.2);
        assertTrue(sim.body().forwardSpeed() < 2.0, "Braking must rapidly reduce forward speed");

        sim.setInput(new VehicleInput(0.0, 0.0, 0.0, true));
        stepSeconds(sim, flat, 1.5);
        assertTrue(sim.telemetry().speedMps() < 0.2, "Handbrake stops vehicle");

        sim.setInput(new VehicleInput(0.0, 1.0, 0.0, false));
        stepSeconds(sim, flat, 2.5);
        assertTrue(sim.body().forwardSpeed() < -3.0, "Holding S from rest must reverse");
        assertTrue(sim.telemetry().reversing());
    }

    @Test
    void steeringTurnsLeftAndRightCorrectly() {
        TestCollisionWorld flat = new TestCollisionWorld(0.0, SurfaceProperties.ASPHALT);
        VehicleSimulation leftCar = new VehicleSimulation(
            VehiclePresetFactory.createSedan(), new Vec3d(0.0, 0.72, 0.0), Quat4d.IDENTITY
        );
        VehicleSimulation rightCar = new VehicleSimulation(
            VehiclePresetFactory.createSedan(), new Vec3d(0.0, 0.72, 0.0), Quat4d.IDENTITY
        );
        stepSeconds(leftCar, flat, 0.5);
        stepSeconds(rightCar, flat, 0.5);

        leftCar.setInput(new VehicleInput(0.7, 0.0, 1.0, false));
        rightCar.setInput(new VehicleInput(0.7, 0.0, -1.0, false));
        stepSeconds(leftCar, flat, 3.0);
        stepSeconds(rightCar, flat, 3.0);

        assertTrue(leftCar.body().position().x() > 4.0, "Left steer (+1) moves toward +X");
        assertTrue(leftCar.body().orientation().yawRadians() > 0.35);
        assertTrue(rightCar.body().position().x() < -4.0, "Right steer (-1) moves toward -X");
        assertTrue(rightCar.body().orientation().yawRadians() < -0.35);
    }

    @Test
    void massAndSurfaceGripSignificantlyChangeAcceleration() {
        TestCollisionWorld asphalt = new TestCollisionWorld(0.0, SurfaceProperties.ASPHALT);
        TestCollisionWorld ice = new TestCollisionWorld(0.0, SurfaceProperties.ICE);

        VehicleConfig base = VehiclePresetFactory.createSedan();
        VehicleConfig heavy = VehicleConfig.builder("heavy")
            .body(RigidBodyConfig.builder().mass(2600.0).dimensions(1.86, 0.72, 4.40).build())
            .wheels(base.wheels())
            .engine(base.engine())
            .transmission(base.transmission())
            .steering(base.steering())
            .build();

        VehicleSimulation normalAsphalt = new VehicleSimulation(base, new Vec3d(0, 0.72, 0), Quat4d.IDENTITY);
        VehicleSimulation heavyAsphalt = new VehicleSimulation(heavy, new Vec3d(0, 0.72, 0), Quat4d.IDENTITY);
        VehicleSimulation normalIce = new VehicleSimulation(base, new Vec3d(0, 0.72, 0), Quat4d.IDENTITY);

        VehicleInput fullGas = new VehicleInput(1.0, 0.0, 0.0, false);
        normalAsphalt.setInput(fullGas);
        heavyAsphalt.setInput(fullGas);
        normalIce.setInput(fullGas);

        stepSeconds(normalAsphalt, asphalt, 3.0);
        stepSeconds(heavyAsphalt, asphalt, 3.0);
        stepSeconds(normalIce, ice, 3.0);

        assertTrue(normalAsphalt.body().forwardSpeed() > heavyAsphalt.body().forwardSpeed() + 2.0);
        assertTrue(normalAsphalt.body().forwardSpeed() > normalIce.body().forwardSpeed() * 2.5);
    }

    @Test
    void vehicleClimbsSlopeAndStopsAgainstSolidWall() {
        TestCollisionWorld ramp = new TestCollisionWorld(0.0, Math.toRadians(10.0), 6.0, SurfaceProperties.ASPHALT, List.of());
        VehicleSimulation climber = new VehicleSimulation(
            VehiclePresetFactory.createSedan(), new Vec3d(0.0, 0.72, 0.0), Quat4d.IDENTITY
        );
        stepSeconds(climber, ramp, 0.5);
        climber.setInput(new VehicleInput(1.0, 0.0, 0.0, false));
        stepSeconds(climber, ramp, 4.5);
        assertTrue(climber.body().position().y() > 2.5, "Vehicle must climb 10-degree ramp");

        CollisionBox wall = new CollisionBox(-10.0, 0.0, 20.0, 10.0, 4.0, 22.0);
        TestCollisionWorld walled = new TestCollisionWorld(0.0, 0.0, Double.POSITIVE_INFINITY, SurfaceProperties.ASPHALT, List.of(wall));
        SimpleEventBus bus = new SimpleEventBus();
        AtomicInteger impacts = new AtomicInteger();
        bus.subscribe(VehicleCollisionEvent.class, e -> impacts.incrementAndGet());

        VehicleSimulation crasher = new VehicleSimulation(
            VehiclePresetFactory.createSedan(), new Vec3d(0.0, 0.72, 0.0), Quat4d.IDENTITY, new BrushTireModel(), bus
        );
        crasher.setInput(new VehicleInput(1.0, 0.0, 0.0, false));
        stepSeconds(crasher, walled, 3.5);

        assertTrue(impacts.get() > 0, "Wall impact must emit VehicleCollisionEvent");
        assertTrue(crasher.body().position().z() < 19.5, "Vehicle must not penetrate wall at Z=20");
        assertFalse(Double.isNaN(crasher.body().position().z()));
    }

    @Test
    void vehicleClimbsAndDescendsOneBlockStepWithoutClipping() {
        CollisionBox step = new CollisionBox(-10.0, 0.0, 8.0, 10.0, 1.0, 22.0);
        TestCollisionWorld world = new TestCollisionWorld(0.0, 0.0, Double.POSITIVE_INFINITY, SurfaceProperties.ASPHALT, List.of(step));
        VehicleSimulation slk = new VehicleSimulation(
            VehiclePresetFactory.createSlk55Amg(), new Vec3d(0.0, 0.72, 0.0), Quat4d.IDENTITY
        );
        stepSeconds(slk, world, 0.5);
        slk.setInput(new VehicleInput(0.85, 0.0, 0.0, false));
        stepSeconds(slk, world, 2.5);
        assertTrue(slk.body().position().y() > 1.38, "SLK55 must climb onto 1.0m block step without clipping, y=" + slk.body().position().y());
        assertTrue(slk.body().position().z() > 10.0);

        stepSeconds(slk, world, 3.0);
        assertTrue(slk.body().position().z() > 24.0, "SLK55 must descend off the 1.0m step");
        assertTrue(slk.body().position().y() < 0.75, "SLK55 must settle back down on ground");
    }
}
