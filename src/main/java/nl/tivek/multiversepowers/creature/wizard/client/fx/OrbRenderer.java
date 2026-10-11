package nl.tivek.multiversepowers.creature.wizard.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

// One of the Wizard's spells in flight drawn as light: a white-hot core in a soft halo of its colour, always turned to
// the eye, with a fading tail of smaller glows behind it along the way it flies (longer for a falling star).
public final class OrbRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final int TAIL = 6;
    private final int colour;
    private final float size;
    private final float tail;

    public OrbRenderer(EntityRendererProvider.Context context, int colour, float size, float tail) {
        super(context);
        this.colour = colour;
        this.size = size;
        this.tail = tail;
    }

    @Override
    public void render(T orb, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        Vec3 v = orb.getDeltaMovement();
        float age = orb.tickCount + partialTick;
        float pulse = 1.0F + 0.12F * (float) Math.sin(age * 0.9F);
        float r = (this.colour >> 16 & 255) / 255.0F;
        float g = (this.colour >> 8 & 255) / 255.0F;
        float b = (this.colour & 255) / 255.0F;
        float middle = orb.getBbHeight() * 0.5F;
        for (int k = TAIL; k >= 0; k--) {
            float back = k * this.tail / TAIL;
            float fade = 1.0F - (float) k / (TAIL + 1);
            pose.pushPose();
            pose.translate(-v.x * back, middle - v.y * back, -v.z * back);
            pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
            pose.mulPose(Axis.ZP.rotation(age * 0.15F + k));
            float halo = this.size * pulse * (0.45F + 0.55F * fade);
            quad(pose, buffer, halo, r * fade, g * fade, b * fade);
            if (k == 0) {
                quad(pose, buffer, this.size * 0.45F * pulse, 1.0F, 1.0F, 1.0F);
                quad(pose, buffer, this.size * 0.7F, r * 0.8F, g * 0.8F, b * 0.8F);
            }
            pose.popPose();
        }
        super.render(orb, yaw, partialTick, pose, buffers, light);
    }

    private static void quad(PoseStack pose, VertexConsumer buffer, float half, float r, float g, float b) {
        Matrix4f m = pose.last().pose();
        PoseStack.Pose normal = pose.last();
        vertex(buffer, m, normal, -half, -half, 0.0F, 1.0F, r, g, b);
        vertex(buffer, m, normal, half, -half, 1.0F, 1.0F, r, g, b);
        vertex(buffer, m, normal, half, half, 1.0F, 0.0F, r, g, b);
        vertex(buffer, m, normal, -half, half, 0.0F, 0.0F, r, g, b);
        // And its back, so it shows whichever way it is wound.
        vertex(buffer, m, normal, -half, half, 0.0F, 0.0F, r, g, b);
        vertex(buffer, m, normal, half, half, 1.0F, 0.0F, r, g, b);
        vertex(buffer, m, normal, half, -half, 1.0F, 1.0F, r, g, b);
        vertex(buffer, m, normal, -half, -half, 0.0F, 1.0F, r, g, b);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f m, PoseStack.Pose normal, float x, float y, float u,
            float v, float r, float g, float b) {
        buffer.addVertex(m, x, y, 0.0F).setColor(r, g, b, 1.0F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(normal, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(T orb) {
        return Additive.ORB;
    }
}
