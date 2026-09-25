package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.client.render.PlanePainter;
import org.joml.Vector3f;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class CallArm {
    static final Vector3f UP_HIGH = new Vector3f(0.42F, 0.24F, -0.78F);
    private static final Vector3f ARM_FROM = new Vector3f(0.75F, -1.1F, -0.15F);
    private static final float RAISED = -2.75F;
    private static final float RAISED_OUT = -0.4F;

    private CallArm() {
    }

    public static float up(Entity player, float partialTick) {
        return PlanePainter.raised(player, partialTick);
    }

    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        if (arm != HumanoidArm.RIGHT) {
            return;
        }
        float up = up(entity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (up <= 0.0F) {
            return;
        }
        model.rightArm.xRot = Mth.lerp(up, model.rightArm.xRot, RAISED);
        model.rightArm.yRot = Mth.lerp(up, model.rightArm.yRot, 0.0F);
        model.rightArm.zRot = Mth.lerp(up, model.rightArm.zRot, RAISED_OUT);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isInvisible() || event.getHand() != InteractionHand.MAIN_HAND
                || !player.getMainHandItem().isEmpty()) {
            return;
        }
        float up = up(player, event.getPartialTick());
        if (up <= 0.0F) {
            return;
        }
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        Vector3f waving = HandsArm.hand(player, event.getPartialTick());
        Vector3f from = waving == null ? new Vector3f(RechargeAnimation.HAND_RIGHT) : waving;
        Vector3f arm = waving == null ? new Vector3f(ARM_FROM) : new Vector3f(HandsArm.ARM_FROM).lerp(ARM_FROM, up);
        RechargeAnimation.arm(pose, buffers, event.getPackedLight(), player, renderer, 1.0F, from.lerp(UP_HIGH, up),
                arm);
    }
}
