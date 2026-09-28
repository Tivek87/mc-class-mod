package nl.tivek.multiversepowers.engine.client.fx;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

// Where your own eye is in first person when a power moves it (a pilot looking out through the front of their
// machine rather than from deep inside it); the first power with a place for it wins (see CameraMixin).
public final class FirstPersonEye {
    @FunctionalInterface
    public interface Place {
        @Nullable
        Vec3 eye(float partialTick);
    }

    private static final List<Place> PLACES = new ArrayList<>();

    private FirstPersonEye() {
    }

    public static void add(Place place) {
        PLACES.add(place);
    }

    @Nullable
    public static Vec3 eye(float partialTick) {
        for (Place place : PLACES) {
            Vec3 eye = place.eye(partialTick);
            if (eye != null) {
                return eye;
            }
        }
        return null;
    }
}
