package nl.tivek.welcomescreen.client.character.lantern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.lantern.Arrival;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The ring's arrival as everyone sees it (the server keeps the time, see {@link Arrival}), and its departure.
 * <ul>
 * <li>The ring shows up far off, a spark of green that pulses and sends out rings of light, then flies in, swooping,
 * a streak of light behind it, and hangs three blocks before his eyes, turning slowly.</li>
 * <li>A beam out of it shapes the lantern below it: white-hot at first, cooling to silver. The lantern flies to his
 * left hand, which reaches out and catches it and holds it up.</li>
 * <li>His right fist comes up and the ring shoots onto its middle finger: a flash, and a ring of hard light races out
 * over the ground as far as the creatures of the dark are sent running. The uniform spreads from the ring (see
 * {@link ClientLooks}), and in the end he smacks the ring into the lantern (see {@link RechargeAnimation}).</li>
 * <li>Taking the uniform off, it draws back into the ring (see {@link ClientLooks}); then the ring slides off his
 * finger, rises over his head, and shoots off into the sky, gone in a flash.</li>
 * </ul>
 * In first person your own arms are drawn here while the lantern comes to your hand and the ring to your finger.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class ArrivalAnimation {
    // The ring out in the world: a silver band round its middle, its setting on the outside and the stone in it, a
    // band one long across; drawn bigger than on a finger, so it can be seen from far off.
    private static final Mesh BAND = Mesh.torus(20, 8, 1.0, 0.24, 1.0);
    private static final Mesh SETTING = Mesh.box(1.12, -0.3, -0.3, 1.42, 0.3, 0.3, 1.0);
    private static final Mesh STONE = Mesh.ball(10, 6, 0.26, 1.0).moved(1.5, 0.0, 0.0);
    private static final int SILVER = 0xD2DBD6;
    private static final int SETTING_COLOR = 0x4B5652;
    private static final int STONE_COLOR = 0x2EF566;
    // The ring's flight in: it waits where it showed up, pulsing, sets off, and arrives before his eyes.
    private static final float SET_OFF = 12.0F;
    private static final float ARRIVES = 58.0F;
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
    private static final float WAVE_TICKS = 14.0F;
    private static final float WAVE_FADE = 22.0F;
    // First person, in blocks before your eyes (x right, y up, -z ahead): where your left hand reaches out to catch
    // the lantern, and where your ring fist comes up to take the ring.
    private static final Vector3f CATCH = new Vector3f(-0.34F, 0.02F, -0.95F);
    private static final Vector3f FIST_UP = new Vector3f(0.3F, -0.1F, -0.72F);
    private static final Vector3f LEFT_FROM = new Vector3f(-1.52F, -0.34F, 0.33F);
    private static final Vector3f RIGHT_FROM = new Vector3f(1.5F, -0.4F, 0.35F);
    private static final Vector3f GRIP_DOWN = new Vector3f(-0.55F, -1.0F, -0.8F);
    private static final Vector3f GRIP_UP = new Vector3f(-0.22F, 0.2F, -0.8F);

    private ArrivalAnimation() {
    }

    // ---- The timeline ----

    /** True while this player holds the lantern up in his left hand, waiting for the smack that ends the arrival. */
    static boolean holdsLantern(Entity player, float partialTick) {
        float a = ClientRing.arrival(player, partialTick);
        return a >= Arrival.LANTERN_CAUGHT && ClientRing.recharge(player, partialTick) < 0.0F;
    }

    /** How brightly the lantern he holds burns meanwhile: it breathes, and flares as the ring slides on. */
    static float lanternGlow(Entity player, float partialTick) {
        float a = ClientRing.arrival(player, partialTick);
        float flare = a >= Arrival.RING_ON ? Math.max(0.0F, 1.0F - (a - Arrival.RING_ON) / 10.0F) : 0.0F;
        return 0.4F + 0.12F * Mth.sin(a * 0.3F) + 0.8F * flare;
    }

    /** How far he has got his left hand out to catch the lantern, 0 to 1, and it stays out holding it. */
    private static float reach(float a) {
        return ClientLooks.smooth((a - Arrival.LANTERN_FORMED + 8.0F) / 10.0F);
    }

    /** How far his ring fist is up to take the ring, 0 to 1: up as it comes, down again once the uniform runs up it. */
    private static float fistUp(float a) {
        return ClientLooks.smooth((a - Arrival.RING_FLY + 5.0F) / 6.0F)
                * (1.0F - ClientLooks.smooth((a - Arrival.RING_ON - 24.0F) / 12.0F));
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
        ConstructPainter painter = null;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        for (AbstractClientPlayer player : level.players()) {
            float a = ClientRing.arrival(player, partialTick);
            float d = ClientLooks.departure(player, partialTick);
            if ((a < 0.0F || a > Arrival.RING_ON + WAVE_FADE) && d < ClientLooks.UNDRESS_TICKS) {
                continue;
            }
            if (painter == null) {
                painter = new ConstructPainter(event.getPoseStack(), camera.getPosition(),
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
    private static void arriving(RenderLevelStageEvent event, ConstructPainter painter,
            MultiBufferSource.BufferSource buffers, AbstractClientPlayer player, float a, float partialTick) {
        Camera camera = event.getCamera();
        boolean own = player == Minecraft.getInstance().player && !camera.isDetached();
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 hover = hover(player, partialTick, a);
        if (a < Arrival.RING_ON) {
            Vec3 from = ClientRing.arrivalFrom(player);
            Vec3 finger = finger(event, player, partialTick);
            Vec3 at = ringAt(from == null ? hover : from, hover, finger, a);
            double size = ringSize(a, own);
            // The ring turns slowly about the way up, its face to him.
            Vec3 toEye = eye.subtract(at);
            Vec3 face = toEye.lengthSqr() < 1.0E-6 ? ahead(player, partialTick).scale(-1.0) : toEye.normalize();
            ring(event, buffers, at, face, a * 0.12, size);
            // It glows, pulsing, brightest far off so he can spot it, and sends out rings of light while it waits.
            float pulse = 0.5F + 0.5F * Mth.sin(a * 0.8F);
            double far = Mth.clamp(at.distanceTo(eye) / 20.0, 0.15, 1.0);
            painter.flare(at, (0.35 + 0.9 * far) * (0.8 + 0.4 * pulse), 0.75 + 0.25 * pulse);
            if (a < SET_OFF + 6.0F) {
                for (int k = 0; k < 2; k++) {
                    float age = (a + k * 6.0F) % 12.0F;
                    Vec3[] across = acrossOf(face);
                    painter.circle(at, across[0], across[1], (0.2 + 0.25 * age) * (0.5 + far),
                            0.04 * (0.5 + far), 0.2 * (0.5 + far), ConstructPainter.alpha(0.9 * (1.0 - age / 12.0)),
                            ConstructPainter.alpha(0.45 * (1.0 - age / 12.0)));
                }
            }
            // A streak of light behind it while it flies.
            if (a > SET_OFF && a < ARRIVES || a > Arrival.RING_FLY) {
                Vec3 last = at;
                for (int k = 1; k <= 7; k++) {
                    Vec3 next = ringAt(from == null ? hover : from, hover, finger, a - 0.7F * k);
                    painter.edge(last, next, size * (0.5 - 0.05 * k), 0.9 - 0.12 * k);
                    last = next;
                }
            }
            lantern(event, painter, buffers, player, at, a, partialTick, own);
            return;
        }
        // The ring is on: a flash at the hand and a ring of hard light racing out over the ground.
        float since = a - Arrival.RING_ON;
        Vec3 finger = finger(event, player, partialTick);
        if (since < 7.0F) {
            float burst = 1.0F - since / 7.0F;
            painter.flare(finger, (0.4 + 1.4 * burst) * (own ? 0.5 : 1.0), burst);
        }
        wave(painter, player.getPosition(partialTick), since);
    }

    /** Where the ring is on its way, from where it showed up to before his eyes and from there onto his finger. */
    private static Vec3 ringAt(Vec3 from, Vec3 hover, Vec3 finger, float a) {
        if (a < SET_OFF) {
            return from.add(0.0, 0.08 * Mth.sin(a * 0.5F), 0.0);
        }
        if (a < ARRIVES) {
            double t = ConstructPainter.smooth((a - SET_OFF) / (ARRIVES - SET_OFF));
            // Swooping in: it rises out of a straight line on the way and comes down to him at the end.
            double swoop = Math.min(3.0, 0.15 * from.distanceTo(hover));
            return from.lerp(hover, t).add(0.0, swoop * Math.sin(Math.PI * t), 0.0);
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
            return Mth.lerp(ConstructPainter.smooth((a - SET_OFF) / (ARRIVES - SET_OFF)), FAR_SIZE, HOVER_SIZE);
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
    private static void lantern(RenderLevelStageEvent event, ConstructPainter painter,
            MultiBufferSource.BufferSource buffers, AbstractClientPlayer player, Vec3 ring, float a, float partialTick,
            boolean own) {
        if (a < Arrival.LANTERN_FORM || a >= Arrival.LANTERN_CAUGHT) {
            return;
        }
        Vec3 home = lanternHome(player, partialTick, a);
        float form = (a - Arrival.LANTERN_FORM) / (Arrival.LANTERN_FORMED - Arrival.LANTERN_FORM);
        float grown = (float) SlamPainter.backOut(form);
        float fly = ClientLooks.smooth((a - Arrival.LANTERN_FORMED)
                / (Arrival.LANTERN_CAUGHT - Arrival.LANTERN_FORMED));
        Vec3 hand = leftHand(event.getCamera(), player, partialTick, own);
        Vec3 at = home.lerp(hand, fly).add(0.0, 0.5 * Mth.sin(Mth.PI * fly), 0.0);
        // Fed by a beam out of the ring while it takes shape.
        if (a < Arrival.LANTERN_FORMED + 3.0F) {
            painter.beam(ring, at.subtract(0.0, 0.35, 0.0), 1.0, 0.8);
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
                1.0F - ClientLooks.smooth((form - 0.1F) / 0.8F));
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
    private static void wave(ConstructPainter painter, Vec3 feet, float since) {
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
            painter.model(block, new ConstructPainter.Frame(foot.add(way.scale(radius)), way.cross(ConstructPainter.UP),
                    ConstructPainter.UP, way, 1.0), 1.0, 1.1);
        }
        double fade = Math.pow(1.0 - since / WAVE_FADE, 1.5);
        painter.circle(foot.add(0.0, 0.03, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), radius,
                0.14 + 0.1 * fade, 1.0, ConstructPainter.alpha(fade), ConstructPainter.alpha(0.6 * fade));
    }

    /** One player's ring leaving him once the uniform is back in it: up over his head, and off into the sky. */
    private static void departing(RenderLevelStageEvent event, ConstructPainter painter,
            MultiBufferSource.BufferSource buffers, AbstractClientPlayer player, float d, float partialTick) {
        float e = (d - ClientLooks.UNDRESS_TICKS) / (ClientLooks.DEPART_TICKS - ClientLooks.UNDRESS_TICKS);
        Vec3 finger = finger(event, player, partialTick);
        Vec3 above = player.getEyePosition(partialTick).add(0.0, 0.9, 0.0);
        Vec3 ahead = ahead(player, partialTick);
        Vec3 away = above.add(0.0, 40.0, 0.0).add(ahead.scale(24.0));
        Vec3 at;
        double size;
        if (e < 0.35F) {
            at = finger.lerp(above, ConstructPainter.smooth(e / 0.35));
            size = Mth.lerp(ConstructPainter.smooth(e / 0.35), FINGER_SIZE, HOVER_SIZE);
        } else {
            double t = (e - 0.35) / 0.65;
            at = above.lerp(away, t * t);
            size = HOVER_SIZE;
            Vec3 last = at;
            for (int k = 1; k <= 7; k++) {
                double q = Math.max(0.0, t - 0.03 * k);
                Vec3 next = above.lerp(away, q * q);
                painter.edge(last, next, 0.25 * (1.0 - 0.1 * k), 0.9 - 0.12 * k);
                last = next;
            }
        }
        Vec3 toCamera = event.getCamera().getPosition().subtract(at);
        Vec3 face = toCamera.lengthSqr() < 1.0E-6 ? ahead : toCamera.normalize();
        if (e < 0.97F) {
            ring(event, buffers, at, face, d * 0.2, size);
            painter.flare(at, 0.35 + 0.25 * Mth.sin(d * 0.8F), 0.9);
        } else {
            painter.flare(at, 2.5, (1.0F - e) / 0.03F);
        }
    }

    /**
     * Draws the ring out in the world at {@code at}, {@code size} blocks across, its face to {@code face}, turned
     * {@code spin} about the way up: a silver band, the dark setting and the green stone, which glows by itself.
     */
    private static void ring(RenderLevelStageEvent event, MultiBufferSource.BufferSource buffers, Vec3 at, Vec3 face,
            double spin, double size) {
        Vec3 axis = ConstructPainter.spin(face, ConstructPainter.UP, spin);
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
            int color = ConstructPainter.shade(rgb, light);
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
        Vec3 side = Math.abs(axis.y) < 0.95 ? axis.cross(ConstructPainter.UP) : axis.cross(new Vec3(1.0, 0.0, 0.0));
        side = side.normalize();
        return new Vec3[] { side, side.cross(axis).normalize() };
    }

    /** The moment the ring slides onto your own finger, your view jolts a little. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float since = ClientRing.arrival(player, (float) event.getPartialTick()) - Arrival.RING_ON;
        if (since < 0.0F || since > 8.0F) {
            return;
        }
        float shake = 0.7F * (1.0F - since / 8.0F);
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
    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float a = ClientRing.arrival(entity, partialTick);
        if (a < 0.0F) {
            return;
        }
        float look = Mth.clamp(model.head.xRot, -0.8F, 0.8F) * 0.5F;
        if (arm == HumanoidArm.LEFT) {
            float reach = reach(a);
            float hold = ClientLooks.smooth((a - Arrival.LANTERN_CAUGHT + 2.0F) / 5.0F);
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
        if (a < Arrival.LANTERN_FORMED - 8.0F || ClientRing.recharge(player, event.getPartialTick()) >= 0.0F) {
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
        float hold = ClientLooks.smooth((a - Arrival.LANTERN_CAUGHT + 2.0F) / 5.0F);
        Vector3f grip = new Vector3f(GRIP_DOWN).lerp(CATCH, reach(a)).lerp(GRIP_UP, hold);
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
