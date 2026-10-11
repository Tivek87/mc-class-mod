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
import nl.tivek.multiversepowers.creature.wizard.abyss.VoidTendrils;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;

// The void tendrils: a pool of black opening on the ground with a magenta rim, the tips of tentacles squirming at its
// edge; then five black tentacles, sheened in the void's light, burst up out of it and wind round their foe's legs,
// squeezing and writhing as they hold. Missing, they lash up at nothing; let go, they slither back down into the pool,
// which closes.
public final class TendrilsRenderer extends EntityRenderer<VoidTendrils> {
    private static final int ARMS = 5;
    private static final int POINTS = 14;
    private static final float[] DARK = Additive.mist(0x140A1E, 1.0F);
    private static final float[] DARK_TIP = Additive.mist(0x2A1236, 1.0F);

    public TendrilsRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(VoidTendrils tendrils, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(VoidTendrils tendrils, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = tendrils.tickCount + partialTick;
        int letGo = tendrils.letGo();
        float open = Mth.clamp(age / VoidTendrils.GRAB, 0.0F, 1.0F);
        float sink = letGo < 0 ? 0.0F : Mth.clamp((age - letGo) / VoidTendrils.SINK, 0.0F, 1.0F);
        float pool = open * (1.0F - sink);
        Entity held = tendrils.held();
        float rise = age < VoidTendrils.GRAB ? 0.18F * open : Mth.clamp((age - VoidTendrils.GRAB) / 5.0F, 0.0F, 1.0F);
        float tall = held == null ? 1.7F : held.getBbHeight() * 0.55F;
        float wide = held == null ? 0.25F : held.getBbWidth() * 0.62F;
        Vec3[][] arms = new Vec3[ARMS][POINTS];
        float[][] thick = new float[ARMS][POINTS];
        for (int arm = 0; arm < ARMS; arm++) {
            Vec3[] points = arms[arm];
            float[] radii = thick[arm];
            float base = arm * Mth.TWO_PI / ARMS + 0.4F;
            float reach = Math.max(0.05F, rise * (1.0F - sink));
            for (int k = 0; k < POINTS; k++) {
                float share = k / (float) (POINTS - 1) * reach;
                float writhe = 0.06F * Mth.sin(age * 0.25F + arm * 1.3F + k * 0.7F);
                // Out of the pool's edge, up and round their legs, one turn and a half as it climbs.
                float angle = base + share * 4.5F * (arm % 2 == 0 ? 1.0F : -1.0F);
                float out = Mth.lerp(Math.min(1.0F, share * 3.0F), 1.0F, wide) + writhe;
                float up = held == null && age >= VoidTendrils.GRAB ? share * tall
                        : tall * Math.max(0.0F, share * 1.1F - 0.1F);
                points[k] = new Vec3(Mth.cos(angle) * out, up - 0.05F - sink * 0.8F, Mth.sin(angle) * out);
                radii[k] = Mth.lerp(share, 0.13F, 0.035F);
            }
        }
        VertexConsumer dark = buffers.getBuffer(Additive.MIST);
        Additive.ring(pose.last(), dark, 0.0F, 1.5F * pool, 0.03F, 32, Additive.mist(0x07020C, 0.92F),
                Additive.mist(0x07020C, 0.6F));
        for (int arm = 0; arm < ARMS; arm++) {
            Additive.tube(pose.last(), dark, arms[arm], thick[arm], 6, DARK, DARK_TIP);
        }
        // Taking another buffer ends the one before, so each kind is drawn whole before the next is taken.
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        Additive.ring(pose.last(), glow, 1.35F * pool, 1.6F * pool, 0.035F, 32, Additive.rgb(Wizard.VOID, 0.6F * pool),
                Additive.rgb(0, 0.0F));
        for (int arm = 0; arm < ARMS; arm++) {
            float[] sheen = new float[POINTS];
            for (int k = 0; k < POINTS; k++) {
                sheen[k] = thick[arm][k] * 1.18F;
            }
            float pulse = 0.18F + 0.1F * Mth.sin(age * 0.4F + arm);
            Additive.tube(pose.last(), glow, arms[arm], sheen, 6, Additive.rgb(Wizard.VOID, pulse),
                    Additive.rgb(0xFF9CF2, pulse * 1.4F));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(VoidTendrils tendrils) {
        return Additive.ORB;
    }
}
