package nl.tivek.multiversepowers.creature.wizard.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardDecoy;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;

// The Wizard drawn from his own model with his staff in hand (StaffLayer) and the runes on his robe glowing in his
// phase's colour, floating from his second phase. As he gathers a great spell a circle of runes in its colour lights up
// on the ground under him. His images are drawn as he is, but cast no shadow and their crystal is pale (StaffLayer);
// the decoy he leaves when he blinks is a see-through ghost of him, shimmering, a violet echo trembling round it.
public final class WizardRenderer<T extends MovingMonster> extends MobRenderer<T, WizardModel<T>> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "wizard"), "main");
    static final ResourceLocation TEXTURE = texture("wizard");
    static final ResourceLocation GLOW = texture("wizard_glow");
    private static final RenderType RUNES = RenderType.eyes(GLOW);
    private static final RenderType ECHO = RenderType.eyes(TEXTURE);
    private static final float CIRCLE = 2.4F;

    WizardRenderer(EntityRendererProvider.Context context, float shadow) {
        super(context, new WizardModel<>(context.bakeLayer(LAYER)), shadow);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return RUNES;
            }

            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, T caster, float limbSwing,
                    float amount, float partialTick, float age, float headYaw, float headPitch) {
                int colour = Wizard.light(phase(caster));
                this.getParentModel().renderToBuffer(pose, buffers.getBuffer(RUNES), 15728640,
                        OverlayTexture.NO_OVERLAY, 0xFF000000 | colour);
            }
        });
        this.addLayer(new StaffLayer<>(this, context.bakeLayer(StaffLayer.LAYER)));
    }

    static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "textures/entity/" + name + ".png");
    }

    static boolean floating(MovingMonster caster) {
        return phase(caster) > 1;
    }

    static int phase(MovingMonster caster) {
        if (caster instanceof Wizard wizard) {
            return wizard.phase();
        }
        return caster instanceof WizardDecoy decoy ? decoy.look() : 1;
    }

    static boolean ghost(MovingMonster caster) {
        return caster instanceof WizardDecoy decoy && decoy.mode() == WizardDecoy.BLINK;
    }

    @Override
    public void render(T caster, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (caster instanceof Wizard) {
            this.circle(caster, partialTick, pose, buffers);
        }
        boolean ghost = ghost(caster);
        float age = caster.tickCount + partialTick;
        this.model.alpha = ghost ? 0.42F + 0.14F * Mth.sin(age * 0.45F) : 1.0F;
        pose.pushPose();
        if (floating(caster)) {
            pose.translate(0.0, Wizard.HOVER, 0.0);
        }
        super.render(caster, yaw, partialTick, pose, buffers, light);
        if (ghost) {
            this.echo(caster, yaw, partialTick, age, pose, buffers);
        }
        pose.popPose();
        this.model.alpha = 1.0F;
    }

    // The blink decoy's violet echo: its body again in light, a little off and trembling.
    private void echo(T caster, float yaw, float partialTick, float age, PoseStack pose, MultiBufferSource buffers) {
        float body = Mth.rotLerp(partialTick, caster.yBodyRotO, caster.yBodyRot);
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - body));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.06F * Mth.sin(age * 1.7F), -1.501F, 0.06F * Mth.cos(age * 1.3F));
        float strength = 0.3F + 0.15F * Mth.sin(age * 0.9F);
        int c = (int) (255 * strength);
        int violet = 0xFF000000 | (int) (c * 0.7F) << 16 | (int) (c * 0.42F) << 8 | c;
        this.model.alpha = 1.0F;
        this.model.renderToBuffer(pose, buffers.getBuffer(ECHO), 15728640, OverlayTexture.NO_OVERLAY, violet);
        pose.popPose();
    }

    // The circle of runes under him while he gathers a great spell, lit up to its release and fading after.
    private void circle(T caster, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        int move = caster.move();
        if (!WizardMove.circled(move)) {
            return;
        }
        float t = (float) caster.moveAge(partialTick);
        float release = WizardMove.release(move);
        float strength = t < release ? Mth.clamp(t / release, 0.0F, 1.0F)
                : Mth.clamp(1.0F - (t - release) / 8.0F, 0.0F, 1.0F);
        if (strength <= 0.001F) {
            return;
        }
        float grow = 0.55F + 0.45F * Math.min(1.0F, t / 6.0F);
        Additive.glyph(pose.last(), buffers.getBuffer(Additive.TYPE), CIRCLE * grow, caster.tickCount + partialTick,
                WizardMove.colour(move), strength);
    }

    @Override
    protected RenderType getRenderType(T caster, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (ghost(caster)) {
            return RenderType.entityTranslucent(TEXTURE);
        }
        return super.getRenderType(caster, bodyVisible, translucent, glowing);
    }

    @Override
    public ResourceLocation getTextureLocation(T caster) {
        return TEXTURE;
    }
}
