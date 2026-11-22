package com.realisticvehicles.client;

import java.util.Random;

/**
 * Pure PCM waveform synthesizer for OpenAL automotive sound effects:
 * V8 AMG rumble, road roll, multi-band frictional tire skid, multi-pop AMG V8 exhaust burble, lock chirp, horn.
 */
public final class VehicleSoundSynthesizer {
    public static final int SAMPLE_RATE = 22050;

    private VehicleSoundSynthesizer() {
    }

    public static short[] synthesizeV8Loop() {
        short[] out = new short[SAMPLE_RATE];
        for (int i = 0; i < SAMPLE_RATE; i++) {
            double p = 2.0 * Math.PI * 44.0 * i / SAMPLE_RATE;
            double w = 0.36 * Math.sin(p) + 0.24 * Math.sin(0.5 * p + 0.4) + 0.22 * Math.sin(1.5 * p + 1.1)
                + 0.18 * Math.sin(2.0 * p) + 0.12 * Math.sin(3.0 * p + 0.7) + 0.08 * Math.sin(4.0 * p);
            out[i] = (short) (Math.tanh(w * 1.9) * 21000);
        }
        return out;
    }

    public static short[] synthesizeRoadRollingLoop() {
        short[] out = new short[SAMPLE_RATE];
        Random rng = new Random(42L);
        double lp1 = 0, lp2 = 0;
        for (int i = 0; i < SAMPLE_RATE; i++) {
            lp1 += 0.08 * ((rng.nextDouble() * 2.0 - 1.0) - lp1);
            lp2 += 0.05 * (lp1 - lp2);
            double hum = 0.22 * Math.sin(2.0 * Math.PI * 66.0 * i / SAMPLE_RATE);
            out[i] = (short) Math.max(-28000, Math.min(28000, (int) ((lp2 * 1.6 + hum) * 18000)));
        }
        return out;
    }

    /**
     * Synthesizes a realistic asphalt rubber tire skid / scrub loop using resonant bandpass-filtered
     * friction noise (stick-slip asphalt grain at 720 Hz + 1480 Hz + 2350 Hz) instead of pure sine tones.
     */
    public static short[] synthesizeTireScreechLoop() {
        short[] out = new short[SAMPLE_RATE];
        Random rng = new Random(77L);
        double bp1 = 0, bp1Vel = 0;
        double bp2 = 0, bp2Vel = 0;
        double lpScrub = 0;
        for (int i = 0; i < SAMPLE_RATE; i++) {
            double t = (double) i / SAMPLE_RATE;
            double noise = rng.nextDouble() * 2.0 - 1.0;
            // Stick-slip amplitude modulation (asphalt texture jitter ~28 Hz + 63 Hz)
            double stickSlip = 0.72 + 0.28 * Math.sin(2.0 * Math.PI * 28.0 * t + Math.sin(2.0 * Math.PI * 7.0 * t));

            // Resonator 1 (~1250 Hz rubber tread resonance)
            double f1 = 2.0 * Math.sin(Math.PI * (1240.0 + 55.0 * Math.sin(2.0 * Math.PI * 11.0 * t)) / SAMPLE_RATE);
            bp1Vel += f1 * (noise - bp1 - 0.14 * bp1Vel);
            bp1 += f1 * bp1Vel;

            // Resonator 2 (~2150 Hz harmonic screech)
            double f2 = 2.0 * Math.sin(Math.PI * (2120.0 + 80.0 * Math.cos(2.0 * Math.PI * 15.0 * t)) / SAMPLE_RATE);
            bp2Vel += f2 * (noise - bp2 - 0.18 * bp2Vel);
            bp2 += f2 * bp2Vel;

            // Low-mid rubber asphalt tearing scrub (250-550 Hz)
            lpScrub += 0.12 * (noise - lpScrub);

            double combined = stickSlip * (0.55 * bp1 + 0.32 * bp2 + 0.45 * lpScrub);
            out[i] = (short) (Math.tanh(combined * 1.45) * 19500);
        }
        return out;
    }

    /**
     * Synthesizes a realistic AMG V8 exhaust overrun crackle / pop-and-bang sequence (4 staggered
     * low-end resonant exhaust manifold thumps + crisp tailpipe crackles over 0.48s).
     */
    public static short[] synthesizeExhaustBackfire() {
        int len = (int) (SAMPLE_RATE * 0.48);
        short[] out = new short[len];
        Random rng = new Random(99L);
        double[] popTimes = { 0.005, 0.115, 0.245, 0.355 };
        double[] popGains = { 1.00, 0.78, 0.88, 0.58 };
        double[] popFreqs = { 78.0, 92.0, 68.0, 85.0 };
        double lpNoise = 0;

        for (int i = 0; i < len; i++) {
            double t = (double) i / SAMPLE_RATE;
            double white = rng.nextDouble() * 2.0 - 1.0;
            lpNoise += 0.22 * (white - lpNoise);
            double sample = 0.0;

            for (int p = 0; p < popTimes.length; p++) {
                double dt = t - popTimes[p];
                if (dt >= 0.0 && dt < 0.12) {
                    // Fast pressure shockwave attack + deep muffler resonance decay
                    double shockEnv = Math.exp(-dt * 75.0);
                    double bodyEnv = Math.exp(-dt * 28.0);
                    double freq = popFreqs[p] * (1.0 - 0.35 * Math.min(1.0, dt * 12.0));
                    double bassThump = Math.sin(2.0 * Math.PI * freq * dt) + 0.45 * Math.sin(2.0 * Math.PI * freq * 1.8 * dt);
                    double crack = lpNoise * shockEnv * 1.35;
                    sample += popGains[p] * (bodyEnv * bassThump * 0.85 + crack);
                }
            }
            out[i] = (short) (Math.tanh(sample * 2.2) * 26500);
        }
        return out;
    }

    public static short[] synthesizeChirp(int beepCount) {
        int len = (int) (SAMPLE_RATE * 0.42);
        short[] out = new short[len];
        for (int i = 0; i < len; i++) {
            double t = (double) i / SAMPLE_RATE;
            boolean b1 = t >= 0.01 && t <= 0.085, b2 = beepCount == 2 && t >= 0.135 && t <= 0.210;
            double v = (b1 || b2) ? Math.sin(Math.min(1.0, ((b1 ? t - 0.01 : t - 0.135) / 0.075)) * Math.PI)
                * (0.65 * Math.sin(2.0 * Math.PI * 2450.0 * t) + 0.35 * Math.sin(2.0 * Math.PI * 2920.0 * t)) : 0.0;
            if (t < 0.035) v += Math.exp(-t * 110.0) * 0.85 * Math.sin(2.0 * Math.PI * 165.0 * t);
            out[i] = (short) Math.max(-30000, Math.min(30000, (int) (v * 23000)));
        }
        return out;
    }

    public static short[] synthesizeDualToneHorn() {
        int len = (int) (SAMPLE_RATE * 0.38);
        short[] out = new short[len];
        for (int i = 0; i < len; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = Math.min(1.0, t * 40.0) * Math.min(1.0, (0.38 - t) * 28.0);
            double w = 0.45 * Math.sin(2.0 * Math.PI * 410.0 * t) + 0.45 * Math.sin(2.0 * Math.PI * 510.0 * t)
                + 0.22 * Math.sin(2.0 * Math.PI * 820.0 * t) + 0.22 * Math.sin(2.0 * Math.PI * 1020.0 * t);
            out[i] = (short) (Math.tanh(w * 1.8) * env * 24000);
        }
        return out;
    }
}
