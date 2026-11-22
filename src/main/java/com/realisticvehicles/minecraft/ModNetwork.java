package com.realisticvehicles.minecraft;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers NeoForge 1.21.1 custom network payloads for vehicle control, actions, and state sync.
 */
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
            VehicleInputPayload.TYPE,
            VehicleInputPayload.STREAM_CODEC,
            VehicleInputPayload::handleServer
        );
        registrar.playToServer(
            VehicleActionPayload.TYPE,
            VehicleActionPayload.STREAM_CODEC,
            VehicleActionPayload::handleServer
        );
        registrar.playToClient(
            VehicleStateSyncPayload.TYPE,
            VehicleStateSyncPayload.STREAM_CODEC,
            VehicleStateSyncPayload::handleClient
        );
    }
}
