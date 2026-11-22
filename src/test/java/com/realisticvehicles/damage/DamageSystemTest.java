package com.realisticvehicles.damage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.realisticvehicles.core.event.SimpleEventBus;
import com.realisticvehicles.core.event.VehicleCollisionEvent;
import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyState;
import org.junit.jupiter.api.Test;

final class DamageSystemTest {

    @Test
    void frontalCrashDamagesRadiatorEngineAndDeformsFrontVertices() {
        SimpleEventBus bus = new SimpleEventBus();
        RigidBodyState body = new RigidBodyState(Vec3d.ZERO, Quat4d.IDENTITY);
        VehicleDamageService service = new VehicleDamageService();
        service.attachTo(bus, body);

        bus.publish(new VehicleCollisionEvent(new Vec3d(0.0, 0.2, 2.0), new Vec3d(0.0, 0.0, -1.0), 18000.0, 18.0));

        assertTrue(service.state().radiatorHealth() < 0.7F);
        assertTrue(service.state().engineHealth() < 0.8F);
        assertTrue(service.effectivePowerMultiplier() < 0.9);
        assertTrue(service.state().zoneDeformation(DamageZone.FRONT_CENTER) > 0.3F);

        float[] deformed = service.deformLocalVertex(0.0F, 0.2F, 2.0F);
        assertTrue(deformed[2] < 2.0F, "Front vertex must crumple inward (-Z)");
    }

    @Test
    void severeSideImpactJamsDoorAndRepairRestoresHealth() {
        SimpleEventBus bus = new SimpleEventBus();
        RigidBodyState body = new RigidBodyState(Vec3d.ZERO, Quat4d.IDENTITY);
        VehicleDamageService service = new VehicleDamageService();
        service.attachTo(bus, body);

        bus.publish(new VehicleCollisionEvent(new Vec3d(0.9, 0.2, 0.0), new Vec3d(-1.0, 0.0, 0.0), 22000.0, 20.0));
        assertTrue(service.state().leftDoorJammed(), "Severe left side impact must jam left door");

        service.state().repairAll();
        assertEquals(1.0F, service.state().overallHealth(), 1e-6);
        assertEquals(1.0, service.effectivePowerMultiplier(), 1e-6);
    }
}
