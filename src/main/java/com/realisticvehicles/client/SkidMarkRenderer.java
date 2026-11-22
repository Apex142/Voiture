package com.realisticvehicles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/**
 * Records and renders 3D rubber tire skid marks on the ground during drifting, hard braking, and burnouts.
 */
public final class SkidMarkRenderer {
    private static final int MAX_SEGMENTS = 512;
    private static final long LIFETIME_MS = 18_000L;
    private static final Deque<SkidSegment> SEGMENTS = new ArrayDeque<>(MAX_SEGMENTS);
    private static final Vec3d[] LAST_WHEEL_POS = new Vec3d[4];

    private record SkidSegment(
        double x1, double y1, double z1,
        double x2, double y2, double z2,
        double nx, double nz,
        float intensity,
        long timestampMs
    ) {
    }

    private SkidMarkRenderer() {
    }

    public static void recordWheelSkid(int wheelSlot, Vec3d worldHubPos, double wheelRadius, Quat4d carRot, float slipIntensity) {
        if (slipIntensity < 0.18F) {
            LAST_WHEEL_POS[wheelSlot] = null;
            return;
        }
        Vec3d contact = new Vec3d(worldHubPos.x(), worldHubPos.y() - wheelRadius + 0.025, worldHubPos.z());
        Vec3d prev = LAST_WHEEL_POS[wheelSlot];
        if (prev != null) {
            double distSq = prev.sub(contact).lengthSquared();
            if (distSq > 0.015 && distSq < 2.2) {
                Vec3d left = carRot.left();
                if (SEGMENTS.size() >= MAX_SEGMENTS) {
                    SEGMENTS.pollFirst();
                }
                SEGMENTS.addLast(new SkidSegment(
                    prev.x(), prev.y(), prev.z(),
                    contact.x(), contact.y(), contact.z(),
                    left.x() * 0.14, left.z() * 0.14,
                    Math.min(0.88F, slipIntensity),
                    System.currentTimeMillis()
                ));
                LAST_WHEEL_POS[wheelSlot] = contact;
            } else if (distSq >= 2.2) {
                LAST_WHEEL_POS[wheelSlot] = contact;
            }
        } else {
            LAST_WHEEL_POS[wheelSlot] = contact;
        }
    }

    public static void renderSkidMarks(
        PoseStack poseStack,
        MultiBufferSource buffers,
        double camOriginX,
        double camOriginY,
        double camOriginZ,
        int packedLight
    ) {
        if (SEGMENTS.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(ProvisionalRenderBackend.WHITE_TEXTURE));
        Matrix4f mat = poseStack.last().pose();

        Iterator<SkidSegment> it = SEGMENTS.iterator();
        while (it.hasNext()) {
            SkidSegment seg = it.next();
            long age = now - seg.timestampMs();
            if (age > LIFETIME_MS) {
                it.remove();
                continue;
            }
            float fade = 1.0F - (float) age / LIFETIME_MS;
            int alpha = Math.max(0, Math.min(220, Math.round(seg.intensity() * fade * 210.0F)));
            if (alpha < 6) {
                continue;
            }

            float ax = (float) (seg.x1() - seg.nx() - camOriginX);
            float ay = (float) (seg.y1() - camOriginY);
            float az = (float) (seg.z1() - seg.nz() - camOriginZ);

            float bx = (float) (seg.x1() + seg.nx() - camOriginX);
            float by = (float) (seg.y1() - camOriginY);
            float bz = (float) (seg.z1() + seg.nz() - camOriginZ);

            float cx = (float) (seg.x2() + seg.nx() - camOriginX);
            float cy = (float) (seg.y2() - camOriginY);
            float cz = (float) (seg.z2() + seg.nz() - camOriginZ);

            float dx = (float) (seg.x2() - seg.nx() - camOriginX);
            float dy = (float) (seg.y2() - camOriginY);
            float dz = (float) (seg.z2() - seg.nz() - camOriginZ);

            emitVert(vc, mat, ax, ay, az, alpha, packedLight);
            emitVert(vc, mat, bx, by, bz, alpha, packedLight);
            emitVert(vc, mat, cx, cy, cz, alpha, packedLight);
            emitVert(vc, mat, dx, dy, dz, alpha, packedLight);
        }
    }

    private static void emitVert(VertexConsumer vc, Matrix4f mat, float x, float y, float z, int alpha, int light) {
        vc.addVertex(mat, x, y, z)
            .setColor(16, 16, 18, alpha)
            .setUv(0.0F, 0.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0.0F, 1.0F, 0.0F);
    }
}
