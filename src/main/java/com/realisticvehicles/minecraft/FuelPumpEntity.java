package com.realisticvehicles.minecraft;

import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.interactions.FuelType;
import com.realisticvehicles.interactions.VehicleFuelSystem;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Interactive 3D Gas Station Fuel Pump entity supporting Gasoline (Essence SP98) & Diesel selection,
 * detachable fuel nozzle with flexible hose, and animated vehicle refueling.
 */
public final class FuelPumpEntity extends Entity {
    private static final EntityDataAccessor<String> DATA_FUEL_TYPE =
        SynchedEntityData.defineId(FuelPumpEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_HOLDER_ID =
        SynchedEntityData.defineId(FuelPumpEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VEHICLE_ID =
        SynchedEntityData.defineId(FuelPumpEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DISPENSED_LITERS =
        SynchedEntityData.defineId(FuelPumpEntity.class, EntityDataSerializers.FLOAT);

    public FuelPumpEntity(EntityType<? extends FuelPumpEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUEL_TYPE, FuelType.GASOLINE.name());
        builder.define(DATA_HOLDER_ID, -1);
        builder.define(DATA_VEHICLE_ID, -1);
        builder.define(DATA_DISPENSED_LITERS, 0.0F);
    }

    public FuelType selectedFuelType() {
        try {
            return FuelType.valueOf(this.entityData.get(DATA_FUEL_TYPE));
        } catch (IllegalArgumentException e) {
            return FuelType.GASOLINE;
        }
    }

    public int nozzleHolderId() { return this.entityData.get(DATA_HOLDER_ID); }
    public int connectedVehicleId() { return this.entityData.get(DATA_VEHICLE_ID); }
    public float dispensedLiters() { return this.entityData.get(DATA_DISPENSED_LITERS); }

    public void attachNozzleToVehicle(VehicleEntity vehicle, Player player) {
        this.entityData.set(DATA_HOLDER_ID, -1);
        this.entityData.set(DATA_VEHICLE_ID, vehicle.getId());
        this.entityData.set(DATA_DISPENSED_LITERS, 0.0F);
        level().playSound(null, vehicle.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.3F);
        player.displayClientMessage(Component.literal(
            "⛽ Pistolet branché (" + labelFor(selectedFuelType()) + ") — Remplissage du réservoir en cours..."
        ), true);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        int vehId = connectedVehicleId();
        if (vehId >= 0) {
            if (!(level().getEntity(vehId) instanceof VehicleEntity vehicle) || vehicle.distanceToSqr(this) > 64.0) {
                this.entityData.set(DATA_VEHICLE_ID, -1);
                return;
            }
            VehicleFuelSystem fuel = vehicle.fuelSystem();
            if (fuel.currentLiters() >= fuel.capacityLiters() - 0.05) {
                this.entityData.set(DATA_VEHICLE_ID, -1);
                level().playSound(null, blockPosition(), SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.9F, 1.5F);
                return;
            }
            double before = fuel.currentLiters();
            fuel.refuel(selectedFuelType(), 3.2, 0.05);
            double added = fuel.currentLiters() - before;
            this.entityData.set(DATA_DISPENSED_LITERS, dispensedLiters() + (float) added);
            if (tickCount % 6 == 0) {
                level().playSound(null, vehicle.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.45F, 1.3F);
            }
        }
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 hitVec, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide()) return InteractionResult.SUCCESS;

        // Upper display panel (y >= 1.12): left half = Essence SP98, right half = Diesel
        if (hitVec.y >= 1.12) {
            double yawRad = Math.toRadians(-getYRot());
            double localX = hitVec.x * Math.cos(yawRad) - hitVec.z * Math.sin(yawRad);
            FuelType next = (localX >= 0.0)
                ? FuelType.GASOLINE
                : (localX < -0.05 ? FuelType.DIESEL : (selectedFuelType() == FuelType.GASOLINE ? FuelType.DIESEL : FuelType.GASOLINE));
            this.entityData.set(DATA_FUEL_TYPE, next.name());
            level().playSound(null, blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.8F, 1.35F);
            player.displayClientMessage(Component.literal("⛽ Carburant sélectionné sur la pompe : " + labelFor(next)), true);
            return InteractionResult.CONSUME;
        }

        // Lower body / holster: take or return the fuel nozzle
        if (nozzleHolderId() == player.getId() || connectedVehicleId() >= 0) {
            this.entityData.set(DATA_HOLDER_ID, -1);
            this.entityData.set(DATA_VEHICLE_ID, -1);
            level().playSound(null, blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 1.2F);
            player.displayClientMessage(Component.literal("⛽ Pistolet raccroché à la pompe."), true);
        } else {
            this.entityData.set(DATA_HOLDER_ID, player.getId());
            this.entityData.set(DATA_VEHICLE_ID, -1);
            this.entityData.set(DATA_DISPENSED_LITERS, 0.0F);
            level().playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.8F, 1.2F);
            player.displayClientMessage(Component.literal(
                "⛽ Pistolet [" + labelFor(selectedFuelType()) + "] en main ! Faites Clic droit sur l'arrière de la voiture pour remplir."
            ), true);
        }
        return InteractionResult.CONSUME;
    }

    public static String labelFor(FuelType type) {
        return type == FuelType.DIESEL ? "DIESEL (Gazole B7)" : "ESSENCE (Sans Plomb 98)";
    }

    public static Vec3d fuelCapWorldPos(VehicleEntity vehicle) {
        Vec3d com = vehicle.level().isClientSide() ? vehicle.clientPose().currentPosition() : vehicle.simulation().body().position();
        Quat4d rot = vehicle.level().isClientSide() ? vehicle.clientPose().currentOrientation() : vehicle.simulation().body().orientation();
        return com.add(rot.rotate(new Vec3d(-1.26, 0.22, -1.52)));
    }

    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean canBeCollidedWith() { return true; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("FuelType")) this.entityData.set(DATA_FUEL_TYPE, tag.getString("FuelType"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("FuelType", this.entityData.get(DATA_FUEL_TYPE));
    }
}
