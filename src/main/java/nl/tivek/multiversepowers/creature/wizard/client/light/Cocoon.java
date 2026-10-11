package nl.tivek.multiversepowers.creature.wizard.client.light;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;

// The cocoon he comes down out of his rift in: eight petals of violet light closed round him in an egg, turning slowly
// as he sinks, bright at their edges and seams, a ring of runes round its middle. As he lands they crack open and fold
// out and down like a flower's, flaring white, and then fall apart into light that drifts up and is gone.
public final class Cocoon {
    private static final int PETALS = 8;
    private static final int STEPS = 12;
    private static final float WIDE = 1.0F;
    private static final float LOW = -0.25F;
    private static final float HIGH = 2.55F;
    // It opens over OPEN ticks from his landing, then fades over FADE.
    private static final float OPEN = 16.0F;
    private static final float FADE = 18.0F;

    private Cocoon() {
    }

    public static void draw(Wizard wizard, float t, float age, PoseStack pose, MultiBufferSource buffers, Vec3 eye) {
        float landed = t - WizardMove.LAND;
        float open = Mth.clamp(landed / OPEN, 0.0F, 1.0F);
        open = 1.0F - (1.0F - open) * (1.0F - open) * (1.0F - open);
        float fade = 1.0F - Mth.clamp((landed - OPEN) / FADE, 0.0F, 1.0F);
        float grow = Mth.clamp(t / 10.0F, 0.0F, 1.0F);
        if (fade <= 0.0F || grow <= 0.0F) {
            return;
        }
        float flare = landed > 0.0F ? Mth.clamp(1.0F - landed / 10.0F, 0.0F, 1.0F) : 0.0F;
        float drift = (1.0F - fade) * 1.2F;
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation(age * 0.03F * (1.0F - open)));
        for (int p = 0; p < PETALS; p++) {
            float middle = p * Mth.TWO_PI / PETALS;
            float half = Mth.PI / PETALS * (0.92F - 0.25F * open);
            for (int k = 0; k < STEPS; k++) {
                float h0 = k / (float) STEPS;
                float h1 = (k + 1) / (float) STEPS;
                float[][] corners = { petal(middle - half, h0, open, grow, drift),
                        petal(middle + half, h0, open, grow, drift), petal(middle + half, h1, open, grow, drift),
                        petal(middle - half, h1, open, grow, drift) };
                float edge0 = 0.15F + 0.5F * (float) Math.pow(Math.abs(h0 - 0.5F) * 2.0F, 3.0F);
                float edge1 = 0.15F + 0.5F * (float) Math.pow(Math.abs(h1 - 0.5F) * 2.0F, 3.0F);
                float lit = (0.35F + 0.65F * flare) * fade;
                float[] c0 = Additive.rgb(blend(h0), (edge0 + 0.1F) * lit);
                float[] c1 = Additive.rgb(blend(h1), (edge1 + 0.1F) * lit);
                Additive.quad(pose.last(), glow, corners[0], corners[1], corners[2], corners[3], c0, c0, c1, c1);
                // The bright seam down each petal's edge.
                float[] seam = Additive.rgb(0xFFFFFF, 0.5F * lit);
                float[] a = petal(middle - half, h0, open, grow, drift);
                float[] b = petal(middle - half, h1, open, grow, drift);
                Additive.ribbon(pose.last(), glow, new Vec3(a[0], a[1], a[2]), new Vec3(b[0], b[1], b[2]), eye, 0.05F,
                        seam, seam);
            }
        }
        pose.popPose();
        if (open < 1.0F) {
            pose.pushPose();
            pose.translate(0.0F, (LOW + HIGH) * 0.5F, 0.0F);
            pose.mulPose(Axis.YP.rotation(-age * 0.08F));
            float r = WIDE * 1.08F * grow;
            float lit = 0.7F * (1.0F - open) * fade;
            Additive.ring(pose.last(), glow, r * 0.97F, r, 0.0F, 48, Additive.rgb(Wizard.VIOLET, lit),
                    Additive.rgb(0xFFFFFF, lit));
            Additive.ticks(pose.last(), glow, r, r * 0.12F, 24, 0.0F, 0.03F, 0.0F, Additive.rgb(0xFFFFFF, lit * 0.6F));
            pose.popPose();
        }
    }

    // A point on a petal at `angle` round and `h` up it: on the closed egg, or folded out and down as it opens.
    private static float[] petal(float angle, float h, float open, float grow, float drift) {
        float lat = (h - 0.5F) * Mth.PI;
        float round = Mth.cos(lat) * WIDE * grow;
        float y = LOW + (HIGH - LOW) * (0.5F + 0.5F * Mth.sin(lat));
        // Folding out about its foot: the higher up the petal, the further out and lower.
        float fold = open * h * h;
        float out = round + fold * 2.2F;
        float down = y * (1.0F - 0.75F * fold) + drift * h;
        return new float[] { Mth.cos(angle) * out, down, Mth.sin(angle) * out };
    }

    private static int blend(float h) {
        return h > 0.8F || h < 0.2F ? 0xE6F0FF : Wizard.VIOLET;
    }
}
