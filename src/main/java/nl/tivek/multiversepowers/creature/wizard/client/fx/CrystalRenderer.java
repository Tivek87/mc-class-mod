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
import nl.tivek.multiversepowers.creature.wizard.spell.WardCrystal;

// A crystal of the Wizard's ward: a long eight-faced crystal in his phase's colour, turning and bobbing on its ring
// round him, its core bright and its points pale, dimmer and flickering as it is struck, in a soft glow, with a thread
// of light that wavers between it and his chest.
public final class CrystalRenderer extends EntityRenderer<WardCrystal> {
    private static final float LONG = 0.9F;

    public CrystalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(WardCrystal crystal, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(WardCrystal crystal, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        Wizard wizard = crystal.owner();
        if (wizard == null) {
            return;
        }
        float age = crystal.tickCount + partialTick;
        double time = crystal.level().getGameTime() + partialTick;
        Vec3 drawn = crystal.getPosition(partialTick);
        // On its ring round him where he is drawn this frame, so it never trails him.
        Vec3 at = WardCrystal.place(wizard, crystal.slot(), time).subtract(wizard.position())
                .add(wizard.getPosition(partialTick)).subtract(drawn);
        int colour = Wizard.light(wizard.phase());
        float health = crystal.health() / WardCrystal.MOST_HEALTH;
        float flicker = health < 0.5F ? 0.75F + 0.25F * Mth.sin(age * 2.7F) : 1.0F;
        float bright = (0.55F + 0.45F * health) * flicker;
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        pose.pushPose();
        pose.translate(at.x, at.y, at.z);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation(age * 0.08F));
        pose.mulPose(Axis.ZP.rotation(0.25F * Mth.sin(age * 0.05F)));
        Additive.spindle(pose.last(), buffer, LONG, 0.24F, 0.5F, 4, Additive.rgb(colour, bright),
                Additive.rgb(0xFFFFFF, 0.7F * bright));
        pose.pushPose();
        pose.translate(0.0F, LONG * 0.2F, 0.0F);
        Additive.spindle(pose.last(), buffer, LONG * 0.6F, 0.1F, 0.5F, 4, Additive.rgb(0xFFFFFF, 0.9F * bright),
                Additive.rgb(colour, 0.5F * bright));
        pose.popPose();
        pose.popPose();
        Additive.glow(pose, buffer, new Vec3(0.0, LONG * 0.5, 0.0), 0.8F, Additive.rgb(colour, 0.35F * bright),
                this.entityRenderDispatcher.cameraOrientation());
        Vec3 chest = wizard.getPosition(partialTick).add(0.0, 1.1 + wizard.hover(), 0.0).subtract(drawn).subtract(at);
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(drawn).subtract(at);
        Vec3 from = new Vec3(0.0, LONG * 0.5, 0.0);
        Vec3 bend = new Vec3(0.0, 0.25 * Mth.sin(age * 0.3F), 0.0);
        Vec3 halfway = from.add(chest).scale(0.5).add(bend);
        float wave = 0.25F + 0.15F * Mth.sin(age * 0.5F);
        Additive.ribbon(pose.last(), buffer, from, halfway, eye, 0.05F, Additive.rgb(colour, wave * bright),
                Additive.rgb(colour, wave * 0.6F));
        Additive.ribbon(pose.last(), buffer, halfway, chest, eye, 0.05F, Additive.rgb(colour, wave * 0.6F),
                Additive.rgb(colour, 0.0F));
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(WardCrystal crystal) {
        return Additive.ORB;
    }
}
