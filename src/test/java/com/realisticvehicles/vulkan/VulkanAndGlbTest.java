package com.realisticvehicles.vulkan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.realisticvehicles.render.glb.GlbLoader;
import com.realisticvehicles.render.glb.GlbMeshPrimitive;
import com.realisticvehicles.render.glb.GlbModel;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

final class VulkanAndGlbTest {

    @Test
    void vulkanBackendInitializesAllocatesAndFreesWithoutLeak() {
        VulkanRenderBackend backend = new VulkanRenderBackend();
        backend.initialize();
        try {
            assertNotNull(backend.context().deviceName());
            float[] pos = { 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f };
            float[] norm = { 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f };
            float[] uv = { 0f, 0f, 1f, 0f, 0f, 1f };
            int[] idx = { 0, 1, 2 };
            backend.uploadMeshToGpu(pos, norm, uv, idx);
            assertEquals(2, backend.liveAllocationCount());
        } finally {
            backend.dispose();
        }
        assertEquals(0, backend.liveAllocationCount());
    }

    @Test
    void glbLoaderParsesMercedesSlk55AndUploadsToVulkan() throws Exception {
        Path zipPath = Path.of("mercedes-benz-slk-55-amg-2012.zip");
        if (!Files.isRegularFile(zipPath)) {
            return;
        }
        GlbModel model;
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            ZipEntry entry = zip.getEntry("source/Mercedes_SLK55_AMG_2012_FM7_Sketchfab.glb");
            assertNotNull(entry);
            try (InputStream stream = zip.getInputStream(entry)) {
                model = new GlbLoader().load(stream);
            }
        }
        assertTrue(model.totalTriangles() > 50_000, "Mercedes GLB must contain high-detail triangles");
        System.out.printf("GLB bounds: X[%.2f..%.2f] Y[%.2f..%.2f] Z[%.2f..%.2f] tris=%d%n",
            model.minX(), model.maxX(), model.minY(), model.maxY(), model.minZ(), model.maxZ(), model.totalTriangles());
        System.out.printf("FL hub=(%.2f, %.2f, %.2f) tris=%d%n",
            model.frontLeftWheel().hubX(), model.frontLeftWheel().hubY(), model.frontLeftWheel().hubZ(),
            model.frontLeftWheel().primitives().size());
        assertTrue(!model.chassisPrimitives().isEmpty());
        assertTrue(!model.frontLeftWheel().primitives().isEmpty(), "FL wheel separated");
        assertTrue(!model.frontRightWheel().primitives().isEmpty(), "FR wheel separated");
        assertTrue(!model.rearLeftWheel().primitives().isEmpty(), "RL wheel separated");
        assertTrue(!model.rearRightWheel().primitives().isEmpty(), "RR wheel separated");

        VulkanRenderBackend backend = new VulkanRenderBackend();
        backend.initialize();
        try {
            for (GlbMeshPrimitive prim : model.chassisPrimitives()) {
                backend.uploadMeshToGpu(prim.positions(), prim.normals(), prim.uvs(), prim.indices());
            }
            assertTrue(backend.liveAllocationCount() > 0);
        } finally {
            backend.dispose();
        }
        assertEquals(0, backend.liveAllocationCount());
    }
}
