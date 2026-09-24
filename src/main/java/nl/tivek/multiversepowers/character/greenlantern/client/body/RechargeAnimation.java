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
import nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation;
import nl.tivek.multiversepowers.character.greenlantern.client.render.PowerBattery;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Recharging the ring, as everyone sees it: the lantern appears in the left hand (the hand that defends)
 * and is raised, the right fist (the one with the ring) swings up over it and smacks it on the back, its
 * light blasts white out of the front, burns on while the fist stays against it, and then it dies down and
 * the lantern is gone.
 * The server keeps the time (see {@link PowerRing#RECHARGE_TICKS}); this only plays it.
 *
 * <ul>
 * <li>In first person your own arms are drawn here instead of by the game: both hands and the lantern,
 * each arm being the game's own empty hand, turned about its shoulder so the hand gets where it has to.</li>
 * <li>Seen from outside (other players, or you in third person) the arms take a pose of their own (see
 * {@link LanternPose}) and the lantern hangs from the left hand.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class RechargeAnimation {
    // The timeline, in ticks: the lantern is up, the fist is cocked back above it, it smacks the lantern
    // (the server's moment), the light dies down and the fist comes off, and the lantern is gone.
    private static final float RAISED = 4.0F;
    private static final float WIND = 8.0F;
    private static final float HIT = PowerRing.RECHARGE_HIT;
    private static final float BACK = PowerRing.RECHARGE_BACK;
    private static final float GONE = PowerRing.RECHARGE_TICKS;
    // How long the lantern takes to grow out of the light, and to shrink back into it.
    private static final float POP = 2.5F;
    // How long the burst of light stays on your screen, and how long the smack keeps shaking the lantern.
    private static final float FLASH = 7.0F;
    private static final float SHAKE = 7.0F;

    // ---- First person: points in front of your eyes (x to the right, y up, -z ahead), in blocks ----
    // Where the left hand grips the lantern once it is raised (a little left of and under your crosshair, so
    // the lantern hangs in front of you), and where it comes up from (out of view).
    // It is held closer than an arm's length away on purpose: hold it further out and the shoulder end of
    // that arm lands behind your eyes, where the near edge of the screen cuts the arm open and you look
    // straight through it.
    private static final Vector3f GRIP_UP = new Vector3f(-0.22F, 0.20F, -0.80F);
    private static final Vector3f GRIP_DOWN = new Vector3f(-0.55F, -1.0F, -0.80F);
    // The left arm reaches in from here, well out on your left, low and behind: it then comes in across the
    // left edge of the screen with its far end out of sight, and never reaches back past your eyes.
    private static final Vector3f LEFT_FROM = new Vector3f(-1.52F, -0.34F, 0.33F);
    // Where the fist lands, from the grip: against the back of the lantern, the side facing you, so the
    // light blasts out of its front, away from you.
    private static final Vector3f HIT_POINT = new Vector3f(0.17F, -0.24F, 0.18F);
    // Where the fist waits before it comes down, from where it lands: pulled back up and towards you.
    private static final Vector3f WIND_BACK = new Vector3f(0.12F, 0.25F, 0.25F);
    // How far the smack knocks the lantern away from the fist, in blocks.
    private static final Vector3f KNOCK = new Vector3f(-0.045F, -0.018F, -0.09F);
    // The lantern in your own hand, as a part of its real size; seen from outside it is about a third of
    // your height.
    private static final float HELD_SCALE = 0.56F;
    private static final float WORN_SCALE = 0.8F;
    // Flying speed at which the wind tugs at the lantern in your hand the hardest, in blocks per tick.
    private static final float FULL_WIND = 1.5F;

    // Your arms where the game holds an empty hand: the shoulder, the grip, and how long the arm is between.
    static final Vector3f SHOULDER_RIGHT = restPoint(1.0F, -2.0F);
    static final Vector3f SHOULDER_LEFT = restPoint(-1.0F, -2.0F);
    public static final Vector3f HAND_RIGHT = restPoint(1.0F, 9.0F);
    private static final Vector3f HAND_LEFT = restPoint(-1.0F, 9.0F);

    private RechargeAnimation() {
    }

    // ---- The timeline ----

    /** How far the lantern is raised: 0 = down out of sight, 1 = up in front of you. */
    static float lift(float t) {
        return (float) Ease.smooth(t / RAISED) * (1.0F - (float) Ease.smooth((t - BACK) / (GONE - BACK)));
    }

    /**
     * True when this recharge ends the ring's arrival (see {@link ArrivalAnimation}): the lantern is in the left hand,
     * raised, already, since it flew there; it does not come up out of nowhere first.
     */
    static boolean heldAlready(Entity player) {
        return ClientRing.arrival(player, 0.0F) >= 0.0F;
    }

    /** {@link #lift}, for this player: raised from the start when the lantern is in the hand already. */
    static float lift(Entity player, float t) {
        return heldAlready(player) && t < BACK ? 1.0F : lift(t);
    }

    /** {@link #shown}, for this player: all there from the start when the lantern is in the hand already. */
    static float shown(Entity player, float t) {
        return heldAlready(player) && t < GONE - POP ? 1.0F : shown(t);
    }

    /**
     * How far the right fist has come in: 0 = still down by your side, 1 = against the lantern. It swings up
     * and waits a moment, comes down hard, bounces off the hit, rests against the lantern while the light
     * burns, and drops back at the end.
     */
    static float press(float t) {
        if (t < RAISED) {
            return 0.0F;
        }
        if (t < WIND) {
            return 0.3F * (float) Ease.smooth((t - RAISED) / (WIND - RAISED));
        }
        if (t < HIT) {
            // Not a smooth curve but a rising one: slow at first and fastest right before it lands.
            float in = (t - WIND) / (HIT - WIND);
            return 0.3F + 0.7F * in * in * in;
        }
        if (t < BACK) {
            return 1.0F - 0.1F * kick(t);
        }
        return 1.0F - (float) Ease.smooth((t - BACK) / (GONE - 2.0F - BACK));
    }

    /** How far the fist is cocked back, away from where it lands: 1 while it waits, 0 the moment it hits. */
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

    /** The jolt of the smack: 1 the moment it lands, shaken out over the next few ticks. */
    static float kick(float t) {
        if (t < HIT) {
            return 0.0F;
        }
        float fade = Mth.clamp(1.0F - (t - HIT) / SHAKE, 0.0F, 1.0F);
        return fade * fade;
    }

    /** How hard the light blasts out of the front of the lantern: hardest on the smack, then it dies away. */
    static float burst(float t) {
        if (t < HIT) {
            return 0.0F;
        }
        return Mth.clamp(1.0F - (t - HIT) / (BACK - HIT), 0.0F, 1.0F);
    }

    /** How much of the lantern is there: it grows out of the light as it appears, and shrinks back into it. */
    static float shown(float t) {
        return (float) Ease.smooth(t / POP) * (1.0F - (float) Ease.smooth((t - (GONE - POP)) / POP));
    }

    /**
     * How brightly the lantern burns: it wakes up while the fist swings, bursts white the moment the fist
     * lands, burns on while the two stay together, and dies down at the end.
     */
    static float glow(float t) {
        if (t < HIT) {
            return 0.25F + 0.45F * (float) Ease.smooth((t - RAISED) / (HIT - RAISED));
        }
        if (t < BACK) {
            return 1.0F + 1.9F * kick(t);
        }
        return 1.0F - (float) Ease.smooth((t - BACK) / (GONE - BACK));
    }

    /** The flash of the hit over your screen: 1 on the hit, fading out quickly. */
    public static float flash(float t) {
        if (t < HIT || t > HIT + FLASH) {
            return 0.0F;
        }
        float fade = 1.0F - (t - HIT) / FLASH;
        return fade * fade;
    }


    // ---- Seen from outside ----

    /**
     * The pose itself, one arm at a time: the left arm raises the lantern in front of the chest, the right arm
     * comes up and in towards it, a little lower, where its light is.
     */
    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float t = ClientRing.recharge(entity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (t < 0.0F) {
            return;
        }
        // Looking up or down tips the arms along a little.
        float look = Mth.clamp(model.head.xRot, -0.8F, 0.8F) * 0.5F;
        if (arm == HumanoidArm.LEFT) {
            // Raised up and forward, so the hand is at the height of the head and the lantern hangs in front
            // of the chest; the smack shoves it a little further out.
            float lift = lift(entity, t);
            model.leftArm.xRot = Mth.lerp(lift, model.leftArm.xRot, -1.85F + look - kick(t) * 0.2F);
            model.leftArm.yRot = Mth.lerp(lift, model.leftArm.yRot, 0.4F);
            model.leftArm.zRot = Mth.lerp(lift, model.leftArm.zRot, 0.0F);
        } else {
            // Up and back over the lantern first, then down onto it.
            float press = press(t);
            float wind = wind(t);
            model.rightArm.xRot = Mth.lerp(press, model.rightArm.xRot, -1.5F + look + wind * 0.75F);
            model.rightArm.yRot = Mth.lerp(press, model.rightArm.yRot, -0.6F - wind * 0.25F);
            model.rightArm.zRot = Mth.lerp(press, model.rightArm.zRot, 0.12F + wind * 0.35F);
        }
    }

    /**
     * The lantern in a left hand, seen from outside. The pose stack must stand in the player model, as it does
     * in a render layer; the lantern hangs straight down from the hand whatever the arm does, and in flight
     * whatever the body does, so its light blasts out ahead of him.
     *
     * @param bodyTurn how the flight pose turns the body (see {@link FlightPose#bodyTurn}), or null
     */
    static void lanternInHand(PoseStack poseStack, MultiBufferSource buffers, ModelPart arm, boolean slim,
            Entity player, float t, @Nullable Quaternionf bodyTurn) {
        lanternInHand(poseStack, buffers, arm, slim, shown(player, t), glow(t), burst(t), bodyTurn);
    }

    /**
     * The lantern in a left hand, seen from outside, as far as it is there ({@code shown}), burning {@code glow} and
     * blasting {@code burst} out of its front (see {@link PowerBattery#draw}).
     */
    static void lanternInHand(PoseStack poseStack, MultiBufferSource buffers, ModelPart arm, boolean slim,
            float shown, float glow, float burst, @Nullable Quaternionf bodyTurn) {
        if (shown <= 0.0F) {
            return;
        }
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        // The grip: the middle of the hand, at the fingertips.
        poseStack.translate((slim ? 0.5F : 1.0F) / 16.0F, 10.0F / 16.0F, 0.0F);
        poseStack.mulPose(new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot).invert());
        if (bodyTurn != null) {
            poseStack.mulPose(new Quaternionf(bodyTurn).invert());
        }
        // A player model is drawn upside down (y runs down); the lantern is made with y up.
        poseStack.scale(1.0F, -1.0F, 1.0F);
        float scale = WORN_SCALE * shown;
        poseStack.scale(scale, scale, scale);
        // A model's own front is -z, and so is the lantern's: its light blasts out away from the chest.
        PowerBattery.draw(poseStack, buffers, glow, burst);
        poseStack.popPose();
    }

    // ---- First person ----

    /**
     * Your own hands while you recharge: instead of what the game would draw (an item, or your empty hand),
     * both arms and the lantern. All of it is drawn along with the main hand; the other hand draws nothing.
     */
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

        // The left hand comes up with the lantern and grips it by its handle; the smack shoves it away. In flight
        // the wind pushes it back and down a little and makes it tremble, the harder the faster you fly.
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
            // Turned a little so you see it is round. Its front (-z of its own) looks away from you, so the
            // fist lands on the back and the light blasts out ahead of you. The wind tips its top towards you.
            pose.mulPose(Axis.YP.rotationDegrees(-14.0F));
            pose.mulPose(Axis.XP.rotationDegrees(gust * (10.0F + 2.0F * Mth.sin(time * 2.3F))));
            float scale = HELD_SCALE * shown;
            pose.scale(scale, scale, scale);
            PowerBattery.draw(pose, buffers, glow(t), burst(t));
            pose.popPose();
        }

        // The right fist swings up over the lantern and comes down on its back.
        float wind = wind(t);
        Vector3f fist = new Vector3f(HAND_RIGHT).lerp(new Vector3f(grip).add(HIT_POINT), press(t))
                .add(WIND_BACK.x * wind, WIND_BACK.y * wind, WIND_BACK.z * wind);
        arm(pose, buffers, light, player, renderer, 1.0F, fist, SHOULDER_RIGHT);
    }

    /**
     * Draws one arm with its hand at {@code hand}: the game's own empty hand, turned about its shoulder until it
     * points from {@code from} at the hand, and slid along that line until it gets there. {@code from} lies out
     * of view, so the shoulder does too and the arm always reaches in from the edge of the screen, the way the
     * game's own does.
     *
     * @param side 1 for the right arm, -1 for the left
     */
    public static void arm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            PlayerRenderer renderer, float side, Vector3f hand, Vector3f from) {
        Vector3f restShoulder = side > 0.0F ? SHOULDER_RIGHT : SHOULDER_LEFT;
        Vector3f restHand = new Vector3f(side > 0.0F ? HAND_RIGHT : HAND_LEFT);
        Vector3f restWay = new Vector3f(restHand).sub(restShoulder).normalize();
        Vector3f toHand = new Vector3f(hand).sub(from).normalize();
        pose.pushPose();
        pose.translate(hand.x, hand.y, hand.z);
        pose.mulPose(new Quaternionf().rotationTo(restWay, toHand));
        pose.translate(-restHand.x, -restHand.y, -restHand.z);
        emptyHand(pose, side);
        if (side > 0.0F) {
            renderer.renderRightHand(pose, buffers, light, player);
        } else {
            renderer.renderLeftHand(pose, buffers, light, player);
        }
        pose.popPose();
    }

    /** The game's own way of holding an empty hand in first person, not swinging and fully raised. */
    private static void emptyHand(PoseStack pose, float side) {
        pose.translate(side * 0.64000005F, -0.6F, -0.71999997F);
        pose.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
        pose.translate(side * -1.0F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        pose.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
        pose.translate(side * 5.6F, 0.0F, 0.0F);
    }

    /**
     * Where a point along the middle of an arm ends up in front of your eyes when the game draws an empty
     * hand. {@code along} is in the arm's own pixels, from its shoulder (-2) to its fingertips (10).
     */
    private static Vector3f restPoint(float side, float along) {
        PoseStack pose = new PoseStack();
        emptyHand(pose, side);
        // What drawing the arm adds: its pivot at the shoulder, and the little sway it always has.
        pose.translate(side * -5.0F / 16.0F, 2.0F / 16.0F, 0.0F);
        pose.mulPose(Axis.ZP.rotation(side * 0.1F));
        return pose.last().pose().transformPosition(side * -1.0F / 16.0F, along / 16.0F, 0.0F, new Vector3f());
    }
}
