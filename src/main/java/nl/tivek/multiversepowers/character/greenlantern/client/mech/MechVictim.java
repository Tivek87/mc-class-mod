package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;

// The creature under the mech's blows, as every client draws it: pressed flat under the foot and the head, squeezed
// thin and lifted by the clap. The server only keeps it on its spot.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class MechVictim {
    private static final double WIDE = 0.65;
    private static final Set<Integer> DRAWN = new HashSet<>();

    private MechVictim() {
    }

    // Last of all, so a creature some other handler cancels the drawing of is never squashed.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (event.isCanceled()) {
            return;
        }
        ClientConstructs.Piloted mech = ClientConstructs.mechVictim(event.getEntity().getId(), event.getPartialTick());
        if (mech == null) {
            return;
        }
        MechScript.Victim victim = MechScript.victim(mech.t());
        if (victim.high() >= 1.0 && victim.narrow() >= 1.0 && victim.lift() <= 0.0) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0.0, victim.lift(), 0.0);
        Vec3 across = mech.stage().right();
        float turn = (float) Math.atan2(across.x, across.z);
        pose.mulPose(Axis.YP.rotation(turn));
        pose.scale(1.0F, 1.0F, (float) victim.narrow());
        pose.mulPose(Axis.YP.rotation(-turn));
        float wide = (float) (1.0 + WIDE * (1.0 - victim.high()));
        pose.scale(wide, (float) Math.max(0.05, victim.high()), wide);
        DRAWN.add(event.getEntity().getId());
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (DRAWN.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }
}
