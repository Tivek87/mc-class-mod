package nl.tivek.multiversepowers.creature.wizard.client.special;

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
import nl.tivek.multiversepowers.creature.wizard.special.SolarWaves;

// The solar flare: each wave a ring of fire rolling out along the ground, a low burning wall with tongues of flame
// licking up off its crest, white-gold at its foot and red at its tips, scorched ground glowing behind it; at its
// heart the ground blazes as each wave leaves.
public final class WavesRenderer extends EntityRenderer<SolarWaves> {
    private static final int TONGUES = 72;

    public WavesRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(SolarWaves waves, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(SolarWaves waves, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = waves.tickCount + partialTick;
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        float heart = 0.0F;
        for (int wave = 0; wave < SolarWaves.WAVES; wave++) {
            float r = (float) SolarWaves.radius(wave, age);
            if (r < 0.0F) {
                continue;
            }
            heart = Math.max(heart, Mth.clamp(1.0F - (age - wave * SolarWaves.GAP) / 8.0F, 0.0F, 1.0F));
            if (r > SolarWaves.REACH) {
                continue;
            }
            float fade = 1.0F - Mth.clamp((r - (float) SolarWaves.REACH * 0.7F) / ((float) SolarWaves.REACH * 0.3F),
                    0.0F, 1.0F);
            float tall = (float) SolarWaves.TALL;
            Additive.wall(pose.last(), glow, r, 0.02F, tall, 96, Additive.rgb(0xFFE6A0, 0.95F * fade),
                    Additive.rgb(0xFF4A10, 0.0F));
            Additive.wall(pose.last(), glow, r - 0.35F, 0.02F, tall * 0.7F, 96, Additive.rgb(Wizard.FIRE,
                    0.6F * fade), Additive.rgb(0xFF2A00, 0.0F));
            Additive.ring(pose.last(), glow, Math.max(0.0F, r - 2.6F), r, 0.04F, 96, Additive.rgb(0x501000, 0.0F),
                    Additive.rgb(Wizard.FIRE, 0.55F * fade));
            for (int k = 0; k < TONGUES; k++) {
                float angle = k * Mth.TWO_PI / TONGUES + wave * 0.4F;
                float lick = 0.5F + 0.5F * Mth.sin(age * 1.3F + k * 1.9F + wave);
                pose.pushPose();
                pose.translate(Mth.cos(angle) * r, tall * 0.5F, Mth.sin(angle) * r);
                pose.mulPose(Axis.YP.rotation(-angle));
                pose.mulPose(Axis.ZP.rotation(-0.35F));
                Additive.spindle(pose.last(), glow, 0.6F + 0.9F * lick, 0.18F, 0.3F, 4,
                        Additive.rgb(0xFFB040, 0.7F * fade), Additive.rgb(0xFF2A00, 0.0F));
                pose.popPose();
            }
        }
        if (heart > 0.0F) {
            Additive.ring(pose.last(), glow, 0.0F, 3.0F, 0.05F, 40, Additive.rgb(0xFFF2C0, heart),
                    Additive.rgb(Wizard.FIRE, 0.0F));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(SolarWaves waves) {
        return Additive.ORB;
    }
}
