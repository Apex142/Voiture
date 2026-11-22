package com.realisticvehicles.minecraft;

import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyState;
import com.realisticvehicles.physics.simulation.VehicleConfig;
import com.realisticvehicles.physics.simulation.VehiclePresetFactory;
import com.realisticvehicles.physics.simulation.VehicleSimulation;
import net.minecraft.nbt.CompoundTag;

/**
 * Repository responsible for persisting and restoring VehicleSimulation state in NBT.
 */
public final class VehicleStateRepository {
    private static final String TAG_PRESET = "PresetId";
    private static final String TAG_POS_X = "PosX";
    private static final String TAG_POS_Y = "PosY";
    private static final String TAG_POS_Z = "PosZ";
    private static final String TAG_VEL_X = "VelX";
    private static final String TAG_VEL_Y = "VelY";
    private static final String TAG_VEL_Z = "VelZ";
    private static final String TAG_QX = "QuatX";
    private static final String TAG_QY = "QuatY";
    private static final String TAG_QZ = "QuatZ";
    private static final String TAG_QW = "QuatW";
    private static final String TAG_ANG_X = "AngX";
    private static final String TAG_ANG_Y = "AngY";
    private static final String TAG_ANG_Z = "AngZ";
    private static final String TAG_RPM = "EngineRpm";
    private static final String TAG_GEAR = "Gear";

    public void save(VehicleSimulation simulation, CompoundTag tag) {
        RigidBodyState body = simulation.body();
        tag.putString(TAG_PRESET, simulation.config().presetId());
        tag.putDouble(TAG_POS_X, body.position().x());
        tag.putDouble(TAG_POS_Y, body.position().y());
        tag.putDouble(TAG_POS_Z, body.position().z());
        tag.putDouble(TAG_VEL_X, body.linearVelocity().x());
        tag.putDouble(TAG_VEL_Y, body.linearVelocity().y());
        tag.putDouble(TAG_VEL_Z, body.linearVelocity().z());
        tag.putDouble(TAG_QX, body.orientation().x());
        tag.putDouble(TAG_QY, body.orientation().y());
        tag.putDouble(TAG_QZ, body.orientation().z());
        tag.putDouble(TAG_QW, body.orientation().w());
        tag.putDouble(TAG_ANG_X, body.angularVelocity().x());
        tag.putDouble(TAG_ANG_Y, body.angularVelocity().y());
        tag.putDouble(TAG_ANG_Z, body.angularVelocity().z());
        tag.putDouble(TAG_RPM, simulation.engine().currentRpm());
        tag.putInt(TAG_GEAR, simulation.transmission().currentGear());
    }

    public VehicleSimulation load(CompoundTag tag, Vec3d fallbackPos) {
        String presetId = tag.contains(TAG_PRESET) ? tag.getString(TAG_PRESET) : VehiclePresetFactory.PRESET_SEDAN;
        VehicleConfig config = VehiclePresetFactory.create(presetId);

        Vec3d pos = tag.contains(TAG_POS_X)
            ? new Vec3d(tag.getDouble(TAG_POS_X), tag.getDouble(TAG_POS_Y), tag.getDouble(TAG_POS_Z))
            : fallbackPos;
        Quat4d rot = tag.contains(TAG_QW)
            ? new Quat4d(tag.getDouble(TAG_QX), tag.getDouble(TAG_QY), tag.getDouble(TAG_QZ), tag.getDouble(TAG_QW)).normalize()
            : Quat4d.IDENTITY;

        VehicleSimulation simulation = new VehicleSimulation(config, pos, rot);
        if (tag.contains(TAG_VEL_X)) {
            simulation.body().setLinearVelocity(new Vec3d(
                tag.getDouble(TAG_VEL_X), tag.getDouble(TAG_VEL_Y), tag.getDouble(TAG_VEL_Z)
            ));
        }
        if (tag.contains(TAG_ANG_X)) {
            simulation.body().setAngularVelocity(new Vec3d(
                tag.getDouble(TAG_ANG_X), tag.getDouble(TAG_ANG_Y), tag.getDouble(TAG_ANG_Z)
            ));
        }
        if (tag.contains(TAG_RPM)) {
            simulation.engine().setCurrentRpm(tag.getDouble(TAG_RPM));
        }
        if (tag.contains(TAG_GEAR)) {
            simulation.transmission().setCurrentGear(tag.getInt(TAG_GEAR));
        }
        return simulation;
    }
}
