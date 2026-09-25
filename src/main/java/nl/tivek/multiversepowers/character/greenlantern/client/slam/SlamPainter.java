package nl.tivek.multiversepowers.character.greenlantern.client.slam;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.ability.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

public final class SlamPainter {
    static final Vec3 UP = Vectors.UP;
    static final Vec3 DOWN = new Vec3(0.0, -1.0, 0.0);
    static final double HANG = 2.6;
    static final double RISE = 0.7;
    private static final double BREAK_TICKS = 8.0;
    private static final double FACING_HIM = Math.toRadians(35.0);
    private static final double WAVE_TICKS = 12.0;
    private static final double WAVE_FADE = 16.0;

    private SlamPainter() {
    }

    record Moment(Vec3 ground, Vec3 forward, Vec3 right, Vec3 him, double t, double size, double grow,
            double windup, double go, double fall, double since, double flash, double apart, double burst) {
        boolean struck() {
            return this.since >= 0.0;
        }

        double bright() {
            return 1.0 + 0.5 * this.flash;
        }

        ConstructPainter.Frame standing(double scale) {
            return new ConstructPainter.Frame(this.ground, this.right, UP, this.forward, scale);
        }

        double scale(double scale) {
            return scale * this.size * this.grow;
        }
    }

    public static double pace(ConstructPayload slam) {
        return slam.charge() > 0.0F ? slam.charge() : 1.0;
    }

    static double size(ConstructPayload slam) {
        return slam.solid() > 0.0F ? slam.solid() : 1.0;
    }

    public static void draw(LanternPainter painter, ConstructPayload slam, double clock, @Nullable Vec3 ring,
            @Nullable Vec3 him) {
        Vec3 ground = slam.center();
        double t = clock / pace(slam);
        double size = size(slam);
        Vec3 forward = new Vec3(slam.facing().x, 0.0, slam.facing().z);
        forward = forward.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
        Vec3 right = forward.cross(UP).normalize();
        Vec3 feet = him != null ? him : ground.subtract(forward.scale(LandingSlam.ahead(slam.variant(), size)));
        double windup = Ease.smooth((t - LandingSlam.FORM_TICKS)
                / (LandingSlam.HANG_TICKS - LandingSlam.FORM_TICKS));
        double go = Mth.clamp((t - LandingSlam.HANG_TICKS) / (LandingSlam.IMPACT_TICK - LandingSlam.HANG_TICKS),
                0.0, 1.0);
        double since = t - LandingSlam.IMPACT_TICK;
        double flash = since >= 0.0 ? Mth.clamp(1.0 - since / 4.0, 0.0, 1.0) : 0.0;
        double burst = burst(slam.variant());
        double apart = Mth.clamp((t - burst) / BREAK_TICKS, 0.0, 1.0);
        Moment m = new Moment(ground, forward, right, feet, t, size, backOut(t / LandingSlam.FORM_TICKS), windup, go,
                go * go, since, flash, apart, burst);
        if (apart < 1.0) {
            painter.glare(Math.max(0.75 * flash, 0.55 * (1.0 - Ease.smooth(t / LandingSlam.FORM_TICKS))));
            Vec3 anchor = construct(painter, slam.variant(), m);
            painter.glare(0.0);
            if (ring != null && t < LandingSlam.IMPACT_TICK + 2.0) {
                painter.beam(ring, anchor, m.struck() ? 0.5 : 1.0, 2.0);
            }
            if (drops(slam.variant())) {
                streaks(painter, anchor, m);
            }
        }
        fistImpact(painter, m);
        if (m.struck()) {
            wave(painter, m, slam.size());
        }
    }

