package com.realisticvehicles.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Placeable Minecraft Block for the Gas Station Fuel Pump.
 * Automatically spawns/links its 3D FuelPumpEntity visual & hose controller on placement.
 */
public final class FuelPumpBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 3.0, 15.0, 32.0, 13.0);

    public FuelPumpBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            ensurePumpEntity(level, pos, state);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        FuelPumpEntity pump = ensurePumpEntity(level, pos, state);
        if (pump != null) {
            return pump.interactAt(player, hit.getLocation().subtract(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), InteractionHand.MAIN_HAND);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            for (FuelPumpEntity pump : level.getEntitiesOfClass(
                FuelPumpEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1.2)
            )) {
                pump.discard();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static FuelPumpEntity ensurePumpEntity(Level level, BlockPos pos, BlockState state) {
        for (FuelPumpEntity existing : level.getEntitiesOfClass(
            FuelPumpEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(0.8)
        )) {
            return existing;
        }
        if (level.isClientSide()) return null;
        FuelPumpEntity pump = ModEntities.FUEL_PUMP.get().create(level);
        if (pump != null) {
            Direction dir = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
            pump.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            pump.setYRot(dir.toYRot());
            level.addFreshEntity(pump);
        }
        return pump;
    }
}
