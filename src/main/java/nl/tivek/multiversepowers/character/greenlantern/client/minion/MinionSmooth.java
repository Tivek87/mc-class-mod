package nl.tivek.multiversepowers.character.greenlantern.client.minion;

import java.util.HashMap;
import java.util.Map;
import nl.tivek.multiversepowers.engine.math.Spring;

// A sentinel's pose followed joint by joint on springs, so it never snaps from one stance to the next: every change of
// move, every step into a stride and every blow eases in and settles a little past, as a body with weight does. The
// arms and hands follow fastest, so a blow still lands on its tick; the trunk and the legs carry the weight.
final class MinionSmooth {
    private static final double ARMS = 0.3;
    private static final double LEGS = 0.22;
    private static final double TRUNK = 0.17;
    private static final double DAMPING = 0.85;
    // A gap longer than this (not drawn for a while) starts it over where the pose is.
    private static final double GAP = 5.0;
    private static final long KEEP = 40L;
    private static final Map<Integer, State> STATES = new HashMap<>();

    private static final class State {
        final Spring[] springs = new Spring[22];
        double time;
        long seen;
    }

    private MinionSmooth() {
    }

    static void follow(int id, MinionPose pose, double time, long gameTime) {
        if (STATES.size() > 64) {
            STATES.values().removeIf(state -> gameTime - state.seen > KEEP);
        }
        double[] target = channels(pose);
        State state = STATES.get(id);
        double dt = state == null ? -1.0 : time - state.time;
        if (state == null || dt < 0.0 || dt > GAP) {
            state = new State();
            for (int i = 0; i < target.length; i++) {
                state.springs[i] = new Spring(target[i]);
            }
            STATES.put(id, state);
            dt = 0.0;
        }
        state.time = time;
        state.seen = gameTime;
        double[] now = new double[target.length];
        for (int i = 0; i < target.length; i++) {
            now[i] = dt > 0.0 ? state.springs[i].step(target[i], dt, frequency(i), DAMPING) : state.springs[i].value;
        }
        apply(pose, now);
    }

    // The trunk first (sink, roll, twist, lean, look, nod), then per side: hip, knee, ankle, then arm, spread, elbow,
    // wrist and curl.
    private static double frequency(int channel) {
        if (channel < 6) {
            return TRUNK;
        }
        return (channel - 6) % 8 < 3 ? LEGS : ARMS;
    }

    private static double[] channels(MinionPose pose) {
        double[] values = new double[22];
        values[0] = pose.sink;
        values[1] = pose.roll;
        values[2] = pose.twist;
        values[3] = pose.lean;
        values[4] = pose.look;
        values[5] = pose.nod;
        for (int s = 0; s < 2; s++) {
            int at = 6 + s * 8;
            values[at] = pose.hip[s];
            values[at + 1] = pose.knee[s];
            values[at + 2] = pose.ankle[s];
            values[at + 3] = pose.arm[s];
            values[at + 4] = pose.spread[s];
            values[at + 5] = pose.elbow[s];
            values[at + 6] = pose.wrist[s];
            values[at + 7] = pose.curl[s];
        }
        return values;
    }

    private static void apply(MinionPose pose, double[] values) {
        pose.sink = values[0];
        pose.roll = values[1];
        pose.twist = values[2];
        pose.lean = values[3];
        pose.look = values[4];
        pose.nod = values[5];
        for (int s = 0; s < 2; s++) {
            int at = 6 + s * 8;
            pose.hip[s] = values[at];
            pose.knee[s] = values[at + 1];
            pose.ankle[s] = values[at + 2];
            pose.arm[s] = values[at + 3];
            pose.spread[s] = values[at + 4];
            pose.elbow[s] = values[at + 5];
            pose.wrist[s] = values[at + 6];
            pose.curl[s] = Math.max(0.0, Math.min(1.0, values[at + 7]));
        }
    }
}
