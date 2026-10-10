package nl.tivek.multiversepowers.character.greenlantern.client.body.pose;

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
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.flight.ClientFlight;
import nl.tivek.multiversepowers.character.greenlantern.client.render.PowerLantern;
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import nl.tivek.multiversepowers.engine.client.render.entity.FirstPersonArm;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class RechargeAnimation {
    // The lantern forms out of the ring's light (its metal glowing green, cooling to gold) as the left hand brings it
    // up before him; the right fist comes slowly to it, stops just short and presses the ring to the emblem at HIT,
    // knocking the lantern back a little; held there while the glass lights from inside and crackles and the ring
    // fills, then drawn back at BACK as the lantern flashes, is lowered and goes back into light.
    private static final float RAISED = 6.0F;
    private static final float HIT = PowerRing.RECHARGE_HIT;
    private static final float BACK = PowerRing.RECHARGE_BACK;
    private static final float GONE = PowerRing.RECHARGE_TICKS;
    private static final float FORM = 8.0F;
    private static final float PUSH = 4.0F;
    private static final float SHORT = 0.86F;
    private static final float KNOCK = 5.0F;
    private static final float FLASH = 7.0F;

    // Where the left hand holds the lantern up in first person, how it is turned and how big: the arrival's too.
    public static final Vector3f GRIP_UP = new Vector3f(-0.24F, 0.12F, -0.95F);
    private static final Vector3f GRIP_DOWN = new Vector3f(-0.55F, -1.0F, -0.80F);
    private static final Vector3f LEFT_FROM = new Vector3f(-1.52F, -0.34F, 0.33F);
    // From the grip to where the fist presses the ring to the emblem, turned towards it from the right.
    private static final Vector3f HIT_POINT = new Vector3f(0.2F, -0.38F, 0.11F);
    public static final float FACING = 32.0F;
    public static final float HELD_SCALE = 0.72F;
    private static final float WORN_SCALE = 0.8F;
    private static final float FULL_WIND = 1.5F;

    public static final Vector3f SHOULDER_RIGHT = FirstPersonArm.SHOULDER_RIGHT;
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
        return heldAlready(player) && t < GONE - FORM ? 1.0F : shown(t);
    }

    // How far the right fist has come to the emblem: slowly in to just short of it, a firm press home at HIT, held,
    // slowly back.
    static float press(float t) {
        if (t < RAISED) {
            return 0.0F;
        }
        if (t < HIT - PUSH) {
            return SHORT * (float) Ease.smooth((t - RAISED) / (HIT - PUSH - RAISED));
        }
        if (t < BACK) {
            float u = Mth.clamp((t - (HIT - PUSH)) / PUSH, 0.0F, 1.0F);
            return SHORT + (1.0F - SHORT) * u * u;
        }
        return 1.0F - (float) Ease.smooth((t - BACK) / (GONE - 2.0F - BACK));
    }

    // The lantern knocked back by the ring's touch, 0 to 1 and back.
    static float knock(float t) {
        return (float) Ease.jolt((t - HIT) / KNOCK);
    }

    // How much the lantern's metal still glows with the light it is made of: while it forms and as it goes.
    static float hot(Entity player, float t) {
        return 1.0F - shown(player, t);
    }

    // How far the charge has come, 0 at the touch to 1 when it is done.
    public static float charge(float t) {
        return (float) Mth.clamp((t - HIT) / (BACK - HIT), 0.0F, 1.0F);
    }

    // A small shudder through both hands while the ring takes the charge.
    static float tremble(float t) {
        if (t < HIT || t > BACK) {
            return 0.0F;
        }
        return 0.6F + 0.4F * charge(t);
    }

    // The flash in the emblem as the charge is full.
    static float burst(float t) {
        if (t < BACK) {
            return 0.0F;
        }
        return Mth.clamp(1.0F - (t - BACK) / FLASH, 0.0F, 1.0F);
    }

    static float shown(float t) {
        return (float) Ease.smooth(t / FORM) * (1.0F - (float) Ease.smooth((t - (GONE - FORM)) / FORM));
    }

    // The lantern's light: dark until the ring touches, rising with the charge and throbbing with the whirr, a flash
    // when full, then fading.
    public static float glow(float t) {
        if (t < HIT) {
            return 0.04F;
        }
        if (t < BACK) {
            float u = charge(t);
            return 0.04F + 0.96F * u * (float) Math.sqrt(u) * (0.9F + 0.1F * Mth.sin(t * (1.2F + 1.6F * u)));
        }
        return 1.0F + 0.8F * burst(t) - 0.6F * (float) Ease.smooth((t - BACK) / (GONE - BACK));
    }

    public static float flash(float t) {
        float burst = burst(t);
        return burst * burst * 0.6F;
    }

    public static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float t = ClientRing.recharge(entity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (t < 0.0F) {
            return;
        }
        float look = Mth.clamp(model.head.xRot, -0.8F, 0.8F) * 0.5F;
        if (arm == HumanoidArm.LEFT) {
            float lift = lift(entity, t);
            model.leftArm.xRot = Mth.lerp(lift, model.leftArm.xRot, -1.85F + look + shake(entity, t));
            model.leftArm.yRot = Mth.lerp(lift, model.leftArm.yRot, 0.4F);
            model.leftArm.zRot = Mth.lerp(lift, model.leftArm.zRot, 0.0F);
        } else {
            float press = press(t);
            model.rightArm.xRot = Mth.lerp(press, model.rightArm.xRot, -1.0F + look + shake(entity, t));
            model.rightArm.yRot = Mth.lerp(press, model.rightArm.yRot, -0.7F);
            model.rightArm.zRot = Mth.lerp(press, model.rightArm.zRot, 0.1F);
        }
    }

    static float shake(Entity entity, float t) {
        return 0.02F * tremble(t) * Mth.sin((entity.tickCount + t) * 3.1F);
    }

    public static void lanternInHand(PoseStack poseStack, MultiBufferSource buffers, int light, ModelPart arm,
            boolean slim, Entity player, float t, @Nullable Quaternionf bodyTurn) {
        lanternInHand(poseStack, buffers, light, arm, slim, shown(player, t), glow(t), burst(t), bodyTurn);
    }

    public static void lanternInHand(PoseStack poseStack, MultiBufferSource buffers, int light, ModelPart arm,
            boolean slim, float shown, float glow, float burst, @Nullable Quaternionf bodyTurn) {
        if (shown <= 0.0F) {
            return;
        }
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        BentParts.farHalf(arm, poseStack);
        poseStack.translate((slim ? 0.5F : 1.0F) / 16.0F, 10.0F / 16.0F, 0.0F);
        poseStack.mulPose(new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot).invert());
        if (bodyTurn != null) {
            poseStack.mulPose(new Quaternionf(bodyTurn).invert());
        }
        // A player model is drawn upside down (y runs down); the lantern is made with y up.
        poseStack.scale(1.0F, -1.0F, 1.0F);
        float scale = WORN_SCALE * shown;
        poseStack.scale(scale, scale, scale);
        PowerLantern.draw(poseStack, buffers, glow, burst, 1.0F - shown, light);
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

        float shudder = tremble(t) * 0.006F;
        float gust = ClientRing.flight(player, event.getPartialTick()) < 0.0F ? 0.0F
                : Mth.clamp((float) ClientFlight.ownVelocity().length() / FULL_WIND, 0.0F, 1.0F);
        float time = player.tickCount + event.getPartialTick();
        Vector3f grip = new Vector3f(GRIP_DOWN).lerp(GRIP_UP, lift(player, t))
                .add(shudder * Mth.sin(time * 3.1F), shudder * Mth.sin(time * 4.3F + 1.0F), 0.0F)
                .add(gust * 0.012F * Mth.sin(time * 1.9F), gust * (0.01F * Mth.sin(time * 2.7F) - 0.04F),
                        gust * 0.05F)
                .add(new Vector3f(HIT_POINT).normalize(-0.035F * knock(t)));
        arm(pose, buffers, light, player, renderer, -1.0F, grip, LEFT_FROM);
        float shown = shown(player, t);
        if (shown > 0.0F) {
            pose.pushPose();
            pose.translate(grip.x, grip.y, grip.z);
            pose.mulPose(Axis.YP.rotationDegrees(FACING));
            pose.mulPose(Axis.XP.rotationDegrees(gust * (10.0F + 2.0F * Mth.sin(time * 2.3F))));
            pose.mulPose(Axis.ZP.rotationDegrees(-4.0F * knock(t)));
            float scale = HELD_SCALE * shown;
            pose.scale(scale, scale, scale);
            PowerLantern.draw(pose, buffers, glow(t), burst(t), hot(player, t), light);
            pose.popPose();
        }

        Vector3f fist = new Vector3f(HAND_RIGHT).lerp(new Vector3f(grip).add(HIT_POINT), press(t));
        arm(pose, buffers, light, player, renderer, 1.0F, fist, SHOULDER_RIGHT);
    }

    public static void arm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            PlayerRenderer renderer, float side, Vector3f hand, Vector3f from) {
        FirstPersonArm.arm(pose, buffers, light, player, renderer, side, hand, from);
    }
}
