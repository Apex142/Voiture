package com.realisticvehicles.physics.collision;

import com.realisticvehicles.core.event.EventBus;
import com.realisticvehicles.core.event.VehicleCollisionEvent;
import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyConfig;
import com.realisticvehicles.physics.body.RigidBodyState;
import com.realisticvehicles.physics.world.CollisionBox;
import com.realisticvehicles.physics.world.CollisionWorld;
import java.util.List;

/**
 * Resolves rigid-body chassis contacts against world geometry using full max-depth
 * projection, pitch/roll torque alignment, and impulse response so meshes never clip blocks.
 */
public final class CollisionResolver {
    private static final double RESTITUTION = 0.10;
    private static final double CHASSIS_FRICTION = 0.45;
    private static final double SEAM_PROBE_OFFSET = 0.04;
    private static final double STEP_TOP_TOLERANCE = 0.95;
    private static final double EVENT_SPEED_THRESHOLD = 1.0;

    private record Contact(Vec3d point, Vec3d normal, double depth) {
    }

    public boolean resolve(
        RigidBodyState body,
        RigidBodyConfig config,
        ChassisCollider collider,
        CollisionWorld world,
        EventBus eventBus
    ) {
        List<Vec3d> points = collider.worldSamplePoints(body);
        List<CollisionBox> boxes = world.queryBoxes(collider.broadphaseBounds(points));
        if (boxes.isEmpty()) {
            return false;
        }

        boolean hadCollision = false;
        double maxPosX = 0, maxNegX = 0, maxPosY = 0, maxNegY = 0, maxPosZ = 0, maxNegZ = 0;
        Vec3d angularNudge = Vec3d.ZERO;

        for (Vec3d point : points) {
            Contact contact = findBestContact(point, boxes);
            if (contact == null) {
                continue;
            }
            hadCollision = true;
            Vec3d n = contact.normal();
            double d = contact.depth();
            if (n.x() > 0.5) maxPosX = Math.max(maxPosX, d);
            else if (n.x() < -0.5) maxNegX = Math.max(maxNegX, d);
            if (n.y() > 0.5) maxPosY = Math.max(maxPosY, d);
            else if (n.y() < -0.5) maxNegY = Math.max(maxNegY, d);
            if (n.z() > 0.5) maxPosZ = Math.max(maxPosZ, d);
            else if (n.z() < -0.5) maxNegZ = Math.max(maxNegZ, d);

            Vec3d r = point.sub(body.position());
            if (n.y() > 0.5) {
                angularNudge = angularNudge.add(r.cross(n.scale(d * 0.25)));
                if (body.linearVelocity().y() < 0.0) {
                    body.setLinearVelocity(new Vec3d(body.linearVelocity().x(), 0.0, body.linearVelocity().z()));
                }
            } else {
                // Wall contact: project horizontally only so wall collisions never pitch the car up into a vertical wheelie
                Vec3d flatR = new Vec3d(r.x(), 0.0, r.z());
                Vec3d flatN = new Vec3d(n.x(), 0.0, n.z());
                angularNudge = angularNudge.add(flatR.cross(flatN.scale(d * 0.20)));
                applyImpulse(body, config, contact, eventBus);
            }
        }

        if (hadCollision) {
            Vec3d push = new Vec3d(maxPosX - maxNegX, maxPosY - maxNegY, maxPosZ - maxNegZ);
            body.setPosition(body.position().add(push));
            if (angularNudge.lengthSquared() > 1e-6) {
                Quat4d rotFix = body.orientation().integrate(angularNudge.clampLength(0.28), 0.30);
                body.setOrientation(rotFix);
            }
        }
        return hadCollision;
    }

    private Contact findBestContact(Vec3d point, List<CollisionBox> boxes) {
        Contact best = null;
        for (CollisionBox box : boxes) {
            if (!box.contains(point)) {
                continue;
            }
            Contact candidate = extractSurfaceContact(point, box, boxes);
            if (candidate != null && (best == null || candidate.depth() > best.depth())) {
                best = candidate;
            }
        }
        return best;
    }

