package com.realisticvehicles.physics.collision;

import com.realisticvehicles.core.event.VehicleCollisionEvent;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyConfig;
import com.realisticvehicles.physics.body.RigidBodyState;
import com.realisticvehicles.physics.simulation.VehicleSimulation;

/**
 * Resolves momentum-conserving 3D collisions between two VehicleSimulation instances.
 */
public final class VehicleToVehicleCollider {
    private static final double RESTITUTION = 0.25;
    private static final double FRICTION = 0.35;

    private VehicleToVehicleCollider() {
    }

    public static boolean resolvePair(VehicleSimulation a, VehicleSimulation b) {
        RigidBodyState ba = a.body();
        RigidBodyState bb = b.body();
        RigidBodyConfig ca = a.config().body();
        RigidBodyConfig cb = b.config().body();

        Vec3d delta = bb.position().sub(ba.position());
        double maxDist = (ca.length() + cb.length()) * 0.55;
        if (delta.lengthSquared() > maxDist * maxDist) {
            return false;
        }

        double radA = boundingRadiusXZ(ca);
        double radB = boundingRadiusXZ(cb);
        double horizDist = Math.hypot(delta.x(), delta.z());
        double vertDiff = Math.abs(delta.y());
        if (vertDiff > (ca.height() + cb.height()) * 0.85 || horizDist >= radA + radB) {
            return false;
        }

        Vec3d normal = horizDist > 1e-5
            ? new Vec3d(delta.x() / horizDist, 0.0, delta.z() / horizDist)
            : new Vec3d(0.0, 0.0, 1.0);
        double penetration = (radA + radB) - horizDist;
        Vec3d contactPoint = ba.position().addScaled(normal, radA - penetration * 0.5);

        double invMassSum = ca.inverseMass() + cb.inverseMass();
        double corr = (penetration * 0.8) / Math.max(invMassSum, 1e-9);
        ba.setPosition(ba.position().addScaled(normal, -corr * ca.inverseMass()));
        bb.setPosition(bb.position().addScaled(normal, corr * cb.inverseMass()));

        Vec3d relVel = bb.velocityAtPoint(contactPoint).sub(ba.velocityAtPoint(contactPoint));
        double vn = relVel.dot(normal);
        if (vn >= 0.0) {
            return true;
        }

        Vec3d ra = contactPoint.sub(ba.position());
        Vec3d rb = contactPoint.sub(bb.position());
        double denom = effMass(ba, ca, ra, normal) + effMass(bb, cb, rb, normal);
        double jn = -(1.0 + RESTITUTION) * vn / Math.max(denom, 1e-6);

        Vec3d impulse = normal.scale(jn);
        applyImpulse(ba, ca, ra, impulse.negate());
        applyImpulse(bb, cb, rb, impulse);

        Vec3d tangent = relVel.sub(normal.scale(vn));
        double tSpeed = tangent.length();
        if (tSpeed > 1e-4) {
            Vec3d tDir = tangent.scale(1.0 / tSpeed);
            double tDenom = effMass(ba, ca, ra, tDir) + effMass(bb, cb, rb, tDir);
            double jt = Math.min(tSpeed / Math.max(tDenom, 1e-6), jn * FRICTION);
            Vec3d tImpulse = tDir.scale(jt);
            applyImpulse(ba, ca, ra, tImpulse);
            applyImpulse(bb, cb, rb, tImpulse.negate());
        }

        if (-vn >= 1.0) {
            a.eventBus().publish(new VehicleCollisionEvent(contactPoint, normal.negate(), jn, -vn));
            b.eventBus().publish(new VehicleCollisionEvent(contactPoint, normal, jn, -vn));
        }
        return true;
    }

    private static double boundingRadiusXZ(RigidBodyConfig c) {
        return Math.hypot(c.width() * 0.45, c.length() * 0.45);
    }

    private static double effMass(RigidBodyState body, RigidBodyConfig cfg, Vec3d r, Vec3d dir) {
        Vec3d term = cfg.inverseInertia().multiplyWorld(body.orientation(), r.cross(dir));
        return cfg.inverseMass() + term.cross(r).dot(dir);
    }

    private static void applyImpulse(RigidBodyState body, RigidBodyConfig cfg, Vec3d r, Vec3d impulse) {
        body.setLinearVelocity(body.linearVelocity().addScaled(impulse, cfg.inverseMass()));
        Vec3d ang = cfg.inverseInertia().multiplyWorld(body.orientation(), r.cross(impulse));
        body.setAngularVelocity(body.angularVelocity().add(ang));
    }
}
