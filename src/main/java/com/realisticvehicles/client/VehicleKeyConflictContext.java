package com.realisticvehicles.client;

import com.realisticvehicles.minecraft.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;

/**
 * Key conflict context active exclusively while the player drives a VehicleEntity.
 * Allows Z/S/Q/D/Space defaults without conflicting with on-foot controls.
 */
public enum VehicleKeyConflictContext implements IKeyConflictContext {
    DRIVING;

    @Override
    public boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof VehicleEntity;
    }

    @Override
    public boolean conflicts(IKeyConflictContext other) {
        return this == other;
    }
}