    private Contact extractSurfaceContact(Vec3d p, CollisionBox box, List<CollisionBox> allBoxes) {
        double[] depths = {
            p.x() - box.minX(), box.maxX() - p.x(),
            p.y() - box.minY(), box.maxY() - p.y(),
            p.z() - box.minZ(), box.maxZ() - p.z()
        };
        Vec3d[] normals = {
            new Vec3d(-1, 0, 0), new Vec3d(1, 0, 0),
            new Vec3d(0, -1, 0), new Vec3d(0, 1, 0),
            new Vec3d(0, 0, -1), new Vec3d(0, 0, 1)
        };

        double topDist = box.maxY() - p.y();
        Vec3d topProbe = p.addScaled(normals[3], topDist + SEAM_PROBE_OFFSET);
        if (topDist > 0.0 && topDist <= STEP_TOP_TOLERANCE && !isInsideAny(topProbe, allBoxes)) {
            return new Contact(p, normals[3], topDist);
        }

        int bestIdx = -1;
        double bestDepth = Double.POSITIVE_INFINITY;
        for (int i = 0; i < 6; i++) {
            Vec3d outsideProbe = p.addScaled(normals[i], depths[i] + SEAM_PROBE_OFFSET);
            if (!isInsideAny(outsideProbe, allBoxes) && depths[i] < bestDepth) {
                bestDepth = depths[i];
                bestIdx = i;
            }
        }
        if (bestIdx < 0) {
            bestIdx = indexOfMin(depths);
            bestDepth = depths[bestIdx];
        }
        return new Contact(p, normals[bestIdx], bestDepth);
    }

    private boolean isInsideAny(Vec3d probe, List<CollisionBox> boxes) {
        for (CollisionBox box : boxes) {
            if (box.contains(probe)) {
                return true;
            }
        }
        return false;
    }

    private int indexOfMin(double[] values) {
        int idx = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] < values[idx]) {
                idx = i;
            }
        }
        return idx;
    }

    private void applyImpulse(
        RigidBodyState body,
        RigidBodyConfig config,
        Contact contact,
        EventBus eventBus
    ) {
        Vec3d rawR = contact.point().sub(body.position());
        // Keep wall collision lever arm at Center-of-Mass height so hitting a wall induces yaw spin, never vertical pitching/flipping
        Vec3d r = Math.abs(contact.normal().y()) < 0.5 ? new Vec3d(rawR.x(), 0.0, rawR.z()) : rawR;
        Vec3d velAtPoint = body.velocityAtPoint(contact.point());
        double vn = velAtPoint.dot(contact.normal());
        if (vn >= 0.0) {
            return;
        }

        double denom = effectiveMassDenominator(body, config, r, contact.normal());
        double jn = -(1.0 + RESTITUTION) * vn / Math.max(denom, 1e-6);
        applyDeltaVelocity(body, config, r, contact.normal().scale(jn));

        Vec3d tangentVel = velAtPoint.sub(contact.normal().scale(vn));
        double tangentSpeed = tangentVel.length();
        if (tangentSpeed > 1e-4) {
            Vec3d tangentDir = tangentVel.scale(1.0 / tangentSpeed);
            double tDenom = effectiveMassDenominator(body, config, r, tangentDir);
            double jt = Math.min(tangentSpeed / Math.max(tDenom, 1e-6), jn * CHASSIS_FRICTION);
            applyDeltaVelocity(body, config, r, tangentDir.scale(-jt));
        }

        if (-vn >= EVENT_SPEED_THRESHOLD && eventBus != null) {
            eventBus.publish(new VehicleCollisionEvent(contact.point(), contact.normal(), jn, -vn));
        }
    }

    private double effectiveMassDenominator(RigidBodyState body, RigidBodyConfig config, Vec3d r, Vec3d dir) {
        Vec3d rCrossDir = r.cross(dir);
        Vec3d invInertiaTerm = config.inverseInertia().multiplyWorld(body.orientation(), rCrossDir);
        return config.inverseMass() + invInertiaTerm.cross(r).dot(dir);
    }

    private void applyDeltaVelocity(RigidBodyState body, RigidBodyConfig config, Vec3d r, Vec3d impulse) {
        body.setLinearVelocity(body.linearVelocity().addScaled(impulse, config.inverseMass()));
        Vec3d angImpulse = config.inverseInertia().multiplyWorld(body.orientation(), r.cross(impulse));
        body.setAngularVelocity(body.angularVelocity().add(angImpulse));
    }
}
