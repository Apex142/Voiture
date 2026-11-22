package com.realisticvehicles.client;

import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.minecraft.VehicleClientPose;
import com.realisticvehicles.minecraft.VehicleEntity;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

/**
 * OpenAL 3D automotive sound & VFX controller:
 * V8 AMG engine rumble, road rolling noise, GTA V lateral-G/handbrake tire skid, lift-off exhaust burble, lock chirp, horn.
 */
public final class VehicleAudioManager {
    private static int engineBuf, roadBuf, skidBuf, backfireBuf, lockBuf, unlockBuf, hornBuf;
    private static boolean initialized;

    private static final Map<Integer, int[]> ENTITY_SOURCES = new HashMap<>();
    private static final Map<Integer, Float> PREV_RPM = new HashMap<>();
    private static final Map<Integer, Integer> PREV_GEAR = new HashMap<>();
    private static final Map<Integer, Boolean> WAS_ACCELERATING = new HashMap<>();
    private static final Map<Integer, Long> LAST_BACKFIRE_MS = new HashMap<>();
    private static final Map<Integer, VehicleEntity> CLIENT_VEHICLES = new java.util.concurrent.ConcurrentHashMap<>();

    private VehicleAudioManager() {}

    public static void trackClientVehicle(VehicleEntity v) {
        if (v != null && v.level().isClientSide()) CLIENT_VEHICLES.put(v.getId(), v);
    }

    private static void ensureInitialized() {
        if (initialized) return;
        try {
            AL10.alGetError();
            engineBuf = createBuffer(VehicleSoundSynthesizer.synthesizeV8Loop());
            roadBuf = createBuffer(VehicleSoundSynthesizer.synthesizeRoadRollingLoop());
            skidBuf = createBuffer(VehicleSoundSynthesizer.synthesizeTireScreechLoop());
            backfireBuf = createBuffer(VehicleSoundSynthesizer.synthesizeExhaustBackfire());
            lockBuf = createBuffer(VehicleSoundSynthesizer.synthesizeChirp(1));
            unlockBuf = createBuffer(VehicleSoundSynthesizer.synthesizeChirp(2));
            hornBuf = createBuffer(VehicleSoundSynthesizer.synthesizeDualToneHorn());
            initialized = engineBuf != 0;
        } catch (Throwable ignored) {}
    }

    public static void playLockSound(double x, double y, double z, boolean locked) {
        ensureInitialized(); if (initialized) playOneShot(locked ? lockBuf : unlockBuf, x, y, z, 0.95F, 1.0F);
    }

    public static void playHorn(double x, double y, double z) {
        ensureInitialized(); if (initialized) playOneShot(hornBuf, x, y, z, 0.92F, 1.0F);
    }

    private static void playOneShot(int buf, double x, double y, double z, float gain, float pitch) {
        int src = AL10.alGenSources();
        if (src == 0) return;
        AL10.alSourcei(src, AL10.AL_BUFFER, buf);
        AL10.alSource3f(src, AL10.AL_POSITION, (float) x, (float) y, (float) z);
        AL10.alSourcef(src, AL10.AL_GAIN, gain);
        AL10.alSourcef(src, AL10.AL_PITCH, pitch);
        AL10.alSourcef(src, AL10.AL_REFERENCE_DISTANCE, 5.0F);
        AL10.alSourcef(src, AL10.AL_MAX_DISTANCE, 40.0F);
        AL10.alSourcePlay(src);
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            stopAll();
            return;
        }
        ensureInitialized();
        if (!initialized) return;

