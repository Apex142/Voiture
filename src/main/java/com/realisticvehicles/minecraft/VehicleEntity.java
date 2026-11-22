package com.realisticvehicles.minecraft;

import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.interactions.FuelType;
import com.realisticvehicles.interactions.VehicleFuelSystem;
import com.realisticvehicles.physics.simulation.VehicleConfig;
import com.realisticvehicles.physics.simulation.VehicleInput;
import com.realisticvehicles.physics.simulation.VehiclePresetFactory;
import com.realisticvehicles.physics.simulation.VehicleSimulation;
import com.realisticvehicles.render.api.VehicleRenderState;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Minecraft 1.21.1 vehicle entity with contoured PartEntity hitboxes, keys, opening panels, fuel tank, and lights.
 */
public final class VehicleEntity extends Entity {
    public static final double COM_GROUND_OFFSET = 0.80;
    private static final double SERVER_TICK_DT = 0.05;
    private static final Vec3d DRIVER_SEAT_LOCAL = new Vec3d(0.54, 0.02, -0.54);
    private static final EntityDataAccessor<String> DATA_PRESET_ID =
        SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);

    private final VehicleStateRepository repository = new VehicleStateRepository();
    private final BlockSurfaceMapper surfaceMapper = new BlockSurfaceMapper();
    private final VehicleFuelSystem fuelSystem = new VehicleFuelSystem(FuelType.GASOLINE, 70.0, 22.0);
    private final VehiclePartEntity[] parts;
    private MinecraftCollisionWorld collisionWorld;
    private VehicleSimulation simulation;
    private VehicleClientPose clientPose;
    private int inputTimeoutTicks;
    private int autoCloseDoorTicks;
    boolean locked;
    byte lightsMask;
    float leftDoorTarget, rightDoorTarget, hoodTarget, trunkTarget;
    float frontDent, rearDent, leftDent, rightDent;

    public VehicleEntity(EntityType<? extends VehicleEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        VehicleConfig initialCfg = VehiclePresetFactory.create(VehiclePresetFactory.PRESET_SLK55);
        this.parts = VehiclePartLayout.createInitialParts(this, initialCfg.body());
        setId(getId());
        initializeSimulation(VehiclePresetFactory.PRESET_SLK55, new Vec3d(getX(), getY() + COM_GROUND_OFFSET, getZ()), Quat4d.IDENTITY);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts != null) for (int i = 0; i < parts.length; i++) parts[i].setId(id + i + 1);
    }

    public void initializePreset(String presetId, Vec3d comPos, Quat4d orientation) {
        this.entityData.set(DATA_PRESET_ID, presetId);
        initializeSimulation(presetId, comPos, orientation);
        VehicleEntitySyncHelper.syncTransform(this, SERVER_TICK_DT);
    }

    private void initializeSimulation(String presetId, Vec3d comPos, Quat4d orientation) {
        VehicleConfig config = VehiclePresetFactory.create(presetId);
        this.simulation = new VehicleSimulation(config, comPos, orientation);
        this.simulation.eventBus().subscribe(com.realisticvehicles.core.event.VehicleCollisionEvent.class, ev -> VehicleEntitySyncHelper.onCollisionImpact(this, ev));
        this.clientPose = new VehicleClientPose(comPos, orientation);
        this.collisionWorld = new MinecraftCollisionWorld(level(), surfaceMapper);
        VehiclePartLayout.configureParts(this.parts, config.body());
        VehiclePartLayout.updatePartTransforms(this.parts, comPos, orientation, true);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(DATA_PRESET_ID, VehiclePresetFactory.PRESET_SLK55); }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_PRESET_ID.equals(key) && level().isClientSide()) {
            String preset = this.entityData.get(DATA_PRESET_ID);
            if (!simulation.config().presetId().equals(preset)) initializeSimulation(preset, clientPose.currentPosition(), clientPose.currentOrientation());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            VehicleEntitySyncHelper.stepClient(this, clientPose, parts);
            return;
        }
        if (autoCloseDoorTicks > 0 && --autoCloseDoorTicks == 0) leftDoorTarget = 0.0F;
        if (++inputTimeoutTicks > 10) simulation.setInput(VehicleInput.NEUTRAL);

        VehicleInput curIn = simulation.currentInput();
        fuelSystem.consume(curIn.throttle(), simulation.engine().currentRpm(), SERVER_TICK_DT);
        if (!fuelSystem.canEngineRun() && curIn.throttle() > 0.01) {
            simulation.setInput(new VehicleInput(0.0, curIn.brake(), curIn.steer(), curIn.handbrake()));
        }

        double speedBefore = simulation.body().linearVelocity().length();
        simulation.step(SERVER_TICK_DT, collisionWorld);
        double speedAfter = simulation.body().linearVelocity().length();
        if (speedBefore - speedAfter > 2.2) {
            Vec3d fwd = simulation.body().orientation().forward();
            double dirSign = simulation.body().linearVelocity().dot(fwd) >= -0.2 ? 1.0 : -1.0;
            Vec3d hitPt = simulation.body().position().addScaled(fwd, dirSign * 2.4);
            VehicleEntitySyncHelper.onCollisionImpact(
                this, new com.realisticvehicles.core.event.VehicleCollisionEvent(hitPt, fwd.negate(), 1000.0, (speedBefore - speedAfter) * 1.8)
            );
        }
        VehicleEntitySyncHelper.resolveNearbyCollisions(this);
        VehicleEntitySyncHelper.syncTransform(this, SERVER_TICK_DT);
        VehiclePartLayout.updatePartTransforms(parts, simulation.body().position(), simulation.body().orientation(), false);
        PacketDistributor.sendToPlayersTrackingEntity(this, VehicleStateSyncPayload.fromEntity(this));
    }

    public void handleAction(byte actionId, Player player) { VehicleEntitySyncHelper.applyAction(this, actionId); }
    public void toggleLockWithKey(Player player, UUID keyVehicleUuid) { VehicleEntitySyncHelper.toggleLockWithKey(this, player, keyVehicleUuid); }

    public void toggleSideDoor(boolean leftSide) {
        if (leftSide) leftDoorTarget = leftDoorTarget > 0.5F ? 0.0F : 1.0F;
        else rightDoorTarget = rightDoorTarget > 0.5F ? 0.0F : 1.0F;
        level().playSound(null, blockPosition(), SoundEvents.IRON_DOOR_OPEN, SoundSource.NEUTRAL, 0.7F, 1.1F);
    }

    public void setDoorForEntry() { leftDoorTarget = 1.0F; autoCloseDoorTicks = 30; }
    public VehicleFuelSystem fuelSystem() { return fuelSystem; }
    public boolean isLocked() { return locked; }
    public byte lightsMask() { return lightsMask; }
    public float leftDoorTarget() { return leftDoorTarget; }
    public float rightDoorTarget() { return rightDoorTarget; }
    public float hoodTarget() { return hoodTarget; }
    public float trunkTarget() { return trunkTarget; }
    public VehicleClientPose clientPose() { return clientPose; }
    public VehicleSimulation simulation() { return simulation; }
    public void applyDriverInput(VehicleInput input) { this.inputTimeoutTicks = 0; this.simulation.setInput(input); }
    public void applyClientSync(VehicleStateSyncPayload payload) { VehicleEntitySyncHelper.applyClientSync(clientPose, parts, payload); }
    public VehicleRenderState buildRenderState(float partialTick) { return clientPose.interpolate(simulation.config(), partialTick); }
    public InteractionResult interactPart(String partName, Player player, InteractionHand hand) { return VehicleEntitySyncHelper.handlePartInteraction(this, partName, player, hand); }

    @Override public InteractionResult interact(Player player, InteractionHand hand) { return interactPart("cabin", player, hand); }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (getPassengers().isEmpty()) { simulation.setInput(VehicleInput.NEUTRAL); setDoorForEntry(); }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (hasPassenger(passenger)) VehicleEntitySyncHelper.positionRider(this, clientPose, DRIVER_SEAT_LOCAL, passenger, callback);
    }

    @Override public net.minecraft.world.phys.Vec3 getDismountLocationForPassenger(LivingEntity passenger) { return VehicleEntitySyncHelper.dismountLocation(this); }
    @Override public boolean shouldRiderSit() { return !level().isClientSide() || clientPose.entrySeatProgress() > 0.58F; }
    @Override public boolean isMultipartEntity() { return true; }
    @Override public PartEntity<?>[] getParts() { return parts; }
    @Override public LivingEntity getControllingPassenger() { return (!level().isClientSide() && getFirstPassenger() instanceof LivingEntity l) ? l : null; }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {}
    @Override public void onRemovedFromLevel() { super.onRemovedFromLevel(); VehicleDynamicLightManager.clearVehicleLights(this); }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.simulation = repository.load(tag, new Vec3d(getX(), getY() + COM_GROUND_OFFSET, getZ()));
        this.locked = tag.getBoolean("Locked");
        this.lightsMask = tag.getByte("LightsMask");
        if (tag.contains("FuelLiters")) fuelSystem.restoreState(tag.getDouble("FuelLiters"), tag.getBoolean("FuelMisfuel"));
        this.entityData.set(DATA_PRESET_ID, simulation.config().presetId());
        this.clientPose = new VehicleClientPose(simulation.body().position(), simulation.body().orientation());
        VehiclePartLayout.configureParts(this.parts, simulation.config().body());
        VehiclePartLayout.updatePartTransforms(this.parts, simulation.body().position(), simulation.body().orientation(), true);
        VehicleEntitySyncHelper.syncTransform(this, SERVER_TICK_DT);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        repository.save(simulation, tag);
        tag.putBoolean("Locked", locked);
        tag.putByte("LightsMask", lightsMask);
        tag.putDouble("FuelLiters", fuelSystem.currentLiters());
        tag.putBoolean("FuelMisfuel", fuelSystem.isMisfueled());
    }
}
