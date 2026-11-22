package com.realisticvehicles.physics.suspension;

import com.realisticvehicles.core.math.MathUtil;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.world.SurfaceProperties;

/**
 * Mutable per-wheel dynamic telemetry and contact state.
 */
public final class WheelState {
    private final WheelConfig config;
    private boolean grounded;
    private double compression;
    private double previousCompression;
    private double normalForce;
    private Vec3d contactPoint = Vec3d.ZERO;
    private Vec3d contactNormal = Vec3d.UNIT_Y;
    private SurfaceProperties surface = SurfaceProperties.ASPHALT;
    private double steerAngle;
    private double angularVelocity;
    private double spinAngle;
    private double slipRatio;
    private double slipAngle;

    public WheelState(WheelConfig config) {
        this.config = config;
    }

    public WheelConfig config() {
        return config;
    }

    public boolean grounded() {
        return grounded;
    }

    public double compression() {
        return compression;
    }

    public double previousCompression() {
        return previousCompression;
    }

    public double normalForce() {
        return normalForce;
    }

    public Vec3d contactPoint() {
        return contactPoint;
    }

    public Vec3d contactNormal() {
        return contactNormal;
    }

    public SurfaceProperties surface() {
        return surface;
    }

    public double steerAngle() {
        return steerAngle;
    }

    public void setSteerAngle(double steerAngle) {
        this.steerAngle = MathUtil.sanitize(steerAngle);
    }

    public double angularVelocity() {
        return angularVelocity;
    }

    public void setAngularVelocity(double angularVelocity) {
        this.angularVelocity = MathUtil.clamp(angularVelocity, -250.0, 250.0);
    }

    public double spinAngle() {
        return spinAngle;
    }

    public void advanceSpin(double dt) {
        spinAngle = (spinAngle + angularVelocity * dt) % (Math.PI * 2.0);
    }

    public double slipRatio() {
        return slipRatio;
    }

    public double slipAngle() {
        return slipAngle;
    }

    public void updateSlip(double slipRatio, double slipAngle) {
        this.slipRatio = MathUtil.sanitize(slipRatio);
        this.slipAngle = MathUtil.sanitize(slipAngle);
    }

    public void updateContact(
        boolean grounded,
        double compression,
        double normalForce,
        Vec3d contactPoint,
        Vec3d contactNormal,
        SurfaceProperties surface
    ) {
        this.previousCompression = this.compression;
        this.grounded = grounded;
        this.compression = MathUtil.clamp(compression, 0.0, config.restLength());
        this.normalForce = Math.max(0.0, MathUtil.sanitize(normalForce));
        this.contactPoint = contactPoint.sanitize();
        this.contactNormal = contactNormal.lengthSquared() > 1e-6 ? contactNormal.normalize() : Vec3d.UNIT_Y;
        this.surface = surface != null ? surface : SurfaceProperties.ASPHALT;
    }

    /** Hub offset relative to chassis center of mass in local coordinates. */
    public Vec3d localHubOffset() {
        double currentLength = config.restLength() - compression;
        return config.mountPoint().add(new Vec3d(0.0, -currentLength, 0.0));
    }

    public void resetSpin(double spinAngle, double angularVelocity) {
        this.spinAngle = MathUtil.sanitize(spinAngle);
        this.angularVelocity = MathUtil.sanitize(angularVelocity);
    }
}
