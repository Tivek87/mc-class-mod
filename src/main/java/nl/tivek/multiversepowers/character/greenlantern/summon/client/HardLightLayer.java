package nl.tivek.multiversepowers.character.greenlantern.summon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;

// A hard-light creature's look: a still green layer over its own, lit by the world as the rest of it, white for a
// moment as a summon forms out of the ring's light. Drawn with the creature's own model, so its body left lying wears it
// too.
final class HardLightLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation PLAIN = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "textures/entity/hard_light.png");
    private static final int GREEN = 0x2FD653;
    private static final int ALPHA = 0x8C;

    HardLightLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        int rgb = Colors.mix(GREEN, 0xFFFFFF, (float) Ease.smooth(ClientSummons.forming(entity, partialTick)));
        this.getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(PLAIN)), light,
                OverlayTexture.NO_OVERLAY, ALPHA << 24 | rgb);
    }
}
