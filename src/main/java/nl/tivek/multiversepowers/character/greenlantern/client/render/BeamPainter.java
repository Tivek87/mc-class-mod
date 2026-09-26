package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.Arrays;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightBeam;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The held Light Beam, with a look of its own at each of its five stages: a thin, steady ray at the first, a raging,
// unstable torrent ringed with energy at the fifth. It is light, not a construct, so it glows through.
public final class BeamPainter {
    private static final int GREEN = LanternPainter.GREEN;
    private static final int BRIGHT = LanternPainter.BRIGHT;
    private static final int HOT = LanternPainter.HOT;
    private static final double NEAR = 0.25;
    private static final double SHOOT = 2.5;
    private static final double BURST = 6.0;
    private static final double GROW = 6.0;
    private static final double SURGE_TICKS = 12.0;
    private static final double SURGE_SPEED = 7.0;
    private static final double[] FURY = { 0.0, 0.1, 0.35, 0.7, 1.0 };
    private static final double[] SHAKY = { 0.0, 0.0, 0.0, 0.45, 1.0 };
    private static final int[] STRANDS = { 2, 3, 3, 4, 5 };
    private static final int[] FOCUS = { 0, 0, 1, 2, 3 };
    private static final int[] BANDS = { 0, 0, 0, 2, 4 };
    private static final int[] WHIPS = { 0, 0, 0, 2, 5 };

    private BeamPainter() {
    }

