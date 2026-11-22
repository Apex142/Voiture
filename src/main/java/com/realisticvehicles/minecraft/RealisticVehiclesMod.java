package com.realisticvehicles.minecraft;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Main NeoForge 1.21.1 entry point for Realistic Vehicles.
 */
@Mod(RealisticVehiclesMod.MOD_ID)
public final class RealisticVehiclesMod {
    public static final String MOD_ID = "realisticvehicles";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RealisticVehiclesMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModNetwork.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(ModCommands::onRegisterCommands);
        LOGGER.info("Realistic Vehicles initialized (Minecraft 1.21.1 / NeoForge).");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Realistic Vehicles common setup complete.");
    }
}
