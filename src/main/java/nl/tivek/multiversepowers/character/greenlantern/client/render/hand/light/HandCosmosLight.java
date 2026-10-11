package nl.tivek.multiversepowers.character.greenlantern.client.render.hand.light;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandPainter;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandCosmos;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandGroup;
import nl.tivek.multiversepowers.engine.client.fx.Cosmos;
import nl.tivek.multiversepowers.engine.client.fx.TexturedBox;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.client.world.LocalSky;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The Cosmos Test's light, in the ring's green: the circle drawn on the ground and the hole of stars it opens, the
// flash as the hand dives in; then in the sky the rift cut beside the moon, the sky's own moon made a cube in the hand's
// grip, cracking and leaking light as it is crushed, and its burst into shards, shock rings and a storm of stars.
public final class HandCosmosLight {
    private static final int DEEP = 0x021C0A;
    private static final int LIGHT = 0x7BFF9C;
    private static final int WHITE = 0xEEFFF2;
    private static final Material COSMIC = new Material(0x0A2E14, 0x7CFF9E, 0x2FD85A, 0xD8FFE2);
    private static final Material MOON = new Material(0x505666, 0x9097A5, 0xAFB8CC, 0xD9E4FF);
    private static final ConstructPainter.Shape MOON_CUBE = ConstructPainter.Shape.of(
            Mesh.bevel(-1.0, -1.0, -1.0, 1.0, 1.0, 1.0, 0.02, 1.0));
    // The sky's moon: its eight phases in a row of four, two rows, each face 8 pixels square in the middle of 32.
    private static final ResourceLocation MOON_PHASES = ResourceLocation.withDefaultNamespace(
            "textures/environment/moon_phases.png");
    private static final int CIRCLE_STEPS = 64;
    private static final int RIFT_POINTS = 28;
    private static final int CRACKS = 26;
    private static final int STARS = 70;
    private static final double SKY = 6.0;
    private static final double BURST_TICKS = 60.0;

    private HandCosmosLight() {
    }

    public static void draw(LanternPainter painter, int id, Vec3 base, Vec3 facing, double clock, double strength,
            double apart) {
        Material lantern = painter.material();
        painter.material(COSMIC);
        ground(painter, id, base, facing, clock, strength);
        sky(painter, id, clock, strength, lantern);
        painter.material(lantern);
    }

    // The circle the fingertip draws, a ring of runes inside it as it closes, and the hole of stars it opens; the
    // flash and shock rings as the hand dives in.
    private static void ground(LanternPainter painter, int id, Vec3 base, Vec3 facing, double clock,
            double strength) {
        double drawn = HandCosmos.drawn(clock);
        double hole = HandCosmos.hole(clock);
        Vec3 along = HandCosmos.along(facing);
        Vec3 across = HandCosmos.across(facing);
        Vec3 ground = base.add(0.0, 0.06, 0.0);
        double fadeLine = strength * (1.0 - Ease.smooth((clock - HandCosmos.SHUTS) / 8.0));
        if (drawn > 0.0 && fadeLine > 0.01) {
            int steps = (int) Math.ceil(CIRCLE_STEPS * drawn);
            Vec3 last = ring(ground, along, across, HandCosmos.RADIUS, -Math.PI * 0.5);
            for (int k = 1; k <= steps; k++) {
                double turn = -Math.PI * 0.5 + Math.PI * 2.0 * Math.min(drawn, k / (double) CIRCLE_STEPS);
                Vec3 next = ring(ground, along, across, HandCosmos.RADIUS, turn);
                painter.edge(last, next, 0.07, fadeLine);
                last = next;
            }
            if (drawn < 1.0) {
                painter.flare(HandCosmos.tip(base, facing, clock), 0.35 + 0.1 * Math.sin(clock * 1.7), fadeLine);
            }
            double runes = Ease.smooth((drawn - 0.6) / 0.4) * fadeLine;
            for (int k = 0; k < 12 && runes > 0.01; k++) {
                double turn = Math.PI * 2.0 * k / 12.0 + clock * 0.02;
                Vec3 a = ring(ground, along, across, HandCosmos.RADIUS * 0.82, turn);
                Vec3 b = ring(ground, along, across, HandCosmos.RADIUS * 0.68, turn + 0.18);
                painter.edge(a, b, 0.04, runes * 0.8);
            }
        }
        if (hole > 0.0) {
            double radius = HandCosmos.RADIUS * hole;
            List<Vec3> outline = new ArrayList<>();
            for (int k = 0; k < RIFT_POINTS; k++) {
                double turn = Math.PI * 2.0 * k / RIFT_POINTS;
                double ragged = 1.0 - 0.06 * Noise.of(k, (int) (clock * 0.25), id);
                outline.add(ring(ground, along, across, radius * ragged, turn));
            }
            Cosmos.window(ground, Vectors.UP, along, outline, SKY, DEEP, LIGHT, 1.0 + 0.6 * HandCosmos.plunge(clock),
                    id);
            for (int k = 0; k < outline.size(); k++) {
                painter.edge(outline.get(k), outline.get((k + 1) % outline.size()), 0.06, strength * hole);
            }
        }
        double plunge = HandCosmos.plunge(clock) * strength;
        if (plunge > 0.01) {
            painter.flare(ground.add(0.0, 0.5, 0.0), 1.5 + 4.0 * plunge, plunge);
            double since = clock - HandCosmos.PLUNGES;
            for (int k = 0; k < 2; k++) {
                double u = Math.max(0.0, since - 2.0 * k) / 10.0;
                double fade = plunge * (1.0 - u);
                if (u < 1.0) {
                    painter.circle(ground, along, across, HandCosmos.RADIUS * (1.0 + 2.5 * u), 0.08, 0.6,
                            Colors.alpha(fade), Colors.alpha(0.5 * fade));
                }
            }
        }
    }

