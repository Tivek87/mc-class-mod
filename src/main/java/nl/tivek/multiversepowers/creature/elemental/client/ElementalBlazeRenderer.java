package nl.tivek.multiversepowers.creature.elemental.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import nl.tivek.multiversepowers.engine.client.fx.Energy;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;

// The Elemental Blaze, never drawn darker than a dim light of its own: its own model, then its rods' own glow (the
// fire rod's flame, the storm rod's sparks, the ice rod's shine), its eyes lit in the colour of the element it uses,
// and the rods of the element it casts burning brighter as they swing round. Its heart burns in the colour of its
// element, the crown's shard of the element it casts lights up, and the element itself plays round it in pixels, as
// the game's particles do (`energy`).
public final class ElementalBlazeRenderer extends MobRenderer<ElementalBlaze, ElementalBlazeModel> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "elemental_blaze"), "main");
    private static final ResourceLocation TEXTURE = texture("elemental_blaze");
    private static final RenderType GLOW = RenderType.eyes(texture("elemental_blaze_glow"));
    private static final RenderType EYES = RenderType.eyes(texture("elemental_blaze_eyes"));
    private static final RenderType BRIGHT = RenderType.eyes(TEXTURE);
    // How many ticks the awakening's rings race out.
    private static final float WAVE = 18.0F;
    // The energy each element is painted as, in the elements' order.
    private static final int[] PATTERN = { Energy.DUST, Energy.FLAME, Energy.FROST, Energy.GUST, Energy.SPARK };
    // How high over its feet its heart hangs, how many ticks a cast's ring of force runs out, and how far it leans
    // (radians per block a tick).
    private static final float HEART = 0.88F;
    private static final float SHOCK = 9.0F;
    private static final float LEAN = 0.9F;
    // The least block light it is drawn in (a blaze takes 15: with fifteen rods that glared), and the share of its
    // rods' own glow laid over it.
    private static final int LIT = 9;
    private static final float GLOW_SHARE = 0.6F;
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
        return Math.max(super.getBlockLightLevel(blaze, pos), LIT);
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
        var facing = this.entityRenderDispatcher.cameraOrientation();
        float body = Mth.rotLerp(partialTick, blaze.yBodyRotO, blaze.yBodyRot);
        RodOrbit.place(state, RodOrbit.leader(element), this.place);
        Vec3 lead = RodOrbit.world(this.place, Vec3.ZERO, body);
        VertexConsumer energy = Energy.buffer(buffers);
        if (energy != null) {
            this.energy(blaze, state, element, lead, body, partialTick, pose, energy, facing);
        }
    }

    // Its element alive round it, painted by the engine's energy shader: smouldering at its heart, gathering at the rod
    // that points, and each cast in its own way: fire drawn in from all sides, a star of frost, clods torn up under
    // it, a whirl that breaks into gusts racing at its foe, sparks leaping from its heart to its rods. As a spell
    // leaves, a ring of force runs out from it.
    private void energy(ElementalBlaze blaze, RodOrbit.State state, Element element, Vec3 lead, float body,
            float partialTick, PoseStack pose, VertexConsumer energy, Quaternionf facing) {
        int seed = blaze.getId();
        int pattern = PATTERN[element.ordinal()];
        float brace = RodOrbit.brace(state);
        float release = RodOrbit.release(state);
        Vec3 heart = new Vec3(0.0, HEART, 0.0);
        float smoulder = (state.awake() >= 0.0F ? 0.26F : 0.16F) + 0.3F * brace;
        Energy.puff(pose, energy, heart, 0.5F + 0.2F * brace + 0.6F * release, facing, pattern, element.colour,
                smoulder, seed);
        if (state.move() == BlazeTimeline.AWAKEN) {
            float out = (state.moveAge() - BlazeTimeline.BURST) / WAVE;
            for (Element each : Element.ALL) {
                float gather = (float) Ease.smooth(state.moveAge() / BlazeTimeline.BURST) * (out < 0.0F ? 1.0F : 0.0F);
                float turn = state.age() * 0.21F + each.ordinal() * Mth.TWO_PI / Element.ALL.length;
                float away = 2.2F * (1.0F - gather) + 0.3F;
                Energy.puff(pose, energy, heart.add(Mth.cos(turn) * away, 0.3 * Mth.sin(turn * 2.0F), Mth.sin(turn)
                        * away), 0.5F + 0.4F * gather, facing, PATTERN[each.ordinal()], each.colour, 0.6F * gather,
                        seed + each.ordinal());
                if (out >= 0.0F && out < 1.0F) {
                    Energy.flat(pose.last(), energy, heart.add(0.0, (each.ordinal() - 2) * 0.22, 0.0), 1.0F
                            + (5.0F + each.ordinal() * 0.7F) * out, Energy.RING, each.colour, 0.6F * (1.0F - out), seed
                                    + each.ordinal());
                }
            }
            return;
        }
        Element casting = BlazeTimeline.element(state.move());
        if (casting == null) {
            return;
        }
        float glow = RodOrbit.glow(state, casting, 0);
        Energy.puff(pose, energy, lead, 0.25F + 0.35F * glow, facing, pattern, casting.colour, 0.5F * glow, seed + 1);
        float c = state.moveAge() - BlazeTimeline.shift(state.move());
        float since = c - BlazeTimeline.cast(casting);
        if (since >= 0.0F && since < SHOCK) {
            float out = since / SHOCK;
            Energy.flat(pose.last(), energy, heart, 0.7F + 3.0F * (1.0F - (1.0F - out) * (1.0F - out)), Energy.RING,
                    casting.colour, 0.9F * (1.0F - out), seed + 2);
        }
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(blaze.getPosition(partialTick));
        switch (casting) {
            case FIRE -> {
                for (int k = 0; k < 3 && since < 0.0F; k++) {
                    float turn = state.age() * 0.45F + k * Mth.TWO_PI / 3.0F;
                    float away = 1.3F * (1.0F - brace) + 0.25F;
                    Energy.puff(pose, energy, lead.add(Mth.cos(turn) * away, Mth.sin(turn * 1.3F) * away * 0.5,
                            Mth.sin(turn) * away), 0.22F + 0.12F * brace, facing, Energy.FLAME, casting.colour,
                            0.45F * brace, seed + 3 + k);
                }
            }
            case ICE -> {
                float held = c < 0.0F ? brace * 0.5F : c < BlazeTimeline.BEAM ? 1.0F : 0.0F;
                Energy.puff(pose, energy, lead, 0.9F + 0.1F * Mth.sin(state.age() * 0.8F), facing, Energy.FROST,
                        casting.colour, 0.6F * held, seed + 3);
            }
            case EARTH -> {
                float lift = c < 0.0F ? brace * 0.4F : c < BlazeTimeline.LIFT ? 1.0F : Math.max(0.0F, 1.0F - (c
                        - BlazeTimeline.LIFT) / 6.0F);
                Energy.flat(pose.last(), energy, new Vec3(0.0, 0.15, 0.0), 1.9F, Energy.DUST, casting.colour,
                        0.9F * lift, seed + 3);
                Energy.puff(pose, energy, new Vec3(0.0, 0.5, 0.0), 1.2F, facing, Energy.DUST, casting.colour,
                        0.6F * lift, seed + 4);
            }
            case WIND -> {
                Energy.flat(pose.last(), energy, heart, 1.6F + 0.5F * brace, Energy.GUST, casting.light,
                        0.6F * Math.max(brace, since >= 0.0F ? 1.0F - since / 10.0F : 0.0F), seed + 3);
                Vec3 look = blaze.getViewVector(partialTick);
                float run = BlazeTimeline.end(casting) - BlazeTimeline.cast(casting);
                for (int k = 0; k < 4 && since >= 0.0F; k++) {
                    float along = since / run + k * 0.14F;
                    if (along < 1.0F) {
                        Energy.puff(pose, energy, heart.add(look.scale(0.8 + 8.0 * along)), 0.7F + 1.6F * along,
                                facing, Energy.GUST, casting.light, 0.7F * (1.0F - along), seed + 4 + k);
                    }
                }
            }
            case STORM -> {
                float charge = since < 0.0F ? brace : Math.max(0.0F, 1.0F - since / 6.0F);
                for (int i = 0; i < Element.ALL.length && charge > 0.02F; i++) {
                    RodOrbit.place(state, i, this.place);
                    Energy.band(pose.last(), energy, heart, RodOrbit.world(this.place, Vec3.ZERO, body), eye, 0.16F,
                            Energy.BOLT, casting.colour, 0.6F * charge, seed + 3 + i);
                }
            }
        }
    }

    // It leans into the way it flies.
    @Override
    protected void setupRotations(ElementalBlaze blaze, PoseStack pose, float bob, float yBodyRot, float partialTick,
            float scale) {
        super.setupRotations(blaze, pose, bob, yBodyRot, partialTick, scale);
        float turn = (180.0F - yBodyRot) * Mth.DEG_TO_RAD;
        float wx = (float) (blaze.getX() - blaze.xo);
        float wz = (float) (blaze.getZ() - blaze.zo);
        float forward = -(wx * Mth.sin(turn) + wz * Mth.cos(turn));
        float side = wx * Mth.cos(turn) - wz * Mth.sin(turn);
        pose.translate(0.0F, HEART, 0.0F);
        pose.mulPose(Axis.XP.rotation(-Mth.clamp(forward * LEAN, -0.3F, 0.3F)));
        pose.mulPose(Axis.ZP.rotation(-Mth.clamp(side * LEAN, -0.3F, 0.3F)));
        pose.translate(0.0F, -HEART, 0.0F);
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
                    grey(GLOW_SHARE * flicker * dim));
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
            float heart = model.coreGlow * dim * flicker;
            model.core.render(pose, bright, 0xF000F0, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(255,
                    Math.round((element.colour >> 16 & 255) * heart), Math.round((element.colour >> 8 & 255) * heart),
                    Math.round((element.colour & 255) * heart)));
            pose.pushPose();
            model.head.translateAndRotate(pose);
            for (int e = 0; e < model.crown.length; e++) {
                if (model.crownGlow[e] > 0.02F) {
                    model.crown[e].render(pose, bright, 0xF000F0, OverlayTexture.NO_OVERLAY,
                            grey(model.crownGlow[e] * dim));
                }
            }
            pose.popPose();
        }

        private static int grey(float strength) {
            int c = Math.round(Mth.clamp(strength, 0.0F, 1.0F) * 255.0F);
            return FastColor.ARGB32.color(255, c, c, c);
        }
    }
}
