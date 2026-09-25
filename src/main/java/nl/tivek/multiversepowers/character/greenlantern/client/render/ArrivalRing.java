package nl.tivek.multiversepowers.character.greenlantern.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.character.greenlantern.client.body.Ring;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import org.joml.Matrix4f;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.ARRIVES;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.FAR_SIZE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.FINGER_SIZE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.HOVER_SIZE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.OWN_FINGER_SIZE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.SET_OFF;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.STREAK_HIGH;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.STREAK_TICKS;

/**
 * The ring itself on its way in (see {@link ArrivalAnimation}): where it is and how big, and drawing it out in the
 * world.
 */
final class ArrivalRing {
    // The ring out in the world: a silver band round its middle, its setting on the outside and the stone in it, a
    // band one long across; drawn bigger than on a finger, so it can be seen from far off.
    private static final Mesh BAND = Mesh.torus(20, 8, 1.0, 0.24, 1.0);
    private static final Mesh SETTING = Mesh.box(1.12, -0.3, -0.3, 1.42, 0.3, 0.3, 1.0);
    private static final Mesh STONE = Mesh.ball(10, 6, 0.26, 1.0).moved(1.5, 0.0, 0.0);
    private static final int SILVER = 0xD2DBD6;
    private static final int SETTING_COLOR = 0x4B5652;
    private static final int STONE_COLOR = 0x2EF566;

    private ArrivalRing() {
    }

    /**
     * Where the ring is on its way: streaking down out of the sky to where it shows up, circling him once from there
     * down to before his eyes, and from there onto his finger.
     */
    static Vec3 ringAt(Vec3 from, Vec3 eye, Vec3 hover, Vec3 finger, float a) {
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
    static double ringSize(float a, boolean own) {
        if (a < ARRIVES) {
            return Mth.lerp(Ease.smooth((a - SET_OFF) / (ARRIVES - SET_OFF)), FAR_SIZE, HOVER_SIZE);
        }
        double t = Mth.clamp((a - Arrival.RING_FLY) / (Arrival.RING_ON - Arrival.RING_FLY), 0.0, 1.0);
        return Mth.lerp(t * t, HOVER_SIZE, own ? OWN_FINGER_SIZE : FINGER_SIZE);
    }

    /**
     * Draws the ring out in the world at {@code at}, {@code size} blocks across, its face to {@code face}, turned
     * {@code spin} about the way up: a silver band, the dark setting and the green stone, which glows by itself.
     */
    static void ring(RenderLevelStageEvent event, MultiBufferSource.BufferSource buffers, Vec3 at, Vec3 face,
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
    static Vec3[] acrossOf(Vec3 axis) {
        Vec3 side = Math.abs(axis.y) < 0.95 ? axis.cross(Vectors.UP) : axis.cross(new Vec3(1.0, 0.0, 0.0));
        side = side.normalize();
        return new Vec3[] { side, side.cross(axis).normalize() };
    }
}
