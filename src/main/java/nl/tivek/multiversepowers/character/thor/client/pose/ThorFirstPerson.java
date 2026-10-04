package nl.tivek.multiversepowers.character.thor.client.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
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
import nl.tivek.multiversepowers.character.thor.client.blow.ThorFists;
import nl.tivek.multiversepowers.engine.client.render.entity.FirstPersonArm;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;

// Your own Thor in flight, seen from his eyes: the left hand holds Mjolnir out ahead of him, as if it pulled him along,
// and the right hand hangs free and ready, and strikes. The left hurls it in a Storm Throw, hangs free while it is out
// and reaches for it as it flies back. View space: x right, y up, -z ahead.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ThorFirstPerson {
    private static final Vector3f HAMMER_HAND = new Vector3f(-0.38F, -0.2F, -0.78F);
    private static final Vector3f LEFT_FROM = new Vector3f(-0.95F, -0.75F, 0.2F);
    private static final Vector3f FREE_HAND = new Vector3f(0.55F, -0.62F, -0.6F);
    private static final Vector3f FREE_LEFT = new Vector3f(-0.55F, -0.62F, -0.6F);
    private static final Vector3f RIGHT_FROM = new Vector3f(1.1F, -0.95F, 0.25F);
    private static final Vector3f LEFT_SHOULDER = new Vector3f(-0.3F, -0.42F, 0.05F);
    private static final float REACH = 0.9F;

    private ThorFirstPerson() {
    }

    // How far the hammer is out in the left hand: 0 on the belt (not seen), 1 held out ahead.
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
        PoseStack pose = event.getPoseStack();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        float partialTick = event.getPartialTick();
        float time = player.tickCount + partialTick;
        float bob = (float) Math.sin(time * 0.11) * 0.02F;
        ClientThor.View view = ClientThor.view(player);
        ThorBlowPoses.Pose blow = view == null ? null : ThorBlowPoses.of(view, partialTick);
        ThorBlow thrown = view == null ? null : ThorBlow.byIndex(view.blow);
        boolean striking = blow != null && thrown != null && view.blowAge(partialTick) < thrown.ticks();
        // A Storm Throw out of his hand, both hands hang free; the left reaches for it as it comes back.
        boolean away = view != null && view.has(ThorStatePayload.THROWN)
                && !ThorHammerLayer.windingUp(view, partialTick);
        Vector3f hand = new Vector3f(FirstPersonArm.HAND_LEFT).lerp(away ? FREE_LEFT : HAMMER_HAND, out)
                .add(0.0F, bob, 0.0F);
        Vector3f leftFrom = new Vector3f(LEFT_FROM);
        if (striking && thrown == ThorBlow.STORM_THROW) {
            hand.lerp(blow.seen[1], blow.weight);
            leftFrom.lerp(blow.from[1], blow.weight);
        }
        Vector3f way = away && view.has(ThorStatePayload.CALLING) ? ThorFists.toHammer(player, partialTick) : null;
        if (way != null) {
            hand.set(way).mul(REACH).add(LEFT_SHOULDER);
            leftFrom.set(way).mul(-0.6F).add(LEFT_SHOULDER);
        }
        float shove = ThorFists.caught(view, partialTick, ThorStatePayload.LEFT_HAND);
        hand.add(0.0F, -0.05F * shove, 0.16F * shove);
        FirstPersonArm.arm(pose, event.getMultiBufferSource(), event.getPackedLight(), player, renderer, -1.0F, hand,
                leftFrom);
        if (!away) {
            pose.pushPose();
            FirstPersonArm.toArm(pose, -1.0F, hand, leftFrom);
            ThorHammerLayer.ahead(pose, false);
            pose.scale(ThorHammerLayer.SEEN_SIZE, ThorHammerLayer.SEEN_SIZE, ThorHammerLayer.SEEN_SIZE);
            ThorHammerLayer.draw(ThorHammerLayer.GRIP, ThorHammerLayer.glow(player, partialTick), pose,
                    event.getMultiBufferSource(), event.getPackedLight());
            pose.popPose();
        }
        Vector3f free = new Vector3f(FirstPersonArm.HAND_RIGHT).lerp(FREE_HAND, out).add(0.0F, -bob, 0.0F);
        Vector3f from = new Vector3f(RIGHT_FROM);
        if (striking && thrown.oneHanded() && thrown != ThorBlow.STORM_THROW) {
            free.lerp(blow.seen[0], blow.weight);
            from.lerp(blow.from[0], blow.weight);
        }
        FirstPersonArm.arm(pose, event.getMultiBufferSource(), event.getPackedLight(), player, renderer, 1.0F, free,
                from);
    }
}
