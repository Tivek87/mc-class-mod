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
import nl.tivek.multiversepowers.creature.wizard.field.MarkedStrike;

// A marked strike: first its mark, a circle of runes on the ground whose light fills in from the rim to the middle as
// the strike nears, flickering faster at the end; then the strike itself: a pillar of fire roaring up out of the ground
// (PILLAR), a shaft of light from high in the sky with a bright core (SHAFT), the void erupting out of a star-shaped
// crack in a twisting column of darkness edged with magenta (RIFT), or a golden lance dropping from the sky and
// standing in the ground, ringing (LANCE). Each fades and sinks as it lingers.
public final class StrikeRenderer extends EntityRenderer<MarkedStrike> {
    public StrikeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(MarkedStrike strike, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(MarkedStrike strike, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = strike.tickCount + partialTick;
        int kind = strike.kind();
        float delay = strike.delay();
        float radius = (float) strike.radius();
        int colour = MarkedStrike.colour(kind);
        float since = age - delay;
        float linger = strike.linger();
        float fade = since <= 0.0F ? 1.0F : Mth.clamp(1.0F - (since - linger * 0.4F) / (linger * 0.6F), 0.0F, 1.0F);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-strike.getYRot()));
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        this.mark(pose, glow, age, delay, radius, colour, fade);
        if (since > (kind == MarkedStrike.LANCE ? -26.0F : kind == MarkedStrike.RIFT ? -8.0F : -3.0F)) {
            switch (kind) {
                case MarkedStrike.PILLAR -> this.pillar(pose, glow, since, radius, fade, false);
                case MarkedStrike.SHAFT -> this.shaft(pose, glow, since, radius, fade);
                case MarkedStrike.RIFT -> this.rift(pose, glow, since, radius, fade, false);
                default -> this.lance(pose, glow, since, radius, fade, false);
            }
            VertexConsumer mist = buffers.getBuffer(Additive.MIST);
            switch (kind) {
                case MarkedStrike.PILLAR -> this.pillar(pose, mist, since, radius, fade, true);
                case MarkedStrike.RIFT -> this.rift(pose, mist, since, radius, fade, true);
                case MarkedStrike.LANCE -> this.lance(pose, mist, since, radius, fade, true);
                default -> {
                }
            }
        }
        pose.popPose();
    }

    // The mark: its rim and runes, the light filling in from the rim as the strike nears.
    private void mark(PoseStack pose, VertexConsumer glow, float age, float delay, float radius, int colour,
            float fade) {
        float left = delay - age;
        float fill = Mth.clamp(1.0F - left / delay, 0.0F, 1.0F);
        float flicker = left < 6.0F && left > 0.0F ? 0.7F + 0.3F * Mth.sin(age * 2.4F) : 1.0F;
        float strength = (left > 0.0F ? 0.35F + 0.65F * fill : 1.0F) * flicker * fade;
        float grow = Mth.clamp(age / 4.0F, 0.0F, 1.0F);
        Additive.ring(pose.last(), glow, radius * 0.92F * grow, radius * grow, 0.04F, 40,
                Additive.rgb(colour, strength), Additive.rgb(colour, strength));
        Additive.ring(pose.last(), glow, radius * grow, radius * 1.25F * grow, 0.04F, 40,
                Additive.rgb(colour, 0.35F * strength), Additive.rgb(colour, 0.0F));
        if (left > 0.0F) {
            float inner = radius * (1.0F - fill);
            Additive.ring(pose.last(), glow, inner, radius * 0.92F, 0.035F, 40, Additive.rgb(colour, 0.12F + 0.3F
                    * fill), Additive.rgb(colour, 0.35F * fill));
        }
        Additive.star(pose.last(), glow, radius * 0.85F * grow, 5, 2, age * 0.03F, 0.06F, 0.045F,
                Additive.rgb(colour, 0.6F * strength));
        Additive.ticks(pose.last(), glow, radius * 0.9F * grow, radius * 0.15F, 12, -age * 0.02F, 0.05F, 0.05F,
                Additive.rgb(0xFFFFFF, 0.35F * strength));
    }

    // A pillar of fire: flames licking up in rings of tongues, white at the root, red at the tips.
    private void pillar(PoseStack pose, VertexConsumer buffer, float since, float radius, float fade, boolean mist) {
        if (since < 0.0F) {
            return;
        }
        float rise = Mth.clamp(since / 3.0F, 0.0F, 1.0F);
        float tall = 6.0F * (1.0F - (1.0F - rise) * (1.0F - rise)) * (0.4F + 0.6F * fade);
        int tongues = 14;
        for (int k = 0; k < tongues; k++) {
            float angle = k * Mth.TWO_PI / tongues + since * 0.15F;
            float out = radius * (0.3F + 0.5F * ((k * 37) % 7) / 7.0F);
            float h = tall * (0.55F + 0.45F * (0.5F + 0.5F * Mth.sin(since * 1.7F + k * 2.3F)));
            pose.pushPose();
            pose.translate(Mth.cos(angle) * out, 0.0F, Mth.sin(angle) * out);
            pose.mulPose(Axis.ZP.rotation(0.12F * Mth.sin(since * 0.9F + k)));
            if (mist) {
                Additive.spindle(pose.last(), buffer, h, 0.32F, 0.3F, 5, Additive.mist(0xFF7A1A, 0.55F * fade),
                        Additive.mist(0xB01A05, 0.0F));
            } else {
                Additive.spindle(pose.last(), buffer, h, 0.4F, 0.25F, 5, Additive.rgb(0xFFC060, 0.9F * fade),
                        Additive.rgb(0xFF3A10, 0.1F * fade));
            }
            pose.popPose();
        }
        if (!mist) {
            Additive.spindle(pose.last(), buffer, tall * 1.1F, radius * 0.45F, 0.2F, 7, Additive.rgb(0xFFF2C0,
                    0.8F * fade), Additive.rgb(0xFF8A2A, 0.2F * fade));
            Additive.ring(pose.last(), buffer, 0.0F, radius * 1.6F, 0.06F, 32, Additive.rgb(Wizard.FIRE, 0.7F * fade),
                    Additive.rgb(Wizard.FIRE, 0.0F));
        }
    }

    // A shaft of light from the sky: a thin white core in a wide soft column, and a flash where it meets the ground.
    private void shaft(PoseStack pose, VertexConsumer glow, float since, float radius, float fade) {
        float come = Mth.clamp((since + 2.0F) / 2.0F, 0.0F, 1.0F);
        if (come <= 0.0F) {
            return;
        }
        float tall = 40.0F;
        float bottom = tall * (1.0F - come);
        float pulse = since < 0.0F ? 0.4F : 1.0F;
        pose.pushPose();
        pose.translate(0.0F, bottom, 0.0F);
        Additive.spindle(pose.last(), glow, tall - bottom, radius * 0.9F, 0.02F, 10, Additive.rgb(0x9FE4FF,
                0.45F * fade * pulse), Additive.rgb(0x6FB8FF, 0.0F));
        Additive.spindle(pose.last(), glow, tall - bottom, radius * 0.3F, 0.02F, 8, Additive.rgb(0xFFFFFF,
                0.9F * fade * pulse), Additive.rgb(0xCFF0FF, 0.1F));
        pose.popPose();
        if (since >= 0.0F) {
            float flash = Mth.clamp(1.0F - since / 6.0F, 0.0F, 1.0F);
            Additive.ring(pose.last(), glow, 0.0F, radius * (1.5F + 1.5F * (1.0F - flash)), 0.06F, 36,
                    Additive.rgb(0xFFFFFF, 0.9F * flash), Additive.rgb(0x9FE4FF, 0.0F));
        }
    }

    // The void erupting: a star-shaped crack, a column of darkness twisting up out of it, magenta at its edges.
    private void rift(PoseStack pose, VertexConsumer buffer, float since, float radius, float fade, boolean mist) {
        if (since < 0.0F) {
            if (!mist) {
                float open = Mth.clamp((since + 8.0F) / 8.0F, 0.0F, 1.0F);
                Additive.star(pose.last(), buffer, radius * open, 7, 3, 0.0F, 0.08F, 0.06F,
                        Additive.rgb(Wizard.VOID, 0.8F * open));
            }
            return;
        }
        float rise = Mth.clamp(since / 3.0F, 0.0F, 1.0F);
        float tall = 5.0F * (1.0F - (1.0F - rise) * (1.0F - rise)) * (0.3F + 0.7F * fade);
        int arms = 7;
        for (int k = 0; k < arms; k++) {
            float angle = k * Mth.TWO_PI / arms + since * 0.35F;
            float out = radius * 0.35F;
            pose.pushPose();
            pose.translate(Mth.cos(angle) * out, 0.0F, Mth.sin(angle) * out);
            pose.mulPose(Axis.YP.rotation(-angle));
            pose.mulPose(Axis.ZP.rotation(-0.25F));
            if (mist) {
                Additive.spindle(pose.last(), buffer, tall * (0.7F + 0.3F * ((k * 13) % 5) / 5.0F), 0.42F, 0.35F, 5,
                        Additive.mist(0x0A0010, 0.9F * fade), Additive.mist(0x1A0026, 0.2F * fade));
            } else {
                Additive.spindle(pose.last(), buffer, tall * 1.05F, 0.55F, 0.35F, 5, Additive.rgb(Wizard.VOID,
                        0.55F * fade), Additive.rgb(0x5A0A8A, 0.0F));
            }
            pose.popPose();
        }
        if (!mist) {
            Additive.star(pose.last(), buffer, radius * 1.2F, 7, 3, 0.0F, 0.1F, 0.06F, Additive.rgb(Wizard.VOID,
                    fade));
            Additive.ring(pose.last(), buffer, 0.0F, radius * 1.4F, 0.05F, 32, Additive.rgb(Wizard.VOID, 0.5F * fade),
                    Additive.rgb(Wizard.VOID, 0.0F));
        }
    }

    // A golden lance of light: hanging high and turning while marked, dropping in the last ticks, then standing in
    // the ground and ringing out.
    private void lance(PoseStack pose, VertexConsumer buffer, float since, float radius, float fade, boolean mist) {
        float high = since < -4.0F ? 14.0F : since < 0.0F ? 14.0F * (-since / 4.0F) * (-since / 4.0F) : 0.0F;
        float show = Mth.clamp((since + 26.0F) / 6.0F, 0.0F, 1.0F) * fade;
        if (show <= 0.0F) {
            return;
        }
        pose.pushPose();
        pose.translate(0.0F, high - 0.6F, 0.0F);
        pose.mulPose(Axis.YP.rotation(since * (since < 0.0F ? 0.3F : 0.0F)));
        if (mist) {
            Additive.spindle(pose.last(), buffer, 3.6F, 0.14F, 0.75F, 4, Additive.mist(0xFFE7A0, 0.8F * show),
                    Additive.mist(0xFFFFFF, 0.9F * show));
        } else {
            Additive.spindle(pose.last(), buffer, 3.8F, 0.3F, 0.75F, 4, Additive.rgb(Wizard.TIME, 0.6F * show),
                    Additive.rgb(0xFFFFFF, 0.9F * show));
            Additive.spindle(pose.last(), buffer, 1.2F, 0.5F, 0.5F, 4, Additive.rgb(Wizard.TIME, 0.5F * show),
                    Additive.rgb(Wizard.TIME, 0.0F));
        }
        pose.popPose();
        if (!mist && since >= 0.0F) {
            float ring = Mth.clamp(since / 8.0F, 0.0F, 1.0F);
            Additive.ring(pose.last(), buffer, radius * 2.5F * ring, radius * (2.5F * ring + 0.4F), 0.07F, 40,
                    Additive.rgb(Wizard.TIME, (1.0F - ring) * fade), Additive.rgb(0xFFFFFF, 0.0F));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(MarkedStrike strike) {
        return Additive.ORB;
    }
}
