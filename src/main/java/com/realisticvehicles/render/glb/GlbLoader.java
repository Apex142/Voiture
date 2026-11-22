package com.realisticvehicles.render.glb;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses binary glTF 2.0 (.glb) files into chassis + 4 articulated wheel parts.
 */
public final class GlbLoader {
    private static final int GLB_MAGIC = 0x46546C67;
    private static final int CHUNK_JSON = 0x4E4F534A;
    private static final int CHUNK_BIN = 0x004E4942;

    public GlbModel load(InputStream stream) throws IOException {
        byte[] allBytes = stream.readAllBytes();
        ByteBuffer buf = ByteBuffer.wrap(allBytes).order(ByteOrder.LITTLE_ENDIAN);
        int magic = buf.getInt();
        int version = buf.getInt();
        buf.getInt(); // total length
        if (magic != GLB_MAGIC || version != 2) {
            throw new IOException("Invalid GLB header");
        }

        String jsonText = null;
        byte[] binChunk = new byte[0];
        while (buf.remaining() >= 8) {
            int chunkLen = buf.getInt();
            int chunkType = buf.getInt();
            byte[] data = new byte[chunkLen];
            buf.get(data);
            if (chunkType == CHUNK_JSON) {
                jsonText = new String(data, StandardCharsets.UTF_8);
            } else if (chunkType == CHUNK_BIN) {
                binChunk = data;
            }
        }
        if (jsonText == null) {
            throw new IOException("Missing GLB JSON chunk");
        }

        JsonObject root = JsonParser.parseString(jsonText).getAsJsonObject();
        List<GlbMaterial> materials = parseMaterials(root, binChunk);
        List<GlbMeshPrimitive> rawPrimitives = new ArrayList<>();
        traverseScene(root, binChunk, rawPrimitives);
        return assembleModel(rawPrimitives, materials);
    }

    private List<GlbMaterial> parseMaterials(JsonObject root, byte[] bin) {
        List<GlbMaterial> list = new ArrayList<>();
        if (!root.has("materials")) {
            return list;
        }
        JsonArray mats = root.getAsJsonArray("materials");
        for (int i = 0; i < mats.size(); i++) {
            JsonObject m = mats.get(i).getAsJsonObject();
            String name = m.has("name") ? m.get("name").getAsString() : "mat_" + i;
            float r = 0.78F, g = 0.78F, b = 0.80F, a = 1.0F, met = 0.25F, rough = 0.5F;
            byte[] texBytes = null;
            if (m.has("pbrMetallicRoughness")) {
                JsonObject pbr = m.getAsJsonObject("pbrMetallicRoughness");
                if (pbr.has("baseColorFactor")) {
                    JsonArray c = pbr.getAsJsonArray("baseColorFactor");
                    r = c.get(0).getAsFloat();
                    g = c.get(1).getAsFloat();
                    b = c.get(2).getAsFloat();
                    a = c.get(3).getAsFloat();
                }
                if (pbr.has("metallicFactor")) {
                    met = pbr.get("metallicFactor").getAsFloat();
                }
                if (pbr.has("roughnessFactor")) {
                    rough = pbr.get("roughnessFactor").getAsFloat();
                }
                if (pbr.has("baseColorTexture")) {
                    int texIdx = pbr.getAsJsonObject("baseColorTexture").get("index").getAsInt();
                    texBytes = extractTextureBytes(root, bin, texIdx);
                }
            }
            list.add(new GlbMaterial(name, r, g, b, a, met, rough, texBytes));
        }
        return list;
    }

    private byte[] extractTextureBytes(JsonObject root, byte[] bin, int textureIndex) {
        if (!root.has("textures") || !root.has("images")) {
            return null;
        }
        JsonObject tex = root.getAsJsonArray("textures").get(textureIndex).getAsJsonObject();
        if (!tex.has("source")) {
            return null;
        }
        JsonObject img = root.getAsJsonArray("images").get(tex.get("source").getAsInt()).getAsJsonObject();
        if (!img.has("bufferView")) {
            return null;
        }
        JsonObject bv = root.getAsJsonArray("bufferViews").get(img.get("bufferView").getAsInt()).getAsJsonObject();
        int offset = bv.has("byteOffset") ? bv.get("byteOffset").getAsInt() : 0;
        int len = bv.get("byteLength").getAsInt();
        byte[] out = new byte[len];
        System.arraycopy(bin, offset, out, 0, len);
        return out;
    }

