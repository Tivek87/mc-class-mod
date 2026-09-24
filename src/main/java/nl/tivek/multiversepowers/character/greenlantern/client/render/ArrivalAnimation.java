package nl.tivek.multiversepowers.character.greenlantern.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The ring's arrival as everyone sees it (the server keeps the time, see {@link Arrival}), and its departure.
 * <ul>
 * <li>The ring streaks down out of the sky like a comet and flares up far off, a ring of light bursting out of it, and
 * pulses there, sending out more; then it flies in, circling him once on its way down, a long streak of light behind
 * it, and stops dead three blocks before his eyes with a flash, turning slowly.</li>
 * <li>It scans him: a band of light sweeps down him from over his head to his feet and back up, fed by a fan of light
 * out of the ring.</li>
 * <li>A beam out of it shapes the lantern below it: white-hot at first, cooling to silver, with a flash as it is done.
 * The lantern flies to his left hand, which reaches out and catches it and holds it up.</li>
 * <li>His right fist comes up and the ring shoots onto its middle finger: a flash, a ring of hard light races out over
 * the ground as far as the creatures of the dark are sent running, a pillar of light shoots up out of him into the sky
 * and the ring's light flares up round him in tongues of light. The uniform spreads from the ring (see
 * {@link ClientLooks}); once the mask is on his eyes light up, and in the end he smacks the ring into the lantern (see
 * {@link RechargeAnimation}).</li>
 * <li>Taking the uniform off, the glow of his eyes goes out and the uniform draws back into the ring (see
 * {@link ClientLooks}), specks of its light streaming off him into it; then the ring slides off his finger, rises over
 * his head and hangs there a moment, turning, sending out a last ring of light, and spirals off up into the sky, a
 * long streak behind it, until it is a twinkle high up and gone.</li>
 * </ul>
 * In first person your own arms are drawn here while the lantern comes to your hand and the ring to your finger.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ArrivalAnimation {
    // The ring out in the world: a silver band round its middle, its setting on the outside and the stone in it, a
    // band one long across; drawn bigger than on a finger, so it can be seen from far off.
    private static final Mesh BAND = Mesh.torus(20, 8, 1.0, 0.24, 1.0);
    private static final Mesh SETTING = Mesh.box(1.12, -0.3, -0.3, 1.42, 0.3, 0.3, 1.0);
    private static final Mesh STONE = Mesh.ball(10, 6, 0.26, 1.0).moved(1.5, 0.0, 0.0);
    private static final int SILVER = 0xD2DBD6;
    private static final int SETTING_COLOR = 0x4B5652;
    private static final int STONE_COLOR = 0x2EF566;
    // The ring's flight in: it streaks down out of the sky to where it shows up (this many blocks above it, coming in
    // over there), waits there pulsing, sets off, and arrives before his eyes.
    private static final float STREAK_TICKS = 5.0F;
    private static final double STREAK_HIGH = 40.0;
    private static final float SET_OFF = Arrival.SET_OFF;
    private static final float ARRIVES = Arrival.APPROACH - 2.0F;
    // How long the flash lasts where it comes to a stop (out of the sky, and before his eyes), with the ring of light
    // racing out of it, in ticks.
    private static final float FLASH_TICKS = 6.0F;
    // How many bits of the streak of light behind the flying ring there are, and how far apart in time, in ticks.
    private static final int TRAIL = 14;
    private static final float TRAIL_STEP = 0.55F;
    // The scan: how far out the band of light round him is, in blocks, and how far over his head it starts.
    private static final double SCAN_RADIUS = 0.75;
    private static final double SCAN_TOP = 0.25;
    // The ring's light flaring up round him as it slides on: how long, in ticks, and how many tongues of light.
    private static final float AURA_TICKS = 30.0F;
    private static final int AURA_TONGUES = 18;
    // The pillar of light shooting up out of him as the ring slides on: how long it takes to shoot up, and how long it
    // lasts, in ticks.
    private static final float PILLAR_RISE = 3.0F;
    private static final float PILLAR_TICKS = 16.0F;
    // His eyes lighting up once the mask is on, and going out as he takes the uniform off: how long, in ticks.
    private static final float EYES_TICKS = 16.0F;
    private static final float EYES_OUT = 8.0F;
    // The ring leaving, as parts of its flight off: it has risen over his head, and sets off up into the sky. And how
    // high it flies, and how far ahead of him, in blocks.
    private static final float RISEN = 0.25F;
    private static final float LAUNCH = 0.45F;
    private static final double AWAY_HIGH = 60.0;
    private static final double AWAY_AHEAD = 30.0;
    // How many specks of the uniform's light stream off him into the ring while it draws back.
    private static final int SPECKS = 26;
    // How big the ring is while far off, while it hangs before him, and on a finger (seen from outside and from your
    // own eyes), across in blocks.
    private static final double FAR_SIZE = 0.5;
    private static final double HOVER_SIZE = 0.3;
    private static final double FINGER_SIZE = 0.1;
    private static final double OWN_FINGER_SIZE = 0.05;
    // How far the lantern takes shape below the ring and to his left of it, in blocks, and how big it is.
    private static final double LANTERN_DOWN = 0.8;
    private static final double LANTERN_LEFT = 0.7;
    private static final float LANTERN_SCALE = 0.8F;
    // How long the ring's light takes to race out over the ground, and to sink away, in ticks.
    private static final float WAVE_TICKS = 10.0F;
    private static final float WAVE_FADE = 16.0F;
    // First person, in blocks before your eyes (x right, y up, -z ahead): where your left hand reaches out to catch
    // the lantern, and where your ring fist comes up to take the ring.
    private static final Vector3f CATCH = new Vector3f(-0.34F, 0.02F, -0.95F);
    private static final Vector3f FIST_UP = new Vector3f(0.3F, -0.1F, -0.72F);
    private static final Vector3f LEFT_FROM = new Vector3f(-1.52F, -0.34F, 0.33F);
    private static final Vector3f RIGHT_FROM = new Vector3f(1.5F, -0.4F, 0.35F);
    private static final Vector3f GRIP_DOWN = new Vector3f(-0.55F, -1.0F, -0.8F);
    private static final Vector3f GRIP_UP = new Vector3f(-0.22F, 0.2F, -0.8F);

    // How far into taking the uniform off each player was last tick (see ClientLooks#departure), for the sounds.
    private static final Map<Integer, Float> DEPARTING = new HashMap<>();

    private ArrivalAnimation() {
    }

    // ---- The timeline ----

    /** True while this player holds the lantern up in his left hand, waiting for the smack that ends the arrival. */
    public static boolean holdsLantern(Entity player, float partialTick) {
        float a = ClientRing.arrival(player, partialTick);
        return a >= Arrival.LANTERN_CAUGHT && ClientRing.recharge(player, partialTick) < 0.0F;
    }

    /** How brightly the lantern he holds burns meanwhile: it breathes, and flares as the ring slides on. */
    public static float lanternGlow(Entity player, float partialTick) {
        float a = ClientRing.arrival(player, partialTick);
        float flare = a >= Arrival.RING_ON ? Math.max(0.0F, 1.0F - (a - Arrival.RING_ON) / 7.0F) : 0.0F;
        return 0.4F + 0.12F * Mth.sin(a * 0.3F) + 0.8F * flare;
    }

    /** How far he has got his left hand out to catch the lantern, 0 to 1, and it stays out holding it. */
    private static float reach(float a) {
        return (float) Ease.smooth((a - Arrival.LANTERN_FORMED + 4.0F) / 6.0F);
    }

    /** How far he has brought the lantern up in front of him since he caught it, 0 to 1. */
    private static float hold(float a) {
        return (float) Ease.smooth((a - Arrival.LANTERN_CAUGHT + 1.0F) / 3.0F);
    }

    /** How far his ring fist is up to take the ring, 0 to 1: up as it comes, down again once the uniform runs up it. */
    private static float fistUp(float a) {
        return (float) Ease.smooth((a - Arrival.RING_FLY + 3.0F) / 4.0F)
                * (1.0F - (float) Ease.smooth((a - Arrival.RING_ON - 12.0F) / 8.0F));
    }

    /** The way ahead of this player, flat along the ground: where he looks. */
    private static Vec3 ahead(Entity player, float partialTick) {
        Vec3 look = player.getViewVector(partialTick);
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        return flat.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick))
                : flat.normalize();
    }

    /** Where the ring hangs before his eyes, bobbing gently. */
    private static Vec3 hover(Entity player, float partialTick, float a) {
        return player.getEyePosition(partialTick).add(ahead(player, partialTick).scale(Arrival.HOVER))
                .add(0.0, 0.1 + 0.06 * Mth.sin(a * 0.2F), 0.0);
    }

    /** Where the lantern takes shape: below the ring and to his left of it. */
    private static Vec3 lanternHome(Entity player, float partialTick, float a) {
        Vec3 ahead = ahead(player, partialTick);
        Vec3 left = new Vec3(ahead.z, 0.0, -ahead.x);
        return hover(player, partialTick, a).add(left.scale(LANTERN_LEFT)).subtract(0.0, LANTERN_DOWN, 0.0);
    }

    // ---- In the world ----

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

    /** One player's ring on its way to him, his lantern taking shape, and the light bursting out as the ring is on. */
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
            // The ring turns slowly about the way up, its face to him; faster while it scans him.
            Vec3 toEye = eye.subtract(at);
            Vec3 face = toEye.lengthSqr() < 1.0E-6 ? ahead(player, partialTick).scale(-1.0) : toEye.normalize();
            float scanning = a >= Arrival.SCAN && a < Arrival.SCANNED ? 1.0F : 0.0F;
            ring(event, buffers, at, face, a * (0.12 + 0.25 * scanning), size);
            // It glows, pulsing, brightest far off so he can spot it, and sends out rings of light while it waits.
            float pulse = 0.5F + 0.5F * Mth.sin(a * (0.8F + 0.6F * scanning));
            double far = Mth.clamp(at.distanceTo(eye) / 20.0, 0.15, 1.0);
            painter.flare(at, (0.35 + 0.9 * far) * (0.8 + 0.4 * pulse), 0.75 + 0.25 * pulse);
            // Flaring up where it comes to a stop out of the sky, a ring of light bursting out of it.
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
            // Stopping dead before his eyes: a flash, and a ring of light racing out of it.
            float stop = a - ARRIVES;
            if (stop >= 0.0F && stop < FLASH_TICKS) {
                float u = stop / FLASH_TICKS;
                painter.flare(at, 1.4 * (1.0F - u), 1.0F - u);
                burst(painter, at, face, u, 1.8);
            }
            // A long streak of light behind it while it flies, down out of the sky and in to him; a short one on its
            // way onto his finger.
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
        // The ring is on: a flash at the hand, a ring of hard light racing out over the ground and the ring's light
        // flaring up round him; once the mask is on, his eyes light up.
        float since = a - Arrival.RING_ON;
        Vec3 finger = finger(event, player, partialTick);
        if (since < 6.0F) {
            float burst = 1.0F - since / 6.0F;
            painter.flare(finger, (0.5 + 2.0 * burst) * (own ? 0.5 : 1.0), burst);
        }
        Vec3 feet = player.getPosition(partialTick);
        wave(painter, feet, since);
        // The pillar would run up through your own eyes: in first person it is left out.
        if (!own) {
            pillar(painter, feet, since);
        }
        aura(painter, feet, player.getBbHeight(), since);
        float lit = a - Arrival.DRESSED;
        if (!own && lit >= 0.0F && lit < EYES_TICKS) {
            eyes(painter, player, lit < 3.0F ? lit / 3.0F : 1.0F - (lit - 3.0F) / (EYES_TICKS - 3.0F), partialTick);
        }
    }

    /**
     * Where the ring is on its way: streaking down out of the sky to where it shows up, circling him once from there
     * down to before his eyes, and from there onto his finger.
     */
    private static Vec3 ringAt(Vec3 from, Vec3 eye, Vec3 hover, Vec3 finger, float a) {
        if (a < STREAK_TICKS) {
            // Out of the sky over the far side of where it shows up, slowing down to a stop.
            Vec3 out = new Vec3(from.x - eye.x, 0.0, from.z - eye.z);
            Vec3 sky = (out.lengthSqr() < 1.0E-6 ? Vectors.UP : out.normalize().scale(0.5)
                    .add(Vectors.UP)).normalize();
            double left = 1.0 - a / STREAK_TICKS;
            return from.add(sky.scale(STREAK_HIGH * left * left));
        }
        if (a < SET_OFF) {
            return from.add(0.0, 0.08 * Mth.sin(a * 0.5F), 0.0);
        }
        if (a < ARRIVES) {
            double t = Ease.smooth((a - SET_OFF) / (ARRIVES - SET_OFF));
            // Round him once on the way, closing in fast and then circling closer, coming down as it goes.
            Vec3 start = from.subtract(eye);
            Vec3 end = hover.subtract(eye);
            double startAngle = Math.atan2(start.z, start.x);
            double turn = Mth.wrapDegrees(Math.toDegrees(Math.atan2(end.z, end.x) - startAngle)) * Mth.DEG_TO_RAD;
            turn += turn >= 0.0 ? Math.PI * 2.0 : -Math.PI * 2.0;
            double angle = startAngle + turn * t;
            double startReach = Math.sqrt(start.x * start.x + start.z * start.z);
            double endReach = Math.sqrt(end.x * end.x + end.z * end.z);
            double reach = Mth.lerp(1.0 - Math.pow(1.0 - t, 2.5), startReach, endReach);
            double high = Mth.lerp(t, start.y, end.y);
            return eye.add(Math.cos(angle) * reach, high, Math.sin(angle) * reach);
        }
        if (a < Arrival.RING_FLY) {
            return hover;
        }
        double t = Mth.clamp((a - Arrival.RING_FLY) / (Arrival.RING_ON - Arrival.RING_FLY), 0.0, 1.0);
        t = t * t;
        return hover.lerp(finger, t).add(0.0, 0.25 * Math.sin(Math.PI * t), 0.0);
    }

    /** How big the ring is at this moment, across in blocks: big far off, a ring's size once it reaches the hand. */
    private static double ringSize(float a, boolean own) {
        if (a < ARRIVES) {
            return Mth.lerp(Ease.smooth((a - SET_OFF) / (ARRIVES - SET_OFF)), FAR_SIZE, HOVER_SIZE);
        }
        double t = Mth.clamp((a - Arrival.RING_FLY) / (Arrival.RING_ON - Arrival.RING_FLY), 0.0, 1.0);
        return Mth.lerp(t * t, HOVER_SIZE, own ? OWN_FINGER_SIZE : FINGER_SIZE);
    }

    /**
     * Where the ring sits on this player's finger: measured where it was really drawn (see {@link RingSpot}), so it
     * lands right on the hand; worked out from the body when it was not drawn lately.
     */
    private static Vec3 finger(RenderLevelStageEvent event, AbstractClientPlayer player, float partialTick) {
        Vec3 seen = RingSpot.of(player, event.getCamera(), event.getProjectionMatrix(), event.getModelViewMatrix());
        return seen != null ? seen : LanternArms.ringPoint(player, partialTick);
    }

    /** The lantern taking shape out of the ring's light and flying to his left hand. */
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
        // Fed by a beam out of the ring while it takes shape, with a flash as it is done.
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
        // Its front, where the light blasts out, looks the way he looks.
        Vec3 ahead = ahead(player, partialTick);
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(-ahead.x, -ahead.z)));
        pose.mulPose(Axis.YP.rotation(0.6F * (1.0F - fly) * Mth.sin(a * 0.25F)));
        float scale = LANTERN_SCALE * Mth.clamp(grown, 0.0F, 1.2F) * (own ? Mth.lerp(fly, 1.0F, 0.7F) : 1.0F);
        pose.scale(scale, scale, scale);
        PowerBattery.draw(pose, buffers, 0.5F + 0.6F * (1.0F - form), 0.0F,
                1.0F - (float) Ease.smooth((form - 0.1F) / 0.8F));
        pose.popPose();
    }

    /** Where his left hand is, reaching out to catch the lantern: before your own eyes, or at the end of his arm. */
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

    /**
     * The ring's light racing out over the ground from where he stands, as far as the creatures of the dark run from
     * it: a low wall of hard light and a line of light at its foot, sinking away as it goes.
     */
    private static void wave(LanternPainter painter, Vec3 feet, float since) {
        if (since >= WAVE_FADE) {
            return;
        }
        double out = 1.0 - Math.pow(1.0 - Mth.clamp(since / WAVE_TICKS, 0.0F, 1.0F), 3.0);
        double radius = Math.max(0.4, Arrival.FEAR_RADIUS * out);
        double height = 0.7 * Math.pow(1.0 - since / WAVE_FADE, 1.3);
        int segments = 72;
        double half = Math.PI * radius / segments;
        Vec3 foot = feet.add(0.0, 0.03, 0.0);
        double[][] block = { { -half, 0.0, -0.1, half, height, 0.1, 1.2 } };
        for (int i = 0; i < segments; i++) {
            double angle = Math.PI * 2.0 * (i + 0.5) / segments;
            Vec3 way = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            painter.model(block, new ConstructPainter.Frame(foot.add(way.scale(radius)), way.cross(Vectors.UP),
                    Vectors.UP, way, 1.0), 1.0, 1.1);
        }
        double fade = Math.pow(1.0 - since / WAVE_FADE, 1.5);
        painter.circle(foot.add(0.0, 0.03, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), radius,
                0.14 + 0.1 * fade, 1.0, Colors.alpha(fade), Colors.alpha(0.6 * fade));
    }

    /**
     * A ring of light racing out of {@code at}, square to {@code face}, {@code reach} blocks out at the end: {@code u}
     * from 0 (just out) to 1 (gone).
     */
    private static void burst(LanternPainter painter, Vec3 at, Vec3 face, float u, double reach) {
        double out = 1.0 - (1.0 - u) * (1.0 - u);
        double fade = 1.0 - u;
        double thick = 1.0 + 0.2 * reach;
        Vec3[] across = acrossOf(face);
        painter.circle(at, across[0], across[1], 0.15 + reach * out, 0.05 * thick, 0.3 * thick,
                Colors.alpha(fade), Colors.alpha(0.5 * fade));
    }

    /**
     * The ring's light shooting up out of him into the sky as it slides on: a pillar of light that is up in a moment,
     * then thins and dies down, with a wider sheath of light round its foot.
     */
    private static void pillar(LanternPainter painter, Vec3 feet, float since) {
        if (since < 0.0F || since >= PILLAR_TICKS) {
            return;
        }
        double life = 1.0 - since / PILLAR_TICKS;
        double top = Arrival.PILLAR_HIGH * Ease.smooth(since / PILLAR_RISE);
        Vec3 base = feet.add(0.0, 0.05, 0.0);
        painter.edge(base, base.add(0.0, top, 0.0), 0.45 * Math.sqrt(life), life);
        painter.edge(base, base.add(0.0, top * 0.35, 0.0), 1.1 * life, 0.4 * life);
    }

    /**
     * The ring scanning him: a band of light round him sweeping down from over his head to his feet and back up, two
     * fainter ones trailing it, fed by a fan of light out of the ring.
     */
    private static void scan(LanternPainter painter, AbstractClientPlayer player, Vec3 ring, float a,
            float partialTick) {
        float u = (a - Arrival.SCAN) / (Arrival.SCANNED - Arrival.SCAN);
        if (u < 0.0F || u > 1.0F) {
            return;
        }
        Vec3 feet = player.getPosition(partialTick);
        double fade = Math.min(1.0, Math.min(u, 1.0F - u) * 10.0);
        Vec3 band = feet;
        for (int k = 2; k >= 0; k--) {
            float behind = Math.max(0.0F, u - 0.04F * k);
            double down = behind < 0.5F ? Ease.smooth(behind * 2.0) : 1.0 - Ease.smooth(behind * 2.0 - 1.0);
            band = feet.add(0.0, Mth.lerp(down, player.getBbHeight() + SCAN_TOP, 0.05), 0.0);
            double strength = fade * (k == 0 ? 1.0 : 0.45 / k);
            painter.circle(band, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), SCAN_RADIUS, 0.03, 0.35,
                    Colors.alpha(0.95 * strength), Colors.alpha(0.45 * strength));
        }
        for (int k = 0; k < 8; k++) {
            double angle = k * Math.PI / 4.0 + a * 0.05;
            Vec3 edge = band.add(Math.cos(angle) * SCAN_RADIUS, 0.0, Math.sin(angle) * SCAN_RADIUS);
            painter.edge(ring, edge, 0.012, 0.35 * fade);
        }
    }

    /**
     * The ring's light flaring up round him as it slides on: tongues of light licking up round him from his feet and
     * a glowing ring of light at them, dying down.
     */
    private static void aura(LanternPainter painter, Vec3 feet, double height, float since) {
        if (since < 0.0F || since >= AURA_TICKS) {
            return;
        }
        double life = 1.0 - since / AURA_TICKS;
        double grow = Math.min(1.0, since / 4.0);
        for (int k = 0; k < AURA_TONGUES; k++) {
            double angle = Math.PI * 2.0 * k / AURA_TONGUES + since * 0.04;
            double phase = (since * 0.09 + Noise.of(k, 171, 0)) % 1.0;
            Vec3 way = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            double out = 0.42 + 0.12 * Math.sin(since * 0.3 + k);
            Vec3 base = feet.add(way.scale(out)).add(0.0, height * (0.05 + 1.0 * phase) * grow, 0.0);
            Vec3 tip = base.add(way.scale(-0.06)).add(0.0,
                    height * (0.2 + 0.2 * Noise.of(k, 171, 1)) * grow, 0.0);
            painter.edge(base, tip, 0.05 * life, life * (1.0 - phase));
        }
        painter.circle(feet.add(0.0, 0.05, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), 0.6, 0.06, 0.6,
                Colors.alpha(0.8 * life), Colors.alpha(0.4 * life));
    }

    /** His eyes glowing behind the mask, {@code glow} from 0 (out) to 1 (as bright as they get). */
    private static void eyes(LanternPainter painter, AbstractClientPlayer player, double glow, float partialTick) {
        if (glow <= 0.0) {
            return;
        }
        Vec3 look = player.getViewVector(partialTick);
        Vec3 side = look.cross(Vectors.UP);
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        Vec3 face = player.getEyePosition(partialTick).add(look.scale(0.27));
        for (int k = -1; k <= 1; k += 2) {
            painter.flare(face.add(side.scale(0.1 * k)), 0.1 + 0.3 * glow, glow);
        }
    }

    /**
     * One player taking the uniform off: the glow of his eyes goes out and specks of the uniform's light stream off
     * him into the ring while it draws back; then the ring leaves him, up over his head, where it hangs a moment,
     * turning, and sends out a last ring of light, and off up into the sky in a spiral, until it is a twinkle high up.
     */
    private static void departing(RenderLevelStageEvent event, LanternPainter painter,
            MultiBufferSource.BufferSource buffers, AbstractClientPlayer player, float d, float partialTick) {
        boolean own = player == Minecraft.getInstance().player && !event.getCamera().isDetached();
        Vec3 finger = finger(event, player, partialTick);
        if (d < ClientLooks.UNDRESS_TICKS) {
            if (!own && d < EYES_OUT) {
                eyes(painter, player, 1.0 - d / EYES_OUT, partialTick);
            }
            specks(painter, player, finger, d, partialTick);
            return;
        }
        float e = (d - ClientLooks.UNDRESS_TICKS) / (ClientLooks.DEPART_TICKS - ClientLooks.UNDRESS_TICKS);
        Vec3 above = player.getEyePosition(partialTick).add(0.0, 1.0, 0.0);
        Vec3 ahead = ahead(player, partialTick);
        Vec3 at = departAt(finger, above, ahead, e);
        double size = Mth.lerp(Ease.smooth(e / RISEN), own ? OWN_FINGER_SIZE : FINGER_SIZE, HOVER_SIZE);
        if (e >= RISEN && e < LAUNCH) {
            // Hanging over his head a moment: a last ring of light out of it.
            float wave = (e - RISEN) / (LAUNCH - RISEN);
            painter.circle(at, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), 0.2 + 2.6 * wave, 0.05, 0.3,
                    Colors.alpha(0.9 * (1.0 - wave)), Colors.alpha(0.45 * (1.0 - wave)));
        }
        if (e >= LAUNCH) {
            // A long streak of light behind it on its way up.
            Vec3 last = at;
            for (int k = 1; k <= TRAIL; k++) {
                Vec3 next = departAt(finger, above, ahead, Math.max(LAUNCH, e - 0.012F * k));
                double fade = 1.0 - (double) k / (TRAIL + 1);
                painter.edge(last, next, 0.3 * fade, 0.95 * fade);
                last = next;
            }
        }
        Vec3 toCamera = event.getCamera().getPosition().subtract(at);
        Vec3 face = toCamera.lengthSqr() < 1.0E-6 ? ahead : toCamera.normalize();
        if (e < 0.96F) {
            ring(event, buffers, at, face, d * (e < LAUNCH ? 0.2 : 0.5), size);
            painter.flare(at, 0.35 + 0.25 * Mth.sin(d * 0.8F) + 1.2 * Math.max(0.0, e - LAUNCH), 0.9);
        } else {
            // Gone in a twinkle high up.
            double twinkle = (1.0F - e) / 0.04F;
            painter.flare(at, 5.0 * twinkle, twinkle);
            painter.edge(at.add(-3.0 * twinkle, 0.0, 0.0), at.add(3.0 * twinkle, 0.0, 0.0), 0.15, twinkle);
            painter.edge(at.add(0.0, -3.0 * twinkle, 0.0), at.add(0.0, 3.0 * twinkle, 0.0), 0.15, twinkle);
        }
    }

    /** Where the ring is as it leaves: {@code e} from 0 (on his finger) to 1 (gone high up in the sky). */
    private static Vec3 departAt(Vec3 finger, Vec3 above, Vec3 ahead, float e) {
        if (e < RISEN) {
            return finger.lerp(above, Ease.smooth(e / RISEN));
        }
        if (e < LAUNCH) {
            return above.add(0.0, 0.08 * Math.sin((e - RISEN) * 40.0), 0.0);
        }
        double t = (e - LAUNCH) / (1.0 - LAUNCH);
        double climb = t * t;
        Vec3 side = new Vec3(-ahead.z, 0.0, ahead.x);
        double swirl = 2.5 * Math.sqrt(t);
        double turn = t * Math.PI * 4.0;
        return above.add(0.0, AWAY_HIGH * climb, 0.0).add(ahead.scale(AWAY_AHEAD * climb))
                .add(side.scale(swirl * Math.cos(turn))).add(ahead.scale(swirl * Math.sin(turn)));
    }

    /** Specks of the uniform's light streaming off him into the ring while it draws back, most of all halfway. */
    private static void specks(LanternPainter painter, AbstractClientPlayer player, Vec3 finger, float d,
            float partialTick) {
        double strength = Math.sin(Math.PI * d / ClientLooks.UNDRESS_TICKS);
        if (strength <= 0.0) {
            return;
        }
        Vec3 feet = player.getPosition(partialTick);
        double height = player.getBbHeight();
        for (int k = 0; k < SPECKS; k++) {
            double cycle = d / 12.0 + Noise.of(k, 181, 0);
            int round = (int) Math.floor(cycle);
            double phase = cycle - round;
            double angle = Noise.of(k, 181 + round, 1) * Math.PI * 2.0;
            Vec3 start = feet.add(0.32 * Math.cos(angle), height * (0.1 + 0.8 * Noise.of(k,
                    181 + round, 2)), 0.32 * Math.sin(angle));
            Vec3 head = start.lerp(finger, phase * phase);
            Vec3 tail = start.lerp(finger, Math.max(0.0, phase - 0.15) * Math.max(0.0, phase - 0.15));
            painter.edge(tail, head, 0.055, Math.min(1.0, strength * (0.6 + 0.6 * phase)));
        }
    }

    /**
     * Draws the ring out in the world at {@code at}, {@code size} blocks across, its face to {@code face}, turned
     * {@code spin} about the way up: a silver band, the dark setting and the green stone, which glows by itself.
     */
    private static void ring(RenderLevelStageEvent event, MultiBufferSource.BufferSource buffers, Vec3 at, Vec3 face,
            double spin, double size) {
        Vec3 axis = Vectors.spin(face, Vectors.UP, spin);
        Vec3[] across = acrossOf(axis);
        Matrix4f matrix = event.getPoseStack().last().pose();
        Vec3 camera = event.getCamera().getPosition();
        VertexConsumer buffer = buffers.getBuffer(Ring.BAND);
        double scale = size * 0.5;
        // The band lies round its own y: here that is the way it faces.
        mesh(buffer, matrix, camera, BAND, at, across[0], axis, across[1], scale, SILVER, true);
        mesh(buffer, matrix, camera, SETTING, at, across[0], axis, across[1], scale, SETTING_COLOR, true);
        mesh(buffer, matrix, camera, STONE, at, across[0], axis, across[1], scale, STONE_COLOR, false);
    }

    /** One round part of the ring, out in the world. */
    private static void mesh(VertexConsumer buffer, Matrix4f matrix, Vec3 camera, Mesh mesh, Vec3 at, Vec3 x, Vec3 y,
            Vec3 z, double scale, int rgb, boolean lit) {
        for (int s = 0; s < mesh.sides.length; s++) {
            Vec3 n = mesh.normals[s];
            Vec3 normal = x.scale(n.x).add(y.scale(n.y)).add(z.scale(n.z));
            double light = lit && normal.lengthSqr() > 1.0E-6 ? Math.min(1.0, 1.15 * ConstructPainter.light(
                    normal.normalize())) : 1.0;
            int color = Colors.shade(rgb, light);
            for (int k : mesh.sides[s]) {
                Vec3 p = mesh.points[k];
                Vec3 world = at.add(x.scale(p.x * scale)).add(y.scale(p.y * scale)).add(z.scale(p.z * scale));
                buffer.addVertex(matrix, (float) (world.x - camera.x), (float) (world.y - camera.y),
                        (float) (world.z - camera.z))
                        .setColor(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255);
            }
        }
    }

    /** Two ways square to {@code axis} and to each other. */
    private static Vec3[] acrossOf(Vec3 axis) {
        Vec3 side = Math.abs(axis.y) < 0.95 ? axis.cross(Vectors.UP) : axis.cross(new Vec3(1.0, 0.0, 0.0));
        side = side.normalize();
        return new Vec3[] { side, side.cross(axis).normalize() };
    }

    /** The sounds of the ring leaving, each as its moment comes: see {@link #departing}. */
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

    /** True when {@code moment} came between last tick ({@code before}) and this one ({@code now}). */
    private static boolean passed(float before, float now, float moment) {
        return before < moment && now >= moment;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DEPARTING.clear();
    }

    /** The moment the ring slides onto your own finger, your view jolts. */
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

    // ---- Seen from outside: the arms ----

    /**
     * His arms while the ring comes to him, seen from outside (the lantern's own part takes over once he smacks it, see
     * {@link RechargeAnimation}): the left reaches out for the lantern as it flies in and holds it up in front of his
     * chest, the right comes up, fist forward, for the ring and goes down again once the uniform has run up it.
     */
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

    // ---- First person ----

    /**
     * Your own arms while the ring comes to you: your left hand reaches out and catches the lantern and holds it up,
     * and your ring fist comes up to take the ring, while the uniform runs up it. Before the lantern comes, and once
     * the smack begins, the game (or the recharge) draws your hands as always.
     */
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
        // Out to catch the lantern, and up with it once it is in the hand.
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
        // The ring fist up to take the ring, and down again.
        Vector3f fist = new Vector3f(RechargeAnimation.HAND_RIGHT).lerp(FIST_UP, fistUp(a));
        RechargeAnimation.arm(pose, buffers, light, player, renderer, 1.0F, fist, RIGHT_FROM);
    }
}
