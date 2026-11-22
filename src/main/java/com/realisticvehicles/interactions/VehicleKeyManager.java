package com.realisticvehicles.interactions;

import java.util.UUID;

/**
 * Manages vehicle key authorization, locking, duplicate keys, and break-in alarms.
 */
public final class VehicleKeyManager {
    private final UUID vehicleId;
    private boolean locked;
    private boolean engineStarted;
    private boolean alarmActive;

    public VehicleKeyManager(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public UUID vehicleId() {
        return vehicleId;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isEngineStarted() {
        return engineStarted;
    }

    public boolean isAlarmActive() {
        return alarmActive;
    }

    public boolean matchesKey(UUID keyVehicleId) {
        return keyVehicleId != null && vehicleId.equals(keyVehicleId);
    }

    public boolean toggleLock(UUID keyVehicleId) {
        if (!matchesKey(keyVehicleId)) {
            alarmActive = true;
            return false;
        }
        locked = !locked;
        if (!locked) {
            alarmActive = false;
        }
        return true;
    }

    public boolean tryStartEngine(UUID keyVehicleId, double batteryVolts, boolean hasFuel) {
        if (!matchesKey(keyVehicleId)) {
            alarmActive = true;
            return false;
        }
        if (locked || batteryVolts < 10.5 || !hasFuel) {
            return false;
        }
        engineStarted = true;
        alarmActive = false;
        return true;
    }

    public void stopEngine() {
        engineStarted = false;
    }
}
