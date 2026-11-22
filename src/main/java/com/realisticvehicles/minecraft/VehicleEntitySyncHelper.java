package com.realisticvehicles.minecraft;

import com.realisticvehicles.core.math.MathUtil;
import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.collision.VehicleToVehicleCollider;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Coordinates nearby collisions, transforms, cinematic entry/exit rider motion, refueling, and actions.
 */
public final class VehicleEntitySyncHelper {
    private static final Vec3d DOOR_THRESHOLD_LOCAL = new Vec3d(1.75, 0.02, -0.35);

    private VehicleEntitySyncHelper() {
    }

    public static void resolveNearbyCollisions(VehicleEntity self) {
        for (VehicleEntity other : self.level().getEntitiesOfClass(VehicleEntity.class, self.getBoundingBox().inflate(5.0))) {
            if (other != self && other.getId() > self.getId()) VehicleToVehicleCollider.resolvePair(self.simulation(), other.simulation());
        }
    }

    public static void syncTransform(VehicleEntity entity, double serverTickDt) {
        Vec3d com = entity.simulation().body().position(), vel = entity.simulation().body().linearVelocity();
        Quat4d rot = entity.simulation().body().orientation();
        entity.xo = entity.xOld = entity.getX(); entity.yo = entity.yOld = entity.getY(); entity.zo = entity.zOld = entity.getZ();
        entity.setPos(com.x(), com.y() - VehicleEntity.COM_GROUND_OFFSET, com.z());
        entity.setYRot((float) Math.toDegrees(-rot.yawRadians()));
        entity.setXRot((float) Math.toDegrees(rot.pitchRadians()));
        entity.setDeltaMovement(vel.x() * serverTickDt, vel.y() * serverTickDt, vel.z() * serverTickDt);
        VehicleDynamicLightManager.updateVehicleLights(entity);
    }

    public static void stepClient(VehicleEntity entity, VehicleClientPose clientPose, VehiclePartEntity[] parts) {
        com.realisticvehicles.client.VehicleAudioManager.trackClientVehicle(entity);
        clientPose.stepClientTick();
        Vec3d pos = clientPose.currentPosition();
        Quat4d rot = clientPose.currentOrientation();
        entity.xo = entity.xOld = entity.getX(); entity.yo = entity.yOld = entity.getY(); entity.zo = entity.zOld = entity.getZ();
        entity.setPos(pos.x(), pos.y() - VehicleEntity.COM_GROUND_OFFSET, pos.z());
        entity.setYRot((float) Math.toDegrees(-rot.yawRadians()));
        VehiclePartLayout.updatePartTransforms(parts, pos, rot, clientPose.speedKmh() < 0.2F);
    }

    public static void positionRider(VehicleEntity entity, VehicleClientPose clientPose, Vec3d seatLocal, Entity passenger, Entity.MoveFunction callback) {
        if (!entity.level().isClientSide()) {
            Vec3d seatWorld = entity.simulation().body().position().add(entity.simulation().body().orientation().rotate(seatLocal));
            callback.accept(passenger, seatWorld.x(), seatWorld.y() - 0.78, seatWorld.z());
            return;
        }
        Vec3d basePos = clientPose.currentPosition();
        Quat4d baseRot = clientPose.currentOrientation();
        double p = clientPose.entrySeatProgress();
        double moveT = MathUtil.clamp((p - 0.18) / 0.72, 0.0, 1.0), smoothMove = moveT * moveT * (3.0 - 2.0 * moveT);
        Vec3d animatedLocal = new Vec3d(1.52, -0.02, -0.32).lerp(seatLocal, smoothMove);
        double yOffset = MathUtil.lerp(-0.30, -0.78, smoothMove) + Math.sin(smoothMove * Math.PI) * 0.24;
        Vec3d seatWorld = basePos.add(baseRot.rotate(animatedLocal));
        callback.accept(passenger, seatWorld.x(), seatWorld.y() + yOffset, seatWorld.z());
        if (p < 0.98 && passenger instanceof Player player) {
            float bodyYaw = (float) Math.toDegrees(-baseRot.yawRadians()) + (1.0F - (float) MathUtil.clamp((p - 0.30) / 0.60, 0.0, 1.0)) * -82.0F;
            player.setYBodyRot(bodyYaw);
            player.setYHeadRot(bodyYaw);
        }
    }

    public static net.minecraft.world.phys.Vec3 dismountLocation(VehicleEntity entity) {
        Vec3d leftExit = entity.simulation().body().localToWorldPoint(new Vec3d(1.88, 0.1, -0.35));
        return new net.minecraft.world.phys.Vec3(leftExit.x(), Math.max(entity.getY(), leftExit.y()), leftExit.z());
    }

    public static void applyClientSync(VehicleClientPose clientPose, VehiclePartEntity[] parts, VehicleStateSyncPayload payload) {
        clientPose.applyServerState(payload);
        if (payload.speedKmh() < 0.2F) {
            Vec3d comPos = new Vec3d(payload.x(), payload.y(), payload.z());
            Quat4d rot = new Quat4d(payload.qx(), payload.qy(), payload.qz(), payload.qw()).normalize();
            VehiclePartLayout.updatePartTransforms(parts, comPos, rot, true);
        }
    }

