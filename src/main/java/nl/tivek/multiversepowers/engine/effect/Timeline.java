package nl.tivek.multiversepowers.engine.effect;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

// A power's moments in order, written once and run as an Effect: at tick 12 do this, every tick from 5 to 20 do that,
// and once something happens (it lands, it hits) do the rest, each counted from that moment. It ends after its last
// moment and once nothing it waits for can still come, or when told to stop.
public final class Timeline implements Effect {
    @FunctionalInterface
    public interface Step {
        // `age` counts from the timeline's start, or from the moment a `when` came true.
        void run(ServerLevel level, int age);
    }

    @FunctionalInterface
    public interface Until {
        boolean happened(ServerLevel level, int age);
    }

    private record Moment(int from, int to, Step step) {
    }

    private static final class Watch {
        final Until until;
        final int giveUp;
        final Timeline then;
        int since = -1;

        Watch(Until until, int giveUp, Timeline then) {
            this.until = until;
            this.giveUp = giveUp;
            this.then = then;
        }
    }

    private final List<Moment> moments = new ArrayList<>();
    private final List<Watch> watches = new ArrayList<>();
    private boolean stopped;

    public Timeline at(int tick, Step step) {
        return this.during(tick, tick, step);
    }

    public Timeline during(int from, int to, Step step) {
        if (from < 0 || to < from) {
            throw new IllegalArgumentException("A moment runs from " + from + " to " + to);
        }
        this.moments.add(new Moment(from, to, step));
        return this;
    }

    // Waits (until tick `giveUp` at the latest) for something to happen; `then` runs from that tick on, its own ticks
    // counted from it.
    public Timeline when(Until until, int giveUp, Timeline then) {
        this.watches.add(new Watch(until, giveUp, then));
        return this;
    }

    public void stop() {
        this.stopped = true;
    }

    public Timeline start(ServerLevel level) {
        Effects.start(level, this);
        return this;
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (this.stopped) {
            return false;
        }
        boolean more = false;
        for (Moment moment : this.moments) {
            if (age >= moment.from() && age <= moment.to()) {
                moment.step().run(level, age);
            }
            more |= age < moment.to();
        }
        for (Watch watch : this.watches) {
            if (watch.since < 0 && age <= watch.giveUp && watch.until.happened(level, age)) {
                watch.since = age;
            }
            if (watch.since >= 0) {
                more |= watch.then.tick(level, age - watch.since);
            } else {
                more |= age < watch.giveUp;
            }
        }
        return more && !this.stopped;
    }
}
