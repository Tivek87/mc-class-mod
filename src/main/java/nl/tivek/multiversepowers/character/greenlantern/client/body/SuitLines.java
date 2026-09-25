package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.math.Colors;
import org.joml.Matrix4f;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitGlow.ALL;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitGlow.BRIGHT;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitGlow.FRONT;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitGlow.GREEN;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitGlow.LIFT;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitGlow.SUIT;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitGlow.vertex;

final class SuitLines {
    private static final float FLOW = 1.8F;
    private static final float GAP = 6.0F;
    private static final float[][] NORMALS = { { -1, 0, 0 }, { 1, 0, 0 }, { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 },
            { 0, 0, 1 } };

    private SuitLines() {
    }

    // line format: {side, start, x0,y0,z0, x1,y1,z1, ...}; side 0=-x 1=+x 4=-z 5=+z
    static final float[][] CHEST_MAIN = {
            { 4, 1.8F, -2.1F, 2.2F, -2, -3.0F, 1.3F, -2, -3.0F, 0.2F, -2 },
            { 4, 1.8F, -2.2F, 3.1F, -2, -3.6F, 1.7F, -2, -3.6F, 0.2F, -2 },
            { 4, 2.0F, -2.0F, 1.6F, -2, -2.4F, 1.2F, -2, -2.4F, 0.2F, -2 } };

    static final float[][] CHEST = {
            { 4, 1.8F, 1.1F, 2.2F, -2, 2.2F, 1.1F, -2, 2.2F, 0.2F, -2 },
            { 4, 1.8F, 1.2F, 3.1F, -2, 2.9F, 1.4F, -2, 2.9F, 0.2F, -2 },
            { 4, 2.4F, -1.4F, 5.4F, -2, -1.4F, 8.0F, -2, -2.2F, 8.8F, -2, -2.2F, 11.8F, -2 },
            { 4, 2.4F, -0.8F, 5.4F, -2, -0.8F, 9.0F, -2, -1.2F, 9.4F, -2, -1.2F, 11.8F, -2 },
            { 4, 2.4F, 0.2F, 5.4F, -2, 0.2F, 9.0F, -2, 0.8F, 9.6F, -2, 0.8F, 11.8F, -2 },
            { 4, 2.4F, 0.8F, 5.4F, -2, 0.8F, 8.0F, -2, 2.2F, 9.4F, -2, 2.2F, 11.8F, -2 },
            { 4, 4.0F, -1.4F, 7.0F, -2, -3.8F, 7.0F, -2 },
            { 4, 4.0F, 0.8F, 7.0F, -2, 3.8F, 7.0F, -2 },
            { 4, 6.5F, -2.2F, 10.2F, -2, -3.8F, 10.2F, -2 },
            { 4, 6.5F, 2.2F, 10.2F, -2, 3.8F, 10.2F, -2 } };

    static final float[][] BACK_MAIN = {
            { 5, 3.0F, -0.8F, 3.0F, 2, -2.4F, 1.4F, 2, -2.4F, 0.2F, 2 },
            { 5, 3.0F, -1.0F, 3.8F, 2, -3.2F, 1.6F, 2, -3.2F, 0.2F, 2 },
            { 5, 3.0F, -0.6F, 2.4F, 2, -1.6F, 1.4F, 2, -1.6F, 0.2F, 2 } };

    static final float[][] BACK = {
            { 5, 3.0F, 0.8F, 3.0F, 2, 2.4F, 1.4F, 2, 2.4F, 0.2F, 2 },
            { 5, 3.0F, 1.0F, 3.8F, 2, 3.2F, 1.6F, 2, 3.2F, 0.2F, 2 },
            { 5, 3.0F, 0.0F, 4.4F, 2, 0.0F, 11.8F, 2 },
            { 5, 3.0F, -0.6F, 4.4F, 2, -0.6F, 8.0F, 2, -2.0F, 9.4F, 2, -2.0F, 11.8F, 2 },
            { 5, 3.0F, 0.6F, 4.4F, 2, 0.6F, 8.0F, 2, 2.0F, 9.4F, 2, 2.0F, 11.8F, 2 },
            { 5, 5.0F, 0.0F, 6.5F, 2, -3.6F, 6.5F, 2 },
            { 5, 5.0F, 0.0F, 6.5F, 2, 3.6F, 6.5F, 2 } };