    static boolean drops(int variant) {
        return switch (variant) {
            case ConstructPayload.SLAM_FIST, ConstructPayload.SLAM_HAMMER, ConstructPayload.SLAM_ANVIL,
                    ConstructPayload.SLAM_BOOT, ConstructPayload.SLAM_WEIGHT, ConstructPayload.SLAM_SWORD,
                    ConstructPayload.SLAM_LANTERN, ConstructPayload.SLAM_SAFE, ConstructPayload.SLAM_ANCHOR,
                    ConstructPayload.SLAM_MACE, ConstructPayload.SLAM_BARBELL, ConstructPayload.SLAM_BELL,
                    ConstructPayload.SLAM_PALM, ConstructPayload.SLAM_PIANO, ConstructPayload.SLAM_BRICK,
                    ConstructPayload.SLAM_STAMP -> true;
            default -> false;
        };
    }

    public static float look(int variant) {
        if (drops(variant)) {
            return 15.0F;
        }
        return switch (variant) {
            case ConstructPayload.SLAM_SWATTER, ConstructPayload.SLAM_PICKAXE -> 12.0F;
            case ConstructPayload.SLAM_HANDS, ConstructPayload.SLAM_FISTS, ConstructPayload.SLAM_CYMBALS,
                    ConstructPayload.SLAM_BOOK, ConstructPayload.SLAM_ROCKETS, ConstructPayload.SLAM_METEOR,
                    ConstructPayload.SLAM_EMBLEM, ConstructPayload.SLAM_GAVEL -> 8.0F;
            default -> 0.0F;
        };
    }

    private static void streaks(LanternPainter painter, Vec3 top, Moment m) {
        if (m.struck() || m.go() <= 0.0) {
            return;
        }
        double speed = 2.0 * m.go();
        double length = (0.6 + 2.6 * speed) * m.size();
        for (int k = 0; k < 7; k++) {
            double angle = Math.PI * 2.0 * (k + Noise.of(k, 71, 0)) / 7.0;
            double out = (0.5 + 0.9 * Noise.of(k, 71, 1)) * m.size();
            Vec3 foot = top.add(m.right().scale(Math.cos(angle) * out)).add(m.forward().scale(Math.sin(angle) * out))
                    .subtract(0.0, (0.4 + 1.2 * Noise.of(k, 71, 2)) * m.size(), 0.0);
            painter.edge(foot, foot.add(0.0, length * (0.6 + 0.4 * Noise.of(k, 71, 3)), 0.0),
                    0.05 * m.size(), 0.35 + 0.5 * speed);
        }
    }

    // Must always land before LandingSlam.END_TICK, when the server ends the slam.
    private static double burst(int variant) {
        return switch (variant) {
            case ConstructPayload.SLAM_SAFE -> LandingSlam.END_TICK - BREAK_TICKS - 1.0;
            case ConstructPayload.SLAM_PIANO, ConstructPayload.SLAM_ANCHOR -> LandingSlam.END_TICK - BREAK_TICKS - 3.0;
            default -> LandingSlam.BURST_TICK;
        };
    }

