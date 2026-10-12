package nl.tivek.multiversepowers.creature.client;

import java.util.HashMap;
import java.util.Map;
import nl.tivek.multiversepowers.engine.math.Spring;

// Each creature's pose followed on springs, so it never snaps from one move into the next (a rush cut short, a dodge
// in the middle of a slash): every channel (a lean, an arm's swing) eases over and settles a little past, as a body
// does. One per kind of creature, keyed by each one's id.
public final class PoseSprings {
    // A gap longer than this (not drawn for a while) starts it over where the pose is.
    private static final double GAP = 5.0;
    private static final long KEEP = 40L;

    private static final class State {
        final Spring[] springs;
        double time;
        long seen;

        State(float[] start) {
            this.springs = new Spring[start.length];
            for (int i = 0; i < start.length; i++) {
                this.springs[i] = new Spring(start[i]);
            }
        }
    }

    private final Map<Integer, State> states = new HashMap<>();
    private final double frequency;
    private final double damping;
    // Each channel's own, when it has its own (a heavy trunk slower than a light hand).
    private final double[] frequencies;
    private final double[] dampings;

    // `frequency`: how quickly it follows (cycles a tick); `damping`: 1 settles without going past.
    public PoseSprings(double frequency, double damping) {
        this.frequency = frequency;
        this.damping = damping;
        this.frequencies = null;
        this.dampings = null;
    }

    // A frequency and a damping for each channel.
    public PoseSprings(double[] frequencies, double[] dampings) {
        this.frequency = frequencies[0];
        this.damping = dampings[0];
        this.frequencies = frequencies.clone();
        this.dampings = dampings.clone();
    }

    // The channels as the creature `id` shows them at `time` (ticks), following `target`, written back into it.
    public void follow(int id, float[] target, double time, long gameTime) {
        if (this.states.size() > 64) {
            this.states.values().removeIf(state -> gameTime - state.seen > KEEP);
        }
        State state = this.states.get(id);
        double dt = state == null ? -1.0 : time - state.time;
        if (state == null || state.springs.length != target.length || dt < 0.0 || dt > GAP) {
            state = new State(target);
            this.states.put(id, state);
            dt = 0.0;
        }
        state.time = time;
        state.seen = gameTime;
        boolean own = this.frequencies != null && this.frequencies.length >= target.length;
        for (int i = 0; i < target.length; i++) {
            double frequency = own ? this.frequencies[i] : this.frequency;
            double damping = own ? this.dampings[i] : this.damping;
            target[i] = (float) (dt > 0.0 ? state.springs[i].step(target[i], dt, frequency, damping)
                    : state.springs[i].value);
        }
    }
}
