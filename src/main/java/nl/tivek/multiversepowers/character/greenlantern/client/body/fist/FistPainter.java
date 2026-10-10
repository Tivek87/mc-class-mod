package nl.tivek.multiversepowers.character.greenlantern.client.body.fist;

import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.fist.FistMoves;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The fists' constructs: a glove of hard light over each hand (a cuff round the wrist, a block over the hand, a ridge
// over the knuckles, a plate on its back and one along the thumb), in an arm's own frame (blocks, y down the arm, -z
// its front, the right arm's middle at x -1/16); and the heavies' giant fists, drawn in the world: slammed down from
// above, bursting up out of the ground, rammed out on a piston. They grow out of the ring's light, from the knuckles
// back, and break into solid pieces or sink away.
public final class FistPainter {
    private static final double KNUCKLES = 0.67;
    private static final double CUFF = 0.32;
    private static final Shape RIGHT = Shape.of(glove(true));
    private static final Shape LEFT = Shape.of(glove(false));
    private static final Shape GIANT = Shape.of(giant());
    private static final Shape ROD = Shape.of(Mesh.bevel(-0.5, 0.0, -0.5, 0.5, 1.0, 0.5, 0.08, 1.0),
            Mesh.bevel(-0.62, 0.0, -0.62, 0.62, 0.08, 0.62, 0.04, 1.3));
    // A giant fist's own middle, in its frame.
    private static final Vec3 MIDDLE = new Vec3(-0.0625, 0.5, 0.0);
    private static final double GIANT_SIZE = 5.0;
    private static final double PISTON_SIZE = 3.2;
    private static final double ROD_WIDE = 0.35;
    private static final double DROP = 6.0;

    private FistPainter() {
    }

    private static Mesh[] glove(boolean right) {
        return new Mesh[] { box(right, -0.23, CUFF, -0.16, 0.105, 0.43, 0.16, 0.03, 1.25),
                box(right, -0.215, 0.43, -0.15, 0.09, 0.66, 0.15, 0.04, 1.0),
                box(right, -0.21, 0.56, -0.19, 0.085, KNUCKLES, -0.13, 0.025, 1.3),
                box(right, -0.25, 0.45, -0.11, -0.2, 0.63, 0.11, 0.02, 1.15),
                box(right, 0.08, 0.47, -0.12, 0.12, 0.6, 0.02, 0.02, 1.1) };
    }

    private static Mesh[] giant() {
        Mesh[] hand = glove(true);
        Mesh[] all = new Mesh[hand.length + 1];
        System.arraycopy(hand, 0, all, 0, hand.length);
        all[hand.length] = Mesh.bevel(-0.19, -0.4, -0.13, 0.065, CUFF, 0.13, 0.04, 0.95);
        return all;
    }

    // A box for the right glove, or the same across the arm's middle for the left.
    private static Mesh box(boolean right, double x0, double y0, double z0, double x1, double y1, double z1,
            double bevel, double bright) {
        return right ? Mesh.bevel(x0, y0, z0, x1, y1, z1, bevel, bright)
                : Mesh.bevel(-x1, y0, z0, -x0, y1, z1, bevel, bright);
    }

