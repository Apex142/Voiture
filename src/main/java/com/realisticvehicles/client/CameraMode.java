package com.realisticvehicles.client;

import com.realisticvehicles.core.math.Vec3d;

/**
 * Available driving camera presets with local anchor and look offsets.
 */
public enum CameraMode {
    THIRD_PERSON("3rd Person", new Vec3d(0.0, 2.05, -6.0), 10.0F, false),
    INTERIOR_DRIVER("Cockpit", new Vec3d(0.54, 0.85, -0.54), 3.0F, false),
    HOOD("Hood", new Vec3d(0.0, 0.78, 0.90), 3.0F, false),
    REAR_CHASE("Rear Bumper", new Vec3d(0.0, 0.65, -2.85), 2.0F, false),
    REVERSE_CAM("Reverse Cam", new Vec3d(0.0, 0.72, -2.65), 8.0F, true);

    private final String label;
    private final Vec3d localOffset;
    private final float pitchDeg;
    private final boolean lookBackward;

    CameraMode(String label, Vec3d localOffset, float pitchDeg, boolean lookBackward) {
        this.label = label;
        this.localOffset = localOffset;
        this.pitchDeg = pitchDeg;
        this.lookBackward = lookBackward;
    }

    public String label() {
        return label;
    }

    public Vec3d localOffset() {
        return localOffset;
    }

    public float pitchDeg() {
        return pitchDeg;
    }

    public boolean lookBackward() {
        return lookBackward;
    }

    public CameraMode next() {
        CameraMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
