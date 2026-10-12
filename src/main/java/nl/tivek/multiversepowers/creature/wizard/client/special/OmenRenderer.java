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
import nl.tivek.multiversepowers.creature.wizard.special.Omen;

// The omens over the Wizard. The black sun: a disc of darkness turned to the eye, ringed by a burning magenta corona
// whose rays reach out and turn slowly, swelling in out of nothing and shrinking away. The golden clock: a great face
// laid flat over him, seen from below: rings, twelve marks and runes round its rim, its two hands standing still while
// time does, then racing round, before the face bursts outward and is gone.
public final class OmenRenderer extends EntityRenderer<Omen> {
    private static final float SUN = 4.5F;
    private static final float FACE = 8.0F;
    private static final int RAYS = 18;
    // Ticks the clock's hands stand still.
    private static final float STILL = 34.0F;

    public OmenRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(Omen omen, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(Omen omen, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = omen.tickCount + partialTick;
        float life = omen.life();
        float grow = Mth.clamp(age / 12.0F, 0.0F, 1.0F);
        grow = 1.0F - (1.0F - grow) * (1.0F - grow) * (1.0F - grow);
        float end = Mth.clamp((life - age) / 10.0F, 0.0F, 1.0F);
        if (omen.kind() == Omen.ECLIPSE) {
            this.sun(pose, buffers, age, grow * end);
        } else {
            this.clock(pose, buffers, age, grow, end);
        }
    }

    private void sun(PoseStack pose, MultiBufferSource buffers, float age, float size) {
        if (size <= 0.0F) {
            return;
        }
        float r = SUN * size;
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.XP.rotation(Mth.HALF_PI));
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        float flicker = 0.85F + 0.15F * Mth.sin(age * 0.9F);
        Additive.ring(pose.last(), glow, r * 0.96F, r * 1.6F, 0.0F, 64, Additive.rgb(Wizard.VOID, 0.95F * flicker),
                Additive.rgb(0x40005A, 0.0F));
        Additive.ring(pose.last(), glow, r * 0.98F, r * 1.08F, -0.01F, 64, Additive.rgb(0xFFD0FF, flicker),
                Additive.rgb(Wizard.VOID, 0.6F));
        for (int k = 0; k < RAYS; k++) {
            float angle = k * Mth.TWO_PI / RAYS + age * 0.01F;
            float reach = r * (0.9F + 0.6F * (0.5F + 0.5F * Mth.sin(age * 0.17F + k * 2.7F)));
            pose.pushPose();
            pose.mulPose(Axis.YP.rotation(-angle));
            pose.translate(r, 0.0F, 0.0F);
            pose.mulPose(Axis.ZP.rotation(-Mth.HALF_PI));
            pose.scale(0.15F, 1.0F, 1.0F);
            Additive.spindle(pose.last(), glow, reach, 0.35F * size, 0.15F, 4, Additive.rgb(Wizard.VOID, 0.7F),
                    Additive.rgb(0x60008A, 0.0F));
            pose.popPose();
        }
        VertexConsumer dark = buffers.getBuffer(Additive.MIST);
        Additive.ring(pose.last(), dark, 0.0F, r, -0.02F, 64, Additive.mist(0x030006, 0.97F),
                Additive.mist(0x030006, 0.97F));
        Additive.ring(pose.last(), dark, r, r * 1.04F, -0.02F, 64, Additive.mist(0x030006, 0.97F),
                Additive.mist(0x030006, 0.0F));
        pose.popPose();
    }

    private void clock(PoseStack pose, MultiBufferSource buffers, float age, float grow, float end) {
        float burst = 1.0F - end;
        float r = FACE * grow * (1.0F + 0.6F * burst);
        float strength = grow * end;
        if (strength <= 0.0F) {
            return;
        }
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        float[] gold = Additive.rgb(Wizard.TIME, strength);
        float[] white = Additive.rgb(0xFFFFFF, 0.8F * strength);
        float[] faint = Additive.rgb(Wizard.TIME, 0.25F * strength);
        Additive.ring(pose.last(), glow, r * 0.95F, r, 0.0F, 96, gold, gold);
        Additive.ring(pose.last(), glow, r, r * 1.15F, 0.0F, 96, faint, Additive.rgb(Wizard.TIME, 0.0F));
        Additive.ring(pose.last(), glow, r * 0.78F, r * 0.8F, 0.01F, 80, gold, gold);
        Additive.ring(pose.last(), glow, 0.0F, r, -0.01F, 64, Additive.rgb(Wizard.TIME, 0.12F * strength),
                Additive.rgb(Wizard.TIME, 0.06F * strength));
        Additive.ticks(pose.last(), glow, r * 0.94F, r * 0.13F, 12, 0.0F, 0.22F, 0.02F, white);
        Additive.ticks(pose.last(), glow, r * 0.94F, r * 0.05F, 60, 0.0F, 0.07F, 0.02F, gold);
        Additive.star(pose.last(), glow, r * 0.78F, 12, 5, -age * 0.004F, 0.08F, 0.015F, faint);
        float run = Math.max(0.0F, age - STILL);
        float minute = -Mth.HALF_PI + run * run * 0.02F;
        float hour = -Mth.HALF_PI + 1.3F + run * run * 0.0017F;
        hand(pose, glow, minute, r * 0.85F, 0.28F, white);
        hand(pose, glow, hour, r * 0.55F, 0.42F, gold);
        Additive.ring(pose.last(), glow, 0.0F, r * 0.07F, 0.03F, 24, white, white);
    }

    private static void hand(PoseStack pose, VertexConsumer glow, float angle, float length, float width,
            float[] colour) {
        Additive.line(pose.last(), glow, -Mth.cos(angle) * length * 0.12F, -Mth.sin(angle) * length * 0.12F,
                Mth.cos(angle) * length, Mth.sin(angle) * length, width, 0.025F, colour);
    }

    @Override
    public ResourceLocation getTextureLocation(Omen omen) {
        return Additive.ORB;
    }
}
