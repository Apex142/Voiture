package com.realisticvehicles.client;

import com.realisticvehicles.minecraft.VehicleEntity;
import com.realisticvehicles.minecraft.VehicleStateSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

/**
 * Client-only network packet handlers isolated from dedicated server classloading.
 */
public final class ClientPacketHandlers {

    private ClientPacketHandlers() {
    }

    public static void handleVehicleStateSync(VehicleStateSyncPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(payload.entityId());
        if (entity instanceof VehicleEntity vehicle) {
            vehicle.applyClientSync(payload);
        }
    }
}
