package nl.tivek.multiversepowers.creature.elemental.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.elemental.BlazeTimeline;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.creature.elemental.ElementalBlaze;
import nl.tivek.multiversepowers.creature.elemental.RodOrbit;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.engine.math.Ease;

// The Elemental Blaze drawn in full light as a blaze is: its own model, then its rods' own glow (the fire rod's flame,
// the storm rod's sparks, the ice rod's shine), its eyes lit in the colour of the element it uses, and the rods of the
// element it casts burning brighter as they swing round, a soft light gathering at the leading rod. Awake, a haze of
// its element's colour hangs round it.
public final class ElementalBlazeRenderer extends MobRenderer<ElementalBlaze, ElementalBlazeModel> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "elemental_blaze"), "main");
    private static final ResourceLocation TEXTURE = texture("elemental_blaze");
    private static final RenderType GLOW = RenderType.eyes(texture("elemental_blaze_glow"));
    private static final RenderType EYES = RenderType.eyes(texture("elemental_blaze_eyes"));
    private static final RenderType BRIGHT = RenderType.eyes(TEXTURE);
    // The awakening's rings of light: how many ticks they race out, and how many sides they are drawn with.
    private static final float WAVE = 12.0F;
    private static final int RING = 28;
    private final float[] place = new float[6];

    ElementalBlazeRenderer(EntityRendererProvider.Context context) {
        super(context, new ElementalBlazeModel(context.bakeLayer(LAYER)), 0.5F);
        this.addLayer(new Glow(this));
    }

    static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "textures/entity/" + name + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(ElementalBlaze blaze) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(ElementalBlaze blaze, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(ElementalBlaze blaze, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        super.render(blaze, yaw, partialTick, pose, buffers, light);
        if (blaze.isInvisible() || blaze.deathTime > 0) {
            return;
        }
        RodOrbit.State state = blaze.orbit(partialTick);
        Element element = blaze.element();
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        var facing = this.entityRenderDispatcher.cameraOrientation();
        float flicker = 0.85F + 0.15F * Mth.sin(state.age() * 0.7F);
        int lead = RodOrbit.leader(element);
        float glow = RodOrbit.glow(state, element, 0);
        if (glow > 0.02F && blaze.move() != BlazeTimeline.AWAKEN) {
            RodOrbit.place(state, lead, this.place);
            float body = Mth.rotLerp(partialTick, blaze.yBodyRotO, blaze.yBodyRot);
            Vec3 at = RodOrbit.world(this.place, Vec3.ZERO, body);
            Additive.glow(pose, buffer, at, 0.45F + 0.35F * glow, Additive.rgb(element.light, 0.9F * glow * flicker),
                    facing);
            Additive.glow(pose, buffer, at, 1.1F * glow, Additive.rgb(element.colour, 0.5F * glow), facing);
        }
        if (blaze.move() == BlazeTimeline.AWAKEN) {
            float burst = (float) Ease.bump((state.moveAge() - BlazeTimeline.BURST) / 10.0);
            for (Element each : Element.ALL) {
                Additive.glow(pose, buffer, new Vec3(0.0, 1.0, 0.0), 0.8F + 2.6F * burst * (1.0F + each.ordinal()
                        * 0.12F), Additive.rgb(each.colour, 0.35F * burst), facing);
            }
            float out = (state.moveAge() - BlazeTimeline.BURST) / WAVE;
            if (out >= 0.0F && out < 1.0F) {
                float reach = 1.0F - (1.0F - out) * (1.0F - out);
                for (Element each : Element.ALL) {
                    ring(pose.last(), buffer, 0.9F + each.ordinal() * 0.08F, 0.5F + (3.6F + each.ordinal() * 0.6F)
                            * reach, 0.25F + 0.5F * out, Additive.rgb(each.light, 0.8F * (1.0F - out)));
                }
            }
        }
        if (state.awake() >= 0.0F) {
            Additive.glow(pose, buffer, new Vec3(0.0, 1.0, 0.0), 1.6F, Additive.rgb(element.colour, 0.22F * flicker),
                    facing);
        }
    }

    // A flat ring of light `radius` wide round its middle, fading to nothing `width` to either side, seen from above
    // and below.
    private static void ring(PoseStack.Pose at, VertexConsumer buffer, float y, float radius, float width,
            float[] colour) {
        float[] none = Additive.rgb(0, 0.0F);
        for (int i = 0; i < RING; i++) {
            float a0 = i * Mth.TWO_PI / RING;
            float a1 = (i + 1) * Mth.TWO_PI / RING;
            float[] m0 = rim(a0, radius, y);
            float[] m1 = rim(a1, radius, y);
            for (float side : new float[] {-width, width}) {
                float[] e0 = rim(a0, radius + side, y);
                float[] e1 = rim(a1, radius + side, y);
                Additive.quad(at, buffer, m0, m1, e1, e0, colour, colour, none, none);
                Additive.quad(at, buffer, e0, e1, m1, m0, none, none, colour, colour);
            }
        }
    }

    private static float[] rim(float angle, float radius, float y) {
        return new float[] {Mth.cos(angle) * radius, y, Mth.sin(angle) * radius};
    }

    // The rods' own glow and the eyes over the body in full light, then the rods of the element it casts once more,
    // as bright as they burn now.
    private static final class Glow extends RenderLayer<ElementalBlaze, ElementalBlazeModel> {
        Glow(LivingEntityRenderer<ElementalBlaze, ElementalBlazeModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, ElementalBlaze blaze,
                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                float headPitch) {
            if (blaze.isInvisible()) {
                return;
            }
            ElementalBlazeModel model = this.getParentModel();
            float dim = blaze.deathTime > 0 ? Math.max(0.0F, 1.0F - (blaze.deathTime + partialTick) / 20.0F) : 1.0F;
            float flicker = 0.8F + 0.2F * Mth.sin(ageInTicks * 0.9F) * Mth.sin(ageInTicks * 0.37F);
            model.renderToBuffer(pose, buffers.getBuffer(GLOW), 0xF000F0, OverlayTexture.NO_OVERLAY,
                    grey(flicker * dim));
            Element element = blaze.element();
            int eyes = FastColor.ARGB32.color(255, Math.round((element.light >> 16 & 255) * dim),
                    Math.round((element.light >> 8 & 255) * dim), Math.round((element.light & 255) * dim));
            model.head.render(pose, buffers.getBuffer(EYES), 0xF000F0, OverlayTexture.NO_OVERLAY, eyes);
            VertexConsumer bright = buffers.getBuffer(BRIGHT);
            for (int i = 0; i < RodOrbit.RODS; i++) {
                if (model.glow[i] > 0.02F) {
                    model.rods[i].render(pose, bright, 0xF000F0, OverlayTexture.NO_OVERLAY,
                            grey(model.glow[i] * dim));
                }
            }
        }

        private static int grey(float strength) {
            int c = Math.round(Mth.clamp(strength, 0.0F, 1.0F) * 255.0F);
            return FastColor.ARGB32.color(255, c, c, c);
        }
    }
}
