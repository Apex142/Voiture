package com.realisticvehicles.minecraft;

import com.realisticvehicles.physics.world.SurfaceProperties;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Maps Minecraft 1.21.1 block states to physical road/terrain surface properties.
 */
public final class BlockSurfaceMapper {

    public SurfaceProperties map(BlockState state) {
        if (state == null || state.isAir()) {
            return SurfaceProperties.ASPHALT;
        }
        if (state.is(BlockTags.ICE)) {
            return SurfaceProperties.ICE;
        }
        if (state.is(BlockTags.SNOW)) {
            return SurfaceProperties.SNOW;
        }
        if (state.is(BlockTags.SAND)) {
            return SurfaceProperties.SAND;
        }
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)) {
            return SurfaceProperties.GRASS;
        }
        if (state.is(BlockTags.DIRT) || state.is(Blocks.GRAVEL) || state.is(Blocks.MUD)) {
            return SurfaceProperties.DIRT;
        }
        if (isAsphaltLike(state)) {
            return SurfaceProperties.ASPHALT;
        }
        return SurfaceProperties.STONE;
    }

    private boolean isAsphaltLike(BlockState state) {
        return state.is(Blocks.BLACK_CONCRETE)
            || state.is(Blocks.GRAY_CONCRETE)
            || state.is(Blocks.LIGHT_GRAY_CONCRETE)
            || state.is(Blocks.WHITE_CONCRETE)
            || state.is(Blocks.DEEPSLATE)
            || state.is(Blocks.POLISHED_DEEPSLATE)
            || state.is(Blocks.BASALT)
            || state.is(Blocks.SMOOTH_BASALT);
    }
}
