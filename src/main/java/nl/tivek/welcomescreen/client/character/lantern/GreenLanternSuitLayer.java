package nl.tivek.welcomescreen.client.character.lantern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Green Lantern's uniform, made by the ring over whatever you wear: the green and black suit, white gloves,
 * and the mask over your eyes. Your own face and hair stay. It sits just outside the skin's own outer layer,
 * so a jacket or sleeves of the skin never poke through; armour still goes over it.
 */
public final class GreenLanternSuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID,
            "textures/entity/green_lantern_suit.png");
    // Just outside the skin's own outer layer (0.25). The mask is on the hat part, which is 0.5 bigger
    // again: just outside the skin's own hat.
    private static final CubeDeformation FIT = new CubeDeformation(0.3F);
    // How far above and below the line of light the uniform fades in or out, in blocks.
    private static final float SOFT = 0.2F;

    private static PlayerModel<AbstractClientPlayer> wide;
    private static PlayerModel<AbstractClientPlayer> slim;

    private GreenLanternSuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** Puts the uniform on the renderer of every kind of player skin (wide and slim arms). */
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(model);
            if (renderer instanceof PlayerRenderer player) {
                player.addLayer(new GreenLanternSuitLayer(player));
            }
        }
    }

    /** The uniform's model for this player's arms (wide or slim), made the first time it is needed. */
    static PlayerModel<AbstractClientPlayer> model(AbstractClientPlayer player) {
        if (player.getSkin().model() == PlayerSkin.Model.SLIM) {
            if (slim == null) {
                slim = build(true);
            }
            return slim;
        }
        if (wide == null) {
            wide = build(false);
        }
        return wide;
    }

    private static PlayerModel<AbstractClientPlayer> build(boolean slimArms) {
        PlayerModel<AbstractClientPlayer> model = new PlayerModel<>(
                LayerDefinition.create(PlayerModel.createMesh(FIT, slimArms), 64, 64).bakeRoot(), slimArms);
        // Only the suit and the mask; your own head stays your own.
        model.setAllVisible(false);
        model.hat.visible = true;
        model.body.visible = true;
        model.rightArm.visible = true;
        model.leftArm.visible = true;
        model.rightLeg.visible = true;
        model.leftLeg.visible = true;
        return model;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        // Recharging: the lantern hangs from the left hand, which the arm pose has raised.
        float recharge = ClientRing.recharge(player, partialTick);
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        if (recharge >= 0.0F) {
            RechargeAnimation.lanternInHand(poseStack, buffers, this.getParentModel().leftArm, slim, recharge);
        }
        ClientLooks.Uniform uniform = ClientLooks.uniform(player, partialTick);
        if (uniform == null) {
            return;
        }
        PlayerModel<AbstractClientPlayer> suit = model(player);
        this.getParentModel().copyPropertiesTo(suit);
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        if (uniform.complete()) {
            suit.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        } else {
            // Entities are drawn with the camera as their origin, so a vertex's height minus the camera's is
            // its height in the world.
            double cameraY = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y;
            suit.renderToBuffer(poseStack, new Wipe(buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),
                    (float) (uniform.line() - cameraY)), light, overlay);
        }
        // While the ring works the uniform lights up, most of all down the right arm into the ring.
        float glow = uniform.complete() ? SuitGlow.level(player, partialTick) : 0.0F;
        SuitGlow.body(poseStack, buffers, suit, slim, glow, ageInTicks);
        poseStack.pushPose();
        suit.rightArm.translateAndRotate(poseStack);
        Ring.draw(poseStack, buffers, light, slim, uniform.ring(), ClientRing.charge(player), glow);
        RingSpot.onBody(player, poseStack, Ring.stone(slim));
        poseStack.popPose();
    }

    /**
     * Shows only what is above the line of light: every corner below it is see-through, with a soft edge,
     * so the uniform seems to be painted on (or wiped off) by the line as it moves.
     */
    private static final class Wipe implements VertexConsumer {
        private final VertexConsumer inner;
        private final float line;

        Wipe(VertexConsumer inner, float line) {
            this.inner = inner;
            this.line = line;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int packedOverlay,
                int packedLight, float normalX, float normalY, float normalZ) {
            float shown = Mth.clamp((y - this.line) / SOFT + 0.5F, 0.0F, 1.0F);
            int alpha = Math.round((color >>> 24) * shown);
            this.inner.addVertex(x, y, z, alpha << 24 | color & 0xFFFFFF, u, v, packedOverlay, packedLight,
                    normalX, normalY, normalZ);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.inner.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.inner.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.inner.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.inner.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.inner.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            this.inner.setNormal(normalX, normalY, normalZ);
            return this;
        }
    }
}