    static final float[][] FLANKS = {
            { 0, 4.5F, -4, 0.6F, -0.8F, -4, 11.6F, -0.8F },
            { 0, 4.5F, -4, 0.6F, 0.8F, -4, 11.6F, 0.8F },
            { 1, 4.5F, 4, 0.6F, -0.8F, 4, 11.6F, -0.8F },
            { 1, 4.5F, 4, 0.6F, 0.8F, 4, 11.6F, 0.8F } };

    static float[][] rightArmMain(boolean slim) {
        float out = slim ? -2.0F : -3.0F;
        return new float[][] { { 0, 5.0F, out, -1.8F, -1.3F, out, 7.0F, -1.3F, out, 8.5F, -0.6F },
                { 0, 5.0F, out, -1.8F, -0.5F, out, 8.5F, -0.5F },
                { 0, 5.0F, out, -1.8F, 0.3F, out, 7.0F, 0.3F, out, 8.5F, -0.4F } };
    }

    static float[][] rightArm(boolean slim) {
        float out = slim ? -2.0F : -3.0F;
        return new float[][] { { 4, 5.0F, out + 1.0F, -1.8F, -2, out + 1.0F, 9.6F, -2 },
                { 4, 5.0F, out + 2.8F, -1.8F, -2, out + 2.8F, 9.6F, -2 },
                { 5, 5.0F, out + 1.0F, -1.8F, 2, out + 1.0F, 9.6F, 2 },
                { 5, 5.0F, out + 2.8F, -1.8F, 2, out + 2.8F, 9.6F, 2 },
                { 1, 5.0F, 1, -1.8F, 0.0F, 1, 9.6F, 0.0F } };
    }

    static float[][] leftArm(boolean slim) {
        float out = slim ? 2.0F : 3.0F;
        float middle = (out - 1.0F) * 0.5F;
        return new float[][] { { 1, 4.5F, out, -1.8F, -0.8F, out, 9.6F, -0.8F },
                { 1, 4.5F, out, -1.8F, 0.8F, out, 9.6F, 0.8F },
                { 4, 4.5F, middle, -1.8F, -2, middle, 9.6F, -2 },
                { 5, 4.5F, middle, -1.8F, 2, middle, 9.6F, 2 },
                { 0, 4.5F, -1, -1.8F, 0.0F, -1, 9.6F, 0.0F } };
    }

    static final float[][] LEG = {
            { 4, 9.5F, -0.9F, 0.3F, -2, -0.9F, 11.7F, -2 },
            { 4, 9.5F, 0.9F, 0.3F, -2, 0.9F, 11.7F, -2 },
            { 5, 9.5F, -0.9F, 0.3F, 2, -0.9F, 11.7F, 2 },
            { 5, 9.5F, 0.9F, 0.3F, 2, 0.9F, 11.7F, 2 },
            { 0, 9.5F, -2, 0.3F, 0.0F, -2, 11.7F, 0.0F },
            { 1, 9.5F, 2, 0.3F, 0.0F, 2, 11.7F, 0.0F } };

    static final float[][] HEAD_LINES = {
            { 5, 6.0F, -1.2F, -0.2F, 4, -1.2F, -4.0F, 4 },
            { 5, 6.0F, 1.2F, -0.2F, 4, 1.2F, -4.0F, 4 },
            { 0, 6.0F, -4, -0.2F, 1.5F, -4, -3.5F, 1.5F },
            { 1, 6.0F, 4, -0.2F, 1.5F, 4, -3.5F, 1.5F } };

    static void part(PoseStack pose, VertexConsumer light, ModelPart part, float glow, float time,
            float[][] lines, float width, float flow, float reach) {
        if (!part.visible || glow <= 0.01F) {
            return;
        }
        pose.pushPose();
        part.translateAndRotate(pose);
        lines(light, pose.last().pose(), lines, glow, time, width, flow, reach);
        pose.popPose();
    }

