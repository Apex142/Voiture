package com.realisticvehicles.minecraft;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Physical car key item paired via UUID to a specific VehicleEntity for remote locking/unlocking.
 */
public final class VehicleKeyItem extends Item {
    private static final String TAG_VEHICLE_UUID = "VehicleUuid";

    public VehicleKeyItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createForVehicle(VehicleEntity vehicle) {
        ItemStack stack = new ItemStack(ModItems.VEHICLE_KEY.get());
        CompoundTag tag = new CompoundTag();
        tag.putUUID(TAG_VEHICLE_UUID, vehicle.getUUID());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(
            DataComponents.CUSTOM_NAME,
            Component.literal("Clé Mercedes-Benz [" + vehicle.simulation().config().presetId().toUpperCase() + "]")
        );
        return stack;
    }

    public static UUID getPairedUuid(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        return tag.hasUUID(TAG_VEHICLE_UUID) ? tag.getUUID(TAG_VEHICLE_UUID) : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        UUID targetUuid = getPairedUuid(stack);
        AABB search = player.getBoundingBox().inflate(20.0);
        VehicleEntity target = level.getEntitiesOfClass(VehicleEntity.class, search).stream()
            .filter(v -> targetUuid == null || v.getUUID().equals(targetUuid))
            .min(Comparator.comparingDouble(v -> v.distanceToSqr(player)))
            .orElse(null);

        if (target == null) {
            player.displayClientMessage(Component.literal("Aucun véhicule appairé à portée (20 blocs)."), true);
            return InteractionResultHolder.fail(stack);
        }
        target.toggleLockWithKey(player, targetUuid != null ? targetUuid : target.getUUID());
        return InteractionResultHolder.consume(stack);
    }
}
