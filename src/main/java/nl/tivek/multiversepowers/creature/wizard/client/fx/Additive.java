package nl.tivek.multiversepowers.creature.wizard.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;

// Light added onto what is behind it, as the Wizard's spells give off: quads in a colour (darker is fainter), drawn
// from both sides, taking the white middle of the orb's picture so they show as plain colour. MIST is laid over what
// is behind it instead (snow), its colours' fourth number how thick. On top: the shapes his magic is drawn from, flat
// on the ground (rings, stars, tick marks, his circle of runes), a ribbon between two points turned to the eye, and
// round bodies of light: a spindle (a lance, a shard), a tube along a path (a tendril, a thread) and a shell (a ward).
public final class Additive {
    public static final ResourceLocation ORB = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "textures/entity/wizard_orb.png");
    public static final RenderType TYPE = RenderType.eyes(ORB);
    public static final RenderType MIST = RenderType.entityTranslucent(ORB);
    private static final float[] NONE = { 0.0F, 0.0F, 0.0F };

    private Additive() {
    }

    // A quad through four corners (x, y, z each), each with its own colour (r, g, b each, 0 to 1).
    public static void quad(PoseStack.Pose pose, VertexConsumer buffer, float[] a, float[] b, float[] c, float[] d,
            float[] ca, float[] cb, float[] cc, float[] cd) {
        vertex(pose, buffer, a, ca);
        vertex(pose, buffer, b, cb);
        vertex(pose, buffer, c, cc);
        vertex(pose, buffer, d, cd);
        vertex(pose, buffer, d, cd);
        vertex(pose, buffer, c, cc);
        vertex(pose, buffer, b, cb);
        vertex(pose, buffer, a, ca);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float[] at, float[] colour) {
        buffer.addVertex(pose.pose(), at[0], at[1], at[2]).setColor(colour[0], colour[1], colour[2],
                        colour.length > 3 ? colour[3] : 1.0F)
                .setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    // A colour laid over at `thick` (0 to 1), for MIST.
    public static float[] mist(int colour, float thick) {
        return new float[] { (colour >> 16 & 255) / 255.0F, (colour >> 8 & 255) / 255.0F, (colour & 255) / 255.0F,
                thick };
    }

    public static float[] rgb(int colour, float strength) {
        return new float[] { (colour >> 16 & 255) / 255.0F * strength, (colour >> 8 & 255) / 255.0F * strength,
                (colour & 255) / 255.0F * strength };
    }

    // A flat ring at height `y` from `inner` to `outer`, its colour fading out to `fade` at the outer edge.
    public static void ring(PoseStack.Pose pose, VertexConsumer buffer, float inner, float outer, float y, int sides,
            float[] colour, float[] fade) {
        for (int i = 0; i < sides; i++) {
            float a0 = i * Mth.TWO_PI / sides;
            float a1 = (i + 1) * Mth.TWO_PI / sides;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            quad(pose, buffer, new float[] { c0 * inner, y, s0 * inner }, new float[] { c1 * inner, y, s1 * inner },
                    new float[] { c1 * outer, y, s1 * outer }, new float[] { c0 * outer, y, s0 * outer }, colour,
                    colour, fade, fade);
        }
    }

    // A wall standing on a ring of `radius` from `y` up `tall`, its foot in `foot` fading to `top`.
    public static void wall(PoseStack.Pose pose, VertexConsumer buffer, float radius, float y, float tall, int sides,
            float[] foot, float[] top) {
        for (int i = 0; i < sides; i++) {
            float a0 = i * Mth.TWO_PI / sides;
            float a1 = (i + 1) * Mth.TWO_PI / sides;
            float c0 = Mth.cos(a0) * radius;
            float s0 = Mth.sin(a0) * radius;
            float c1 = Mth.cos(a1) * radius;
            float s1 = Mth.sin(a1) * radius;
            quad(pose, buffer, new float[] { c0, y, s0 }, new float[] { c1, y, s1 }, new float[] { c1, y + tall, s1 },
                    new float[] { c0, y + tall, s0 }, foot, foot, top, top);
        }
    }

    // A flat line on the ground from (x0, z0) to (x1, z1), `width` across.
    public static void line(PoseStack.Pose pose, VertexConsumer buffer, float x0, float z0, float x1, float z1,
            float width, float y, float[] colour) {
        float dx = x1 - x0;
        float dz = z1 - z0;
        float length = Mth.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4F) {
            return;
        }
        float nx = -dz / length * width * 0.5F;
        float nz = dx / length * width * 0.5F;
        quad(pose, buffer, new float[] { x0 + nx, y, z0 + nz }, new float[] { x1 + nx, y, z1 + nz },
                new float[] { x1 - nx, y, z1 - nz }, new float[] { x0 - nx, y, z0 - nz }, colour, colour, colour,
                colour);
    }

    // The points of a star of `points` round `radius`, each joined to the one `skip` on.
    public static void star(PoseStack.Pose pose, VertexConsumer buffer, float radius, int points, int skip,
            float turn, float width, float y, float[] colour) {
        for (int i = 0; i < points; i++) {
            float a0 = turn + i * Mth.TWO_PI / points;
            float a1 = turn + (i + skip) * Mth.TWO_PI / points;
            line(pose, buffer, Mth.cos(a0) * radius, Mth.sin(a0) * radius, Mth.cos(a1) * radius,
                    Mth.sin(a1) * radius, width, y, colour);
        }
    }

    // `count` marks round a ring from `radius` inward `length`.
    public static void ticks(PoseStack.Pose pose, VertexConsumer buffer, float radius, float length, int count,
            float turn, float width, float y, float[] colour) {
        for (int i = 0; i < count; i++) {
            float a = turn + i * Mth.TWO_PI / count;
            line(pose, buffer, Mth.cos(a) * radius, Mth.sin(a) * radius, Mth.cos(a) * (radius - length),
                    Mth.sin(a) * (radius - length), width, y, colour);
        }
    }

    // His circle of runes, `radius` across, lit `strength`: two rings turning against each other, a six-pointed star,
    // marks round its rim and a soft glow over it all.
    public static void glyph(PoseStack.Pose pose, VertexConsumer buffer, float radius, float age, int colour,
            float strength) {
        float y = 0.03F;
        float[] bright = rgb(colour, strength);
        float[] white = rgb(0xFFFFFF, strength * 0.55F);
        float[] faint = rgb(colour, strength * 0.3F);
        float w = Math.max(0.05F, radius * 0.035F);
        ring(pose, buffer, radius * 0.96F, radius, y, 48, bright, bright);
        ring(pose, buffer, radius * 0.99F, radius * 1.12F, y, 48, faint, NONE);
        ring(pose, buffer, radius * 0.8F, radius * 0.83F, y + 0.001F, 40, bright, bright);
        ticks(pose, buffer, radius * 0.95F, radius * 0.1F, 24, age * 0.02F, w * 0.7F, y + 0.002F, white);
        star(pose, buffer, radius * 0.8F, 6, 2, -age * 0.035F, w, y + 0.003F, bright);
        star(pose, buffer, radius * 0.45F, 3, 1, age * 0.05F, w * 0.8F, y + 0.004F, white);
        ring(pose, buffer, radius * 0.18F, radius * 0.22F, y + 0.005F, 24, white, white);
        ring(pose, buffer, 0.0F, radius, y - 0.001F, 32, faint, faint);
    }

    // A ribbon of light from `a` to `b` (in the pose's frame), turned to the eye at `eye`, `width` across, each end in
    // its own colour.
    public static void ribbon(PoseStack.Pose pose, VertexConsumer buffer, Vec3 a, Vec3 b, Vec3 eye, float width,
            float[] ca, float[] cb) {
        Vec3 along = b.subtract(a);
        Vec3 side = along.cross(eye.subtract(a));
        if (side.lengthSqr() < 1.0E-8) {
            return;
        }
        side = side.normalize().scale(width * 0.5);
        quad(pose, buffer, point(a.add(side)), point(b.add(side)), point(b.subtract(side)), point(a.subtract(side)),
                ca, cb, cb, ca);
    }

    // A soft glow round `at` turned to the eye, `half` across.
    public static void glow(PoseStack pose, VertexConsumer buffer, Vec3 at, float half, float[] colour,
            org.joml.Quaternionf facing) {
        pose.pushPose();
        pose.translate(at.x, at.y, at.z);
        pose.mulPose(facing);
        PoseStack.Pose last = pose.last();
        float[] none = NONE;
        float[] middle = { 0.0F, 0.0F, 0.0F };
        for (int i = 0; i < 12; i++) {
            float a0 = i * Mth.TWO_PI / 12;
            float a1 = (i + 1) * Mth.TWO_PI / 12;
            quad(last, buffer, middle, middle, new float[] { Mth.cos(a1) * half, Mth.sin(a1) * half, 0.0F },
                    new float[] { Mth.cos(a0) * half, Mth.sin(a0) * half, 0.0F }, colour, colour, none, none);
        }
        pose.popPose();
    }

    // A crystal along +y from 0 to `length`: `sides` faces widening to `radius` at `waist` of its length and closing to
    // a point at each end, the middle colour fading to `tips` at both points.
    public static void spindle(PoseStack.Pose pose, VertexConsumer buffer, float length, float radius, float waist,
            int sides, float[] middle, float[] tips) {
        float[] bottom = { 0.0F, 0.0F, 0.0F };
        float[] top = { 0.0F, length, 0.0F };
        for (int i = 0; i < sides; i++) {
            float a0 = i * Mth.TWO_PI / sides;
            float a1 = (i + 1) * Mth.TWO_PI / sides;
            float[] p0 = { Mth.cos(a0) * radius, length * waist, Mth.sin(a0) * radius };
            float[] p1 = { Mth.cos(a1) * radius, length * waist, Mth.sin(a1) * radius };
            quad(pose, buffer, bottom, p0, p1, bottom, tips, middle, middle, tips);
            quad(pose, buffer, p0, top, top, p1, middle, tips, tips, middle);
        }
    }

    // A tube through `points` (x, y, z each), each ring `radii` across, `sides` round, its colour from `from` at the
    // first point to `to` at the last.
    public static void tube(PoseStack.Pose pose, VertexConsumer buffer, Vec3[] points, float[] radii, int sides,
            float[] from, float[] to) {
        Vec3[][] rings = new Vec3[points.length][];
        Vec3 side = null;
        for (int k = 0; k < points.length; k++) {
            Vec3 along = points[Math.min(points.length - 1, k + 1)].subtract(points[Math.max(0, k - 1)]);
            along = along.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 1.0, 0.0) : along.normalize();
            if (side == null || Math.abs(side.dot(along)) > 0.95) {
                side = Math.abs(along.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            }
            Vec3 u = along.cross(side).normalize();
            Vec3 v = along.cross(u).normalize();
            side = v;
            rings[k] = new Vec3[sides];
            for (int i = 0; i < sides; i++) {
                double a = i * Math.PI * 2.0 / sides;
                rings[k][i] = points[k].add(u.scale(Math.cos(a) * radii[k])).add(v.scale(Math.sin(a) * radii[k]));
            }
        }
        for (int k = 0; k + 1 < points.length; k++) {
            float[] c0 = mix(from, to, k / (float) (points.length - 1));
            float[] c1 = mix(from, to, (k + 1) / (float) (points.length - 1));
            for (int i = 0; i < sides; i++) {
                int j = (i + 1) % sides;
                quad(pose, buffer, point(rings[k][i]), point(rings[k][j]), point(rings[k + 1][j]),
                        point(rings[k + 1][i]), c0, c0, c1, c1);
            }
        }
    }

    // A shell round the middle, `wide` across and `tall` high in its halves, in `rings` bands of `sides` cells, each
    // cell lit by `light` from how its corners face `eye` (the pose's frame): brighter where the shell is seen edge on,
    // as light gathers at a bubble's rim. `gap` shrinks each cell towards its middle (0 for none), `out` pushes it out.
    public static void shell(PoseStack.Pose pose, VertexConsumer buffer, float wide, float tall, int rings, int sides,
            Vec3 eye, CellLight light, float gap, float out) {
        for (int r = 0; r < rings; r++) {
            for (int i = 0; i < sides; i++) {
                float[][] corner = new float[4][];
                float[][] colour = new float[4][];
                int[][] at = { { r, i }, { r, i + 1 }, { r + 1, i + 1 }, { r + 1, i } };
                Vec3 middle = Vec3.ZERO;
                Vec3[] ways = new Vec3[4];
                for (int c = 0; c < 4; c++) {
                    double lat = Math.PI * at[c][0] / rings - Math.PI / 2.0;
                    double lon = Math.PI * 2.0 * at[c][1] / sides;
                    ways[c] = new Vec3(Math.cos(lat) * Math.cos(lon), Math.sin(lat), Math.cos(lat) * Math.sin(lon));
                    middle = middle.add(ways[c].scale(0.25));
                }
                for (int c = 0; c < 4; c++) {
                    Vec3 way = ways[c].add(middle.subtract(ways[c]).scale(gap)).scale(1.0 + out);
                    Vec3 p = new Vec3(way.x * wide, way.y * tall, way.z * wide);
                    Vec3 look = eye.subtract(p);
                    double facing = look.lengthSqr() < 1.0E-6 ? 1.0 : Math.abs(way.normalize().dot(look.normalize()));
                    corner[c] = point(p);
                    colour[c] = light.of(r, i, (float) (1.0 - facing));
                }
                quad(pose, buffer, corner[0], corner[1], corner[2], corner[3], colour[0], colour[1], colour[2],
                        colour[3]);
            }
        }
    }

    // The colour of a shell's cell `ring`, `side` at a corner seen `rim` edge on (0 face on, 1 edge on).
    @FunctionalInterface
    public interface CellLight {
        float[] of(int ring, int side, float rim);
    }

    public static float[] mix(float[] a, float[] b, float t) {
        float[] out = new float[Math.max(a.length, b.length)];
        for (int i = 0; i < out.length; i++) {
            float x = i < a.length ? a[i] : 1.0F;
            float y = i < b.length ? b[i] : 1.0F;
            out[i] = x + (y - x) * t;
        }
        return out;
    }

    private static float[] point(Vec3 at) {
        return new float[] { (float) at.x, (float) at.y, (float) at.z };
    }
}
