package nl.tivek.multiversepowers.creature.wizard.client.field;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.creature.wizard.field.RuneMine;

// A rune of the rune circle: it burns into the ground, spinning and brightening, flashing fast just before it goes
// off; then a pillar of violet light shoots up from it and a ring of light runs out over the ground, both fading.
public final class RuneRenderer extends EntityRenderer<RuneMine> {
    private static final int SIDES = 16;
    private static final float PILLAR_HEIGHT = 7.0F;

    public RuneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(RuneMine rune, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(RuneMine rune, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = rune.tickCount + partialTick;
        int fuse = rune.fuse();
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        PoseStack.Pose at = pose.last();
        float radius = (float) RuneMine.RADIUS;
        if (age < fuse) {
            float grow = Math.min(1.0F, age / 8.0F);
            float near = Mth.clamp(age / fuse, 0.0F, 1.0F);
            float flash = fuse - age < 8.0F ? 0.6F + 0.4F * Mth.sin(age * 2.5F) : 1.0F;
            Additive.glyph(at, buffer, radius * grow, age * (1.0F + 4.0F * near), Wizard.VIOLET,
                    (0.3F + 0.7F * near) * flash);
            return;
        }
        float since = age - fuse;
        float fade = Mth.clamp(1.0F - since / RuneMine.PILLAR, 0.0F, 1.0F);
        float wave = radius * (1.0F + since * 0.5F);
        Additive.ring(at, buffer, wave * 0.85F, wave, 0.05F, 32, Additive.rgb(Wizard.VIOLET, fade),
                Additive.rgb(Wizard.VIOLET, 0.0F));
        Additive.glyph(at, buffer, radius, age * 5.0F, Wizard.VIOLET, fade * 0.8F);
        float width = radius * (0.65F - 0.35F * (1.0F - fade));
        float height = PILLAR_HEIGHT * Math.min(1.0F, since / 2.0F);
        this.pillar(at, buffer, width, height, Additive.rgb(Wizard.VIOLET, 0.7F * fade));
        this.pillar(at, buffer, width * 0.4F, height * 1.1F, Additive.rgb(0xFFFFFF, 0.9F * fade));
    }

    // An open cylinder of light from the ground up, fading towards its top.
    private void pillar(PoseStack.Pose at, VertexConsumer buffer, float radius, float height, float[] colour) {
        float[] none = Additive.rgb(0, 0.0F);
        for (int i = 0; i < SIDES; i++) {
            float a0 = i * Mth.TWO_PI / SIDES;
            float a1 = (i + 1) * Mth.TWO_PI / SIDES;
            float x0 = Mth.cos(a0) * radius;
            float z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            Additive.quad(at, buffer, new float[] { x0, 0.0F, z0 }, new float[] { x1, 0.0F, z1 },
                    new float[] { x1 * 0.6F, height, z1 * 0.6F }, new float[] { x0 * 0.6F, height, z0 * 0.6F },
                    colour, colour, none, none);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(RuneMine rune) {
        return Additive.ORB;
    }
}