        var it = CLIENT_VEHICLES.entrySet().iterator();
        while (it.hasNext()) {
            VehicleEntity vehicle = it.next().getValue();
            if (vehicle == null || vehicle.isRemoved() || vehicle.level() != mc.level) {
                it.remove();
                continue;
            }
            if (mc.player.distanceToSqr(vehicle) <= 2304.0) {
                updateVehicleAudioAndVfx(mc, vehicle);
            }
        }
    }

    private static void updateVehicleAudioAndVfx(Minecraft mc, VehicleEntity vehicle) {
        VehicleClientPose pose = vehicle.clientPose();
        Vec3d pos = pose.currentPosition();
        Quat4d rot = pose.currentOrientation();
        double distSq = mc.player.distanceToSqr(pos.x(), pos.y(), pos.z());
        float attenuation = (float) Math.max(0.0, 1.0 - Math.sqrt(distSq) / 44.0);

        int[] srcs = ENTITY_SOURCES.computeIfAbsent(vehicle.getId(), id -> new int[] {
            createLoopSource(engineBuf), createLoopSource(roadBuf), createLoopSource(skidBuf)
        });

        float rpm = pose.engineRpm();
        float rpmNorm = Math.max(0.0F, Math.min(1.0F, (rpm - 800.0F) / 6400.0F));
        float speedKmh = pose.speedKmh();
        boolean active = !vehicle.getPassengers().isEmpty() || speedKmh > 0.5F || rpmNorm > 0.03F;

        AL10.alSourcef(srcs[0], AL10.AL_PITCH, 0.72F + rpmNorm * 1.48F);
        AL10.alSourcef(srcs[0], AL10.AL_GAIN, active ? (0.30F + rpmNorm * 0.52F) * attenuation : 0.0F);

        float speedNorm = Math.max(0.0F, Math.min(1.0F, speedKmh / 140.0F));
        AL10.alSourcef(srcs[1], AL10.AL_PITCH, 0.65F + speedNorm * 0.95F);
        AL10.alSourcef(srcs[1], AL10.AL_GAIN, (speedKmh > 1.5F ? Math.min(0.62F, speedNorm * 0.72F) : 0.0F) * attenuation);

        // GTA V Skid Threshold: only skid on hard high-speed cornering (> 42 km/h & high lateral G), hard braking (> 32 km/h), or handbrake
        var renderState = vehicle.buildRenderState(1.0F);
        float steerAbs = renderState.wheels().isEmpty() ? 0.0F : (float) Math.abs(renderState.wheels().get(0).steerAngleRadians());
        boolean handbrakeDown = mc.player.getVehicle() == vehicle && VehicleKeyMappings.HANDBRAKE.isDown();
        float latG = (speedKmh > 42.0F && steerAbs > 0.24F) ? ((speedKmh - 42.0F) / 55.0F) * ((steerAbs - 0.20F) * 2.4F) : 0.0F;
        float brakeSlip = (pose.braking() && speedKmh > 32.0F) ? Math.min(0.9F, (speedKmh - 28.0F) / 55.0F) : 0.0F;
        float hbSlip = (handbrakeDown && speedKmh > 12.0F) ? Math.min(1.0F, speedKmh / 35.0F) : 0.0F;
        float burnoutSlip = (rpm > 4500.0F && speedKmh < 12.0F && speedKmh > 0.8F && handbrakeDown) ? 0.85F : 0.0F;
        float slipIntensity = Math.min(1.0F, Math.max(latG, Math.max(brakeSlip, Math.max(hbSlip, burnoutSlip))));

        AL10.alSourcef(srcs[2], AL10.AL_PITCH, 0.88F + slipIntensity * 0.18F);
        AL10.alSourcef(srcs[2], AL10.AL_GAIN, (slipIntensity > 0.25F ? (slipIntensity - 0.20F) * 0.75F : 0.0F) * attenuation);

        for (int i = 0; i < renderState.wheels().size(); i++) {
            var w = renderState.wheels().get(i);
            Vec3d worldHub = pos.add(rot.rotate(w.localHubOffset()));
            float wheelSlip = (i >= 2) ? slipIntensity : (slipIntensity * 0.75F);
            SkidMarkRenderer.recordWheelSkid(i, worldHub, w.radius(), rot, (w.grounded() && wheelSlip > 0.28F) ? wheelSlip : 0.0F);
            if (w.grounded() && wheelSlip > 0.42F && mc.level.random.nextFloat() < 0.50F) {
                mc.level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    worldHub.x(), worldHub.y() - w.radius() + 0.08, worldHub.z(), 0.0, 0.03, 0.0);
            }
        }

        if (renderState.frontDent() > 0.35F && mc.level.random.nextFloat() < renderState.frontDent() * 0.45F) {
            Vec3d hoodPt = pos.add(rot.rotate(new Vec3d(0.0, 0.32, 1.75)));
            mc.level.addParticle(renderState.frontDent() > 0.7F ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE,
                hoodPt.x(), hoodPt.y(), hoodPt.z(), 0.0, 0.04, 0.0);
        }

        // GTA V Exhaust Pop & Burble: triggers immediately when releasing accelerator pedal above 2600 RPM or on high-RPM upshift
        float prevRpm = PREV_RPM.getOrDefault(vehicle.getId(), rpm);
        int prevGear = PREV_GEAR.getOrDefault(vehicle.getId(), pose.gear());
        boolean isAccelerating = (mc.player.getVehicle() == vehicle && VehicleKeyMappings.ACCELERATE.isDown()) || (rpm > prevRpm + 25.0F);
        boolean wasAcc = WAS_ACCELERATING.getOrDefault(vehicle.getId(), false);
        long now = System.currentTimeMillis();
        long lastPop = LAST_BACKFIRE_MS.getOrDefault(vehicle.getId(), 0L);

        boolean pedalLiftOff = wasAcc && !isAccelerating && (rpm > 2400.0F || speedKmh > 28.0F);
        boolean rpmDropBurble = !isAccelerating && (prevRpm - rpm) > 55.0F && rpm > 2500.0F;
        boolean gearShiftBang = pose.gear() != prevGear && pose.gear() > 1 && prevRpm > 3100.0F;

        if ((pedalLiftOff || rpmDropBurble || gearShiftBang) && (now - lastPop) > 520L) {
            LAST_BACKFIRE_MS.put(vehicle.getId(), now);
            Vec3d exhaustL = pos.add(rot.rotate(new Vec3d(0.65, -0.26, -2.95)));
            Vec3d exhaustR = pos.add(rot.rotate(new Vec3d(-0.65, -0.26, -2.95)));
            playOneShot(backfireBuf, exhaustL.x(), exhaustL.y(), exhaustL.z(), 0.95F * attenuation, 0.90F + mc.level.random.nextFloat() * 0.20F);
            mc.level.addParticle(ParticleTypes.FLAME, exhaustL.x(), exhaustL.y(), exhaustL.z(), 0.0, 0.02, 0.0);
            mc.level.addParticle(ParticleTypes.FLAME, exhaustR.x(), exhaustR.y(), exhaustR.z(), 0.0, 0.02, 0.0);
        }
        PREV_RPM.put(vehicle.getId(), rpm);
        PREV_GEAR.put(vehicle.getId(), pose.gear());
        WAS_ACCELERATING.put(vehicle.getId(), isAccelerating);
    }

    private static int createLoopSource(int buffer) {
        int src = AL10.alGenSources();
        AL10.alSourcei(src, AL10.AL_BUFFER, buffer);
        AL10.alSourcei(src, AL10.AL_LOOPING, AL10.AL_TRUE);
        AL10.alSourcei(src, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        AL10.alSourcef(src, AL10.AL_GAIN, 0.0F);
        AL10.alSourcePlay(src);
        return src;
    }

    private static void stopAll() {
        for (int[] srcs : ENTITY_SOURCES.values()) {
            for (int s : srcs) { AL10.alSourceStop(s); AL10.alDeleteSources(s); }
        }
        ENTITY_SOURCES.clear();
    }

    private static int createBuffer(short[] pcm) {
        int buf = AL10.alGenBuffers();
        ByteBuffer data = BufferUtils.createByteBuffer(pcm.length * 2);
        data.asShortBuffer().put(pcm);
        AL10.alBufferData(buf, AL10.AL_FORMAT_MONO16, data, VehicleSoundSynthesizer.SAMPLE_RATE);
        return buf;
    }
}
