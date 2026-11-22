package com.realisticvehicles.physics.suspension;

import com.realisticvehicles.core.math.Vec3d;

/**
 * Immutable configuration of a single vehicle wheel and suspension strut.
 */
public final class WheelConfig {
    private final String id;
    private final Vec3d mountPoint;
    private final double restLength;
    private final double radius;
    private final double width;
    private final double springStiffness;
    private final double bumpDamping;
    private final double reboundDamping;
    private final double wheelInertia;
    private final double gripFactor;
    private final boolean steerable;
    private final boolean driven;
    private final boolean handbrakeAffected;
    private final double brakeBias;

    private WheelConfig(Builder b) {
        this.id = b.id;
        this.mountPoint = b.mountPoint;
        this.restLength = b.restLength;
        this.radius = b.radius;
        this.width = b.width;
        this.springStiffness = b.springStiffness;
        this.bumpDamping = b.bumpDamping;
        this.reboundDamping = b.reboundDamping;
        this.wheelInertia = b.wheelInertia;
        this.gripFactor = b.gripFactor;
        this.steerable = b.steerable;
        this.driven = b.driven;
        this.handbrakeAffected = b.handbrakeAffected;
        this.brakeBias = b.brakeBias;
    }

    public static Builder builder(String id, Vec3d mountPoint) {
        return new Builder(id, mountPoint);
    }

    public String id() {
        return id;
    }

    public Vec3d mountPoint() {
        return mountPoint;
    }

    public double restLength() {
        return restLength;
    }

    public double radius() {
        return radius;
    }

    public double width() {
        return width;
    }

    public double springStiffness() {
        return springStiffness;
    }

    public double bumpDamping() {
        return bumpDamping;
    }

    public double reboundDamping() {
        return reboundDamping;
    }

    public double wheelInertia() {
        return wheelInertia;
    }

    public double gripFactor() {
        return gripFactor;
    }

    public boolean steerable() {
        return steerable;
    }

    public boolean driven() {
        return driven;
    }

    public boolean handbrakeAffected() {
        return handbrakeAffected;
    }

    public double brakeBias() {
        return brakeBias;
    }

    public static final class Builder {
        private final String id;
        private final Vec3d mountPoint;
        private double restLength = 0.42;
        private double radius = 0.34;
        private double width = 0.22;
        private double springStiffness = 26000.0;
        private double bumpDamping = 3000.0;
        private double reboundDamping = 4200.0;
        private double wheelInertia = 1.25;
        private double gripFactor = 1.15;
        private boolean steerable;
        private boolean driven;
        private boolean handbrakeAffected;
        private double brakeBias = 0.25;

        private Builder(String id, Vec3d mountPoint) {
            this.id = id;
            this.mountPoint = mountPoint;
        }

        public Builder geometry(double restLength, double radius, double width) {
            this.restLength = restLength;
            this.radius = radius;
            this.width = width;
            return this;
        }

        public Builder suspension(double stiffness, double bumpDamping, double reboundDamping) {
            this.springStiffness = stiffness;
            this.bumpDamping = bumpDamping;
            this.reboundDamping = reboundDamping;
            return this;
        }

        public Builder tire(double wheelInertia, double gripFactor) {
            this.wheelInertia = wheelInertia;
            this.gripFactor = gripFactor;
            return this;
        }

        public Builder roles(boolean steerable, boolean driven, boolean handbrakeAffected, double brakeBias) {
            this.steerable = steerable;
            this.driven = driven;
            this.handbrakeAffected = handbrakeAffected;
            this.brakeBias = brakeBias;
            return this;
        }

        public WheelConfig build() {
            return new WheelConfig(this);
        }
    }
}
