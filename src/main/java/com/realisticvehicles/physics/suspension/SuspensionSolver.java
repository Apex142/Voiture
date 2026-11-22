package com.realisticvehicles.physics.suspension;

import com.realisticvehicles.core.math.MathUtil;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyState;
import com.realisticvehicles.physics.world.CollisionWorld;
import com.realisticvehicles.physics.world.RayHit;
import com.realisticvehicles.physics.world.SurfaceProperties;
import java.util.Optional;

/**
 * Solves spring-damper suspension forces using multi-ray terrain contact per wheel.
 */
public final class SuspensionSolver {
    private static final double BUMP_STOP_MULTIPLIER = 3.5;
    private static final double FEELER_ANGLE_RAD = Math.toRadians(22.0);
    private static final double MAX_SUSPENSION_FORCE_FACTOR = 4.0;

    public void solveWheel(
        RigidBodyState body,
        WheelState wheel,
        CollisionWorld world,
        double vehicleWeight,
        double dt
    ) {
        WheelConfig cfg = wheel.config();
        Vec3d upWorld = body.orientation().up();
        if (upWorld.y() < 0.25) {
            wheel.updateContact(false, 0.0, 0.0, body.localToWorldPoint(cfg.mountPoint()), upWorld, SurfaceProperties.ASPHALT);
            return;
        }
        Vec3d downWorld = upWorld.negate();
        Vec3d mountWorld = body.localToWorldPoint(cfg.mountPoint());
        double maxRayLength = cfg.restLength() + cfg.radius();

        Optional<RayHit> bestHit = castWheelRays(body, mountWorld, downWorld, maxRayLength, world);
        if (bestHit.isEmpty()) {
            wheel.updateContact(false, 0.0, 0.0, mountWorld.addScaled(downWorld, maxRayLength), upWorld, SurfaceProperties.ASPHALT);
            return;
        }

        RayHit hit = bestHit.get();
        double projectedDist = hit.point().sub(mountWorld).dot(downWorld);
        double compression = maxRayLength - projectedDist;
        if (compression <= 0.0) {
            wheel.updateContact(false, 0.0, 0.0, hit.point(), hit.normal(), hit.surface());
            return;
        }

        double clampedComp = MathUtil.clamp(compression, 0.0, cfg.restLength());
        double compressionVel = (clampedComp - wheel.compression()) / Math.max(dt, 1e-4);
        double springForce = cfg.springStiffness() * clampedComp;
        if (compression > cfg.restLength()) {
            double overshoot = compression - cfg.restLength();
            springForce += cfg.springStiffness() * BUMP_STOP_MULTIPLIER * overshoot;
        }

        double dampingCoeff = compressionVel >= 0.0 ? cfg.bumpDamping() : cfg.reboundDamping();
        double damperForce = dampingCoeff * compressionVel;
        double maxForce = vehicleWeight * MAX_SUSPENSION_FORCE_FACTOR;
        double totalForce = MathUtil.clamp(springForce + damperForce, 0.0, maxForce);

        double alignment = MathUtil.clamp(hit.normal().dot(upWorld), 0.15, 1.0);
        Vec3d suspensionImpulseForce = hit.normal().scale(totalForce * alignment);
        body.addForceAtPoint(suspensionImpulseForce, mountWorld);

        wheel.updateContact(true, clampedComp, totalForce * alignment, hit.point(), hit.normal(), hit.surface());
    }

    private Optional<RayHit> castWheelRays(
        RigidBodyState body,
        Vec3d mountWorld,
        Vec3d downWorld,
        double maxRayLength,
        CollisionWorld world
    ) {
        Optional<RayHit> centerHit = world.raycast(mountWorld, downWorld, maxRayLength);
        Vec3d forward = body.orientation().forward();

        RayHit best = centerHit.orElse(null);
        double bestProjected = best != null ? best.point().sub(mountWorld).dot(downWorld) : Double.POSITIVE_INFINITY;

        double[] anglesDeg = { 22.0, -22.0, 48.0, -48.0 };
        for (double deg : anglesDeg) {
            double rad = Math.toRadians(deg);
            double cosA = Math.cos(rad);
            double sinA = Math.sin(rad);
            double feelerLen = (maxRayLength + 0.45) / Math.max(0.35, cosA);
            Vec3d dir = downWorld.scale(cosA).addScaled(forward, sinA).normalize();
            Optional<RayHit> hit = world.raycast(mountWorld, dir, feelerLen);
            if (hit.isPresent()) {
                double proj = hit.get().point().sub(mountWorld).dot(downWorld);
                if (proj < bestProjected && hit.get().normal().y() > 0.25) {
                    best = hit.get();
                    bestProjected = proj;
                }
            }
        }
        return Optional.ofNullable(best);
    }
}
