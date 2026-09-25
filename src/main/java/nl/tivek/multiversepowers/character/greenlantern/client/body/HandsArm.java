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

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class HandsArm {
    private static final float RAISE = 3.0F;
    private static final float DOWN = 8.0F;
    private static final float HOLD = GiantHands.WAVE_TICKS - DOWN;
    private static final float FLING = 8.0F;
    private static final float FLING_UP = 0.5F;
    private static final double WIDEST = Math.toRadians(115.0);
    private static final double SHOULDER_HEIGHT = 0.8;
    private static final double SHOULDER_SIDE = 0.31;
    private static final Vector3f AHEAD = new Vector3f(0.18F, -0.12F, -0.86F);
    private static final float SIDEWAYS = 0.5F;
    private static final float UPWARDS = 0.32F;
    private static final float FIRST_FLING = 0.16F;
    static final Vector3f ARM_FROM = new Vector3f(0.85F, -0.9F, 0.25F);

    private HandsArm() {
    }

    static float out(Entity player, float partialTick) {
        ClientConstructs.Wave wave = ClientConstructs.wave(player.getId(), partialTick);
        if (wave == null) {
            return 0.0F;
        }
        double clock = wave.clock();
        return (float) (Ease.smooth(clock / RAISE) * (1.0 - Ease.smooth((clock - HOLD) / DOWN)));
    }

    private static double fling(double clock) {
        double u = Mth.clamp(clock / FLING, 0.0, 1.0);
        return 6.75 * u * (1.0 - u) * (1.0 - u);
    }

    private static Vec3 pointing(ClientConstructs.Wave wave, Vec3 from) {
        return wave.newest().subtract(from);
    }

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
