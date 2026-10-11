package nl.tivek.multiversepowers.creature.wizard.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;

// The light his later phases wear over their dress: from his second, two rings of runes turning round his waist in
// his phase's light; at his third a crown of flames on his hat's spikes and fire licking round his hem; at his fourth
// shards of the void circling his head and a dark smoke trailing from his cape; at his fifth a halo behind his head,
// starred, and six-feathered wings of light on his back, slowly beating. All of it fades with him (a ghost, his death).
final class RegaliaLayer<T extends MovingMonster> extends RenderLayer<T, WizardModel<T>> {
    private static final float PX = 1.0F / 16.0F;
    private static final int FEATHERS = 6;

    RegaliaLayer(RenderLayerParent<T, WizardModel<T>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T caster, float limbSwing, float amount,
            float partialTick, float age, float headYaw, float headPitch) {
        int phase = WizardRenderer.phase(caster);
        WizardModel<T> model = this.getParentModel();
        float shown = model.alpha * (caster instanceof Wizard ? 1.0F : 0.6F);
        if (phase < 2 || shown <= 0.02F) {
            return;
        }
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        int colour = Wizard.light(phase);
        this.runes(pose, glow, model, age, colour, shown);
        switch (phase) {
            case 3 -> {
                this.crown(pose, glow, model, age, shown);
                this.hem(pose, glow, model, age, shown);
            }
            case 4 -> {
                this.shards(pose, glow, model, age, shown, false);
                // Taking another buffer ends the one before, so each kind is drawn whole before the next is taken.
                VertexConsumer dark = buffers.getBuffer(Additive.MIST);
                this.shards(pose, dark, model, age, shown, true);
                this.smoke(pose, dark, model, age, shown);
            }
            case 5 -> {
                this.halo(pose, glow, model, age, shown);
                this.wings(pose, glow, model, age, shown);
            }
            default -> {
            }
        }
    }

