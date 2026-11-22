package com.realisticvehicles.render.glb;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Scales and centers a parsed GLB model, separating 4 wheel hubs (including tyre_*)
 * and 4 hinged panels (left door, right door, hood, trunk).
 */
public final class GlbModelAssembler {
    private GlbModelAssembler() {
    }

    public static GlbModel assemble(List<GlbMeshPrimitive> raw, List<GlbMaterial> materials) {
        float scale = 1.48F;
        for (GlbMeshPrimitive p : raw) {
            float[] pos = p.positions();
            for (int i = 0; i < pos.length; i++) {
                pos[i] *= scale;
            }
        }
        float[] bounds = computeBounds(raw);
        float cx = (bounds[0] + bounds[3]) * 0.5F;
        float cz = (bounds[2] + bounds[5]) * 0.5F;
        float shiftY = -bounds[1] - 0.635F;

        List<GlbMeshPrimitive> chassis = new ArrayList<>();
        List<GlbMeshPrimitive> fl = new ArrayList<>(), fr = new ArrayList<>();
        List<GlbMeshPrimitive> rl = new ArrayList<>(), rr = new ArrayList<>();
        List<GlbMeshPrimitive> doorL = new ArrayList<>(), doorR = new ArrayList<>();
        List<GlbMeshPrimitive> hood = new ArrayList<>(), trunk = new ArrayList<>();

        for (GlbMeshPrimitive p : raw) {
            shiftPositions(p.positions(), -cx, shiftY, -cz);
            String lower = p.nodeName().toLowerCase(Locale.ROOT);
            if (isWheelPart(lower)) {
                classifyWheel(p, lower, fl, fr, rl, rr);
            } else if (isHoodPart(lower)) {
                hood.add(p);
            } else if (isTrunkPart(lower)) {
                trunk.add(p);
            } else if (isDoorPart(lower)) {
                if (computeCentroid(p.positions())[0] >= 0.0F) doorL.add(p);
                else doorR.add(p);
            } else {
                chassis.add(p);
            }
        }

        return new GlbModel(
            chassis,
            centerWheel(fl), centerWheel(fr), centerWheel(rl), centerWheel(rr),
            hingePart(doorL, true, false, true),
            hingePart(doorR, false, false, true),
            hingePart(hood, false, true, false),
            hingePart(trunk, false, true, true),
            materials,
            bounds[0] - cx, bounds[1] + shiftY, bounds[2] - cz,
            bounds[3] - cx, bounds[4] + shiftY, bounds[5] - cz
        );
    }

    private static boolean isWheelPart(String lower) {
        return lower.startsWith("rim_")
            || lower.startsWith("tire")
            || lower.startsWith("tyre_")
            || lower.startsWith("wheel")
            || lower.contains("rotor");
    }

    private static boolean isHoodPart(String lower) {
        return lower.startsWith("hood_") || lower.startsWith("hoodliner");
    }

    private static boolean isTrunkPart(String lower) {
        return lower.startsWith("trunk_")
            || lower.startsWith("trunkliner")
            || lower.startsWith("wing_")
            || lower.startsWith("tailightc")
            || lower.startsWith("taillightc")
            || lower.startsWith("glassctl")
            || lower.startsWith("plateeu");
    }

    private static boolean isDoorPart(String lower) {
        return lower.startsWith("doorcard")
            || lower.startsWith("doorjamb")
            || lower.startsWith("doorhandle")
            || lower.startsWith("speakerdoor")
            || lower.startsWith("mirrorbase")
            || lower.startsWith("wingmirror")
            || lower.startsWith("glasswingmirror")
            || lower.startsWith("glasslf")
            || lower.startsWith("glassrf");
    }

    private static void classifyWheel(
        GlbMeshPrimitive p, String lower,
        List<GlbMeshPrimitive> fl, List<GlbMeshPrimitive> fr,
        List<GlbMeshPrimitive> rl, List<GlbMeshPrimitive> rr
    ) {
        if (lower.contains("lf")) fl.add(p);
        else if (lower.contains("rf")) fr.add(p);
        else if (lower.contains("lr")) rl.add(p);
        else if (lower.contains("rr")) rr.add(p);
        else {
            float[] c = computeCentroid(p.positions());
            if (c[0] >= 0 && c[2] >= 0) fl.add(p);
            else if (c[0] < 0 && c[2] >= 0) fr.add(p);
            else if (c[0] >= 0) rl.add(p);
            else rr.add(p);
        }
    }

    private static GlbWheelPart centerWheel(List<GlbMeshPrimitive> parts) {
        if (parts.isEmpty()) return new GlbWheelPart(0, 0, 0, List.of());
        float[] b = computeBounds(parts);
        float hx = (b[0] + b[3]) * 0.5F, hy = (b[1] + b[4]) * 0.5F, hz = (b[2] + b[5]) * 0.5F;
        for (GlbMeshPrimitive p : parts) shiftPositions(p.positions(), -hx, -hy, -hz);
        return new GlbWheelPart(hx, hy, hz, parts);
    }

    private static GlbHingedPart hingePart(
        List<GlbMeshPrimitive> parts, boolean useMaxX, boolean centerX, boolean useMaxZ
    ) {
        if (parts.isEmpty()) return GlbHingedPart.EMPTY;
        float[] b = computeBounds(parts);
        float px = centerX ? (b[0] + b[3]) * 0.5F : (useMaxX ? b[3] : b[0]);
        float py = (b[1] + b[4]) * 0.5F;
        float pz = useMaxZ ? b[5] : b[2];
        for (GlbMeshPrimitive p : parts) shiftPositions(p.positions(), -px, -py, -pz);
        return new GlbHingedPart(px, py, pz, parts);
    }

    private static float[] computeBounds(List<GlbMeshPrimitive> prims) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (GlbMeshPrimitive p : prims) {
            float[] pos = p.positions();
            for (int i = 0; i < pos.length; i += 3) {
                minX = Math.min(minX, pos[i]);
                minY = Math.min(minY, pos[i + 1]);
                minZ = Math.min(minZ, pos[i + 2]);
                maxX = Math.max(maxX, pos[i]);
                maxY = Math.max(maxY, pos[i + 1]);
                maxZ = Math.max(maxZ, pos[i + 2]);
            }
        }
        return new float[] { minX, minY, minZ, maxX, maxY, maxZ };
    }

    private static void shiftPositions(float[] pos, float dx, float dy, float dz) {
        for (int i = 0; i < pos.length; i += 3) {
            pos[i] += dx;
            pos[i + 1] += dy;
            pos[i + 2] += dz;
        }
    }

    private static float[] computeCentroid(float[] pos) {
        double sx = 0, sy = 0, sz = 0;
        int n = pos.length / 3;
        for (int i = 0; i < pos.length; i += 3) {
            sx += pos[i];
            sy += pos[i + 1];
            sz += pos[i + 2];
        }
        return n > 0 ? new float[] { (float) (sx / n), (float) (sy / n), (float) (sz / n) } : new float[3];
    }
}
