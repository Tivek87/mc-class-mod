package nl.tivek.multiversepowers.creature.wizard.client.abyss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.abyss.VoidOrb;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.engine.client.fx.Singularity;

// The void orb: a small black hole (Singularity: the world bent round it, a disc of magenta light whirling round it,
// the picture dragged in towards it), its disc tilted and slowly turning, growing as it drifts and trembling as it falls
// in on itself. Without the shader a black ball in rings of magenta light stands in for it. Round it always a faint
// glow and streaks of light spiralling in.
public final class VoidOrbRenderer extends EntityRenderer<VoidOrb> {
    private static final int HOT = 0xFFB8F4;
    private static final int COOL = 0x7A1AA8;

    public VoidOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(VoidOrb orb, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(VoidOrb orb, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = orb.tickCount + partialTick;
        float size = VoidOrb.size(age);
        float collapse = VoidOrb.collapse(age);
        float tremble = 1.0F + 0.12F * collapse * Mth.sin(age * 3.3F);
        float radius = 1.1F * size * tremble;
        Vec3 middle = orb.getPosition(partialTick).add(0.0, 0.5, 0.0);
        Vec3 tilt = new Vec3(0.35 * Mth.sin(age * 0.02F), 1.0, 0.35 * Mth.cos(age * 0.017F));
        pose.pushPose();
        pose.translate(0.0F, 0.5F, 0.0F);
        boolean hole = Singularity.available();
        if (hole) {
            Singularity.hole(middle, radius * 0.45, tilt, 2.2, 2.2 + 2.6 * Math.min(1.0, size * 1.5), Math.min(1.0,
                    age / 8.0), collapse, HOT, COOL, 0.5 * size, orb.getId());
        } else {
            Additive.shell(pose.last(), buffers.getBuffer(Additive.MIST), radius * 0.9F, radius * 0.9F, 8, 12,
                    Vec3.ZERO, (ring, side, rim) -> Additive.mist(0x000000, 1.0F), 0.0F, 0.0F);
        }
        // Taking another buffer ends the one before, so each kind is drawn whole before the next is taken.
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        if (!hole) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotation(0.35F * Mth.sin(age * 0.02F)));
            pose.mulPose(Axis.YP.rotation(age * 0.12F));
            Additive.ring(pose.last(), glow, radius * 1.1F, radius * 2.6F, 0.0F, 40, Additive.rgb(HOT, 0.8F),
                    Additive.rgb(COOL, 0.0F));
            pose.popPose();
        }
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(middle);
        for (int k = 0; k < 8; k++) {
            float run = (age * 0.05F + k * 0.125F) % 1.0F;
            float angle = k * 0.785F + run * 4.0F + age * 0.03F;
            float far = radius * (4.5F - 3.5F * run);
            Vec3 a = new Vec3(Mth.cos(angle) * far, 0.4 * Mth.sin(k * 2.1F) * far * 0.3, Mth.sin(angle) * far);
            Vec3 b = a.scale(0.8);
            Additive.ribbon(pose.last(), glow, a, b, eye, 0.06F, Additive.rgb(COOL, 0.0F),
                    Additive.rgb(HOT, 0.5F * run));
        }
        Additive.glow(pose, glow, Vec3.ZERO, radius * 3.2F, Additive.rgb(Wizard.VOID, 0.25F + 0.4F * collapse),
                this.entityRenderDispatcher.cameraOrientation());
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(VoidOrb orb) {
        return Additive.ORB;
    }
}
