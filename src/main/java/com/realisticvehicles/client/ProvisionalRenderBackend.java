package com.realisticvehicles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.minecraft.RealisticVehiclesMod;
import com.realisticvehicles.physics.simulation.VehiclePresetFactory;
import com.realisticvehicles.render.api.BackendType;
import com.realisticvehicles.render.api.RenderBackend;
import com.realisticvehicles.render.api.VehicleRenderState;
import com.realisticvehicles.render.api.WheelRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

/**
 * Provisional Phase 1 RenderBackend implementation using Minecraft's buffer pipeline.
 * Will be superseded by the native LWJGL Vulkan backend in Phase 3.
 */
public final class ProvisionalRenderBackend implements RenderBackend {
    public static final ResourceLocation WHITE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(RealisticVehiclesMod.MOD_ID, "textures/entity/white.png");

    private PoseStack activePoseStack;
    private MultiBufferSource activeBuffers;
    private int activeLight;

    @Override
    public BackendType type() {
        return BackendType.PROVISIONAL_MINECRAFT;
    }

    @Override
    public void initialize() {
        // Uses Minecraft's existing pipeline during Phase 1 validation.
    }

    public void bindFrameContext(PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        this.activePoseStack = poseStack;
        this.activeBuffers = buffers;
        this.activeLight = packedLight;
    }

    @Override
    public void renderVehicle(VehicleRenderState state, float partialTick) {
        if (activePoseStack == null || activeBuffers == null) {
            return;
        }
        activePoseStack.pushPose();
        Quat4d q = state.orientation();
        activePoseStack.mulPose(new Quaternionf((float) q.x(), (float) q.y(), (float) q.z(), (float) q.w()));

        if (VehiclePresetFactory.PRESET_SLK55.equalsIgnoreCase(state.presetId())) {
            var glb = GlbModelRegistry.getOrLoadSlk55();
            if (glb != null) {
                GlbVehicleRenderer.render(activePoseStack, activeBuffers, glb, state, activeLight);
                activePoseStack.popPose();
                return;
            }
        }

        VertexConsumer vc = activeBuffers.getBuffer(RenderType.entitySolid(WHITE_TEXTURE));
        drawChassis(activePoseStack.last(), vc, state, activeLight);
        for (WheelRenderState wheel : state.wheels()) {
            drawWheel(activePoseStack, vc, wheel, activeLight);
        }

        activePoseStack.popPose();
    }

    private void drawChassis(PoseStack.Pose pose, VertexConsumer vc, VehicleRenderState state, int light) {
        boolean isSport = VehiclePresetFactory.PRESET_SPORT.equalsIgnoreCase(state.presetId());
        int bodyR = isSport ? 195 : 42;
        int bodyG = isSport ? 36 : 96;
        int bodyB = isSport ? 42 : 168;

        float hw = (float) (state.chassisWidth() * 0.5);
        float hl = (float) (state.chassisLength() * 0.5);

        // Main lower body shell
        ProvisionalMeshBuilder.drawBox(pose, vc, -hw, -0.24F, -hl, hw, 0.34F, hl, bodyR, bodyG, bodyB, 255, light);
        // Dark underbody skirt & front/rear bumpers
        ProvisionalMeshBuilder.drawBox(pose, vc, -hw - 0.02F, -0.27F, hl - 0.25F, hw + 0.02F, -0.05F, hl + 0.05F, 35, 38, 44, 255, light);
        ProvisionalMeshBuilder.drawBox(pose, vc, -hw - 0.02F, -0.27F, -hl - 0.05F, hw + 0.02F, -0.05F, -hl + 0.25F, 35, 38, 44, 255, light);

        // Cabin greenhouse & roof
        float cabW = hw * 0.84F;
        ProvisionalMeshBuilder.drawBox(pose, vc, -cabW, 0.34F, -hl * 0.48F, cabW, 0.86F, hl * 0.28F, bodyR, bodyG, bodyB, 255, light);
        // Windshield & rear glass panels
        ProvisionalMeshBuilder.drawBox(pose, vc, -cabW + 0.06F, 0.38F, hl * 0.28F, cabW - 0.06F, 0.80F, hl * 0.31F, 120, 175, 215, 255, light);
        ProvisionalMeshBuilder.drawBox(pose, vc, -cabW + 0.06F, 0.38F, -hl * 0.51F, cabW - 0.06F, 0.80F, -hl * 0.48F, 90, 130, 165, 255, light);

        // Front grille & bright headlights (+Z)
        ProvisionalMeshBuilder.drawBox(pose, vc, -0.45F, -0.02F, hl, 0.45F, 0.20F, hl + 0.03F, 28, 30, 34, 255, light);
        ProvisionalMeshBuilder.drawBox(pose, vc, hw - 0.36F, 0.06F, hl, hw - 0.08F, 0.24F, hl + 0.04F, 255, 248, 210, 255, 15728880);
        ProvisionalMeshBuilder.drawBox(pose, vc, -hw + 0.08F, 0.06F, hl, -hw + 0.36F, 0.24F, hl + 0.04F, 255, 248, 210, 255, 15728880);

        // Dynamic brake & reverse taillights (-Z)
        int tailR = state.braking() ? 255 : 145;
        int tailG = state.braking() ? 35 : 18;
        int tailLight = state.braking() ? 15728880 : light;
        ProvisionalMeshBuilder.drawBox(pose, vc, hw - 0.38F, 0.06F, -hl - 0.04F, hw - 0.08F, 0.24F, -hl, tailR, tailG, 20, 255, tailLight);
        ProvisionalMeshBuilder.drawBox(pose, vc, -hw + 0.08F, 0.06F, -hl - 0.04F, -hw + 0.38F, 0.24F, -hl, tailR, tailG, 20, 255, tailLight);

        if (state.reversing()) {
            ProvisionalMeshBuilder.drawBox(pose, vc, -0.32F, 0.08F, -hl - 0.04F, -0.10F, 0.22F, -hl, 255, 255, 255, 255, 15728880);
            ProvisionalMeshBuilder.drawBox(pose, vc, 0.10F, 0.08F, -hl - 0.04F, 0.32F, 0.22F, -hl, 255, 255, 255, 255, 15728880);
        }
    }

    private void drawWheel(PoseStack poseStack, VertexConsumer vc, WheelRenderState wheel, int light) {
        poseStack.pushPose();
        poseStack.translate(wheel.localHubOffset().x(), wheel.localHubOffset().y(), wheel.localHubOffset().z());
        poseStack.mulPose(Axis.YP.rotation((float) wheel.steerAngleRadians()));
        poseStack.mulPose(Axis.XP.rotation((float) wheel.spinAngleRadians()));
        ProvisionalMeshBuilder.drawWheelCylinder(poseStack.last(), vc, (float) wheel.radius(), (float) wheel.width(), 14, light);
        poseStack.popPose();
    }

    @Override
    public void dispose() {
        activePoseStack = null;
        activeBuffers = null;
    }
}
