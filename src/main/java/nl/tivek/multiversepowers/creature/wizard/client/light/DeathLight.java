package nl.tivek.multiversepowers.creature.wizard.client.light;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;

// The light of his death, over his own pose: as he kneels, beams of his phase's light crack out of his body one after
// another, more and longer, turning slowly; as it bursts out of him a pillar of light rises from him into the sky and
// widens while he hangs lifted in it; then rings of light close in on him as he is drawn in on himself, and in the
// end a last glow flares where he was and fades.
public final class DeathLight {
    private static final int BEAMS = 14;
    private static final float CRACK_FROM = 24.0F;
    private static final float DRAW_IN = 84.0F;

    private DeathLight() {
    }

    public static void draw(Wizard wizard, float t, float age, PoseStack pose, MultiBufferSource buffers,
            EntityRenderDispatcher dispatcher) {
        int colour = Wizard.light(wizard.phase());
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        float partial = age - wizard.tickCount;
        Vec3 eye = dispatcher.camera.getPosition().subtract(wizard.getPosition(partial));
        Vec3 chest = new Vec3(0.0, 1.1, 0.0);
        float gone = Mth.clamp((t - WizardMove.GONE) / WizardMove.AFTERGLOW, 0.0F, 1.0F);
        if (t > CRACK_FROM && t < WizardMove.GONE) {
            RandomSource random = RandomSource.create(wizard.getId() * 17L);
            float cracks = Mth.clamp((t - CRACK_FROM) / (WizardMove.BREAK - CRACK_FROM), 0.0F, 1.0F);
            float shrink = 1.0F - Mth.clamp((t - DRAW_IN) / (WizardMove.GONE - DRAW_IN), 0.0F, 1.0F);
            for (int k = 0; k < BEAMS; k++) {
                Vec3 way = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.8 + 0.3, random.nextGaussian())
                        .normalize();
                float since = cracks * BEAMS - k;
                if (since <= 0.0F) {
                    continue;
                }
                float length = (0.8F + 2.2F * Mth.clamp(since / 3.0F, 0.0F, 1.0F)) * shrink
                        * (1.0F + 0.15F * Mth.sin(age * 0.7F + k));
                Vec3 spun = way.yRot(age * 0.01F);
                Vec3 from = chest.add(spun.scale(0.2));
                Vec3 to = chest.add(spun.scale(length));
                float lit = 0.6F + 0.4F * Mth.sin(age * 1.3F + k * 2.0F);
                Additive.ribbon(pose.last(), glow, from, to, eye, 0.18F * shrink, Additive.rgb(0xFFFFFF, 0.8F * lit),
                        Additive.rgb(colour, 0.0F));
            }
        }
        if (t > WizardMove.BREAK && t < WizardMove.GONE + 4) {
            float rise = Mth.clamp((t - WizardMove.BREAK) / 10.0F, 0.0F, 1.0F);
            float fade = 1.0F - Mth.clamp((t - WizardMove.GONE + 4) / 8.0F, 0.0F, 1.0F);
            float wide = (0.4F + 0.9F * rise) * (1.0F + 0.08F * Mth.sin(age * 2.0F));
            Vec3 sky = new Vec3(0.0, 40.0 * rise, 0.0);
            // Nested ribbons, each wider and fainter, so the pillar's edges are soft rather than a box's.
            float[] widths = { 0.3F, 0.6F, 1.0F, 1.6F, 2.4F, 3.4F };
            float[] strengths = { 0.45F, 0.3F, 0.2F, 0.12F, 0.06F, 0.03F };
            for (int k = 0; k < widths.length; k++) {
                Additive.ribbon(pose.last(), glow, chest, sky, eye, wide * widths[k],
                        Additive.rgb(k < 3 ? 0xFFFFFF : colour, strengths[k] * fade), Additive.rgb(colour, 0.0F));
            }
        }
        if (t > DRAW_IN && t < WizardMove.GONE) {
            float f = (t - DRAW_IN) / (WizardMove.GONE - DRAW_IN);
            for (int k = 0; k < 3; k++) {
                float run = (f * 3.0F + k / 3.0F) % 1.0F;
                float r = 3.0F * (1.0F - run) + 0.1F;
                pose.pushPose();
                pose.translate(0.0F, 1.1F, 0.0F);
                pose.mulPose(Axis.XP.rotation(0.7F * k - 0.7F));
                pose.mulPose(Axis.ZP.rotation(0.4F * k));
                Additive.ring(pose.last(), glow, r * 0.94F, r, 0.0F, 40, Additive.rgb(colour, 0.8F * run),
                        Additive.rgb(0xFFFFFF, 0.8F * run));
                pose.popPose();
            }
        }
        if (t > DRAW_IN) {
            float flare = t < WizardMove.GONE ? Mth.clamp((t - DRAW_IN) / (WizardMove.GONE - DRAW_IN), 0.0F, 1.0F)
                    : 1.0F - gone;
            Additive.glow(pose, glow, chest, 0.6F + 2.4F * flare, Additive.rgb(colour, 0.9F * flare),
                    dispatcher.cameraOrientation());
            Additive.glow(pose, glow, chest, 0.3F + 1.0F * flare, Additive.rgb(0xFFFFFF, flare),
                    dispatcher.cameraOrientation());
        }
    }
}
