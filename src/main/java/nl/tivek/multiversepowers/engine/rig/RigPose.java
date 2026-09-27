package nl.tivek.multiversepowers.engine.rig;

// How far every joint of a rig is turned, one value per turn, always within the joint's range.
public final class RigPose {
    private final Rig rig;
    private final double[] values;

    public RigPose(Rig rig) {
        this.rig = rig;
        this.values = new double[rig.values()];
        this.rest();
    }

    public Rig rig() {
        return this.rig;
    }

    public double get(int bone, int turn) {
        return this.values[this.rig.value(bone, turn)];
    }

    // Stores the angle as the joint allows it and returns what was stored.
    public double set(int bone, int turn, double angle) {
        double held = this.rig.joint(bone).turn(turn).clamp(angle);
        this.values[this.rig.value(bone, turn)] = held;
        return held;
    }

    public void rest() {
        for (int b = 0; b < this.rig.size(); b++) {
            Joint joint = this.rig.joint(b);
            for (int t = 0; t < joint.size(); t++) {
                this.values[this.rig.value(b, t)] = joint.turn(t).clamp(0.0);
            }
        }
    }

    public void copy(RigPose from) {
        this.check(from);
        System.arraycopy(from.values, 0, this.values, 0, this.values.length);
    }

    // Between two poses of the same rig: 0 is from, 1 is to.
    public void lerp(RigPose from, RigPose to, double t) {
        this.check(from);
        this.check(to);
        for (int i = 0; i < this.values.length; i++) {
            this.values[i] = from.values[i] + (to.values[i] - from.values[i]) * t;
        }
    }

    // Moves every bone towards the other pose by its own weight (0 keeps this pose, 1 takes the other's).
    public void blend(RigPose other, double[] boneWeights) {
        this.check(other);
        if (boneWeights.length != this.rig.size()) {
            throw new IllegalArgumentException("One weight per bone: " + boneWeights.length + " for " + this.rig.size());
        }
        for (int b = 0; b < this.rig.size(); b++) {
            double w = boneWeights[b];
            for (int t = 0; t < this.rig.joint(b).size(); t++) {
                int i = this.rig.value(b, t);
                this.values[i] += (other.values[i] - this.values[i]) * w;
            }
        }
    }

    private void check(RigPose other) {
        if (other.rig != this.rig) {
            throw new IllegalArgumentException("Poses of two different rigs");
        }
    }
}
