package nl.tivek.multiversepowers.creature.wizard.client.field;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.field.StarRift;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.engine.client.fx.Cosmos;

// The Wizard's rift into the night sky: a ragged round window, facing down, into a deep sky of stars (Cosmos), its rim
// burning violet and white, three arms of light wheeling in it and a faint shaft of its light falling to the ground.
// It swirls open from nothing and closes again the same way.
public final class RiftRenderer extends EntityRenderer<StarRift> {
    private static final int SIDES = 48;
    private static final int DEEP = 0x07031A;
    private static final int LIGHT = 0xD9C2FF;
    private static final double SHAFT = 9.0;

    public RiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(StarRift rift, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(StarRift rift, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float open = rift.openness(partialTick);
        if (open <= 0.001F) {
            return;
        }
        float age = rift.tickCount + partialTick;
        double radius = StarRift.RADIUS * open * (0.85 + 0.15 * open);
        Vec3 center = rift.getPosition(partialTick);
        List<Vec3> outline = new ArrayList<>(SIDES);
        float[] reach = new float[SIDES];
        for (int i = 0; i < SIDES; i++) {
            double a = i * Mth.TWO_PI / SIDES;
            reach[i] = (float) (radius * (1.0 + 0.07 * Math.sin(3.0 * a + age * 0.11) + 0.04 * Math.sin(7.0 * a
                    - age * 0.17)));
            outline.add(center.add(Math.cos(a) * reach[i], 0.0, Math.sin(a) * reach[i]));
        }
        Cosmos.window(center, new Vec3(0.0, -1.0, 0.0), new Vec3(1.0, 0.0, 0.0), outline, 4.0, DEEP, LIGHT,
                0.8 + 0.4 * open, rift.getId());
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        PoseStack.Pose at = pose.last();
        float lower = -0.02F;
        for (int i = 0; i < SIDES; i++) {
            int j = (i + 1) % SIDES;
            double a0 = i * Mth.TWO_PI / SIDES;
            double a1 = j * Mth.TWO_PI / SIDES;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float flicker = 0.75F + 0.25F * Mth.sin(age * 0.6F + i * 1.7F);
            // The burning rim: white at its edge, violet outward.
            ring(at, buffer, c0, s0, c1, s1, reach[i], reach[j], 0.94F, 1.04F, lower,
                    Additive.rgb(0xFFFFFF, 0.9F * open * flicker), Additive.rgb(Wizard.VIOLET, 0.9F * open));
            ring(at, buffer, c0, s0, c1, s1, reach[i], reach[j], 1.04F, 1.45F, lower,
                    Additive.rgb(Wizard.VIOLET, 0.75F * open * flicker), Additive.rgb(0, 0.0F));
            // Its light falling to the ground, fading as it goes.
            float[] top0 = { c0 * reach[i], lower, s0 * reach[i] };
            float[] top1 = { c1 * reach[j], lower, s1 * reach[j] };
            float spread = 1.15F;
            float[] low0 = { c0 * reach[i] * spread, (float) -SHAFT, s0 * reach[i] * spread };
            float[] low1 = { c1 * reach[j] * spread, (float) -SHAFT, s1 * reach[j] * spread };
            float[] shaft = Additive.rgb(Wizard.VIOLET, 0.13F * open);
            float[] none = Additive.rgb(0, 0.0F);
            Additive.quad(at, buffer, top0, top1, low1, low0, shaft, shaft, none, none);
        }
        // Three arms of light wheeling in towards its middle.
        for (int arm = 0; arm < 3; arm++) {
            for (int k = 0; k < 10; k++) {
                float f0 = k / 10.0F;
                float f1 = (k + 1) / 10.0F;
                float[] p0 = spiral(arm, f0, age, radius);
                float[] p1 = spiral(arm, f1, age, radius);
                float w0 = (float) radius * 0.09F * (1.0F - f0 * 0.7F);
                float w1 = (float) radius * 0.09F * (1.0F - f1 * 0.7F);
                float[] c = Additive.rgb(LIGHT, 0.45F * open * (1.0F - f0));
                float[] d = Additive.rgb(LIGHT, 0.45F * open * (1.0F - f1));
                Additive.quad(at, buffer, new float[] { p0[0] + p0[2] * w0, lower * 2.0F, p0[1] + p0[3] * w0 },
                        new float[] { p0[0] - p0[2] * w0, lower * 2.0F, p0[1] - p0[3] * w0 },
                        new float[] { p1[0] - p1[2] * w1, lower * 2.0F, p1[1] - p1[3] * w1 },
                        new float[] { p1[0] + p1[2] * w1, lower * 2.0F, p1[1] + p1[3] * w1 }, c, c, d, d);
            }
        }
        super.render(rift, yaw, partialTick, pose, buffers, light);
    }

    // A point along one of the arms, from the rim (0) in to near the middle (1), and the way across it there.
    private static float[] spiral(int arm, float along, float age, double radius) {
        double a = arm * Mth.TWO_PI / 3.0 + along * 2.4 + age * 0.06;
        double r = radius * (0.98 - 0.8 * along);
        float x = (float) (Math.cos(a) * r);
        float z = (float) (Math.sin(a) * r);
        return new float[] { x, z, (float) -Math.sin(a), (float) Math.cos(a) };
    }

    private static void ring(PoseStack.Pose at, VertexConsumer buffer, float c0, float s0, float c1, float s1,
            float r0, float r1, float from, float to, float y, float[] inner, float[] outer) {
        Additive.quad(at, buffer, new float[] { c0 * r0 * from, y, s0 * r0 * from },
                new float[] { c1 * r1 * from, y, s1 * r1 * from }, new float[] { c1 * r1 * to, y, s1 * r1 * to },
                new float[] { c0 * r0 * to, y, s0 * r0 * to }, inner, inner, outer, outer);
    }

    @Override
    public ResourceLocation getTextureLocation(StarRift rift) {
        return Additive.ORB;
    }
}
