package nl.tivek.multiversepowers.character.greenlantern.client.victim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;

// The creatures the abyss's tentacles hold, as every client knows them: which pit holds each, so a tentacle winds
// round it, and held it keeps its own pose, never limp, down to where it dies under the pit's floor.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Abyssed {
    private static final int KEEP = 260;
    private static final Map<Integer, int[]> HELD = new HashMap<>();
    private static int ticks;

    private Abyssed() {
    }

    static void mark(int entity, int hand) {
        HELD.put(entity, new int[] { hand, ticks });
    }

    static void free(int entity) {
        HELD.remove(entity);
    }

    public static boolean has(int entity) {
        return HELD.containsKey(entity);
    }

    // The creatures a pit's tentacles hold, in the order they were taken.
    public static List<Entity> heldBy(int hand) {
        List<Entity> held = new ArrayList<>();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return held;
        }
        HELD.entrySet().stream().filter(entry -> entry.getValue()[0] == hand)
                .sorted((a, b) -> Integer.compare(a.getValue()[1], b.getValue()[1])).forEach(entry -> {
                    Entity entity = level.getEntity(entry.getKey());
                    if (entity != null) {
                        held.add(entity);
                    }
                });
        return held;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        ticks++;
        HELD.values().removeIf(held -> ticks - held[1] > KEEP);
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        HELD.clear();
    }
}
