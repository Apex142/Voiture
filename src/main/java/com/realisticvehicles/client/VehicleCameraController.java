package com.realisticvehicles.client;

import com.realisticvehicles.core.math.MathUtil;
import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.minecraft.VehicleEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Manages multi-mode vehicle cameras, free 360-degree mouse orbit, and cinematic 3rd-person entry/exit animations.
 */
public final class VehicleCameraController {
    private static CameraMode activeMode = CameraMode.THIRD_PERSON;
    private static Vec3d smoothedOffset = CameraMode.THIRD_PERSON.localOffset();
    private static float smoothedPitch = CameraMode.THIRD_PERSON.pitchDeg();
    private static boolean freeLookEnabled = true;

    private static boolean wasRiding;
    private static VehicleEntity lastRiddenVehicle;
    private static int entryCameraTicks;
    private static int exitCameraTicks;
    private static CameraType savedPerspective;

    private VehicleCameraController() {
    }

    public static CameraMode activeMode() { return activeMode; }
    public static void setActiveMode(CameraMode mode) { if (mode != null) activeMode = mode; }
    public static boolean isFreeLookEnabled() { return freeLookEnabled; }
    public static boolean toggleFreeLook() { freeLookEnabled = !freeLookEnabled; return freeLookEnabled; }

    public static void onClientTick() {
        while (VehicleKeyMappings.CYCLE_CAMERA.consumeClick()) {
            activeMode = activeMode.next();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean riding = mc.player.getVehicle() instanceof VehicleEntity;
        if (riding && !wasRiding) {
            lastRiddenVehicle = (VehicleEntity) mc.player.getVehicle();
            lastRiddenVehicle.clientPose().beginEntryAnimation();
            savedPerspective = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            entryCameraTicks = 28;
            exitCameraTicks = 0;
        } else if (!riding && wasRiding) {
            savedPerspective = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            exitCameraTicks = 24;
            entryCameraTicks = 0;
        }
        wasRiding = riding;

        if (entryCameraTicks > 0) {
            if (--entryCameraTicks == 0 && savedPerspective != null && activeMode == CameraMode.INTERIOR_DRIVER) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
        }
        if (exitCameraTicks > 0) {
            // Walk player outward from the open driver door during the exit animation
            if (lastRiddenVehicle != null && exitCameraTicks > 10) {
                Quat4d rot = lastRiddenVehicle.clientPose().currentOrientation();
                Vec3d stepOut = rot.left().scale(0.055);
                mc.player.setDeltaMovement(stepOut.x(), mc.player.getDeltaMovement().y, stepOut.z());
            }
            if (--exitCameraTicks == 0 && savedPerspective != null) {
                mc.options.setCameraType(savedPerspective);
            }
        }

        if (!riding) {
            smoothedOffset = activeMode.localOffset();
            smoothedPitch = activeMode.pitchDeg();
            return;
        }
        smoothedOffset = smoothedOffset.lerp(activeMode.localOffset(), 0.25);
        smoothedPitch = (float) MathUtil.lerp(smoothedPitch, activeMode.pitchDeg(), 0.25);
    }

    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        float partialTick = (float) event.getPartialTick();

        // 1. Cinematic 3rd-person Exit Camera (watches player step out of the open driver door and close it)
        if (!(mc.player.getVehicle() instanceof VehicleEntity) && exitCameraTicks > 0 && lastRiddenVehicle != null) {
            var state = lastRiddenVehicle.buildRenderState(partialTick);
            applyDoorCinematicCamera(event, mc.level, state.position(), state.orientation(), false);
            return;
        }
        if (!(mc.player.getVehicle() instanceof VehicleEntity vehicle)) return;

        var renderState = vehicle.buildRenderState(partialTick);
        Vec3d carPos = renderState.position();
        Quat4d carRot = renderState.orientation();

        // 2. Cinematic 3rd-person Entry Camera (watches door open and player step in & sit behind the wheel)
        if (entryCameraTicks > 0) {
            applyDoorCinematicCamera(event, mc.level, carPos, carRot, true);
            return;
        }

        Vec3d worldAnchor = carPos.add(carRot.rotate(new Vec3d(0.0, 0.65, 0.0)));
        double carYawRad = carRot.yawRadians();
        float carMcYaw = (float) Math.toDegrees(-carYawRad);

        if (activeMode == CameraMode.THIRD_PERSON && freeLookEnabled) {
            float playerYaw = mc.player.getViewYRot(partialTick);
            float playerPitch = Math.max(-25.0F, Math.min(68.0F, mc.player.getViewXRot(partialTick)));
            double orbitDist = 6.2;
            double yawRad = Math.toRadians(-playerYaw), pitchRad = Math.toRadians(playerPitch);
            double horiz = orbitDist * Math.cos(pitchRad);
            Vec3d desiredWorld = new Vec3d(
                worldAnchor.x() - Math.sin(yawRad) * horiz,
                worldAnchor.y() + orbitDist * Math.sin(pitchRad) + 0.72,
                worldAnchor.z() - Math.cos(yawRad) * horiz
            );
            Vec3d safeWorld = clipCameraAgainstBlocks(mc.level, worldAnchor, desiredWorld);
            event.getCamera().setPosition(safeWorld.x(), safeWorld.y(), safeWorld.z());
            event.setYaw(playerYaw);
            event.setPitch(playerPitch);
            event.setRoll(0.0F);
            return;
        }

        Vec3d desiredWorld = carPos.add(carRot.rotate(smoothedOffset));
        Vec3d safeWorld = activeMode == CameraMode.INTERIOR_DRIVER
            ? desiredWorld
            : clipCameraAgainstBlocks(mc.level, worldAnchor, desiredWorld);

        Camera cam = event.getCamera();
        cam.setPosition(safeWorld.x(), safeWorld.y(), safeWorld.z());
        if (activeMode == CameraMode.INTERIOR_DRIVER && freeLookEnabled) {
            event.setYaw(mc.player.getViewYRot(partialTick));
            event.setPitch(mc.player.getViewXRot(partialTick));
        } else {
            event.setYaw(activeMode.lookBackward() ? carMcYaw + 180.0F : carMcYaw);
            event.setPitch(smoothedPitch);
        }
        event.setRoll((float) Math.toDegrees(-carRot.rollRadians() * 0.35));
    }

