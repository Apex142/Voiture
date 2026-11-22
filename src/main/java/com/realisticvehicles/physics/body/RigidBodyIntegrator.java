package com.realisticvehicles.physics.body;

import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;

/**
 * Symplectic (semi-implicit Euler) rigid body integrator with safety velocity clamps.
 */
public final class RigidBodyIntegrator {
    private static final double AIR_DENSITY = 1.225;
    private static final double MAX_LINEAR_SPEED = 110.0;
    private static final double MAX_ANGULAR_SPEED = 12.0;

    public void applyAerodynamicDrag(RigidBodyState state, RigidBodyConfig config) {
        Vec3d vel = state.linearVelocity();
        double speedSq = vel.lengthSquared();
        if (speedSq < 1e-6) {
            return;
        }
        double speed = Math.sqrt(speedSq);
        double dragMag = 0.5 * AIR_DENSITY * config.dragCoefficient() * config.frontalArea() * speedSq;
        state.addForce(vel.scale(-dragMag / speed));
    }

    public void integrate(RigidBodyState state, RigidBodyConfig config, double dt) {
        if (dt <= 0.0 || !Double.isFinite(dt)) {
            return;
        }
        Vec3d linearAcc = state.accumulatedForce().scale(config.inverseMass());
        Vec3d nextVel = state.linearVelocity().addScaled(linearAcc, dt);
        double linearDecay = Math.exp(-config.linearDamping() * dt);
        nextVel = nextVel.scale(linearDecay).clampLength(MAX_LINEAR_SPEED).sanitize();

        Vec3d angularAcc = config.inverseInertia().multiplyWorld(
            state.orientation(),
            state.accumulatedTorque()
        );
        Vec3d nextOmega = state.angularVelocity().addScaled(angularAcc, dt);
        double angularDecay = Math.exp(-config.angularDamping() * dt);
        nextOmega = nextOmega.scale(angularDecay).clampLength(MAX_ANGULAR_SPEED).sanitize();

        Vec3d nextPos = state.position().addScaled(nextVel, dt).sanitize();
        Quat4d nextOrientation = state.orientation().integrate(nextOmega, dt);

        state.setLinearVelocity(nextVel);
        state.setAngularVelocity(nextOmega);
        state.setPosition(nextPos);
        state.setOrientation(nextOrientation);
        state.clearAccumulators();
    }
}
