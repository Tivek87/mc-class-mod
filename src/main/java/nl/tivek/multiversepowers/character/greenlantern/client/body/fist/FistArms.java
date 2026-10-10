package nl.tivek.multiversepowers.character.greenlantern.client.body.fist;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.ConstructChoice;
import nl.tivek.multiversepowers.character.greenlantern.construct.Construct;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.fist.FistMoves;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import nl.tivek.multiversepowers.engine.client.render.entity.FirstPersonArm;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// The fist blows on the body: as others see him, his trunk turning and leaning into each blow and his hands reaching
// where it goes, elbows bent (Stance), the whole body turning round in a spin; from his own eyes, both arms thrown
// where the blow goes. Each hand wears its glove meanwhile (FistLayer, and here in his own view).
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class FistArms {
    private static final Vector3f REST_FROM = new Vector3f(0.75F, -1.1F, -0.15F);
    private static final Vector3f KNEE = new Vector3f(0.0F, 0.0F, -1.0F);
    private static final Vector3f HIPS = new Vector3f();
    private static final Vector3f NECK = new Vector3f();
    private static final Quaternionf LEAN = new Quaternionf();
    private static final Quaternionf WAIST = new Quaternionf();
    private static final Quaternionf CHEST = new Quaternionf();
    // A spin's gloves swell this much bigger at its height.
    private static final double SWOLLEN = 2.4;

    private FistArms() {
    }

    // A Poses layer: the blows' pose over the game's own.
    public static boolean pose(PlayerModel<?> model, LivingEntity entity) {
        if (!EntityPass.inWorld()) {
            return false;
        }
        ClientFists.View view = ClientFists.view(entity);
        if (view == null) {
            return false;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        FistPoses.Pose pose = FistPoses.of(view, partialTick);
        if (pose == null || pose.weight < 1.0E-3F) {
            return false;
        }
        float w = pose.weight;
        Vector3f[] feet = { Stance.foot(model, true, new Vector3f()), Stance.foot(model, false, new Vector3f()) };
        HIPS.set(0.0F, Stance.HIP_Y + pose.drop * w, 0.0F);
        LEAN.rotationZYX(pose.roll * 0.6F * w, pose.twist * 0.4F * w, pose.pitch * w);
        WAIST.rotationZYX(pose.roll * 0.4F * w, pose.twist * 0.6F * w, 0.0F);
        Stance.trunk(model, HIPS, LEAN, WAIST);
        for (int side = 0; side < 2; side++) {
            Stance.leg(model, side == 0, feet[side], KNEE);
        }
        Stance.neck(NECK);
        Stance.chest(CHEST);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            Vector3f hand = Stance.hand(model, right, new Vector3f());
            Vector3f target = CHEST.transform(new Vector3f(pose.hand[side])).add(NECK);
            hand.lerp(target, w);
            Stance.arm(model, right, hand, pose.pole[side]);
        }
        return true;
    }

    // Registered with BodyTurns: a spin turns his whole body once round.
    public static void turnBody(AbstractClientPlayer player, PoseStack pose, float scale) {
        ClientFists.View view = ClientFists.view(player);
        if (view == null) {
            return;
        }
        float spin = FistPoses.spin(view, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (spin > 0.0F && spin < Math.PI * 2.0) {
            pose.mulPose(Axis.YP.rotation(spin));
        }
    }

    // How big the gloves are now: swollen through a spin.
    static double size(ClientFists.View view, float partialTick) {
        if (view.move() != FistMoves.SPIN) {
            return 1.0;
        }
        double age = view.age(partialTick);
        return 1.0 + (SWOLLEN - 1.0) * Ease.smooth((age - 2.0) / 4.0) * (1.0 - Ease.smooth((age - 14.0) / 5.0));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isInvisible() || !player.getMainHandItem().isEmpty()
                || ConstructChoice.held() != Construct.NONE
                || ClientRing.recharge(player, event.getPartialTick()) >= 0.0F) {
            return;
        }
        ClientFists.View view = ClientFists.view(player);
        if (view == null) {
            return;
        }
        float partialTick = event.getPartialTick();
        FistPoses.Pose pose = FistPoses.of(view, partialTick);
        double apart = view.apart(partialTick);
        if ((pose == null || pose.weight < 1.0E-3F) && apart < 0.0) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        PoseStack stack = event.getPoseStack();
        float w = pose == null ? 0.0F : pose.weight;
        double formed = view.formed(partialTick);
        double size = size(view, partialTick);
        for (int side = 0; side < 2; side++) {
            float sign = side == 0 ? 1.0F : -1.0F;
            Vector3f hand = new Vector3f(side == 0 ? FirstPersonArm.HAND_RIGHT : FirstPersonArm.HAND_LEFT);
            Vector3f from = new Vector3f(REST_FROM.x * sign, REST_FROM.y, REST_FROM.z);
            if (pose != null) {
                hand.lerp(pose.seen[side], w);
                from.lerp(pose.from[side], w);
            }
            FirstPersonArm.arm(stack, event.getMultiBufferSource(), event.getPackedLight(), player, renderer, sign,
                    hand, from);
            stack.pushPose();
            FirstPersonArm.toArm(stack, sign, hand, from);
            // A painter keeps the pose it was made in: one for each glove, made in its arm's frame.
            LanternPainter painter = LanternPainter.hand(stack, player.tickCount + partialTick);
            FistPainter.glove(painter, side == 0, formed, apart, size, player.getId() * 2 + side);
            painter.finish(minecraft.renderBuffers().bufferSource());
            stack.popPose();
        }
    }
}
