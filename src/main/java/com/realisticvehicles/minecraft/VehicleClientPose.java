package com.realisticvehicles.minecraft;

import com.realisticvehicles.core.math.MathUtil;
import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.simulation.VehicleConfig;
import com.realisticvehicles.physics.suspension.WheelConfig;
import com.realisticvehicles.render.api.VehicleRenderState;
import com.realisticvehicles.render.api.WheelRenderState;
import java.util.ArrayList;
import java.util.List;

/**
 * Triple-buffered client pose + smooth opening panel animation (doors, hood, trunk) and light states.
 */
public final class VehicleClientPose {
    private Vec3d prevPos, currPos, targetPos;
    private Quat4d prevRot, currRot, targetRot;
    private final double[] prevComp = new double[4], currComp = new double[4], targetComp = new double[4];
    private final double[] prevSteer = new double[4], currSteer = new double[4], targetSteer = new double[4];
    private final double[] prevSpin = new double[4], currSpin = new double[4], targetSpin = new double[4];
    private final boolean[] grounded = new boolean[4];
    private boolean braking, reversing, initialized;
    private float speedKmh, engineRpm = 850.0F;
    private int gear = 1;
    private byte lightsMask;
    private float prevDoorL, currDoorL, targetDoorL;
    private float prevDoorR, currDoorR, targetDoorR;
    private float prevHood, currHood, targetHood;
    private float prevTrunk, currTrunk, targetTrunk;
    private float frontDent, rearDent, leftDent, rightDent;
    private float fuelLiters = 70.0F;
    private boolean misfueled;
    private float entrySeatProgress = 1.0F;

    public VehicleClientPose(Vec3d startPos, Quat4d startRot) {
        this.prevPos = this.currPos = this.targetPos = startPos;
        this.prevRot = this.currRot = this.targetRot = startRot;
    }

    public void stepClientTick() {
        this.prevPos = this.currPos;
        this.prevRot = this.currRot;
        this.currPos = this.targetPos;
        this.currRot = this.targetRot;
        for (int i = 0; i < 4; i++) {
            prevComp[i] = currComp[i];
            prevSteer[i] = currSteer[i];
            prevSpin[i] = currSpin[i];
            currComp[i] = targetComp[i];
            currSteer[i] = targetSteer[i];
            currSpin[i] = targetSpin[i];
        }
        prevDoorL = currDoorL;
        prevDoorR = currDoorR;
        prevHood = currHood;
        prevTrunk = currTrunk;
        currDoorL = (float) MathUtil.moveTowards(currDoorL, targetDoorL, 0.14);
        currDoorR = (float) MathUtil.moveTowards(currDoorR, targetDoorR, 0.14);
        currHood = (float) MathUtil.moveTowards(currHood, targetHood, 0.12);
        currTrunk = (float) MathUtil.moveTowards(currTrunk, targetTrunk, 0.12);
        entrySeatProgress = (float) MathUtil.moveTowards(entrySeatProgress, 1.0, 0.045);
    }

    public void beginEntryAnimation() {
        this.entrySeatProgress = 0.0F;
    }

    public float entrySeatProgress() { return entrySeatProgress; }
    public float fuelLiters() { return fuelLiters; }
    public boolean isMisfueled() { return misfueled; }

    public void applyServerState(VehicleStateSyncPayload p) {
        Vec3d pos = new Vec3d(p.x(), p.y(), p.z());
        Quat4d normalized = new Quat4d(p.qx(), p.qy(), p.qz(), p.qw()).normalize();
        this.targetPos = pos;
        this.targetRot = normalized;
        for (int i = 0; i < 4; i++) {
            targetComp[i] = p.compressions()[i];
            targetSteer[i] = p.steerAngles()[i];
            targetSpin[i] = p.spinAngles()[i];
            grounded[i] = ((p.groundedMask() >> i) & 1) != 0;
        }
        if (!initialized) {
            this.prevPos = this.currPos = pos;
            this.prevRot = this.currRot = normalized;
            System.arraycopy(targetComp, 0, prevComp, 0, 4);
            System.arraycopy(targetComp, 0, currComp, 0, 4);
            System.arraycopy(targetSteer, 0, prevSteer, 0, 4);
            System.arraycopy(targetSteer, 0, currSteer, 0, 4);
            System.arraycopy(targetSpin, 0, prevSpin, 0, 4);
            System.arraycopy(targetSpin, 0, currSpin, 0, 4);
            this.initialized = true;
        }
        this.braking = p.braking();
        this.reversing = p.reversing();
        this.speedKmh = p.speedKmh();
        this.engineRpm = p.engineRpm();
        this.gear = p.gear();
        this.lightsMask = p.lightsMask();
        this.targetDoorL = p.leftDoorOpen();
        this.targetDoorR = p.rightDoorOpen();
        this.targetHood = p.hoodOpen();
        this.targetTrunk = p.trunkOpen();
        this.frontDent = p.frontDent();
        this.rearDent = p.rearDent();
        this.leftDent = p.leftDent();
        this.rightDent = p.rightDent();
        this.fuelLiters = p.fuelLiters();
        this.misfueled = p.misfueled();
    }

    public float speedKmh() { return speedKmh; }
    public float engineRpm() { return engineRpm; }
    public int gear() { return gear; }
    public boolean braking() { return braking; }
    public boolean reversing() { return reversing; }
    public byte lightsMask() { return lightsMask; }
    public Vec3d currentPosition() { return currPos; }
    public Quat4d currentOrientation() { return currRot; }

    public VehicleRenderState interpolate(VehicleConfig config, float partialTick) {
        double t = MathUtil.clamp(partialTick, 0.0, 1.0);
        Vec3d pos = prevPos.lerp(currPos, t);
        Quat4d rot = prevRot.slerp(currRot, t);

        List<WheelRenderState> wheels = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            WheelConfig wc = config.wheels().get(i);
            double comp = MathUtil.lerp(prevComp[i], currComp[i], t);
            double steer = MathUtil.lerp(prevSteer[i], currSteer[i], t);
            double spin = lerpAngle(prevSpin[i], currSpin[i], t);
            Vec3d hubOffset = wc.mountPoint().add(new Vec3d(0.0, -(wc.restLength() - comp), 0.0));
            wheels.add(new WheelRenderState(hubOffset, wc.radius(), wc.width(), steer, spin, grounded[i]));
        }
        return new VehicleRenderState(
            config.presetId(), pos, rot,
            config.body().width(), config.body().height(), config.body().length(),
            braking, reversing, lightsMask,
            (float) MathUtil.lerp(prevDoorL, currDoorL, t),
            (float) MathUtil.lerp(prevDoorR, currDoorR, t),
            (float) MathUtil.lerp(prevHood, currHood, t),
            (float) MathUtil.lerp(prevTrunk, currTrunk, t),
            speedKmh,
            engineRpm,
            frontDent, rearDent, leftDent, rightDent,
            wheels
        );
    }

    private double lerpAngle(double a, double b, double t) {
        double diff = ((b - a + Math.PI * 3.0) % (Math.PI * 2.0)) - Math.PI;
        return a + diff * t;
    }
}
