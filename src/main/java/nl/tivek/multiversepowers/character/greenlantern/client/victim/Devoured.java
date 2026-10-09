package nl.tivek.multiversepowers.character.greenlantern.client.victim;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;

// A creature a hand has swallowed, as every client draws it: into the maw's mouth it shrinks away to nothing, into the
// rift it sinks below the surface; wholly in, it is not drawn at all, until the hand lets go of it.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Devoured {
    private static final double SMALLEST = 0.08;
    // How far past its own size a creature sinks, so its top is under the surface too.
    private static final double BELOW = 0.4;
    private static final Set<Integer> DRAWN = new HashSet<>();

    private Devoured() {
    }

    // Whether a hand has swallowed this creature now: it keeps its own pose then, never limp.
    public static boolean has(int entity) {
        return ClientConstructs.devoured(entity, 0.0F) != null;
    }

    // Last of all, so a creature some other handler cancels the drawing of is left alone.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (event.isCanceled()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        ClientConstructs.Devour devour = ClientConstructs.devoured(entity.getId(), event.getPartialTick());
        if (devour == null || devour.shrink() <= 0.0 && devour.sink() <= 0.0) {
            return;
        }
        if (devour.gone()) {
            event.setCanceled(true);
            return;
        }
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        double tall = entity.getBbHeight();
        if (devour.shrink() > 0.0) {
            float size = (float) Mth.lerp(devour.shrink(), 1.0, SMALLEST);
            pose.translate(0.0, tall * 0.5, 0.0);
            pose.scale(size, size, size);
            pose.translate(0.0, -tall * 0.5, 0.0);
        }
        if (devour.sink() > 0.0) {
            Vec3 down = devour.down().scale(devour.sink() * (Math.max(tall, entity.getBbWidth()) + BELOW));
            pose.translate(down.x, down.y, down.z);
        }
        DRAWN.add(entity.getId());
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (DRAWN.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }
}
