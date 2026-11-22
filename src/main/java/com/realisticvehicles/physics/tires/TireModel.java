package com.realisticvehicles.physics.tires;

/**
 * Strategy interface for physical tire force calculation (Open/Closed Principle).
 */
public interface TireModel {
    TireForces calculate(TireContext context);
}
