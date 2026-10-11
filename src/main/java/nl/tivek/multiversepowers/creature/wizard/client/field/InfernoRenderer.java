package nl.tivek.multiversepowers.creature.wizard.client.field;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.creature.wizard.field.InfernoRing;

// The inferno ring: a wall of fire round its middle, white-gold at its roots and deep orange up its tongues, which lick
// higher and lower as they flicker, two walls of them one inside the other turning against each other; a glowing ring
// of embers on the ground where it stands. As it closes it burns brighter, and at the end it bursts up into a pillar
// of flame that falls away.
public final class InfernoRenderer extends EntityRenderer<InfernoRing> {
    private static final int TONGUES = 96;

    public InfernoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(InfernoRing ring, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(InfernoRing ring, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = ring.tickCount + partialTick;
        float radius = (float) InfernoRing.radius(age);
        float tall = (float) InfernoRing.height(age);
        float heat = 0.7F + 0.3F * Mth.clamp((age - InfernoRing.RISE) / (InfernoRing.CLOSE - InfernoRing.RISE), 0.0F,
                1.0F);
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        if (tall > 0.02F) {
            for (int wall = 0; wall < 2; wall++) {
                float r = radius - 0.25F * wall;
                for (int k = 0; k < TONGUES; k++) {
                    float a0 = k * Mth.TWO_PI / TONGUES + (wall == 0 ? 1.0F : -1.0F) * age * 0.01F;
                    float a1 = (k + 1) * Mth.TWO_PI / TONGUES + (wall == 0 ? 1.0F : -1.0F) * age * 0.01F;
                    float lick = 0.55F + 0.45F * Mth.sin(age * (0.6F + 0.07F * (k % 5)) + k * 1.7F + wall);
                    float h = tall * (0.5F + 0.6F * lick);
                    float[] root = Additive.rgb(0xFFE6A0, 0.9F * heat);
                    float[] tip = Additive.rgb(Wizard.FIRE, 0.0F);
                    float[] mid = Additive.rgb(Wizard.FIRE, 0.7F * heat);
                    float x0 = Mth.cos(a0) * r;
                    float z0 = Mth.sin(a0) * r;
                    float x1 = Mth.cos(a1) * r;
                    float z1 = Mth.sin(a1) * r;
                    float sway = 0.15F * Mth.sin(age * 0.3F + k);
                    Additive.quad(pose.last(), buffer, new float[] { x0, 0.0F, z0 }, new float[] { x1, 0.0F, z1 },
                            new float[] { x1, h * 0.45F, z1 }, new float[] { x0, h * 0.45F, z0 }, root, root, mid,
                            mid);
                    Additive.quad(pose.last(), buffer, new float[] { x0, h * 0.45F, z0 },
                            new float[] { x1, h * 0.45F, z1 }, new float[] { x1 * (1.0F + sway * 0.1F), h, z1 },
                            new float[] { x0 * (1.0F + sway * 0.1F), h, z0 }, mid, mid, tip, tip);
                }
            }
            Additive.ring(pose.last(), buffer, radius - 0.6F, radius + 0.6F, 0.04F, 72,
                    Additive.rgb(0xFF6A1A, 0.6F * heat), Additive.rgb(0, 0.0F));
        }
        float flare = age > InfernoRing.CLOSE ? Mth.clamp(1.0F - (age - InfernoRing.CLOSE) / 14.0F, 0.0F, 1.0F) : 0.0F;
        if (flare > 0.0F) {
            float r = (float) InfernoRing.NARROW + 2.0F;
            float h = 9.0F * (1.0F - flare * 0.4F);
            for (int k = 0; k < 32; k++) {
                float a0 = k * Mth.TWO_PI / 32;
                float a1 = (k + 1) * Mth.TWO_PI / 32;
                Additive.quad(pose.last(), buffer, new float[] { Mth.cos(a0) * r, 0.0F, Mth.sin(a0) * r },
                        new float[] { Mth.cos(a1) * r, 0.0F, Mth.sin(a1) * r },
                        new float[] { Mth.cos(a1) * r * 0.5F, h, Mth.sin(a1) * r * 0.5F },
                        new float[] { Mth.cos(a0) * r * 0.5F, h, Mth.sin(a0) * r * 0.5F },
                        Additive.rgb(0xFFF0C0, flare), Additive.rgb(0xFFF0C0, flare), Additive.rgb(Wizard.FIRE, 0.0F),
                        Additive.rgb(Wizard.FIRE, 0.0F));
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(InfernoRing ring) {
        return Additive.ORB;
    }
}
