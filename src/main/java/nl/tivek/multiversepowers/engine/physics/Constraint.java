package nl.tivek.multiversepowers.engine.physics;

// One rule a simulation keeps, solved once every substep of length h (seconds).
@FunctionalInterface
public interface Constraint {
    void solve(RigidWorld world, double h);

    // A second pass at the end of the substep for what must hold most of all (a joint's anchors), after every other
    // rule has moved the bodies.
    default void rejoin(RigidWorld world, double h) {
    }
}
