package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientFlight;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.render.PowerBattery;
import nl.tivek.multiversepowers.engine.client.render.FirstPersonArm;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class RechargeAnimation {
    private static final float RAISED = 4.0F;
    private static final float WIND = 8.0F;
    private static final float HIT = PowerRing.RECHARGE_HIT;
    private static final float BACK = PowerRing.RECHARGE_BACK;
    private static final float GONE = PowerRing.RECHARGE_TICKS;
    private static final float POP = 2.5F;
    private static final float FLASH = 7.0F;
    private static final float SHAKE = 7.0F;

    private static final Vector3f GRIP_UP = new Vector3f(-0.22F, 0.20F, -0.80F);
    private static final Vector3f GRIP_DOWN = new Vector3f(-0.55F, -1.0F, -0.80F);
    private static final Vector3f LEFT_FROM = new Vector3f(-1.52F, -0.34F, 0.33F);
    private static final Vector3f HIT_POINT = new Vector3f(0.17F, -0.24F, 0.18F);
    private static final Vector3f WIND_BACK = new Vector3f(0.12F, 0.25F, 0.25F);
    private static final Vector3f KNOCK = new Vector3f(-0.045F, -0.018F, -0.09F);
    private static final float HELD_SCALE = 0.56F;
    private static final float WORN_SCALE = 0.8F;
    private static final float FULL_WIND = 1.5F;

    static final Vector3f SHOULDER_RIGHT = FirstPersonArm.SHOULDER_RIGHT;
    static final Vector3f SHOULDER_LEFT = FirstPersonArm.SHOULDER_LEFT;
    public static final Vector3f HAND_RIGHT = FirstPersonArm.HAND_RIGHT;

    private RechargeAnimation() {
    }

    static float lift(float t) {
        return (float) Ease.smooth(t / RAISED) * (1.0F - (float) Ease.smooth((t - BACK) / (GONE - BACK)));
    }

    static boolean heldAlready(Entity player) {
        return ClientRing.arrival(player, 0.0F) >= 0.0F;
    }

    static float lift(Entity player, float t) {
        return heldAlready(player) && t < BACK ? 1.0F : lift(t);
    }

    static float shown(Entity player, float t) {
        return heldAlready(player) && t < GONE - POP ? 1.0F : shown(t);
    }

    static float press(float t) {
        if (t < RAISED) {
            return 0.0F;
        }
        if (t < WIND) {
            return 0.3F * (float) Ease.smooth((t - RAISED) / (WIND - RAISED));
        }
        if (t < HIT) {
            float in = (t - WIND) / (HIT - WIND);
            return 0.3F + 0.7F * in * in * in;
        }
        if (t < BACK) {
            return 1.0F - 0.1F * kick(t);
        }
        return 1.0F - (float) Ease.smooth((t - BACK) / (GONE - 2.0F - BACK));
    }

    static float wind(float t) {
        if (t < RAISED || t >= HIT) {
            return 0.0F;
        }
        if (t < WIND) {
            return (float) Ease.smooth((t - RAISED) / (WIND - RAISED));
        }
        float in = (t - WIND) / (HIT - WIND);
        return 1.0F - in * in * in;
    }

    static float kick(float t) {
        if (t < HIT) {
            return 0.0F;
        }
        float fade = Mth.clamp(1.0F - (t - HIT) / SHAKE, 0.0F, 1.0F);
        return fade * fade;
    }

    static float burst(float t) {
        if (t < HIT) {
            return 0.0F;
        }
        return Mth.clamp(1.0F - (t - HIT) / (BACK - HIT), 0.0F, 1.0F);
    }

    static float shown(float t) {
        return (float) Ease.smooth(t / POP) * (1.0F - (float) Ease.smooth((t - (GONE - POP)) / POP));
    }

    static float glow(float t) {
        if (t < HIT) {
            return 0.25F + 0.45F * (float) Ease.smooth((t - RAISED) / (HIT - RAISED));
        }
        if (t < BACK) {
            return 1.0F + 1.9F * kick(t);
        }
        return 1.0F - (float) Ease.smooth((t - BACK) / (GONE - BACK));
    }

    public static float flash(float t) {
        if (t < HIT || t > HIT + FLASH) {
            return 0.0F;
        }
        float fade = 1.0F - (t - HIT) / FLASH;
        return fade * fade;
    }


    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float t = ClientRing.recharge(entity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (t < 0.0F) {
            return;
        }
        float look = Mth.clamp(model.head.xRot, -0.8F, 0.8F) * 0.5F;
        if (arm == HumanoidArm.LEFT) {
            float lift = lift(entity, t);
            model.leftArm.xRot = Mth.lerp(lift, model.leftArm.xRot, -1.85F + look - kick(t) * 0.2F);
            model.leftArm.yRot = Mth.lerp(lift, model.leftArm.yRot, 0.4F);
            model.leftArm.zRot = Mth.lerp(lift, model.leftArm.zRot, 0.0F);
        } else {
            float press = press(t);
            float wind = wind(t);
            model.rightArm.xRot = Mth.lerp(press, model.rightArm.xRot, -1.5F + look + wind * 0.75F);
            model.rightArm.yRot = Mth.lerp(press, model.rightArm.yRot, -0.6F - wind * 0.25F);
            model.rightArm.zRot = Mth.lerp(press, model.rightArm.zRot, 0.12F + wind * 0.35F);
        }
    }

    static void lanternInHand(PoseStack poseStack, MultiBufferSource buffers, ModelPart arm, boolean slim,
            Entity player, float t, @Nullable Quaternionf bodyTurn) {
        lanternInHand(poseStack, buffers, arm, slim, shown(player, t), glow(t), burst(t), bodyTurn);
    }

    static void lanternInHand(PoseStack poseStack, MultiBufferSource buffers, ModelPart arm, boolean slim,
            float shown, float glow, float burst, @Nullable Quaternionf bodyTurn) {
        if (shown <= 0.0F) {
            return;
        }
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        poseStack.translate((slim ? 0.5F : 1.0F) / 16.0F, 10.0F / 16.0F, 0.0F);
        poseStack.mulPose(new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot).invert());
        if (bodyTurn != null) {
            poseStack.mulPose(new Quaternionf(bodyTurn).invert());
        }
        // A player model is drawn upside down (y runs down); the lantern is made with y up.
        poseStack.scale(1.0F, -1.0F, 1.0F);
        float scale = WORN_SCALE * shown;
        poseStack.scale(scale, scale, scale);
        PowerBattery.draw(poseStack, buffers, glow, burst);
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        float t = ClientRing.recharge(player, event.getPartialTick());
        if (t < 0.0F) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND || player.isInvisible()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);

        float kick = kick(t);
        float gust = ClientRing.flight(player, event.getPartialTick()) < 0.0F ? 0.0F
                : Mth.clamp((float) ClientFlight.ownVelocity().length() / FULL_WIND, 0.0F, 1.0F);
        float time = player.tickCount + event.getPartialTick();
        Vector3f grip = new Vector3f(GRIP_DOWN).lerp(GRIP_UP, lift(player, t))
                .add(KNOCK.x * kick, KNOCK.y * kick, KNOCK.z * kick)
                .add(gust * 0.012F * Mth.sin(time * 1.9F), gust * (0.01F * Mth.sin(time * 2.7F) - 0.04F),
                        gust * 0.05F);
        arm(pose, buffers, light, player, renderer, -1.0F, grip, LEFT_FROM);
        float shown = shown(player, t);
        if (shown > 0.0F) {
            pose.pushPose();
            pose.translate(grip.x, grip.y, grip.z);
            pose.mulPose(Axis.YP.rotationDegrees(-14.0F));
            pose.mulPose(Axis.XP.rotationDegrees(gust * (10.0F + 2.0F * Mth.sin(time * 2.3F))));
            float scale = HELD_SCALE * shown;
            pose.scale(scale, scale, scale);
            PowerBattery.draw(pose, buffers, glow(t), burst(t));
            pose.popPose();
        }

        float wind = wind(t);
        Vector3f fist = new Vector3f(HAND_RIGHT).lerp(new Vector3f(grip).add(HIT_POINT), press(t))
                .add(WIND_BACK.x * wind, WIND_BACK.y * wind, WIND_BACK.z * wind);
        arm(pose, buffers, light, player, renderer, 1.0F, fist, SHOULDER_RIGHT);
    }

    public static void arm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            PlayerRenderer renderer, float side, Vector3f hand, Vector3f from) {
        FirstPersonArm.arm(pose, buffers, light, player, renderer, side, hand, from);
    }
}