    private static Vec3 construct(LanternPainter painter, int variant, Moment m) {
        return switch (variant) {
            case ConstructPayload.SLAM_HANDS -> SlamHands.hands(painter, m);
            case ConstructPayload.SLAM_FISTS -> SlamHands.fists(painter, m);
            case ConstructPayload.SLAM_HAMMER -> SlamDrops.hammer(painter, m);
            case ConstructPayload.SLAM_EMBLEM -> SlamRisers.emblem(painter, m);
            case ConstructPayload.SLAM_ANVIL -> SlamCartoon.anvil(painter, m);
            case ConstructPayload.SLAM_CYMBALS -> SlamStrikes.cymbals(painter, m);
            case ConstructPayload.SLAM_UPPERCUT -> SlamHands.uppercut(painter, m);
            case ConstructPayload.SLAM_SPIKES -> SlamRisers.spikes(painter, m);
            case ConstructPayload.SLAM_BOOT -> SlamDrops.boot(painter, m);
            case ConstructPayload.SLAM_WEIGHT -> SlamCartoon.weight(painter, m);
            case ConstructPayload.SLAM_SWORD -> SlamDrops.sword(painter, m);
            case ConstructPayload.SLAM_ROCKETS -> SlamDrops.rockets(painter, m);
            case ConstructPayload.SLAM_SWATTER -> SlamStrikes.swatter(painter, m);
            case ConstructPayload.SLAM_LANTERN -> SlamDrops.lantern(painter, m);
            case ConstructPayload.SLAM_SAFE -> SlamCartoon.safe(painter, m);
            case ConstructPayload.SLAM_ANCHOR -> SlamDrops.anchor(painter, m);
            case ConstructPayload.SLAM_MACE -> SlamDrops.mace(painter, m);
            case ConstructPayload.SLAM_BARBELL -> SlamDrops.barbell(painter, m);
            case ConstructPayload.SLAM_BELL -> SlamDrops.bell(painter, m);
            case ConstructPayload.SLAM_METEOR -> SlamDrops.meteor(painter, m);
            case ConstructPayload.SLAM_PALM -> SlamHands.palm(painter, m);
            case ConstructPayload.SLAM_GAVEL -> SlamStrikes.gavel(painter, m);
            case ConstructPayload.SLAM_PICKAXE -> SlamStrikes.pickaxe(painter, m);
            case ConstructPayload.SLAM_TRAP -> SlamStrikes.trap(painter, m);
            case ConstructPayload.SLAM_BOOK -> SlamStrikes.book(painter, m);
            case ConstructPayload.SLAM_DRUM -> SlamStrikes.drum(painter, m);
            case ConstructPayload.SLAM_PILLAR -> SlamRisers.pillar(painter, m);
            case ConstructPayload.SLAM_TNT -> SlamCartoon.tnt(painter, m);
            case ConstructPayload.SLAM_PIANO -> SlamCartoon.piano(painter, m);
            case ConstructPayload.SLAM_BRICK -> SlamCartoon.brick(painter, m);
            case ConstructPayload.SLAM_STAMP -> SlamCartoon.stamp(painter, m);
            default -> SlamHands.skyFist(painter, m);
        };
    }

    static Vec3 facingHim(Vec3 way) {
        return Vectors.spin(way, UP, FACING_HIM);
    }

    static void piece(LanternPainter painter, double[][] model, ConstructPainter.Frame frame, Moment m) {
        if (m.apart() > 0.0) {
            painter.shattered(model, frame, m.apart(), m.bright());
        } else {
            painter.model(model, frame, 1.0, m.bright());
        }
    }

    static void piece(LanternPainter painter, ConstructPainter.Shape shape, ConstructPainter.Frame frame,
            Moment m) {
        if (m.apart() > 0.0) {
            painter.shattered(shape, frame, m.apart(), m.bright());
        } else {
            painter.shape(shape, frame, 1.0, m.bright());
        }
    }

    static double spring(double since, double rate) {
        if (since <= 0.0) {
            return 0.0;
        }
        return 1.0 - Math.exp(-0.45 * rate * since) * Math.cos(rate * since);
    }

    static double bounce(double since, double rate) {
        if (since <= 0.0) {
            return 0.0;
        }
        return 1.0 - Math.abs(Math.exp(-0.45 * rate * since) * Math.cos(rate * since));
    }

    static ConstructPainter.Frame loose(ConstructPainter.Frame base, Vec3 at, Vec3 axis, double angle) {
        return new ConstructPainter.Frame(base.at(at.x, at.y, at.z), Vectors.spin(base.right(), axis, angle),
                Vectors.spin(base.up(), axis, angle), Vectors.spin(base.forward(), axis, angle),
                base.scale());
    }

    static ConstructPainter.Frame squashed(ConstructPainter.Frame frame, Moment m, double amount) {
        if (!m.struck() || m.since() > 12.0) {
            return frame;
        }
        double squash = amount * Math.exp(-0.5 * m.since()) * Math.cos(1.4 * m.since());
        double tall = 1.0 - squash;
        double wide = 1.0 / Math.sqrt(tall);
        return frame.stretched(wide, tall, wide);
    }

