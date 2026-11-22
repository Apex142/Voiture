package com.realisticvehicles.audio;

import com.realisticvehicles.core.math.MathUtil;

/**
 * Computes continuous engine pitch/gain and tire skid audio parameters from simulation telemetry.
 */
public final class DynamicEngineAudioController {
    private float smoothedPitch = 0.6F;
    private float smoothedGain = 0.2F;

    public record AudioParams(float enginePitch, float engineGain, float tireSkidGain) {
    }

    public AudioParams compute(
        boolean engineRunning,
        double rpm,
        double idleRpm,
        double redlineRpm,
        double throttle,
        double maxWheelSlip,
        double dt
    ) {
        if (!engineRunning) {
            smoothedGain = (float) MathUtil.moveTowards(smoothedGain, 0.0, dt * 2.0);
            return new AudioParams(0.5F, smoothedGain, 0.0F);
        }
        double rpmNorm = MathUtil.clamp((rpm - idleRpm) / Math.max(500.0, redlineRpm - idleRpm), 0.0, 1.0);
        float targetPitch = (float) (0.58 + rpmNorm * 1.55);
        float targetGain = (float) (0.22 + rpmNorm * 0.45 + MathUtil.clamp(throttle, 0.0, 1.0) * 0.33);

        double alpha = MathUtil.clamp(dt * 14.0, 0.0, 1.0);
        smoothedPitch = (float) MathUtil.lerp(smoothedPitch, targetPitch, alpha);
        smoothedGain = (float) MathUtil.lerp(smoothedGain, targetGain, alpha);

        float skidGain = (float) MathUtil.clamp((Math.abs(maxWheelSlip) - 0.18) * 1.4, 0.0, 1.0);
        return new AudioParams(smoothedPitch, smoothedGain, skidGain);
    }
}
