package com.realisticvehicles.vulkan;

import com.realisticvehicles.render.api.BackendType;
import com.realisticvehicles.render.api.RenderBackend;
import com.realisticvehicles.render.api.VehicleRenderState;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.vulkan.VK10;

/**
 * Native LWJGL Vulkan RenderBackend managing device, VMA memory, sync, and GPU mesh buffers.
 */
public final class VulkanRenderBackend implements RenderBackend {
    private VulkanContext context;
    private VulkanMemoryAllocator allocator;
    private VulkanFrameSync frameSync;
    private final List<VulkanBuffer> gpuBuffers = new ArrayList<>();
    private boolean initialized;

    @Override
    public BackendType type() {
        return BackendType.VULKAN;
    }

    @Override
    public void initialize() {
        if (initialized) {
            return;
        }
        this.context = new VulkanContext();
        this.context.initialize("RealisticVehicles");
        this.allocator = new VulkanMemoryAllocator(context);
        this.frameSync = new VulkanFrameSync(context);
        this.initialized = true;
    }

    public void uploadMeshToGpu(float[] positions, float[] normals, float[] uvs, int[] indices) {
        if (!initialized) {
            initialize();
        }
        VulkanBuffer vbo = VulkanBuffer.createHostBuffer(
            allocator, (long) positions.length * Float.BYTES, VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
        );
        vbo.uploadFloats(positions);
        gpuBuffers.add(vbo);

        VulkanBuffer ibo = VulkanBuffer.createHostBuffer(
            allocator, (long) indices.length * Integer.BYTES, VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT
        );
        ibo.uploadInts(indices);
        gpuBuffers.add(ibo);
    }

    public VulkanContext context() {
        return context;
    }

    public int liveAllocationCount() {
        return allocator != null ? allocator.liveAllocations() : 0;
    }

    @Override
    public void renderVehicle(VehicleRenderState state, float partialTick) {
        // Full swapchain integration with Minecraft's GLFW window is tracked in P3-008.
    }

    @Override
    public void dispose() {
        if (!initialized) {
            return;
        }
        for (VulkanBuffer buffer : gpuBuffers) {
            buffer.close();
        }
        gpuBuffers.clear();
        frameSync.close();
        allocator.close();
        context.close();
        initialized = false;
    }
}
