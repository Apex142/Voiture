package com.realisticvehicles.physics.simulation;

import com.realisticvehicles.physics.body.RigidBodyConfig;
import com.realisticvehicles.physics.drivetrain.DifferentialStrategy;
import com.realisticvehicles.physics.drivetrain.EngineConfig;
import com.realisticvehicles.physics.drivetrain.OpenDifferential;
import com.realisticvehicles.physics.drivetrain.TransmissionConfig;
import com.realisticvehicles.physics.steering.SteeringConfig;
import com.realisticvehicles.physics.suspension.WheelConfig;
import java.util.List;
import java.util.Objects;

/**
 * Composite immutable vehicle specification assembled via Builder pattern.
 */
public final class VehicleConfig {
    private final String presetId;
    private final RigidBodyConfig body;
    private final List<WheelConfig> wheels;
    private final EngineConfig engine;
    private final TransmissionConfig transmission;
    private final SteeringConfig steering;
    private final DifferentialStrategy differential;
    private final double maxServiceBrakeTorqueNm;
    private final double handbrakeTorqueNm;
    private final double frontAntiRollStiffness;
    private final double rearAntiRollStiffness;
    private final boolean absEnabled;
    private final boolean tcsEnabled;

    private VehicleConfig(Builder b) {
        this.presetId = Objects.requireNonNull(b.presetId, "presetId");
        this.body = Objects.requireNonNull(b.body, "body");
        this.wheels = List.copyOf(Objects.requireNonNull(b.wheels, "wheels"));
        if (this.wheels.size() != 4) {
            throw new IllegalArgumentException("VehicleConfig requires exactly 4 wheels");
        }
        this.engine = Objects.requireNonNull(b.engine, "engine");
        this.transmission = Objects.requireNonNull(b.transmission, "transmission");
        this.steering = Objects.requireNonNull(b.steering, "steering");
        this.differential = Objects.requireNonNull(b.differential, "differential");
        this.maxServiceBrakeTorqueNm = b.maxServiceBrakeTorqueNm;
        this.handbrakeTorqueNm = b.handbrakeTorqueNm;
        this.frontAntiRollStiffness = b.frontAntiRollStiffness;
        this.rearAntiRollStiffness = b.rearAntiRollStiffness;
        this.absEnabled = b.absEnabled;
        this.tcsEnabled = b.tcsEnabled;
    }

    public static Builder builder(String presetId) {
        return new Builder(presetId);
    }

    public String presetId() {
        return presetId;
    }

    public RigidBodyConfig body() {
        return body;
    }

    public List<WheelConfig> wheels() {
        return wheels;
    }

    public EngineConfig engine() {
        return engine;
    }

    public TransmissionConfig transmission() {
        return transmission;
    }

    public SteeringConfig steering() {
        return steering;
    }

    public DifferentialStrategy differential() {
        return differential;
    }

    public double maxServiceBrakeTorqueNm() {
        return maxServiceBrakeTorqueNm;
    }

    public double handbrakeTorqueNm() {
        return handbrakeTorqueNm;
    }

    public double frontAntiRollStiffness() {
        return frontAntiRollStiffness;
    }

    public double rearAntiRollStiffness() {
        return rearAntiRollStiffness;
    }

    public boolean absEnabled() {
        return absEnabled;
    }

    public boolean tcsEnabled() {
        return tcsEnabled;
    }

    public static final class Builder {
        private final String presetId;
        private RigidBodyConfig body = RigidBodyConfig.builder().build();
        private List<WheelConfig> wheels = List.of();
        private EngineConfig engine = EngineConfig.standardSedan();
        private TransmissionConfig transmission = TransmissionConfig.sixSpeedAutomatic();
        private SteeringConfig steering = SteeringConfig.standardSedan();
        private DifferentialStrategy differential = new OpenDifferential();
        private double maxServiceBrakeTorqueNm = 5200.0;
        private double handbrakeTorqueNm = 3400.0;
        private double frontAntiRollStiffness = 7500.0;
        private double rearAntiRollStiffness = 6000.0;
        private boolean absEnabled = true;
        private boolean tcsEnabled = true;

        private Builder(String presetId) {
            this.presetId = presetId;
        }

        public Builder body(RigidBodyConfig body) {
            this.body = body;
            return this;
        }

        public Builder wheels(List<WheelConfig> wheels) {
            this.wheels = wheels;
            return this;
        }

        public Builder engine(EngineConfig engine) {
            this.engine = engine;
            return this;
        }

        public Builder transmission(TransmissionConfig transmission) {
            this.transmission = transmission;
            return this;
        }

        public Builder steering(SteeringConfig steering) {
            this.steering = steering;
            return this;
        }

        public Builder differential(DifferentialStrategy differential) {
            this.differential = differential;
            return this;
        }

        public Builder antiRollBars(double frontStiffness, double rearStiffness) {
            this.frontAntiRollStiffness = frontStiffness;
            this.rearAntiRollStiffness = rearStiffness;
            return this;
        }

        public Builder electronics(boolean absEnabled, boolean tcsEnabled) {
            this.absEnabled = absEnabled;
            this.tcsEnabled = tcsEnabled;
            return this;
        }

        public Builder brakes(double maxServiceBrakeTorqueNm, double handbrakeTorqueNm) {
            this.maxServiceBrakeTorqueNm = maxServiceBrakeTorqueNm;
            this.handbrakeTorqueNm = handbrakeTorqueNm;
            return this;
        }

        public VehicleConfig build() {
            return new VehicleConfig(this);
        }
    }
}
