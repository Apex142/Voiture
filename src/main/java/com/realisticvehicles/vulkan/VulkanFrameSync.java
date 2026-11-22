package com.realisticvehicles.vulkan;

import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

/**
 * Manages Vulkan command pools, command buffers, semaphores, and fences for frame sync.
 */
public final class VulkanFrameSync implements AutoCloseable {
    private final VulkanContext context;
    private final long commandPool;
    private final long imageAvailableSemaphore;
    private final long renderFinishedSemaphore;
    private final long inFlightFence;
    private final VkCommandBuffer commandBuffer;

    public VulkanFrameSync(VulkanContext context) {
        this.context = context;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(context.graphicsQueueFamily());

            LongBuffer pLong = stack.mallocLong(1);
            VulkanContext.checkVk(VK10.vkCreateCommandPool(context.device(), poolInfo, null, pLong), "vkCreateCommandPool");
            this.commandPool = pLong.get(0);

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool)
                .level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1);
            var pCmd = stack.mallocPointer(1);
            VulkanContext.checkVk(VK10.vkAllocateCommandBuffers(context.device(), allocInfo, pCmd), "vkAllocateCommandBuffers");
            this.commandBuffer = new VkCommandBuffer(pCmd.get(0), context.device());

            VkSemaphoreCreateInfo semInfo = VkSemaphoreCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            VulkanContext.checkVk(VK10.vkCreateSemaphore(context.device(), semInfo, null, pLong), "vkCreateSemaphore");
            this.imageAvailableSemaphore = pLong.get(0);
            VulkanContext.checkVk(VK10.vkCreateSemaphore(context.device(), semInfo, null, pLong), "vkCreateSemaphore");
            this.renderFinishedSemaphore = pLong.get(0);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                .flags(VK10.VK_FENCE_CREATE_SIGNALED_BIT);
            VulkanContext.checkVk(VK10.vkCreateFence(context.device(), fenceInfo, null, pLong), "vkCreateFence");
            this.inFlightFence = pLong.get(0);
        }
    }

    public VkCommandBuffer commandBuffer() {
        return commandBuffer;
    }

    public long imageAvailableSemaphore() {
        return imageAvailableSemaphore;
    }

    public long renderFinishedSemaphore() {
        return renderFinishedSemaphore;
    }

    public long inFlightFence() {
        return inFlightFence;
    }

    @Override
    public void close() {
        VK10.vkDestroyFence(context.device(), inFlightFence, null);
        VK10.vkDestroySemaphore(context.device(), renderFinishedSemaphore, null);
        VK10.vkDestroySemaphore(context.device(), imageAvailableSemaphore, null);
        VK10.vkDestroyCommandPool(context.device(), commandPool, null);
    }
}
