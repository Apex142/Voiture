package com.realisticvehicles.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.realisticvehicles.render.api.VehicleRenderState;
import com.realisticvehicles.render.api.WheelRenderState;
import com.realisticvehicles.render.glb.GlbHingedPart;
import com.realisticvehicles.render.glb.GlbModel;
import com.realisticvehicles.render.glb.GlbWheelPart;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

/**
 * Renders a GlbModel via static GPU VertexBuffers with day/night shading, rotating cockpit, and emissive 3D lamps.
 */
public final class GlbVehicleRenderer {
    private static volatile RetainedGlbMesh cachedMesh;
    private static volatile VehicleLightsRenderer cachedLights;
    private static volatile VehicleCockpitAnimator cachedCockpit;
    private static volatile GlbModel cachedSource;

    private GlbVehicleRenderer() {
    }

    public static void render(
        PoseStack poseStack,
        MultiBufferSource buffers,
        GlbModel model,
        VehicleRenderState state,
        int packedLight
    ) {
        if (cachedMesh == null || cachedLights == null || cachedCockpit == null || cachedSource != model) {
            cachedMesh = new RetainedGlbMesh(model);
            cachedLights = new VehicleLightsRenderer(model);
            cachedCockpit = new VehicleCockpitAnimator(model);
            cachedSource = model;
        }
        cachedMesh.updateDamageDeformation(state.frontDent(), state.rearDent(), state.leftDent(), state.rightDent());
        cachedLights.updateDamageDeformation(state.frontDent(), state.rearDent(), state.leftDent(), state.rightDent());

        if (buffers instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }

        Matrix4f baseMv = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f proj = RenderSystem.getProjectionMatrix();

        // Compute realistic day/night ambient & sky light modulation from packedLight and time of day
        float[] envColor = computeEnvironmentShade(packedLight);
        RenderSystem.setShaderColor(envColor[0], envColor[1], envColor[2], 1.0F);

        RenderType solidType = RenderType.entityCutoutNoCull(ProvisionalRenderBackend.WHITE_TEXTURE);
        solidType.setupRenderState();
        RenderSystem.disableCull();
        ShaderInstance opaqueShader = GameRenderer.getRendertypeEntityCutoutNoCullShader();
        Matrix4f chassisMv = new Matrix4f(baseMv).mul(poseStack.last().pose());

        cachedMesh.drawOpaqueChassis(chassisMv, proj, opaqueShader);
        cachedCockpit.render(poseStack, baseMv, proj, opaqueShader, state);

        List<WheelRenderState> physWheels = state.wheels();
        if (physWheels.size() >= 4) {
            drawWheel(poseStack, baseMv, 0, model.frontLeftWheel(), physWheels.get(0), proj, opaqueShader);
            drawWheel(poseStack, baseMv, 1, model.frontRightWheel(), physWheels.get(1), proj, opaqueShader);
            drawWheel(poseStack, baseMv, 2, model.rearLeftWheel(), physWheels.get(2), proj, opaqueShader);
            drawWheel(poseStack, baseMv, 3, model.rearRightWheel(), physWheels.get(3), proj, opaqueShader);
        }

        Matrix4f leftDoorMv = computeHingedMv(poseStack, baseMv, model.leftDoor(), -state.leftDoorOpen() * 1.10F, true);
        Matrix4f rightDoorMv = computeHingedMv(poseStack, baseMv, model.rightDoor(), state.rightDoorOpen() * 1.10F, true);
        Matrix4f hoodMv = computeHingedMv(poseStack, baseMv, model.hood(), -state.hoodOpen() * 0.85F, false);
        Matrix4f trunkMv = computeHingedMv(poseStack, baseMv, model.trunk(), state.trunkOpen() * 0.82F, false);

        cachedMesh.drawHingedOpaque(0, leftDoorMv, proj, opaqueShader);
        cachedMesh.drawHingedOpaque(1, rightDoorMv, proj, opaqueShader);
        cachedMesh.drawHingedOpaque(2, hoodMv, proj, opaqueShader);
        cachedMesh.drawHingedOpaque(3, trunkMv, proj, opaqueShader);

        RenderSystem.enableCull();
        solidType.clearRenderState();

        RenderType glassType = RenderType.entityTranslucentCull(ProvisionalRenderBackend.WHITE_TEXTURE);
        glassType.setupRenderState();
        ShaderInstance glassShader = GameRenderer.getRendertypeEntityTranslucentCullShader();
        RenderSystem.depthMask(false);
        cachedMesh.drawGlassChassis(chassisMv, proj, glassShader);
        cachedMesh.drawHingedGlass(0, leftDoorMv, proj, glassShader);
        cachedMesh.drawHingedGlass(1, rightDoorMv, proj, glassShader);
        cachedMesh.drawHingedGlass(3, trunkMv, proj, glassShader);
        RenderSystem.depthMask(true);
        glassType.clearRenderState();

        // Draw active 3D lamps at 100% full emissive brightness AFTER glass pass so they glow through covers at night
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        solidType.setupRenderState();
        RenderSystem.disableCull();
        cachedLights.drawEmissiveMeshes(chassisMv, trunkMv, leftDoorMv, rightDoorMv, proj, opaqueShader, state);
        RenderSystem.enableCull();
        solidType.clearRenderState();

        VehicleLightsRenderer.renderDirectionalGroundPools(poseStack, buffers, state);
    }

