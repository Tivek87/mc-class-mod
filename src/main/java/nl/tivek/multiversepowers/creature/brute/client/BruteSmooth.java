package nl.tivek.multiversepowers.creature.brute.client;

import java.util.HashMap;
import java.util.Map;
import nl.tivek.multiversepowers.engine.math.Spring;

// Each brute's pose followed on springs, so it never snaps from one move into the next (a charge into a wall, a leap
// cut short): its trunk, head, jaw and arms ease over and settle a little past, as a heavy body does. The legs keep the
// walk's own swing.
final class BruteSmooth {
    private static final int CHANNELS = 13;
    private static final double FREQUENCY = 0.3;
    private static final double DAMPING = 0.9;
    // A gap longer than this (not drawn for a while) starts it over where the pose is.
    private static final double GAP = 5.0;
    private static final long KEEP = 40L;
    private static final Map<Integer, State> STATES = new HashMap<>();

    private static final class State {
        final Spring[] springs = new Spring[CHANNELS];
        double time;
        long seen;
    }

    private BruteSmooth() {
    }

    static void follow(int id, BrutePoses pose, double time, long gameTime) {
        if (STATES.size() > 64) {
            STATES.values().removeIf(state -> gameTime - state.seen > KEEP);
        }
        float[] target = channels(pose);
        State state = STATES.get(id);
        double dt = state == null ? -1.0 : time - state.time;
        if (state == null || dt < 0.0 || dt > GAP) {
            state = new State();
            for (int i = 0; i < CHANNELS; i++) {
                state.springs[i] = new Spring(target[i]);
            }
            STATES.put(id, state);
            dt = 0.0;
        }
        state.time = time;
        state.seen = gameTime;
        float[] now = new float[CHANNELS];
        for (int i = 0; i < CHANNELS; i++) {
            now[i] = (float) (dt > 0.0 ? state.springs[i].step(target[i], dt, FREQUENCY, DAMPING)
                    : state.springs[i].value);
        }
        apply(pose, now);
    }

    private static float[] channels(BrutePoses pose) {
        return new float[] { pose.lean, pose.twist, pose.roll, pose.sink, pose.nod, pose.jaw, pose.legSpread,
                pose.arm[0], pose.arm[1], pose.spread[0], pose.spread[1], pose.armTwist[0], pose.armTwist[1] };
    }

    private static void apply(BrutePoses pose, float[] v) {
        pose.lean = v[0];
        pose.twist = v[1];
        pose.roll = v[2];
        pose.sink = v[3];
        pose.nod = v[4];
        pose.jaw = Math.max(0.0F, v[5]);
        pose.legSpread = Math.max(0.0F, v[6]);
        pose.arm[0] = v[7];
        pose.arm[1] = v[8];
        pose.spread[0] = v[9];
        pose.spread[1] = v[10];
        pose.armTwist[0] = v[11];
        pose.armTwist[1] = v[12];
    }
}
