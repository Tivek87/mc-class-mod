package nl.tivek.multiversepowers.creature.elemental.client;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.elemental.BlazeCasts;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.creature.elemental.spell.StormArc;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.engine.client.fx.Energy;

// Chain lightning in the storm rod's yellow: a jagged bolt with a white-hot core from the rod to the first struck and
// on from each to the next, new jags every tick, forks splitting off; each leap flashes as it strikes and flickers out.
// With no one struck, one bolt cracks into the ground.
final class StormArcRenderer extends EntityRenderer<StormArc> {
    private static final int JAGS = 9;
    private static final int FADE = 7;

    StormArcRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(StormArc arc, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(StormArc arc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        Entity caster = arc.caster();
        if (caster == null) {
            return;
        }
        Vec3 origin = arc.getPosition(partialTick);
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        Entity first = arc.struck(0);
        Vec3 aimed = first == null ? arc.miss() : first.getPosition(partialTick).add(0.0, first.getBbHeight() * 0.55,
                0.0);
        Vec3 from = ElementalClient.start(caster, BlazeCasts.tip(caster, partialTick).subtract(origin),
                aimed.subtract(origin));
        float age = arc.tickCount + partialTick;
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        PoseStack.Pose at = pose.last();
        RandomSource random = RandomSource.create(arc.getId() * 31L + arc.tickCount);
        var facing = this.entityRenderDispatcher.cameraOrientation();
        // Every leg of the chain that still burns: from, to and how strongly, for the energy drawn over the bolts.
        List<Vec3> legs = new ArrayList<>();
        List<Float> strengths = new ArrayList<>();
        if (first == null) {
            float strength = Mth.clamp(1.0F - age / FADE, 0.0F, 1.0F);
            if (strength > 0.0F) {
                Vec3 to = arc.miss().subtract(origin);
                this.bolt(at, buffer, from, to, eye, strength, random, true);
                legs.add(from);
                legs.add(to);
                strengths.add(strength);
            }
        }
        for (int i = 0; i < StormArc.MOST && first != null; i++) {
            Entity struck = arc.struck(i);
            if (struck == null) {
                break;
            }
            float since = age - (i + 1) * StormArc.STEP + 1.0F;
            if (since < 0.0F) {
                break;
            }
            Vec3 to = struck.getPosition(partialTick).add(0.0, struck.getBbHeight() * 0.55, 0.0).subtract(origin);
            float strength = Mth.clamp(1.0F - since / FADE, 0.0F, 1.0F) * (0.75F + 0.25F * random.nextFloat());
            if (strength > 0.0F) {
                this.bolt(at, buffer, from, to, eye, strength, random, true);
                legs.add(from);
                legs.add(to);
                strengths.add(strength);
            }
            from = to;
        }
        VertexConsumer energy = Energy.buffer(buffers);
        for (int i = 0; i < strengths.size() && energy != null; i++) {
            Vec3 to = legs.get(i * 2 + 1);
            float strength = strengths.get(i);
            Energy.band(at, energy, legs.get(i * 2), to, eye, 0.5F, Energy.BOLT, Element.STORM.colour, strength,
                    arc.getId() + i);
            Energy.puff(pose, energy, to, 1.1F * strength, facing, Energy.SPARK, Element.STORM.colour, strength,
                    arc.getId() + i);
        }
    }

    // One jagged bolt, and now and then a fork off it.
    private void bolt(PoseStack.Pose at, VertexConsumer buffer, Vec3 from, Vec3 to, Vec3 eye, float strength,
            RandomSource random, boolean forks) {
        Vec3 way = to.subtract(from);
        double length = way.length();
        double jag = Math.min(0.6, length * 0.08);
        Vec3 previous = from;
        for (int k = 1; k <= JAGS; k++) {
            Vec3 next = from.add(way.scale(k / (double) JAGS));
            if (k < JAGS) {
                next = next.add((random.nextDouble() - 0.5) * jag * 2.0, (random.nextDouble() - 0.5) * jag * 2.0,
                        (random.nextDouble() - 0.5) * jag * 2.0);
            }
            Additive.ribbon(at, buffer, previous, next, eye, 0.3F, Additive.rgb(Element.STORM.colour,
                    0.3F * strength), Additive.rgb(Element.STORM.colour, 0.3F * strength));
            Additive.ribbon(at, buffer, previous, next, eye, 0.08F, Additive.rgb(0xFFFFFF, strength),
                    Additive.rgb(Element.STORM.light, strength));
            if (forks && k > 1 && k < JAGS - 1 && random.nextInt(4) == 0) {
                Vec3 off = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.6, random.nextDouble() - 0.5)
                        .normalize().scale(length * 0.25);
                this.bolt(at, buffer, next, next.add(off), eye, strength * 0.6F, random, false);
            }
            previous = next;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(StormArc arc) {
        return Additive.ORB;
    }
}
