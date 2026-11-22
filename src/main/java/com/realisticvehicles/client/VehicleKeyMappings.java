package com.realisticvehicles.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Rebindable vehicle key mappings defaulting to Z/S/Q/D/Space + lights, turn signals, doors, hood, trunk.
 */
public final class VehicleKeyMappings {
    private static final String CATEGORY = "key.categories.realisticvehicles";

    public static final KeyMapping ACCELERATE = create("key.realisticvehicles.accelerate", GLFW.GLFW_KEY_Z);
    public static final KeyMapping BRAKE_REVERSE = create("key.realisticvehicles.brake_reverse", GLFW.GLFW_KEY_S);
    public static final KeyMapping STEER_LEFT = create("key.realisticvehicles.steer_left", GLFW.GLFW_KEY_Q);
    public static final KeyMapping STEER_RIGHT = create("key.realisticvehicles.steer_right", GLFW.GLFW_KEY_D);
    public static final KeyMapping HANDBRAKE = create("key.realisticvehicles.handbrake", GLFW.GLFW_KEY_SPACE);
    public static final KeyMapping CYCLE_CAMERA = create("key.realisticvehicles.cycle_camera", GLFW.GLFW_KEY_C);
    public static final KeyMapping TOGGLE_LIGHTS = create("key.realisticvehicles.toggle_lights", GLFW.GLFW_KEY_L);
    public static final KeyMapping LEFT_INDICATOR = create("key.realisticvehicles.left_indicator", GLFW.GLFW_KEY_LEFT);
    public static final KeyMapping RIGHT_INDICATOR = create("key.realisticvehicles.right_indicator", GLFW.GLFW_KEY_RIGHT);
    public static final KeyMapping HAZARDS = create("key.realisticvehicles.hazards", GLFW.GLFW_KEY_DOWN);
    public static final KeyMapping TOGGLE_DOORS = create("key.realisticvehicles.toggle_doors", GLFW.GLFW_KEY_G);
    public static final KeyMapping TOGGLE_HOOD = create("key.realisticvehicles.toggle_hood", GLFW.GLFW_KEY_H);
    public static final KeyMapping TOGGLE_TRUNK = create("key.realisticvehicles.toggle_trunk", GLFW.GLFW_KEY_K);
    public static final KeyMapping HORN = create("key.realisticvehicles.horn", GLFW.GLFW_KEY_B);

    private VehicleKeyMappings() {
    }

    private static KeyMapping create(String translationKey, int defaultGlfwKey) {
        return new KeyMapping(
            translationKey,
            VehicleKeyConflictContext.DRIVING,
            InputConstants.Type.KEYSYM,
            defaultGlfwKey,
            CATEGORY
        );
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ACCELERATE);
        event.register(BRAKE_REVERSE);
        event.register(STEER_LEFT);
        event.register(STEER_RIGHT);
        event.register(HANDBRAKE);
        event.register(CYCLE_CAMERA);
        event.register(TOGGLE_LIGHTS);
        event.register(LEFT_INDICATOR);
        event.register(RIGHT_INDICATOR);
        event.register(HAZARDS);
        event.register(TOGGLE_DOORS);
        event.register(TOGGLE_HOOD);
        event.register(TOGGLE_TRUNK);
        event.register(HORN);
    }
}
