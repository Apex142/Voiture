package com.realisticvehicles.minecraft;

import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Projects directional Minecraft dynamic block light (Blocks.LIGHT) strictly in front for headlights
 * and behind the rear bumper when braking or reversing, cleaning up previous blocks automatically.
 */
public final class VehicleDynamicLightManager {
    private static final Map<Integer, Set<BlockPos>> ACTIVE_LIGHTS = new HashMap<>();

    private VehicleDynamicLightManager() {
    }

    public static void updateVehicleLights(VehicleEntity vehicle) {
        Level level = vehicle.level();
        if (level.isClientSide()) {
            return;
        }
        int id = vehicle.getId();
        byte mask = vehicle.lightsMask();
        boolean lowBeams = (mask & 1) != 0 || (mask & 2) != 0;
        boolean highBeams = (mask & 2) != 0;
        boolean braking = vehicle.simulation().telemetry().braking();
        boolean reversing = vehicle.simulation().telemetry().reversing();

        Set<BlockPos> desired = new HashSet<>();
        if (!vehicle.isRemoved()) {
            Vec3d com = vehicle.simulation().body().position();
            Quat4d rot = vehicle.simulation().body().orientation();
            Vec3d forward = rot.forward();
            Vec3d left = rot.left();

            // 1. Directional Front Headlight Cone: start 9.0m ahead of CoM so the car & rear stay 100% dark
            if (lowBeams) {
                int maxDist = highBeams ? 26 : 17;
                int baseLevel = highBeams ? 15 : 13;
                for (int d = 9; d <= maxDist; d += 3) {
                    double spread = (d > 14) ? 1.8 : 0.95;
                    Vec3d centerPt = com.addScaled(forward, d);
                    Vec3d leftPt = centerPt.addScaled(left, spread);
                    Vec3d rightPt = centerPt.addScaled(left, -spread);

                    int lightLvl = Math.max(9, baseLevel - ((d - 9) / 4));
                    tryAddLightPos(level, desired, toBlockPos(centerPt), lightLvl);
                    tryAddLightPos(level, desired, toBlockPos(leftPt), lightLvl);
                    tryAddLightPos(level, desired, toBlockPos(rightPt), lightLvl);
                }
            }

            // 2. Rear Reverse Light Illumination behind the rear bumper (-5.5m to -8.0m) strictly when reversing
            if (reversing) {
                Vec3d rearCenter = com.addScaled(forward, -5.5);
                tryAddLightPos(level, desired, toBlockPos(rearCenter), 12);
                Vec3d rearFar = com.addScaled(forward, -7.8);
                tryAddLightPos(level, desired, toBlockPos(rearFar.addScaled(left, 0.85)), 10);
                tryAddLightPos(level, desired, toBlockPos(rearFar.addScaled(left, -0.85)), 10);
            }
        }

        Set<BlockPos> previous = ACTIVE_LIGHTS.get(id);
        if (previous != null) {
            Iterator<BlockPos> it = previous.iterator();
            while (it.hasNext()) {
                BlockPos oldPos = it.next();
                if (!desired.contains(oldPos)) {
                    if (level.hasChunkAt(oldPos) && level.getBlockState(oldPos).is(Blocks.LIGHT)) {
                        level.setBlock(oldPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                    it.remove();
                }
            }
        }

        if (desired.isEmpty()) {
            ACTIVE_LIGHTS.remove(id);
        } else {
            ACTIVE_LIGHTS.put(id, desired);
        }
    }

    public static void clearVehicleLights(VehicleEntity vehicle) {
        Level level = vehicle.level();
        if (level.isClientSide()) {
            return;
        }
        Set<BlockPos> previous = ACTIVE_LIGHTS.remove(vehicle.getId());
        if (previous != null) {
            for (BlockPos oldPos : previous) {
                if (level.hasChunkAt(oldPos) && level.getBlockState(oldPos).is(Blocks.LIGHT)) {
                    level.setBlock(oldPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static BlockPos toBlockPos(Vec3d pt) {
        return BlockPos.containing(pt.x(), pt.y() + 0.25, pt.z());
    }

    private static void tryAddLightPos(Level level, Set<BlockPos> desired, BlockPos pos, int lightLevel) {
        if (!level.hasChunkAt(pos)) {
            return;
        }
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.is(Blocks.LIGHT)) {
            BlockState target = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel);
            if (!existing.equals(target)) {
                level.setBlock(pos, target, 3);
            }
            desired.add(pos.immutable());
            return;
        }
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.isAir() || aboveState.is(Blocks.LIGHT)) {
            BlockState target = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel);
            if (!aboveState.equals(target)) {
                level.setBlock(above, target, 3);
            }
            desired.add(above.immutable());
        }
    }
}