    private void traverseScene(JsonObject root, byte[] bin, List<GlbMeshPrimitive> out) {
        if (!root.has("nodes") || !root.has("scenes")) {
            return;
        }
        int sceneIdx = root.has("scene") ? root.get("scene").getAsInt() : 0;
        JsonArray rootNodes = root.getAsJsonArray("scenes").get(sceneIdx).getAsJsonObject().getAsJsonArray("nodes");
        float[] identity = GlbAccessorReader.identity();
        for (int i = 0; i < rootNodes.size(); i++) {
            visitNode(root, bin, rootNodes.get(i).getAsInt(), identity, out);
        }
    }

    private void visitNode(
        JsonObject root, byte[] bin, int nodeIdx, float[] parentMat, List<GlbMeshPrimitive> out
    ) {
        JsonObject node = root.getAsJsonArray("nodes").get(nodeIdx).getAsJsonObject();
        String name = node.has("name") ? node.get("name").getAsString() : "node_" + nodeIdx;
        float[] worldMat = GlbAccessorReader.multiply(parentMat, GlbAccessorReader.nodeLocalMatrix(node));
        if (node.has("mesh")) {
            readMesh(root, bin, node.get("mesh").getAsInt(), name, worldMat, out);
        }
        if (node.has("children")) {
            JsonArray children = node.getAsJsonArray("children");
            for (int i = 0; i < children.size(); i++) {
                visitNode(root, bin, children.get(i).getAsInt(), worldMat, out);
            }
        }
    }

    private void readMesh(
        JsonObject root, byte[] bin, int meshIdx, String nodeName, float[] mat, List<GlbMeshPrimitive> out
    ) {
        JsonArray primitives = root.getAsJsonArray("meshes").get(meshIdx).getAsJsonObject().getAsJsonArray("primitives");
        for (int i = 0; i < primitives.size(); i++) {
            JsonObject prim = primitives.get(i).getAsJsonObject();
            JsonObject attrs = prim.getAsJsonObject("attributes");
            if (!attrs.has("POSITION") || !prim.has("indices")) {
                continue;
            }
            float[] rawPos = GlbAccessorReader.readFloatVec(root, bin, attrs.get("POSITION").getAsInt(), 3);
            float[] rawNorm = attrs.has("NORMAL")
                ? GlbAccessorReader.readFloatVec(root, bin, attrs.get("NORMAL").getAsInt(), 3)
                : new float[rawPos.length];
            float[] uvs = attrs.has("TEXCOORD_0")
                ? GlbAccessorReader.readFloatVec(root, bin, attrs.get("TEXCOORD_0").getAsInt(), 2)
                : new float[(rawPos.length / 3) * 2];
            int[] indices = GlbAccessorReader.readIndices(root, bin, prim.get("indices").getAsInt());
            int matIdx = prim.has("material") ? prim.get("material").getAsInt() : -1;

            transformVertices(rawPos, rawNorm, mat);
            out.add(new GlbMeshPrimitive(nodeName, rawPos, rawNorm, uvs, indices, matIdx));
        }
    }

    private void transformVertices(float[] pos, float[] norm, float[] m) {
        for (int i = 0; i < pos.length; i += 3) {
            float x = pos[i], y = pos[i + 1], z = pos[i + 2];
            pos[i] = m[0] * x + m[4] * y + m[8] * z + m[12];
            pos[i + 1] = m[1] * x + m[5] * y + m[9] * z + m[13];
            pos[i + 2] = m[2] * x + m[6] * y + m[10] * z + m[14];

            float nx = norm[i], ny = norm[i + 1], nz = norm[i + 2];
            float tnx = m[0] * nx + m[4] * ny + m[8] * nz;
            float tny = m[1] * nx + m[5] * ny + m[9] * nz;
            float tnz = m[2] * nx + m[6] * ny + m[10] * nz;
            float len = (float) Math.sqrt(tnx * tnx + tny * tny + tnz * tnz);
            if (len > 1e-6F) {
                norm[i] = tnx / len;
                norm[i + 1] = tny / len;
                norm[i + 2] = tnz / len;
            }
        }
    }

    private GlbModel assembleModel(List<GlbMeshPrimitive> raw, List<GlbMaterial> materials) {
        return GlbModelAssembler.assemble(raw, materials);
    }
}
