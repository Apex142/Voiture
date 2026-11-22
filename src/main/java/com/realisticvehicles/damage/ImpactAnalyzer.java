package com.realisticvehicles.damage;

import com.realisticvehicles.core.event.VehicleCollisionEvent;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyState;

/**
 * Classifies collision impacts into structural zones and propagates mechanical + crumple damage.
 */
public final class ImpactAnalyzer {
    private static final double MIN_DAMAGING_SPEED_MPS = 3.5;

    public DamageZone analyzeImpact(VehicleCollisionEvent event, RigidBodyState body, VehicleDamageState state) {
        if (event.closingSpeed() < MIN_DAMAGING_SPEED_MPS) {
            return null;
        }
        Vec3d localPoint = body.worldToLocalPoint(event.contactPoint());
        DamageZone zone = classifyZone(localPoint);
        float severity = (float) Math.min(1.0, (event.closingSpeed() - MIN_DAMAGING_SPEED_MPS) / 22.0);
        state.addZoneDeformation(zone, severity * 0.75F);

        switch (zone) {
            case FRONT_CENTER -> {
                state.damageRadiator(severity * 0.65F);
                state.damageEngine(severity * 0.45F);
                state.damageHeadlights(severity * 0.80F);
                state.damageWindshield(severity * 0.25F);
            }
            case FRONT_LEFT -> {
                state.damageHeadlights(severity * 0.70F);
                state.damageSteering(severity * 0.40F);
                state.damageWheel(0, severity * 0.50F, severity * 0.40F);
            }
            case FRONT_RIGHT -> {
                state.damageHeadlights(severity * 0.70F);
                state.damageSteering(severity * 0.40F);
                state.damageWheel(1, severity * 0.50F, severity * 0.40F);
            }
            case SIDE_LEFT -> state.damageWheel(2, severity * 0.25F, severity * 0.20F);
            case SIDE_RIGHT -> state.damageWheel(3, severity * 0.25F, severity * 0.20F);
            case REAR_LEFT -> {
                state.damageFuelTank(severity * 0.35F);
                state.damageWheel(2, severity * 0.45F, severity * 0.35F);
            }
            case REAR_RIGHT -> {
                state.damageFuelTank(severity * 0.35F);
                state.damageWheel(3, severity * 0.45F, severity * 0.35F);
            }
            case REAR_CENTER -> {
                state.damageTransmission(severity * 0.35F);
                state.damageFuelTank(severity * 0.50F);
            }
            case ROOF -> state.damageWindshield(severity * 0.85F);
        }
        if (severity > 0.45F) {
            state.damageBrakes(severity * 0.25F);
        }
        return zone;
    }

    public DamageZone classifyZone(Vec3d localPoint) {
        if (localPoint.y() > 0.52) {
            return DamageZone.ROOF;
        }
        if (localPoint.z() > 1.15) {
            if (localPoint.x() > 0.45) return DamageZone.FRONT_LEFT;
            if (localPoint.x() < -0.45) return DamageZone.FRONT_RIGHT;
            return DamageZone.FRONT_CENTER;
        }
        if (localPoint.z() < -1.15) {
            if (localPoint.x() > 0.45) return DamageZone.REAR_LEFT;
            if (localPoint.x() < -0.45) return DamageZone.REAR_RIGHT;
            return DamageZone.REAR_CENTER;
        }
        return localPoint.x() >= 0.0 ? DamageZone.SIDE_LEFT : DamageZone.SIDE_RIGHT;
    }
}
