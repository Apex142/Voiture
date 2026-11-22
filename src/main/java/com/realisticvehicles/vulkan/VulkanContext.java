package com.realisticvehicles.vulkan;

import java.nio.IntBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

/**
 * Manages the Vulkan VkInstance, VkPhysicalDevice, logical VkDevice, and graphics VkQueue.
 */
public final class VulkanContext implements AutoCloseable {
    private VkInstance instance;
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue graphicsQueue;
    private int graphicsQueueFamily = -1;
    private String deviceName = "Unknown";
    private int apiVersion;

    public void initialize(String appName) {
        org.lwjgl.system.Configuration.STACK_SIZE.set(512);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(stack.UTF8(appName))
                .applicationVersion(VK10.VK_MAKE_VERSION(0, 1, 0))
                .pEngineName(stack.UTF8("RealisticVehiclesVulkan"))
                .engineVersion(VK10.VK_MAKE_VERSION(0, 1, 0))
                .apiVersion(VK10.VK_MAKE_VERSION(1, 2, 0));

            VkInstanceCreateInfo instInfo = VkInstanceCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(appInfo);

            PointerBuffer pInstance = stack.mallocPointer(1);
            checkVk(VK10.vkCreateInstance(instInfo, null, pInstance), "vkCreateInstance");
            this.instance = new VkInstance(pInstance.get(0), instInfo);

            selectPhysicalDevice(stack);
            createLogicalDevice(stack);
        }
    }

    private void selectPhysicalDevice(MemoryStack stack) {
        IntBuffer count = stack.mallocInt(1);
        checkVk(VK10.vkEnumeratePhysicalDevices(instance, count, null), "vkEnumeratePhysicalDevices");
        if (count.get(0) == 0) {
            throw new IllegalStateException("No Vulkan-capable physical GPU found.");
        }
        PointerBuffer devices = stack.mallocPointer(count.get(0));
        checkVk(VK10.vkEnumeratePhysicalDevices(instance, count, devices), "vkEnumeratePhysicalDevices");

        VkPhysicalDevice best = null;
        int bestScore = -1;
        int bestQueue = -1;
        for (int i = 0; i < devices.limit(); i++) {
            VkPhysicalDevice cand = new VkPhysicalDevice(devices.get(i), instance);
            int qFamily = findGraphicsQueueFamily(cand, stack);
            if (qFamily < 0) {
                continue;
            }
            VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc(stack);
            VK10.vkGetPhysicalDeviceProperties(cand, props);
            int score = props.deviceType() == VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU ? 1000 : 100;
            if (score > bestScore) {
                bestScore = score;
                best = cand;
                bestQueue = qFamily;
                this.deviceName = props.deviceNameString();
                this.apiVersion = props.apiVersion();
            }
        }
        if (best == null) {
            throw new IllegalStateException("No Vulkan GPU with graphics queue found.");
        }
        this.physicalDevice = best;
        this.graphicsQueueFamily = bestQueue;
    }

    private int findGraphicsQueueFamily(VkPhysicalDevice dev, MemoryStack stack) {
        IntBuffer qCount = stack.mallocInt(1);
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(dev, qCount, null);
        VkQueueFamilyProperties.Buffer props = VkQueueFamilyProperties.calloc(qCount.get(0), stack);
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(dev, qCount, props);
        for (int i = 0; i < props.limit(); i++) {
            if ((props.get(i).queueFlags() & VK10.VK_QUEUE_GRAPHICS_BIT) != 0) {
                return i;
            }
        }
        return -1;
    }

    private void createLogicalDevice(MemoryStack stack) {
        VkDeviceQueueCreateInfo.Buffer queueInfo = VkDeviceQueueCreateInfo.calloc(1, stack)
            .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
            .queueFamilyIndex(graphicsQueueFamily)
            .pQueuePriorities(stack.floats(1.0F));

        VkDeviceCreateInfo devInfo = VkDeviceCreateInfo.calloc(stack)
            .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
            .pQueueCreateInfos(queueInfo);

        PointerBuffer pDevice = stack.mallocPointer(1);
        checkVk(VK10.vkCreateDevice(physicalDevice, devInfo, null, pDevice), "vkCreateDevice");
        this.device = new VkDevice(pDevice.get(0), physicalDevice, devInfo);

        PointerBuffer pQueue = stack.mallocPointer(1);
        VK10.vkGetDeviceQueue(device, graphicsQueueFamily, 0, pQueue);
        this.graphicsQueue = new VkQueue(pQueue.get(0), device);
    }

    public VkInstance instance() {
        return instance;
    }

    public VkPhysicalDevice physicalDevice() {
        return physicalDevice;
    }

    public VkDevice device() {
        return device;
    }

    public VkQueue graphicsQueue() {
        return graphicsQueue;
    }

    public int graphicsQueueFamily() {
        return graphicsQueueFamily;
    }

    public String deviceName() {
        return deviceName;
    }

    public int apiVersion() {
        return apiVersion;
    }

    public static void checkVk(int result, String operation) {
        if (result != VK10.VK_SUCCESS) {
            throw new IllegalStateException("Vulkan error in " + operation + ": code=" + result);
        }
    }

    @Override
    public void close() {
        if (device != null) {
            VK10.vkDeviceWaitIdle(device);
            VK10.vkDestroyDevice(device, null);
            device = null;
        }
        if (instance != null) {
            VK10.vkDestroyInstance(instance, null);
            instance = null;
        }
    }
}
