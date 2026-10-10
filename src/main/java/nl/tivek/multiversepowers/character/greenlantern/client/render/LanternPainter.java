package nl.tivek.multiversepowers.character.greenlantern.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Vectors;

public class LanternPainter extends LanternBeams {
    public static final Material HARD_LIGHT = new Material(0x4BEF78, 0x6CFF8E, 0x3CE86A, 0xE4FFEA);
    // The mech's deeper, purer green, as in its clip: dark glassy sides under rims of lime light.
    public static final Material MECH_LIGHT = new Material(0x36DA4C, 0x86FF74, 0x2EE646, 0xEAFFE2);
    public static final int MASS_GREEN = 0x4BEF78;
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
    public static final Shape FIST_RING = new Shape(new double[][] { { -0.285, 0.28, 0.36, -0.005, 0.31, 0.44, 1.2 } },
            Mesh.cylinder(10, 0.08, 0.30, 0.335, 1.3).moved(-0.145, 0.0, 0.40),
            Mesh.ball(10, 6, 0.065, 1.6).scaled(1.0, 0.6, 1.0).moved(-0.145, 0.34, 0.40));
    private static final double BOLT_WIDTH = 0.64;
    private static final int RAM_SIDES = 16;
    private static final double[][] RAM = { { -0.45, 0.92 }, { 0.35, 0.8 }, { 1.05, 0.52 }, { 1.55, 0.2 },
            { 1.8, 0.0 } };
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

    public void dome(Vec3 center, double size, double solid, boolean breaking, double flash, Vec3 struck,
            @Nullable Vec3 ring, boolean own) {
        DomePainter.draw(this, center, size, solid, breaking, flash, struck, ring, own);
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
