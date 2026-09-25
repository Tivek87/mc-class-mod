package nl.tivek.multiversepowers.character.greenlantern.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * The light Green Lantern's ring pours out, under his painter (see {@link LanternPainter}): the beam from the ring
 * that feeds each construct ({@link #beam}) and the beam of light of the attack button ({@link #beamOfLight}), in the
 * colours of his light. Light, not constructs.
 */
abstract class LanternBeams extends ConstructPainter {
    // The ring's green, the bright green of the edges, and the almost white heart of fresh light.
    public static final int GREEN = 0x3CE86A;
    public static final int BRIGHT = 0x6CFF8E;
    public static final int HOT = 0xE4FFEA;
    // The beam from the ring is drawn in this many pieces, and a piece this close to the camera is left
    // out because it cannot be turned to face it.
    private static final int BEAM_STEPS = 16;
    private static final double BEAM_NEAR = 0.25;
    // How wide its bright core and its glow are where it meets a construct of scale 1, in blocks, and from
    // how far away from the camera on it is drawn at its full width.
    private static final double BEAM_CORE = 0.05;
    private static final double BEAM_HALO = 0.19;
    private static final double BEAM_FULL = 1.6;
    // How long the beam of the attack button takes to shoot out of the ring, and how long the flash lasts as it breaks
    // loose, in ticks.
    private static final double BEAM_SHOOT = 2.5;
    private static final double BEAM_BURST = 6.0;

    /** See the constructors of {@link LanternPainter}. */
    LanternBeams(PoseStack pose, Vec3 camera, float time, Material material) {
        super(pose, camera, time, material);
    }

    LanternBeams(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material) {
        super(pose, camera, time, frustum, material);
    }

    LanternBeams(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material,
            boolean hand) {
        super(pose, camera, time, frustum, material, hand);
    }

    /**
     * The beam of light from the ring to what it made: a white-hot thread leaving the stone with a spark of light
     * on it, swelling to a cord of green light where it meets the construct, with light running down it into the
     * construct and two fine strands winding around it. It stays as long as the construct does: the light always
     * hangs on the ring that wills it.
     */
    public void beam(Vec3 ring, Vec3 end, double solid, double scale) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        double length = ring.distanceTo(end);
        if (strength <= 0.0 || length < 1.0E-3) {
            return;
        }
        // How wide it is where it meets the construct: a cord that suits what hangs on it, never a bar.
        double wide = Mth.clamp(scale, 0.4, 2.0);
        Vec3 axis = end.subtract(ring).scale(1.0 / length);
        Vec3[] across = Vectors.across(axis);
        // Cut into short pieces, closer together near the ring. One long piece is turned to face the camera from
        // its middle only, so a beam that starts at your own hand and runs far out would end up edge-on near you
        // and seem to stop in mid-air.
        Vec3 last = ring;
        Vec3 lastA = ring;
        Vec3 lastB = ring;
        for (int i = 1; i <= BEAM_STEPS; i++) {
            double t = Math.pow((double) i / BEAM_STEPS, 1.5);
            Vec3 next = ring.lerp(end, t);
            Vec3 middle = last.add(next).scale(0.5);
            double away = this.camera().distanceTo(middle);
            // Thin where it leaves the stone, full where it meets the construct, and never fat on screen: a piece
            // right in front of your eye is drawn as thin as it looks from far off.
            double near = Mth.clamp(away / BEAM_FULL, 0.18, 1.0);
            double thick = wide * (0.25 + 0.75 * t) * near;
            // Light runs down the beam into the construct.
            double run = Mth.frac(this.time() * 0.05 - t * 0.9);
            double pulse = Math.max(0.0, 1.0 - Math.abs(run - 0.5) * 6.0);
            // Two fine strands winding around it.
            double turn = length * t * 3.0 - this.time() * 0.35;
            double radius = BEAM_HALO * thick * 0.55;
            Vec3 a = next.add(across[0].scale(Math.cos(turn) * radius)).add(across[1].scale(Math.sin(turn) * radius));
            Vec3 b = next.subtract(across[0].scale(Math.cos(turn) * radius))
                    .subtract(across[1].scale(Math.sin(turn) * radius));
            // A piece almost on top of the camera cannot be turned towards it at all: skip it.
            if (away >= BEAM_NEAR) {
                this.lightLine(last, next, BEAM_CORE * thick * (1.0 + 0.8 * pulse), HOT,
                        Colors.alpha(strength));
                this.lightLine(last, next, BEAM_CORE * 2.2 * thick, BRIGHT, Colors.alpha(0.55 * strength));
                this.glowLine(last, next, BEAM_HALO * thick * (1.0 + 0.5 * pulse), GREEN,
                        Colors.alpha((0.4 + 0.3 * pulse) * strength));
                if (i > 1) {
                    this.lightLine(lastA, a, BEAM_CORE * 0.6 * thick, BRIGHT, Colors.alpha(0.6 * strength));
                    this.lightLine(lastB, b, BEAM_CORE * 0.6 * thick, BRIGHT, Colors.alpha(0.6 * strength));
                }
            }
            last = next;
            lastA = a;
            lastB = b;
        }
        this.flare(ring, 0.03 * Mth.clamp(this.camera().distanceTo(ring) / 0.5, 0.6, 3.0), strength);
    }

    /**
     * The beam that pours out of the ring while the attack button is held. It shoots out of the ring in a blink, with
     * a flash and a ring of light bursting out of the fist, and then roars on: a white-hot core in a cord of bright
     * light and a thick, breathing glow of green, surges of light racing along it away from the ring, three strands
     * winding round it, rings of light running down it and sparks of it crackling off its sides. At the ring a lens of
     * light turns; where it strikes it splashes: a hot flare, ripples running out and sparks spraying back.
     *
     * @param solid how far it is there, 0 to 1 (it dies down when he lets go)
     * @param age   ticks since it broke loose
     * @param thick how thick it is: 1 for the beam of the attack button, more for the air strike's pillar of light
     */
    public void beamOfLight(Vec3 from, Vec3 target, double solid, double age, double thick) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        double full = from.distanceTo(target);
        if (strength <= 0.0 || full < 0.05) {
            return;
        }
        Vec3 axis = target.subtract(from).scale(1.0 / full);
        Vec3[] across = Vectors.across(axis);
        // It shoots out of the ring in a blink.
        double out = Mth.clamp(age / BEAM_SHOOT, 0.0, 1.0);
        Vec3 to = from.lerp(target, 1.0 - (1.0 - out) * (1.0 - out));
        double length = from.distanceTo(to);
        double flicker = 0.9 + 0.1 * Math.sin(this.time() * 2.7) * Math.sin(this.time() * 1.3 + 1.0);
        int steps = Mth.clamp((int) (length / 0.5), 10, 90);
        Vec3 last = from;
        Vec3[] strands = { from, from, from };
        for (int i = 1; i <= steps && length > 0.02; i++) {
            double t = Math.pow((double) i / steps, 1.3);
            Vec3 next = from.lerp(to, t);
            double away = this.camera().distanceTo(last.add(next).scale(0.5));
            // Thin where it is right in front of your eye, so your own beam never fills your screen.
            double near = Mth.clamp(away / 1.2, 0.22, 1.0) * thick;
            double along = t * length;
            // Surges of light racing along it, and its glow breathing in and out along its length.
            double run = Mth.frac(along * 0.2 - this.time() * 0.45);
            double surge = Math.max(0.0, 1.0 - Math.abs(run - 0.5) * 5.0);
            double breath = 1.0 + 0.25 * Math.sin(along * 1.3 - this.time() * 0.9);
            if (away >= BEAM_NEAR) {
                this.lightLine(last, next, 0.09 * near * flicker * (1.0 + 0.5 * surge), HOT, Colors.alpha(strength));
                this.lightLine(last, next, 0.24 * near * flicker, BRIGHT, Colors.alpha((0.6 + 0.3 * surge) * strength));
                this.glowLine(last, next, 0.8 * near * breath, GREEN, Colors.alpha((0.45 + 0.25 * surge) * strength));
                if (away > 2.5) {
                    this.glowLine(last, next, 1.7 * near * breath, GREEN, Colors.alpha(0.14 * strength));
                }
            }
            // Three strands winding round it.
            for (int k = 0; k < 3; k++) {
                double turn = along * 2.4 + this.time() * 0.8 + k * Math.PI * 2.0 / 3.0;
                double radius = 0.15 * near * (1.0 + 0.2 * surge);
                Vec3 strand = next.add(across[0].scale(Math.cos(turn) * radius))
                        .add(across[1].scale(Math.sin(turn) * radius));
                if (i > 1 && away >= BEAM_NEAR) {
                    this.lightLine(strands[k], strand, 0.035 * near, BRIGHT, Colors.alpha(0.6 * strength));
                }
                strands[k] = strand;
            }
            last = next;
        }
        // Rings of light running down it, away from the ring; none right in front of your eye.
        double spacing = 2.4 * thick;
        for (double d = this.time() * 0.7 % spacing; d < length; d += spacing) {
            Vec3 at = from.add(axis.scale(d));
            double away = this.camera().distanceTo(at);
            if (away < 1.4) {
                continue;
            }
            double near = Mth.clamp(away / 1.2, 0.22, 1.0) * thick;
            double swell = 0.5 + 0.5 * Math.sin(d * 0.9 - this.time() * 0.3);
            this.circle(at, across[0], across[1], (0.26 + 0.08 * swell) * near, 0.03 * near, 0.16 * near,
                    Colors.alpha(0.75 * strength), Colors.alpha(0.35 * strength));
        }
        // Sparks of it crackling off its sides, a new few every other tick.
        int flick = (int) (this.time() / 2.0);
        for (int k = 0; k < 5; k++) {
            Vec3 base = from.add(axis.scale(Noise.of(flick, k, 21) * length));
            if (this.camera().distanceTo(base) < 1.4) {
                continue;
            }
            double angle = Noise.of(flick, k, 22) * Math.PI * 2.0;
            Vec3 side = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle)));
            Vec3 middle = base.add(side.scale(0.25 * thick)).add(axis.scale(0.15 * thick));
            Vec3 tip = base.add(side.scale((0.45 + 0.3 * Noise.of(flick, k, 23)) * thick))
                    .subtract(axis.scale(0.1 * thick));
            this.edge(base, middle, 0.03 * thick, 0.9 * strength);
            this.edge(middle, tip, 0.025 * thick, 0.7 * strength);
        }
        // At the ring a lens of light turns.
        double muzzle = Mth.clamp(this.camera().distanceTo(from) / 0.5, 0.6, 3.0);
        this.flare(from, 0.06 * muzzle * thick, strength);
        double lens = this.time() * 0.25;
        Vec3 lensA = across[0].scale(Math.cos(lens)).add(across[1].scale(Math.sin(lens)));
        Vec3 lensB = axis.cross(lensA);
        this.circle(from.add(axis.scale(0.04 * muzzle)), lensA, lensB, 0.05 * muzzle * thick, 0.008 * muzzle,
                0.04 * muzzle, Colors.alpha(0.8 * strength), Colors.alpha(0.4 * strength));
        // It broke loose just now: a flash and a ring of light bursting out of the fist, never so big that it fills
        // your own screen.
        if (age < BEAM_BURST) {
            double u = Math.max(0.0, age) / BEAM_BURST;
            double fade = (1.0 - u) * strength;
            double burst = Mth.clamp(this.camera().distanceTo(from) / 1.5, 0.25, 1.2) * thick;
            this.flare(from, (0.12 + 0.2 * (1.0 - u)) * muzzle * thick, fade);
            double wide = (0.1 + 0.6 * (1.0 - (1.0 - u) * (1.0 - u) * (1.0 - u))) * burst;
            this.circle(from.add(axis.scale(0.05 * muzzle)), across[0], across[1], wide, 0.02 * burst,
                    0.12 * burst, Colors.alpha(fade), Colors.alpha(0.5 * fade));
        }
        if (out < 1.0) {
            // The head of the beam on its way out.
            this.flare(to, 0.25 * thick, strength);
            return;
        }
        // Where it strikes: a hot flare, ripples running out and sparks spraying back.
        double splash = 0.42 * thick * (0.85 + 0.15 * Math.sin(this.time() * 1.9));
        this.flare(to, splash, strength);
        Vec3 face = to.subtract(axis.scale(0.05));
        for (int k = 0; k < 3; k++) {
            double ripple = Mth.frac(this.time() / 9.0 + k / 3.0);
            this.circle(face, across[0], across[1], (0.15 + 1.0 * ripple) * thick, 0.03 * thick, 0.16 * thick,
                    Colors.alpha(0.8 * (1.0 - ripple) * strength), Colors.alpha(0.4 * (1.0 - ripple) * strength));
        }
        for (int k = 0; k < 8; k++) {
            double phase = this.time() / 6.0 + Noise.of(k, 31, 0);
            int round = (int) Math.floor(phase);
            double cycle = phase - round;
            // Back towards the ring, spread out, and dropping as they go.
            Vec3 way = Noise.direction(k, 31 + round);
            way = way.subtract(axis.scale(1.4 + way.dot(axis))).normalize();
            Vec3 head = face.add(way.scale((0.2 + 1.3 * cycle) * thick)).add(0.0, -0.3 * cycle * cycle * thick, 0.0);
            this.edge(head.subtract(way.scale(0.3 * thick)), head, 0.03 * thick, 1.0 - cycle);
        }
    }
}
