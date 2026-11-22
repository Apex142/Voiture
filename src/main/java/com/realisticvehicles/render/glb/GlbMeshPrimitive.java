package com.realisticvehicles.render.glb;

/**
 * Indexed triangle mesh primitive in car-local or wheel-hub-local space.
 */
public record GlbMeshPrimitive(
    String nodeName,
    float[] positions,
    float[] normals,
    float[] uvs,
    int[] indices,
    int materialIndex
) {
    public int vertexCount() {
        return positions.length / 3;
    }

    public int triangleCount() {
        return indices.length / 3;
    }
}
