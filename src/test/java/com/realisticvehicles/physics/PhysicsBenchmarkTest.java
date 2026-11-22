package com.realisticvehicles.physics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.simulation.VehicleInput;
import com.realisticvehicles.physics.simulation.VehiclePresetFactory;
import com.realisticvehicles.physics.simulation.VehicleSimulation;
import com.realisticvehicles.physics.world.SurfaceProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

final class PhysicsBenchmarkTest {

    @Test
    void measureMultiVehicleCpuTickTime() {
        TestCollisionWorld world = new TestCollisionWorld(0.0, SurfaceProperties.ASPHALT);
        int[] vehicleCounts = { 1, 5, 10, 20 };

        for (int count : vehicleCounts) {
            List<VehicleSimulation> fleet = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                VehicleSimulation sim = new VehicleSimulation(
                    VehiclePresetFactory.createSedan(),
                    new Vec3d(i * 4.0, 0.72, 0.0),
                    Quat4d.IDENTITY
                );
                sim.setInput(new VehicleInput(0.8, 0.0, (i % 2 == 0) ? 0.25 : -0.25, false));
                fleet.add(sim);
            }

            // Warmup JIT
            for (int t = 0; t < 40; t++) {
                for (VehicleSimulation sim : fleet) {
                    sim.step(0.05, world);
                }
            }

            int measuredTicks = 200;
            long startNs = System.nanoTime();
            for (int t = 0; t < measuredTicks; t++) {
                for (VehicleSimulation sim : fleet) {
                    sim.step(0.05, world);
                }
            }
            long elapsedNs = System.nanoTime() - startNs;
            double avgMsPerServerTick = (elapsedNs / (double) measuredTicks) / 1_000_000.0;
            System.out.printf(
                Locale.ROOT,
                "[BENCHMARK] %2d vehicle(s): %.4f ms / 20Hz tick (6x 120Hz substeps)%n",
                count, avgMsPerServerTick
            );
            assertTrue(avgMsPerServerTick < 15.0, "20 vehicles must stay well under 50ms Minecraft tick budget");
        }
    }
}
