package nl.tivek.multiversepowers.engine.world;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.ToDoubleFunction;
import net.minecraft.world.entity.Entity;

// How much faster a power's own flight goes where its flyer is: 1 everywhere, unless something that knows of a place
// with nothing to fly into (open space) says more. Every flight asks here, on the server and in the flyer's own game.
public final class FlightLanes {
    private static final List<ToDoubleFunction<Entity>> LANES = new CopyOnWriteArrayList<>();

    private FlightLanes() {
    }

    public static void add(ToDoubleFunction<Entity> lane) {
        LANES.add(lane);
    }

    public static double haste(Entity flyer) {
        double most = 1.0;
        for (ToDoubleFunction<Entity> lane : LANES) {
            most = Math.max(most, lane.applyAsDouble(flyer));
        }
        return most;
    }
}
