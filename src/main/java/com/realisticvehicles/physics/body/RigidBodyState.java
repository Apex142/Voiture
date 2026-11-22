package com.realisticvehicles.physics.body;

import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;

/**
 * Mutable dynamic state of a 3D rigid body in world space.
 */
public final class RigidBodyState {
    private Vec3d position;
    private Vec3d linearVelocity;
    private Quat4d orientation;
    private Vec3d angularVelocity;
    private Vec3d accumulatedForce = Vec3d.ZERO;
    private Vec3d accumulatedTorque = Vec3d.ZERO;

    public RigidBodyState(Vec3d position, Quat4d orientation) {
        this.position = position.sanitize();
        this.orientation = orientation.normalize();
        this.linearVelocity = Vec3d.ZERO;
        this.angularVelocity = Vec3d.ZERO;
    }

    public Vec3d position() {
        return position;
    }

    public void setPosition(Vec3d position) {
        this.position = position.sanitize();
    }

    public Vec3d linearVelocity() {
        return linearVelocity;
    }

    public void setLinearVelocity(Vec3d linearVelocity) {
        this.linearVelocity = linearVelocity.sanitize();
    }

    public Quat4d orientation() {
        return orientation;
    }

    public void setOrientation(Quat4d orientation) {
        this.orientation = orientation.isFinite() ? orientation.normalize() : Quat4d.IDENTITY;
    }

    public Vec3d angularVelocity() {
        return angularVelocity;
    }

    public void setAngularVelocity(Vec3d angularVelocity) {
        this.angularVelocity = angularVelocity.sanitize();
    }

    public Vec3d accumulatedForce() {
        return accumulatedForce;
    }

    public Vec3d accumulatedTorque() {
        return accumulatedTorque;
    }

    public void addForce(Vec3d worldForce) {
        if (worldForce.isFinite()) {
            accumulatedForce = accumulatedForce.add(worldForce);
        }
    }

    public void addTorque(Vec3d worldTorque) {
        if (worldTorque.isFinite()) {
            accumulatedTorque = accumulatedTorque.add(worldTorque);
        }
    }

    public void addForceAtPoint(Vec3d worldForce, Vec3d worldPoint) {
        if (!worldForce.isFinite() || !worldPoint.isFinite()) {
            return;
        }
        accumulatedForce = accumulatedForce.add(worldForce);
        Vec3d leverArm = worldPoint.sub(position);
        accumulatedTorque = accumulatedTorque.add(leverArm.cross(worldForce));
    }

    public void clearAccumulators() {
        accumulatedForce = Vec3d.ZERO;
        accumulatedTorque = Vec3d.ZERO;
    }

    /** Velocity of an arbitrary world-space point rigidly attached to this body. */
    public Vec3d velocityAtPoint(Vec3d worldPoint) {
        Vec3d leverArm = worldPoint.sub(position);
        return linearVelocity.add(angularVelocity.cross(leverArm));
    }

    public Vec3d localToWorldPoint(Vec3d localPoint) {
        return position.add(orientation.rotate(localPoint));
    }

    public Vec3d worldToLocalPoint(Vec3d worldPoint) {
        return orientation.inverseRotate(worldPoint.sub(position));
    }

    public double forwardSpeed() {
        return linearVelocity.dot(orientation.forward());
    }
}