    public static void applyAction(VehicleEntity v, byte actionId) {
        if (v.locked) return;
        switch (actionId) {
            case VehicleActionPayload.ACTION_HEADLIGHTS -> {
                int mode = ((v.lightsMask & 1) != 0 ? 1 : 0) + ((v.lightsMask & 2) != 0 ? 1 : 0);
                v.lightsMask &= ~3;
                if (mode == 0) v.lightsMask |= 1;
                else if (mode == 1) v.lightsMask |= 3;
            }
            case VehicleActionPayload.ACTION_LEFT_TURN -> v.lightsMask = (byte) ((v.lightsMask ^ 4) & ~8);
            case VehicleActionPayload.ACTION_RIGHT_TURN -> v.lightsMask = (byte) ((v.lightsMask ^ 8) & ~4);
            case VehicleActionPayload.ACTION_HAZARDS -> v.lightsMask ^= 16;
            case VehicleActionPayload.ACTION_DOORS -> {
                float next = (v.leftDoorTarget > 0.5F || v.rightDoorTarget > 0.5F) ? 0.0F : 1.0F;
                v.leftDoorTarget = next;
                v.rightDoorTarget = next;
            }
            case VehicleActionPayload.ACTION_HOOD -> v.hoodTarget = v.hoodTarget > 0.5F ? 0.0F : 1.0F;
            case VehicleActionPayload.ACTION_TRUNK -> v.trunkTarget = v.trunkTarget > 0.5F ? 0.0F : 1.0F;
        }
        v.level().playSound(null, v.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.6F, 1.2F);
    }

    public static void onCollisionImpact(VehicleEntity v, com.realisticvehicles.core.event.VehicleCollisionEvent ev) {
        if (ev.closingSpeed() < 2.2) return;
        Vec3d localPt = v.simulation().body().worldToLocalPoint(ev.contactPoint());
        float severity = (float) Math.min(0.55, (ev.closingSpeed() - 1.8) * 0.10);
        if (localPt.z() > 1.1) {
            v.frontDent = Math.min(1.0F, v.frontDent + severity);
            if (v.frontDent > 0.55F) v.hoodTarget = Math.max(v.hoodTarget, 0.12F);
        } else if (localPt.z() < -1.1) {
            v.rearDent = Math.min(1.0F, v.rearDent + severity);
            if (v.rearDent > 0.65F) v.trunkTarget = Math.max(v.trunkTarget, 0.10F);
        } else if (localPt.x() > 0.45) {
            v.leftDent = Math.min(1.0F, v.leftDent + severity);
        } else if (localPt.x() < -0.45) {
            v.rightDent = Math.min(1.0F, v.rightDent + severity);
        }
        v.level().playSound(null, v.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL,
            Math.min(0.9F, severity * 1.8F), 0.75F);
    }

    public static void repairVehicle(VehicleEntity v) {
        v.frontDent = 0.0F; v.rearDent = 0.0F; v.leftDent = 0.0F; v.rightDent = 0.0F;
        v.hoodTarget = 0.0F; v.trunkTarget = 0.0F;
        v.fuelSystem().restoreState(v.fuelSystem().capacityLiters(), false);
    }

    public static void toggleLockWithKey(VehicleEntity v, Player player, UUID keyVehicleUuid) {
        if (!v.getUUID().equals(keyVehicleUuid)) {
            player.displayClientMessage(Component.literal("❌ Cette clé ne correspond pas à ce véhicule !"), true);
            return;
        }
        v.locked = !v.locked;
        if (v.locked) { v.leftDoorTarget = 0.0F; v.rightDoorTarget = 0.0F; }
        player.displayClientMessage(Component.literal(v.locked ? "🔒 Véhicule verrouillé" : "🔓 Véhicule déverrouillé"), true);
    }

    public static InteractionResult handlePartInteraction(
        VehicleEntity vehicle, String partName, Player player, InteractionHand hand
    ) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof VehicleKeyItem) {
            if (!vehicle.level().isClientSide()) {
                UUID keyUuid = VehicleKeyItem.getPairedUuid(held);
                vehicle.toggleLockWithKey(player, keyUuid != null ? keyUuid : vehicle.getUUID());
            } else {
                com.realisticvehicles.client.VehicleAudioManager.playLockSound(
                    vehicle.getX(), vehicle.getY(), vehicle.getZ(), !vehicle.isLocked()
                );
            }
            return InteractionResult.sidedSuccess(vehicle.level().isClientSide());
        }
        if (vehicle.level().isClientSide()) return InteractionResult.SUCCESS;
        // Check if player is holding a fuel nozzle from a nearby FuelPumpEntity (server thread only)
        for (FuelPumpEntity pump : vehicle.level().getEntitiesOfClass(
            FuelPumpEntity.class, vehicle.getBoundingBox().inflate(9.0)
        )) {
            if (pump.nozzleHolderId() == player.getId()) {
                pump.attachNozzleToVehicle(vehicle, player);
                return InteractionResult.CONSUME;
            }
        }
        if (vehicle.isLocked()) {
            player.displayClientMessage(Component.literal("🔒 Véhicule verrouillé ! Utilisez votre clé."), true);
            vehicle.level().playSound(null, vehicle.blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 0.7F, 1.5F);
            return InteractionResult.CONSUME;
        }
        if ("front_hood".equals(partName) || ("front_nose".equals(partName) && player.isSecondaryUseActive())) {
            vehicle.handleAction(VehicleActionPayload.ACTION_HOOD, player);
            return InteractionResult.CONSUME;
        }
        if ("rear_trunk".equals(partName)) {
            vehicle.handleAction(VehicleActionPayload.ACTION_TRUNK, player);
            return InteractionResult.CONSUME;
        }
        if (player.isSecondaryUseActive()) {
            Vec3d localPlayer = vehicle.simulation().body().worldToLocalPoint(
                new Vec3d(player.getX(), player.getY(), player.getZ())
            );
            vehicle.toggleSideDoor(localPlayer.x() >= 0.0);
            return InteractionResult.CONSUME;
        }
        if (!vehicle.hasPassenger(player) && vehicle.getPassengers().isEmpty()) {
            vehicle.setDoorForEntry();
            return player.startRiding(vehicle) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }
}
