package com.realisticvehicles.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.realisticvehicles.render.glb.GlbMaterial;
import com.realisticvehicles.render.glb.GlbMeshPrimitive;
import com.realisticvehicles.render.glb.GlbModel;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/**
 * Static GPU-retained VertexBuffer cache for a GlbModel with real-time 3D crash dent deformation support.
 */
public final class RetainedGlbMesh implements AutoCloseable {
    private static final int FULL_BRIGHT = 15728880;

    private VertexBuffer chassisOpaque;
    private VertexBuffer chassisGlass;
    private final VertexBuffer wheelFl, wheelFr, wheelRl, wheelRr;
    private VertexBuffer leftDoorOpaque, leftDoorGlass;
    private VertexBuffer rightDoorOpaque, rightDoorGlass;
    private VertexBuffer hoodOpaque;
    private VertexBuffer trunkOpaque, trunkGlass;
    private final GlbModel source;
    private final float[] activeDamage = new float[4];

    public RetainedGlbMesh(GlbModel model) {
        this.source = model;
        List<GlbMaterial> mats = model.materials();
        this.chassisOpaque = uploadPrimitives(model.chassisPrimitives(), mats, false, true);
        this.chassisGlass = uploadPrimitives(model.chassisPrimitives(), mats, true, true);
        this.wheelFl = uploadPrimitives(model.frontLeftWheel().primitives(), mats, false, false);
        this.wheelFr = uploadPrimitives(model.frontRightWheel().primitives(), mats, false, false);
        this.wheelRl = uploadPrimitives(model.rearLeftWheel().primitives(), mats, false, false);
        this.wheelRr = uploadPrimitives(model.rearRightWheel().primitives(), mats, false, false);
        this.leftDoorOpaque = uploadPrimitives(model.leftDoor().primitives(), mats, false, false);
        this.leftDoorGlass = uploadPrimitives(model.leftDoor().primitives(), mats, true, false);
        this.rightDoorOpaque = uploadPrimitives(model.rightDoor().primitives(), mats, false, false);
        this.rightDoorGlass = uploadPrimitives(model.rightDoor().primitives(), mats, true, false);
        this.hoodOpaque = uploadPrimitives(model.hood().primitives(), mats, false, false);
        this.trunkOpaque = uploadPrimitives(model.trunk().primitives(), mats, false, false);
        this.trunkGlass = uploadPrimitives(model.trunk().primitives(), mats, true, false);
    }

    /** Rebuilds body & panel VBOs with localized crumple-zone vertex deformation only when a new crash impact exceeds 10%. */
    public void updateDamageDeformation(float front, float rear, float left, float right) {
        if (Math.abs(front - activeDamage[0]) < 0.10F && Math.abs(rear - activeDamage[1]) < 0.10F
            && Math.abs(left - activeDamage[2]) < 0.10F && Math.abs(right - activeDamage[3]) < 0.10F) return;
        activeDamage[0] = front; activeDamage[1] = rear; activeDamage[2] = left; activeDamage[3] = right;
        List<GlbMaterial> mats = source.materials();
        for (VertexBuffer b : new VertexBuffer[] { chassisOpaque, chassisGlass, leftDoorOpaque, leftDoorGlass, rightDoorOpaque, rightDoorGlass, hoodOpaque, trunkOpaque, trunkGlass }) if (b != null) b.close();
        this.chassisOpaque = uploadPrimitives(source.chassisPrimitives(), mats, false, true);
        this.chassisGlass = uploadPrimitives(source.chassisPrimitives(), mats, true, true);
        this.leftDoorOpaque = uploadPrimitives(source.leftDoor().primitives(), mats, false, false);
        this.leftDoorGlass = uploadPrimitives(source.leftDoor().primitives(), mats, true, false);
        this.rightDoorOpaque = uploadPrimitives(source.rightDoor().primitives(), mats, false, false);
        this.rightDoorGlass = uploadPrimitives(source.rightDoor().primitives(), mats, true, false);
        this.hoodOpaque = uploadPrimitives(source.hood().primitives(), mats, false, false);
        this.trunkOpaque = uploadPrimitives(source.trunk().primitives(), mats, false, false);
        this.trunkGlass = uploadPrimitives(source.trunk().primitives(), mats, true, false);
    }

