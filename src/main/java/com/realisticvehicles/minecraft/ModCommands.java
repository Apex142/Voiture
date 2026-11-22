package com.realisticvehicles.minecraft;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.simulation.VehiclePresetFactory;
import com.realisticvehicles.physics.simulation.VehicleTelemetry;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Registers /rv commands: spawn, info, and remove for testing and gameplay.
 */
public final class ModCommands {

    private ModCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("rv")
            .then(Commands.literal("spawn")
                .executes(ctx -> spawnVehicle(ctx.getSource(), VehiclePresetFactory.PRESET_SLK55))
                .then(Commands.argument("preset", StringArgumentType.word())
                    .executes(ctx -> spawnVehicle(ctx.getSource(), StringArgumentType.getString(ctx, "preset")))))
            .then(Commands.literal("info")
                .executes(ctx -> showVehicleInfo(ctx.getSource())))
            .then(Commands.literal("remove")
                .executes(ctx -> removeNearbyVehicles(ctx.getSource())))
            .then(Commands.literal("repair")
                .executes(ctx -> repairTargetVehicle(ctx.getSource())))
            .then(Commands.literal("pump")
                .executes(ctx -> spawnFuelPump(ctx.getSource())))
            .then(Commands.literal("cam")
                .executes(ctx -> toggleFreeCam(ctx.getSource(), "free"))
                .then(Commands.argument("mode", StringArgumentType.word())
                    .executes(ctx -> toggleFreeCam(ctx.getSource(), StringArgumentType.getString(ctx, "mode")))))
        );
    }

    private static int toggleFreeCam(CommandSourceStack source, String mode) {
        String m = mode.toLowerCase(Locale.ROOT);
        if ("cockpit".equals(m) || "interior".equals(m)) {
            com.realisticvehicles.client.VehicleCameraController.setActiveMode(com.realisticvehicles.client.CameraMode.INTERIOR_DRIVER);
        } else if ("3rd".equals(m) || "third".equals(m)) {
            com.realisticvehicles.client.VehicleCameraController.setActiveMode(com.realisticvehicles.client.CameraMode.THIRD_PERSON);
        } else if ("hood".equals(m)) {
            com.realisticvehicles.client.VehicleCameraController.setActiveMode(com.realisticvehicles.client.CameraMode.HOOD);
        } else {
            boolean enabled = com.realisticvehicles.client.VehicleCameraController.toggleFreeLook();
            source.sendSuccess(() -> Component.literal("🎥 Caméra libre 360° à la souris : " + (enabled ? "ACTIVÉE" : "VERROUILLÉE")), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("🎥 Mode caméra : " + com.realisticvehicles.client.VehicleCameraController.activeMode().label()), false);
        return 1;
    }

    private static int spawnVehicle(CommandSourceStack source, String preset) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command must be executed by a player."));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        VehicleEntity vehicle = ModEntities.VEHICLE.get().create(level);
        if (vehicle == null) {
            source.sendFailure(Component.literal("Failed to instantiate VehicleEntity."));
            return 0;
        }

        Vec3 look = player.getLookAngle();
        double horizLen = Math.hypot(look.x, look.z);
        double fx = horizLen > 1e-4 ? look.x / horizLen : 0.0;
        double fz = horizLen > 1e-4 ? look.z / horizLen : 1.0;

        double targetX = player.getX() + fx * 3.8;
        double targetZ = player.getZ() + fz * 3.8;
        double groundY = player.getY();
        net.minecraft.core.BlockPos checkPos = net.minecraft.core.BlockPos.containing(targetX, groundY, targetZ);
        while (!level.getBlockState(checkPos).isAir() && groundY < player.getY() + 4.0) {
            groundY += 1.0;
            checkPos = checkPos.above();
        }

        Vec3d spawnCom = new Vec3d(
            targetX,
            groundY + VehicleEntity.COM_GROUND_OFFSET + 0.12,
            targetZ
        );
        double yawRad = Math.atan2(fx, fz);
        String normalizedPreset = VehiclePresetFactory.create(preset).presetId();

        vehicle.setPos(targetX, groundY + 0.12, targetZ);
        vehicle.initializePreset(normalizedPreset, spawnCom, Quat4d.fromYaw(yawRad));
        level.addFreshEntity(vehicle);
        player.getInventory().add(VehicleKeyItem.createForVehicle(vehicle));
        source.sendSuccess(
            () -> Component.literal("Spawned realistic vehicle [" + normalizedPreset + "] + Clé remise (Clic droit pour monter, Shift+Clic droit pour ouvrir portes/capot/coffre)."),
            false
        );
        return 1;
    }

    private static int showVehicleInfo(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        VehicleEntity target = findTargetVehicle(player);
        if (target == null) {
            source.sendFailure(Component.literal("No vehicle found nearby (within 16 blocks)."));
            return 0;
        }
        VehicleTelemetry t = target.simulation().telemetry();
        String gearLabel = t.gear() < 0 ? "R" : (t.gear() == 0 ? "N" : String.valueOf(t.gear()));
        String msg = String.format(
            Locale.ROOT,
            "[RV %s] %.1f km/h | RPM: %.0f | Gear: %s | Steer: %.1f deg | Wheels: %d/4 | Sleep: %s",
            t.presetId(), t.speedKmh(), t.engineRpm(), gearLabel, t.steerAngleDeg(), t.groundedWheels(), t.sleeping()
        );
        source.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int removeNearbyVehicles(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        AABB box = player.getBoundingBox().inflate(32.0);
        List<VehicleEntity> found = player.serverLevel().getEntitiesOfClass(VehicleEntity.class, box);
        for (VehicleEntity vehicle : found) {
            vehicle.discard();
        }
        int count = found.size();
        source.sendSuccess(() -> Component.literal("Removed " + count + " realistic vehicle(s)."), false);
        return count;
    }

    private static int repairTargetVehicle(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        VehicleEntity target = findTargetVehicle(player);
        if (target == null) {
            source.sendFailure(Component.literal("Aucun véhicule trouvé à proximité."));
            return 0;
        }
        VehicleEntitySyncHelper.repairVehicle(target);
        source.sendSuccess(() -> Component.literal("🔧 Carrosserie et mécanique du véhicule remises à neuf !"), false);
        return 1;
    }

    private static int spawnFuelPump(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        double len = Math.hypot(look.x, look.z);
        double fx = len > 1e-4 ? look.x / len : 0.0, fz = len > 1e-4 ? look.z / len : 1.0;
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(player.getX() + fx * 2.5, player.getY(), player.getZ() + fz * 2.5);
        net.minecraft.core.Direction facing = player.getDirection().getOpposite();
        var state = ModBlocks.FUEL_PUMP_BLOCK.get().defaultBlockState().setValue(FuelPumpBlock.FACING, facing);
        level.setBlock(pos, state, 3);
        FuelPumpBlock.ensurePumpEntity(level, pos, state);
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.FUEL_PUMP_ITEM.get()));
        source.sendSuccess(() -> Component.literal("⛽ Bloc Pompe à essence posé (+ bloc ajouté dans votre inventaire) !"), false);
        return 1;
    }

    private static VehicleEntity findTargetVehicle(ServerPlayer player) {
        if (player.getVehicle() instanceof VehicleEntity ridden) {
            return ridden;
        }
        AABB box = player.getBoundingBox().inflate(16.0);
        return player.serverLevel().getEntitiesOfClass(VehicleEntity.class, box).stream()
            .min(Comparator.comparingDouble(v -> v.distanceToSqr(player)))
            .orElse(null);
    }
}
