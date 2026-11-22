package com.realisticvehicles.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.realisticvehicles.minecraft.VehicleActionPayload;
import com.realisticvehicles.minecraft.VehicleEntity;
import com.realisticvehicles.minecraft.VehicleInputPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Samples rebindable vehicle controls on the client and dispatches input and action commands.
 */
public final class VehicleInputHandler {
    private boolean prevLights;
    private boolean prevLeftTurn;
    private boolean prevRightTurn;
    private boolean prevHazards;
    private boolean prevDoors;
    private boolean prevHood;
    private boolean prevTrunk;
    private boolean prevHorn;

    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!(mc.player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        if (vehicle.getFirstPassenger() != mc.player) {
            return;
        }
        if (mc.screen != null) {
            PacketDistributor.sendToServer(new VehicleInputPayload(vehicle.getId(), 0.0F, 0.0F, 0.0F, false));
            return;
        }

        float throttle = isPressed(mc, VehicleKeyMappings.ACCELERATE) ? 1.0F : 0.0F;
        float brake = isPressed(mc, VehicleKeyMappings.BRAKE_REVERSE) ? 1.0F : 0.0F;
        float steer = 0.0F;
        if (isPressed(mc, VehicleKeyMappings.STEER_LEFT)) {
            steer += 1.0F;
        }
        if (isPressed(mc, VehicleKeyMappings.STEER_RIGHT)) {
            steer -= 1.0F;
        }
        boolean handbrake = isPressed(mc, VehicleKeyMappings.HANDBRAKE);

        PacketDistributor.sendToServer(
            new VehicleInputPayload(vehicle.getId(), throttle, brake, steer, handbrake)
        );

        prevLights = dispatchEdge(mc, vehicle.getId(), VehicleKeyMappings.TOGGLE_LIGHTS, prevLights, VehicleActionPayload.ACTION_HEADLIGHTS);
        prevLeftTurn = dispatchEdge(mc, vehicle.getId(), VehicleKeyMappings.LEFT_INDICATOR, prevLeftTurn, VehicleActionPayload.ACTION_LEFT_TURN);
        prevRightTurn = dispatchEdge(mc, vehicle.getId(), VehicleKeyMappings.RIGHT_INDICATOR, prevRightTurn, VehicleActionPayload.ACTION_RIGHT_TURN);
        prevHazards = dispatchEdge(mc, vehicle.getId(), VehicleKeyMappings.HAZARDS, prevHazards, VehicleActionPayload.ACTION_HAZARDS);
        prevDoors = dispatchEdge(mc, vehicle.getId(), VehicleKeyMappings.TOGGLE_DOORS, prevDoors, VehicleActionPayload.ACTION_DOORS);
        prevHood = dispatchEdge(mc, vehicle.getId(), VehicleKeyMappings.TOGGLE_HOOD, prevHood, VehicleActionPayload.ACTION_HOOD);
        prevTrunk = dispatchEdge(mc, vehicle.getId(), VehicleKeyMappings.TOGGLE_TRUNK, prevTrunk, VehicleActionPayload.ACTION_TRUNK);
        boolean hornDown = isPressed(mc, VehicleKeyMappings.HORN);
        if (hornDown && !prevHorn) {
            VehicleAudioManager.playHorn(vehicle.getX(), vehicle.getY(), vehicle.getZ());
        }
        prevHorn = hornDown;
    }

    private boolean dispatchEdge(Minecraft mc, int entityId, KeyMapping key, boolean wasDown, byte actionId) {
        boolean down = isPressed(mc, key);
        if (down && !wasDown) {
            PacketDistributor.sendToServer(new VehicleActionPayload(entityId, actionId));
        }
        return down;
    }

    private boolean isPressed(Minecraft mc, KeyMapping mapping) {
        if (mapping.isDown()) {
            return true;
        }
        InputConstants.Key key = mapping.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue()) {
            return InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
        }
        return false;
    }
}