    static void sparks(LanternPainter painter, Vec3 at, double since, int count, double speed, int seed) {
        if (since < 0.0 || since > 7.0) {
            return;
        }
        double fade = 1.0 - since / 7.0;
        for (int k = 0; k < count; k++) {
            Vec3 way = Noise.direction(seed, k);
            way = new Vec3(way.x, 0.25 + 0.75 * Math.abs(way.y), way.z).normalize();
            double fast = speed * (0.5 + Noise.of(seed, k, 14));
            double before = Math.max(0.0, since - 0.9);
            Vec3 head = at.add(way.scale(fast * since)).add(0.0, -0.035 * since * since, 0.0);
            Vec3 tail = at.add(way.scale(fast * before)).add(0.0, -0.035 * before * before, 0.0);
            painter.edge(tail, head, 0.06, fade);
        }
    }

    static double drop(Moment m) {
        return (HANG + RISE * m.windup()) * m.size() * (1.0 - m.fall());
    }

    static ConstructPainter.Frame dropped(Moment m, double scale, double foot, Vec3 axis, double tumble) {
        double s = m.scale(scale);
        double height = drop(m) + foot * s - (m.struck() ? 0.12 * s : 0.0);
        double angle = tumble * (1.0 - m.go()) - 0.12 * m.windup() * (1.0 - m.go());
        return new ConstructPainter.Frame(m.ground().add(0.0, height, 0.0),
                Vectors.spin(m.right(), axis, angle), Vectors.spin(UP, axis, angle),
                Vectors.spin(m.forward(), axis, angle), s);
    }

    static void marker(LanternPainter painter, Moment m, double size) {
        if (m.struck() || m.t() < 1.0) {
            return;
        }
        double pulse = 0.6 + 0.4 * Math.sin(m.t() * 1.8);
        painter.circle(m.ground().add(0.0, 0.06, 0.0), m.right(), m.forward(),
                size * m.size() * (1.3 - 0.5 * m.go()), 0.07, 0.35, Colors.alpha(0.8 * pulse),
                Colors.alpha(0.4 * pulse));
    }

    static double vibrate(Moment m, double size) {
        if (m.since() <= 0.0) {
            return 0.0;
        }
        return size * Math.abs(Math.sin(m.since() * 2.6)) * Math.max(0.0, 1.0 - m.since() / 10.0);
    }

    public static double backOut(double x) {
        double c = Mth.clamp(x, 0.0, 1.0) - 1.0;
        return 1.0 + 2.2 * c * c * c + 1.2 * c * c;
    }

    private static void fistImpact(LanternPainter painter, Moment m) {
        Vec3 at = m.him().add(m.forward().scale(0.5)).add(m.right().scale(0.3));
        if (m.t() < 20.0) {
            cracks(painter, at, 1.5, 1.0 - m.t() / 20.0, 1);
        }
        rubble(painter, at, 0.0, m.t(), 7, 0.16, 0.24, 1);
    }

    static void cracks(LanternPainter painter, Vec3 at, double reach, double strength, int seed) {
        if (strength <= 0.0) {
            return;
        }
        int count = 9;
        for (int k = 0; k < count; k++) {
            double angle = Math.PI * 2.0 * (k + Noise.of(seed, k, 1)) / count;
            double length = reach * (0.55 + 0.45 * Noise.of(seed, k, 2));
            Vec3 last = at.add(0.0, 0.03, 0.0);
            for (int j = 1; j <= 4; j++) {
                double d = length * j / 4.0;
                double bend = angle + (Noise.of(seed, k, 3 + j) - 0.5) * 0.7;
                Vec3 next = at.add(Math.cos(bend) * d, 0.03, Math.sin(bend) * d);
                painter.edge(last, next, 0.07 * (1.25 - 0.2 * j), Math.min(1.0, strength));
                last = next;
            }
        }
    }

