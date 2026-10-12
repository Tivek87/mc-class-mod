package nl.tivek.multiversepowers.engine.client.world;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.world.phys.Vec3;

// The player came into a world that lies turned to the one they left: whatever keeps a way or a speed of its own for
// them (a power's flight) is told how every way turned, so they fly on as they flew.
public final class WorldTurn {
    private static final List<Consumer<UnaryOperator<Vec3>>> KEEPERS = new CopyOnWriteArrayList<>();

    private WorldTurn() {
    }

    public static void keep(Consumer<UnaryOperator<Vec3>> keeper) {
        KEEPERS.add(keeper);
    }

    public static void turn(UnaryOperator<Vec3> turned) {
        for (Consumer<UnaryOperator<Vec3>> keeper : KEEPERS) {
            keeper.accept(turned);
        }
    }
}
