package nl.tivek.multiversepowers.engine.rig;

// The turns a joint may make, in order, each about an axis of the bone's own frame and each within its range.
public record Joint(Turn... turns) {
    public static final Joint FIXED = new Joint();

    public record Turn(Axis axis, double min, double max) {
        public Turn {
            if (!(min <= max)) {
                throw new IllegalArgumentException("A turn's range runs from min to max: " + min + " > " + max);
            }
        }

        public double clamp(double angle) {
            return angle < this.min ? this.min : Math.min(angle, this.max);
        }

        public boolean holds(double angle) {
            return angle >= this.min && angle <= this.max;
        }
    }

    public enum Axis {
        X(1.0, 0.0, 0.0),
        Y(0.0, 1.0, 0.0),
        Z(0.0, 0.0, 1.0);

        public final double x;
        public final double y;
        public final double z;

        Axis(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static Turn x(double min, double max) {
        return new Turn(Axis.X, min, max);
    }

    public static Turn y(double min, double max) {
        return new Turn(Axis.Y, min, max);
    }

    public static Turn z(double min, double max) {
        return new Turn(Axis.Z, min, max);
    }

    public static Turn free(Axis axis) {
        return new Turn(axis, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public int size() {
        return this.turns.length;
    }

    public Turn turn(int index) {
        return this.turns[index];
    }
}
