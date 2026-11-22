package com.realisticvehicles.physics.simulation;

import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyConfig;
import com.realisticvehicles.physics.drivetrain.EngineConfig;
import com.realisticvehicles.physics.drivetrain.LimitedSlipDifferential;
import com.realisticvehicles.physics.drivetrain.OpenDifferential;
import com.realisticvehicles.physics.drivetrain.TransmissionConfig;
import com.realisticvehicles.physics.steering.SteeringConfig;
import com.realisticvehicles.physics.suspension.WheelConfig;
import java.util.List;
import java.util.Locale;

/**
 * Factory pattern producing physically calibrated vehicle presets in SI units.
 */
public final class VehiclePresetFactory {
    public static final String PRESET_SEDAN = "sedan";
    public static final String PRESET_SPORT = "sport";
    public static final String PRESET_SLK55 = "slk55";
    public static final String PRESET_OFFROAD = "offroad";

    private VehiclePresetFactory() {
    }

    public static VehicleConfig create(String presetId) {
        if (presetId == null) {
            return createSedan();
        }
        return switch (presetId.toLowerCase(Locale.ROOT)) {
            case PRESET_SPORT -> createSport();
            case PRESET_SLK55 -> createSlk55Amg();
            case PRESET_OFFROAD -> createOffroad();
            default -> createSedan();
        };
    }

    public static VehicleConfig createSedan() {
        RigidBodyConfig body = RigidBodyConfig.builder()
            .mass(1450.0).dimensions(1.86, 0.72, 4.40).damping(0.03, 0.35).aerodynamics(0.31, 2.25).build();
        List<WheelConfig> wheels = buildFourWheels(
            0.84, -0.05, 1.38, -1.38, 0.42, 0.34, 0.22,
            25500.0, 3100.0, 4300.0, 1.15, true, false
        );
        return VehicleConfig.builder(PRESET_SEDAN)
            .body(body).wheels(wheels).engine(EngineConfig.standardSedan())
            .transmission(TransmissionConfig.sixSpeedAutomatic()).steering(SteeringConfig.standardSedan())
            .differential(new OpenDifferential()).antiRollBars(7500.0, 6000.0)
            .electronics(true, true).brakes(5400.0, 3400.0).build();
    }

    public static VehicleConfig createSport() {
        RigidBodyConfig body = RigidBodyConfig.builder()
            .mass(1320.0).dimensions(1.92, 0.66, 4.32).damping(0.025, 0.32).aerodynamics(0.28, 2.05).build();
        List<WheelConfig> wheels = buildFourWheels(
            0.88, -0.06, 1.34, -1.34, 0.38, 0.34, 0.25,
            32000.0, 3600.0, 4900.0, 1.28, false, true
        );
        SteeringConfig steering = new SteeringConfig(
            Math.toRadians(36.0), Math.toRadians(175.0), Math.toRadians(230.0), 0.35, 38.0, 2.68, 1.76
        );
        return VehicleConfig.builder(PRESET_SPORT)
            .body(body).wheels(wheels).engine(EngineConfig.sportCoupe())
            .transmission(TransmissionConfig.sportSixSpeed()).steering(steering)
            .differential(new LimitedSlipDifferential(0.65, 90.0)).antiRollBars(11500.0, 9500.0)
            .electronics(true, true).brakes(6400.0, 3800.0).build();
    }

    /** 2012 Mercedes-Benz SLK 55 AMG (1.48x real human proportion scale + agile GTA V sports car calibration). */
    public static VehicleConfig createSlk55Amg() {
        RigidBodyConfig body = RigidBodyConfig.builder()
            .mass(1340.0).dimensions(2.72, 0.95, 5.98).damping(0.028, 0.55).aerodynamics(0.26, 2.10).build();
        List<WheelConfig> wheels = buildFourWheels(
            1.14, 0.14, 1.76, -1.76, 0.40, 0.50, 0.35,
            42000.0, 4800.0, 6200.0, 1.78, false, true
        );
        EngineConfig v8 = new EngineConfig(850.0, 4800.0, 7600.0, 880.0, 85.0);
        SteeringConfig steering = new SteeringConfig(
            Math.toRadians(38.0), Math.toRadians(155.0), Math.toRadians(210.0), 0.34, 46.0, 3.52, 2.28
        );
        return VehicleConfig.builder(PRESET_SLK55)
            .body(body).wheels(wheels).engine(v8)
            .transmission(TransmissionConfig.sportSixSpeed()).steering(steering)
            .differential(new LimitedSlipDifferential(0.82, 110.0)).antiRollBars(22000.0, 19500.0)
            .electronics(true, true).brakes(9800.0, 6200.0).build();
    }

    public static VehicleConfig createOffroad() {
        RigidBodyConfig body = RigidBodyConfig.builder()
            .mass(2050.0).dimensions(1.98, 0.85, 4.65).damping(0.035, 0.38).aerodynamics(0.42, 2.95).build();
        List<WheelConfig> wheels = buildFourWheels(
            0.92, -0.10, 1.45, -1.45, 0.56, 0.41, 0.27,
            24000.0, 2900.0, 4000.0, 1.18, true, true
        );
        EngineConfig torqueV6 = new EngineConfig(800.0, 3400.0, 6000.0, 430.0, 70.0);
        return VehicleConfig.builder(PRESET_OFFROAD)
            .body(body).wheels(wheels).engine(torqueV6)
            .transmission(TransmissionConfig.sixSpeedAutomatic()).steering(SteeringConfig.standardSedan())
            .differential(new LimitedSlipDifferential(0.85, 140.0)).antiRollBars(5200.0, 4800.0)
            .electronics(true, false).brakes(6000.0, 3600.0).build();
    }

    private static List<WheelConfig> buildFourWheels(
        double halfTrack, double mountY, double frontZ, double rearZ,
        double restLength, double radius, double width,
        double stiffness, double bumpDamping, double reboundDamping,
        double gripFactor, boolean frontDriven, boolean rearDriven
    ) {
        WheelConfig fl = WheelConfig.builder("FL", new Vec3d(halfTrack, mountY, frontZ))
            .geometry(restLength, radius, width).suspension(stiffness, bumpDamping, reboundDamping)
            .tire(1.25, gripFactor).roles(true, frontDriven, false, 0.30).build();
        WheelConfig fr = WheelConfig.builder("FR", new Vec3d(-halfTrack, mountY, frontZ))
            .geometry(restLength, radius, width).suspension(stiffness, bumpDamping, reboundDamping)
            .tire(1.25, gripFactor).roles(true, frontDriven, false, 0.30).build();
        WheelConfig rl = WheelConfig.builder("RL", new Vec3d(halfTrack, mountY, rearZ))
            .geometry(restLength, radius, width).suspension(stiffness, bumpDamping, reboundDamping)
            .tire(1.25, gripFactor).roles(false, rearDriven, true, 0.20).build();
        WheelConfig rr = WheelConfig.builder("RR", new Vec3d(-halfTrack, mountY, rearZ))
            .geometry(restLength, radius, width).suspension(stiffness, bumpDamping, reboundDamping)
            .tire(1.25, gripFactor).roles(false, rearDriven, true, 0.20).build();
        return List.of(fl, fr, rl, rr);
    }
}
