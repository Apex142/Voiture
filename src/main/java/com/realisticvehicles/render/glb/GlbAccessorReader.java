package com.realisticvehicles.render.glb;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Low-level glTF 2.0 binary accessor decoder and 4x4 column-major transform math.
 */
public final class GlbAccessorReader {
    private GlbAccessorReader() {
    }

    public static float[] readFloatVec(JsonObject root, byte[] bin, int accessorIndex, int components) {
        JsonObject acc = root.getAsJsonArray("accessors").get(accessorIndex).getAsJsonObject();
        int count = acc.get("count").getAsInt();
        int bvIdx = acc.get("bufferView").getAsInt();
        JsonObject bv = root.getAsJsonArray("bufferViews").get(bvIdx).getAsJsonObject();

        int baseOffset = (bv.has("byteOffset") ? bv.get("byteOffset").getAsInt() : 0)
            + (acc.has("byteOffset") ? acc.get("byteOffset").getAsInt() : 0);
        int defaultStride = components * Float.BYTES;
        int stride = bv.has("byteStride") ? bv.get("byteStride").getAsInt() : defaultStride;

        ByteBuffer buf = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN);
        float[] out = new float[count * components];
        for (int i = 0; i < count; i++) {
            int pos = baseOffset + i * stride;
            for (int c = 0; c < components; c++) {
                out[i * components + c] = buf.getFloat(pos + c * Float.BYTES);
            }
        }
        return out;
    }

    public static int[] readIndices(JsonObject root, byte[] bin, int accessorIndex) {
        JsonObject acc = root.getAsJsonArray("accessors").get(accessorIndex).getAsJsonObject();
        int count = acc.get("count").getAsInt();
        int compType = acc.get("componentType").getAsInt();
        int bvIdx = acc.get("bufferView").getAsInt();
        JsonObject bv = root.getAsJsonArray("bufferViews").get(bvIdx).getAsJsonObject();
        int baseOffset = (bv.has("byteOffset") ? bv.get("byteOffset").getAsInt() : 0)
            + (acc.has("byteOffset") ? acc.get("byteOffset").getAsInt() : 0);

        ByteBuffer buf = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN);
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            out[i] = switch (compType) {
                case 5121 -> buf.get(baseOffset + i) & 0xFF;
                case 5123 -> buf.getShort(baseOffset + i * 2) & 0xFFFF;
                case 5125 -> buf.getInt(baseOffset + i * 4);
                default -> throw new IllegalStateException("Unsupported glTF index componentType: " + compType);
            };
        }
        return out;
    }

    public static float[] nodeLocalMatrix(JsonObject node) {
        if (node.has("matrix")) {
            JsonArray m = node.getAsJsonArray("matrix");
            float[] mat = new float[16];
            for (int i = 0; i < 16; i++) {
                mat[i] = m.get(i).getAsFloat();
            }
            return mat;
        }
        float tx = 0, ty = 0, tz = 0;
        if (node.has("translation")) {
            JsonArray t = node.getAsJsonArray("translation");
            tx = t.get(0).getAsFloat();
            ty = t.get(1).getAsFloat();
            tz = t.get(2).getAsFloat();
        }
        float qx = 0, qy = 0, qz = 0, qw = 1;
        if (node.has("rotation")) {
            JsonArray r = node.getAsJsonArray("rotation");
            qx = r.get(0).getAsFloat();
            qy = r.get(1).getAsFloat();
            qz = r.get(2).getAsFloat();
            qw = r.get(3).getAsFloat();
        }
        float sx = 1, sy = 1, sz = 1;
        if (node.has("scale")) {
            JsonArray s = node.getAsJsonArray("scale");
            sx = s.get(0).getAsFloat();
            sy = s.get(1).getAsFloat();
            sz = s.get(2).getAsFloat();
        }
        return fromTrs(tx, ty, tz, qx, qy, qz, qw, sx, sy, sz);
    }

    public static float[] identity() {
        return new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
    }

    public static float[] multiply(float[] a, float[] b) {
        float[] r = new float[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                r[col * 4 + row] = a[row] * b[col * 4]
                    + a[4 + row] * b[col * 4 + 1]
                    + a[8 + row] * b[col * 4 + 2]
                    + a[12 + row] * b[col * 4 + 3];
            }
        }
        return r;
    }

    private static float[] fromTrs(
        float tx, float ty, float tz,
        float qx, float qy, float qz, float qw,
        float sx, float sy, float sz
    ) {
        float xx = qx * qx, yy = qy * qy, zz = qz * qz;
        float xy = qx * qy, xz = qx * qz, yz = qy * qz;
        float wx = qw * qx, wy = qw * qy, wz = qw * qz;
        return new float[] {
            (1 - 2 * (yy + zz)) * sx, (2 * (xy + wz)) * sx, (2 * (xz - wy)) * sx, 0,
            (2 * (xy - wz)) * sy, (1 - 2 * (xx + zz)) * sy, (2 * (yz + wx)) * sy, 0,
            (2 * (xz + wy)) * sz, (2 * (yz - wx)) * sz, (1 - 2 * (xx + yy)) * sz, 0,
            tx, ty, tz, 1
        };
    }
}
