package com.realisticvehicles.interactions;

import com.realisticvehicles.core.math.MathUtil;

/**
 * Simulates fuel tank capacity, load-dependent consumption, and pump refueling.
 */
public final class VehicleFuelSystem {
    private final FuelType fuelType;
    private final double capacityLiters;
    private double currentLiters;
    private boolean capOpen;
    private boolean misfueled;

    public VehicleFuelSystem(FuelType fuelType, double capacityLiters, double initialLiters) {
        this.fuelType = fuelType;
        this.capacityLiters = Math.max(5.0, capacityLiters);
        this.currentLiters = MathUtil.clamp(initialLiters, 0.0, this.capacityLiters);
    }

    public FuelType fuelType() { return fuelType; }
    public double capacityLiters() { return capacityLiters; }
    public double currentLiters() { return currentLiters; }
    public float fuelFraction() { return (float) (currentLiters / capacityLiters); }
    public boolean isEmpty() { return currentLiters <= 1e-4; }
    public boolean isCapOpen() { return capOpen; }
    public void setCapOpen(boolean open) { this.capOpen = open; }
    public boolean isMisfueled() { return misfueled; }
    public boolean canEngineRun() { return !isEmpty() && !misfueled; }

    public void restoreState(double liters, boolean isMisfueled) {
        this.currentLiters = MathUtil.clamp(liters, 0.0, this.capacityLiters);
        this.misfueled = isMisfueled;
    }

    public void consume(double engineRpm, double throttle, double dt) {
        if (isEmpty() || dt <= 0.0) {
            return;
        }
        double rpmFactor = Math.max(0.15, engineRpm / 3000.0);
        double loadFactor = 0.25 + 0.75 * MathUtil.clamp(throttle, 0.0, 1.0);
        double litersPerSec = 0.0022 * rpmFactor * loadFactor * fuelType.consumptionFactor();
        currentLiters = Math.max(0.0, currentLiters - litersPerSec * dt);
    }

    /** Refuels up to tank capacity and returns the total price for the dispensed amount. */
    public double refuel(double requestedLiters) {
        if (!capOpen || requestedLiters <= 0.0 || currentLiters >= capacityLiters) {
            return 0.0;
        }
        double added = Math.min(requestedLiters, capacityLiters - currentLiters);
        currentLiters += added;
        return added * fuelType.basePricePerUnit();
    }

    /** Refuels from a gas station pump nozzle (detecting misfueling if Diesel is pumped into a Gasoline tank or vice versa). */
    public double refuel(FuelType dispensedType, double litersPerSec, double dt) {
        if (litersPerSec <= 0.0 || dt <= 0.0 || currentLiters >= capacityLiters) {
            return 0.0;
        }
        if (dispensedType != null && dispensedType != this.fuelType) {
            this.misfueled = true;
        }
        double added = Math.min(litersPerSec * dt, capacityLiters - currentLiters);
        currentLiters += added;
        return added;
    }
}
