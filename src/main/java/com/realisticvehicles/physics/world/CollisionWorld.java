package com.realisticvehicles.physics.world;

import com.realisticvehicles.core.math.Vec3d;
import java.util.List;
import java.util.Optional;

/**
 * Dependency-inverted collision world abstraction.
 * Allows running vehicle physics identically in unit tests and Minecraft 1.21.1.
 */
public interface CollisionWorld {
    Optional<RayHit> raycast(Vec3d origin, Vec3d direction, double maxDistance);

    List<CollisionBox> queryBoxes(CollisionBox bounds);
}
