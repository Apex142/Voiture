package com.realisticvehicles.minecraft;

import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.simulation.VehicleInput;
import com.realisticvehicles.physics.simulation.VehiclePresetFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * End-to-end Minecraft 1.21.1 NeoForge GameTests validating VehicleEntity on real blocks.
 */
@GameTestHolder(RealisticVehiclesMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VehicleGameTests {

    private VehicleGameTests() {
    }

    @GameTest(template = "flat_track", timeoutTicks = 120)
    public static void vehicleSpawnsAndSettlesOnBlocks(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(10, 1, 6));
        VehicleEntity vehicle = spawnAt(helper, origin, VehiclePresetFactory.PRESET_SEDAN);

        helper.runAfterDelay(40, () -> {
            int grounded = vehicle.simulation().telemetry().groundedWheels();
            double speed = vehicle.simulation().telemetry().speedMps();
            if (grounded != 4) {
                helper.fail("Expected 4 grounded wheels on concrete blocks, got: " + grounded);
            }
            if (speed > 0.15) {
                helper.fail("Expected vehicle stable at rest, got speed: " + speed);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "flat_track", timeoutTicks = 140)
    public static void vehicleDrivesAndSteersOnMinecraftBlocks(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(10, 1, 5));
        VehicleEntity vehicle = spawnAt(helper, origin, VehiclePresetFactory.PRESET_SEDAN);
        double startX = vehicle.getX();
        double startZ = vehicle.getZ();

        for (int tick = 10; tick <= 60; tick++) {
            helper.runAfterDelay(tick, () -> vehicle.applyDriverInput(new VehicleInput(1.0, 0.0, 0.35, false)));
        }
        helper.runAfterDelay(65, () -> {
            double dz = vehicle.getZ() - startZ;
            double dx = vehicle.getX() - startX;
            if (dz < 6.0) {
                helper.fail("Vehicle failed to drive forward on Minecraft blocks, dz=" + dz);
            }
            if (dx < 1.0) {
                helper.fail("Vehicle failed to steer left (+X) on Minecraft blocks, dx=" + dx);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "flat_track", timeoutTicks = 140)
    public static void vehicleStopsAtMinecraftStoneWall(GameTestHelper helper) {
        for (int x = 5; x <= 15; x++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(x, y, 16), Blocks.STONE);
            }
        }
        BlockPos origin = helper.absolutePos(new BlockPos(10, 1, 6));
        VehicleEntity vehicle = spawnAt(helper, origin, VehiclePresetFactory.PRESET_SPORT);
        double wallWorldZ = helper.absolutePos(new BlockPos(10, 1, 16)).getZ();

        for (int tick = 5; tick <= 70; tick++) {
            helper.runAfterDelay(tick, () -> vehicle.applyDriverInput(new VehicleInput(1.0, 0.0, 0.0, false)));
        }
        helper.runAfterDelay(75, () -> {
            if (vehicle.getZ() >= wallWorldZ) {
                helper.fail("Vehicle penetrated Minecraft stone wall! z=" + vehicle.getZ() + ", wallZ=" + wallWorldZ);
            }
            if (!vehicle.simulation().body().position().isFinite()) {
                helper.fail("Vehicle state became non-finite after wall collision");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "flat_track", timeoutTicks = 60)
    public static void vehiclePersistsStateToNbt(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(10, 1, 8));
        VehicleEntity original = spawnAt(helper, origin, VehiclePresetFactory.PRESET_SPORT);
        original.simulation().body().setLinearVelocity(new Vec3d(0.0, 0.0, 12.5));
        original.simulation().transmission().setCurrentGear(3);

        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);

        VehicleEntity restored = ModEntities.VEHICLE.get().create(helper.getLevel());
        if (restored == null) {
            helper.fail("Failed to create VehicleEntity for NBT load test");
            return;
        }
        restored.load(saved);
        if (!VehiclePresetFactory.PRESET_SPORT.equals(restored.simulation().config().presetId())) {
            helper.fail("Preset mismatch after NBT reload");
        }
        if (Math.abs(restored.simulation().body().linearVelocity().z() - 12.5) > 1e-4) {
            helper.fail("Velocity mismatch after NBT reload");
        }
        if (restored.simulation().transmission().currentGear() != 3) {
            helper.fail("Gear mismatch after NBT reload");
        }
        helper.succeed();
    }

    private static VehicleEntity spawnAt(GameTestHelper helper, BlockPos absPos, String preset) {
        VehicleEntity vehicle = ModEntities.VEHICLE.get().create(helper.getLevel());
        if (vehicle == null) {
            throw new IllegalStateException("Could not create VehicleEntity");
        }
        Vec3d com = new Vec3d(
            absPos.getX() + 0.5,
            absPos.getY() + VehicleEntity.COM_GROUND_OFFSET + 0.1,
            absPos.getZ() + 0.5
        );
        vehicle.initializePreset(preset, com, Quat4d.IDENTITY);
        helper.getLevel().addFreshEntity(vehicle);
        return vehicle;
    }
}
