package nl.tivek.multiversepowers.creature.wizard.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.spell.RuneSentry;

// A rune sentry: a crystal eye in two rings of runes standing on edge, turning against each other round it, the eye
// turned to its foe and swelling with light before each spark; it opens out of a point as it is raised and closes into
// one as its time runs out.
public final class SentryRenderer extends EntityRenderer<RuneSentry> {
    public SentryRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RuneSentry sentry, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = sentry.tickCount + partialTick;
        float open = Mth.clamp(age / 8.0F, 0.0F, 1.0F) * Mth.clamp((RuneSentry.LIFE - age) / 8.0F, 0.0F, 1.0F);
        if (open <= 0.0F) {
            return;
        }
        open = 1.0F - (1.0F - open) * (1.0F - open);
        float beat = (age % RuneSentry.EVERY) / RuneSentry.EVERY;
        float swell = beat > 0.72F ? (beat - 0.72F) / 0.28F : 0.0F;
        int colour = sentry.owner() == null ? Wizard.VIOLET : Wizard.light(sentry.owner().phase());
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        pose.pushPose();
        pose.translate(0.0F, 0.4F + 0.12F * Mth.sin(age * 0.12F), 0.0F);
        Additive.glow(pose, glow, Vec3.ZERO, (0.7F + 0.6F * swell) * open, Additive.rgb(colour, 0.55F + 0.45F * swell),
                this.entityRenderDispatcher.cameraOrientation());
        float turnYaw = Mth.rotLerp(partialTick, sentry.yRotO, sentry.getYRot());
        float turnPitch = Mth.lerp(partialTick, sentry.xRotO, sentry.getXRot());
        pose.mulPose(Axis.YP.rotationDegrees(-turnYaw));
        pose.mulPose(Axis.XP.rotationDegrees(turnPitch));
        pose.scale(open, open, open);
        for (int k = 0; k < 2; k++) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotation(Mth.HALF_PI));
            pose.mulPose(Axis.ZP.rotation(k == 0 ? 0.6F : -0.6F));
            pose.mulPose(Axis.YP.rotation((k == 0 ? 1.0F : -1.3F) * age * 0.06F));
            Additive.glyph(pose.last(), glow, 0.75F + 0.2F * k, age * (k == 0 ? 1.0F : -1.0F), colour, 0.8F);
            pose.popPose();
        }
        pose.pushPose();
        pose.mulPose(Axis.XP.rotation(Mth.HALF_PI));
        pose.translate(0.0F, -0.45F, 0.0F);
        Additive.spindle(pose.last(), glow, 0.9F, 0.18F + 0.06F * swell, 0.5F, 6, Additive.rgb(0xFFFFFF,
                0.8F + 0.2F * swell), Additive.rgb(colour, 0.6F));
        pose.popPose();
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(RuneSentry sentry) {
        return Additive.ORB;
    }
}
