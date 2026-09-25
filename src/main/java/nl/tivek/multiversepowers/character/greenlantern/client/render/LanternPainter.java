package nl.tivek.multiversepowers.character.greenlantern.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

public class LanternPainter extends LanternBeams {
    public static final Material HARD_LIGHT = new Material(0x4BEF78, 0x6CFF8E, 0x3CE86A, 0xE4FFEA);
    public static final int MASS_GREEN = 0x4BEF78;
    private static final double MODEL_WIDTH = 1.23;
    private static final int SPECKS = 12;
    private static final double BOLT_BEAM = 6.0;

    private static final double[][] FIST = {
            { -0.52, -0.30, -0.42, 0.52, 0.28, 0.16, 1.0 },
            { -0.46, 0.28, -0.36, -0.30, 0.32, 0.08, 1.05 },
            { -0.20, 0.28, -0.36, -0.06, 0.32, 0.08, 1.05 },
            { 0.06, 0.28, -0.36, 0.20, 0.32, 0.08, 1.05 },
            { 0.31, 0.26, -0.36, 0.45, 0.30, 0.08, 1.05 },
            { -0.53, -0.02, 0.16, -0.29, 0.28, 0.53, 1.0 },
            { -0.53, -0.32, 0.12, -0.29, -0.02, 0.47, 0.92 },
            { -0.51, -0.37, -0.06, -0.31, -0.28, 0.13, 0.85 },
            { -0.27, -0.02, 0.16, -0.02, 0.28, 0.57, 1.0 },
            { -0.27, -0.32, 0.12, -0.02, -0.02, 0.51, 0.92 },
            { -0.25, -0.37, -0.06, -0.04, -0.28, 0.13, 0.85 },
            { 0.00, -0.02, 0.16, 0.25, 0.28, 0.54, 1.0 },
            { 0.00, -0.32, 0.12, 0.25, -0.02, 0.48, 0.92 },
            { 0.02, -0.37, -0.06, 0.23, -0.28, 0.13, 0.85 },
            { 0.27, -0.01, 0.16, 0.49, 0.24, 0.48, 1.0 },
            { 0.27, -0.28, 0.12, 0.49, -0.01, 0.43, 0.92 },
            { 0.29, -0.33, -0.06, 0.47, -0.25, 0.13, 0.85 },
            { -0.50, 0.28, 0.08, -0.32, 0.38, 0.30, 1.1 },
            { -0.24, 0.28, 0.08, -0.05, 0.39, 0.33, 1.1 },
            { 0.03, 0.28, 0.08, 0.22, 0.38, 0.31, 1.1 },
            { 0.30, 0.24, 0.08, 0.46, 0.33, 0.27, 1.1 },
            { -0.72, -0.28, -0.30, -0.52, 0.06, 0.10, 1.0 },
            { -0.74, -0.34, 0.08, -0.50, -0.06, 0.42, 1.0 },
            { -0.64, -0.40, 0.42, -0.08, -0.16, 0.62, 1.05 },
            { -0.30, -0.36, 0.62, -0.10, -0.20, 0.635, 1.25 },
            { -0.38, -0.27, -0.72, 0.38, 0.25, -0.42, 0.9 },
            { -0.44, -0.33, -0.86, 0.44, 0.31, -0.72, 1.2 },
            { -0.34, -0.25, -1.25, 0.34, 0.23, -0.86, 0.6 } };
    private static final double BACK = -1.25;
    public static final Shape FIST_RING = new Shape(new double[][] { { -0.285, 0.28, 0.36, -0.005, 0.31, 0.44, 1.2 } },
            Mesh.cylinder(10, 0.08, 0.30, 0.335, 1.3).moved(-0.145, 0.0, 0.40),
            Mesh.ball(10, 6, 0.065, 1.6).scaled(1.0, 0.6, 1.0).moved(-0.145, 0.34, 0.40));
    private static final double BOLT_WIDTH = 0.64;
    private static final int DOME_RINGS = 12;
    private static final int DOME_SLICES = 24;
    private static final int RAM_SIDES = 16;
    private static final double[][] RAM = { { -0.45, 0.92 }, { 0.35, 0.8 }, { 1.05, 0.52 }, { 1.55, 0.2 },
            { 1.8, 0.0 } };
    private static final Shape SHIELD = Shape.of(
            Mesh.lathe(40, 1.0, 0.0, -0.07, 0.90, -0.07, 0.97, -0.03, 0.97, 0.03, 0.90, 0.05, 0.60, 0.11, 0.30, 0.14,
                    0.0, 0.15).alongZ(),
            Mesh.torus(48, 8, 0.96, 0.06, 1.2).alongZ(),
            Mesh.torus(48, 6, 0.70, 0.025, 1.3).alongZ().moved(0.0, 0.0, 0.095),
            Mesh.torus(32, 6, 0.24, 0.045, 1.45).alongZ().moved(0.0, 0.0, 0.155),
            Mesh.box(-0.30, 0.31, 0.10, 0.30, 0.39, 0.20, 1.45),
            Mesh.box(-0.30, -0.39, 0.10, 0.30, -0.31, 0.20, 1.45),
            Mesh.box(-0.08, -0.30, -0.13, 0.08, 0.30, -0.07, 1.0));
    private static final Shape SHIELD_RIVETS = Shape.of(rivets());
    private static final Shape BOLT_SHAPE = Shape.of(Mesh.lathe(10, 1.15, 0.0, -0.75, 0.18, -0.75, 0.24, -0.55,
            0.32, -0.40, 0.32, 0.20, 0.24, 0.40, 0.10, 0.55, 0.0, 0.60).alongZ());
    private static final double BOLT_NOSE = 0.60;
    private static final double BOLT_TAIL = -0.75;

