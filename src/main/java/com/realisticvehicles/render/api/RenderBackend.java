package com.realisticvehicles.render.api;

/**
 * Render backend abstraction allowing Phase 1 provisional rendering to be replaced
 * cleanly by the custom LWJGL Vulkan backend in Phase 3 without touching physics.
 */
public interface RenderBackend {
    BackendType type();

    void initialize();

    void renderVehicle(VehicleRenderState state, float partialTick);

    void dispose();
}
