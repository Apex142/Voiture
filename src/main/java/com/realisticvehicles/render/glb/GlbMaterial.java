package com.realisticvehicles.render.glb;

/**
 * PBR material extracted from a binary glTF 2.0 (GLB) file.
 */
public record GlbMaterial(
    String name,
    float r,
    float g,
    float b,
    float a,
    float metallic,
    float roughness,
    byte[] baseColorImageBytes
) {
    public static final GlbMaterial DEFAULT = new GlbMaterial(
        "default", 0.8F, 0.8F, 0.82F, 1.0F, 0.2F, 0.5F, null
    );
}
