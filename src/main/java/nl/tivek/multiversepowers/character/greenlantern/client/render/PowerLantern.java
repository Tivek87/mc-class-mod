package nl.tivek.multiversepowers.character.greenlantern.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.body.suit.Ring;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import org.joml.Matrix4f;

// The Power Battery: the green lantern with a gold ring on top, gold and stone bands at both ends, ribbed green glass
// above and below its body, two gold hoops round its middle and the emblem on its front and back. Built in the
// reference's own pixels (y up from its foot to the ring's top, which is where it is held) and scaled to model pixels.
// Its glass lights up from inside as it charges.
public final class PowerLantern {
    // Model pixels per reference pixel.
    private static final double SIZE = 0.0125;
    private static final int GOLD = 0xD8B04C;
    private static final int STONE = 0x9C9C96;
    private static final int GLASS = 0x1C5C30;
    private static final int RIB = 0x2A7A42;
    private static final int DISC = 0x1E6436;
    private static final int GROOVE = 0x2E8048;
    private static final int SIGN = 0x103A1E;
    private static final int LIT = 0x5CFF8E;
    private static final int BRIGHT = 0x9CFFB8;
    private static final int HOT = 0xF0FFF4;
    private static final double EMBLEM_Y = -675.0;
    private static final double EMBLEM_FRONT = 176.0;
    // Where the emblem's middle is, in model pixels: the ring is pressed to it to charge.
    public static final Vec3 EMBLEM = new Vec3(0.0, EMBLEM_Y * SIZE, EMBLEM_FRONT * SIZE);

    private record Part(Mesh mesh, int rgb, boolean glass) {
    }

    private static final List<Part> PARTS = build();
    private static final Mesh GLOW = Mesh.lathe(16, 1.0, 0.0, -930.0, 150.0, -930.0, 160.0, -680.0, 140.0, -430.0,
            0.0, -430.0).scaled(SIZE, SIZE, SIZE);

    private PowerLantern() {
    }

    private static List<Part> build() {
        List<Part> parts = new ArrayList<>();
        // The ring on top and the egg-shaped knob it hangs from.
        parts.add(new Part(Mesh.torus(32, 8, 140.0, 11.0, 1.0).alongZ().moved(0.0, -135.0, 0.0), GOLD, false));
        parts.add(new Part(Mesh.ball(14, 10, 1.0, 1.0).scaled(45.0, 52.0, 45.0).moved(0.0, -282.0, 0.0), GOLD,
                false));
        // The bands of the cap and of the foot, top to bottom.
        double[][] bands = { { -347.0, -320.0, 122.0, 0 }, { -400.0, -347.0, 116.0, 1 }, { -425.0, -400.0, 122.0, 0 },
                { -935.0, -915.0, 132.0, 0 }, { -970.0, -935.0, 128.0, 1 }, { -988.0, -970.0, 138.0, 0 },
                { -1013.0, -988.0, 134.0, 1 }, { -1030.0, -1013.0, 142.0, 0 } };
        for (double[] band : bands) {
            parts.add(new Part(Mesh.lathe(28, 1.0, 0.0, band[0], band[2], band[0], band[2], band[1], 0.0, band[1]),
                    band[3] == 0 ? GOLD : STONE, false));
        }
        // The green glass: a ribbed neck, the body swelling a little, a ribbed foot.
        parts.add(new Part(Mesh.lathe(28, 1.0, 0.0, -545.0, 108.0, -545.0, 104.0, -425.0, 0.0, -425.0), GLASS, true));
        parts.add(new Part(Mesh.lathe(28, 1.0, 0.0, -835.0, 116.0, -835.0, 126.0, -805.0, 130.0, -685.0, 124.0,
                -575.0, 110.0, -545.0, 0.0, -545.0), GLASS, true));
        parts.add(new Part(Mesh.lathe(28, 1.0, 0.0, -915.0, 114.0, -915.0, 116.0, -835.0, 0.0, -835.0), GLASS, true));
        for (int k = 0; k < 6; k++) {
            parts.add(new Part(Mesh.torus(28, 5, 107.0 - 0.6 * k, 7.0, 1.0).moved(0.0, -437.0 - 20.0 * k, 0.0), RIB,
                    true));
        }
        for (int k = 0; k < 4; k++) {
            parts.add(new Part(Mesh.torus(28, 5, 116.0, 7.0, 1.0).moved(0.0, -848.0 - 20.0 * k, 0.0), RIB, true));
        }
        // The two gold hoops round the middle, wider to the sides than to the front.
        for (double y : new double[] { -615.0, -700.0 }) {
            parts.add(new Part(Mesh.torus(36, 6, 130.0, 9.0, 1.0).scaled(1.5, 1.0, 1.08).moved(0.0, y, 0.0), GOLD,
                    false));
        }
        for (int side = -1; side <= 1; side += 2) {
            for (Part part : emblem()) {
                Mesh mesh = side > 0 ? part.mesh() : part.mesh().scaled(1.0, 1.0, -1.0);
                parts.add(new Part(mesh, part.rgb(), part.glass()));
            }
        }
        List<Part> scaled = new ArrayList<>();
        for (Part part : parts) {
            scaled.add(new Part(part.mesh().scaled(SIZE, SIZE, SIZE), part.rgb(), part.glass()));
        }
        return scaled;
    }

