package com.realisticvehicles.minecraft;

import com.realisticvehicles.client.ClientPacketHandlers;
import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.simulation.VehicleSimulation;
import com.realisticvehicles.physics.simulation.VehicleTelemetry;
import com.realisticvehicles.physics.suspension.WheelState;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-to-client authoritative vehicle pose, wheel, opening panels, lights, and telemetry packet.
 */
public record VehicleStateSyncPayload(
    int entityId,
    double x,
    double y,
    double z,
    float qx,
    float qy,
    float qz,
    float qw,
    float[] compressions,
    float[] steerAngles,
    float[] spinAngles,
    byte groundedMask,
    boolean braking,
    boolean reversing,
    float speedKmh,
    float engineRpm,
    byte gear,
    byte lightsMask,
    float leftDoorOpen,
    float rightDoorOpen,
    float hoodOpen,
    float trunkOpen,
    float frontDent,
    float rearDent,
    float leftDent,
    float rightDent,
    float fuelLiters,
    boolean misfueled
) implements CustomPacketPayload {

    public static final Type<VehicleStateSyncPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(RealisticVehiclesMod.MOD_ID, "vehicle_state_sync")
    );

    public static final StreamCodec<ByteBuf, VehicleStateSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public VehicleStateSyncPayload decode(ByteBuf buf) {
            int id = buf.readInt();
            double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
            float qx = buf.readFloat(), qy = buf.readFloat(), qz = buf.readFloat(), qw = buf.readFloat();
            float[] comp = new float[4], steer = new float[4], spin = new float[4];
            for (int i = 0; i < 4; i++) {
                comp[i] = buf.readFloat();
                steer[i] = buf.readFloat();
                spin[i] = buf.readFloat();
            }
            byte mask = buf.readByte();
            boolean braking = buf.readBoolean(), reversing = buf.readBoolean();
            float speedKmh = buf.readFloat(), engineRpm = buf.readFloat();
            byte gear = buf.readByte(), lightsMask = buf.readByte();
            float dl = buf.readFloat(), dr = buf.readFloat(), hd = buf.readFloat(), tr = buf.readFloat();
            float fd = buf.readFloat(), rd = buf.readFloat(), ld = buf.readFloat(), rgd = buf.readFloat();
            float fuel = buf.readFloat();
            boolean misfuel = buf.readBoolean();
            return new VehicleStateSyncPayload(
                id, x, y, z, qx, qy, qz, qw, comp, steer, spin, mask,
                braking, reversing, speedKmh, engineRpm, gear, lightsMask, dl, dr, hd, tr, fd, rd, ld, rgd, fuel, misfuel
            );
        }

        @Override
        public void encode(ByteBuf buf, VehicleStateSyncPayload p) {
            buf.writeInt(p.entityId);
            buf.writeDouble(p.x);
            buf.writeDouble(p.y);
            buf.writeDouble(p.z);
            buf.writeFloat(p.qx);
            buf.writeFloat(p.qy);
            buf.writeFloat(p.qz);
            buf.writeFloat(p.qw);
            for (int i = 0; i < 4; i++) {
                buf.writeFloat(p.compressions[i]);
                buf.writeFloat(p.steerAngles[i]);
                buf.writeFloat(p.spinAngles[i]);
            }
            buf.writeByte(p.groundedMask);
            buf.writeBoolean(p.braking);
            buf.writeBoolean(p.reversing);
            buf.writeFloat(p.speedKmh);
            buf.writeFloat(p.engineRpm);
            buf.writeByte(p.gear);
            buf.writeByte(p.lightsMask);
            buf.writeFloat(p.leftDoorOpen);
            buf.writeFloat(p.rightDoorOpen);
            buf.writeFloat(p.hoodOpen);
            buf.writeFloat(p.trunkOpen);
            buf.writeFloat(p.frontDent);
            buf.writeFloat(p.rearDent);
            buf.writeFloat(p.leftDent);
            buf.writeFloat(p.rightDent);
            buf.writeFloat(p.fuelLiters);
            buf.writeBoolean(p.misfueled);
        }
    };

    public static VehicleStateSyncPayload fromEntity(VehicleEntity entity) {
        VehicleSimulation sim = entity.simulation();
        Vec3d pos = sim.body().position();
        Quat4d rot = sim.body().orientation();
        List<WheelState> wheels = sim.wheels();
        float[] comp = new float[4], steer = new float[4], spin = new float[4];
        byte mask = 0;
        for (int i = 0; i < 4; i++) {
            WheelState w = wheels.get(i);
            comp[i] = (float) w.compression();
            steer[i] = (float) w.steerAngle();
            spin[i] = (float) w.spinAngle();
            if (w.grounded()) mask |= (byte) (1 << i);
        }
        VehicleTelemetry t = sim.telemetry();
        return new VehicleStateSyncPayload(
            entity.getId(), pos.x(), pos.y(), pos.z(),
            (float) rot.x(), (float) rot.y(), (float) rot.z(), (float) rot.w(),
            comp, steer, spin, mask, t.braking(), t.reversing(),
            (float) t.speedKmh(), (float) t.engineRpm(), (byte) t.gear(),
            entity.lightsMask(),
            entity.leftDoorTarget(), entity.rightDoorTarget(), entity.hoodTarget(), entity.trunkTarget(),
            entity.frontDent, entity.rearDent, entity.leftDent, entity.rightDent,
            (float) entity.fuelSystem().currentLiters(), entity.fuelSystem().isMisfueled()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(VehicleStateSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandlers.handleVehicleStateSync(payload));
    }
}
