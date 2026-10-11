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
import nl.tivek.multiversepowers.creature.wizard.field.ArcaneChains;

// The arcane chains: the circle of runes on the ground, pulsing faster as it is about to burst; then four chains of
// violet light shooting up out of four small circles round it, link after link, and fastening on whoever it holds, or
// lashing up at nothing and falling back.
public final class ChainsRenderer extends EntityRenderer<ArcaneChains> {
    private static final int ANCHORS = 4;
    private static final double ANCHOR = 2.3;
    private static final double LINK = 0.32;

    public ChainsRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(ArcaneChains chains, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(ArcaneChains chains, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = chains.tickCount + partialTick;
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        PoseStack.Pose at = pose.last();
        Vec3 origin = chains.getPosition(partialTick);
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        boolean erupted = age >= ArcaneChains.ERUPT;
        int end = ArcaneChains.ERUPT + (chains.holdsAny() ? chains.hold() : 12);
        float fade = Mth.clamp((end - age) / 6.0F, 0.0F, 1.0F);
        if (!erupted) {
            float near = age / ArcaneChains.ERUPT;
            float pulse = 0.6F + 0.4F * Mth.sin(age * (0.4F + near * 1.6F));
            Additive.glyph(at, buffer, (float) ArcaneChains.RADIUS * (0.6F + 0.4F * Math.min(1.0F, age / 5.0F)),
                    age * 2.0F, Wizard.VIOLET, (0.35F + 0.65F * near) * pulse);
            return;
        }
        Additive.glyph(at, buffer, (float) ArcaneChains.RADIUS, age * 2.0F, Wizard.VIOLET, 0.55F * fade);
        float since = age - ArcaneChains.ERUPT;
        float grow = Math.min(1.0F, since / 4.0F);
        for (int k = 0; k < ANCHORS; k++) {
            double angle = Math.PI / 4.0 + k * Math.PI / 2.0;
            Vec3 anchor = new Vec3(Math.cos(angle) * ANCHOR, 0.05, Math.sin(angle) * ANCHOR);
            pose.pushPose();
            pose.translate(anchor.x, 0.0, anchor.z);
            Additive.ring(pose.last(), buffer, 0.32F, 0.4F, 0.06F, 20, Additive.rgb(Wizard.VIOLET, fade),
                    Additive.rgb(Wizard.VIOLET, fade));
            pose.popPose();
            for (int i = 0; i < ArcaneChains.MOST; i++) {
                Entity held = chains.bound(i);
                Vec3 to;
                if (held != null) {
                    to = held.getPosition(partialTick).subtract(origin).add(0.0, held.getBbHeight() * 0.5, 0.0);
                } else if (i == 0 && !chains.holdsAny()) {
                    // Nobody to hold: they lash up at nothing and fall back.
                    float lash = Mth.sin(Math.min(1.0F, since / 12.0F) * Mth.PI);
                    to = new Vec3(anchor.x * 0.3, 0.3 + 2.6 * lash, anchor.z * 0.3);
                } else {
                    continue;
                }
                this.chain(at, buffer, anchor, anchor.lerp(to, grow), eye, fade, age + k * 3.0F);
            }
        }
    }

    // A chain of links from `from` to `to`, each link turned a quarter from the last.
    private void chain(PoseStack.Pose at, VertexConsumer buffer, Vec3 from, Vec3 to, Vec3 eye, float strength,
            float age) {
        Vec3 way = to.subtract(from);
        double length = way.length();
        if (length < 0.05) {
            return;
        }
        Vec3 along = way.scale(1.0 / length);
        Vec3 side = along.cross(new Vec3(0.0, 1.0, 0.0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        Vec3 other = along.cross(side).normalize();
        int links = (int) Math.ceil(length / LINK);
        float[] bright = Additive.rgb(0xE6CCFF, strength);
        float[] glow = Additive.rgb(Wizard.VIOLET, 0.55F * strength);
        Additive.ribbon(at, buffer, from, to, eye, 0.35F, glow, glow);
        for (int i = 0; i < links; i++) {
            double d0 = i * LINK;
            double d1 = Math.min(length, d0 + LINK * 1.1);
            Vec3 a = from.add(along.scale(d0));
            Vec3 b = from.add(along.scale(d1));
            Vec3 across = (i % 2 == 0 ? side : other).scale(0.07 + 0.01 * Math.sin(age * 0.6 + i));
            float flick = 0.8F + 0.2F * Mth.sin(age * 0.9F + i * 1.3F);
            float[] c = { bright[0] * flick, bright[1] * flick, bright[2] * flick };
            Additive.quad(at, buffer, point(a.add(across)), point(b.add(across)), point(b.subtract(across)),
                    point(a.subtract(across)), c, c, c, c);
        }
    }

    private static float[] point(Vec3 p) {
        return new float[] { (float) p.x, (float) p.y, (float) p.z };
    }

    @Override
    public ResourceLocation getTextureLocation(ArcaneChains chains) {
        return Additive.ORB;
    }
}
