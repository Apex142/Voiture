package com.realisticvehicles.client;

import com.realisticvehicles.minecraft.RealisticVehiclesMod;
import com.realisticvehicles.render.glb.GlbLoader;
import com.realisticvehicles.render.glb.GlbMeshPrimitive;
import com.realisticvehicles.render.glb.GlbModel;
import com.realisticvehicles.vulkan.VulkanRenderBackend;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads and caches GLB 3D vehicle models asynchronously and uploads them to Vulkan GPU buffers.
 */
public final class GlbModelRegistry {
    private static final String RESOURCE_GLB = "/assets/realisticvehicles/models/entity/slk55.glb";
    private static final String ZIP_ENTRY = "source/Mercedes_SLK55_AMG_2012_FM7_Sketchfab.glb";

    private static volatile GlbModel slk55Model;
    private static final AtomicBoolean LOADING = new AtomicBoolean(false);
    private static VulkanRenderBackend vulkanBackend;

    private GlbModelRegistry() {
    }

    public static GlbModel getOrLoadSlk55() {
        if (slk55Model != null) {
            return slk55Model;
        }
        if (LOADING.compareAndSet(false, true)) {
            Thread loader = new Thread(GlbModelRegistry::loadSlk55Async, "RV-GLB-Loader");
            loader.setDaemon(true);
            loader.start();
        }
        return null;
    }

    private static void loadSlk55Async() {
        try {
            GlbModel model = loadFromClasspathOrLocalZip();
            if (model != null) {
                uploadToVulkanDiagnostics(model);
                slk55Model = model;
                RealisticVehiclesMod.LOGGER.info(
                    "Loaded Mercedes-Benz SLK 55 AMG GLB ({} triangles, {} materials).",
                    model.totalTriangles(), model.materials().size()
                );
            }
        } catch (Exception e) {
            RealisticVehiclesMod.LOGGER.error("Failed to load Mercedes SLK 55 AMG GLB model", e);
        }
    }

    private static GlbModel loadFromClasspathOrLocalZip() throws Exception {
        GlbLoader loader = new GlbLoader();
        try (InputStream res = GlbModelRegistry.class.getResourceAsStream(RESOURCE_GLB)) {
            if (res != null) {
                return loader.load(res);
            }
        }
        Path[] candidates = {
            Path.of("mercedes-benz-slk-55-amg-2012.zip"),
            Path.of("../mercedes-benz-slk-55-amg-2012.zip")
        };
        for (Path zipPath : candidates) {
            if (Files.isRegularFile(zipPath)) {
                try (ZipFile zip = new ZipFile(zipPath.toFile())) {
                    ZipEntry entry = zip.getEntry(ZIP_ENTRY);
                    if (entry != null) {
                        try (InputStream stream = zip.getInputStream(entry)) {
                            return loader.load(stream);
                        }
                    }
                }
            }
        }
        RealisticVehiclesMod.LOGGER.warn("Mercedes SLK 55 AMG GLB not found; using provisional mesh.");
        return null;
    }

    private static void uploadToVulkanDiagnostics(GlbModel model) {
        try {
            vulkanBackend = new VulkanRenderBackend();
            vulkanBackend.initialize();
            for (GlbMeshPrimitive prim : model.chassisPrimitives()) {
                vulkanBackend.uploadMeshToGpu(prim.positions(), prim.normals(), prim.uvs(), prim.indices());
            }
            RealisticVehiclesMod.LOGGER.info(
                "Vulkan backend active on GPU '{}' ({} VMA buffers uploaded).",
                vulkanBackend.context().deviceName(), vulkanBackend.liveAllocationCount()
            );
        } catch (Throwable t) {
            RealisticVehiclesMod.LOGGER.warn("Vulkan GPU upload skipped: {}", t.getMessage());
        }
    }

    public static void dispose() {
        if (vulkanBackend != null) {
            vulkanBackend.dispose();
            vulkanBackend = null;
        }
    }
}
