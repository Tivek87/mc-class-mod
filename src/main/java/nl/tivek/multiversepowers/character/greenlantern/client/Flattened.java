package nl.tivek.multiversepowers.character.greenlantern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands;
import nl.tivek.multiversepowers.engine.math.Ease;

/**
 * Creatures a giant hand slapped flat against the ground (see {@link GiantHands}): like in a cartoon they lie there
 * squashed flat and wide a moment, then spring back up into shape. Only how they are drawn changes.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Flattened {
    // How long a creature takes to be squashed flat, how long it lies flat, and how long it takes to spring back, in
    // ticks; and how flat and how wide it is then.
    private static final double SQUASH = 1.5;
    private static final double FLAT = 30.0;
    private static final double BACK = 9.0;
    private static final double LOW = 0.14;
    private static final double WIDE = 0.65;

    // The client tick each creature was slapped flat on, by its entity id, and the ones being drawn squashed right now.
    private static final Map<Integer, Integer> SQUASHED = new HashMap<>();
    private static final Set<Integer> DRAWN = new HashSet<>();
    private static int ticks;

    private Flattened() {
    }

    /** This creature has just been slapped flat (the server tells, see FlattenPayload). */
    public static void flatten(int entity) {
        SQUASHED.put(entity, ticks);
    }

    /** The world is left: nothing is flat any more. */
    static void clear() {
        SQUASHED.clear();
        DRAWN.clear();
    }

    /** How high a creature slapped {@code since} ticks ago is drawn, next to its own height: 1 once it is back. */
    private static double height(double since) {
        if (since < SQUASH) {
            return Mth.lerp(Ease.smooth(since / SQUASH), 1.0, LOW);
        }
        if (since < SQUASH + FLAT) {
            return LOW;
        }
        return Mth.lerp(Ease.backOut((since - SQUASH - FLAT) / BACK), LOW, 1.0);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        ticks++;
        SQUASHED.values().removeIf(start -> ticks - start > SQUASH + FLAT + BACK);
    }

    // Last of all, so a creature some other handler does not draw is never squashed (and never left squashed).
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        Integer start = SQUASHED.get(event.getEntity().getId());
        if (start == null || event.isCanceled()) {
            return;
        }
        double high = height(ticks - start + event.getPartialTick());
        double wide = 1.0 + WIDE * (1.0 - high);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.scale((float) wide, (float) Math.max(0.05, high), (float) wide);
        DRAWN.add(event.getEntity().getId());
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (DRAWN.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }
}
