package com.realisticvehicles.interactions;

import com.realisticvehicles.core.math.MathUtil;

/**
 * Tracks engine oil, coolant temperature, battery charge, and consumable wear.
 */
public final class VehicleMaintenanceState {
    private float oilLevel = 1.0F;
    private float coolantTempC = 24.0F;
    private float batteryVolts = 12.6F;
    private float tireWear = 0.0F;
    private float brakePadWear = 0.0F;

    public float oilLevel() {
        return oilLevel;
    }

    public float coolantTempC() {
        return coolantTempC;
    }

    public float batteryVolts() {
        return batteryVolts;
    }

    public float tireWear() {
        return tireWear;
    }

    public float brakePadWear() {
        return brakePadWear;
    }

    public void update(
        boolean engineRunning,
        double rpm,
        double speedMps,
        double throttle,
        double brake,
        float radiatorHealth,
        double dt
    ) {
        if (dt <= 0.0) {
            return;
        }
        float ambient = 22.0F;
        if (engineRunning) {
            float heatGen = (float) ((0.6 + throttle * 1.4) * (rpm / 3500.0) * (1.8 - radiatorHealth));
            float airflowCooling = (float) ((0.5 + Math.abs(speedMps) * 0.06) * Math.max(0.25, radiatorHealth));
            float targetTemp = Math.min(132.0F, 88.0F + heatGen * 22.0F - airflowCooling * 6.0F);
            coolantTempC = (float) MathUtil.lerp(coolantTempC, targetTemp, MathUtil.clamp(dt * 0.12, 0.0, 1.0));
            batteryVolts = (float) MathUtil.lerp(batteryVolts, 14.1, MathUtil.clamp(dt * 0.5, 0.0, 1.0));
            oilLevel = (float) Math.max(0.0, oilLevel - 0.000015 * dt);
        } else {
            coolantTempC = (float) MathUtil.lerp(coolantTempC, ambient, MathUtil.clamp(dt * 0.05, 0.0, 1.0));
        }

        tireWear = (float) Math.min(1.0, tireWear + Math.abs(speedMps) * 0.000008 * dt);
        brakePadWear = (float) Math.min(1.0, brakePadWear + brake * Math.abs(speedMps) * 0.00002 * dt);
    }

    public void serviceMaintenance() {
        oilLevel = 1.0F;
        batteryVolts = 12.6F;
        tireWear = 0.0F;
        brakePadWear = 0.0F;
        coolantTempC = Math.min(coolantTempC, 85.0F);
    }
}
