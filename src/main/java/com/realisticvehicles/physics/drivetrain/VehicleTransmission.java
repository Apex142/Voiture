package com.realisticvehicles.physics.drivetrain;

/**
 * Manages active gear (-1 = Reverse, 0 = Neutral, 1..N = Forward gears) and shift timing.
 */
public final class VehicleTransmission {
    private final TransmissionConfig config;
    private int currentGear = 1;
    private double shiftCooldown;

    public VehicleTransmission(TransmissionConfig config) {
        this.config = config;
    }

    public int currentGear() {
        return currentGear;
    }

    public void setCurrentGear(int gear) {
        int maxGear = config.forwardGearRatios().size();
        this.currentGear = Math.max(-1, Math.min(gear, maxGear));
    }

    public TransmissionConfig config() {
        return config;
    }

    public double activeTotalRatio() {
        if (currentGear == 0) {
            return 0.0;
        }
        double gearRatio = currentGear < 0
            ? config.reverseGearRatio()
            : config.forwardGearRatios().get(currentGear - 1);
        return gearRatio * config.finalDriveRatio();
    }

    public void updateAutomatic(double engineRpm, double forwardSpeed, double throttle, double brake, double dt) {
        if (shiftCooldown > 0.0) {
            shiftCooldown = Math.max(0.0, shiftCooldown - dt);
        }

        if (brake > 0.1 && throttle < 0.05 && forwardSpeed <= 0.4) {
            currentGear = -1;
            return;
        }
        if (throttle > 0.05 && forwardSpeed >= -0.4 && currentGear <= 0) {
            currentGear = 1;
            shiftCooldown = config.shiftCooldownSeconds();
            return;
        }
        if (currentGear <= 0 || shiftCooldown > 0.0) {
            return;
        }

        int maxGear = config.forwardGearRatios().size();
        if (engineRpm >= config.upshiftRpm() && currentGear < maxGear && throttle > 0.15) {
            currentGear++;
            shiftCooldown = config.shiftCooldownSeconds();
        } else if (engineRpm <= config.downshiftRpm() && currentGear > 1) {
            currentGear--;
            shiftCooldown = config.shiftCooldownSeconds();
        }
    }
}
