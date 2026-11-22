package com.realisticvehicles.physics.world;

/**
 * Physical friction and rolling resistance parameters for a ground surface.
 */
public record SurfaceProperties(String name, double gripMultiplier, double rollingResistance) {
    public static final SurfaceProperties ASPHALT = new SurfaceProperties("asphalt", 1.05, 0.014);
    public static final SurfaceProperties STONE = new SurfaceProperties("stone", 0.95, 0.016);
    public static final SurfaceProperties DIRT = new SurfaceProperties("dirt", 0.72, 0.032);
    public static final SurfaceProperties GRASS = new SurfaceProperties("grass", 0.65, 0.038);
    public static final SurfaceProperties SAND = new SurfaceProperties("sand", 0.55, 0.065);
    public static final SurfaceProperties SNOW = new SurfaceProperties("snow", 0.42, 0.050);
    public static final SurfaceProperties ICE = new SurfaceProperties("ice", 0.16, 0.010);
}
