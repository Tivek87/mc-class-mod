package nl.tivek.multiversepowers.character.thor.client.pose;

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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.thor.ThorBlow;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.client.blow.ThorBlowPoses;
import nl.tivek.multiversepowers.engine.client.render.entity.FirstPersonArm;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;

// Your own Thor in flight, seen from his eyes: the left hand holds the axe out ahead of him, as if it pulled him along,
// and the right hand hangs free and ready, and strikes. View space: x right, y up, -z ahead.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ThorFirstPerson {
    private static final Vector3f AXE_HAND = new Vector3f(-0.38F, -0.2F, -0.78F);
    private static final Vector3f LEFT_FROM = new Vector3f(-0.95F, -0.75F, 0.2F);
    private static final Vector3f FREE_HAND = new Vector3f(0.55F, -0.62F, -0.6F);
    private static final Vector3f RIGHT_FROM = new Vector3f(1.1F, -0.95F, 0.25F);
    @Nullable
    private static ItemStack axe;

    private ThorFirstPerson() {
    }

    // How far the axe is out in the left hand: 0 on the belt (not seen), 1 held out ahead.
    private static float held(LocalPlayer player, float partialTick) {
        ClientThor.View view = ClientThor.view(player);
        if (view == null) {
            return 0.0F;
        }
        float age = view.age(partialTick);
        return switch (view.move()) {
            case ThorStatePayload.TAKE_OFF -> view.has(ThorStatePayload.FLYING)
                    ? (float) Ease.smooth((age - ThorBody.DRAW) / 5.0) : 0.0F;
            case ThorStatePayload.TOUCH_DOWN, ThorStatePayload.SLAM -> 1.0F - (float) Ease.smooth(age
                    / ThorBody.SHEATHE);
            default -> view.has(ThorStatePayload.FLYING) ? 1.0F : 0.0F;
        };
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isInvisible() || ClientCharacter.active() != GameCharacter.THOR
                || !player.getMainHandItem().isEmpty()) {
            return;
        }
        float out = held(player, event.getPartialTick());
        if (out <= 0.0F) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (axe == null) {
            axe = new ItemStack(Items.IRON_AXE);
        }
        PoseStack pose = event.getPoseStack();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        float time = player.tickCount + event.getPartialTick();
        float bob = (float) Math.sin(time * 0.11) * 0.02F;
        Vector3f hand = new Vector3f(FirstPersonArm.HAND_LEFT).lerp(AXE_HAND, out).add(0.0F, bob, 0.0F);
        FirstPersonArm.arm(pose, event.getMultiBufferSource(), event.getPackedLight(), player, renderer, -1.0F, hand,
                LEFT_FROM);
        pose.pushPose();
        pose.translate(hand.x, hand.y, hand.z);
        pose.mulPose(Axis.XP.rotationDegrees(-25.0F));
        pose.mulPose(Axis.YP.rotationDegrees(15.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-40.0F));
        pose.translate(0.0F, 0.12F, 0.0F);
        pose.scale(0.8F, 0.8F, 0.8F);
        minecraft.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, axe,
                ItemDisplayContext.NONE, true, pose, event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
        Vector3f free = new Vector3f(FirstPersonArm.HAND_RIGHT).lerp(FREE_HAND, out).add(0.0F, -bob, 0.0F);
        Vector3f from = new Vector3f(RIGHT_FROM);
        ClientThor.View view = ClientThor.view(player);
        ThorBlowPoses.Pose blow = view == null ? null : ThorBlowPoses.of(view, event.getPartialTick());
        ThorBlow thrown = view == null ? null : ThorBlow.byIndex(view.blow);
        if (blow != null && thrown != null && thrown.oneHanded()
                && view.blowAge(event.getPartialTick()) < thrown.ticks()) {
            free.lerp(blow.seen[0], blow.weight);
            from.lerp(blow.from[0], blow.weight);
        }
        FirstPersonArm.arm(pose, event.getMultiBufferSource(), event.getPackedLight(), player, renderer, 1.0F, free,
                from);
    }
}
