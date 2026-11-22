package com.realisticvehicles.vulkan;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

/**
 * GPU buffer (Vertex, Index, Uniform, or Staging) backed by native VkDeviceMemory.
 */
public final class VulkanBuffer implements AutoCloseable {
    private final VulkanMemoryAllocator allocator;
    private final long buffer;
    private final long memory;
    private final long sizeBytes;
    private boolean closed;

    private VulkanBuffer(VulkanMemoryAllocator allocator, long buffer, long memory, long sizeBytes) {
        this.allocator = allocator;
        this.buffer = buffer;
        this.memory = memory;
        this.sizeBytes = sizeBytes;
        this.allocator.recordAllocation();
    }

    public static VulkanBuffer createHostBuffer(VulkanMemoryAllocator allocator, long sizeBytes, int usageFlags) {
        VkDevice device = allocator.context().device();
        long safeSize = Math.max(sizeBytes, 16L);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufInfo = VkBufferCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(safeSize)
                .usage(usageFlags)
                .sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuf = stack.mallocLong(1);
            VulkanContext.checkVk(VK10.vkCreateBuffer(device, bufInfo, null, pBuf), "vkCreateBuffer");
            long bufHandle = pBuf.get(0);

            VkMemoryRequirements memReq = VkMemoryRequirements.calloc(stack);
            VK10.vkGetBufferMemoryRequirements(device, bufHandle, memReq);

            int memType = allocator.findMemoryType(
                memReq.memoryTypeBits(),
                VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            );

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memReq.size())
                .memoryTypeIndex(memType);

            LongBuffer pMem = stack.mallocLong(1);
            VulkanContext.checkVk(VK10.vkAllocateMemory(device, allocInfo, null, pMem), "vkAllocateMemory");
            long memHandle = pMem.get(0);

            VulkanContext.checkVk(VK10.vkBindBufferMemory(device, bufHandle, memHandle, 0), "vkBindBufferMemory");
            return new VulkanBuffer(allocator, bufHandle, memHandle, safeSize);
        }
    }

    public void uploadFloats(float[] data) {
        if (data.length == 0) {
            return;
        }
        VkDevice device = allocator.context().device();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer mapped = stack.mallocPointer(1);
            int byteSize = data.length * Float.BYTES;
            VulkanContext.checkVk(VK10.vkMapMemory(device, memory, 0, byteSize, 0, mapped), "vkMapMemory");
            ByteBuffer dst = MemoryUtil.memByteBuffer(mapped.get(0), byteSize);
            dst.asFloatBuffer().put(data);
            VK10.vkUnmapMemory(device, memory);
        }
    }

    public void uploadInts(int[] data) {
        if (data.length == 0) {
            return;
        }
        VkDevice device = allocator.context().device();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer mapped = stack.mallocPointer(1);
            int byteSize = data.length * Integer.BYTES;
            VulkanContext.checkVk(VK10.vkMapMemory(device, memory, 0, byteSize, 0, mapped), "vkMapMemory");
            ByteBuffer dst = MemoryUtil.memByteBuffer(mapped.get(0), byteSize);
            dst.asIntBuffer().put(data);
            VK10.vkUnmapMemory(device, memory);
        }
    }

    public long handle() {
        return buffer;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            VkDevice device = allocator.context().device();
            VK10.vkDestroyBuffer(device, buffer, null);
            VK10.vkFreeMemory(device, memory, null);
            allocator.recordFree();
        }
    }
}
