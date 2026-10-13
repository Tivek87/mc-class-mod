package nl.tivek.multiversepowers.creature.elemental.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.elemental.BlazeCasts;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.creature.elemental.spell.FrostRay;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.engine.client.fx.Energy;

// The frost ray: a thin flicker as it starts, then a beam of cold light from the ice rod to where it ends, a white core
// in a pale blue haze, six-sided rings of frost turning as they race along it, and a burst of light and spinning
// crystals of ice where it strikes.
final class FrostRayRenderer extends EntityRenderer<FrostRay> {
    private static final int SIDES = 6;
    private static final double GAP = 1.3;

    FrostRayRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(FrostRay ray, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(FrostRay ray, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        Entity caster = ray.caster();
        if (caster == null) {
            return;
        }
        Vec3 origin = ray.getPosition(partialTick);
        Vec3 to = ray.end(partialTick).subtract(origin);
        Vec3 from = ElementalClient.start(caster, BlazeCasts.tip(caster, partialTick).subtract(origin), to);
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        float age = ray.tickCount + partialTick;
        float strength = ray.strength(partialTick);
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        PoseStack.Pose at = pose.last();
        int ice = Element.ICE.colour;
        int frost = Element.ICE.light;
        if (age < FrostRay.WARM) {
            float flicker = 0.5F + 0.5F * Mth.sin(age * 3.0F);
            Additive.ribbon(at, buffer, from, to, eye, 0.08F, Additive.rgb(frost, flicker), Additive.rgb(ice,
                    0.5F * flicker));
            return;
        }
        float width = strength * (1.0F + 0.06F * Mth.sin(age * 1.9F));
        Additive.ribbon(at, buffer, from, to, eye, 0.14F * width, Additive.rgb(0xFFFFFF, 0.9F * strength),
                Additive.rgb(frost, 0.8F * strength));
        this.rings(at, buffer, from, to, age, strength);
        var facing = this.entityRenderDispatcher.cameraOrientation();
        for (int i = 0; i < 3; i++) {
            pose.pushPose();
            pose.translate(to.x, to.y, to.z);
            pose.mulPose(Axis.YP.rotation(age * 0.3F + i * 2.1F));
            pose.mulPose(Axis.ZP.rotation(0.6F + i * 1.3F));
            Additive.spindle(pose.last(), buffer, 0.7F * width, 0.08F * width, 0.5F, 4, Additive.rgb(frost,
                    0.8F * strength), Additive.rgb(ice, 0.2F * strength));
            pose.popPose();
        }
        // Over the beam's light: the cold itself streaming down it, and a star of frost where it strikes.
        VertexConsumer energy = Energy.buffer(buffers);
        if (energy != null) {
            Energy.band(pose.last(), energy, from, to, eye, 0.55F * width, Energy.STREAM, ice, 0.9F * strength,
                    ray.getId());
            Energy.puff(pose, energy, to, 1.5F * width, facing, Energy.FROST, ice, strength, ray.getId());
        }
    }

    // Six-sided rings of frost racing out along the beam, each turning as it goes.
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
        double offset = (age * 0.5) % GAP;
        for (double d = offset; d < length; d += GAP) {
            Vec3 middle = from.add(along.scale(d));
            float r = (float) (0.3 + 0.08 * Math.sin(d * 1.7 + age * 0.5)) * strength;
            float fade = (float) (1.0 - d / length * 0.5) * strength;
            float[] colour = Additive.rgb(Element.ICE.light, 0.75F * fade);
            float[] none = Additive.rgb(0, 0.0F);
            double turn = age * 0.2 + d;
            for (int i = 0; i < SIDES; i++) {
                double a0 = turn + i * Mth.TWO_PI / SIDES;
                double a1 = turn + (i + 1) * Mth.TWO_PI / SIDES;
                Vec3 p0 = middle.add(u.scale(Math.cos(a0) * r)).add(v.scale(Math.sin(a0) * r));
                Vec3 p1 = middle.add(u.scale(Math.cos(a1) * r)).add(v.scale(Math.sin(a1) * r));
                Vec3 q0 = middle.add(u.scale(Math.cos(a0) * r * 1.4)).add(v.scale(Math.sin(a0) * r * 1.4));
                Vec3 q1 = middle.add(u.scale(Math.cos(a1) * r * 1.4)).add(v.scale(Math.sin(a1) * r * 1.4));
                Additive.quad(at, buffer, point(p0), point(p1), point(q1), point(q0), colour, colour, none, none);
            }
        }
    }

    private static float[] point(Vec3 p) {
        return new float[] { (float) p.x, (float) p.y, (float) p.z };
    }

    @Override
    public ResourceLocation getTextureLocation(FrostRay ray) {
        return Additive.ORB;
    }
}
