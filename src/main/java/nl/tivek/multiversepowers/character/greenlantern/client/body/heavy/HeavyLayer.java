package nl.tivek.multiversepowers.character.greenlantern.client.body.heavy;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;

// A heavy weapon in a player's hands as others see him (and he sees himself from outside), held where his hands are.
public final class HeavyLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private HeavyLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(model);
            if (renderer instanceof PlayerRenderer player) {
                player.addLayer(new HeavyLayer(player));
            }
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        ClientHeavy.Held held = ClientHeavy.view(player);
        if (held == null || !held.posed || player.isInvisible()) {
            return;
        }
        Frame frame = held.frame;
        LanternPainter painter = LanternPainter.hand(pose, ageInTicks);
        HeavyPainter.weapon(painter, held.weapon, frame, held.formed(partialTick), held.apart(partialTick),
                HeavyPoses.revving(held, partialTick), ClientHeavy.now(partialTick), player.getId());
        HeavyPainter.trail(painter, back -> HeavyArms.past(held, partialTick, back),
                HeavyPainter.trailing(held, partialTick));
        painter.finish(Minecraft.getInstance().renderBuffers().bufferSource());
    }
}
