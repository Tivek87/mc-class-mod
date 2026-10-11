package nl.tivek.multiversepowers.character.greenlantern.client.victim;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.render.entity.ShadedBuffers;

// The creatures the hourglass stopped in time, as every client draws them: washed pale and grey, every part of them
// still, down to the swing of an arm, until time goes on again.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class TimeStopped {
    private static final int WASH = 0xB4CCBC;
    private static final float WHITEN = 0.35F;
    private static final int KEEP = 200;
    // Per creature: the tick it stopped (the client's count) and its own age then.
    private static final Map<Integer, int[]> STOPPED = new HashMap<>();
    private static int ticks;
    private static boolean redrawing;

    private TimeStopped() {
    }

    static void stop(int entity) {
        Entity living = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(entity);
        STOPPED.put(entity, new int[] { ticks, living == null ? 0 : living.tickCount });
    }

    static void goOn(int entity) {
        STOPPED.remove(entity);
    }

    public static boolean has(int entity) {
        return STOPPED.containsKey(entity);
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        int[] stopped = STOPPED.get(entity.getId());
        if (stopped == null || redrawing) {
            return;
        }
        event.setCanceled(true);
        redrawing = true;
        int age = entity.tickCount;
        try {
            entity.tickCount = stopped[1];
            redraw(event, entity);
        } finally {
            entity.tickCount = age;
            redrawing = false;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void redraw(RenderLivingEvent.Pre event, LivingEntity entity) {
        event.getRenderer().render(entity, entity.getYRot(), 0.0F, event.getPoseStack(),
                new ShadedBuffers(event.getMultiBufferSource(), WASH, false, WHITEN), event.getPackedLight());
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        ticks++;
        STOPPED.values().removeIf(stopped -> ticks - stopped[0] > KEEP);
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        STOPPED.clear();
    }
}