    // A glove over a hand, in its arm's frame as the pose stack holds it: `formed` of it grown, from the knuckles
    // back, or breaking apart `apart` of the way; `size` times a hand's own.
    static void glove(LanternPainter painter, boolean right, double formed, double apart, double size, int seed) {
        double middle = right ? -0.0625 : 0.0625;
        Frame frame = new Frame(new Vec3(middle * (1.0 - size), 0.5 * (1.0 - size), 0.0), new Vec3(1.0, 0.0, 0.0),
                new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, 1.0), size);
        Shape shape = right ? RIGHT : LEFT;
        if (apart >= 0.0) {
            painter.shattered(shape, frame, Math.min(1.0, apart), 1.0, seed);
            return;
        }
        if (formed < 1.0) {
            painter.clip(frame.at(0.0, Mth.lerp(Ease.smooth(formed), KNUCKLES + 0.02, CUFF - 0.02), 0.0),
                    new Vec3(0.0, 1.0, 0.0), 1.0);
        }
        painter.shape(shape, frame, 1.0, 1.0);
        painter.noClip();
    }

    public static boolean any() {
        return !ClientFists.all().isEmpty();
    }

    // Every heavy's giant fist in the world.
    public static void drawAll(LanternPainter painter, ClientLevel level, float partialTick) {
        for (Map.Entry<Integer, ClientFists.View> entry : ClientFists.all().entrySet()) {
            ClientFists.View view = entry.getValue();
            if (!FistMoves.heavy(view.move())) {
                continue;
            }
            double age = view.age(partialTick);
            if (age < 0.0 || age > FistMoves.length(view.move()) + 2.0) {
                continue;
            }
            Entity owner = level.getEntity(entry.getKey());
            double yaw = Math.toRadians(view.yaw());
            Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
            Vec3 side = ahead.cross(Vectors.UP).normalize();
            Vec3 ground = view.at().add(ahead.scale(FistMoves.AHEAD));
            int seed = entry.getKey() * 31 + view.move();
            Vec3 ring = owner == null ? view.at().add(0.0, 1.2, 0.0) : owner.getPosition(partialTick).add(0.0, 1.2,
                    0.0);
            switch (view.move()) {
                case FistMoves.HAMMER -> hammer(painter, ground, ahead, side, ring, age, seed);
                case FistMoves.RISING -> rising(painter, ground, ahead, side, ring, age, seed);
                case FistMoves.PISTON -> piston(painter, view.at(), ahead, side, ring, age, seed);
                case FistMoves.SPIN -> sweep(painter, view.at(), age);
                default -> {
                }
            }
        }
    }

    // Formed high over the ground ahead, it drops onto it at the blow, rests a moment and breaks apart.
    private static void hammer(LanternPainter painter, Vec3 ground, Vec3 ahead, Vec3 side, Vec3 ring, double age,
            int seed) {
        double hit = FistMoves.hit(FistMoves.HAMMER);
        double formed = Ease.smooth((age - 3.0) / 6.0);
        if (formed <= 0.0) {
            return;
        }
        double fall = Ease.smoother((age - (hit - 4.0)) / 4.0);
        double fallen = fall * fall;
        Vec3 knuckles = ground.add(0.0, Mth.lerp(fallen, DROP, 0.0), 0.0);
        // Knuckles down, the back of the hand to the sky, the arm reaching up out of it.
        Frame frame = fist(knuckles, new Vec3(0.0, -1.0, 0.0), side, ahead.scale(-1.0), GIANT_SIZE);
        double apart = (age - hit - 4.0) / 6.0;
        draw(painter, frame, formed, apart, seed);
        if (formed < 1.0) {
            painter.beam(ring, knuckles.add(0.0, GIANT_SIZE * 0.5, 0.0), 1.0 - formed, 1.0);
        }
        crack(painter, ground, age - hit);
    }

    // Up out of the ground ahead, knuckles first, to well over a man's height at the blow; then it sinks away.
    private static void rising(LanternPainter painter, Vec3 ground, Vec3 ahead, Vec3 side, Vec3 ring, double age,
            int seed) {
        double hit = FistMoves.hit(FistMoves.RISING);
        double up = Ease.smoother((age - (hit - 4.0)) / 4.0) - Ease.smooth((age - hit - 4.0) / 8.0);
        if (up <= 0.0) {
            return;
        }
        double top = Mth.lerp(up, -0.5, GIANT_SIZE * 0.75);
        Vec3 knuckles = ground.add(0.0, top, 0.0);
        Frame frame = fist(knuckles, Vectors.UP, side.scale(-1.0), ahead.scale(-1.0), GIANT_SIZE);
        painter.clip(ground, Vectors.UP, 1.0);
        painter.shape(GIANT, frame, 1.0, 1.0);
        painter.noClip();
        crack(painter, ground, age - (hit - 4.0));
        if (age < hit) {
            painter.beam(ring, ground, 0.6, 0.8);
        }
    }

    // Out of his right fist a rod drives a giant fist straight out ahead to the piston's reach, holds, and draws it
    // back; then both break apart.
    private static void piston(LanternPainter painter, Vec3 at, Vec3 ahead, Vec3 side, Vec3 ring, double age,
            int seed) {
        double hit = FistMoves.hit(FistMoves.PISTON);
        double formed = Ease.smooth((age - (hit - 4.0)) / 2.0);
        if (formed <= 0.0) {
            return;
        }
        double out = Ease.smoother((age - (hit - 3.0)) / 3.0) * (1.0 - 0.6 * Ease.smooth((age - hit - 4.0) / 5.0));
        Vec3 chest = at.add(0.0, 1.25, 0.0).add(side.scale(0.3)).add(ahead.scale(0.6));
        double reach = Mth.lerp(out, 0.4, FistMoves.PISTON_REACH);
        Vec3 knuckles = chest.add(ahead.scale(reach + PISTON_SIZE * KNUCKLES * 0.5));
        Frame frame = fist(knuckles, ahead, side, Vectors.UP.scale(-1.0), PISTON_SIZE);
        double apart = (age - hit - 7.0) / 4.0;
        draw(painter, frame, formed, apart, seed);
        Frame rod = Frame.of(chest, Vectors.UP, ahead.scale(-1.0), 1.0);
        rod = new Frame(chest, rod.right().scale(ROD_WIDE), ahead.scale(Math.max(0.05, reach)),
                rod.forward().scale(ROD_WIDE), 1.0);
        if (apart >= 0.0) {
            painter.shattered(ROD, rod, Math.min(1.0, apart), 1.0, seed + 7);
        } else {
            painter.shape(ROD, rod, 1.0, 1.0);
        }
    }

    // A ring of light sweeping out round him as he turns (light, no construct).
    private static void sweep(LanternPainter painter, Vec3 at, double age) {
        double hit = FistMoves.hit(FistMoves.SPIN);
        double u = (age - hit + 2.0) / 6.0;
        if (u <= 0.0 || u >= 1.0) {
            return;
        }
        double fade = 1.0 - Ease.smooth(u);
        painter.circle(at.add(0.0, 1.1, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0),
                Mth.lerp(Ease.smooth(u), 1.0, 3.2), 0.06, 0.45, Colors.alpha(0.9 * fade), Colors.alpha(0.5 * fade));
    }

    // Where a giant fist strikes the ground: a ring of light running out from it.
    private static void crack(LanternPainter painter, Vec3 ground, double since) {
        double u = since / 8.0;
        if (u <= 0.0 || u >= 1.0) {
            return;
        }
        double fade = 1.0 - Ease.smooth(u);
        painter.circle(ground.add(0.0, 0.05, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0),
                Mth.lerp(Ease.smooth(u), 0.8, 3.4), 0.07, 0.5, Colors.alpha(0.95 * fade), Colors.alpha(0.55 * fade));
    }

    // A giant fist's frame: its knuckles at `knuckles`, the arm's line pointing `way` (from the wrist to the
    // knuckles), `across` its x and `front` the way its knuckle face looks (its -z).
    private static Frame fist(Vec3 knuckles, Vec3 way, Vec3 across, Vec3 front, double size) {
        Vec3 z = front.scale(-1.0);
        Vec3 origin = knuckles.subtract(way.scale(KNUCKLES * size)).subtract(across.scale(MIDDLE.x * size));
        return new Frame(origin, across, way, z, size);
    }

    private static void draw(LanternPainter painter, Frame frame, double formed, double apart, int seed) {
        if (apart >= 0.0) {
            if (apart < 1.0) {
                painter.shattered(GIANT, frame, apart, 1.0, seed);
            }
            return;
        }
        if (formed < 1.0) {
            painter.clip(frame.at(0.0, Mth.lerp(Ease.smooth(formed), KNUCKLES + 0.02, -0.42), 0.0), frame.up(), 1.0);
        }
        painter.shape(GIANT, frame, 1.0, 1.0);
        painter.noClip();
    }
}
