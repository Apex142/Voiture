package com.realisticvehicles.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.realisticvehicles.core.event.SimpleEventBus;
import com.realisticvehicles.core.event.VehicleCollisionEvent;
import com.realisticvehicles.core.math.Mat3d;
import com.realisticvehicles.core.math.MathUtil;
import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class MathAndEventTest {

    @Test
    void vec3dOperationsFollowRightHandRule() {
        Vec3d cross = Vec3d.UNIT_X.cross(Vec3d.UNIT_Y);
        assertEquals(0.0, cross.x(), 1e-9);
        assertEquals(0.0, cross.y(), 1e-9);
        assertEquals(1.0, cross.z(), 1e-9);
        assertEquals(5.0, new Vec3d(3.0, 4.0, 0.0).length(), 1e-9);
    }

    @Test
    void quaternionPositiveYawTurnsLeftTowardPositiveX() {
        Quat4d q = Quat4d.fromYaw(Math.PI / 2.0);
        Vec3d fwd = q.forward();
        assertEquals(1.0, fwd.x(), 1e-6);
        assertEquals(0.0, fwd.y(), 1e-6);
        assertEquals(0.0, fwd.z(), 1e-6);
        assertEquals(Math.PI / 2.0, q.yawRadians(), 1e-6);
    }

    @Test
    void quaternionIntegrationAndInverseRotationRemainUnitLength() {
        Quat4d q = Quat4d.IDENTITY;
        Vec3d omega = new Vec3d(0.2, 1.5, -0.3);
        for (int i = 0; i < 120; i++) {
            q = q.integrate(omega, 1.0 / 120.0);
        }
        double norm = Math.sqrt(q.x() * q.x() + q.y() * q.y() + q.z() * q.z() + q.w() * q.w());
        assertEquals(1.0, norm, 1e-6);

        Vec3d v = new Vec3d(1.5, -2.0, 3.2);
        Vec3d roundTrip = q.inverseRotate(q.rotate(v));
        assertEquals(v.x(), roundTrip.x(), 1e-6);
        assertEquals(v.y(), roundTrip.y(), 1e-6);
        assertEquals(v.z(), roundTrip.z(), 1e-6);
    }

    @Test
    void mat3dSolidBoxInertiaIsPositiveAndInvertible() {
        Mat3d inertia = Mat3d.solidBoxInertia(1500.0, 1.8, 0.8, 4.4);
        Mat3d inv = inertia.inverse();
        assertTrue(inertia.diagY() > inertia.diagZ());
        assertEquals(1.0, inertia.diagX() * inv.diagX(), 1e-9);
    }

    @Test
    void mathUtilSanitizesNonFiniteNumbers() {
        assertEquals(0.0, MathUtil.sanitize(Double.NaN));
        assertEquals(0.0, MathUtil.sanitize(Double.POSITIVE_INFINITY));
        assertEquals(42.0, MathUtil.safeDivide(1.0, 0.0, 42.0));
    }

    @Test
    void eventBusPublishesCollisionEventsToSubscribers() {
        SimpleEventBus bus = new SimpleEventBus();
        AtomicInteger received = new AtomicInteger();
        bus.subscribe(VehicleCollisionEvent.class, e -> received.incrementAndGet());
        bus.publish(new VehicleCollisionEvent(Vec3d.ZERO, Vec3d.UNIT_Z, 5000.0, 12.0));
        assertEquals(1, received.get());
    }
}
