package com.realisticvehicles.damage;

import com.realisticvehicles.core.math.MathUtil;

/**
 * Tracks mechanical subsystem health [0.0..1.0] and per-zone structural deformation.
 */
public final class VehicleDamageState {
    private float engineHealth = 1.0F;
    private float transmissionHealth = 1.0F;
    private float radiatorHealth = 1.0F;
    private float steeringHealth = 1.0F;
    private float brakesHealth = 1.0F;
    private float fuelTankHealth = 1.0F;
    private float headlightsHealth = 1.0F;
    private float windshieldHealth = 1.0F;
    private final float[] suspensionHealth = { 1.0F, 1.0F, 1.0F, 1.0F };
    private final float[] tireHealth = { 1.0F, 1.0F, 1.0F, 1.0F };
    private final float[] zoneDeformation = new float[DamageZone.values().length];
    private boolean leftDoorJammed;
    private boolean rightDoorJammed;

    public float engineHealth() {
        return engineHealth;
    }

    public float transmissionHealth() {
        return transmissionHealth;
    }

    public float radiatorHealth() {
        return radiatorHealth;
    }

    public float steeringHealth() {
        return steeringHealth;
    }

    public float brakesHealth() {
        return brakesHealth;
    }

    public float fuelTankHealth() {
        return fuelTankHealth;
    }

    public float headlightsHealth() {
        return headlightsHealth;
    }

    public float windshieldHealth() {
        return windshieldHealth;
    }

    public float suspensionHealth(int wheelIdx) {
        return suspensionHealth[wheelIdx];
    }

    public float tireHealth(int wheelIdx) {
        return tireHealth[wheelIdx];
    }

    public float zoneDeformation(DamageZone zone) {
        return zoneDeformation[zone.ordinal()];
    }

    public boolean leftDoorJammed() {
        return leftDoorJammed;
    }

    public boolean rightDoorJammed() {
        return rightDoorJammed;
    }

    public void damageEngine(float amount) {
        engineHealth = clamp01(engineHealth - amount);
    }

    public void damageTransmission(float amount) {
        transmissionHealth = clamp01(transmissionHealth - amount);
    }

    public void damageRadiator(float amount) {
        radiatorHealth = clamp01(radiatorHealth - amount);
    }

    public void damageSteering(float amount) {
        steeringHealth = clamp01(steeringHealth - amount);
    }

    public void damageBrakes(float amount) {
        brakesHealth = clamp01(brakesHealth - amount);
    }

    public void damageFuelTank(float amount) {
        fuelTankHealth = clamp01(fuelTankHealth - amount);
    }

    public void damageHeadlights(float amount) {
        headlightsHealth = clamp01(headlightsHealth - amount);
    }

    public void damageWindshield(float amount) {
        windshieldHealth = clamp01(windshieldHealth - amount);
    }

    public void damageWheel(int idx, float suspAmount, float tireAmount) {
        suspensionHealth[idx] = clamp01(suspensionHealth[idx] - suspAmount);
        tireHealth[idx] = clamp01(tireHealth[idx] - tireAmount);
    }

    public void addZoneDeformation(DamageZone zone, float amount) {
        int i = zone.ordinal();
        zoneDeformation[i] = clamp01(zoneDeformation[i] + amount);
        if (zone == DamageZone.SIDE_LEFT && zoneDeformation[i] > 0.35F) {
            leftDoorJammed = true;
        }
        if (zone == DamageZone.SIDE_RIGHT && zoneDeformation[i] > 0.35F) {
            rightDoorJammed = true;
        }
    }

    public void repairAll() {
        engineHealth = 1.0F;
        transmissionHealth = 1.0F;
        radiatorHealth = 1.0F;
        steeringHealth = 1.0F;
        brakesHealth = 1.0F;
        fuelTankHealth = 1.0F;
        headlightsHealth = 1.0F;
        windshieldHealth = 1.0F;
        for (int i = 0; i < 4; i++) {
            suspensionHealth[i] = 1.0F;
            tireHealth[i] = 1.0F;
        }
        for (int i = 0; i < zoneDeformation.length; i++) {
            zoneDeformation[i] = 0.0F;
        }
        leftDoorJammed = false;
        rightDoorJammed = false;
    }

    public float overallHealth() {
        return (engineHealth + transmissionHealth + radiatorHealth + steeringHealth + brakesHealth) / 5.0F;
    }

    private static float clamp01(float v) {
        return (float) MathUtil.clamp(v, 0.0, 1.0);
    }
}
