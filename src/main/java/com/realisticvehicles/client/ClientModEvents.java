package com.realisticvehicles.client;

import com.realisticvehicles.minecraft.ModEntities;
import com.realisticvehicles.minecraft.RealisticVehiclesMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Registers client-only renderers, key mappings, camera controller, HUD overlay, and input sampling.
 */
@EventBusSubscriber(modid = RealisticVehiclesMod.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {
    private static final VehicleInputHandler INPUT_HANDLER = new VehicleInputHandler();

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.VEHICLE.get(), ProvisionalVehicleRenderer::new);
        event.registerEntityRenderer(ModEntities.FUEL_PUMP.get(), FuelPumpRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        VehicleKeyMappings.onRegisterKeyMappings(event);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
            VanillaGuiLayers.HOTBAR,
            ResourceLocation.fromNamespaceAndPath(RealisticVehiclesMod.MOD_ID, "vehicle_hud"),
            VehicleHudOverlay::render
        );
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        INPUT_HANDLER.onClientTick();
        VehicleCameraController.onClientTick();
        VehicleAudioManager.onClientTick();
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        VehicleCameraController.onComputeCameraAngles(event);
    }
}
