package nl.tivek.multiversepowers.creature.wizard.client.fx;

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
import nl.tivek.multiversepowers.creature.wizard.spell.PrismLance;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A prism lance: a long six-faced crystal of storm-blue light with a white core, growing out of nothing beside him and
// turning to point at its mark; while it aims a thin line of light runs from its point to where it points, flickering
// faster and turning white as it is about to shoot; in flight a streak of light trails it.
public final class LanceRenderer extends EntityRenderer<PrismLance> {
    private static final float LONG = 2.4F;
    private static final int GROW = 8;

    public LanceRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(PrismLance lance, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(PrismLance lance, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = lance.tickCount + partialTick;
        Vec3 at = lance.getPosition(partialTick);
        boolean flying = lance.flying();
        Vec3 way = flying ? lance.getDeltaMovement() : lance.aim().subtract(at);
        if (way.lengthSqr() < 1.0E-6) {
            way = new Vec3(0.0, -1.0, 0.0);
        }
        way = way.normalize();
        float grown = Mth.clamp(age / GROW, 0.0F, 1.0F);
        grown = grown * grown * (3.0F - 2.0F * grown);
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(at);
        if (!flying) {
            float left = lance.launch() - age;
            boolean locked = left < PrismLance.LOCK;
            float flick = locked ? 0.6F + 0.4F * Mth.sin(age * 3.0F) : 0.35F + 0.15F * Mth.sin(age * 0.8F);
            float[] line = locked ? Additive.rgb(0xFFFFFF, 0.55F * flick) : Additive.rgb(Wizard.STORM, 0.4F * flick);
            Vec3 tip = way.scale(LONG * 0.5 * grown);
            Additive.ribbon(pose.last(), buffer, tip, lance.aim().subtract(at), eye, locked ? 0.06F : 0.035F, line,
                    Additive.rgb(Wizard.STORM, 0.05F));
        } else {
            Additive.ribbon(pose.last(), buffer, way.scale(-LONG * 1.6), way.scale(-LONG * 0.3), eye, 0.3F,
                    Additive.rgb(Wizard.STORM, 0.0F), Additive.rgb(Wizard.STORM, 0.5F));
        }
        pose.pushPose();
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), way.toVector3f()));
        pose.mulPose(Axis.YP.rotation(age * 0.15F));
        pose.translate(0.0F, -LONG * 0.5F * grown, 0.0F);
        Additive.spindle(pose.last(), buffer, LONG * grown, 0.2F * grown, 0.62F, 6, Additive.rgb(Wizard.STORM, 0.9F),
                Additive.rgb(0xFFFFFF, 0.8F));
        pose.translate(0.0F, LONG * 0.15F * grown, 0.0F);
        Additive.spindle(pose.last(), buffer, LONG * 0.7F * grown, 0.07F * grown, 0.62F, 6,
                Additive.rgb(0xFFFFFF, 0.9F), Additive.rgb(Wizard.STORM, 0.6F));
        pose.popPose();
        Additive.glow(pose, buffer, Vec3.ZERO, 0.9F * grown, Additive.rgb(Wizard.STORM, 0.3F),
                this.entityRenderDispatcher.cameraOrientation());
    }

    @Override
    public ResourceLocation getTextureLocation(PrismLance lance) {
        return Additive.ORB;
    }
}