    public LanternPainter(PoseStack pose, Vec3 camera, float time) {
        super(pose, camera, time, HARD_LIGHT);
    }

    public LanternPainter(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum) {
        super(pose, camera, time, frustum, HARD_LIGHT);
    }

    private LanternPainter(PoseStack pose, float time) {
        super(pose, Vec3.ZERO, time, null, HARD_LIGHT, true);
    }

    public static LanternPainter hand(PoseStack pose, float time) {
        return new LanternPainter(pose, time);
    }

    public void fist(Vec3 center, Vec3 facing, double size, double solid, double charge, boolean held,
            @Nullable Vec3 ring) {
        Vec3 right = facing.cross(Vectors.UP);
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        Frame frame = new Frame(center, right, right.cross(facing), facing, size / MODEL_WIDTH);
        double strength = Mth.clamp(solid, 0.0, 1.0);
        double grown = held ? Mth.clamp(charge, 0.0, 1.0) : 0.0;
        boolean full = grown >= 0.99;
        double throb = full ? 0.85 + 0.25 * Math.sin(this.time() * 0.6) : 1.0;
        this.chargedModel(FIST, frame, strength, throb, grown);
        this.shape(FIST_RING, frame, strength, throb);
        if (ring == null) {
            return;
        }
        Vec3 end = frame.at(0.0, 0.0, BACK);
        this.beam(ring, end, strength, frame.scale());
        if (held && !full) {
            this.feed(frame);
        }
    }

    public void bolt(Vec3 center, Vec3 facing, double size, double solid, @Nullable Vec3 ring) {
        Vec3 right = facing.cross(Vectors.UP);
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        Frame frame = new Frame(center, right, right.cross(facing), facing, size * 0.5 / BOLT_WIDTH);
        double strength = Mth.clamp(solid, 0.0, 1.0);
        this.shape(BOLT_SHAPE, frame, strength, 1.0);
        this.nearFade(true);
        Vec3 nose = frame.at(0.0, 0.0, BOLT_NOSE + 0.15);
        Vec3 tail = frame.at(0.0, 0.0, BOLT_TAIL);
        this.lightLine(tail, nose, size * 0.3, HOT, Colors.alpha(0.9 * strength));
        this.glowLine(tail, nose, size * 1.3, GREEN, Colors.alpha(0.6 * strength));
        Vec3 last = tail;
        for (int i = 1; i <= 4; i++) {
            Vec3 next = tail.subtract(facing.scale(0.35 * i));
            double fade = 1.0 - i / 5.0;
            this.glowLine(last, next, size * (0.9 * fade + 0.2), GREEN, Colors.alpha(0.45 * fade * strength));
            this.lightLine(last, next, size * 0.18 * fade, BRIGHT, Colors.alpha(0.6 * fade * strength));
            last = next;
        }
        this.nearFade(false);
        if (ring != null && ring.distanceToSqr(center) < BOLT_BEAM * BOLT_BEAM) {
            this.beam(ring, frame.at(0.0, 0.0, BOLT_TAIL), strength * 0.7, frame.scale());
        }
    }

