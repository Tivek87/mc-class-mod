package nl.tivek.multiversepowers.character.thor.client.blow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.client.motion.ThorMotion;
import nl.tivek.multiversepowers.engine.client.render.entity.FirstPersonArm;
import nl.tivek.multiversepowers.engine.client.render.entity.FirstPersonLeg;
import org.joml.Vector3f;

// Your own Thor's blows seen from his eyes: both fists up in his guard and thrown where each blow goes, and the leg of
// a kick coming up into view; with the hammer in hand, his right fist holds it. View space: x right, y up, -z ahead.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ThorFists {
    private static final Vector3f REST_FROM = new Vector3f(0.75F, -1.1F, -0.15F);
    private static final Vector3f HIP = new Vector3f(0.14F, -1.05F, 0.05F);
    @Nullable
    private static ItemStack axe;

    private ThorFists() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isInvisible() || ClientCharacter.active() != GameCharacter.THOR
                || !player.getMainHandItem().isEmpty() || ThorMotion.flying()) {
            return;
        }
        ClientThor.View view = ClientThor.view(player);
        ThorBlowPoses.Pose pose = view == null ? null : ThorBlowPoses.of(view, event.getPartialTick());
        boolean armed = view != null && view.has(ThorStatePayload.ARMED) && !view.has(ThorStatePayload.THROWN);
        float w = pose == null ? 0.0F : pose.weight;
        if (w < 1.0E-3F && !armed) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        for (int side = 0; side < 2; side++) {
            if (side == 1 && w < 1.0E-3F) {
                continue;
            }
            float sign = side == 0 ? 1.0F : -1.0F;
            Vector3f rest = side == 0 ? FirstPersonArm.HAND_RIGHT : FirstPersonArm.HAND_LEFT;
            Vector3f hand = new Vector3f(rest);
            Vector3f from = new Vector3f(REST_FROM.x * sign, REST_FROM.y, REST_FROM.z);
            if (pose != null) {
                hand.lerp(pose.seen[side], w);
                from.lerp(pose.from[side], w);
            }
            FirstPersonArm.arm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player,
                    renderer, sign, hand, from);
            if (side == 0 && armed) {
                hammer(minecraft, player, event, hand);
            }
        }
        if (pose != null && pose.footSide >= 0 && pose.kick * pose.weight > 0.05F) {
            float sign = pose.footSide == 0 ? 1.0F : -1.0F;
            Vector3f hip = new Vector3f(HIP.x * sign, HIP.y, HIP.z);
            Vector3f down = new Vector3f(hip).add(0.0F, -0.75F, 0.0F);
            Vector3f foot = down.lerp(pose.footSeen, pose.kick * pose.weight);
            FirstPersonLeg.leg(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player,
                    renderer, sign, hip, foot);
        }
    }

    // The hammer (the axe standing in for it) in his right fist, head up.
    private static void hammer(Minecraft minecraft, LocalPlayer player, RenderHandEvent event, Vector3f hand) {
        if (axe == null) {
            axe = new ItemStack(Items.IRON_AXE);
        }
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(hand.x, hand.y, hand.z);
        pose.mulPose(Axis.XP.rotationDegrees(-25.0F));
        pose.mulPose(Axis.YP.rotationDegrees(-15.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(40.0F));
        pose.translate(0.0F, 0.12F, 0.0F);
        pose.scale(0.8F, 0.8F, 0.8F);
        minecraft.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, axe, ItemDisplayContext.NONE,
                false, pose, event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
    }
}
