package com.realisticvehicles.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.realisticvehicles.render.api.VehicleRenderState;
import com.realisticvehicles.render.glb.GlbMaterial;
import com.realisticvehicles.render.glb.GlbMeshPrimitive;
import com.realisticvehicles.render.glb.GlbModel;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/**
 * Renders the 3D rotating steering wheel and rotating speedometer / tachometer gauge needles inside the cockpit.
 * Groups the metallic needle cap (dashStructure_a_15/18/19) with the emissive needle pointer (dashStructure_a_1/2)
 * around their exact circular hub center so there is never a static duplicate needle left behind.
 */
public final class VehicleCockpitAnimator implements AutoCloseable {
    private static final int FULL_BRIGHT = 15728880;
    private static final float STEER_PIVOT_X = 0.543F;
    private static final float STEER_PIVOT_Y = 0.561F;
    private static final float STEER_PIVOT_Z = -0.030F;

    private static final float SPEED_PIVOT_X = 0.6820F;
    private static final float SPEED_PIVOT_Y = 0.6170F;
    private static final float SPEED_PIVOT_Z = 0.3305F;

    private static final float RPM_PIVOT_X = 0.3682F;
    private static final float RPM_PIVOT_Y = 0.6174F;
    private static final float RPM_PIVOT_Z = 0.3220F;

    private final VertexBuffer steeringWheelVbo;
    private final VertexBuffer speedNeedleVbo;
    private final VertexBuffer rpmNeedleVbo;
    private float smoothedSteerRad;
    private float smoothedSpeedKmh;
    private float smoothedRpm;

    public VehicleCockpitAnimator(GlbModel model) {
        List<GlbMaterial> mats = model.materials();
        this.steeringWheelVbo = buildSteerVbo(model.chassisPrimitives(), mats);
        this.speedNeedleVbo = buildNeedleVbo(model.chassisPrimitives(), true, SPEED_PIVOT_X, SPEED_PIVOT_Y, SPEED_PIVOT_Z);
        this.rpmNeedleVbo = buildNeedleVbo(model.chassisPrimitives(), false, RPM_PIVOT_X, RPM_PIVOT_Y, RPM_PIVOT_Z);
    }

    public static boolean isAnimatedCockpitNode(String lowerName) {
        return lowerName.startsWith("steeringwheel")
            || lowerName.equals("dashstructure_a_1_[gaugeneedle]")
            || lowerName.equals("dashstructure_a_2_[gaugeneedle]")
            || lowerName.startsWith("dashstructure_a_15_")
            || lowerName.startsWith("dashstructure_a_16_")
            || lowerName.startsWith("dashstructure_a_18_")
            || lowerName.startsWith("dashstructure_a_19_");
    }

    public void render(
        PoseStack poseStack,
        Matrix4f baseMv,
        Matrix4f proj,
        ShaderInstance shader,
        VehicleRenderState state
    ) {
        float targetSteer = state.wheels().isEmpty() ? 0.0F : (float) state.wheels().get(0).steerAngleRadians();
        smoothedSteerRad += (targetSteer - smoothedSteerRad) * 0.14F;
        smoothedSpeedKmh += (state.speedKmh() - smoothedSpeedKmh) * 0.18F;
        smoothedRpm += (state.engineRpm() - smoothedRpm) * 0.20F;

        float wheelRotZ = -smoothedSteerRad * 2.2F;

        if (steeringWheelVbo != null) {
            poseStack.pushPose();
            poseStack.translate(STEER_PIVOT_X, STEER_PIVOT_Y, STEER_PIVOT_Z);
            poseStack.mulPose(Axis.XP.rotationDegrees(21.0F));
            poseStack.mulPose(Axis.ZP.rotation(wheelRotZ));
            poseStack.mulPose(Axis.XP.rotationDegrees(-21.0F));
            Matrix4f mv = new Matrix4f(baseMv).mul(poseStack.last().pose());
            drawVbo(steeringWheelVbo, mv, proj, shader);
            poseStack.popPose();
        }

        // Speedometer needle (left dial): 0 km/h points straight down (0 deg), rotates clockwise (+Z in car-local looking at +Z)
        if (speedNeedleVbo != null) {
            float speedFrac = Math.max(0.0F, Math.min(1.0F, smoothedSpeedKmh / 280.0F));
            float speedDeg = speedFrac * 240.0F;
            poseStack.pushPose();
            poseStack.translate(SPEED_PIVOT_X, SPEED_PIVOT_Y, SPEED_PIVOT_Z);
            poseStack.mulPose(Axis.ZP.rotationDegrees(speedDeg));
            Matrix4f mv = new Matrix4f(baseMv).mul(poseStack.last().pose());
            drawVbo(speedNeedleVbo, mv, proj, shader);
            poseStack.popPose();
        }

        // Tachometer RPM needle (right dial): 0 RPM points straight down (0 deg), rotates clockwise (+Z)
        if (rpmNeedleVbo != null) {
            float rpmFrac = Math.max(0.0F, Math.min(1.0F, smoothedRpm / 7800.0F));
            float rpmDeg = rpmFrac * 240.0F;
            poseStack.pushPose();
            poseStack.translate(RPM_PIVOT_X, RPM_PIVOT_Y, RPM_PIVOT_Z);
            poseStack.mulPose(Axis.ZP.rotationDegrees(rpmDeg));
            Matrix4f mv = new Matrix4f(baseMv).mul(poseStack.last().pose());
            drawVbo(rpmNeedleVbo, mv, proj, shader);
            poseStack.popPose();
        }
    }

