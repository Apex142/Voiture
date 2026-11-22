package com.realisticvehicles.physics.body;

import com.realisticvehicles.core.math.Mat3d;

/**
 * Immutable rigid body physical properties (mass, dimensions, inertia, damping).
 */
public final class RigidBodyConfig {
    private final double mass;
    private final double width;
    private final double height;
    private final double length;
    private final double linearDamping;
    private final double angularDamping;
    private final double dragCoefficient;
    private final double frontalArea;
    private final Mat3d inverseInertia;

    private RigidBodyConfig(Builder builder) {
        this.mass = builder.mass;
        this.width = builder.width;
        this.height = builder.height;
        this.length = builder.length;
        this.linearDamping = builder.linearDamping;
        this.angularDamping = builder.angularDamping;
        this.dragCoefficient = builder.dragCoefficient;
        this.frontalArea = builder.frontalArea;
        this.inverseInertia = Mat3d.solidBoxInertia(mass, width, height, length).inverse();
    }

    public static Builder builder() {
        return new Builder();
    }

    public double mass() {
        return mass;
    }

    public double inverseMass() {
        return 1.0 / mass;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public double length() {
        return length;
    }

    public double linearDamping() {
        return linearDamping;
    }

    public double angularDamping() {
        return angularDamping;
    }

    public double dragCoefficient() {
        return dragCoefficient;
    }

    public double frontalArea() {
        return frontalArea;
    }

    public Mat3d inverseInertia() {
        return inverseInertia;
    }

    public static final class Builder {
        private double mass = 1450.0;
        private double width = 1.86;
        private double height = 0.72;
        private double length = 4.40;
        private double linearDamping = 0.04;
        private double angularDamping = 0.35;
        private double dragCoefficient = 0.31;
        private double frontalArea = 2.25;

        public Builder mass(double mass) {
            if (mass <= 0.0 || !Double.isFinite(mass)) {
                throw new IllegalArgumentException("Mass must be positive finite");
            }
            this.mass = mass;
            return this;
        }

        public Builder dimensions(double width, double height, double length) {
            if (width <= 0.0 || height <= 0.0 || length <= 0.0) {
                throw new IllegalArgumentException("Dimensions must be positive");
            }
            this.width = width;
            this.height = height;
            this.length = length;
            return this;
        }

        public Builder damping(double linearDamping, double angularDamping) {
            this.linearDamping = Math.max(0.0, linearDamping);
            this.angularDamping = Math.max(0.0, angularDamping);
            return this;
        }

        public Builder aerodynamics(double dragCoefficient, double frontalArea) {
            this.dragCoefficient = Math.max(0.0, dragCoefficient);
            this.frontalArea = Math.max(0.0, frontalArea);
            return this;
        }

        public RigidBodyConfig build() {
            return new RigidBodyConfig(this);
        }
    }
}
