package nl.tivek.multiversepowers.engine.physics;

// The solid world around a simulation: boxes that never move.
@FunctionalInterface
public interface Blocks {
    // Writes the solid boxes that touch the given region to out (minX, minY, minZ, maxX, maxY, maxZ each) and returns
    // how many; never more than out.length / 6.
    int collect(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double[] out);

    Blocks NONE = (minX, minY, minZ, maxX, maxY, maxZ, out) -> 0;
}
