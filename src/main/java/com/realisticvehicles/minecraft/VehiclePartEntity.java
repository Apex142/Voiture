package com.realisticvehicles.minecraft;

import com.realisticvehicles.core.math.Vec3d;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * Contoured sub-hitbox part (Composite pattern) attached to a VehicleEntity.
 * Instances are created once per VehicleEntity so NeoForge's level.partEntities registry stays valid.
 */
public final class VehiclePartEntity extends PartEntity<VehicleEntity> {
    private final String partName;
    private Vec3d localOffset;
    private EntityDimensions dimensions;

    public VehiclePartEntity(VehicleEntity parent, String partName, Vec3d localOffset, float width, float height) {
        super(parent);
        this.partName = partName;
        this.localOffset = localOffset;
        this.dimensions = EntityDimensions.scalable(width, height);
        this.blocksBuilding = true;
        this.refreshDimensions();
    }

    public void reconfigure(Vec3d newLocalOffset, float width, float height) {
        this.localOffset = newLocalOffset;
        this.dimensions = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
    }

    public String partName() {
        return partName;
    }

    public Vec3d localOffset() {
        return localOffset;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return dimensions;
    }

    @Override
    public boolean isPickable() {
        return !getParent().isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return !getParent().isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return getParent().interactPart(this.partName, player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().hurt(source, amount);
    }

    @Override
    public boolean is(net.minecraft.world.entity.Entity entity) {
        return this == entity || getParent() == entity;
    }
}
