package com.realisticvehicles.vulkan;

import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

/**
 * Manages Vulkan VkDeviceMemory heap selection and strict allocation leak tracking.
 */
public final class VulkanMemoryAllocator implements AutoCloseable {
    private final VulkanContext context;
    private final AtomicInteger liveAllocations = new AtomicInteger();

    public VulkanMemoryAllocator(VulkanContext context) {
        this.context = context;
    }

    public VulkanContext context() {
        return context;
    }

    public int findMemoryType(int typeFilter, int requiredProperties) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.calloc(stack);
            VK10.vkGetPhysicalDeviceMemoryProperties(context.physicalDevice(), memProps);
            for (int i = 0; i < memProps.memoryTypeCount(); i++) {
                boolean typeMatch = (typeFilter & (1 << i)) != 0;
                int flags = memProps.memoryTypes(i).propertyFlags();
                if (typeMatch && (flags & requiredProperties) == requiredProperties) {
                    return i;
                }
            }
        }
        throw new IllegalStateException("No matching Vulkan memory type for flags=" + requiredProperties);
    }

    public void recordAllocation() {
        liveAllocations.incrementAndGet();
    }

    public void recordFree() {
        liveAllocations.decrementAndGet();
    }

    public int liveAllocations() {
        return liveAllocations.get();
    }

    @Override
    public void close() {
        if (liveAllocations.get() != 0) {
            throw new IllegalStateException("VRAM leak detected! Live Vulkan allocations: " + liveAllocations.get());
        }
    }
}