    public void drawOpaqueChassis(Matrix4f mv, Matrix4f proj, ShaderInstance shader) { drawBuffer(chassisOpaque, mv, proj, shader); }
    public void drawGlassChassis(Matrix4f mv, Matrix4f proj, ShaderInstance shader) { drawBuffer(chassisGlass, mv, proj, shader); }

    public void drawWheel(int index, Matrix4f mv, Matrix4f proj, ShaderInstance shader) {
        drawBuffer(switch (index) { case 0 -> wheelFl; case 1 -> wheelFr; case 2 -> wheelRl; default -> wheelRr; }, mv, proj, shader);
    }

    public void drawHingedOpaque(int partIdx, Matrix4f mv, Matrix4f proj, ShaderInstance shader) {
        drawBuffer(switch (partIdx) { case 0 -> leftDoorOpaque; case 1 -> rightDoorOpaque; case 2 -> hoodOpaque; default -> trunkOpaque; }, mv, proj, shader);
    }

    public void drawHingedGlass(int partIdx, Matrix4f mv, Matrix4f proj, ShaderInstance shader) {
        drawBuffer(switch (partIdx) { case 0 -> leftDoorGlass; case 1 -> rightDoorGlass; case 3 -> trunkGlass; default -> null; }, mv, proj, shader);
    }

    private void drawBuffer(VertexBuffer buf, Matrix4f mv, Matrix4f proj, ShaderInstance shader) {
        if (buf == null || shader == null) return;
        buf.bind();
        buf.drawWithShader(mv, proj, shader);
        VertexBuffer.unbind();
    }

    private VertexBuffer uploadPrimitives(
        List<GlbMeshPrimitive> prims, List<GlbMaterial> mats, boolean glassOnly, boolean filterCockpit
    ) {
        if (prims.isEmpty()) return null;
        try (ByteBufferBuilder byteBuf = new ByteBufferBuilder(2 * 1024 * 1024)) {
            BufferBuilder builder = new BufferBuilder(byteBuf, VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.NEW_ENTITY);
            boolean hasVertices = false;
            for (GlbMeshPrimitive prim : prims) {
                String lowerNode = prim.nodeName().toLowerCase(Locale.ROOT);
                if (filterCockpit && VehicleCockpitAnimator.isAnimatedCockpitNode(lowerNode)) continue;
                GlbMaterial mat = (prim.materialIndex() >= 0 && prim.materialIndex() < mats.size())
                    ? mats.get(prim.materialIndex()) : GlbMaterial.DEFAULT;
                boolean isGlass = GlbMaterialPalette.isGlass(mat);
                if (isGlass != glassOnly) continue;
                int[] rgba = GlbMaterialPalette.resolveRgba(mat, isGlass);
                if (rgba[3] < 16) continue;
                boolean isCarpaint = mat.name().toLowerCase(Locale.ROOT).contains("carpaint")
                    || mat.name().toLowerCase(Locale.ROOT).contains("chrome");
                emitPrimitive(builder, prim, rgba, isGlass, isCarpaint, filterCockpit);
                hasVertices = true;
            }
            if (!hasVertices) return null;
            MeshData mesh = builder.build();
            if (mesh == null) return null;
            VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vbo.bind();
            vbo.upload(mesh);
            VertexBuffer.unbind();
            return vbo;
        }
    }

    private void emitPrimitive(
        BufferBuilder vc, GlbMeshPrimitive prim, int[] rgba, boolean isGlass, boolean isCarpaint, boolean applyDent
    ) {
        float[] pos = prim.positions(), norm = prim.normals(), uvs = prim.uvs();
        int[] indices = prim.indices();
        for (int i = 0; i + 2 < indices.length; i += 3) {
            emitVert(vc, pos, norm, uvs, indices[i], rgba, isGlass, isCarpaint, applyDent);
            emitVert(vc, pos, norm, uvs, indices[i + 1], rgba, isGlass, isCarpaint, applyDent);
            emitVert(vc, pos, norm, uvs, indices[i + 2], rgba, isGlass, isCarpaint, applyDent);
        }
    }

