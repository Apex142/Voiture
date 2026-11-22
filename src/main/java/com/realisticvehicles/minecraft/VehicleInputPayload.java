package com.realisticvehicles.minecraft;

import com.realisticvehicles.physics.simulation.VehicleInput;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server driver control packet (Command pattern over NeoForge 1.21.1 networking).
 */
public record VehicleInputPayload(
    int entityId,
    float throttle,
    float brake,
    float steer,
    boolean handbrake
) implements CustomPacketPayload {

    public static final Type<VehicleInputPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(RealisticVehiclesMod.MOD_ID, "vehicle_input")
    );

    public static final StreamCodec<ByteBuf, VehicleInputPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, VehicleInputPayload::entityId,
        ByteBufCodecs.FLOAT, VehicleInputPayload::throttle,
        ByteBufCodecs.FLOAT, VehicleInputPayload::brake,
        ByteBufCodecs.FLOAT, VehicleInputPayload::steer,
        ByteBufCodecs.BOOL, VehicleInputPayload::handbrake,
        VehicleInputPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(VehicleInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getId() == payload.entityId()) {
                if (vehicle.getControllingPassenger() == player) {
                    vehicle.applyDriverInput(new VehicleInput(
                        payload.throttle(),
                        payload.brake(),
                        payload.steer(),
                        payload.handbrake()
                    ));
                }
            }
        });
    }
}
