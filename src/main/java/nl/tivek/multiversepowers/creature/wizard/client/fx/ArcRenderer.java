package nl.tivek.multiversepowers.creature.wizard.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcLightning;
import nl.tivek.multiversepowers.creature.wizard.staff.WizardStaff;

// Chain lightning: from his crystal to the first he struck and on from each to the next, a jagged bolt with a white-hot
// core in a blue-white glow, new jags every tick, a fork splitting off here and there; each leap shows the moment it
// strikes and flickers out after.
public final class ArcRenderer extends EntityRenderer<ArcLightning> {
    private static final int JAGS = 9;
    private static final int FADE = 7;

    public ArcRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(ArcLightning arc, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(ArcLightning arc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        Entity caster = arc.caster();
        if (caster == null) {
            return;
        }
        Vec3 origin = arc.getPosition(partialTick);
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        Vec3 from = (caster instanceof Wizard wizard ? wizard.crystal()
                : caster instanceof WizardStaff staff ? staff.tip() : caster.getEyePosition(partialTick))
                .subtract(origin);
        float age = arc.tickCount + partialTick;
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        PoseStack.Pose at = pose.last();
        RandomSource random = RandomSource.create(arc.getId() * 31L + arc.tickCount);
        for (int i = 0; i < ArcLightning.MOST; i++) {
            Entity struck = arc.struck(i);
            if (struck == null) {
                break;
            }
            float since = age - (i + 1) * ArcLightning.STEP + 1.0F;
            if (since < 0.0F) {
                break;
            }
            Vec3 to = struck.getPosition(partialTick).add(0.0, struck.getBbHeight() * 0.55, 0.0).subtract(origin);
            float strength = Mth.clamp(1.0F - since / FADE, 0.0F, 1.0F) * (0.75F + 0.25F * random.nextFloat());
            if (strength > 0.0F) {
                this.bolt(at, buffer, from, to, eye, strength, random, true);
            }
            from = to;
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
            Additive.ribbon(at, buffer, previous, next, eye, 0.42F, Additive.rgb(Wizard.STORM, 0.45F * strength),
                    Additive.rgb(Wizard.STORM, 0.45F * strength));
            Additive.ribbon(at, buffer, previous, next, eye, 0.1F, Additive.rgb(0xFFFFFF, strength),
                    Additive.rgb(0xFFFFFF, strength));
            if (forks && k > 1 && k < JAGS - 1 && random.nextInt(4) == 0) {
                Vec3 off = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.6, random.nextDouble() - 0.5)
                        .normalize().scale(length * 0.25);
                this.bolt(at, buffer, next, next.add(off), eye, strength * 0.6F, random, false);
            }
            previous = next;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ArcLightning arc) {
        return Additive.ORB;
    }
}
