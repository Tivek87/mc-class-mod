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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import org.joml.Vector3f;

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
        if (held.weapon >= HeavyMoves.REVOLVERS) {
            GunPainter.draw(painter, held, 0, frame, held.formed(partialTick), held.apart(partialTick), partialTick,
                    player.getId());
            if (HeavyMoves.dual(held.weapon)) {
                GunPainter.draw(painter, held, 1, held.left, held.formed(partialTick), held.apart(partialTick),
                        partialTick, player.getId());
            }
            painter.finish(Minecraft.getInstance().renderBuffers().bufferSource());
            if (!EntityPass.inWorld()) {
                return;
            }
            // Drawn here round the camera, so the camera's place makes it a place in the world.
            Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vec3 mouth = GunPainter.muzzle(held.weapon);
            for (int side = 0; side < (HeavyMoves.dual(held.weapon) ? 2 : 1); side++) {
                Vec3 at = (side == 0 ? frame : held.left).at(mouth.x, mouth.y, mouth.z);
                Vector3f seen = pose.last().pose().transformPosition((float) at.x, (float) at.y, (float) at.z,
                        new Vector3f());
                held.muzzle[side] = eye.add(seen.x, seen.y, seen.z);
            }
            held.muzzleAt = ClientHeavy.now(partialTick);
            return;
        }
        HeavyPainter.weapon(painter, held.weapon, frame, held.formed(partialTick), held.apart(partialTick),
                HeavyPoses.revving(held, partialTick), ClientHeavy.now(partialTick), player.getId(),
                HeavyPoses.flash(held, partialTick), HeavyPoses.loaded(held, partialTick),
                HeavyPoses.reload(held, partialTick));
        HeavyPainter.trail(painter, back -> HeavyArms.past(held, partialTick, back),
                HeavyPainter.trailing(held, partialTick));
        painter.finish(Minecraft.getInstance().renderBuffers().bufferSource());
    }
}
