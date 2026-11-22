package com.realisticvehicles.minecraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server discrete action packet for toggling lights, turn signals, doors, hood, and trunk.
 * actionId: 1=CycleHeadlights, 2=LeftTurn, 3=RightTurn, 4=Hazards, 5=Doors, 6=Hood, 7=Trunk.
 */
public record VehicleActionPayload(int entityId, byte actionId) implements CustomPacketPayload {

    public static final byte ACTION_HEADLIGHTS = 1;
    public static final byte ACTION_LEFT_TURN = 2;
    public static final byte ACTION_RIGHT_TURN = 3;
    public static final byte ACTION_HAZARDS = 4;
    public static final byte ACTION_DOORS = 5;
    public static final byte ACTION_HOOD = 6;
    public static final byte ACTION_TRUNK = 7;
    public static final byte ACTION_HORN = 8;

    public static final Type<VehicleActionPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(RealisticVehiclesMod.MOD_ID, "vehicle_action")
    );

    public static final StreamCodec<ByteBuf, VehicleActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, VehicleActionPayload::entityId,
        ByteBufCodecs.BYTE, VehicleActionPayload::actionId,
        VehicleActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(VehicleActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.level().getEntity(payload.entityId()) instanceof VehicleEntity vehicle) {
                if (player.distanceToSqr(vehicle) <= 64.0) {
                    vehicle.handleAction(payload.actionId(), player);
                }
            }
        });
    }
}
