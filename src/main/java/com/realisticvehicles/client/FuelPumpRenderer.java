package com.realisticvehicles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.interactions.FuelType;
import com.realisticvehicles.minecraft.FuelPumpEntity;
import com.realisticvehicles.minecraft.VehicleEntity;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

/**
 * Renders the 3D Gas Station Fuel Pump block visual, high-contrast LCD screen (Liters, Total Price €, Price/L),
 * illuminated Essence SP98 / Diesel buttons, continuous 3D tubular rubber hose, and detailed 5-part fuel pistol.
 */
public final class FuelPumpRenderer extends EntityRenderer<FuelPumpEntity> {
    private static final int FULL_BRIGHT = 15728880;

    public FuelPumpRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.55F;
    }

    @Override
    public void render(
        FuelPumpEntity pump, float entityYaw, float partialTick,
        PoseStack poseStack, MultiBufferSource buffers, int packedLight
    ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-pump.getYRot()));

        VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(ProvisionalRenderBackend.WHITE_TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        // 1. Concrete island base, steel cabinet, side pillars & canopy header
        ProvisionalMeshBuilder.drawBox(pose, vc, -0.52F, 0.00F, -0.36F, 0.52F, 0.12F, 0.36F, 85, 88, 94, 255, packedLight);
        ProvisionalMeshBuilder.drawBox(pose, vc, -0.44F, 0.12F, -0.26F, 0.44F, 1.88F, 0.26F, 34, 38, 46, 255, packedLight);
        ProvisionalMeshBuilder.drawBox(pose, vc, -0.48F, 1.88F, -0.30F, 0.48F, 2.14F, 0.30F, 200, 32, 32, 255, packedLight);
        // Backlit digital LCD screen bezel
        ProvisionalMeshBuilder.drawBox(pose, vc, -0.38F, 1.08F, -0.275F, 0.38F, 1.82F, 0.275F, 14, 18, 24, 255, FULL_BRIGHT);

        // 2. Interactive 3D Selection Buttons on front (+Z) & back (-Z): Left = ESSENCE SP98 (Green), Right = DIESEL (Yellow)
        boolean isGas = pump.selectedFuelType() == FuelType.GASOLINE;
        int gasR = isGas ? 35 : 20, gasG = isGas ? 215 : 70, gasB = isGas ? 65 : 30;
        int dslR = !isGas ? 245 : 80, dslG = !isGas ? 185 : 60, dslB = !isGas ? 18 : 14;
        ProvisionalMeshBuilder.drawBox(pose, vc, 0.04F, 1.12F, -0.295F, 0.35F, 1.38F, 0.295F, gasR, gasG, gasB, 255, isGas ? FULL_BRIGHT : packedLight);
        ProvisionalMeshBuilder.drawBox(pose, vc, -0.35F, 1.12F, -0.295F, -0.04F, 1.38F, 0.295F, dslR, dslG, dslB, 255, !isGas ? FULL_BRIGHT : packedLight);
        ProvisionalMeshBuilder.drawBox(pose, vc, -0.52F, 0.85F, -0.10F, -0.44F, 1.18F, 0.10F, 60, 64, 72, 255, packedLight);

        // Flush geometry before drawing LCD text on both front and back faces so text is never overwritten
        if (buffers instanceof MultiBufferSource.BufferSource bs) bs.endBatch();
        renderLcdScreenFace(pump, poseStack, buffers, 0.285F, 0.0F);
        renderLcdScreenFace(pump, poseStack, buffers, -0.285F, 180.0F);
        poseStack.popPose();

        renderContinuousHoseAndPistol(pump, partialTick, poseStack, buffers, packedLight);
        super.render(pump, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private void renderLcdScreenFace(
        FuelPumpEntity pump, PoseStack poseStack, MultiBufferSource buffers, float zOffset, float rotYDeg
    ) {
        Font font = this.getFont();
        FuelType type = pump.selectedFuelType();
        float liters = pump.dispensedLiters();
        double unitPrice = type.basePricePerUnit();
        double totalPrice = liters * unitPrice;

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.76F, zOffset);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotYDeg));
        poseStack.scale(-0.011F, -0.011F, 0.011F);
        Matrix4f mat = poseStack.last().pose();

        String priceTxt = String.format(Locale.ROOT, "PRIX: %5.2f EUR", totalPrice);
        String literTxt = String.format(Locale.ROOT, "LITRES: %5.2f L", liters);
        String rateTxt = String.format(Locale.ROOT, "%.2f EUR/L", unitPrice);

        font.drawInBatch(priceTxt, -font.width(priceTxt) * 0.5F, 0, 0xFF66FF66, false, mat, buffers, Font.DisplayMode.POLYGON_OFFSET, 0, FULL_BRIGHT);
        font.drawInBatch(literTxt, -font.width(literTxt) * 0.5F, 12, 0xFF4FC3F7, false, mat, buffers, Font.DisplayMode.POLYGON_OFFSET, 0, FULL_BRIGHT);
        font.drawInBatch(rateTxt, -font.width(rateTxt) * 0.5F, 24, 0xFFFFD54F, false, mat, buffers, Font.DisplayMode.POLYGON_OFFSET, 0, FULL_BRIGHT);
        font.drawInBatch("SP98", -26, 42, 0xFFFFFFFF, false, mat, buffers, Font.DisplayMode.POLYGON_OFFSET, 0, FULL_BRIGHT);
        font.drawInBatch("DIESEL", 4, 42, 0xFFFFFFFF, false, mat, buffers, Font.DisplayMode.POLYGON_OFFSET, 0, FULL_BRIGHT);
        poseStack.popPose();
    }

    private void renderContinuousHoseAndPistol(
        FuelPumpEntity pump, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        double px = Mth.lerp(partialTick, pump.xOld, pump.getX());
        double py = Mth.lerp(partialTick, pump.yOld, pump.getY());
        double pz = Mth.lerp(partialTick, pump.zOld, pump.getZ());

        double yawRad = Math.toRadians(-pump.getYRot());
        Vec3d hoseAnchor = new Vec3d(px - 0.48 * Math.cos(yawRad), py + 1.48, pz + 0.48 * Math.sin(yawRad));
        Vec3d nozzleWorld = new Vec3d(hoseAnchor.x(), py + 1.00, hoseAnchor.z());
        float nozzleYawDeg = pump.getYRot();
        boolean inCar = false;

        if (pump.connectedVehicleId() >= 0 && mc.level.getEntity(pump.connectedVehicleId()) instanceof VehicleEntity car) {
            nozzleWorld = FuelPumpEntity.fuelCapWorldPos(car);
            nozzleYawDeg = car.getYRot() + 90.0F;
            inCar = true;
        } else if (pump.nozzleHolderId() >= 0) {
            Entity holder = mc.level.getEntity(pump.nozzleHolderId());
            if (holder != null) {
                double hx = Mth.lerp(partialTick, holder.xOld, holder.getX());
                double hy = Mth.lerp(partialTick, holder.yOld, holder.getY()) + 0.92;
                double hz = Mth.lerp(partialTick, holder.zOld, holder.getZ());
                double hYaw = Math.toRadians(-holder.getYRot());
                nozzleWorld = new Vec3d(hx + Math.sin(hYaw) * 0.45 - Math.cos(hYaw) * 0.28, hy, hz + Math.cos(hYaw) * 0.45 + Math.sin(hYaw) * 0.28);
                nozzleYawDeg = holder.getYRot();
            }
        }

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(ProvisionalRenderBackend.WHITE_TEXTURE));
        Matrix4f mat = poseStack.last().pose();
        // Continuous 24-segment catenary rubber hose tube (connected quad strips, zero floating dots)
        int segs = 24;
        float r = 0.022F;
        Vec3d prev = new Vec3d(hoseAnchor.x() - px, hoseAnchor.y() - py, hoseAnchor.z() - pz);
        for (int i = 1; i <= segs; i++) {
            double t = (double) i / segs;
            double sag = Math.sin(t * Math.PI) * 0.42;
            Vec3d curr = new Vec3d(
                Mth.lerp(t, hoseAnchor.x(), nozzleWorld.x()) - px,
                Mth.lerp(t, hoseAnchor.y(), nozzleWorld.y()) - sag - py,
                Mth.lerp(t, hoseAnchor.z(), nozzleWorld.z()) - pz
            );
            drawTubeSegment(vc, mat, prev, curr, r, light);
            prev = curr;
        }

        // Detailed 5-Part 3D Fuel Pistol Nozzle (ergonomic angled grip, trigger guard, trigger, boot body, curved chrome spout)
        poseStack.pushPose();
        poseStack.translate(nozzleWorld.x() - px, nozzleWorld.y() - py, nozzleWorld.z() - pz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-nozzleYawDeg));
        if (inCar) poseStack.mulPose(Axis.XP.rotationDegrees(22.0F));
        boolean isGas = pump.selectedFuelType() == FuelType.GASOLINE;
        int cr = isGas ? 30 : 235, cg = isGas ? 185 : 175, cb = isGas ? 60 : 18;
        // 1. Insulated colored main valve body
        ProvisionalMeshBuilder.drawBox(poseStack.last(), vc, -0.024F, -0.022F, -0.06F, 0.024F, 0.036F, 0.08F, cr, cg, cb, 255, light);
        // 2. Angled ergonomic black rubber pistol handle
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.055F, -0.045F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
        ProvisionalMeshBuilder.drawBox(poseStack.last(), vc, -0.018F, -0.055F, -0.018F, 0.018F, 0.045F, 0.018F, 26, 28, 32, 255, light);
        poseStack.popPose();
        // 3. Lower trigger guard loop & inner metallic trigger lever
        ProvisionalMeshBuilder.drawBox(poseStack.last(), vc, -0.014F, -0.075F, -0.04F, 0.014F, -0.062F, 0.055F, cr, cg, cb, 255, light);
        ProvisionalMeshBuilder.drawBox(poseStack.last(), vc, -0.014F, -0.075F, 0.042F, 0.014F, -0.022F, 0.055F, cr, cg, cb, 255, light);
        ProvisionalMeshBuilder.drawBox(poseStack.last(), vc, -0.006F, -0.052F, -0.01F, 0.006F, -0.022F, 0.025F, 195, 202, 210, 255, light);
        // 4. Curved stainless-steel fuel spout
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.010F, 0.08F);
        poseStack.mulPose(Axis.XP.rotationDegrees(14.0F));
        ProvisionalMeshBuilder.drawBox(poseStack.last(), vc, -0.011F, -0.011F, 0.00F, 0.011F, 0.011F, 0.19F, 215, 222, 232, 255, light);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void drawTubeSegment(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, float r, int light) {
        emitQuad(vc, m, a.x() - r, a.y() + r, a.z(), b.x() - r, b.y() + r, b.z(), b.x() + r, b.y() + r, b.z(), a.x() + r, a.y() + r, a.z(), 32, 35, 40, light);
        emitQuad(vc, m, a.x() - r, a.y() - r, a.z(), a.x() - r, a.y() + r, a.z(), b.x() - r, b.y() + r, b.z(), b.x() - r, b.y() - r, b.z(), 20, 22, 26, light);
        emitQuad(vc, m, a.x() + r, a.y() - r, a.z(), b.x() + r, b.y() - r, b.z(), b.x() + r, b.y() + r, b.z(), a.x() + r, a.y() + r, a.z(), 24, 26, 30, light);
    }

    private static void emitQuad(
        VertexConsumer vc, Matrix4f m, double x1, double y1, double z1, double x2, double y2, double z2,
        double x3, double y3, double z3, double x4, double y4, double z4, int r, int g, int b, int light
    ) {
        vc.addVertex(m, (float) x1, (float) y1, (float) z1).setColor(r, g, b, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        vc.addVertex(m, (float) x2, (float) y2, (float) z2).setColor(r, g, b, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        vc.addVertex(m, (float) x3, (float) y3, (float) z3).setColor(r, g, b, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        vc.addVertex(m, (float) x4, (float) y4, (float) z4).setColor(r, g, b, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(FuelPumpEntity entity) {
        return ProvisionalRenderBackend.WHITE_TEXTURE;
    }
}