    private static VertexBuffer buildSteerVbo(List<GlbMeshPrimitive> prims, List<GlbMaterial> mats) {
        try (ByteBufferBuilder byteBuf = new ByteBufferBuilder(256 * 1024)) {
            BufferBuilder vc = new BufferBuilder(byteBuf, VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.NEW_ENTITY);
            boolean hasVerts = false;
            for (GlbMeshPrimitive p : prims) {
                if (!p.nodeName().toLowerCase(Locale.ROOT).startsWith("steeringwheel")) continue;
                GlbMaterial mat = (p.materialIndex() >= 0 && p.materialIndex() < mats.size())
                    ? mats.get(p.materialIndex()) : GlbMaterial.DEFAULT;
                int[] rgba = GlbMaterialPalette.resolveRgba(mat, false);
                float[] pos = p.positions(), norm = p.normals();
                int[] idx = p.indices();
                for (int i = 0; i < idx.length; i++) {
                    int p3 = idx[i] * 3;
                    float nx = norm[p3], ny = norm[p3 + 1], nz = norm[p3 + 2];
                    float shade = 0.55F + 0.45F * Math.max(0.0F, ny);
                    vc.addVertex(pos[p3] - STEER_PIVOT_X, pos[p3 + 1] - STEER_PIVOT_Y, pos[p3 + 2] - STEER_PIVOT_Z)
                        .setColor(Math.round(rgba[0] * shade), Math.round(rgba[1] * shade), Math.round(rgba[2] * shade), 255)
                        .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(FULL_BRIGHT).setNormal(nx, ny, nz);
                }
                hasVerts = true;
            }
            if (!hasVerts) return null;
            MeshData mesh = vc.build();
            if (mesh == null) return null;
            VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vbo.bind();
            vbo.upload(mesh);
            VertexBuffer.unbind();
            return vbo;
        }
    }

    private static VertexBuffer buildNeedleVbo(List<GlbMeshPrimitive> prims, boolean leftSpeedDial, float px, float py, float pz) {
        try (ByteBufferBuilder byteBuf = new ByteBufferBuilder(32 * 1024)) {
            BufferBuilder vc = new BufferBuilder(byteBuf, VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.NEW_ENTITY);
            boolean hasVerts = false;
            for (GlbMeshPrimitive p : prims) {
                String n = p.nodeName().toLowerCase(Locale.ROOT);
                boolean isPointer = leftSpeedDial
                    ? n.equals("dashstructure_a_1_[gaugeneedle]")
                    : n.equals("dashstructure_a_2_[gaugeneedle]");
                boolean isBody = leftSpeedDial
                    ? (n.startsWith("dashstructure_a_18_") || n.startsWith("dashstructure_a_16_"))
                    : (n.startsWith("dashstructure_a_19_") || n.startsWith("dashstructure_a_15_"));
                if (!isPointer && !isBody) continue;
                int r = isPointer ? 255 : 210, g = isPointer ? 48 : 215, b = isPointer ? 36 : 225;
                float[] pos = p.positions(), norm = p.normals();
                int[] idx = p.indices();
                for (int i = 0; i < idx.length; i++) {
                    int p3 = idx[i] * 3;
                    vc.addVertex(pos[p3] - px, pos[p3 + 1] - py, pos[p3 + 2] - pz - 0.003F)
                        .setColor(r, g, b, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(FULL_BRIGHT).setNormal(norm[p3], norm[p3 + 1], norm[p3 + 2]);
                }
                hasVerts = true;
            }
            if (!hasVerts) return null;
            MeshData mesh = vc.build();
            if (mesh == null) return null;
            VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vbo.bind();
            vbo.upload(mesh);
            VertexBuffer.unbind();
            return vbo;
        }
    }

    private static void drawVbo(VertexBuffer buf, Matrix4f mv, Matrix4f proj, ShaderInstance shader) {
        if (buf != null && shader != null) {
            buf.bind();
            buf.drawWithShader(mv, proj, shader);
            VertexBuffer.unbind();
        }
    }

    @Override
    public void close() {
        RenderSystem.recordRenderCall(() -> {
            if (steeringWheelVbo != null) steeringWheelVbo.close();
            if (speedNeedleVbo != null) speedNeedleVbo.close();
            if (rpmNeedleVbo != null) rpmNeedleVbo.close();
        });
    }
}
