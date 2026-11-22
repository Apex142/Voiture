package com.realisticvehicles.minecraft;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge deferred item registration for vehicle keys.
 */
public final class ModItems {
    private static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(RealisticVehiclesMod.MOD_ID);

    public static final DeferredItem<VehicleKeyItem> VEHICLE_KEY =
        ITEMS.register("vehicle_key", () -> new VehicleKeyItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> FUEL_PUMP_ITEM =
        ITEMS.registerSimpleBlockItem("fuel_pump", ModBlocks.FUEL_PUMP_BLOCK);

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModItems::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES || event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(VEHICLE_KEY.get());
            event.accept(FUEL_PUMP_ITEM.get());
        }
    }
}
