package nl.tivek.multiversepowers.creature.animal.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Mth;

// Each animal's stride as a clock of its own: a phase that runs on as fast as its feet must go round to keep up with
// how fast it goes, so a foot set down stays where it is while the body goes over it, a slow walk taking slow steps
// and a run long ones, where the game's own walk clock turns at the same pace for every gait. One per kind of animal,
// keyed by each one's id.
public final class Strides {
    // A gap longer than this (not drawn for a while) goes on from where it was without running the time between.
    private static final double GAP = 5.0;
    private static final long KEEP = 40L;

    private static final class State {
        float phase;
        double time;
        long seen;
    }

    private final Map<Integer, State> states = new HashMap<>();

    // The phase of the animal `id` at `time` (ticks), run on at `rate` (radians a tick) since it was last asked.
    public float phase(int id, double time, long gameTime, float rate) {
        if (this.states.size() > 64) {
            this.states.values().removeIf(state -> gameTime - state.seen > KEEP);
        }
        State state = this.states.get(id);
        if (state == null) {
            state = new State();
            state.phase = id * 2.399963F % Mth.TWO_PI;
            state.time = time;
            this.states.put(id, state);
        }
        double dt = time - state.time;
        if (dt > 0.0 && dt <= GAP) {
            state.phase += (float) (rate * dt);
            if (state.phase > 64.0F * Mth.TWO_PI) {
                state.phase -= 64.0F * Mth.TWO_PI;
            }
        }
        state.time = time;
        state.seen = gameTime;
        return state.phase;
    }

    // How fast a stride's phase must turn for a foot on the ground `duty` of each stride to sweep `sweep` pixels back
    // while the body goes `speed` pixels a tick: the game's walk speed (0 to 1) is four times the blocks it goes a tick.
    public static float rate(float walkSpeed, float duty, float sweep) {
        float speed = 4.0F * walkSpeed;
        return Mth.TWO_PI * speed * duty / Math.max(1.0F, sweep);
    }
}