    private static float[] computeEnvironmentShade(int packedLight) {
        int blockL = LightTexture.block(packedLight);
        int skyL = LightTexture.sky(packedLight);
        float skySun = 1.0F;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            skySun = Math.max(0.16F, 1.0F - mc.level.getSkyDarken() / 11.0F);
        }
        float effectiveLight = Math.max(blockL / 15.0F, (skyL / 15.0F) * skySun);
        float brightness = 0.14F + 0.86F * effectiveLight;
        // Cool moonlight tint at night, warm sunlight balance during the day
        float nightFactor = 1.0F - skySun;
        float r = brightness * (1.0F - 0.12F * nightFactor);
        float g = brightness * (1.0F - 0.06F * nightFactor);
        float b = brightness * (1.0F + 0.05F * nightFactor);
        return new float[] { r, g, b };
    }

    private static void drawWheel(
        PoseStack poseStack, Matrix4f baseMv, int index, GlbWheelPart wheelPart,
        WheelRenderState physWheel, Matrix4f proj, ShaderInstance shader
    ) {
        if (wheelPart.primitives().isEmpty()) return;
        poseStack.pushPose();
        double yDelta = physWheel.localHubOffset().y() - (-0.145);
        poseStack.translate(wheelPart.hubX(), wheelPart.hubY() + yDelta, wheelPart.hubZ());
        poseStack.mulPose(Axis.YP.rotation((float) physWheel.steerAngleRadians()));
        poseStack.mulPose(Axis.XP.rotation((float) physWheel.spinAngleRadians()));
        Matrix4f wheelMv = new Matrix4f(baseMv).mul(poseStack.last().pose());
        cachedMesh.drawWheel(index, wheelMv, proj, shader);
        poseStack.popPose();
    }

    private static Matrix4f computeHingedMv(
        PoseStack poseStack, Matrix4f baseMv, GlbHingedPart part, float angleRad, boolean rotateAroundY
    ) {
        if (part.primitives().isEmpty()) return null;
        poseStack.pushPose();
        poseStack.translate(part.pivotX(), part.pivotY(), part.pivotZ());
        if (Math.abs(angleRad) > 1.0e-4F) {
            poseStack.mulPose(rotateAroundY ? Axis.YP.rotation(angleRad) : Axis.XP.rotation(angleRad));
        }
        Matrix4f partMv = new Matrix4f(baseMv).mul(poseStack.last().pose());
        poseStack.popPose();
        return partMv;
    }
}
