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
// on the ground (rings, stars, tick marks, his circle of runes) or a ribbon between two points turned to the eye.
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

    private static float[] point(Vec3 at) {
        return new float[] { (float) at.x, (float) at.y, (float) at.z };
    }
}