    public static void draw(LanternPainter painter, Vec3 from, Vec3 target, double solid, double age, int stage,
            double stageAge, boolean own) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        double full = from.distanceTo(target);
        if (strength <= 0.0 || full < 0.05) {
            return;
        }
        Pass pass = new Pass(painter, from, target, full, strength, age, Mth.clamp(stage, 0, LightBeam.LAST),
                Math.max(0.0, stageAge), own);
        pass.body();
        pass.rings();
        pass.bands();
        pass.crackle();
        pass.whips();
        pass.surge();
        pass.muzzle();
        pass.impact();
    }

    private static final class Pass {
        private final LanternPainter painter;
        private final Vec3 from;
        private final Vec3 to;
        private final Vec3 axis;
        private final Vec3[] across;
        private final double length;
        private final double strength;
        private final double age;
        private final int stage;
        private final double stageAge;
        private final double rise;
        private final double thick;
        private final double fury;
        private final double shaky;
        private final double flicker;
        private final double time;
        private final boolean arrived;
        // Looking down your own beam every ring round it circles your view, so they stay faint there.
        private final double rings;

        Pass(LanternPainter painter, Vec3 from, Vec3 target, double full, double strength, double age, int stage,
                double stageAge, boolean own) {
            this.painter = painter;
            this.rings = own ? 0.4 : 1.0;
            this.from = from;
            this.axis = target.subtract(from).scale(1.0 / full);
            this.across = Vectors.across(this.axis);
            double out = Mth.clamp(age / SHOOT, 0.0, 1.0);
            this.to = from.lerp(target, 1.0 - (1.0 - out) * (1.0 - out));
            this.arrived = out >= 1.0;
            this.length = from.distanceTo(this.to);
            this.strength = strength;
            this.age = age;
            this.stage = stage;
            this.stageAge = stageAge;
            this.rise = (double) stage / LightBeam.LAST;
            this.time = painter.time();
            // A new stage swells out of the one before, overshooting a little.
            double grown = stage == 0 ? 1.0 : Ease.smooth(stageAge / GROW);
            double swell = stage == 0 ? 0.0 : 0.35 * Math.exp(-stageAge / 3.0);
            this.thick = Mth.lerp(grown, LightBeam.STAGE_THICK[Math.max(0, stage - 1)], LightBeam.STAGE_THICK[stage])
                    * (1.0 + swell);
            this.fury = FURY[stage];
            this.shaky = SHAKY[stage];
            this.flicker = 0.9 - 0.1 * this.fury + (0.1 + 0.25 * this.fury)
                    * Math.sin(this.time * (2.7 + 3.0 * this.fury)) * Math.sin(this.time * 1.3 + 1.0);
        }

        private double away(Vec3 at) {
            return this.painter.camera().distanceTo(at);
        }

        // Close to the camera the beam thins, so your own beam never fills your view.
        private double near(double away) {
            return Mth.clamp(away / 1.2, 0.22, 1.0) * this.thick;
        }

        // At the top stages the beam no longer holds its shape: it bulges and pinches all along its length.
        private double wobble(double along) {
            if (this.shaky <= 0.0) {
                return 1.0;
            }
            return 1.0 + this.shaky * (0.2 * Math.sin(along * 0.9 - this.time * 1.1)
                    + 0.12 * Math.sin(along * 2.3 + this.time * 1.7) + 0.1 * Math.sin(this.time * 5.1));
        }

        private Vec3 round(double angle) {
            return this.across[0].scale(Math.cos(angle)).add(this.across[1].scale(Math.sin(angle)));
        }

        private record Point(double near, double surge, double breath, double throb) {
        }

        private Point point(double along) {
            double near = this.near(this.away(this.from.add(this.axis.scale(along)))) * this.wobble(along);
            double run = Mth.frac(along * 0.2 - this.time * (0.45 + 0.3 * this.rise));
            double surge = Math.max(0.0, 1.0 - Math.abs(run - 0.5) * 5.0);
            double breath = 1.0 + 0.25 * Math.sin(along * 1.3 - this.time * 0.9);
            double throb = 0.85 + 0.3 * Math.sin(along * 0.6 - this.time * 2.3);
            return new Point(near, surge, breath, throb);
        }

        // Every layer tapers from one point to the next, so its width runs on smoothly instead of in steps.
        void body() {
            int steps = Mth.clamp((int) (this.length / 0.5), 10, 90);
            int strands = STRANDS[this.stage];
            Vec3[] twists = new Vec3[strands];
            Arrays.fill(twists, this.from);
            Vec3 last = this.from;
            Vec3 jag = this.from;
            int crackle = (int) (this.time * (1.0 + this.fury));
            double halo = 0.8 * (0.55 + 0.45 * this.rise);
            double spin = 0.8 + 0.9 * this.rise;
            Point was = this.point(0.0);
            for (int i = 1; i <= steps && this.length > 0.02; i++) {
                double t = Math.pow((double) i / steps, 1.3);
                Vec3 next = this.from.lerp(this.to, t);
                Point now = this.point(t * this.length);
                double away = this.away(last.add(next).scale(0.5));
                double near = now.near;
                double surge = now.surge;
                boolean seen = away >= NEAR;
                if (seen) {
                    this.painter.lightTaper(last, next, 0.09 * was.near * this.flicker * (1.0 + 0.5 * was.surge),
                            0.09 * near * this.flicker * (1.0 + 0.5 * surge), HOT, this.strength, this.strength);
                    this.painter.lightTaper(last, next, 0.24 * was.near * this.flicker, 0.24 * near * this.flicker,
                            BRIGHT, (0.6 + 0.3 * was.surge) * this.strength, (0.6 + 0.3 * surge) * this.strength);
                    this.painter.glowTaper(last, next, halo * was.near * was.breath, halo * near * now.breath, GREEN,
                            (0.45 + 0.25 * was.surge) * this.strength, (0.45 + 0.25 * surge) * this.strength);
                    double far = Mth.clamp((away - 2.0) / 2.0, 0.0, 1.0);
                    if (this.stage >= 1 && far > 0.0) {
                        double alpha = (0.07 + 0.07 * this.rise) * far * this.strength;
                        this.painter.glowTaper(last, next, 1.5 * was.near * was.breath, 1.5 * near * now.breath, GREEN,
                                alpha, alpha);
                    }
                    if (this.stage >= 3 && far > 0.0) {
                        double alpha = (0.05 + 0.05 * this.rise) * far * this.strength;
                        this.painter.glowTaper(last, next, 2.1 * was.near * was.throb, 2.1 * near * now.throb, GREEN,
                                alpha, alpha);
                    }
                    if (this.stage == LightBeam.LAST && far > 0.0) {
                        double alpha = 0.05 * far * this.strength;
                        this.painter.glowTaper(last, next, 2.8 * was.near * was.throb, 2.8 * near * now.throb, BRIGHT,
                                alpha, alpha);
                    }
                }
                was = now;
                if (this.fury > 0.0) {
                    double swing = 0.35 * near * this.fury;
                    Vec3 wild = next.add(this.across[0].scale((Noise.of(crackle, i, 41) - 0.5) * 2.0 * swing))
                            .add(this.across[1].scale((Noise.of(crackle, i, 42) - 0.5) * 2.0 * swing));
                    if (i > 1 && seen) {
                        this.painter.lightLine(jag, wild, 0.03 * near, HOT, Colors.alpha(this.fury * this.strength));
                    }
                    jag = wild;
                }
                for (int k = 0; k < strands; k++) {
                    double turn = t * this.length * (2.4 - 0.6 * this.rise) + this.time * spin
                            + k * Math.PI * 2.0 / strands;
                    double radius = (0.13 + 0.07 * this.rise) * near * (1.0 + 0.25 * surge);
                    Vec3 strand = next.add(this.round(turn).scale(radius));
                    if (i > 1 && seen) {
                        this.painter.lightLine(twists[k], strand, 0.035 * near, BRIGHT,
                                Colors.alpha((0.5 + 0.2 * this.rise) * this.strength));
                    }
                    twists[k] = strand;
                }
                last = next;
            }
        }

        // Small rings of light running down the beam: none on the thin first stage, ever more and faster after it.
        void rings() {
            if (this.stage == 0) {
                return;
            }
            double spacing = 2.4 * this.thick / (1.0 + 0.4 * this.rise);
            double speed = 0.7 + 0.9 * this.rise;
            for (double d = this.time * speed % spacing; d < this.length; d += spacing) {
                Vec3 at = this.from.add(this.axis.scale(d));
                double away = this.away(at);
                if (away < 1.4) {
                    continue;
                }
                double near = this.near(away) * this.wobble(d);
                double swell = 0.5 + 0.5 * Math.sin(d * 0.9 - this.time * 0.3);
                this.painter.circle(at, this.across[0], this.across[1], (0.26 + 0.08 * swell) * near, 0.03 * near,
                        0.16 * near, Colors.alpha(0.75 * this.strength), Colors.alpha(0.35 * this.strength));
            }
        }

        // Big energy rings round the beam: a few that focus it just ahead of the hand, turning and tipping, and at the
        // top stages wide bands that race down its whole length.
        void bands() {
            for (int k = 0; k < FOCUS[this.stage]; k++) {
                double d = 1.6 + 1.3 * k;
                if (d > this.length - 0.5) {
                    break;
                }
                double spin = this.time * (0.18 + 0.05 * k) * (k % 2 == 0 ? 1.0 : -1.0);
                double radius = (0.55 + 0.22 * k) * this.thick * (1.0 + 0.06 * Math.sin(this.time * 0.7 + k));
                this.band(this.from.add(this.axis.scale(d)), radius, 0.28, spin, 0.18 * this.rise, 0.85);
            }
            int bands = BANDS[this.stage];
            for (int k = 0; k < bands && this.length > 3.0; k++) {
                double d = Mth.frac(this.time * 0.035 + (double) k / bands) * this.length;
                double spin = this.time * 0.3 * (k % 2 == 0 ? 1.0 : -1.0);
                double open = Math.min(1.0, d / 3.0) * Math.min(1.0, (this.length - d) / 3.0);
                this.band(this.from.add(this.axis.scale(d)), 0.95 * this.thick, 0.35, spin, 0.3, open);
            }
        }

        private void band(Vec3 at, double radius, double tilt, double spin, double lean, double shown) {
            double away = this.away(at);
            if (away < 1.4 || shown <= 0.0) {
                return;
            }
            double near = Mth.clamp(away / 1.2, 0.22, 1.0);
            // A ring you look through from close by stays faint too, so it never walls off the view.
            double alpha = shown * this.rings * Mth.clamp((away - 1.0) / (radius * near + 1.5), 0.25, 1.0);
            Vec3 tipped = this.axis.scale(Math.cos(tilt)).add(this.round(spin * 0.5).scale(Math.sin(tilt)));
            Vec3[] plane = Vectors.across(tipped.normalize());
            double width = 0.05 * this.thick * near;
            this.painter.circle(at, plane[0], plane[1], radius * near, width, 5.0 * width,
                    Colors.alpha(0.9 * alpha * this.strength), Colors.alpha(0.45 * alpha * this.strength));
            // Dashes round the ring show it turning.
            for (int j = 0; j < 6; j++) {
                double angle = spin + j * Math.PI / 3.0;
                Vec3 a = at.add(plane[0].scale(Math.cos(angle) * radius * near))
                        .add(plane[1].scale(Math.sin(angle) * radius * near));
                Vec3 b = at.add(plane[0].scale(Math.cos(angle + 0.35) * radius * near))
                        .add(plane[1].scale(Math.sin(angle + 0.35) * radius * near));
                this.painter.lightLine(a, b, 2.2 * width, HOT, Colors.alpha(alpha * this.strength));
            }
            if (lean > 0.0) {
                this.painter.glowDisc(at, radius * near * 1.1, GREEN, 0.12 * lean * alpha * this.strength, 0.1,
                        (int) (this.time / 3.0));
            }
        }

        // Short forks of light crackling off the beam, more the stronger it is.
        void crackle() {
            int count = (int) Math.round(30.0 * this.fury);
            int flick = (int) (this.time / 2.0);
            double arc = this.thick * (1.0 + 1.5 * this.fury);
            for (int k = 0; k < count; k++) {
                Vec3 base = this.from.add(this.axis.scale(Noise.of(flick, k, 21) * this.length));
                if (this.away(base) < 1.4) {
                    continue;
                }
                Vec3 side = this.round(Noise.of(flick, k, 22) * Math.PI * 2.0);
                Vec3 middle = base.add(side.scale(0.25 * arc)).add(this.axis.scale(0.15 * arc));
                Vec3 tip = base.add(side.scale((0.45 + 0.3 * Noise.of(flick, k, 23)) * arc))
                        .subtract(this.axis.scale(0.1 * arc));
                this.painter.edge(base, middle, 0.03 * this.thick, 0.9 * this.strength);
                this.painter.edge(middle, tip, 0.025 * this.thick, 0.7 * this.strength);
            }
        }

        // At the top stages whole bolts whip out of the beam and back into it further along.
        void whips() {
            int count = WHIPS[this.stage];
            int flick = (int) (this.time / 2.0);
            for (int k = 0; k < count && this.length > 4.0; k++) {
                double start = 2.5 + Noise.of(flick, k, 61) * (this.length - 4.5);
                double span = 1.2 + 1.8 * Noise.of(flick, k, 62);
                double reach = (0.45 + 0.55 * Noise.of(flick, k, 63)) * this.thick;
                Vec3 side = this.round(Noise.of(flick, k, 64) * Math.PI * 2.0);
                Vec3 last = this.from.add(this.axis.scale(start));
                if (this.away(last) < 2.0) {
                    continue;
                }
                int kinks = 8;
                for (int j = 1; j <= kinks; j++) {
                    double u = (double) j / kinks;
                    double bow = Math.sin(u * Math.PI);
                    Vec3 jitter = Noise.direction(flick * 7 + k, 65 + j).scale(0.3 * this.thick * bow);
                    Vec3 next = this.from.add(this.axis.scale(start + span * u)).add(side.scale(bow * reach))
                            .add(j == kinks ? Vec3.ZERO : jitter);
                    this.painter.edge(last, next, 0.035 * this.thick, this.strength);
                    this.painter.glowLine(last, next, 0.2 * this.thick, GREEN, Colors.alpha(0.3 * this.strength));
                    last = next;
                }
            }
        }

        // A new stage sends a wave of light racing down the beam and a shock ring out of the hand.
        void surge() {
            if (this.stage == 0 || this.stageAge >= SURGE_TICKS) {
                return;
            }
            double left = 1.0 - this.stageAge / SURGE_TICKS;
            double d = this.stageAge * SURGE_SPEED;
            if (d < this.length) {
                Vec3 a = this.from.add(this.axis.scale(Math.max(0.0, d - 2.0)));
                Vec3 b = this.from.add(this.axis.scale(Math.min(this.length, d + 2.0)));
                double near = this.near(this.away(a.add(b).scale(0.5)));
                this.painter.glowLine(a, b, 2.4 * near, BRIGHT, Colors.alpha(0.6 * left * this.strength));
                this.painter.lightLine(a, b, 0.35 * near, HOT, Colors.alpha(left * this.strength));
            }
            double muzzle = Mth.clamp(this.away(this.from) / 0.5, 0.6, 3.0);
            Vec3 at = this.from.add(this.axis.scale(0.1 * muzzle));
            double radius = (0.15 + this.stageAge * 0.12 * (1.0 + this.rise)) * muzzle * this.thick;
            this.painter.circle(at, this.across[0], this.across[1], radius, 0.02 * muzzle, 0.12 * muzzle,
                    Colors.alpha(left * this.strength), Colors.alpha(0.5 * left * this.strength));
            this.painter.flare(this.from, (0.2 + 0.25 * this.rise) * muzzle * this.thick * left, left * this.strength);
        }

        void muzzle() {
            double muzzle = Mth.clamp(this.away(this.from) / 0.5, 0.6, 3.0);
            this.painter.flare(this.from, (0.06 + 0.04 * this.rise) * muzzle * this.thick, this.strength);
            double lens = this.time * 0.25;
            Vec3 lensA = this.round(lens);
            Vec3 lensB = this.axis.cross(lensA);
            this.painter.circle(this.from.add(this.axis.scale(0.04 * muzzle)), lensA, lensB, 0.05 * muzzle * this.thick,
                    0.008 * muzzle, 0.04 * muzzle, Colors.alpha(0.8 * this.strength),
                    Colors.alpha(0.4 * this.strength));
            // From the third stage on, rings turn round the hand, one way and the other, to hold the beam together.
            for (int k = 0; k < this.stage - 1; k++) {
                double spin = this.time * (0.2 + 0.07 * k) * (k % 2 == 0 ? 1.0 : -1.0);
                Vec3 a = this.round(spin);
                Vec3 b = this.axis.cross(a);
                double radius = (0.07 + 0.035 * k) * muzzle * this.thick;
                this.painter.circle(this.from.add(this.axis.scale((0.06 + 0.03 * k) * muzzle)), a, b, radius,
                        0.007 * muzzle, 0.035 * muzzle, Colors.alpha(0.7 * this.strength),
                        Colors.alpha(0.35 * this.strength));
            }
            if (this.age < BURST) {
                double u = Math.max(0.0, this.age) / BURST;
                double fade = (1.0 - u) * this.strength;
                double burst = Mth.clamp(this.away(this.from) / 1.5, 0.25, 1.2) * this.thick;
                this.painter.flare(this.from, (0.12 + 0.2 * (1.0 - u)) * muzzle * this.thick, fade);
                double wide = (0.1 + 0.6 * (1.0 - (1.0 - u) * (1.0 - u) * (1.0 - u))) * burst;
                this.painter.circle(this.from.add(this.axis.scale(0.05 * muzzle)), this.across[0], this.across[1], wide,
                        0.02 * burst, 0.12 * burst, Colors.alpha(fade), Colors.alpha(0.5 * fade));
            }
        }

        void impact() {
            if (!this.arrived) {
                this.painter.flare(this.to, 0.25 * this.thick, this.strength);
                return;
            }
            double big = 1.0 + 0.6 * this.rise;
            double splash = 0.42 * this.thick * big * (0.85 + 0.15 * Math.sin(this.time * 1.9));
            this.painter.flare(this.to, splash, this.strength);
            Vec3 face = this.to.subtract(this.axis.scale(0.05));
            int ripples = 3 + this.stage;
            for (int k = 0; k < ripples; k++) {
                double ripple = Mth.frac(this.time / (9.0 - 3.0 * this.rise) + (double) k / ripples);
                this.painter.circle(face, this.across[0], this.across[1], (0.15 + (1.0 + 0.8 * this.rise) * ripple)
                        * this.thick, 0.03 * this.thick, 0.16 * this.thick,
                        Colors.alpha(0.8 * (1.0 - ripple) * this.strength),
                        Colors.alpha(0.4 * (1.0 - ripple) * this.strength));
            }
            if (this.stage >= 2) {
                this.painter.glowDisc(face, (0.8 + 0.6 * this.rise) * this.thick, GREEN, 0.3 * this.strength, 0.25,
                        (int) (this.time / 2.0));
            }
            int sparks = 8 + 6 * this.stage;
            for (int k = 0; k < sparks; k++) {
                double phase = this.time / (6.0 - 2.0 * this.rise) + Noise.of(k, 31, 0);
                int round = (int) Math.floor(phase);
                double cycle = phase - round;
                Vec3 way = Noise.direction(k, 31 + round);
                way = way.subtract(this.axis.scale(1.4 + way.dot(this.axis))).normalize();
                double reach = (0.2 + (1.3 + this.rise) * cycle) * this.thick;
                Vec3 head = face.add(way.scale(reach)).add(0.0, -0.3 * cycle * cycle * this.thick, 0.0);
                this.painter.edge(head.subtract(way.scale(0.3 * this.thick)), head, 0.03 * this.thick,
                        (1.0 - cycle) * this.strength);
            }
            if (this.stage == LightBeam.LAST) {
                double pulse = Mth.frac(this.time / 7.0);
                this.painter.circle(face, this.across[0], this.across[1], (0.5 + 3.0 * pulse) * this.thick,
                        0.05 * this.thick, 0.3 * this.thick, Colors.alpha(0.9 * (1.0 - pulse) * this.strength),
                        Colors.alpha(0.5 * (1.0 - pulse) * this.strength));
                this.painter.flare(face, splash * (1.6 + 0.4 * Math.sin(this.time * 3.7)), 0.7 * this.strength);
            }
        }
    }
}
