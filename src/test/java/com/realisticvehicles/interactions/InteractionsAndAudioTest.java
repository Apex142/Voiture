package com.realisticvehicles.interactions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.realisticvehicles.audio.DynamicEngineAudioController;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class InteractionsAndAudioTest {

    @Test
    void fuelConsumesUnderLoadAndStopsRefuelingAtCapacity() {
        VehicleFuelSystem fuel = new VehicleFuelSystem(FuelType.GASOLINE, 60.0, 10.0);
        for (int i = 0; i < 200; i++) {
            fuel.consume(4500.0, 1.0, 0.05);
        }
        assertTrue(fuel.currentLiters() < 10.0, "High RPM throttle must consume fuel");

        assertEquals(0.0, fuel.refuel(10.0), 1e-6, "Cannot refuel while cap is closed");
        fuel.setCapOpen(true);
        double cost = fuel.refuel(100.0);
        assertTrue(cost > 0.0);
        assertEquals(60.0, fuel.currentLiters(), 1e-6, "Refueling stops at tank capacity");
    }

    @Test
    void keyManagerEnforcesOwnershipAndTriggersAlarm() {
        UUID carId = UUID.randomUUID();
        UUID wrongKey = UUID.randomUUID();
        VehicleKeyManager keys = new VehicleKeyManager(carId);

        assertFalse(keys.toggleLock(wrongKey));
        assertTrue(keys.isAlarmActive(), "Wrong key must trigger alarm");

        assertTrue(keys.toggleLock(carId));
        assertTrue(keys.isLocked());
        assertFalse(keys.tryStartEngine(carId, 12.6, true), "Cannot start while locked");

        assertTrue(keys.toggleLock(carId));
        assertTrue(keys.tryStartEngine(carId, 12.6, true));
        assertTrue(keys.isEngineStarted());
    }

    @Test
    void maintenanceAndAudioRespondToRpmAndRadiatorDamage() {
        VehicleMaintenanceState maint = new VehicleMaintenanceState();
        for (int i = 0; i < 200; i++) {
            maint.update(true, 6000.0, 5.0, 1.0, 0.0, 0.2F, 0.1);
        }
        assertTrue(maint.coolantTempC() > 95.0F, "Damaged radiator under high load must overheat");

        DynamicEngineAudioController audio = new DynamicEngineAudioController();
        var low = audio.compute(true, 900.0, 850.0, 7200.0, 0.0, 0.0, 0.1);
        var high = audio.compute(true, 6800.0, 850.0, 7200.0, 1.0, 0.45, 0.5);
        assertTrue(high.enginePitch() > low.enginePitch() + 0.5F);
        assertTrue(high.tireSkidGain() > 0.2F);
    }
}
