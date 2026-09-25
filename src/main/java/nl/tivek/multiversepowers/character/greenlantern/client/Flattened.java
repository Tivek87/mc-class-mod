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
import nl.tivek.multiversepowers.engine.math.Ease;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Flattened {
    private static final double SQUASH = 1.5;
    private static final double FLAT = 30.0;
    private static final double BACK = 9.0;
    private static final double LOW = 0.14;
    private static final double WIDE = 0.65;

    private static final Map<Integer, Integer> SQUASHED = new HashMap<>();
    private static final Set<Integer> DRAWN = new HashSet<>();
    private static int ticks;

    private Flattened() {
    }

    public static void flatten(int entity) {
        SQUASHED.put(entity, ticks);
    }

    static void clear() {
        SQUASHED.clear();
        DRAWN.clear();
    }

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

    // Last of all, so a creature some other handler cancels the drawing of is never squashed.
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
