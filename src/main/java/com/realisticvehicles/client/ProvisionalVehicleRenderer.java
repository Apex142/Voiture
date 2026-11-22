package com.realisticvehicles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.realisticvehicles.minecraft.VehicleEntity;
import com.realisticvehicles.render.api.VehicleRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * NeoForge EntityRenderer bridging VehicleEntity to the active RenderBackend.
 */
public final class ProvisionalVehicleRenderer extends EntityRenderer<VehicleEntity> {
    private final ProvisionalRenderBackend backend;

    public ProvisionalVehicleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.6F;
        this.backend = new ProvisionalRenderBackend();
        this.backend.initialize();
    }

    @Override
    public void render(
        VehicleEntity entity,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight
    ) {
        VehicleRenderState state = entity.buildRenderState(partialTick);
        double renderOriginX = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double renderOriginY = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double renderOriginZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        poseStack.pushPose();
        poseStack.translate(
            state.position().x() - renderOriginX,
            state.position().y() - renderOriginY,
            state.position().z() - renderOriginZ
        );
        backend.bindFrameContext(poseStack, bufferSource, packedLight);
        backend.renderVehicle(state, partialTick);
        poseStack.popPose();

        SkidMarkRenderer.renderSkidMarks(poseStack, bufferSource, renderOriginX, renderOriginY, renderOriginZ, packedLight);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(VehicleEntity entity) {
        return ProvisionalRenderBackend.WHITE_TEXTURE;
    }
}
