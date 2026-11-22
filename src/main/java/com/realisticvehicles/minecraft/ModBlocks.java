package com.realisticvehicles.minecraft;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge deferred block registration for the placeable Gas Station Fuel Pump block.
 */
public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(RealisticVehiclesMod.MOD_ID);

    public static final DeferredBlock<FuelPumpBlock> FUEL_PUMP_BLOCK =
        BLOCKS.register(
            "fuel_pump",
            () -> new FuelPumpBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
            )
        );

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
