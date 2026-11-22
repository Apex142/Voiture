package com.realisticvehicles.client;

import com.realisticvehicles.render.glb.GlbMaterial;
import java.util.Locale;

/**
 * Resolves PBR material colors and glass classification for GLB rendering.
 */
public final class GlbMaterialPalette {

    private GlbMaterialPalette() {
    }

    public static boolean isGlass(GlbMaterial mat) {
        return mat.name().toLowerCase(Locale.ROOT).contains("glass");
    }

    public static int[] resolveRgba(GlbMaterial mat, boolean isGlass) {
        if (isGlass) {
            return new int[] { 85, 130, 175, 145 };
        }
        int r = clampColor(mat.r());
        int g = clampColor(mat.g());
        int b = clampColor(mat.b());
        int a = clampColor(mat.a());
        if (r > 240 && g > 240 && b > 240) {
            String n = mat.name().toLowerCase(Locale.ROOT);
            if (n.contains("carpaint")) return new int[] { 205, 212, 222, a };
            if (n.contains("tiretread")) {
                return new int[] { 24, 26, 30, a };
            }
            if (n.contains("tiresidewall") || n.contains("rubber") || n.contains("tire")) {
                return new int[] { 38, 41, 46, a };
            }
            if (n.contains("matteblack") || n.contains("undercarriage")) {
                return new int[] { 32, 34, 38, a };
            }
            if (n.contains("plastic") || n.contains("grille") || n.contains("carbon") || n.contains("leather")) {
                return new int[] { 48, 52, 58, a };
            }
            if (n.contains("rim") || n.contains("wheel") || n.contains("chrome") || n.contains("aluminum") || n.contains("metal")) {
                return new int[] { 228, 234, 242, a };
            }
            if (n.contains("rotor") || n.contains("caliper")) {
                return new int[] { 185, 45, 40, a };
            }
            if (n.contains("brake") || n.contains("tail")) return new int[] { 95, 16, 18, a };
            if (n.contains("emitter")) return new int[] { 70, 74, 82, a };
            if (n.contains("backfacing")) return new int[] { 38, 42, 48, a };
        }
        return new int[] { r, g, b, a };
    }

    private static int clampColor(float c) {
        return Math.max(0, Math.min(255, Math.round(c * 255.0F)));
    }
}
