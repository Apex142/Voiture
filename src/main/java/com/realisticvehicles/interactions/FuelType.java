package com.realisticvehicles.interactions;

/**
 * Supported vehicle energy types and consumption characteristics.
 */
public enum FuelType {
    GASOLINE(1.00, 1.85),
    DIESEL(0.82, 1.75),
    ELECTRIC(0.45, 0.35);

    private final double consumptionFactor;
    private final double basePricePerUnit;

    FuelType(double consumptionFactor, double basePricePerUnit) {
        this.consumptionFactor = consumptionFactor;
        this.basePricePerUnit = basePricePerUnit;
    }

    public double consumptionFactor() {
        return consumptionFactor;
    }

    public double basePricePerUnit() {
        return basePricePerUnit;
    }
}