    // The emblem on the front (+z): a green disc ringed with grooves, gold crescents at its sides, and the gold plate
    // in its middle with the lantern's sign on it.
    private static List<Part> emblem() {
        List<Part> parts = new ArrayList<>();
        double y = EMBLEM_Y;
        parts.add(new Part(Mesh.cylinder(32, 102.0, -22.0, 22.0, 1.0).alongZ().moved(0.0, y, 138.0), DISC, false));
        for (double r : new double[] { 88.0, 72.0, 56.0 }) {
            parts.add(new Part(Mesh.torus(32, 4, r, 3.5, 1.0).alongZ().moved(0.0, y, 160.0), GROOVE, false));
        }
        for (int side = -1; side <= 1; side += 2) {
            List<Vec3> arc = new ArrayList<>();
            for (int k = 0; k <= 10; k++) {
                double a = Math.toRadians(-55.0 + 11.0 * k);
                arc.add(new Vec3(side * Math.cos(a) * 106.0, y + Math.sin(a) * 106.0, 158.0));
            }
            parts.add(new Part(Mesh.tube(false, 6, 8.0, 1.0, arc.toArray(Vec3[]::new)), GOLD, false));
        }
        parts.add(new Part(Mesh.bevel(-42.0, y - 42.0, 152.0, 42.0, y + 42.0, 170.0, 7.0, 1.0), GOLD, false));
        parts.add(new Part(Mesh.torus(24, 5, 19.0, 6.0, 1.0).alongZ().moved(0.0, y, 172.0), SIGN, false));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(new Part(Mesh.box(-32.0, y + side * 31.0 - 4.0, 168.0, 32.0, y + side * 31.0 + 4.0, 175.0, 1.0),
                    SIGN, false));
        }
        return parts;
    }

    public static void draw(PoseStack poseStack, MultiBufferSource buffers, float glow, float burst) {
        draw(poseStack, buffers, glow, burst, 0.0F);
    }

    // `glow` 0 dark to 1 fully charged, past 1 a flash; `burst` a flash at the emblem; `hot` the metal lit up.
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, float glow, float burst, float hot) {
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer solid = buffers.getBuffer(Ring.BAND);
        float heat = Mth.clamp(hot, 0.0F, 1.0F);
        float burn = Mth.clamp(glow, 0.0F, 1.0F);
        float flash = Mth.clamp(glow - 1.0F, 0.0F, 1.0F);
        for (Part part : PARTS) {
            int rgb = part.glass() ? Colors.mix(Colors.mix(part.rgb(), LIT, burn * 0.85F), HOT, flash * 0.7F)
                    : Colors.mix(part.rgb(), BRIGHT, heat);
            mesh(solid, matrix, part.mesh(), rgb, !part.glass() || burn < 0.5F);
        }
        // Everything solid is drawn before the first glow is asked for: asking closes the buffer before it.
        VertexConsumer halo = buffers.getBuffer(Ring.HALO);
        float strength = Math.min(1.5F, 0.15F + 0.85F * burn + flash);
        haze(halo, matrix, 1.12, (int) (70 * strength));
        haze(halo, matrix, 1.45 + 0.5 * flash, (int) (28 * strength));
        float blast = Mth.clamp(burst, 0.0F, 1.0F);
        if (blast > 0.0F) {
            float r = (float) ((40.0 + 90.0 * blast) * SIZE);
            float z = (float) EMBLEM.z;
            float y = (float) EMBLEM.y;
            for (int k = 0; k < 3; k++) {
                float wide = r * (1.0F + 0.8F * k);
                square(halo, matrix, y, z + 0.05F * k, wide, (int) (110 * blast / (1 + k)));
            }
        }
    }

    // Every side of a mesh, lit from above and in front when shaded.
    private static void mesh(VertexConsumer buffer, Matrix4f matrix, Mesh mesh, int rgb, boolean shaded) {
        for (int[] side : mesh.sides) {
            double light = 1.0;
            if (shaded) {
                Vec3 normal = Vec3.ZERO;
                for (int i = 0; i < 4; i++) {
                    Vec3 a = mesh.points[side[i]];
                    Vec3 b = mesh.points[side[(i + 1) % 4]];
                    normal = normal.add((a.y - b.y) * (a.z + b.z), (a.z - b.z) * (a.x + b.x), (a.x - b.x) * (a.y + b.y));
                }
                double length = normal.length();
                double facing = length < 1.0E-9 ? 0.5
                        : Math.abs(normal.x * 0.35 + normal.y * 0.8 + normal.z * 0.48) / length;
                light = 0.55 + 0.45 * facing;
            }
            int red = (int) ((rgb >> 16 & 0xFF) * light);
            int green = (int) ((rgb >> 8 & 0xFF) * light);
            int blue = (int) ((rgb & 0xFF) * light);
            for (int corner : side) {
                Vec3 p = mesh.points[corner];
                buffer.addVertex(matrix, (float) (p.x / 16.0), (float) (p.y / 16.0), (float) (p.z / 16.0))
                        .setColor(red, green, blue, 255);
            }
        }
    }

    // The glass's glow: its shape again, made bigger, in see-through light.
    private static void haze(VertexConsumer buffer, Matrix4f matrix, double grow, int alpha) {
        if (alpha <= 0) {
            return;
        }
        double middle = -680.0 * SIZE;
        for (int[] side : GLOW.sides) {
            for (int corner : side) {
                Vec3 p = GLOW.points[corner];
                buffer.addVertex(matrix, (float) (p.x * grow / 16.0), (float) ((middle + (p.y - middle) * grow) / 16.0),
                        (float) (p.z * grow / 16.0)).setColor(0x3C, 0xE8, 0x6A, Math.min(255, alpha));
            }
        }
    }

    private static void square(VertexConsumer buffer, Matrix4f matrix, float y, float z, float half, int alpha) {
        float[][] corners = { { -half, y - half }, { half, y - half }, { half, y + half }, { -half, y + half } };
        for (float[] c : corners) {
            buffer.addVertex(matrix, c[0] / 16.0F, c[1] / 16.0F, z / 16.0F).setColor(0x3C, 0xE8, 0x6A,
                    Math.min(255, alpha));
        }
    }
}
