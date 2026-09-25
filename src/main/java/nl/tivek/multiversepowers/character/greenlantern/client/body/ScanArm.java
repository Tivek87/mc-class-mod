package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
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
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ScanArm {
    private static final float RAISE = 5.0F;
    private static final float SWEEP_FROM = 4.0F;
    private static final float SWEEP_TO = 34.0F;
    private static final float DOWN = 42.0F;
    private static final float HOLD = -1.42F;
    private static final float SWING = 0.55F;
    private static final Vector3f HELD = new Vector3f(0.22F, -0.2F, -0.82F);
    private static final float SWEEP = 0.26F;
    private static final Vector3f ARM_FROM = new Vector3f(0.85F, -0.9F, 0.25F);

    private ScanArm() {
    }

    static float out(Entity player, float partialTick) {
        float age = ClientConstructs.scanAge(player.getId(), partialTick);
        if (age < 0.0F || age > DOWN) {
            return 0.0F;
        }
        return (float) Ease.smooth(age / RAISE) * (1.0F - (float) Ease.smooth((age - SWEEP_TO) / (DOWN - SWEEP_TO)));
    }

    private static float sweep(Entity player, float partialTick) {
        float age = Math.max(0.0F, ClientConstructs.scanAge(player.getId(), partialTick));
        return -1.0F + 2.0F * (float) Ease.smooth((age - SWEEP_FROM) / (SWEEP_TO - SWEEP_FROM));
    }

    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        if (arm != HumanoidArm.RIGHT) {
            return;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float out = out(entity, partialTick);
        if (out <= 0.0F) {
            return;
        }
        float look = Mth.clamp(model.head.xRot, -0.8F, 0.8F) * 0.6F;
        model.rightArm.xRot = Mth.lerp(out, model.rightArm.xRot, HOLD + look);
        model.rightArm.yRot = Mth.lerp(out, model.rightArm.yRot, SWING * sweep(entity, partialTick));
        model.rightArm.zRot = Mth.lerp(out, model.rightArm.zRot, 0.0F);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isInvisible() || event.getHand() != InteractionHand.MAIN_HAND
                || !player.getMainHandItem().isEmpty() || ClientRing.recharge(player, event.getPartialTick()) >= 0.0F
                || CallArm.up(player, event.getPartialTick()) > 0.0F
                || HandsArm.out(player, event.getPartialTick()) > 0.0F) {
            return;
        }
        float out = out(player, event.getPartialTick());
        if (out <= 0.0F) {
            return;
        }
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        Vector3f held = new Vector3f(HELD).add(SWEEP * sweep(player, event.getPartialTick()), 0.0F, 0.0F);
        RechargeAnimation.arm(pose, event.getMultiBufferSource(), event.getPackedLight(), player, renderer, 1.0F,
                new Vector3f(RechargeAnimation.HAND_RIGHT).lerp(held, out), ARM_FROM);
    }
}
