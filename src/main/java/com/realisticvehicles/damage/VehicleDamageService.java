package com.realisticvehicles.damage;

import com.realisticvehicles.core.event.EventBus;
import com.realisticvehicles.core.event.VehicleCollisionEvent;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyState;

/**
 * Coordinates collision subscription, physical performance penalties, and hybrid crumple deformation.
 */
public final class VehicleDamageService {
    private final VehicleDamageState state = new VehicleDamageState();
    private final ImpactAnalyzer analyzer = new ImpactAnalyzer();

    public void attachTo(EventBus eventBus, RigidBodyState body) {
        eventBus.subscribe(VehicleCollisionEvent.class, event -> analyzer.analyzeImpact(event, body, state));
    }

    public VehicleDamageState state() {
        return state;
    }

    public double effectivePowerMultiplier() {
        if (state.engineHealth() < 0.12F) {
            return 0.0;
        }
        return 0.25 + 0.75 * state.engineHealth() * (0.5 + 0.5 * state.transmissionHealth());
    }

    public double effectiveBrakeMultiplier() {
        return 0.30 + 0.70 * state.brakesHealth();
    }

    public double effectiveWheelGripMultiplier(int wheelIdx) {
        float tire = state.tireHealth(wheelIdx);
        return tire < 0.15F ? 0.28 : 0.40 + 0.60 * tire;
    }

    public float[] deformLocalVertex(float x, float y, float z) {
        DamageZone zone = analyzer.classifyZone(new Vec3d(x, y, z));
        float d = state.zoneDeformation(zone);
        if (d <= 0.01F) {
            return new float[] { x, y, z };
        }
        float scaleInward = d * 0.28F;
        float nx = x;
        float ny = y;
        float nz = z;
        switch (zone) {
            case FRONT_LEFT, FRONT_CENTER, FRONT_RIGHT -> nz -= scaleInward;
            case REAR_LEFT, REAR_CENTER, REAR_RIGHT -> nz += scaleInward;
            case SIDE_LEFT -> nx -= scaleInward;
            case SIDE_RIGHT -> nx += scaleInward;
            case ROOF -> ny -= scaleInward;
        }
        return new float[] { nx, ny, nz };
    }
}
