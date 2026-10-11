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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.creature.wizard.abyss.Supernova;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.ScreenFlash;
import nl.tivek.multiversepowers.engine.client.fx.Singularity;

// The supernova: a star kindled over his staff, growing from a spark to a sun many blocks across, its white core
// seething and its corona of rays turning, rings of light drawn in to it faster and faster as it nears its end, and it
// trembles. Then it bursts: a blinding flash for whoever sees it (nothing for whoever hides from it), the ground
// shaking, a shell of light and a ring of bent light racing out across the sky, and the star is gone.
public final class SupernovaRenderer extends EntityRenderer<Supernova> {
    private static final int WHITE = 0xFFFDF2;
    private static final int GOLD = 0xFFD27A;
    private static final int CORONA = 0xFF9A3C;
    private static final float BIGGEST = 3.6F;
    private int flashed = -1;

    public SupernovaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(Supernova star, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(Supernova star, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = star.tickCount + partialTick;
        Vec3 middle = star.getPosition(partialTick);
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition();
        if (age < Supernova.BURST) {
            this.grow(star, age, pose, glow, eye.subtract(middle));
            return;
        }
        float after = age - Supernova.BURST;
        if (this.flashed != star.getId()) {
            this.flashed = star.getId();
            double far = eye.distanceTo(middle);
            boolean seen = star.level().clip(new ClipContext(eye, middle, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
            double near = Mth.clamp(1.0 - far / (Supernova.REACH * 2.0), 0.0, 1.0);
            if (seen) {
                ScreenFlash.add(WHITE, (float) (0.4 + 0.55 * near), 26);
            }
            CameraShake.add((float) (5.0 * near), 30);
        }
        float out = after / (Supernova.LIFE - Supernova.BURST);
        float fade = 1.0F - out;
        if (Singularity.available()) {
            Singularity.wave(middle, 4.0 + 70.0 * out, fade, WHITE);
        }
        float shell = BIGGEST + 40.0F * out;
        Additive.shell(pose.last(), glow, shell, shell * 0.8F, 10, 20, eye.subtract(middle),
                (ring, side, rim) -> Additive.rgb(ring % 2 == 0 ? WHITE : GOLD, (0.05F + 0.6F * rim * rim) * fade),
                0.0F, 0.0F);
        Additive.glow(pose, glow, Vec3.ZERO, BIGGEST * (1.0F + 2.0F * out) * 1.6F, Additive.rgb(WHITE, fade),
                this.entityRenderDispatcher.cameraOrientation());
        Additive.ring(pose.last(), glow, shell * 0.9F, shell * 1.05F, 0.0F, 64, Additive.rgb(GOLD, 0.6F * fade),
                Additive.rgb(0, 0.0F));
    }

    // The star while it grows.
    private void grow(Supernova star, float age, PoseStack pose, VertexConsumer glow, Vec3 eye) {
        float grown = Supernova.grown(age);
        float tremble = 1.0F + 0.06F * grown * Mth.sin(age * 2.9F) + 0.03F * Mth.sin(age * 7.1F);
        float r = (0.3F + (BIGGEST - 0.3F) * grown) * tremble;
        Additive.shell(pose.last(), glow, r, r, 8, 16, eye,
                (ring, side, rim) -> Additive.rgb((ring + side) % 3 == 0 ? GOLD : WHITE, 0.55F + 0.45F * rim), 0.0F,
                0.0F);
        Additive.glow(pose, glow, Vec3.ZERO, r * 2.2F, Additive.rgb(GOLD, 0.7F),
                this.entityRenderDispatcher.cameraOrientation());
        Additive.glow(pose, glow, Vec3.ZERO, r * 4.0F, Additive.rgb(CORONA, 0.35F),
                this.entityRenderDispatcher.cameraOrientation());
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.ZP.rotation(age * 0.02F));
        for (int k = 0; k < 16; k++) {
            float angle = k * Mth.TWO_PI / 16;
            float length = r * (1.6F + 0.8F * Mth.sin(age * 0.3F + k * 1.9F));
            Vec3 a = new Vec3(Mth.cos(angle) * r * 0.9F, Mth.sin(angle) * r * 0.9F, 0.0);
            Vec3 b = new Vec3(Mth.cos(angle) * length, Mth.sin(angle) * length, 0.0);
            Additive.ribbon(pose.last(), glow, a, b, new Vec3(0.0, 0.0, 10.0), r * 0.25F,
                    Additive.rgb(WHITE, 0.6F), Additive.rgb(CORONA, 0.0F));
        }
        pose.popPose();
        for (int k = 0; k < 3; k++) {
            float run = (age * (0.02F + 0.03F * grown) + k / 3.0F) % 1.0F;
            float ringR = r * (6.0F - 4.8F * run);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotation(0.6F * k - 0.6F));
            pose.mulPose(Axis.YP.rotation(age * 0.05F + k));
            Additive.ring(pose.last(), glow, ringR * 0.96F, ringR, 0.0F, 48, Additive.rgb(GOLD, 0.6F * run),
                    Additive.rgb(WHITE, 0.4F * run));
            pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(Supernova star) {
        return Additive.ORB;
    }
}
