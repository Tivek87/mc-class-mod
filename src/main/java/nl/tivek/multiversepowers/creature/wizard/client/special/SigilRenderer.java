package nl.tivek.multiversepowers.creature.wizard.client.special;

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
import nl.tivek.multiversepowers.creature.wizard.special.GrandSigil;

// The grand sigil: a great circle of runes burned into the ground round him, opening out from his feet, with a second
// circle inside turning the other way and a ring of runes between; each ring of force a bright band with a wall of
// light standing on it, running out to the rim; the whole circle flares with each and fades once the last has run.
public final class SigilRenderer extends EntityRenderer<GrandSigil> {
    public SigilRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(GrandSigil sigil, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(GrandSigil sigil, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = sigil.tickCount + partialTick;
        float open = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
        open = 1.0F - (1.0F - open) * (1.0F - open) * (1.0F - open);
        float fade = Mth.clamp((GrandSigil.LIFE - age) / 10.0F, 0.0F, 1.0F);
        float reach = (float) GrandSigil.REACH * open;
        float flare = 0.0F;
        for (int pulse = 0; pulse < GrandSigil.PULSES; pulse++) {
            float since = age - pulse * GrandSigil.GAP;
            if (since >= 0.0F) {
                flare = Math.max(flare, Mth.clamp(1.0F - since / 6.0F, 0.0F, 1.0F));
            }
        }
        float strength = (0.55F + 0.45F * flare) * fade;
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        Additive.glyph(pose.last(), glow, reach, age * 0.6F, Wizard.VIOLET, strength);
        Additive.glyph(pose.last(), glow, reach * 0.55F, -age, Wizard.STARLIGHT, 0.7F * strength);
        Additive.ticks(pose.last(), glow, reach * 0.74F, reach * 0.06F, 36, -age * 0.015F, 0.12F, 0.06F,
                Additive.rgb(0xFFFFFF, 0.4F * strength));
        Additive.star(pose.last(), glow, reach * 0.74F, 9, 4, age * 0.01F, 0.1F, 0.065F,
                Additive.rgb(Wizard.VIOLET, 0.6F * strength));
        for (int pulse = 0; pulse < GrandSigil.PULSES; pulse++) {
            float r = (float) GrandSigil.ring(pulse, age);
            if (r < 0.0F || r > GrandSigil.REACH) {
                continue;
            }
            float left = 1.0F - r / (float) GrandSigil.REACH * 0.5F;
            Additive.ring(pose.last(), glow, Math.max(0.0F, r - 0.9F), r, 0.08F, 96, Additive.rgb(Wizard.VIOLET,
                    0.2F * left), Additive.rgb(0xFFFFFF, 0.9F * left));
            Additive.ring(pose.last(), glow, r, r + 0.5F, 0.08F, 96, Additive.rgb(Wizard.VIOLET, 0.6F * left),
                    Additive.rgb(Wizard.VIOLET, 0.0F));
            Additive.wall(pose.last(), glow, r, 0.05F, 0.65F, 96, Additive.rgb(Wizard.VIOLET, 0.7F * left),
                    Additive.rgb(Wizard.STARLIGHT, 0.0F));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(GrandSigil sigil) {
        return Additive.ORB;
    }
}
