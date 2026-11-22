package com.realisticvehicles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Low-level immediate geometry helper for the provisional Phase 1 vehicle renderer.
 */
public final class ProvisionalMeshBuilder {

    private ProvisionalMeshBuilder() {
    }

    public static void drawBox(
        PoseStack.Pose pose,
        VertexConsumer vc,
        float minX, float minY, float minZ,
        float maxX, float maxY, float maxZ,
        int r, int g, int b, int a,
        int light
    ) {
        // +Y top
        quad(pose, vc, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0, 1, 0, r, g, b, a, light);
        // -Y bottom
        quad(pose, vc, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0, -1, 0, r, g, b, a, light);
        // +Z front
        quad(pose, vc, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0, 0, 1, r, g, b, a, light);
        // -Z rear
        quad(pose, vc, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0, 0, -1, r, g, b, a, light);
        // +X left
        quad(pose, vc, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1, 0, 0, r, g, b, a, light);
        // -X right
        quad(pose, vc, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1, 0, 0, r, g, b, a, light);
    }

    public static void drawWheelCylinder(
        PoseStack.Pose pose,
        VertexConsumer vc,
        float radius,
        float width,
        int segments,
        int light
    ) {
        float halfW = width * 0.5F;
        for (int i = 0; i < segments; i++) {
            double a0 = (2.0 * Math.PI * i) / segments;
            double a1 = (2.0 * Math.PI * (i + 1)) / segments;
            float y0 = (float) (Math.cos(a0) * radius);
            float z0 = (float) (Math.sin(a0) * radius);
            float y1 = (float) (Math.cos(a1) * radius);
            float z1 = (float) (Math.sin(a1) * radius);
            float ny = (float) Math.cos((a0 + a1) * 0.5);
            float nz = (float) Math.sin((a0 + a1) * 0.5);

            // Tire tread band
            quad(pose, vc, -halfW, y0, z0, halfW, y0, z0, halfW, y1, z1, -halfW, y1, z1, 0, ny, nz, 32, 34, 38, 255, light);
            // Left and right tire sidewalls
            quad(pose, vc, halfW, 0, 0, halfW, y1, z1, halfW, y0, z0, halfW, 0, 0, 1, 0, 0, 48, 52, 58, 255, light);
            quad(pose, vc, -halfW, 0, 0, -halfW, y0, z0, -halfW, y1, z1, -halfW, 0, 0, -1, 0, 0, 48, 52, 58, 255, light);
        }

        // Visible alloy rim spoke bars so wheel spin is immediately legible
        float hubR = radius * 0.56F;
        float spokeT = radius * 0.14F;
        float rimW = halfW + 0.008F;
        drawBox(pose, vc, -rimW, -hubR, -spokeT, rimW, hubR, spokeT, 185, 192, 202, 255, light);
        drawBox(pose, vc, -rimW, -spokeT, -hubR, rimW, spokeT, hubR, 185, 192, 202, 255, light);
    }

    private static void quad(
        PoseStack.Pose pose,
        VertexConsumer vc,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float nx, float ny, float nz,
        int r, int g, int b, int a,
        int light
    ) {
        vertex(pose, vc, x0, y0, z0, 0.0F, 0.0F, nx, ny, nz, r, g, b, a, light);
        vertex(pose, vc, x1, y1, z1, 1.0F, 0.0F, nx, ny, nz, r, g, b, a, light);
        vertex(pose, vc, x2, y2, z2, 1.0F, 1.0F, nx, ny, nz, r, g, b, a, light);
        vertex(pose, vc, x3, y3, z3, 0.0F, 1.0F, nx, ny, nz, r, g, b, a, light);
    }

    private static void vertex(
        PoseStack.Pose pose,
        VertexConsumer vc,
        float x, float y, float z,
        float u, float v,
        float nx, float ny, float nz,
        int r, int g, int b, int a,
        int light
    ) {
        vc.addVertex(pose, x, y, z)
            .setColor(r, g, b, a)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, nx, ny, nz);
    }
}
