package nl.tivek.multiversepowers.character.greenlantern.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientLooks;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.body.LanternArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.RechargeAnimation;
import nl.tivek.multiversepowers.character.greenlantern.client.body.Ring;
import nl.tivek.multiversepowers.character.greenlantern.client.body.RingSpot;
import nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamPainter;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalDeparture.LAUNCH;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalDeparture.departing;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalLight.aura;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalLight.burst;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalLight.eyes;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalLight.pillar;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalLight.scan;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalLight.wave;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalRing.acrossOf;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalRing.ring;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalRing.ringAt;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalRing.ringSize;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ArrivalAnimation {
    static final float STREAK_TICKS = 5.0F;
    static final double STREAK_HIGH = 40.0;
    static final float SET_OFF = Arrival.SET_OFF;
    static final float ARRIVES = Arrival.APPROACH - 2.0F;
    private static final float FLASH_TICKS = 6.0F;
    static final int TRAIL = 14;
    private static final float TRAIL_STEP = 0.55F;
    private static final float EYES_TICKS = 16.0F;
    static final float EYES_OUT = 8.0F;
    static final double FAR_SIZE = 0.5;
    static final double HOVER_SIZE = 0.3;
    static final double FINGER_SIZE = 0.1;
    static final double OWN_FINGER_SIZE = 0.05;
    private static final double LANTERN_DOWN = 0.8;
    private static final double LANTERN_LEFT = 0.7;
    private static final float LANTERN_SCALE = 0.8F;
    // These are in first-person eye space: x right, y up, -z ahead
    private static final Vector3f CATCH = new Vector3f(-0.34F, 0.02F, -0.95F);
    private static final Vector3f FIST_UP = new Vector3f(0.3F, -0.1F, -0.72F);
    private static final Vector3f LEFT_FROM = new Vector3f(-1.52F, -0.34F, 0.33F);
    private static final Vector3f RIGHT_FROM = new Vector3f(1.5F, -0.4F, 0.35F);
    private static final Vector3f GRIP_DOWN = new Vector3f(-0.55F, -1.0F, -0.8F);
    private static final Vector3f GRIP_UP = new Vector3f(-0.22F, 0.2F, -0.8F);

    private static final Map<Integer, Float> DEPARTING = new HashMap<>();

    private ArrivalAnimation() {
    }

    public static boolean holdsLantern(Entity player, float partialTick) {
        float a = ClientRing.arrival(player, partialTick);
        return a >= Arrival.LANTERN_CAUGHT && ClientRing.recharge(player, partialTick) < 0.0F;
    }

    public static float lanternGlow(Entity player, float partialTick) {
        float a = ClientRing.arrival(player, partialTick);
        float flare = a >= Arrival.RING_ON ? Math.max(0.0F, 1.0F - (a - Arrival.RING_ON) / 7.0F) : 0.0F;
        return 0.4F + 0.12F * Mth.sin(a * 0.3F) + 0.8F * flare;
    }

    private static float reach(float a) {
        return (float) Ease.smooth((a - Arrival.LANTERN_FORMED + 4.0F) / 6.0F);
    }

    private static float hold(float a) {
        return (float) Ease.smooth((a - Arrival.LANTERN_CAUGHT + 1.0F) / 3.0F);
    }

    private static float fistUp(float a) {
        return (float) Ease.smooth((a - Arrival.RING_FLY + 3.0F) / 4.0F)
                * (1.0F - (float) Ease.smooth((a - Arrival.RING_ON - 12.0F) / 8.0F));
    }

    static Vec3 ahead(Entity player, float partialTick) {
        Vec3 look = player.getViewVector(partialTick);
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        return flat.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick))
                : flat.normalize();
    }

    private static Vec3 hover(Entity player, float partialTick, float a) {
        return player.getEyePosition(partialTick).add(ahead(player, partialTick).scale(Arrival.HOVER))
                .add(0.0, 0.1 + 0.06 * Mth.sin(a * 0.2F), 0.0);
    }

    private static Vec3 lanternHome(Entity player, float partialTick, float a) {
        Vec3 ahead = ahead(player, partialTick);
        Vec3 left = new Vec3(ahead.z, 0.0, -ahead.x);
        return hover(player, partialTick, a).add(left.scale(LANTERN_LEFT)).subtract(0.0, LANTERN_DOWN, 0.0);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        LanternPainter painter = null;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        for (AbstractClientPlayer player : level.players()) {
            float a = ClientRing.arrival(player, partialTick);
            float d = ClientLooks.departure(player, partialTick);
            if ((a < 0.0F || a > Arrival.DRESSED + EYES_TICKS) && d < 0.0F) {
                continue;
            }
            if (painter == null) {
                painter = new LanternPainter(event.getPoseStack(), camera.getPosition(),
                        (float) (level.getGameTime() % 24000L) + partialTick);
            }
            if (a >= 0.0F) {
                arriving(event, painter, buffers, player, a, partialTick);
            } else {
                departing(event, painter, buffers, player, d, partialTick);
            }
        }
        if (painter != null) {
            buffers.endBatch(Ring.BAND);
            buffers.endBatch(Ring.HALO);
            painter.finish(buffers);
        }
    }

    private static void arriving(RenderLevelStageEvent event, LanternPainter painter,
            MultiBufferSource.BufferSource buffers, AbstractClientPlayer player, float a, float partialTick) {
        Camera camera = event.getCamera();
        boolean own = player == Minecraft.getInstance().player && !camera.isDetached();
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 hover = hover(player, partialTick, a);
        if (a < Arrival.RING_ON) {
            Vec3 shown = ClientRing.arrivalFrom(player);
            Vec3 from = shown == null ? hover : shown;
            Vec3 finger = finger(event, player, partialTick);
            Vec3 at = ringAt(from, eye, hover, finger, a);
            double size = ringSize(a, own);
            Vec3 toEye = eye.subtract(at);
            Vec3 face = toEye.lengthSqr() < 1.0E-6 ? ahead(player, partialTick).scale(-1.0) : toEye.normalize();
            float scanning = a >= Arrival.SCAN && a < Arrival.SCANNED ? 1.0F : 0.0F;
            ring(event, buffers, at, face, a * (0.12 + 0.25 * scanning), size);
            float pulse = 0.5F + 0.5F * Mth.sin(a * (0.8F + 0.6F * scanning));
            double far = Mth.clamp(at.distanceTo(eye) / 20.0, 0.15, 1.0);
            painter.flare(at, (0.35 + 0.9 * far) * (0.8 + 0.4 * pulse), 0.75 + 0.25 * pulse);
            if (a >= STREAK_TICKS && a < STREAK_TICKS + FLASH_TICKS) {
                float u = (a - STREAK_TICKS) / FLASH_TICKS;
                painter.flare(at, 6.0 * (1.0F - u) * (0.5 + far), 1.0F - u);
                burst(painter, at, face, u, 4.5 * (0.5 + far));
            }
            if (a >= STREAK_TICKS && a < SET_OFF + 4.0F) {
                for (int k = 0; k < 2; k++) {
                    float age = (a + k * 4.0F) % 8.0F;
                    Vec3[] across = acrossOf(face);
                    painter.circle(at, across[0], across[1], (0.2 + 0.375 * age) * (0.5 + far),
                            0.04 * (0.5 + far), 0.2 * (0.5 + far), Colors.alpha(0.9 * (1.0 - age / 8.0)),
                            Colors.alpha(0.45 * (1.0 - age / 8.0)));
                }
            }
            float stop = a - ARRIVES;
            if (stop >= 0.0F && stop < FLASH_TICKS) {
                float u = stop / FLASH_TICKS;
                painter.flare(at, 1.4 * (1.0F - u), 1.0F - u);
                burst(painter, at, face, u, 1.8);
            }
            boolean streaking = a < STREAK_TICKS + 2.0F || a > SET_OFF && a < ARRIVES + 2.0F;
            if (streaking || a > Arrival.RING_FLY) {
                int bits = streaking ? TRAIL : 7;
                Vec3 last = at;
                for (int k = 1; k <= bits; k++) {
                    Vec3 next = ringAt(from, eye, hover, finger, Math.max(0.0F, a - TRAIL_STEP * k));
                    double fade = 1.0 - (double) k / (bits + 1);
                    painter.edge(last, next, Math.max(size, 0.25 * far) * 0.55 * fade, 0.95 * fade);
                    last = next;
                }
            }
            scan(painter, player, at, a, partialTick);
            lantern(event, painter, buffers, player, at, a, partialTick, own);
            return;
        }
        float since = a - Arrival.RING_ON;
        Vec3 finger = finger(event, player, partialTick);
        if (since < 6.0F) {
            float burst = 1.0F - since / 6.0F;
            painter.flare(finger, (0.5 + 2.0 * burst) * (own ? 0.5 : 1.0), burst);
        }
        Vec3 feet = player.getPosition(partialTick);
        wave(painter, feet, since);
        // Skipped in first person: it would run straight up through your own eyes
        if (!own) {
            pillar(painter, feet, since);
        }
        aura(painter, feet, player.getBbHeight(), since);
        float lit = a - Arrival.DRESSED;
        if (!own && lit >= 0.0F && lit < EYES_TICKS) {
            eyes(painter, player, lit < 3.0F ? lit / 3.0F : 1.0F - (lit - 3.0F) / (EYES_TICKS - 3.0F), partialTick);
        }
    }

    static Vec3 finger(RenderLevelStageEvent event, AbstractClientPlayer player, float partialTick) {
        Vec3 seen = RingSpot.of(player, event.getCamera(), event.getProjectionMatrix(), event.getModelViewMatrix());
        return seen != null ? seen : LanternArms.ringPoint(player, partialTick);
    }

    private static void lantern(RenderLevelStageEvent event, LanternPainter painter,
            MultiBufferSource.BufferSource buffers, AbstractClientPlayer player, Vec3 ring, float a, float partialTick,
            boolean own) {
        if (a < Arrival.LANTERN_FORM || a >= Arrival.LANTERN_CAUGHT) {
            return;
        }
        Vec3 home = lanternHome(player, partialTick, a);
        float form = (a - Arrival.LANTERN_FORM) / (Arrival.LANTERN_FORMED - Arrival.LANTERN_FORM);
        float grown = (float) SlamPainter.backOut(form);
        float fly = (float) Ease.smooth((a - Arrival.LANTERN_FORMED)
                / (Arrival.LANTERN_CAUGHT - Arrival.LANTERN_FORMED));
        Vec3 hand = leftHand(event.getCamera(), player, partialTick, own);
        Vec3 at = home.lerp(hand, fly).add(0.0, 0.5 * Mth.sin(Mth.PI * fly), 0.0);
        if (a < Arrival.LANTERN_FORMED + 2.0F) {
            painter.beam(ring, at.subtract(0.0, 0.35, 0.0), 1.0, 0.8);
        }
        float done = (a - Arrival.LANTERN_FORMED) / FLASH_TICKS;
        if (done >= 0.0F && done < 1.0F) {
            painter.flare(at, 1.2 * (1.0F - done), 1.0F - done);
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
        Vec3 ahead = ahead(player, partialTick);
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(-ahead.x, -ahead.z)));
        pose.mulPose(Axis.YP.rotation(0.6F * (1.0F - fly) * Mth.sin(a * 0.25F)));
        float scale = LANTERN_SCALE * Mth.clamp(grown, 0.0F, 1.2F) * (own ? Mth.lerp(fly, 1.0F, 0.7F) : 1.0F);
        pose.scale(scale, scale, scale);
        PowerBattery.draw(pose, buffers, 0.5F + 0.6F * (1.0F - form), 0.0F,
                1.0F - (float) Ease.smooth((form - 0.1F) / 0.8F));
        pose.popPose();
    }

    private static Vec3 leftHand(Camera camera, AbstractClientPlayer player, float partialTick, boolean own) {
        if (own) {
            Vec3 forward = new Vec3(camera.getLookVector());
            Vec3 up = new Vec3(camera.getUpVector());
            Vec3 left = new Vec3(camera.getLeftVector());
            return camera.getPosition().add(forward.scale(-GRIP_UP.z)).subtract(left.scale(GRIP_UP.x))
                    .add(up.scale(GRIP_UP.y));
        }
        double yaw = Math.toRadians(Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        return player.getPosition(partialTick).add(0.0, player.getBbHeight() * 0.8, 0.0).add(left.scale(0.31))
                .add(forward.scale(0.62)).add(0.0, 0.05, 0.0);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        float undress = ClientLooks.UNDRESS_TICKS;
        float span = ClientLooks.DEPART_TICKS - undress;
        for (AbstractClientPlayer player : level.players()) {
            float d = ClientLooks.departure(player, 0.0F);
            if (d < 0.0F) {
                DEPARTING.remove(player.getId());
                continue;
            }
            Float was = DEPARTING.put(player.getId(), d);
            float before = was == null ? -1.0F : was;
            Vec3 at = player.position().add(0.0, 1.2, 0.0);
            if (passed(before, d, 0.0F)) {
                level.playLocalSound(at.x, at.y, at.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.3F,
                        false);
            }
            if (passed(before, d, undress)) {
                level.playLocalSound(at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F,
                        1.2F, false);
            }
            if (passed(before, d, undress + span * LAUNCH)) {
                level.playLocalSound(at.x, at.y, at.z, SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS,
                        0.9F, 1.5F, false);
            }
            if (passed(before, d, undress + span * 0.96F)) {
                level.playLocalSound(at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.6F,
                        1.8F, false);
            }
        }
        DEPARTING.keySet().removeIf(id -> level.getEntity(id) == null);
    }

    private static boolean passed(float before, float now, float moment) {
        return before < moment && now >= moment;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DEPARTING.clear();
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float since = ClientRing.arrival(player, (float) event.getPartialTick()) - Arrival.RING_ON;
        if (since < 0.0F || since > 10.0F) {
            return;
        }
        float shake = (1.0F - since / 10.0F) * ClientSettings.cameraShake();
        float time = player.tickCount + (float) event.getPartialTick();
        event.setPitch(event.getPitch() + 1.6F * shake * Mth.sin(time * 3.1F));
        event.setYaw(event.getYaw() + 1.2F * shake * Mth.sin(time * 3.9F + 1.0F));
    }

    public static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float a = ClientRing.arrival(entity, partialTick);
        if (a < 0.0F) {
            return;
        }
        float look = Mth.clamp(model.head.xRot, -0.8F, 0.8F) * 0.5F;
        if (arm == HumanoidArm.LEFT) {
            float reach = reach(a);
            float hold = hold(a);
            float x = Mth.lerp(hold, -1.45F + look, -1.85F + look);
            float y = Mth.lerp(hold, 0.25F, 0.4F);
            model.leftArm.xRot = Mth.lerp(reach, model.leftArm.xRot, x);
            model.leftArm.yRot = Mth.lerp(reach, model.leftArm.yRot, y);
            model.leftArm.zRot = Mth.lerp(reach, model.leftArm.zRot, 0.0F);
        } else {
            float up = fistUp(a);
            model.rightArm.xRot = Mth.lerp(up, model.rightArm.xRot, -1.35F + look);
            model.rightArm.yRot = Mth.lerp(up, model.rightArm.yRot, -0.25F);
            model.rightArm.zRot = Mth.lerp(up, model.rightArm.zRot, 0.05F);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        float a = ClientRing.arrival(player, event.getPartialTick());
        if (a < Arrival.LANTERN_FORMED - 4.0F || ClientRing.recharge(player, event.getPartialTick()) >= 0.0F) {
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
        Vector3f grip = new Vector3f(GRIP_DOWN).lerp(CATCH, reach(a)).lerp(GRIP_UP, hold(a));
        RechargeAnimation.arm(pose, buffers, light, player, renderer, -1.0F, grip, LEFT_FROM);
        if (a >= Arrival.LANTERN_CAUGHT) {
            pose.pushPose();
            pose.translate(grip.x, grip.y, grip.z);
            pose.mulPose(Axis.YP.rotationDegrees(-14.0F));
            pose.scale(0.56F, 0.56F, 0.56F);
            PowerBattery.draw(pose, buffers, lanternGlow(player, event.getPartialTick()), 0.0F);
            pose.popPose();
        }
        Vector3f fist = new Vector3f(RechargeAnimation.HAND_RIGHT).lerp(FIST_UP, fistUp(a));
        RechargeAnimation.arm(pose, buffers, light, player, renderer, 1.0F, fist, RIGHT_FROM);
    }
}
