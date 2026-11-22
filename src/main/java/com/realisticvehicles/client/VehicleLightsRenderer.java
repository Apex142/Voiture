package com.realisticvehicles.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.realisticvehicles.render.api.VehicleRenderState;
import com.realisticvehicles.render.glb.GlbMeshPrimitive;
import com.realisticvehicles.render.glb.GlbModel;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/**
 * Renders real 3D GLB lamp emitter & lens meshes (headlights, taillights, brake lights, reverse, indicators)
 * plus smooth radial ground light pools behind the car (red brake glow, white reverse beam).
 */
public final class VehicleLightsRenderer implements AutoCloseable {
    private static final int FULL_BRIGHT = 15728880;

    private final GlbModel source;
    private final float[] activeDamage = new float[4];
    private VertexBuffer lowBeamVbo, highBeamVbo, tailLightVbo, brakeLightVbo, trunkBrakeVbo;
    private VertexBuffer reverseLightVbo, leftTurnChassisVbo, leftTurnDoorVbo, rightTurnChassisVbo, rightTurnDoorVbo;

    public VehicleLightsRenderer(GlbModel model) {
        this.source = model;
        rebuildAllLamps();
    }

    private void rebuildAllLamps() {
        List<GlbMeshPrimitive> ch = source.chassisPrimitives();
        this.lowBeamVbo = buildEmissiveVbo(ch, VehicleLightsRenderer::isLowBeamNode, 235, 246, 255, true);
        this.highBeamVbo = buildEmissiveVbo(ch, VehicleLightsRenderer::isHighBeamNode, 255, 255, 255, true);
        this.tailLightVbo = buildEmissiveVbo(ch, VehicleLightsRenderer::isTailNode, 230, 14, 18, true);
        this.brakeLightVbo = buildEmissiveVbo(ch, VehicleLightsRenderer::isBrakeNode, 255, 16, 16, true);
        this.trunkBrakeVbo = buildEmissiveVbo(source.trunk().primitives(), VehicleLightsRenderer::isCenterBrakeNode, 255, 16, 16, false);
        this.reverseLightVbo = buildEmissiveVbo(ch, VehicleLightsRenderer::isReverseNode, 255, 255, 255, true);
        this.leftTurnChassisVbo = buildEmissiveVbo(ch, VehicleLightsRenderer::isLeftTurnChassis, 255, 165, 0, true);
        this.leftTurnDoorVbo = buildEmissiveVbo(source.leftDoor().primitives(), VehicleLightsRenderer::isMirrorTurn, 255, 165, 0, false);
        this.rightTurnChassisVbo = buildEmissiveVbo(ch, VehicleLightsRenderer::isRightTurnChassis, 255, 165, 0, true);
        this.rightTurnDoorVbo = buildEmissiveVbo(source.rightDoor().primitives(), VehicleLightsRenderer::isMirrorTurn, 255, 165, 0, false);
    }

    public void updateDamageDeformation(float front, float rear, float left, float right) {
        if (Math.abs(front - activeDamage[0]) < 0.10F && Math.abs(rear - activeDamage[1]) < 0.10F
            && Math.abs(left - activeDamage[2]) < 0.10F && Math.abs(right - activeDamage[3]) < 0.10F) return;
        activeDamage[0] = front; activeDamage[1] = rear; activeDamage[2] = left; activeDamage[3] = right;
        closeAllVbos();
        rebuildAllLamps();
    }

    public void drawEmissiveMeshes(
        Matrix4f chassisMv, Matrix4f trunkMv, Matrix4f leftDoorMv, Matrix4f rightDoorMv,
        Matrix4f proj, ShaderInstance shader, VehicleRenderState state
    ) {
        boolean blinkOn = ((System.currentTimeMillis() / 300L) & 1L) == 0L;
        if (state.frontDent() < 0.65F) {
            if (state.highBeamsOn()) drawVbo(highBeamVbo, chassisMv, proj, shader);
            else if (state.lowBeamsOn()) drawVbo(lowBeamVbo, chassisMv, proj, shader);
        }
        if (state.rearDent() < 0.65F) {
            if (state.braking()) { drawVbo(brakeLightVbo, chassisMv, proj, shader); drawVbo(trunkBrakeVbo, trunkMv, proj, shader); }
            else if (state.lowBeamsOn() || state.highBeamsOn()) drawVbo(tailLightVbo, chassisMv, proj, shader);
            if (state.reversing()) drawVbo(reverseLightVbo, chassisMv, proj, shader);
        }
        if (state.leftIndicatorOn() && blinkOn) {
            if (state.frontDent() < 0.65F && state.rearDent() < 0.65F) drawVbo(leftTurnChassisVbo, chassisMv, proj, shader);
            drawVbo(leftTurnDoorVbo, leftDoorMv, proj, shader);
        }
        if (state.rightIndicatorOn() && blinkOn) {
            if (state.frontDent() < 0.65F && state.rearDent() < 0.65F) drawVbo(rightTurnChassisVbo, chassisMv, proj, shader);
            drawVbo(rightTurnDoorVbo, rightDoorMv, proj, shader);
        }
    }