    public void shield(Vec3 center, Vec3 facing, double size, double solid, double flash, @Nullable Vec3 ring,
            boolean own) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        if (strength <= 0.0) {
            return;
        }
        Vec3 forward = facing.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : facing.normalize();
        Vec3 right = forward.cross(Vectors.UP);
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(forward);
        double radius = size * 0.5 * (0.55 + 0.45 * strength);
        Frame frame = new Frame(center, right, up, forward, radius);
        double hit = Mth.clamp(flash, 0.0, 1.0);
        double burn = 1.0 + 0.5 * hit;
        Frame rivets = frame.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, this.time() * 0.02);
        if (own) {
            this.seeThrough(SHIELD, frame, (0.1 + 0.12 * hit) * strength, burn);
            this.seeThrough(SHIELD_RIVETS, rivets, (0.1 + 0.12 * hit) * strength, burn);
        } else {
            this.shape(SHIELD, frame, 1.0, burn);
            this.shape(SHIELD_RIVETS, rivets, 1.0, burn);
        }
        Vec3 face = frame.at(0.0, 0.0, 0.17);
        double quiet = own ? 0.5 : 1.0;
        if (hit > 0.0) {
            this.circle(face, right, up, radius * (0.2 + 0.8 * (1.0 - hit)), 0.05, 0.25, Colors.alpha(hit * quiet),
                    Colors.alpha(0.5 * hit * quiet));
        }
        double sweep = Mth.frac(this.time() / 70.0) * 4.0 - 1.2;
        if (Math.abs(sweep) < 1.0) {
            double reach = Math.sqrt(1.0 - sweep * sweep) * 0.85;
            Vec3 across = right.add(up).normalize();
            Vec3 along = right.subtract(up).normalize();
            Vec3 middle = face.add(along.scale(sweep * radius * 0.85));
            this.lightLine(middle.subtract(across.scale(reach * radius)), middle.add(across.scale(reach
                    * radius)), radius * 0.03, HOT, Colors.alpha(0.5 * strength * quiet * (1.0 - Math.abs(sweep))));
        }
        if (ring != null) {
            this.beam(ring, frame.at(0.0, 0.0, -0.13), strength, radius);
        }
    }

    private static Mesh[] rivets() {
        Mesh[] rivets = new Mesh[12];
        for (int i = 0; i < rivets.length; i++) {
            double angle = Math.PI * 2.0 * i / rivets.length;
            rivets[i] = Mesh.ball(6, 4, 0.035, 1.4).moved(0.84 * Math.cos(angle), 0.84 * Math.sin(angle), 0.065);
        }
        return rivets;
    }

    private void feed(Frame frame) {
        for (int k = 0; k < SPECKS; k++) {
            double cycle = this.time() / 14.0 + Noise.of(k, 11, 0);
            int round = Mth.floor(cycle);
            double[] box = FIST[(int) (Noise.of(k, round, 1) * FIST.length)];
            Vec3 target = frame.at(Mth.lerp(Noise.of(k, round, 2), box[0], box[3]),
                    Mth.lerp(Noise.of(k, round, 3), box[1], box[4]), Mth.lerp(Noise.of(k, round, 4), box[2], box[5]));
            this.speck(target, Noise.direction(k, round), frame.forward(), frame.scale() * 0.8, cycle - round,
                    frame.scale());
        }
    }

    private void speck(Vec3 target, Vec3 direction, Vec3 axis, double distance, double t, double scale) {
        double before = Math.max(0.0, t - 0.12);
        Vec3 at = target.add(Vectors.spin(direction, axis, 2.0 * t).scale(distance * (1.0 - t) * (1.0 - t)));
        Vec3 tail = target.add(Vectors.spin(direction, axis, 2.0 * before)
                .scale(distance * (1.0 - before) * (1.0 - before)));
        double fade = Math.min(1.0, t * 4.0);
        this.lightLine(tail, at, 0.025 * scale, BRIGHT, Colors.alpha(0.9 * fade));
        this.glowLine(tail, at, 0.1 * scale, GREEN, Colors.alpha(0.5 * fade));
    }

    public void dome(Vec3 center, double size, double solid, double flash, boolean inside) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        if (strength <= 0.0) {
            return;
        }
        double radius = size * 0.5 * (0.45 + 0.55 * strength);
        double hit = Mth.clamp(flash, 0.0, 1.0);
        boolean under = inside || this.camera().distanceTo(center) < radius;
        double base = under ? 0.04 : 0.1;
        double edge = under ? 0.16 : 0.42;
        Vec3[][] points = new Vec3[DOME_RINGS + 1][DOME_SLICES + 1];
        for (int i = 0; i <= DOME_RINGS; i++) {
            double polar = Math.PI * i / DOME_RINGS;
            for (int j = 0; j <= DOME_SLICES; j++) {
                double around = Math.PI * 2 * j / DOME_SLICES;
                points[i][j] = center.add(Math.sin(polar) * Math.cos(around) * radius, Math.cos(polar) * radius,
                        Math.sin(polar) * Math.sin(around) * radius);
            }
        }
        double ripple = Mth.frac(this.time() * 0.03);
        for (int i = 0; i < DOME_RINGS; i++) {
            for (int j = 0; j < DOME_SLICES; j++) {
                Vec3 p = points[i][j].add(points[i + 1][j + 1]).scale(0.5);
                Vec3 normal = p.subtract(center).normalize();
                Vec3 view = this.camera().subtract(p);
                double facing = view.lengthSqr() < 1.0E-8 ? 1.0 : Math.abs(normal.dot(view.normalize()));
                double rim = (1.0 - facing) * (1.0 - facing);
                double band = Math.max(0.0, 1.0 - Math.abs((double) i / DOME_RINGS - ripple) * 8.0);
                if (!under) {
                    double lit = (0.62 + 0.38 * (normal.y * 0.5 + 0.5)) * (0.9 + 0.2 * band) * (1.0 + 0.3 * hit);
                    this.massQuad(points[i][j], points[i + 1][j], points[i + 1][j + 1], points[i][j + 1],
                            Colors.shade(MASS_GREEN, Math.min(1.0, lit)), 255);
                    continue;
                }
                double a = (base + edge * rim + 0.12 * band + 0.25 * hit) * strength;
                this.lightQuad(points[i][j], points[i + 1][j], points[i + 1][j + 1], points[i][j + 1],
                        Colors.shade(MASS_GREEN, 0.85 + 0.25 * band), Colors.alpha(a));
            }
        }
        int seam = Colors.alpha((0.5 + 0.4 * hit) * strength);
        int seamGlow = Colors.alpha((0.25 + 0.3 * hit) * strength);
        for (int i = 2; i < DOME_RINGS; i += 2) {
            for (int j = 0; j < DOME_SLICES; j++) {
                this.lightLine(points[i][j], points[i][j + 1], 0.025, BRIGHT, seam);
                this.glowLine(points[i][j], points[i][j + 1], 0.12, GREEN, seamGlow);
            }
        }
        for (int band = 0; band < DOME_RINGS; band += 2) {
            for (int j = (band / 2 % 2) * 2; j < DOME_SLICES; j += 4) {
                for (int i = band; i < band + 2; i++) {
                    this.lightLine(points[i][j], points[i + 1][j], 0.025, BRIGHT, seam);
                    this.glowLine(points[i][j], points[i + 1][j], 0.12, GREEN, seamGlow);
                }
            }
        }
        int band = Colors.alpha((0.8 + 0.2 * hit) * strength);
        int bandGlow = Colors.alpha((0.4 + 0.3 * hit) * strength);
        int middle = DOME_RINGS / 2;
        for (int j = 0; j < DOME_SLICES; j++) {
            this.lightLine(points[middle][j], points[middle][j + 1], 0.06, BRIGHT, band);
            this.glowLine(points[middle][j], points[middle][j + 1], 0.25, GREEN, bandGlow);
            this.lightLine(points[1][j], points[1][j + 1], 0.04, BRIGHT, band);
        }
        this.flare(points[0][0], 0.35 * (1.0 + hit), 0.8 * strength);
    }

    public void ram(Vec3 center, Vec3 way, double solid, double flash, boolean own) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        if (strength <= 0.0) {
            return;
        }
        Vec3 forward = way.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : way.normalize();
        Vec3[] across = Vectors.across(forward);
        double grow = 0.4 + 0.6 * strength;
        double hit = Mth.clamp(flash, 0.0, 1.0);
        Vec3[][] rings = new Vec3[RAM.length][RAM_SIDES + 1];
        for (int k = 0; k < RAM.length; k++) {
            Vec3 middle = center.add(forward.scale(RAM[k][0] * grow));
            double radius = RAM[k][1] * grow;
            for (int s = 0; s <= RAM_SIDES; s++) {
                double angle = Math.PI * 2 * s / RAM_SIDES + this.time() * 0.08;
                rings[k][s] = middle.add(across[0].scale(Math.cos(angle) * radius))
                        .add(across[1].scale(Math.sin(angle) * radius));
            }
        }
        double burn = 1.0 + 0.5 * hit;
        this.nearFade(true);
        for (int k = 0; k + 1 < RAM.length; k++) {
            for (int s = 0; s < RAM_SIDES; s++) {
                Vec3 p0 = rings[k][s];
                Vec3 p1 = rings[k + 1][s];
                Vec3 p2 = rings[k + 1][s + 1];
                Vec3 p3 = rings[k][s + 1];
                int rgb = Colors.shade(MASS_GREEN, Math.min(1.0, lit(p0, p1, p2, center.add(forward.scale(RAM[k][0])))
                        * burn * (0.85 + 0.15 * k / RAM.length)));
                if (own) {
                    this.lightQuad(p0, p1, p2, p3, rgb, Colors.alpha(0.16 * strength));
                } else {
                    this.massQuad(p0, p1, p2, p3, rgb, 255);
                }
            }
        }
        double quiet = own ? 0.3 : 1.0;
        for (int s = 0; s < RAM_SIDES; s += 4) {
            for (int k = 0; k + 1 < RAM.length; k++) {
                Vec3 from = rings[k][(s + 2 * k) % RAM_SIDES];
                Vec3 to = rings[k + 1][(s + 2 * k + 2) % RAM_SIDES];
                this.lightLine(from, to, 0.04, BRIGHT, Colors.alpha(EDGE * burn * strength * quiet));
                this.glowLine(from, to, 0.16, GREEN, Colors.alpha(HALO * burn * strength * quiet));
            }
        }
        if (!own) {
            for (int s = 0; s < RAM_SIDES; s++) {
                this.lightLine(rings[1][s], rings[1][s + 1], 0.035, BRIGHT, Colors.alpha(0.7 * burn * strength));
            }
        }
        for (int s = 0; s < RAM_SIDES; s++) {
            this.lightLine(rings[0][s], rings[0][s + 1], 0.05, BRIGHT, Colors.alpha(EDGE * burn * strength * quiet));
            this.glowLine(rings[0][s], rings[0][s + 1], 0.22, GREEN, Colors.alpha(HALO * burn * strength * quiet));
        }
        this.nearFade(false);
        this.flare(rings[RAM.length - 1][0], 0.2 * (1.0 + hit), strength);
    }

    public static double[][] fistModel() {
        return FIST;
    }
}
