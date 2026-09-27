package nl.tivek.multiversepowers.engine.client.fx;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

// Stands a third-person camera further back than the game would, round something much bigger than its own player (see
// CameraMixin): a rig names the point to circle and how far to stand from it. The camera keeps its own turn; walls pull
// it in towards that point.
public final class ChaseCamera {
    private static final List<Rigger> RIGGERS = new ArrayList<>();

    public record Rig(Vec3 pivot, double distance) {
    }

    @FunctionalInterface
    public interface Rigger {
        @Nullable
        Rig rig(float partialTick);
    }

    private ChaseCamera() {
    }

    public static void add(Rigger rigger) {
        RIGGERS.add(rigger);
    }

    // Where a third-person camera looking along forward stands, or null to leave it where the game put it.
    @Nullable
    public static Vec3 eye(Vec3 forward, float partialTick) {
        for (Rigger rigger : RIGGERS) {
            Rig rig = rigger.rig(partialTick);
            if (rig != null) {
                return Cinematic.clear(rig.pivot(), rig.pivot().subtract(forward.scale(rig.distance())));
            }
        }
        return null;
    }
}