    private static void applyDoorCinematicCamera(
        ViewportEvent.ComputeCameraAngles event, Level level, Vec3d carPos, Quat4d carRot, boolean entering
    ) {
        Vec3d doorTarget = carPos.add(carRot.rotate(new Vec3d(0.75, 0.35, -0.35)));
        Vec3d camLocal = entering ? new Vec3d(3.4, 1.35, -2.2) : new Vec3d(3.6, 1.45, -1.9);
        Vec3d desiredCam = carPos.add(carRot.rotate(camLocal));
        Vec3d safeCam = clipCameraAgainstBlocks(level, doorTarget, desiredCam);
        event.getCamera().setPosition(safeCam.x(), safeCam.y(), safeCam.z());

        Vec3d dir = doorTarget.sub(safeCam);
        double horiz = Math.hypot(dir.x(), dir.z());
        float yaw = (float) Math.toDegrees(-Math.atan2(dir.x(), dir.z()));
        float pitch = (float) Math.toDegrees(Math.atan2(-dir.y(), horiz));
        event.setYaw(yaw);
        event.setPitch(pitch);
        event.setRoll(0.0F);
    }

    private static Vec3d clipCameraAgainstBlocks(Level level, Vec3d anchor, Vec3d desired) {
        Vec3 start = new Vec3(anchor.x(), anchor.y(), anchor.z());
        Vec3 end = new Vec3(desired.x(), desired.y(), desired.z());
        BlockHitResult hit = level.clip(new ClipContext(
            start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
            net.minecraft.world.phys.shapes.CollisionContext.empty()
        ));
        if (hit.getType() != HitResult.Type.BLOCK) return desired;
        Vec3 hitLoc = hit.getLocation();
        return new Vec3d(hitLoc.x, hitLoc.y, hitLoc.z).addScaled(anchor.sub(desired).normalize(), 0.25);
    }
}
