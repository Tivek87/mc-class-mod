package nl.tivek.multiversepowers.character.greenlantern.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.character.greenlantern.client.body.Ring;
import nl.tivek.multiversepowers.engine.math.Colors;
import org.joml.Matrix4f;

public final class PowerBattery {
    private static final int SILVER = 0xD4DCE0;
    private static final int STEEL = 0x9AA6AC;
    private static final int DARK = 0x5C666C;
    private static final int GREEN = 0x3CE86A;
    private static final int BRIGHT = 0x9CFFB8;
    private static final int HOT = 0xF0FFF4;

    private static final float[][] METAL = {
            { -2.6F, -0.7F, -0.5F, 2.6F, 0.3F, 0.5F },
            { -2.6F, -3.0F, -0.5F, -1.8F, -0.7F, 0.5F },
            { 1.8F, -3.0F, -0.5F, 2.6F, -0.7F, 0.5F },
            { -0.8F, -3.2F, -0.8F, 0.8F, -2.5F, 0.8F },
            { -1.6F, -3.9F, -1.6F, 1.6F, -3.2F, 1.6F },
            { -2.4F, -4.5F, -2.4F, 2.4F, -3.9F, 2.4F },
            { -3.1F, -5.1F, -3.1F, 3.1F, -4.5F, 3.1F },
            { -3.0F, -9.6F, -3.0F, -2.4F, -5.1F, -2.4F },
            { 2.4F, -9.6F, -3.0F, 3.0F, -5.1F, -2.4F },
            { -3.0F, -9.6F, 2.4F, -2.4F, -5.1F, 3.0F },
            { 2.4F, -9.6F, 2.4F, 3.0F, -5.1F, 3.0F },
            { -3.1F, -10.2F, -3.1F, 3.1F, -9.6F, 3.1F },
            { -2.4F, -11.0F, -2.4F, 2.4F, -10.2F, 2.4F },
            { -3.3F, -11.6F, -3.3F, 3.3F, -11.0F, 3.3F } };
    private static final int[] METAL_COLOR = { SILVER, STEEL, STEEL, SILVER, STEEL, SILVER, DARK, STEEL, STEEL,
            STEEL, STEEL, DARK, STEEL, DARK };
    private static final float[] BODY = { -2.8F, -9.6F, -2.8F, 2.8F, -5.1F, 2.8F };
    private static final float SIGN_Y = -7.35F;
    private static final float SIGN_RADIUS = 1.5F;
    private static final float SIGN_LINE = 0.45F;
    private static final float BOTTOM = 0.55F;
    private static final float SIDE = 0.75F;
    private static final float FRONT = 0.88F;

    private PowerBattery() {
    }

    public static void draw(PoseStack poseStack, MultiBufferSource buffers, float glow, float burst) {
        draw(poseStack, buffers, glow, burst, 0.0F);
    }

    public static void draw(PoseStack poseStack, MultiBufferSource buffers, float glow, float burst, float hot) {
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer solid = buffers.getBuffer(Ring.BAND);
        float heat = Mth.clamp(hot, 0.0F, 1.0F);
        for (int i = 0; i < METAL.length; i++) {
            box(solid, matrix, METAL[i], Colors.mix(METAL_COLOR[i], BRIGHT, heat), true, 255);
        }
        float burn = Mth.clamp(glow, 0.0F, 1.0F);
        float flash = Mth.clamp(glow - 1.0F, 0.0F, 1.0F);
        int light = Colors.mix(Colors.mix(GREEN, BRIGHT, burn * 0.6F), HOT, flash);
        box(solid, matrix, BODY, light, false, 255);
        int sign = Colors.mix(BRIGHT, HOT, 0.5F + 0.5F * burn);
        for (float front : new float[] { BODY[5], -BODY[5] - 0.15F }) {
            sign(solid, matrix, front, sign);
        }
        float blast = Mth.clamp(burst, 0.0F, 1.0F);
        if (blast > 0.0F) {
            float heart = 1.4F + 1.6F * blast;
            box(solid, matrix, new float[] { -heart, SIGN_Y - heart, BODY[2] - 1.2F * blast, heart,
                    SIGN_Y + heart, BODY[2] }, HOT, false, (int) (255 * Math.min(1.0F, blast * 1.6F)));
        }
        // Everything solid is drawn before the first glow is asked for: asking closes the buffer before it.
        VertexConsumer halo = buffers.getBuffer(Ring.HALO);
        float strength = 0.35F + 0.65F * burn + flash;
        box(halo, matrix, new float[] { -3.6F, -10.2F, -3.6F, 3.6F, -4.5F, 3.6F }, GREEN, false,
                (int) (90 * Math.min(1.0F, strength)));
        float wide = 4.6F + 3.0F * flash;
        box(halo, matrix, new float[] { -wide, -11.0F - flash * 2.0F, -wide, wide, -3.7F + flash * 2.0F, wide },
                GREEN, false, (int) (40 * Math.min(1.5F, strength)));
        if (blast > 0.0F) {
            jet(halo, matrix, blast);
        }
    }

    private static void jet(VertexConsumer halo, Matrix4f matrix, float burst) {
        float front = BODY[2];
        for (int i = 0; i < 4; i++) {
            float along = i / 4.0F;
            float half = 2.2F + (4.0F + 10.0F * along) * burst;
            float far = front - (1.0F + 10.0F * along) * burst;
            box(halo, matrix, new float[] { -half, SIGN_Y - half, far - 3.0F * burst, half, SIGN_Y + half, far },
                    GREEN, false, (int) (80 * burst * (1.0F - along * 0.6F)));
        }
    }

    private static void sign(VertexConsumer buffer, Matrix4f matrix, float z, int rgb) {
        float r = SIGN_RADIUS;
        float t = SIGN_LINE;
        float y = SIGN_Y;
        float z1 = z + 0.15F;
        float[][] parts = {
                { -r, y + r - t, z, r, y + r, z1 },
                { -r, y - r, z, r, y - r + t, z1 },
                { -r, y - r, z, -r + t, y + r, z1 },
                { r - t, y - r, z, r, y + r, z1 },
                { -r - 0.6F, y + r + 0.35F, z, r + 0.6F, y + r + 0.35F + t, z1 },
                { -r - 0.6F, y - r - 0.35F - t, z, r + 0.6F, y - r - 0.35F, z1 } };
        for (float[] part : parts) {
            box(buffer, matrix, part, rgb, false, 255);
        }
    }

    private static void box(VertexConsumer buffer, Matrix4f matrix, float[] box, int rgb, boolean shaded, int alpha) {
        float[] x = { box[0] / 16, box[3] / 16 };
        float[] y = { box[1] / 16, box[4] / 16 };
        float[] z = { box[2] / 16, box[5] / 16 };
        int[][] sides = { { 0, 2, 6, 4 }, { 1, 5, 7, 3 }, { 0, 4, 5, 1 }, { 2, 3, 7, 6 }, { 0, 1, 3, 2 },
                { 4, 6, 7, 5 } };
        float[] shade = { SIDE, SIDE, BOTTOM, 1.0F, FRONT, FRONT };
        for (int s = 0; s < sides.length; s++) {
            float light = shaded ? shade[s] : 1.0F;
            int red = (int) ((rgb >> 16 & 0xFF) * light);
            int green = (int) ((rgb >> 8 & 0xFF) * light);
            int blue = (int) ((rgb & 0xFF) * light);
            for (int corner : sides[s]) {
                buffer.addVertex(matrix, x[corner & 1], y[corner >> 1 & 1], z[corner >> 2 & 1])
                        .setColor(red, green, blue, alpha);
            }
        }
    }
}
