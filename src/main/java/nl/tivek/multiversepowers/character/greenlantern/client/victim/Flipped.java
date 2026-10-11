package nl.tivek.multiversepowers.character.greenlantern.client.victim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;

// The creatures the gravity flip turned over, as every client draws them: tumbling head over heels as they fall up,
// hanging upside down, and righted as they are slammed back into the ground.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Flipped {
    private static final double TURNS = 8.0;
    private static final double RIGHTS = 4.0;
    private static final int KEEP = 160;
    // Per creature: the tick it was turned over and the tick it landed (-1 while it still hangs).
    private static final Map<Integer, int[]> FLIPPED = new HashMap<>();
    private static final Set<Integer> PUSHED = new HashSet<>();
    private static int ticks;

    private Flipped() {
    }

    static void flip(int entity) {
        FLIPPED.put(entity, new int[] { ticks, -1 });
    }

    static void land(int entity) {
        int[] flipped = FLIPPED.get(entity);
        if (flipped != null && flipped[1] < 0) {
            flipped[1] = ticks;
        }
    }

    public static boolean has(int entity) {
        return FLIPPED.containsKey(entity);
    }

    // Last, so a creature some other look draws itself (and cancels) is never turned without being turned back.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        int[] flipped = FLIPPED.get(entity.getId());
        if (flipped == null || event.isCanceled()) {
            return;
        }
        float partialTick = event.getPartialTick();
        double over = Ease.smoother((ticks - flipped[0] + partialTick) / TURNS);
        if (flipped[1] >= 0) {
            over *= 1.0 - Ease.smoother((ticks - flipped[1] + partialTick) / RIGHTS);
        }
        if (over <= 0.001) {
            return;
        }
        double yaw = Math.toRadians(entity.getViewYRot(partialTick));
        float middle = entity.getBbHeight() * 0.5F;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0.0F, middle, 0.0F);
        pose.mulPose(Axis.of(new Vector3f((float) Math.cos(yaw), 0.0F, (float) Math.sin(yaw)))
                .rotation((float) (Math.PI * over)));
        pose.translate(0.0F, -middle, 0.0F);
        PUSHED.add(entity.getId());
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (PUSHED.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        ticks++;
        FLIPPED.values().removeIf(flipped -> flipped[1] >= 0 ? ticks - flipped[1] > RIGHTS + 2 : ticks - flipped[0]
                > KEEP);
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        FLIPPED.clear();
        PUSHED.clear();
    }
}
