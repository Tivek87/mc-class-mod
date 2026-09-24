package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightBubble;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * The Light Bubble as everyone sees it (see {@link LightBubble}): a ball-shaped cage of hard light round the creature
 * the ring caught, its struts running in triangles over it like the frame of a geodesic dome, with a knot of light
 * where they meet. It is as solid as every construct; you see the creature through the gaps between its struts. It
 * grows round its creature out of the ring's light, white-hot at first, turns slowly while the ring holds it up by a
 * beam of its light, and a band of light sweeps over it now and then. Pounded into the ground it streaks through the
 * air, stretched long as it is driven down, squashes flat against the ground at every slam and springs back, flaring
 * up; every slam sends a shockwave of light over the ground with cracks of light shooting out of it (see
 * {@link #pound}). Bursting or at the last slam it breaks into solid pieces, panel by panel.
 */
public final class BubblePainter {
    /** How long the shockwave of one slam of a pound plays, in ticks. */
    public static final int POUND_TICKS = 16;
    // How thick its struts are and how big the knots where they meet, next to its radius.
    private static final double STRUT = 0.055;
    private static final double KNOT = 0.085;
    // How far its pieces fly when it breaks up, next to how far pieces usually do.
    private static final double FLING = 1.8;
    // How flat a slam squashes it (a part of its height), how quickly it springs back and how often it wobbles on the
    // way; and how long it is stretched at most as it is driven down, and at which speed (blocks per tick) that is.
    private static final double SQUASH = 0.42;
    private static final double SQUASH_DAMP = 0.55;
    private static final double SQUASH_WOBBLE = 1.3;
    private static final double STRETCH = 0.28;
    private static final double STRETCH_SPEED = 2.2;
    /** The cage round a ball of radius 1: twenty panels of struts, each flying off on its own when it breaks. */
    private static final ConstructPainter.Shape CAGE = ConstructPainter.Shape.of(cage());

    private BubblePainter() {
    }

    /**
     * The cage: an icosahedron with every triangle cut into four, pushed out onto the ball, so its struts run in small
     * triangles all over it. Each of the twenty big triangles is a panel of its own.
     */
    private static Mesh[] cage() {
        double p = (1.0 + Math.sqrt(5.0)) * 0.5;
        List<Vec3> points = new ArrayList<>(List.of(new Vec3(-1, p, 0), new Vec3(1, p, 0), new Vec3(-1, -p, 0),
                new Vec3(1, -p, 0), new Vec3(0, -1, p), new Vec3(0, 1, p), new Vec3(0, -1, -p), new Vec3(0, 1, -p),
                new Vec3(p, 0, -1), new Vec3(p, 0, 1), new Vec3(-p, 0, -1), new Vec3(-p, 0, 1)));
        points.replaceAll(Vec3::normalize);
        int[][] faces = { { 0, 11, 5 }, { 0, 5, 1 }, { 0, 1, 7 }, { 0, 7, 10 }, { 0, 10, 11 }, { 1, 5, 9 },
                { 5, 11, 4 }, { 11, 10, 2 }, { 10, 7, 6 }, { 7, 1, 8 }, { 3, 9, 4 }, { 3, 4, 2 }, { 3, 2, 6 },
                { 3, 6, 8 }, { 3, 8, 9 }, { 4, 9, 5 }, { 2, 4, 11 }, { 6, 2, 10 }, { 8, 6, 7 }, { 9, 8, 1 } };
        Map<Long, Integer> middles = new HashMap<>();
        Set<Long> edges = new HashSet<>();
        Set<Integer> knots = new HashSet<>();
        Mesh[] panels = new Mesh[faces.length];
        for (int f = 0; f < faces.length; f++) {
            int a = faces[f][0];
            int b = faces[f][1];
            int c = faces[f][2];
            int ab = middle(points, middles, a, b);
            int bc = middle(points, middles, b, c);
            int ca = middle(points, middles, c, a);
            int[][] struts = { { a, ab }, { ab, b }, { b, bc }, { bc, c }, { c, ca }, { ca, a }, { ab, bc },
                    { bc, ca }, { ca, ab } };
            List<Mesh> parts = new ArrayList<>();
            for (int[] strut : struts) {
                long key = (long) Math.min(strut[0], strut[1]) << 32 | Math.max(strut[0], strut[1]);
                if (edges.add(key)) {
                    parts.add(Mesh.tube(false, 6, STRUT, 1.15, points.get(strut[0]), points.get(strut[1])));
                }
            }
            for (int knot : new int[] { a, b, c, ab, bc, ca }) {
                if (knots.add(knot)) {
                    Vec3 at = points.get(knot);
                    parts.add(Mesh.ball(8, 5, KNOT, 1.45).moved(at.x, at.y, at.z));
                }
            }
            panels[f] = Mesh.merged(parts.toArray(Mesh[]::new));
        }
        return panels;
    }

    /** The point halfway along an edge, pushed out onto the ball: made once for the two triangles that share it. */
    private static int middle(List<Vec3> points, Map<Long, Integer> middles, int a, int b) {
        long key = (long) Math.min(a, b) << 32 | Math.max(a, b);
        Integer known = middles.get(key);
        if (known != null) {
            return known;
        }
        points.add(points.get(a).add(points.get(b)).normalize());
        middles.put(key, points.size() - 1);
        return points.size() - 1;
    }

    /**
     * One bubble.
     *
     * @param solid  how far it has grown round its creature, 0 to 1
     * @param broken how many ticks ago it began to break up (only while it breaks up)
     * @param held   true while the ring still holds it up by a beam of its light
     * @param clock  ticks since it caught its creature, by the client's own clock
     * @param ring   where the ring of its maker is, or null when he is out of sight
     * @param pound  ticks into the pound, as drawn (only while it is pounded)
     * @param motion how far it moved over the last tick, in blocks
     */
    public static void draw(LanternPainter painter, ConstructPayload bubble, Vec3 center, double solid, double broken,
            boolean held, double clock, @Nullable Vec3 ring, double pound, Vec3 motion) {
        double radius = bubble.size();
        Vec3 flat = new Vec3(bubble.facing().x, 0.0, bubble.facing().z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        // Pounded: squashed flat by the last slam and springing back, or stretched long as it is driven down.
        double squash = 0.0;
        double flash = 0.0;
        if (bubble.variant() == LightBubble.SMASHING) {
            for (int k = LightBubble.POUND.length - 1; k >= 0; k--) {
                if (pound >= LightBubble.POUND[k]) {
                    double since = pound - LightBubble.POUND[k];
                    squash = SQUASH * Math.exp(-since * SQUASH_DAMP) * Math.cos(since * SQUASH_WOBBLE);
                    flash = Math.max(0.0, 1.0 - since / 3.0);
                    break;
                }
            }
            double down = Math.max(0.0, -motion.y);
            squash -= STRETCH * Math.min(1.0, down / STRETCH_SPEED);
            center = center.subtract(0.0, radius * 0.5 * Math.max(0.0, squash), 0.0);
        }
        ConstructPainter.Frame frame = ConstructPainter.Frame.of(center, flat, Vectors.UP, radius)
                .turned(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, clock * 0.012);
        if (squash != 0.0) {
            double wide = 1.0 + squash * 0.6;
            frame = frame.stretched(wide, 1.0 - squash, wide);
        }
        if (bubble.variant() == LightBubble.BREAKING) {
            double apart = Mth.clamp(broken / LightBubble.BREAK_TICKS, 0.0, 1.0);
            painter.glare(0.6 * (1.0 - apart));
            painter.fling(FLING);
            painter.shattered(CAGE, frame, apart, 1.3);
            painter.fling(1.0);
            painter.glare(0.0);
            if (apart < 0.5) {
                painter.flare(center, radius * 1.6 * (1.0 - 2.0 * apart), 1.0 - 2.0 * apart);
            }
            return;
        }
        double grown = Ease.backOut(solid);
        ConstructPainter.Frame shape = new ConstructPainter.Frame(frame.center(), frame.right(), frame.up(),
                frame.forward(), radius * (0.15 + 0.85 * grown));
        // White-hot as it grows out of the ring's light, and breathing a little in its glow after; flaring up at a slam.
        painter.glare(Math.max(0.7 * (1.0 - Mth.clamp(solid, 0.0, 1.0)), 0.65 * flash));
        painter.shape(CAGE, shape, 1.0, 1.0 + 0.12 * Math.sin(painter.time() * 0.2) + 0.6 * flash);
        painter.glare(0.0);
        // A soft light in it, and now and then a band of light sweeping over it from its top to its bottom.
        painter.flare(center, radius * 0.85, 0.16 * grown);
        double sweep = Mth.frac(painter.time() / 44.0) * 3.0 - 1.0;
        if (Math.abs(sweep) < 1.0) {
            double across = Math.sqrt(1.0 - sweep * sweep) * radius * grown;
            painter.circle(center.add(0.0, sweep * radius * grown, 0.0), new Vec3(1.0, 0.0, 0.0),
                    new Vec3(0.0, 0.0, 1.0), across, 0.04, 0.3, Colors.alpha(0.6 * (1.0 - Math.abs(sweep))),
                    Colors.alpha(0.3 * (1.0 - Math.abs(sweep))));
        }
        double speed = motion.length();
        if (bubble.variant() == LightBubble.SMASHING && speed > 0.15) {
            // Streaking through the air: lines of light trailing behind it from all round its edge.
            Vec3 way = motion.scale(1.0 / speed);
            Vec3[] across = Vectors.across(way);
            double length = Math.min(radius * 3.0, speed * 2.2);
            double strength = Math.min(1.0, speed / 1.2);
            for (int k = 0; k < 10; k++) {
                double angle = Math.PI * 2.0 * k / 10.0;
                Vec3 side = center.add(across[0].scale(Math.cos(angle) * radius * 0.85))
                        .add(across[1].scale(Math.sin(angle) * radius * 0.85));
                painter.edge(side, side.subtract(way.scale(length * (0.7 + 0.3 * Math.sin(k * 2.3)))),
                        0.06 * radius, 0.85 * strength);
            }
        }
        // The ring holds it up by a beam of its light, from the ring to where the bubble is nearest to it.
        if (ring != null && held) {
            Vec3 toRing = ring.subtract(center);
            if (toRing.lengthSqr() > radius * radius) {
                painter.beam(ring, center.add(toRing.normalize().scale(radius * grown)), Mth.clamp(solid * 2.0, 0.0,
                        1.0), Math.min(1.4, radius));
            }
        }
    }

    /** True for the last slam of a pound: the hardest. */
    public static boolean last(ConstructPayload pound) {
        return pound.variant() >= LightBubble.POUND.length - 1;
    }

    /**
     * One slam of a pound (see {@link LightBubble}) where it struck the ground: a flash, rings of light running out over
     * the ground as far as its shockwave reaches, and jagged cracks of light shooting out of it in a star and fading;
     * the last slam's are bigger and brighter, with a burst of light up out of the ground. Light, not a construct.
     *
     * @param since ticks since it struck
     */
    public static void pound(LanternPainter painter, ConstructPayload pound, double since) {
        if (since < 0.0 || since > POUND_TICKS) {
            return;
        }
        boolean last = last(pound);
        double reach = pound.size();
        Vec3 at = pound.center().add(0.0, 0.06, 0.0);
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        for (int k = 0; k < (last ? 3 : 2); k++) {
            double ring = since - k * 2.5;
            if (ring < 0.0) {
                continue;
            }
            double wave = 1.0 - Math.pow(1.0 - Math.min(1.0, ring / 9.0), 2.0);
            double fade = Math.max(0.0, 1.0 - ring / 12.0);
            painter.circle(at, east, south, 0.5 + reach * wave, 0.08, 0.6, Colors.alpha(fade),
                    Colors.alpha(0.5 * fade));
        }
        int cracks = last ? 12 : 8;
        double grow = Math.min(1.0, since / 2.5);
        double fade = 1.0 - since / POUND_TICKS;
        for (int i = 0; i < cracks; i++) {
            double angle = Math.PI * 2.0 * (i + 0.4 * Noise.of(pound.id(), i, 1)) / cracks;
            double length = reach * (0.5 + 0.4 * Noise.of(pound.id(), i, 2)) * grow;
            Vec3 from = at;
            for (int s = 1; s <= 3; s++) {
                double bend = angle + (Noise.of(pound.id(), i, 3 + s) - 0.5) * 0.5;
                Vec3 to = at.add(Math.cos(bend) * length * s / 3.0, 0.0, Math.sin(bend) * length * s / 3.0);
                painter.edge(from, to, 0.07 * (1.1 - 0.25 * s), fade * (last ? 1.0 : 0.8));
                from = to;
            }
        }
        if (since < 4.0) {
            double flash = 1.0 - since / 4.0;
            painter.flare(at.add(0.0, 0.5, 0.0), (last ? 2.6 : 1.6) * flash, flash);
        }
        if (last && since < 8.0) {
            double burst = 1.0 - since / 8.0;
            double high = 1.0 + since * 0.6;
            double wide = 0.3 + 0.9 * burst;
            painter.haze(at.add(0.0, high * 0.5, 0.0), new Vec3(wide, 0.0, 0.0), new Vec3(0.0, high, 0.0),
                    new Vec3(0.0, 0.0, wide), LanternPainter.BRIGHT, 0.5 * burst);
        }
    }
}