    static void buildUp(LanternPainter painter, Vec3 at, double reach, Moment m, int seed) {
        if (m.struck()) {
            return;
        }
        double warn = Mth.clamp(m.t() / LandingSlam.IMPACT_TICK, 0.0, 1.0);
        double pulse = 0.65 + 0.35 * Math.sin(m.t() * (1.0 + 2.5 * warn));
        painter.flare(at.add(0.0, 0.15, 0.0), (0.7 + 1.6 * warn) * m.size(), (0.45 + 0.55 * warn) * pulse);
        for (int k = 0; k < 9; k++) {
            double angle = Math.PI * 2.0 * (k + Noise.of(seed, k, 31)) / 9.0;
            double out = reach * (0.2 + 0.7 * Noise.of(seed, k, 32));
            Vec3 foot = at.add(Math.cos(angle) * out, 0.03, Math.sin(angle) * out);
            double flicker = 0.5 + 0.5 * Math.sin(m.t() * (2.0 + Noise.of(seed, k, 33) * 2.0) + k);
            double height = (0.3 + 2.4 * warn * Noise.of(seed, k, 34)) * m.size() * flicker;
            painter.edge(foot, foot.add(0.0, height, 0.0), 0.09 * m.size(), (0.3 + 0.7 * warn) * flicker);
        }
    }

    static void rubble(LanternPainter painter, Vec3 at, double from, double t, int count, double size,
            double speed, int seed) {
        double age = t - from;
        if (age < 0.0 || age > 16.0) {
            return;
        }
        for (int k = 0; k < count; k++) {
            double yaw = Math.PI * 2.0 * (k + Noise.of(seed, k, 5)) / count;
            double up = (0.8 + 0.8 * Noise.of(seed, k, 6)) * speed * 1.4;
            double out = speed * (0.6 + 0.8 * Noise.of(seed, k, 7));
            Vec3 pos = at.add(Math.cos(yaw) * out * age, up * age - 0.05 * age * age, Math.sin(yaw) * out * age);
            if (pos.y < at.y - 0.2) {
                continue;
            }
            double chunk = size * (0.6 + 0.8 * Noise.of(seed, k, 8));
            painter.chunk(pos, chunk, Noise.direction(seed + k, 4), age * 0.5, 1.0);
        }
    }

    private static void wave(LanternPainter painter, Moment m, double reach) {
        double since = m.since();
        if (since >= WAVE_FADE) {
            return;
        }
        double out = 1.0 - Math.pow(1.0 - Mth.clamp(since / WAVE_TICKS, 0.0, 1.0), 3.0);
        double radius = Math.max(0.3, reach * out);
        double height = 0.6 * Math.pow(1.0 - since / WAVE_FADE, 1.4);
        int segments = 40;
        double half = Math.PI * radius / segments;
        Vec3 foot = m.ground().add(0.0, 0.02, 0.0);
        double[][] block = { { -half, 0.0, -0.13, half, height, 0.13, 1.1 } };
        for (int i = 0; i < segments; i++) {
            double angle = Math.PI * 2.0 * (i + 0.5) / segments;
            Vec3 way = m.right().scale(Math.cos(angle)).add(m.forward().scale(Math.sin(angle)));
            painter.model(block, new ConstructPainter.Frame(foot.add(way.scale(radius)), way.cross(UP), UP, way, 1.0),
                    1.0, 1.0);
        }
        double fade = Math.pow(1.0 - since / WAVE_FADE, 1.5);
        painter.circle(foot.add(0.0, 0.03, 0.0), m.right(), m.forward(), radius, 0.12 + 0.12 * fade, 0.9,
                Colors.alpha(fade), Colors.alpha(0.6 * fade));
        if (since < 5.0) {
            double burst = 1.0 - since / 5.0;
            painter.flare(m.ground().add(0.0, 0.6, 0.0), 0.5 + 1.6 * burst, burst);
        }
    }
}
