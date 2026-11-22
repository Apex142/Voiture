package com.realisticvehicles.physics.simulation;

import com.realisticvehicles.core.event.EventBus;
import com.realisticvehicles.core.event.SimpleEventBus;
import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyState;
import com.realisticvehicles.physics.drivetrain.DrivetrainOutput;
import com.realisticvehicles.physics.drivetrain.VehicleEngine;
import com.realisticvehicles.physics.drivetrain.VehicleTransmission;
import com.realisticvehicles.physics.steering.AckermannSteeringSolver;
import com.realisticvehicles.physics.suspension.WheelConfig;
import com.realisticvehicles.physics.suspension.WheelState;
import com.realisticvehicles.physics.tires.PacejkaTireModel;
import com.realisticvehicles.physics.tires.TireModel;
import com.realisticvehicles.physics.world.CollisionWorld;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Facade managing fixed-timestep (120 Hz) vehicle simulation independent of Minecraft.
 */
public final class VehicleSimulation {
    public static final double FIXED_DT = 1.0 / 120.0;
    private static final int MAX_SUBSTEPS = 8;

    private final VehicleConfig config;
    private final RigidBodyState body;
    private final List<WheelState> wheels;
    private final VehicleEngine engine;
    private final VehicleTransmission transmission;
    private final AckermannSteeringSolver steeringSolver;
    private final VehiclePhysicsStep physicsStep;
    private final EventBus eventBus;

    private VehicleInput currentInput = VehicleInput.NEUTRAL;
    private double accumulator;
    private boolean braking;
    private boolean reversing;

    public VehicleSimulation(VehicleConfig config, Vec3d initialPosition, Quat4d initialOrientation) {
        this(config, initialPosition, initialOrientation, new PacejkaTireModel(), new SimpleEventBus());
    }

    public VehicleSimulation(
        VehicleConfig config,
        Vec3d initialPosition,
        Quat4d initialOrientation,
        TireModel tireModel,
        EventBus eventBus
    ) {
        this.config = config;
        this.body = new RigidBodyState(initialPosition, initialOrientation);
        List<WheelState> createdWheels = new ArrayList<>(config.wheels().size());
        for (WheelConfig wheelCfg : config.wheels()) {
            createdWheels.add(new WheelState(wheelCfg));
        }
        this.wheels = Collections.unmodifiableList(createdWheels);
        this.engine = new VehicleEngine(config.engine());
        this.transmission = new VehicleTransmission(config.transmission());
        this.steeringSolver = new AckermannSteeringSolver();
        this.physicsStep = new VehiclePhysicsStep(config, tireModel, steeringSolver);
        this.eventBus = eventBus;
    }

    public void setInput(VehicleInput input) {
        this.currentInput = input != null ? input : VehicleInput.NEUTRAL;
    }

    public VehicleInput currentInput() {
        return currentInput;
    }

    public void step(double deltaSeconds, CollisionWorld world) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds <= 0.0) {
            return;
        }
        accumulator += Math.min(deltaSeconds, FIXED_DT * MAX_SUBSTEPS);
        int substeps = 0;
        while (accumulator >= FIXED_DT && substeps < MAX_SUBSTEPS) {
            DrivetrainOutput output = physicsStep.execute(
                config, body, wheels, engine, transmission, currentInput, world, eventBus, FIXED_DT
            );
            this.braking = output.braking();
            this.reversing = output.reversing();
            accumulator -= FIXED_DT;
            substeps++;
        }
    }

    public VehicleConfig config() {
        return config;
    }

    public RigidBodyState body() {
        return body;
    }

    public List<WheelState> wheels() {
        return wheels;
    }

    public VehicleEngine engine() {
        return engine;
    }

    public VehicleTransmission transmission() {
        return transmission;
    }

    public AckermannSteeringSolver steeringSolver() {
        return steeringSolver;
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public VehicleTelemetry telemetry() {
        double speedMps = body.linearVelocity().length();
        int grounded = 0;
        for (WheelState wheel : wheels) {
            if (wheel.grounded()) {
                grounded++;
            }
        }
        boolean sleeping = speedMps < 0.01 && body.angularVelocity().length() < 0.01 && grounded >= 3;
        return new VehicleTelemetry(
            config.presetId(),
            speedMps,
            speedMps * 3.6,
            engine.currentRpm(),
            transmission.currentGear(),
            Math.toDegrees(steeringSolver.currentSteerAngle()),
            grounded,
            braking,
            reversing,
            sleeping
        );
    }
}
