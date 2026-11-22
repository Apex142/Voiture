package com.realisticvehicles.minecraft;

import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.world.CollisionBox;
import com.realisticvehicles.physics.world.CollisionWorld;
import com.realisticvehicles.physics.world.RayHit;
import com.realisticvehicles.physics.world.SurfaceProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Adapter translating Minecraft 1.21.1 Level block shapes into pure CollisionWorld queries.
 */
public final class MinecraftCollisionWorld implements CollisionWorld {
    private final Level level;
    private final BlockSurfaceMapper surfaceMapper;

    public MinecraftCollisionWorld(Level level, BlockSurfaceMapper surfaceMapper) {
        this.level = level;
        this.surfaceMapper = surfaceMapper;
    }

    @Override
    public Optional<RayHit> raycast(Vec3d origin, Vec3d direction, double maxDistance) {
        Vec3d dir = direction.normalize();
        if (dir.lengthSquared() < 1e-9 || maxDistance <= 0.0) {
            return Optional.empty();
        }
        Vec3 start = new Vec3(origin.x(), origin.y(), origin.z());
        Vec3 end = new Vec3(
            origin.x() + dir.x() * maxDistance,
            origin.y() + dir.y() * maxDistance,
            origin.z() + dir.z() * maxDistance
        );
        ClipContext context = new ClipContext(
            start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()
        );
        BlockHitResult result = level.clip(context);
        if (result.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        Vec3 hitLoc = result.getLocation();
        Vec3d hitPoint = new Vec3d(hitLoc.x, hitLoc.y, hitLoc.z);
        Direction face = result.getDirection();
        Vec3d normal = new Vec3d(face.getStepX(), face.getStepY(), face.getStepZ());
        double distance = hitPoint.sub(origin).length();
        BlockState blockState = level.getBlockState(result.getBlockPos());
        SurfaceProperties surface = surfaceMapper.map(blockState);
        return Optional.of(new RayHit(hitPoint, normal, distance, surface));
    }

    @Override
    public List<CollisionBox> queryBoxes(CollisionBox bounds) {
        AABB queryAabb = new AABB(
            bounds.minX(), bounds.minY(), bounds.minZ(),
            bounds.maxX(), bounds.maxY(), bounds.maxZ()
        );
        List<CollisionBox> boxes = new ArrayList<>();
        Iterable<VoxelShape> shapes = level.getBlockCollisions(null, queryAabb);
        for (VoxelShape shape : shapes) {
            for (AABB box : shape.toAabbs()) {
                boxes.add(new CollisionBox(
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ
                ));
            }
        }
        return boxes;
    }
}
