package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.List;
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

public final class BubblePainter {
    public static final int POUND_TICKS = 16;
    private static final int BARS = 14;
    private static final double BAR = 0.04;
    private static final double BAR_RING = 0.9;
    private static final double HOOP = 0.035;
    private static final double FLING = 1.8;
    private static final double SQUASH = 0.42;
    private static final double SQUASH_DAMP = 0.55;
    private static final double SQUASH_WOBBLE = 1.3;
    private static final double STRETCH = 0.28;
    private static final double STRETCH_SPEED = 2.2;
    private static final ConstructPainter.Shape CAGE = ConstructPainter.Shape.of(cage());

    private BubblePainter() {
    }

    // A prison cage in the frame's unit size: floor plate, domed roof with a ring to hold it by, upright bars and
    // two hoops. Every bar is its own piece, so it breaks into solid bars.
    private static Mesh[] cage() {
        List<Mesh> pieces = new ArrayList<>();
        pieces.add(Mesh.lathe(24, 1.0, 0.0, -1.0, BAR_RING + 0.06, -1.0, BAR_RING + 0.1, -0.94,
                BAR_RING + 0.06, -0.87, 0.0, -0.87));
        pieces.add(Mesh.lathe(24, 1.0, 0.0, 0.87, BAR_RING + 0.06, 0.87, BAR_RING + 0.1, 0.93,
                BAR_RING - 0.05, 1.02, 0.6, 1.1, 0.3, 1.15, 0.0, 1.17));
        pieces.add(Mesh.merged(Mesh.cylinder(8, 0.05, 1.12, 1.26, 1.2),
                Mesh.torus(16, 8, 0.15, 0.04, 1.45).turned(1.0, 0.0, 0.0, 90.0).moved(0.0, 1.4, 0.0)));
        for (double y : new double[] { -0.4, 0.4 }) {
            pieces.add(Mesh.torus(32, 6, BAR_RING, HOOP, 1.3).moved(0.0, y, 0.0));
        }
        for (int i = 0; i < BARS; i++) {
            double angle = Math.PI * 2.0 * i / BARS;
            double x = Math.cos(angle) * BAR_RING;
            double z = Math.sin(angle) * BAR_RING;
            pieces.add(Mesh.tube(false, 6, BAR, 1.15, new Vec3(x, -0.9, z), new Vec3(x, 0.9, z)));
        }
        return pieces.toArray(Mesh[]::new);
    }

    public static Vec3 handle(Vec3 center, double radius, double grown) {
        return center.add(0.0, 1.55 * radius * grown, 0.0);
    }

    public static void draw(LanternPainter painter, ConstructPayload bubble, Vec3 center, double solid, double broken,
            boolean held, double clock, @Nullable Vec3 ring, double pound, Vec3 motion) {
        double radius = bubble.size();
        Vec3 flat = new Vec3(bubble.facing().x, 0.0, bubble.facing().z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
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
        ConstructPainter.Frame frame = ConstructPainter.Frame.of(center, flat, Vectors.UP, radius);
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
        painter.glare(Math.max(0.7 * (1.0 - Mth.clamp(solid, 0.0, 1.0)), 0.65 * flash));
        painter.shape(CAGE, shape, 1.0, 1.0 + 0.12 * Math.sin(painter.time() * 0.2) + 0.6 * flash);
        painter.glare(0.0);
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
        if (ring != null && held) {
            Vec3 hook = handle(center, radius, grown);
            if (ring.distanceToSqr(hook) > 0.25) {
                painter.beam(ring, hook, Mth.clamp(solid * 2.0, 0.0, 1.0), Math.min(1.4, radius));
            }
        }
    }

    public static boolean last(ConstructPayload pound) {
        return pound.variant() >= LightBubble.POUND.length - 1;
    }

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