    public static float applyDentToPoint(float[] pt, float[] dmg) {
        float x = pt[0], y = pt[1], z = pt[2], scuff = 0.0F;
        if (dmg[0] > 0.02F && z > 1.80F) {
            float pen = (z - 1.80F) / 1.25F, crush = pen * pen * (float) Math.exp(-x * x * 1.1F) * dmg[0];
            z -= crush * 0.42F; y += crush * 0.07F * Math.max(0.0F, 0.35F - y); scuff = Math.max(scuff, crush);
        }
        if (dmg[1] > 0.02F && z < -1.80F) {
            float pen = (-1.80F - z) / 1.25F, crush = pen * pen * (float) Math.exp(-x * x * 1.1F) * dmg[1];
            z += crush * 0.38F; y += crush * 0.06F * Math.max(0.0F, 0.35F - y); scuff = Math.max(scuff, crush);
        }
        if (dmg[2] > 0.02F && x > 0.85F && Math.abs(z) < 1.85F) {
            float crush = ((x - 0.85F) / 0.65F) * (float) Math.exp(-z * z * 0.55F) * dmg[2];
            x -= crush * 0.26F; scuff = Math.max(scuff, crush);
        }
        if (dmg[3] > 0.02F && x < -0.85F && Math.abs(z) < 1.85F) {
            float crush = ((-0.85F - x) / 0.65F) * (float) Math.exp(-z * z * 0.55F) * dmg[3];
            x += crush * 0.26F; scuff = Math.max(scuff, crush);
        }
        pt[0] = x; pt[1] = y; pt[2] = z;
        return scuff;
    }

    private void emitVert(
        BufferBuilder vc, float[] pos, float[] norm, float[] uvs, int idx, int[] rgba,
        boolean isGlass, boolean isCarpaint, boolean applyDent
    ) {
        int p3 = idx * 3, p2 = idx * 2;
        float[] pt = { pos[p3], pos[p3 + 1], pos[p3 + 2] };
        float nx = norm[p3], ny = norm[p3 + 1], nz = norm[p3 + 2];
        float scuff = applyDent ? applyDentToPoint(pt, activeDamage) : 0.0F;
        float x = pt[0], y = pt[1], z = pt[2];

        float sunDot = Math.max(0.0F, nx * 0.38F + ny * 0.82F + nz * 0.42F);
        float hemiAo = 0.55F + 0.45F * Math.max(-0.6F, Math.min(1.0F, (y + 0.35F) * 1.1F));
        float shade = (0.34F + 0.66F * sunDot) * hemiAo * (1.0F - scuff * 0.38F);
        float spec = isCarpaint ? (float) Math.pow(sunDot, 12.0) * 38.0F : 0.0F;
        if (isGlass) {
            shade *= 0.72F + 0.28F * (1.0F - Math.abs(ny));
            spec = (float) Math.pow(sunDot, 16.0) * 55.0F;
        }
        int r = Math.min(255, Math.round(rgba[0] * shade + spec));
        int g = Math.min(255, Math.round(rgba[1] * shade + spec));
        int b = Math.min(255, Math.round(rgba[2] * shade + spec));
        float u = p2 + 1 < uvs.length ? uvs[p2] : 0.0F, v = p2 + 1 < uvs.length ? uvs[p2 + 1] : 0.0F;
        vc.addVertex(x, y, z).setColor(r, g, b, rgba[3]).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(nx, ny, nz);
    }

    @Override
    public void close() {
        RenderSystem.recordRenderCall(() -> {
            for (VertexBuffer b : new VertexBuffer[] {
                chassisOpaque, chassisGlass, wheelFl, wheelFr, wheelRl, wheelRr,
                leftDoorOpaque, leftDoorGlass, rightDoorOpaque, rightDoorGlass, hoodOpaque, trunkOpaque, trunkGlass
            }) {
                if (b != null) b.close();
            }
        });
    }
}
