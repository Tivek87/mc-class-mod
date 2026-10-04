package nl.tivek.multiversepowers.character.thor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.character.thor.hammer.ThrownHammer;
import nl.tivek.multiversepowers.character.thor.client.pose.ThorHammerLayer;

// Mjolnir out of Thor's hands, as big as it was in his hand: flying out head first and back grip first (the server
// turns it), its rune faces to the sides; resting, as what stopped it decides: lying on its head with its handle up
// and its peak pressed a little into the ground, stuck head first in a wall or ceiling with its handle straight out,
// or hanging upright in the air, bobbing and turning slowly.
public final class ThrownHammerRenderer extends EntityRenderer<ThrownHammer> {
    // How far its peak sinks into what it rests in, and how far a lying hammer leans off upright.
    private static final float SUNK = 0.1F;
    private static final float LEAN = 4.0F;
    private static final float PEAK = 1.0F;
    private static final double NEAR = 1.8;

    private ThrownHammerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ThrownHammer.TYPE.get(), ThrownHammerRenderer::new);
    }

    // Your own hammer just out of your hand, or about to land in it, would fill your view: unseen that close to your
    // eyes.
    @Override
    public boolean shouldRender(ThrownHammer hammer, Frustum frustum, double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && hammer.owner() == minecraft.player.getId()
                && minecraft.options.getCameraType().isFirstPerson() && hammer.distanceToSqr(x, y, z) < NEAR * NEAR) {
            return false;
        }
        return super.shouldRender(hammer, frustum, x, y, z);
    }

    @Override
    public void render(ThrownHammer hammer, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float size = hammer.size();
        float time = hammer.tickCount + partialTick;
        float turn = Mth.rotLerp(partialTick, hammer.yRotO, hammer.getYRot());
        float glow = hammer.charged() ? ThorHammerLayer.flicker(hammer.getId(), time) : 0.0F;
        pose.pushPose();
        byte rest = hammer.rest();
        if (rest == ThrownHammer.LYING || rest == ThrownHammer.STUCK) {
            Direction face = rest == ThrownHammer.LYING ? Direction.UP : hammer.face();
            Vec3i into = face.getOpposite().getNormal();
            pose.translate(into.getX() * SUNK * size, into.getY() * SUNK * size, into.getZ() * SUNK * size);
            if (rest == ThrownHammer.LYING) {
                pose.mulPose(Axis.XP.rotationDegrees(LEAN));
            }
            // Its peak into the floor, wall or ceiling, turned about its handle the way it flew.
            pose.mulPose(face.getOpposite().getRotation());
            pose.mulPose(Axis.YP.rotationDegrees(-turn + 90.0F));
            pose.scale(size, size, size);
            ThorHammerLayer.draw(PEAK, glow, pose, buffers, light);
        } else if (rest == ThrownHammer.HANGING) {
            pose.translate(0.0F, Mth.sin(time * 0.08F) * 0.06F * size, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(-turn + 90.0F + time * 1.5F));
            pose.scale(size, size, size);
            ThorHammerLayer.draw(ThorHammerLayer.HEFT, glow, pose, buffers, light);
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(-turn));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.rotLerp(partialTick, hammer.xRotO, hammer.getXRot())));
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            pose.scale(size, size, size);
            ThorHammerLayer.draw(ThorHammerLayer.HEFT, glow, pose, buffers, light);
        }
        pose.popPose();
        super.render(hammer, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownHammer hammer) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
