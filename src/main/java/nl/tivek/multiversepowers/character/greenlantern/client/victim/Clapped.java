package nl.tivek.multiversepowers.character.greenlantern.client.victim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandGroup;

// The creature between the clapping palms, as every client draws it: the palms never pass into it; it is pressed
// thin between them, trembling under the force, and springs back as they part.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Clapped {
    // How far outside the creature the palms stay, in blocks.
    private static final double SKIN = 0.03;
    // How far a person's arms stand out from their middle, wider than their box.
    private static final double ARMS = 0.5;
    private static final double TREMBLE = 0.04;
    private static final double BULGE = 0.45;
    private static final double RISE = 0.2;
    private static final Set<Integer> DRAWN = new HashSet<>();

    private Clapped() {
    }

    // Whether a clap holds this creature now: it keeps its own pose then, never limp.
    public static boolean has(int entity) {
        return ClientConstructs.clapped(entity, 0.0F) != null;
    }

    // Last of all, so a creature some other handler cancels the drawing of is never pressed.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (event.isCanceled()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        ClientConstructs.Clap clap = ClientConstructs.clapped(entity.getId(), event.getPartialTick());
        if (clap == null) {
            return;
        }
        double half = entity.getBbWidth() * 0.5;
        if (event.getRenderer().getModel() instanceof HumanoidModel<?>) {
            half = Math.max(half, ARMS * entity.getScale());
        }
        double narrow = (HandGroup.clapGap(clap.t()) - SKIN) / half;
        if (narrow >= 1.0) {
            return;
        }
        narrow = Math.max(0.15, narrow) * (1.0 + TREMBLE * Math.sin(clap.t() * 9.0));
        float turn = (float) Math.atan2(clap.across().x, clap.across().z);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation(turn));
        pose.scale((float) (1.0 + BULGE * (1.0 - narrow)), (float) (1.0 + RISE * (1.0 - narrow)), (float) narrow);
        pose.mulPose(Axis.YP.rotation(-turn));
        DRAWN.add(entity.getId());
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (DRAWN.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }
}
