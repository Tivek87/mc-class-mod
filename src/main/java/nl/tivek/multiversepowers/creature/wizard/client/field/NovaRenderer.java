package nl.tivek.multiversepowers.creature.wizard.client.field;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.creature.wizard.field.FrostNova;

// The frost nova: a wall of jagged ice spikes bursting out of the ground in a ring that runs outward, each spike
// leaning out the way it runs, some taller than others; behind it a bright ring of frost on the ground and frozen mist. The
// spikes stand a moment once it has run its course, then sink back into the ground.
public final class NovaRenderer extends EntityRenderer<FrostNova> {
    private static final int SPIKES = 64;

    public NovaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(FrostNova nova, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(FrostNova nova, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = nova.tickCount + partialTick;
        float radius = (float) FrostNova.radius(age);
        float sink = Mth.clamp((age - (FrostNova.LIFE - 10)) / 10.0F, 0.0F, 1.0F);
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        float fade = 1.0F - sink;
        Additive.ring(pose.last(), glow, Math.max(0.0F, radius - 0.8F), radius, 0.04F, 72,
                Additive.rgb(Wizard.ICE, 0.7F * fade), Additive.rgb(0xFFFFFF, 0.9F * fade));
        Additive.ring(pose.last(), glow, radius, radius + 1.4F, 0.04F, 72, Additive.rgb(Wizard.ICE, 0.4F * fade),
                Additive.rgb(0, 0.0F));
        Additive.ring(pose.last(), glow, 0.0F, radius, 0.03F, 48, Additive.rgb(Wizard.ICE, 0.12F * fade),
                Additive.rgb(Wizard.ICE, 0.25F * fade));
        spikes(pose, glow, radius, age, sink, false);
        // Taking another buffer ends the one before, so each kind is drawn whole before the next is taken.
        spikes(pose, buffers.getBuffer(Additive.MIST), radius, age, sink, true);
    }

    // The ring of spikes: their ice, or the light in them.
    private static void spikes(PoseStack pose, VertexConsumer buffer, float radius, float age, float sink,
            boolean ice) {
        float fade = 1.0F - sink;
        for (int k = 0; k < SPIKES; k++) {
            float angle = k * Mth.TWO_PI / SPIKES + 0.04F * Mth.sin(k * 7.3F);
            float out = radius - 0.3F * ((k * 37) % 5) / 5.0F;
            float tall = (0.6F + 0.7F * ((k * 53) % 7) / 7.0F) * Mth.clamp(age / 3.0F, 0.0F, 1.0F) * (1.0F - sink);
            if (tall <= 0.02F) {
                continue;
            }
            pose.pushPose();
            pose.translate(Mth.cos(angle) * out, -0.15F - tall * sink, Mth.sin(angle) * out);
            pose.mulPose(Axis.YP.rotation(-angle));
            pose.mulPose(Axis.ZP.rotation(-0.35F - 0.15F * ((k * 11) % 3)));
            if (ice) {
                Additive.spindle(pose.last(), buffer, tall * 1.6F, 0.16F + 0.06F * (k % 3), 0.25F, 5,
                        Additive.mist(0xBFEFFF, 0.85F), Additive.mist(0xF4FCFF, 0.95F));
            } else {
                Additive.spindle(pose.last(), buffer, tall * 1.6F, 0.1F, 0.25F, 5,
                        Additive.rgb(Wizard.ICE, 0.25F * fade), Additive.rgb(0xFFFFFF, 0.5F * fade));
            }
            pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(FrostNova nova) {
        return Additive.ORB;
    }
}