    static void lines(VertexConsumer light, Matrix4f matrix, float[][] lines, float glow, float time,
            float width, float flow, float reach) {
        for (float[] line : lines) {
            if (line[1] < reach) {
                trace(light, matrix, line, glow, time, width, flow, reach);
            }
        }
    }

    private static void trace(VertexConsumer light, Matrix4f matrix, float[] line, float glow, float time,
            float width, float flow, float reach) {
        int face = (int) line[0];
        float[] normal = NORMALS[face];
        List<float[]> points = new ArrayList<>();
        for (int i = 2; i + 5 < line.length; i += 3) {
            float x0 = line[i];
            float y0 = line[i + 1];
            float z0 = line[i + 2];
            float x1 = line[i + 3];
            float y1 = line[i + 4];
            float z1 = line[i + 5];
            float length = Mth.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
            int steps = Math.max(1, (int) Math.ceil(length / 0.8F));
            for (int s = 0; s < steps; s++) {
                float a = (float) s / steps;
                points.add(lifted(normal, Mth.lerp(a, x0, x1), Mth.lerp(a, y0, y1), Mth.lerp(a, z0, z1)));
            }
        }
        int last = line.length - 3;
        points.add(lifted(normal, line[last], line[last + 1], line[last + 2]));
        run(light, matrix, points.toArray(new float[0][]), line[1], glow, time, width, flow, reach);
    }

    private static float[] lifted(float[] normal, float x, float y, float z) {
        float push = SUIT + LIFT;
        return new float[] { x + normal[0] * push, y + normal[1] * push, z + normal[2] * push, normal[0], normal[1],
                normal[2] };
    }

    private static void run(VertexConsumer light, Matrix4f matrix, float[][] points, float start, float glow,
            float time, float width, float flow, float reach) {
        float along = start;
        for (int i = 0; i + 1 < points.length && along < reach; i++) {
            float[] a = points[i];
            float[] b = points[i + 1];
            float dx = b[0] - a[0];
            float dy = b[1] - a[1];
            float dz = b[2] - a[2];
            float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1.0E-4F) {
                continue;
            }
            float pulse = pulse(along + length * 0.5F, time, flow);
            float front = reach == ALL ? 0.0F : Mth.clamp(1.0F - (reach - along - length * 0.5F) / FRONT, 0.0F, 1.0F);
            pulse = Math.max(pulse, front);
            along += length;
            float cx = dy * a[5] - dz * a[4];
            float cy = dz * a[3] - dx * a[5];
            float cz = dx * a[4] - dy * a[3];
            float across = Mth.sqrt(cx * cx + cy * cy + cz * cz);
            if (across < 1.0E-4F) {
                continue;
            }
            float bright = glow * (0.55F + 0.45F * pulse);
            float core = (0.36F + 0.3F * pulse) * width * 0.5F / across;
            float halo = (1.5F + 0.8F * pulse) * width * 0.5F / across;
            strip(light, matrix, a, b, cx * core, cy * core, cz * core, Colors.mix(BRIGHT, 0xE6FFEC, pulse), bright);
            strip(light, matrix, a, b, cx * halo, cy * halo, cz * halo, GREEN, bright * 0.45F);
        }
    }

    private static void strip(VertexConsumer light, Matrix4f matrix, float[] a, float[] b, float ox, float oy,
            float oz, int rgb, float alpha) {
        for (int side = -1; side <= 1; side += 2) {
            vertex(light, matrix, a[0], a[1], a[2], rgb, alpha);
            vertex(light, matrix, b[0], b[1], b[2], rgb, alpha);
            vertex(light, matrix, b[0] + side * ox, b[1] + side * oy, b[2] + side * oz, rgb, 0.0F);
            vertex(light, matrix, a[0] + side * ox, a[1] + side * oy, a[2] + side * oz, rgb, 0.0F);
        }
    }

    private static float pulse(float distance, float time, float flow) {
        float phase = (distance - time * FLOW * flow) / GAP;
        float wave = phase - Mth.floor(phase);
        return wave > 0.8F ? (1.0F - wave) / 0.2F : (float) Math.pow(wave / 0.8F, 3.0);
    }
}
