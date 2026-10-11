package nl.tivek.multiversepowers.creature.wizard.client.abyss;

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
import nl.tivek.multiversepowers.creature.wizard.abyss.SoulDrain;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;

// The soul drain: three threads of the void's light wound round each other from his open left hand to his foe's
// chest, sagging and swaying, and pale wisps of their life running along them into his hand.
public final class DrainRenderer extends EntityRenderer<SoulDrain> {
    private static final int STEPS = 18;
    private static final int PALE = 0xF2D8FF;

    public DrainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(SoulDrain drain, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(SoulDrain drain, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        if (!(drain.caster() instanceof Wizard wizard) || drain.held() == null) {
            return;
        }
        Entity held = drain.held();
        float age = drain.tickCount + partialTick;
        float grow = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
        Vec3 origin = drain.getPosition(partialTick);
        Vec3 to = held.getPosition(partialTick).add(0.0, held.getBbHeight() * 0.55, 0.0).subtract(origin);
        Vec3 from = wizard.hand().subtract(origin);
        Vec3 along = to.subtract(from);
        double length = along.length();
        if (length < 0.3) {
            return;
        }
        Vec3 side = along.cross(new Vec3(0.0, 1.0, 0.0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        Vec3 up = side.cross(along).normalize();
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        for (int strand = 0; strand < 3; strand++) {
            Vec3 last = null;
            for (int k = 0; k <= STEPS; k++) {
                float share = k / (float) STEPS * grow;
                float twist = age * 0.35F + strand * Mth.TWO_PI / 3.0F + share * 9.0F;
                float swell = Mth.sin(share * Mth.PI) * 0.22F;
                double sag = -Mth.sin(share * Mth.PI) * Math.min(1.2, length * 0.06);
                Vec3 p = from.add(along.scale(share)).add(side.scale(Mth.cos(twist) * swell))
                        .add(up.scale(Mth.sin(twist) * swell + sag));
                if (last != null) {
                    Additive.ribbon(pose.last(), glow, last, p, eye, 0.07F, Additive.rgb(Wizard.VOID, 0.55F),
                            Additive.rgb(Wizard.VOID, 0.55F));
                }
                last = p;
            }
        }
        for (int w = 0; w < 4; w++) {
            float run = 1.0F - (age * 0.06F + w * 0.25F) % 1.0F;
            double sag = -Mth.sin(run * Mth.PI) * Math.min(1.2, length * 0.06);
            Vec3 p = from.add(along.scale(run)).add(up.scale(sag));
            Additive.glow(pose, glow, p, 0.28F, Additive.rgb(PALE, 0.7F), this.entityRenderDispatcher
                    .cameraOrientation());
        }
        Additive.glow(pose, glow, to, 0.7F, Additive.rgb(Wizard.VOID, 0.35F), this.entityRenderDispatcher
                .cameraOrientation());
    }

    @Override
    public ResourceLocation getTextureLocation(SoulDrain drain) {
        return Additive.ORB;
    }
}
