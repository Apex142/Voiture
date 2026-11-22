package com.realisticvehicles.render.api;

import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import java.util.List;

/**
 * Complete rendering snapshot of a vehicle consumed by provisional, GLB, or Vulkan backends.
 * lightsMask bits: 1=LowBeam, 2=HighBeam, 4=LeftTurn, 8=RightTurn, 16=Hazards.
 */
public record VehicleRenderState(
    String presetId,
    Vec3d position,
    Quat4d orientation,
    double chassisWidth,
    double chassisHeight,
    double chassisLength,
    boolean braking,
    boolean reversing,
    byte lightsMask,
    float leftDoorOpen,
    float rightDoorOpen,
    float hoodOpen,
    float trunkOpen,
    float speedKmh,
    float engineRpm,
    float frontDent,
    float rearDent,
    float leftDent,
    float rightDent,
    List<WheelRenderState> wheels
) {
    public boolean lowBeamsOn() {
        return (lightsMask & 1) != 0;
    }

    public boolean highBeamsOn() {
        return (lightsMask & 2) != 0;
    }

    public boolean leftIndicatorOn() {
        return (lightsMask & 4) != 0 || (lightsMask & 16) != 0;
    }

    public boolean rightIndicatorOn() {
        return (lightsMask & 8) != 0 || (lightsMask & 16) != 0;
    }
}
