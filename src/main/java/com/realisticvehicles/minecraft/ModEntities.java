package com.realisticvehicles.minecraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge deferred registration for vehicle entity types.
 */
public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.ENTITY_TYPE, RealisticVehiclesMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<VehicleEntity>> VEHICLE =
        ENTITIES.register(
            "vehicle",
            () -> EntityType.Builder.<VehicleEntity>of(VehicleEntity::new, MobCategory.MISC)
                .sized(1.4F, 1.1F)
                .clientTrackingRange(12)
                .updateInterval(1)
                .build("vehicle")
        );

    public static final DeferredHolder<EntityType<?>, EntityType<FuelPumpEntity>> FUEL_PUMP =
        ENTITIES.register(
            "fuel_pump",
            () -> EntityType.Builder.<FuelPumpEntity>of(FuelPumpEntity::new, MobCategory.MISC)
                .sized(0.95F, 2.15F)
                .clientTrackingRange(10)
                .updateInterval(2)
                .build("fuel_pump")
        );

    private ModEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}