    private static Vec3 ring(Vec3 center, Vec3 a, Vec3 b, double radius, double turn) {
        return center.add(a.scale(Math.cos(turn) * radius)).add(b.scale(Math.sin(turn) * radius));
    }

    // Everything in the sky, laid round the moon as this eye sees it.
    private static void sky(LanternPainter painter, int id, double clock, double strength, Material lantern) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || HandCosmos.rift(clock) <= 0.0 && HandCosmos.sinceBurst(clock) > BURST_TICKS
                || clock < HandCosmos.TRACES) {
            return;
        }
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 eye = minecraft.gameRenderer.getMainCamera().getPosition();
        double away = Math.max(60.0, Math.min(160.0, minecraft.options.getEffectiveRenderDistance() * 64.0 * 0.45));
        double radius = away * 0.1;
        Vec3 skyMoon = eye.add(LocalSky.moon(minecraft.level, partialTick).scale(away));
        Vec3 moon = HandCosmos.moonAt(skyMoon, radius, eye, clock);
        rift(painter, id, skyMoon, radius, eye, clock, strength);
        if (HandCosmos.moonWhole(clock)) {
            moon(painter, id, moon, radius, eye, clock, minecraft.level.getMoonPhase());
        }
        HandGroup.Sub hand = HandCosmos.moonHand(skyMoon, radius, eye, clock);
        if (HandCosmos.rift(clock) > 0.0) {
            painter.clip(hand.portal().center(), hand.portal().normal(), HandPainter.PORTAL_SEAM);
            Material cosmic = painter.material();
            painter.material(lantern);
            HandPainter.drawHand(painter, hand.pose(), hand.place(), hand.left(), 1.15, -1.0, id * 13, true);
            painter.material(cosmic);
            painter.noClip();
        }
        burst(painter, id, moon, radius, eye, clock, strength);
    }

    // The rift beside the moon: first a cut of light drawn slowly down the sky, then a ragged window on the stars
    // opening out of it, its lips burning.
    private static void rift(LanternPainter painter, int id, Vec3 moon, double radius, Vec3 eye, double clock,
            double strength) {
        double open = HandCosmos.rift(clock);
        double traced = HandCosmos.riftTraced(clock);
        Vec3 center = HandCosmos.riftCenter(moon, radius, eye);
        Vec3 normal = HandCosmos.riftNormal(moon, radius, eye);
        Vec3[] axes = Vectors.across(normal);
        double size = radius * HandCosmos.RIFT_SIZE;
        double cut = traced * (1.0 - Ease.smooth(open / 0.2)) * strength;
        if (cut > 0.01) {
            Vec3 top = center.add(axes[1].scale(size * 1.3));
            Vec3 tip = top.lerp(center.subtract(axes[1].scale(size * 1.3)), traced);
            painter.edge(top, tip, radius * 0.04, cut);
            painter.flare(tip, radius * (0.25 + 0.05 * Math.sin(clock * 0.9)), cut);
        }
        if (open <= 0.0) {
            return;
        }
        List<Vec3> outline = new ArrayList<>();
        for (int k = 0; k < RIFT_POINTS; k++) {
            double turn = Math.PI * 2.0 * k / RIFT_POINTS;
            double ragged = 1.0 - 0.18 * Noise.of(k, (int) (clock * 0.2), id + 5);
            double tall = 1.0 + 0.5 * Math.abs(Math.sin(turn));
            outline.add(center.add(axes[0].scale(Math.cos(turn) * size * open * ragged))
                    .add(axes[1].scale(Math.sin(turn) * size * tall * Math.sqrt(open) * ragged)));
        }
        Cosmos.window(center, normal, axes[0], outline, SKY * radius * 0.3, DEEP, LIGHT, 1.3, id + 9);
        for (int k = 0; k < outline.size(); k++) {
            painter.edge(outline.get(k), outline.get((k + 1) % outline.size()), radius * 0.05, strength * open);
        }
        painter.flare(center, radius * 0.6 * open, 0.4 * open * strength);
    }

    // The moon as the sky draws it, laid on the sky's own axes so its face stands just as it did there: its texture's
    // u running north, v along the moon's path. Then turned slowly out of that face, so it shows it is a cube, drawn
    // nearer, and pressed flat by the fingers as they crush it.
    private static ConstructPainter.Frame moonFrame(Vec3 moon, double radius, Vec3 eye, double clock) {
        double crush = HandCosmos.crush(clock);
        Vec3 view = moon.subtract(eye).normalize();
        Vec3 path = view.cross(new Vec3(0.0, 0.0, 1.0));
        path = path.lengthSqr() < 1.0E-6 ? Vectors.UP : path.normalize();
        Vec3 north = view.cross(path).normalize();
        double turn = HandCosmos.moonTurn(clock);
        Vec3 right = north.scale(Math.cos(turn)).add(view.scale(Math.sin(turn)));
        Vec3 forward = view.scale(Math.cos(turn)).subtract(north.scale(Math.sin(turn)));
        double tilt = turn * 0.35;
        Vec3 up = path.scale(Math.cos(tilt)).add(forward.scale(Math.sin(tilt)));
        forward = forward.scale(Math.cos(tilt)).subtract(path.scale(Math.sin(tilt)));
        return new ConstructPainter.Frame(moon, right, up, forward, radius * HandCosmos.moonSize(clock))
                .stretched(1.0 - 0.26 * crush, 1.0 + 0.12 * crush, 1.0 - 0.18 * crush);
    }

    // The moon made solid: the sky's own moon, its square face the face of a cube, cracks spreading over it as it is
    // crushed and light leaking out of them, brighter and brighter.
    private static void moon(LanternPainter painter, int id, Vec3 moon, double radius, Vec3 eye, double clock,
            int phase) {
        double crush = HandCosmos.crush(clock);
        ConstructPainter.Frame frame = moonFrame(moon, radius, eye, clock);
        Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = frame.at((i & 1) * 2.0 - 1.0, ((i >> 1) & 1) * 2.0 - 1.0, ((i >> 2) & 1) * 2.0 - 1.0);
        }
        float u = (Math.floorMod(phase, 4) * 32 + 12) / 128.0F;
        float v = (Math.floorMod(phase, 8) / 4 * 32 + 12) / 64.0F;
        TexturedBox.draw(MOON_PHASES, u, v, u + 8.0F / 128.0F, v + 8.0F / 64.0F, corners);
        if (crush <= 0.0) {
            return;
        }
        Vec3 camera = painter.camera();
        int cracks = (int) Math.ceil(CRACKS * Math.min(1.0, crush * 1.3));
        for (int k = 0; k < cracks; k++) {
            Vec3 from = Noise.direction(k * 3 + 1, id + 31);
            Vec3 to = Noise.direction(k * 3 + 2, id + 31);
            double grown = Math.min(1.0, crush * 1.3 * CRACKS - k);
            Vec3 last = onCube(frame, from, camera, radius);
            for (int s = 1; s <= 6; s++) {
                Vec3 next = onCube(frame, from.lerp(to, s / 6.0 * 0.45 * grown), camera, radius);
                painter.edge(last, next, radius * (0.02 + 0.025 * crush), 0.5 + 0.5 * crush);
                last = next;
            }
            if (k % 4 == 0) {
                painter.flare(last, radius * (0.08 + 0.2 * crush), crush);
            }
        }
        painter.flare(moon, radius * (0.6 + 1.6 * crush * crush), 0.1 + 0.8 * crush);
    }

    // A way out of the cube's middle laid on its surface, lifted a little toward the eye so it never sinks into it.
    private static Vec3 onCube(ConstructPainter.Frame frame, Vec3 way, Vec3 camera, double radius) {
        double most = Math.max(Math.abs(way.x), Math.max(Math.abs(way.y), Math.abs(way.z)));
        Vec3 on = most < 1.0E-6 ? way : way.scale(1.0 / most);
        Vec3 at = frame.at(on.x, on.y, on.z);
        return at.add(camera.subtract(at).normalize().scale(radius * 0.03));
    }

    // The moon bursting: a blinding flash, shards flung out, rings of shock across the sky and a storm of stars that
    // flies out, twinkles and fades.
    private static void burst(LanternPainter painter, int id, Vec3 moon, double radius, Vec3 eye, double clock,
            double strength) {
        double since = HandCosmos.sinceBurst(clock);
        if (since < 0.0 || since > BURST_TICKS) {
            return;
        }
        double u = since / BURST_TICKS;
        Vec3 view = moon.subtract(eye).normalize();
        Vec3[] axes = Vectors.across(view);
        if (since < 10.0) {
            double flash = 1.0 - since / 10.0;
            painter.flare(moon, radius * (3.0 + 5.0 * (1.0 - flash)), flash * strength);
            painter.glowDisc(moon, radius * (2.0 + 4.0 * (1.0 - flash)), WHITE, flash * strength, 0.3, id);
        }
        Material cosmic = painter.material();
        painter.material(MOON);
        painter.fling(5.0);
        painter.shattered(MOON_CUBE, moonFrame(moon, radius, eye, clock), Math.min(1.0, Math.sqrt(since / 30.0)),
                1.2, id + 77);
        painter.fling(1.0);
        painter.material(cosmic);
        for (int k = 0; k < 3; k++) {
            double w = Math.max(0.0, since - 4.0 * k) / (BURST_TICKS - 4.0 * k);
            if (w <= 0.0 || w >= 1.0) {
                continue;
            }
            double fade = strength * (1.0 - w) * (1.0 - w);
            Vec3 a = Vectors.spin(axes[0], view, k * 0.7);
            Vec3 b = Vectors.spin(axes[1], view, k * 0.7).add(view.scale(0.25 * k)).normalize();
            painter.circle(moon, a, b, radius * (1.2 + 9.0 * (1.0 - (1.0 - w) * (1.0 - w))), radius * 0.08,
                    radius * 0.4, Colors.alpha(0.9 * fade), Colors.alpha(0.45 * fade));
        }
        for (int k = 0; k < STARS; k++) {
            Vec3 way = Noise.direction(k * 5 + 11, id + 51);
            double speed = 0.6 + 1.4 * Noise.of(k, 3, id + 52);
            double out = radius * (1.0 + 10.0 * speed * (1.0 - (1.0 - u) * (1.0 - u)));
            double twinkle = 0.6 + 0.4 * Math.sin(clock * (0.8 + speed) + k);
            double fade = strength * (1.0 - u) * twinkle;
            Vec3 at = moon.add(way.scale(out));
            painter.flare(at, radius * (0.1 + 0.15 * Noise.of(k, 7, id + 53)), fade);
            if (since < 20.0) {
                painter.edge(moon.add(way.scale(out * 0.7)), at, radius * 0.03, fade * (1.0 - since / 20.0));
            }
        }
    }
}
