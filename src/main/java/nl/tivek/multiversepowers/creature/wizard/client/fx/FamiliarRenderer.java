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
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneFamiliar;

// A familiar: a little bird of violet light, a white-hot body with two long wings beating fast (faster as it dives at
// you), a forked tail and a glowing trail behind it along the way it flies; it blinks red as it is struck.
public final class FamiliarRenderer extends EntityRenderer<ArcaneFamiliar> {
    public FamiliarRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ArcaneFamiliar familiar, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light) {
        float age = familiar.tickCount + partialTick;
        boolean diving = familiar.isCharging();
        int colour = familiar.hurtTime > 0 ? 0xFF5A7A : Wizard.VIOLET;
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        Vec3 v = familiar.getDeltaMovement();
        float heading = (float) Mth.atan2(v.x, v.z);
        float pitch = (float) Mth.atan2(-v.y, Math.sqrt(v.x * v.x + v.z * v.z));
        float middle = familiar.getBbHeight() * 0.5F;
        var facing = this.entityRenderDispatcher.cameraOrientation();
        pose.pushPose();
        pose.translate(0.0F, middle + 0.06F * Mth.sin(age * 0.3F), 0.0F);
        Additive.glow(pose, buffer, Vec3.ZERO, 0.55F, Additive.rgb(colour, 0.7F), facing);
        Additive.glow(pose, buffer, Vec3.ZERO, 0.18F, Additive.rgb(0xFFFFFF, 1.0F), facing);
        pose.mulPose(Axis.YP.rotation(heading));
        pose.mulPose(Axis.XP.rotation(pitch));
        PoseStack.Pose at = pose.last();
        float beat = Mth.sin(age * (diving ? 2.4F : 1.2F));
        for (int s = -1; s <= 1; s += 2) {
            float lift = 0.35F * beat;
            float[] root0 = { 0.0F, 0.02F, 0.12F };
            float[] root1 = { 0.0F, 0.02F, -0.1F };
            float[] tip = { s * 0.62F, lift * 0.62F + 0.05F, -0.16F };
            float[] elbow = { s * 0.34F, lift * 0.3F + 0.04F, 0.06F };
            float[] bright = Additive.rgb(colour, 0.95F);
            float[] dim = Additive.rgb(colour, 0.25F);
            Additive.quad(at, buffer, root0, elbow, tip, root1, bright, bright, dim, bright);
        }
        float[] tail = Additive.rgb(colour, 0.7F);
        float[] none = Additive.rgb(0, 0.0F);
        float sway = 0.08F * Mth.sin(age * 0.5F);
        Additive.quad(at, buffer, new float[] { -0.05F, 0.0F, -0.08F }, new float[] { 0.05F, 0.0F, -0.08F },
                new float[] { 0.16F + sway, 0.0F, -0.62F }, new float[] { -0.16F + sway, 0.0F, -0.62F }, tail, tail,
                none, none);
        pose.popPose();
        Vec3 back = v.lengthSqr() < 1.0E-6 ? Vec3.ZERO : v.normalize().scale(-0.35);
        for (int k = 1; k <= 4; k++) {
            Additive.glow(pose, buffer, back.scale(k).add(0.0, middle, 0.0), 0.32F - k * 0.05F,
                    Additive.rgb(colour, 0.45F - k * 0.09F), facing);
        }
        super.render(familiar, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ArcaneFamiliar familiar) {
        return Additive.ORB;
    }
}