    /** Draws smooth radial ground light fans (zero sharp rectangles) for red brake lights & white reverse lights. */
    public static void renderDirectionalGroundPools(PoseStack poseStack, MultiBufferSource buffers, VehicleRenderState state) {
        boolean braking = state.braking(), tail = state.lowBeamsOn() || state.highBeamsOn(), reversing = state.reversing();
        if (!braking && !tail && !reversing) return;

        RenderSystem.disableCull();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(ProvisionalRenderBackend.WHITE_TEXTURE));
        Matrix4f mat = poseStack.last().pose();
        float y = -0.605F;
        if (braking || tail) {
            int alpha = braking ? 175 : 75;
            float reach = braking ? 4.4F : 2.2F;
            drawRadialFan(vc, mat, 0.94F, y, -2.90F, 1.95F, reach, -1.0F, 255, 12, 16, alpha);
            drawRadialFan(vc, mat, -0.94F, y, -2.90F, 1.95F, reach, -1.0F, 255, 12, 16, alpha);
        }
        if (reversing) {
            drawRadialFan(vc, mat, 0.78F, y + 0.008F, -2.95F, 2.10F, 6.2F, -1.0F, 250, 252, 255, 195);
            drawRadialFan(vc, mat, -0.78F, y + 0.008F, -2.95F, 2.10F, 6.2F, -1.0F, 250, 252, 255, 195);
        }
        if (buffers instanceof MultiBufferSource.BufferSource bs) bs.endBatch();
        RenderSystem.enableCull();
    }

    private static void drawRadialFan(VertexConsumer vc, Matrix4f mat, float cx, float y, float cz, float rx, float rz, float dz, int r, int g, int b, int a) {
        for (int i = 0; i < 14; i++) {
            double a1 = Math.PI * i / 14, a2 = Math.PI * (i + 1) / 14;
            emitFanVert(vc, mat, cx, y, cz, r, g, b, a);
            emitFanVert(vc, mat, cx + (float) Math.cos(a2) * rx, y, cz + (float) Math.sin(a2) * rz * dz, r, g, b, 0);
            emitFanVert(vc, mat, cx + (float) Math.cos(a1) * rx, y, cz + (float) Math.sin(a1) * rz * dz, r, g, b, 0);
            emitFanVert(vc, mat, cx, y, cz, r, g, b, a);
        }
    }

    private static void emitFanVert(VertexConsumer vc, Matrix4f mat, float x, float y, float z, int r, int g, int b, int a) {
        vc.addVertex(mat, x, y, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static boolean isLowBeamNode(String n) {
        return n.startsWith("headlightl_a_1_") || n.startsWith("headlightl_a_2_") || n.startsWith("headlightl_a_3")
            || n.startsWith("headlightl_a_5") || n.startsWith("headlightlbulbs") || n.startsWith("headlightr_a_1")
            || n.startsWith("headlightr_a_3") || n.startsWith("headlightrbulbs") || n.startsWith("lenslhl_a_1")
            || n.startsWith("lenslhl_a_2") || n.startsWith("lensrhl_a_1") || n.startsWith("lensrhl_a_2")
            || n.startsWith("bumperfsidemarkers_a_1") || n.startsWith("bumperfsidemarkers_a_2");
    }
    private static boolean isHighBeamNode(String n) {
        return isLowBeamNode(n) || n.startsWith("headlightl_a_11") || n.startsWith("headlightl_a_13") || n.startsWith("headlightl_a_14")
            || n.startsWith("headlightr_a_9") || n.startsWith("headlightr_a_11") || n.startsWith("headlightr_a_12") || n.startsWith("lenslhl_a_0") || n.startsWith("lensrhl_a_0");
    }
    private static boolean isTailNode(String n) {
        return n.startsWith("taillightl_a_1") || n.startsWith("taillightl_a_5") || n.startsWith("taillightlbulbs_a_0")
            || n.startsWith("taillightr_a_1") || n.startsWith("taillightr_a_5") || n.startsWith("taillightrbulbs_a_0");
    }
    private static boolean isBrakeNode(String n) {
        return isTailNode(n) || n.startsWith("taillightlbulbs_a_3") || n.startsWith("taillightrbulbs_a_3")
            || n.startsWith("taillightl_a_0") || n.startsWith("taillightr_a_0") || n.startsWith("lensltl_a_0") || n.startsWith("lensrtl_a_0");
    }
    private static boolean isCenterBrakeNode(String n) { return n.startsWith("tailightc") || n.startsWith("taillightc") || n.startsWith("glassctl"); }
    private static boolean isReverseNode(String n) {
        return n.startsWith("taillightl_a_2") || n.startsWith("taillightlbulbs_a_1") || n.startsWith("taillightr_a_2")
            || n.startsWith("taillightrbulbs_a_1") || n.startsWith("glassltl_a_1") || n.startsWith("glassrtl_a_1");
    }
    private static boolean isLeftTurnChassis(String n) {
        return n.startsWith("headlightl_a_4") || n.startsWith("headlightl_a_6") || n.startsWith("lenslhl_a_0") || n.startsWith("taillightl_a_3")
            || n.startsWith("taillightl_a_4") || n.startsWith("taillightlbulbs_a_2") || n.startsWith("taillightlbulbs_a_4") || n.startsWith("glassbumperfsidemarkerl");
    }
    private static boolean isRightTurnChassis(String n) {
        return n.startsWith("headlightr_a_2") || n.startsWith("headlightr_a_4") || n.startsWith("lensrhl_a_0") || n.startsWith("taillightr_a_3")
            || n.startsWith("taillightr_a_4") || n.startsWith("taillightrbulbs_a_2") || n.startsWith("taillightrbulbs_a_4") || n.startsWith("glassbumperfsidemarkerr");
    }
    private static boolean isMirrorTurn(String n) { return n.startsWith("glasswingmirrorsidemarker") || n.startsWith("wingmirrorlf_a_4") || n.startsWith("wingmirrorrf_a_4"); }

    @FunctionalInterface private interface NodePredicate { boolean test(String lowerName); }

    private VertexBuffer buildEmissiveVbo(List<GlbMeshPrimitive> prims, NodePredicate pred, int r, int g, int b, boolean applyDent) {
        try (ByteBufferBuilder byteBuf = new ByteBufferBuilder(256 * 1024)) {
            BufferBuilder vc = new BufferBuilder(byteBuf, VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.NEW_ENTITY);
            boolean hasVerts = false;
            float offset = (r == 255 && g == 255 && b == 255) ? 0.008F : 0.005F;
            for (GlbMeshPrimitive p : prims) {
                if (!pred.test(p.nodeName().toLowerCase(Locale.ROOT))) continue;
                float[] pos = p.positions(), norm = p.normals();
                int[] idx = p.indices();
                for (int i = 0; i < idx.length; i++) {
                    int p3 = idx[i] * 3;
                    float nx = norm[p3], ny = norm[p3 + 1], nz = norm[p3 + 2];
                    float[] pt = { pos[p3] + nx * offset, pos[p3 + 1] + ny * offset, pos[p3 + 2] + nz * offset };
                    if (applyDent) RetainedGlbMesh.applyDentToPoint(pt, activeDamage);
                    vc.addVertex(pt[0], pt[1], pt[2]).setColor(r, g, b, 255).setUv(0, 0)
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(nx, ny, nz);
                }
                hasVerts = true;
            }
            if (!hasVerts) return null;
            MeshData mesh = vc.build();
            if (mesh == null) return null;
            VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vbo.bind(); vbo.upload(mesh); VertexBuffer.unbind();
            return vbo;
        }
    }

    private static void drawVbo(VertexBuffer buf, Matrix4f mv, Matrix4f proj, ShaderInstance shader) {
        if (buf != null && shader != null && mv != null) { buf.bind(); buf.drawWithShader(mv, proj, shader); VertexBuffer.unbind(); }
    }

    private void closeAllVbos() {
        for (VertexBuffer b : new VertexBuffer[] { lowBeamVbo, highBeamVbo, tailLightVbo, brakeLightVbo, trunkBrakeVbo, reverseLightVbo, leftTurnChassisVbo, leftTurnDoorVbo, rightTurnChassisVbo, rightTurnDoorVbo }) {
            if (b != null) b.close();
        }
    }

    @Override public void close() { RenderSystem.recordRenderCall(this::closeAllVbos); }
}
