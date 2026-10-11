package nl.tivek.multiversepowers.creature.wizard.client.fx;

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
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneBeam;

// The arcane beam: first a thin flickering line where it will burn; then a torrent of light from his crystal to where
// it strikes, a white core in layers of violet, rings of light racing along it, a blinding flare at the crystal and a
// splash of light where it ends.
public final class BeamRenderer extends EntityRenderer<ArcaneBeam> {
    private static final int RING_SIDES = 14;
    private static final double RING_GAP = 1.6;

    public BeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(ArcaneBeam beam, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(ArcaneBeam beam, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        Entity caster = beam.caster();
        Vec3 origin = beam.getPosition(partialTick);
        Vec3 from = (caster instanceof Wizard wizard ? wizard.crystal() : beam.position()).subtract(origin);
        Vec3 to = beam.end().subtract(origin);
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        float age = beam.tickCount + partialTick;
        float strength = beam.strength(partialTick);
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        PoseStack.Pose at = pose.last();
        if (age < ArcaneBeam.WARN) {
            float flicker = 0.5F + 0.5F * Mth.sin(age * 2.2F);
            Additive.ribbon(at, buffer, from, to, eye, 0.06F, Additive.rgb(0xFF6AD5, 0.9F * flicker),
                    Additive.rgb(0xFF6AD5, 0.4F * flicker));
            return;
        }
        float pulse = 1.0F + 0.08F * Mth.sin(age * 1.7F);
        float width = strength * pulse;
        Additive.ribbon(at, buffer, from, to, eye, 2.6F * width, Additive.rgb(Wizard.VIOLET, 0.18F * strength),
                Additive.rgb(Wizard.VIOLET, 0.14F * strength));
        Additive.ribbon(at, buffer, from, to, eye, 1.3F * width, Additive.rgb(Wizard.VIOLET, 0.6F * strength),
                Additive.rgb(Wizard.VIOLET, 0.5F * strength));
        Additive.ribbon(at, buffer, from, to, eye, 0.55F * width, Additive.rgb(0xF0DEFF, 0.9F * strength),
                Additive.rgb(0xF0DEFF, 0.8F * strength));
        Additive.ribbon(at, buffer, from, to, eye, 0.22F * width, Additive.rgb(0xFFFFFF, strength),
                Additive.rgb(0xFFFFFF, strength));
        this.rings(at, buffer, from, to, age, strength);
        var facing = this.entityRenderDispatcher.cameraOrientation();
        Additive.glow(pose, buffer, from, 0.9F * width + 0.3F, Additive.rgb(0xFFFFFF, 0.9F * strength), facing);
        Additive.glow(pose, buffer, from, 2.2F * width, Additive.rgb(Wizard.VIOLET, 0.6F * strength), facing);
        Additive.glow(pose, buffer, to, 1.8F * width, Additive.rgb(Wizard.VIOLET, 0.8F * strength), facing);
        Additive.glow(pose, buffer, to, 0.8F * width, Additive.rgb(0xFFFFFF, 0.9F * strength), facing);
    }

    // Rings of light round the beam racing out along it.
    private void rings(PoseStack.Pose at, VertexConsumer buffer, Vec3 from, Vec3 to, float age, float strength) {
        Vec3 way = to.subtract(from);
        double length = way.length();
        if (length < 0.5) {
            return;
        }
        Vec3 along = way.scale(1.0 / length);
        Vec3 side = Math.abs(along.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 u = along.cross(side).normalize();
        Vec3 v = along.cross(u).normalize();
        double offset = (age * 0.6) % RING_GAP;
        for (double d = offset; d < length; d += RING_GAP) {
            Vec3 middle = from.add(along.scale(d));
            float r = (float) (0.55 + 0.15 * Math.sin(d * 1.3 + age * 0.4)) * strength;
            float fade = (float) (1.0 - d / length * 0.6) * strength;
            float[] colour = Additive.rgb(0xD9B8FF, 0.7F * fade);
            for (int i = 0; i < RING_SIDES; i++) {
                double a0 = i * Mth.TWO_PI / RING_SIDES;
                double a1 = (i + 1) * Mth.TWO_PI / RING_SIDES;
                Vec3 p0 = middle.add(u.scale(Math.cos(a0) * r)).add(v.scale(Math.sin(a0) * r));
                Vec3 p1 = middle.add(u.scale(Math.cos(a1) * r)).add(v.scale(Math.sin(a1) * r));
                Vec3 q0 = middle.add(u.scale(Math.cos(a0) * r * 1.25)).add(v.scale(Math.sin(a0) * r * 1.25))
                        .add(along.scale(-0.1));
                Vec3 q1 = middle.add(u.scale(Math.cos(a1) * r * 1.25)).add(v.scale(Math.sin(a1) * r * 1.25))
                        .add(along.scale(-0.1));
                Additive.quad(at, buffer, point(p0), point(p1), point(q1), point(q0), colour, colour,
                        Additive.rgb(0, 0.0F), Additive.rgb(0, 0.0F));
            }
        }
    }

    private static float[] point(Vec3 p) {
        return new float[] { (float) p.x, (float) p.y, (float) p.z };
    }

    @Override
    public ResourceLocation getTextureLocation(ArcaneBeam beam) {
        return Additive.ORB;
    }
}
