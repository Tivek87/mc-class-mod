package nl.tivek.multiversepowers.creature.wizard.client.field;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.creature.wizard.field.GravityWell;

// The gravity lift round whoever it holds: three rings of deep blue light closing in round them and turning, tilted
// against each other, streaks of light running up past them as they rise; at the slam the rings squash flat and a cone
// of light points them down.
public final class WellRenderer extends EntityRenderer<GravityWell> {
    private static final int SIDES = 28;

    public WellRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(GravityWell well, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(GravityWell well, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        Entity held = well.held();
        if (held == null) {
            return;
        }
        float age = well.tickCount + partialTick;
        Vec3 origin = well.getPosition(partialTick);
        Vec3 body = held.getPosition(partialTick).subtract(origin);
        float tall = held.getBbHeight();
        float wide = Math.max(0.7F, held.getBbWidth() * 1.4F);
        boolean slammed = age >= GravityWell.SLAM;
        float close = Math.min(1.0F, age / GravityWell.RISE);
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        pose.pushPose();
        pose.translate(body.x, body.y, body.z);
        for (int k = 0; k < 3; k++) {
            float radius = wide * (2.4F - 1.4F * close) * (1.0F - 0.1F * k);
            float y = tall * (0.2F + 0.35F * k);
            float squash = slammed ? Math.max(0.0F, 1.0F - (age - GravityWell.SLAM) / 4.0F) : 1.0F;
            pose.pushPose();
            pose.translate(0.0F, y * (slammed ? squash : 1.0F), 0.0F);
            pose.mulPose(com.mojang.math.Axis.YP.rotation(age * (0.2F + 0.1F * k) * (k % 2 == 0 ? 1 : -1)));
            pose.mulPose(com.mojang.math.Axis.XP.rotation((k - 1) * 0.35F * squash));
            float[] bright = Additive.rgb(Wizard.GRAVITY, 0.9F);
            float[] white = Additive.rgb(0xCFD8FF, 0.8F);
            Additive.ring(pose.last(), buffer, radius * 0.9F, radius, 0.0F, SIDES, white, bright);
            Additive.ring(pose.last(), buffer, radius, radius * 1.3F, 0.0F, SIDES, bright, Additive.rgb(0, 0.0F));
            pose.popPose();
        }
        PoseStack.Pose at = pose.last();
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(origin).subtract(body);
        if (!slammed && age >= GravityWell.RISE) {
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3.0 + age * 0.1;
                float run = (age * 0.35F + k * 0.6F) % 3.0F;
                Vec3 low = new Vec3(Math.cos(a) * wide, -1.5 + run, Math.sin(a) * wide);
                Additive.ribbon(at, buffer, low, low.add(0.0, 1.2, 0.0), eye, 0.08F,
                        Additive.rgb(Wizard.GRAVITY, 0.0F), Additive.rgb(0xCFD8FF, 0.7F));
            }
        }
        if (slammed) {
            float fade = Mth.clamp(1.0F - (age - GravityWell.SLAM) / 10.0F, 0.0F, 1.0F);
            Additive.ribbon(at, buffer, new Vec3(0.0, tall + 2.5, 0.0), new Vec3(0.0, 0.0, 0.0), eye, wide * 2.0F,
                    Additive.rgb(Wizard.GRAVITY, 0.0F), Additive.rgb(Wizard.GRAVITY, 0.8F * fade));
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(GravityWell well) {
        return Additive.ORB;
    }
}
