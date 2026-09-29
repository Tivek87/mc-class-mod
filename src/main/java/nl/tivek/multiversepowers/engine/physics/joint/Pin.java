package nl.tivek.multiversepowers.engine.physics.joint;

import nl.tivek.multiversepowers.engine.physics.Constraint;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;

// Holds a point of a body (in its own axes) to a place in the world that the caller moves: a creature hanging from a
// hand, or a soft pull keeping a body where its entity really is (compliance above 0 makes it a spring).
public final class Pin implements Constraint {
    private final int body;
    private final double lx;
    private final double ly;
    private final double lz;
    private final double compliance;
    private final double[] at = new double[3];
    private final double[] pace = new double[3];
    private final double[] t = new double[3];
    private boolean on;

    public Pin(int body, double lx, double ly, double lz, double compliance) {
        this.body = body;
        this.lx = lx;
        this.ly = ly;
        this.lz = lz;
        this.compliance = compliance;
    }

    public void to(double x, double y, double z) {
        this.at[0] = x;
        this.at[1] = y;
        this.at[2] = z;
        this.pace[0] = 0.0;
        this.pace[1] = 0.0;
        this.pace[2] = 0.0;
        this.on = true;
    }

    // Moves the place evenly from `from` to `to` over the next step of dt seconds, a little every substep, so a body
    // pulled along by something that moves keeps up with it smoothly instead of with a jolt each step.
    public void sweep(double[] from, double[] to, double dt) {
        for (int i = 0; i < 3; i++) {
            this.at[i] = from[i];
            this.pace[i] = (to[i] - from[i]) / dt;
        }
        this.on = true;
    }

    public void release() {
        this.on = false;
    }

    public boolean holding() {
        return this.on;
    }

    @Override
    public void solve(RigidWorld world, double h) {
        if (!this.on) {
            return;
        }
        this.at[0] += this.pace[0] * h;
        this.at[1] += this.pace[1] * h;
        this.at[2] += this.pace[2] * h;
        world.point(this.body, this.lx, this.ly, this.lz, this.t);
        world.attach(this.body, this.t[0], this.t[1], this.t[2], -1, this.at[0], this.at[1], this.at[2],
                this.compliance, h);
    }
}