    // Two rings of runes round his waist, tilted against each other, turning opposite ways.
    private void runes(PoseStack pose, VertexConsumer glow, WizardModel<T> model, float age, int colour, float shown) {
        pose.pushPose();
        model.body.translateAndRotate(pose);
        pose.translate(0.0F, -5.0F * PX, 0.0F);
        for (int k = 0; k < 2; k++) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotation(k == 0 ? 0.32F : -0.25F));
            pose.mulPose(Axis.ZP.rotation(k == 0 ? 0.12F : -0.18F));
            pose.mulPose(Axis.YP.rotation(age * (k == 0 ? 0.05F : -0.07F)));
            float r = k == 0 ? 0.62F : 0.72F;
            float[] lit = Additive.rgb(colour, 0.65F * shown);
            Additive.ring(pose.last(), glow, r * 0.96F, r, 0.0F, 40, lit, lit);
            Additive.ticks(pose.last(), glow, r * 0.94F, r * 0.12F, 16, 0.0F, 0.025F, 0.0F,
                    Additive.rgb(0xFFFFFF, 0.5F * shown));
            Additive.ring(pose.last(), glow, r, r * 1.12F, 0.0F, 40, Additive.rgb(colour, 0.25F * shown),
                    Additive.rgb(0, 0.0F));
            pose.popPose();
        }
        pose.popPose();
    }

    // Tongues of flame on the spikes of his crown.
    private void crown(PoseStack pose, VertexConsumer glow, WizardModel<T> model, float age, float shown) {
        pose.pushPose();
        model.hat.translateAndRotate(pose);
        Vec3 eye = new Vec3(0.0, 0.0, -20.0);
        for (int k = 0; k < WizardModel.SPIKES; k++) {
            float angle = -Mth.HALF_PI + k * Mth.TWO_PI / WizardModel.SPIKES;
            float tall = (k == 0 ? 0.55F : 0.4F) * (0.75F + 0.25F * Mth.sin(age * 0.9F + k * 1.7F));
            Vec3 root = new Vec3(Mth.cos(angle) * 5.6F * PX, -(8.6F + (k == 0 ? 4.5F : 3.0F)) * PX,
                    Mth.sin(angle) * 5.6F * PX);
            Vec3 tip = root.add(0.03 * Mth.sin(age * 0.6F + k), -tall, 0.03 * Mth.cos(age * 0.5F + k));
            Additive.ribbon(pose.last(), glow, root, tip, eye.add(root), 0.14F, Additive.rgb(0xFFE6A0, 0.9F * shown),
                    Additive.rgb(Wizard.FIRE, 0.0F));
            Additive.ribbon(pose.last(), glow, root, tip, eye.reverse().add(root), 0.14F,
                    Additive.rgb(0xFFE6A0, 0.9F * shown), Additive.rgb(Wizard.FIRE, 0.0F));
        }
        pose.popPose();
    }

    // Fire licking up round the hem of his robe.
    private void hem(PoseStack pose, VertexConsumer glow, WizardModel<T> model, float age, float shown) {
        float y = (model.body.y + 10.5F) * PX;
        for (int k = 0; k < 22; k++) {
            float a0 = k * Mth.TWO_PI / 22;
            float a1 = (k + 1) * Mth.TWO_PI / 22;
            float r = 0.4F;
            float lick = 0.18F + 0.14F * Mth.sin(age * 0.8F + k * 2.3F);
            Additive.quad(pose.last(), glow, new float[] { Mth.cos(a0) * r, y, Mth.sin(a0) * r },
                    new float[] { Mth.cos(a1) * r, y, Mth.sin(a1) * r },
                    new float[] { Mth.cos(a1) * r * 0.95F, y - lick, Mth.sin(a1) * r * 0.95F },
                    new float[] { Mth.cos(a0) * r * 0.95F, y - lick, Mth.sin(a0) * r * 0.95F },
                    Additive.rgb(0xFFD070, 0.8F * shown), Additive.rgb(0xFFD070, 0.8F * shown),
                    Additive.rgb(Wizard.FIRE, 0.0F), Additive.rgb(Wizard.FIRE, 0.0F));
        }
    }

    // Shards of the void circling his head, black (`dark`) edged in magenta.
    private void shards(PoseStack pose, VertexConsumer buffer, WizardModel<T> model, float age, float shown,
            boolean dark) {
        pose.pushPose();
        model.head.translateAndRotate(pose);
        pose.translate(0.0F, -6.0F * PX, 0.0F);
        for (int k = 0; k < 5; k++) {
            float angle = age * 0.06F + k * Mth.TWO_PI / 5;
            pose.pushPose();
            pose.translate(Mth.cos(angle) * 0.62F, 0.12F * Mth.sin(age * 0.1F + k), Mth.sin(angle) * 0.62F);
            pose.mulPose(Axis.YP.rotation(age * 0.12F + k));
            pose.mulPose(Axis.ZP.rotation(0.6F));
            if (dark) {
                Additive.spindle(pose.last(), buffer, 0.26F, 0.06F, 0.4F, 4, Additive.mist(0x10061A, shown),
                        Additive.mist(0x2C0E3E, shown));
            } else {
                Additive.spindle(pose.last(), buffer, 0.28F, 0.075F, 0.4F, 4, Additive.rgb(Wizard.VOID, 0.35F * shown),
                        Additive.rgb(0xFFB8F4, 0.6F * shown));
            }
            pose.popPose();
        }
        pose.popPose();
    }

    // Dark smoke trailing from the hem of his cape.
    private void smoke(PoseStack pose, VertexConsumer dark, WizardModel<T> model, float age, float shown) {
        pose.pushPose();
        model.body.translateAndRotate(pose);
        Vec3 eye = new Vec3(20.0, 0.0, 0.0);
        for (int k = 0; k < 5; k++) {
            float x = (k - 2) * 1.8F * PX;
            Vec3 a = new Vec3(x, 9.0F * PX, 3.2F * PX);
            Vec3 b = a.add(0.08 * Mth.sin(age * 0.2F + k), 0.25 + 0.1 * Mth.sin(age * 0.13F + k * 2.0F),
                    0.35 + 0.08 * Mth.cos(age * 0.17F + k));
            Additive.ribbon(pose.last(), dark, a, b, eye, 0.16F, Additive.mist(0x12061C, 0.7F * shown),
                    Additive.mist(0x2A0A3A, 0.0F));
        }
        pose.popPose();
    }

    // A halo behind his head: a ring of white gold, starred round its rim, slowly turning.
    private void halo(PoseStack pose, VertexConsumer glow, WizardModel<T> model, float age, float shown) {
        pose.pushPose();
        model.head.translateAndRotate(pose);
        pose.translate(0.0F, -9.0F * PX, 6.5F * PX);
        pose.mulPose(Axis.XP.rotation(Mth.HALF_PI - 0.2F));
        pose.mulPose(Axis.YP.rotation(age * 0.02F));
        float[] gold = Additive.rgb(0xFFE3A0, 0.85F * shown);
        Additive.ring(pose.last(), glow, 0.4F, 0.46F, 0.0F, 48, gold, Additive.rgb(0xFFFFFF, 0.9F * shown));
        Additive.ring(pose.last(), glow, 0.46F, 0.62F, 0.0F, 48, Additive.rgb(0xFFE3A0, 0.3F * shown),
                Additive.rgb(0, 0.0F));
        Additive.ring(pose.last(), glow, 0.2F, 0.4F, 0.0F, 48, Additive.rgb(0, 0.0F),
                Additive.rgb(0xFFE3A0, 0.15F * shown));
        Additive.star(pose.last(), glow, 0.52F, 12, 5, 0.0F, 0.02F, 0.001F, Additive.rgb(0xFFFFFF, 0.5F * shown));
        pose.popPose();
    }

    // Wings of light on his back, six long feathers a side, fanned out and slowly beating.
    private void wings(PoseStack pose, VertexConsumer glow, WizardModel<T> model, float age, float shown) {
        pose.pushPose();
        model.body.translateAndRotate(pose);
        pose.translate(0.0F, -9.0F * PX, 2.8F * PX);
        float beat = 0.18F * Mth.sin(age * 0.11F);
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            for (int k = 0; k < FEATHERS; k++) {
                float share = k / (float) (FEATHERS - 1);
                float angle = Mth.lerp(share, -0.25F, 1.45F) + beat * (0.5F + share);
                float length = 0.75F + 0.75F * Mth.sin(share * Mth.PI * 0.8F + 0.3F);
                float back = 0.35F + 0.15F * share;
                Vec3 way = new Vec3(side * Mth.cos(angle), -Mth.sin(angle), back).normalize();
                Vec3 root = new Vec3(side * 0.05F, 0.0, 0.0);
                Vec3 tip = root.add(way.scale(length));
                Vec3 across = way.cross(new Vec3(0.0, 0.0, 1.0)).normalize().scale(0.09);
                float[] base = Additive.rgb(0xFFF6E0, 0.75F * shown);
                float[] end = Additive.rgb(0xFFD27A, 0.05F * shown);
                Additive.quad(pose.last(), glow, point(root.subtract(across)), point(root.add(across)),
                        point(tip.add(across.scale(0.4))), point(tip.subtract(across.scale(0.4))), base, base, end,
                        end);
            }
        }
        pose.popPose();
    }

    private static float[] point(Vec3 at) {
        return new float[] { (float) at.x, (float) at.y, (float) at.z };
    }
}
