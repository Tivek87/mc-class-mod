package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;

/**
 * Green Lantern's ring hand while he calls his Giant Hands (see {@link GiantHands}): at every hand he raises his arm
 * towards where it comes up, a quick wave up and out that sends the ring's light there, and lowers it again before the
 * next one. Seen from outside and in first person alike; calling an air strike's plane, the ring fist thrown up high
 * (see {@link CallArm}) goes over it.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class HandsArm {
    // How long the arm takes to come up, how long it keeps pointing at the hand, and how long it takes to come down
    // after that, in ticks: all of it as long as the server lets his ring hand wave.
    private static final float RAISE = 3.0F;
    private static final float DOWN = 8.0F;
    private static final float HOLD = GiantHands.WAVE_TICKS - DOWN;
    // How long it takes to fling up and out as it waves, and back.
    private static final float FLING = 8.0F;
    // Seen from outside: how far the arm flings up as it waves, in radians, and how far round from straight ahead it
    // may point, either way, in radians.
    private static final float FLING_UP = 0.5F;
    private static final double WIDEST = Math.toRadians(115.0);
    // Where the shoulder is on the body, as a part of its height and in blocks to the side.
    private static final double SHOULDER_HEIGHT = 0.8;
    private static final double SHOULDER_SIDE = 0.31;
    // Your own ring hand in first person, in blocks in front of your eyes: where it points to straight ahead, how far it
    // goes to the side and up or down, how far it flings up as it waves, and where the arm reaches in from.
    private static final Vector3f AHEAD = new Vector3f(0.18F, -0.12F, -0.86F);
    private static final float SIDEWAYS = 0.5F;
    private static final float UPWARDS = 0.32F;
    private static final float FIRST_FLING = 0.16F;
    static final Vector3f ARM_FROM = new Vector3f(0.85F, -0.9F, 0.25F);

    private HandsArm() {
    }

    /**
     * How far this player's ring arm is up to wave his hands on, 0 to 1: up, held and down again at every hand. The
     * next hand only comes once it is down (see {@link GiantHands#WAVE_TICKS}), so it always starts from down.
     */
    static float out(Entity player, float partialTick) {
        ClientConstructs.Wave wave = ClientConstructs.wave(player.getId(), partialTick);
        if (wave == null) {
            return 0.0F;
        }
        double clock = wave.clock();
        return (float) (Ease.smooth(clock / RAISE) * (1.0 - Ease.smooth((clock - HOLD) / DOWN)));
    }

    /**
     * How far the arm is flung up and out right now, 0 to 1 and back: a wave at every hand, quick up and settling
     * back without a jolt.
     */
    private static double fling(double clock) {
        double u = Mth.clamp(clock / FLING, 0.0, 1.0);
        return 6.75 * u * (1.0 - u) * (1.0 - u);
    }

    /** The way the arm points, from {@code from}: towards the newest hand. */
    private static Vec3 pointing(ClientConstructs.Wave wave, Vec3 from) {
        return wave.newest().subtract(from);
    }

    /**
     * Seen from outside: the ring arm flung out towards the newest hand, low and far or high and near. The ring fist
     * thrown up to call an air strike's plane (posed just before) goes over it, as far as it is up.
     */
    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        if (arm != HumanoidArm.RIGHT) {
            return;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float out = out(entity, partialTick) * (1.0F - CallArm.up(entity, partialTick));
        ClientConstructs.Wave wave = ClientConstructs.wave(entity.getId(), partialTick);
        if (out <= 0.0F || wave == null) {
            return;
        }
        double yaw = Math.toRadians(Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        Vec3 shoulder = entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * SHOULDER_HEIGHT, 0.0)
                .subtract(left.scale(SHOULDER_SIDE));
        Vec3 from = FlightPose.turned(entity, shoulder, partialTick);
        Vec3 way = FlightPose.untilted(entity, pointing(wave, from));
        if (way.lengthSqr() < 1.0E-6) {
            return;
        }
        way = way.normalize();
        // Never round behind him: at most this far round to either side.
        double ahead = way.dot(forward);
        double side = way.dot(left);
        double round = Math.atan2(side, ahead);
        if (Math.abs(round) > WIDEST) {
            double flat = Math.sqrt(ahead * ahead + side * side);
            double turned = Math.copySign(WIDEST, round);
            way = forward.scale(Math.cos(turned) * flat).add(left.scale(Math.sin(turned) * flat)).add(0.0, way.y, 0.0);
        }
        // The model's own directions: x to the left, y down, z back (see LanternArms.reach).
        double x = way.dot(left);
        double y = -way.y;
        double z = -way.dot(forward);
        float xRot = (float) Math.asin(Mth.clamp(z, -1.0, 1.0)) - FLING_UP * (float) fling(wave.clock());
        model.rightArm.xRot = Mth.lerp(out, model.rightArm.xRot, xRot);
        model.rightArm.yRot = Mth.lerp(out, model.rightArm.yRot, 0.0F);
        model.rightArm.zRot = Mth.lerp(out, model.rightArm.zRot, (float) Math.atan2(-x, y));
    }

    /**
     * Your own ring hand in first person, flung out towards every hand as you call it. While your ring fist is thrown
     * up to call an air strike's plane, {@link CallArm} draws it (from where this one has it).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isInvisible() || event.getHand() != InteractionHand.MAIN_HAND
                || !player.getMainHandItem().isEmpty() || ClientRing.recharge(player, event.getPartialTick()) >= 0.0F
                || CallArm.up(player, event.getPartialTick()) > 0.0F) {
            return;
        }
        Vector3f hand = hand(player, event.getPartialTick());
        if (hand == null) {
            return;
        }
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        RechargeAnimation.arm(pose, event.getMultiBufferSource(), event.getPackedLight(), player, renderer, 1.0F, hand,
                ARM_FROM);
    }

    /**
     * Where your own ring hand is in first person while you wave, in blocks in front of your eyes: from where the
     * game rests it out towards the newest hand, as far as your arm is up; null while you do not wave.
     */
    @Nullable
    static Vector3f hand(LocalPlayer player, float partialTick) {
        float out = out(player, partialTick);
        ClientConstructs.Wave wave = ClientConstructs.wave(player.getId(), partialTick);
        if (out <= 0.0F || wave == null) {
            return null;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 to = pointing(wave, camera.getPosition());
        Vec3 look = new Vec3(camera.getLookVector());
        Vec3 up = new Vec3(camera.getUpVector());
        Vec3 left = new Vec3(camera.getLeftVector());
        // How far round to the right and how far up it is from where you look, kept on your screen.
        double round = Math.atan2(-to.dot(left), to.dot(look));
        double high = Math.atan2(to.dot(up), Math.sqrt(sq(to.dot(left)) + sq(to.dot(look))));
        Vector3f hand = new Vector3f(AHEAD).add((float) (SIDEWAYS * Math.sin(Mth.clamp(round, -1.3, 1.3))),
                (float) (UPWARDS * Math.sin(Mth.clamp(high, -1.0, 1.0)) + FIRST_FLING * fling(wave.clock())), 0.0F);
        return new Vector3f(RechargeAnimation.HAND_RIGHT).lerp(hand, out);
    }

    private static double sq(double value) {
        return value * value;
    }
}
