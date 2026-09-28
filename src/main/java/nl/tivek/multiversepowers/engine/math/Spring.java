package nl.tivek.multiversepowers.engine.math;

// A value that follows its target like a weight on a spring: it has speed, so it carries on a little past a sudden
// stop and settles back, and a push (kick) sets it swinging. Time in ticks; frequency in turns per tick; damping 1
// settles without swinging past, below 1 swings past a little.
public final class Spring {
    // Longer steps are cut into these, so a slow frame never makes it wobble apart.
    private static final double STEP = 0.25;

    public double value;
    public double speed;

    public Spring() {
    }

    public Spring(double value) {
        this.value = value;
    }

    public double step(double target, double dt, double frequency, double damping) {
        double left = Math.max(0.0, Math.min(dt, 4.0));
        double w = 2.0 * Math.PI * frequency;
        while (left > 1.0E-9) {
            double h = Math.min(STEP, left);
            left -= h;
            this.speed += (w * w * (target - this.value) - 2.0 * damping * w * this.speed) * h;
            this.value += this.speed * h;
        }
        return this.value;
    }

    public void kick(double speed) {
        this.speed += speed;
    }

    public void set(double value) {
        this.value = value;
        this.speed = 0.0;
    }
}
