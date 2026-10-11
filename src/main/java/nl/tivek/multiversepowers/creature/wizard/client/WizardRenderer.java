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
// the decoy he leaves when he blinks is a see-through ghost of him, shimmering, a violet echo trembling round it. He
// never falls over or reddens as the game's dying creatures do: his death is his own move (WizardMove.DEATH), and at
// its end he burns brighter and brighter in his phase's light as his body fades into it.
public final class WizardRenderer<T extends MovingMonster> extends MobRenderer<T, WizardModel<T>> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "wizard"), "main");
    static final ResourceLocation TEXTURE = texture("wizard");
    static final ResourceLocation GLOW = texture("wizard_glow");
    private static final RenderType RUNES = RenderType.eyes(GLOW);
    private static final RenderType ECHO = RenderType.eyes(TEXTURE);
    private static final float CIRCLE = 2.4F;
    // His body fades into light from this tick of his death.
    private static final float FADE_FROM = 60.0F;

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
                float shown = this.getParentModel().alpha;
                int r = (int) ((colour >> 16 & 255) * shown);
                int g = (int) ((colour >> 8 & 255) * shown);
                int b = (int) ((colour & 255) * shown);
                this.getParentModel().renderToBuffer(pose, buffers.getBuffer(RUNES), 15728640,
                        OverlayTexture.NO_OVERLAY, 0xFF000000 | r << 16 | g << 8 | b);
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
        float t = (float) caster.moveAge(partialTick);
        boolean dying = caster.move() == WizardMove.DEATH;
        float fade = dying ? Mth.clamp((t - FADE_FROM) / (WizardMove.GONE - FADE_FROM), 0.0F, 1.0F) : 0.0F;
        this.model.alpha = ghost ? 0.42F + 0.14F * Mth.sin(age * 0.45F) : 1.0F - fade;
        double hover = floating(caster) ? Wizard.HOVER : 0.0;
        if (dying) {
            hover *= 1.0 - Mth.clamp(t / 14.0F, 0.0F, 1.0F);
        }
        pose.pushPose();
        pose.translate(0.0, hover, 0.0);
        // The game tints and topples a creature by its death time: the Wizard's death is drawn by his own pose.
        int deathTime = caster.deathTime;
        caster.deathTime = 0;
        super.render(caster, yaw, partialTick, pose, buffers, light);
        caster.deathTime = deathTime;
        if (ghost) {
            float strength = 0.3F + 0.15F * Mth.sin(age * 0.9F);
            this.echo(caster, partialTick, pose, buffers, Wizard.VIOLET, strength, 0.06F * Mth.sin(age * 1.7F),
                    0.06F * Mth.cos(age * 1.3F));
        } else if (dying && t > WizardMove.BREAK - 8) {
            float rise = Mth.clamp((t - WizardMove.BREAK + 8) / 16.0F, 0.0F, 1.0F);
            float strength = rise * (0.5F + 0.5F * fade) * (1.0F - Mth.clamp((t - WizardMove.GONE + 2) / 4.0F, 0.0F,
                    1.0F));
            this.echo(caster, partialTick, pose, buffers, Wizard.light(phase(caster)), strength,
                    0.04F * Mth.sin(age * 2.3F), 0.04F * Mth.cos(age * 1.9F));
        }
        pose.popPose();
        this.model.alpha = 1.0F;
    }

    // His body again in light over itself (a blink ghost's violet echo, the light he dies in), a little off.
    private void echo(T caster, float partialTick, PoseStack pose, MultiBufferSource buffers, int colour,
            float strength, float dx, float dz) {
        float body = Mth.rotLerp(partialTick, caster.yBodyRotO, caster.yBodyRot);
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - body));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(dx, -1.501F, dz);
        int r = (int) ((colour >> 16 & 255) * strength);
        int g = (int) ((colour >> 8 & 255) * strength);
        int b = (int) ((colour & 255) * strength);
        float alpha = this.model.alpha;
        this.model.alpha = 1.0F;
        this.model.renderToBuffer(pose, buffers.getBuffer(ECHO), 15728640, OverlayTexture.NO_OVERLAY,
                0xFF000000 | r << 16 | g << 8 | b);
        this.model.alpha = alpha;
        pose.popPose();
    }

    @Override
    protected void setupRotations(T caster, PoseStack pose, float bob, float bodyYaw, float partialTick,
            float scale) {
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - bodyYaw));
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
        if (ghost(caster) || caster.move() == WizardMove.DEATH) {
            return RenderType.entityTranslucent(TEXTURE);
        }
        return super.getRenderType(caster, bodyVisible, translucent, glowing);
    }

    @Override
    public ResourceLocation getTextureLocation(T caster) {
        return TEXTURE;
    }
}
