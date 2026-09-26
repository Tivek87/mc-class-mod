package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class LanternArms {
    private static final Vector3f REACH = new Vector3f(0.5F, -0.22F, -1.25F);
    private static final Vector3f SMASH = new Vector3f(0.36F, -0.72F, -0.95F);
    private static final Vector3f POUND_UP = new Vector3f(0.42F, 0.02F, -1.05F);
    private static final double POUND_HIGH = 0.5;
    private static final double POUND_LOW = -0.08;
    private static final float CHARGE_REACH = 0.7F;
    private static final Vector3f POINT = new Vector3f(0.36F, -0.24F, -1.3F);
    private static final Vector3f POINT_KICK = new Vector3f(0.0F, 0.04F, 0.12F);
    private static final float POINT_UP = 30.0F;
    private static final float POINT_DOWN = 7.0F;
    private static final double SHOULDER_HEIGHT = 0.8;
    private static final double SHOULDER_SIDE = 0.31;
    private static final double ARM = 0.68;
    private static final Vector3f HAND_KICK = new Vector3f(0.0F, 0.09F, 0.2F);
    private static final float HAND_TREMBLE = 0.018F;
    private static final float WRIST_BACK = 0.17F;
    private static final Vector3f UNDER_WRIST = new Vector3f(-0.03F, -0.075F, 0.0F);
    private static final Vector3f BRACE_REST = new Vector3f(-0.45F, -1.1F, -0.7F);
    private static final Vector3f BRACE_FROM = new Vector3f(-1.25F, -1.15F, 0.25F);

    private static float beam;
    private static float point;
    private static float brace;
    private static long beamAt = Util.getMillis();

    private LanternArms() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        HumanoidModel.ArmPose pose = LanternPose.POSE.getValue();
        FlightPose.pre(event);
        if (ClientRing.recharge(player, event.getPartialTick()) >= 0.0F
                || ClientRing.arrival(player, event.getPartialTick()) >= 0.0F) {
            model.leftArmPose = pose;
            model.rightArmPose = pose;
            return;
        }
        if (SwordArms.posing(player, event.getPartialTick())) {
            model.leftArmPose = pose;
            model.rightArmPose = pose;
            SwordArms.spin(event);
        }
        if (FlameArms.posing(player, event.getPartialTick())) {
            model.leftArmPose = pose;
            model.rightArmPose = pose;
            FlameArms.spin(event);
        }
        if (WhipArms.posing(player, event.getPartialTick())) {
            model.leftArmPose = pose;
            model.rightArmPose = pose;
            WhipArms.spin(event);
        }
        if (CallArm.up(player, event.getPartialTick()) > 0.0F || ScanArm.out(player, event.getPartialTick()) > 0.0F
                || HandsArm.out(player, event.getPartialTick()) > 0.0F) {
            model.rightArmPose = pose;
        }
        if (ClientConstructs.heldBy(player.getId(), false) != null) {
            model.rightArmPose = pose;
        }
        if (ClientConstructs.heldBy(player.getId(), true) != null) {
            model.leftArmPose = pose;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        SwordArms.unspin(event);
        FlameArms.unspin(event);
        WhipArms.unspin(event);
        FlightPose.post(event);
    }

    public static void turnBody(AbstractClientPlayer player, PoseStack pose, float scale) {
        if (scale <= 0.0F) {
            return;
        }
        pose.scale(1.0F / scale, 1.0F / scale, 1.0F / scale);
        FlightPose.turnModel(player, pose);
        SwordArms.turnModel(player, pose);
        FlameArms.turnModel(player, pose);
        WhipArms.turnModel(player, pose);
        pose.scale(scale, scale, scale);
    }

    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        FlightPose.pose(model, entity, arm);
        if (ClientRing.recharge(entity, partialTick) >= 0.0F) {
            RechargeAnimation.pose(model, entity, arm);
            return;
        }
        if (ClientRing.arrival(entity, partialTick) >= 0.0F) {
            ArrivalAnimation.pose(model, entity, arm);
            return;
        }
        SwordArms.pose(model, entity, arm);
        FlameArms.pose(model, entity, arm);
        WhipArms.pose(model, entity, arm);
        ScanArm.pose(model, entity, arm);
        CallArm.pose(model, entity, arm);
        HandsArm.pose(model, entity, arm);
        ClientConstructs.Held held = ClientConstructs.heldBy(entity.getId(), arm == HumanoidArm.LEFT);
        if (held != null) {
            reach(arm == HumanoidArm.LEFT ? model.leftArm : model.rightArm, entity, held, partialTick);
        }
    }

    private static void reach(ModelPart limb, LivingEntity entity, ClientConstructs.Held held,
            float partialTick) {
        double yaw = Math.toRadians(Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        Vec3 shoulder = shoulder(entity, left, partialTick);
        Vec3 from = FlightPose.turned(entity,
                held.defends() ? shoulder.add(left.scale(SHOULDER_SIDE * 2.0)) : shoulder, partialTick);
        Vec3 way = FlightPose.untilted(entity, held.center().subtract(from));
        if (way.lengthSqr() < 1.0E-6) {
            return;
        }
        way = way.normalize();
        // The model's own directions: x to the left, y down, z back. An arm hangs along y, so turning it by
        // xRot alone tips it forward and by zRot alone swings it out to the side.
        double x = way.dot(left);
        double y = -way.y;
        double z = -way.dot(forward);
        float out = Mth.clamp(held.strength(), 0.0F, 1.0F);
        limb.xRot = Mth.lerp(out, limb.xRot, (float) Math.asin(Mth.clamp(z, -1.0, 1.0)));
        limb.yRot = Mth.lerp(out, limb.yRot, 0.0F);
        limb.zRot = Mth.lerp(out, limb.zRot, (float) Math.atan2(-x, y));
    }

    private static Vec3 shoulder(LivingEntity entity, Vec3 left, float partialTick) {
        return entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * SHOULDER_HEIGHT, 0.0)
                .subtract(left.scale(SHOULDER_SIDE));
    }

    public static Vec3 ringPoint(LivingEntity entity, float partialTick) {
        double yaw = Math.toRadians(Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        Vec3 shoulder = shoulder(entity, left, partialTick);
        Vec3 down = new Vec3(0.0, -1.0, 0.0).add(forward.scale(0.25)).normalize();
        ClientConstructs.Held held = ClientConstructs.heldBy(entity.getId(), false);
        Vec3 way = BoltArm.pointing(entity, partialTick) ? entity.getViewVector(partialTick) : down;
        if (held != null) {
            Vec3 out = held.center().subtract(shoulder);
            if (out.lengthSqr() > 1.0E-6) {
                way = down.lerp(out.normalize(), Mth.clamp(held.strength(), 0.0F, 1.0F));
            }
        }
        return shoulder.add(way.lengthSqr() < 1.0E-6 ? down.scale(ARM) : way.normalize().scale(ARM));
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.getMainArm() != HumanoidArm.RIGHT || player.isInvisible()
                || ClientRing.recharge(player, event.getPartialTick()) >= 0.0F || FlightPose.hands(event)) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) {
            return;
        }
        if (reach(player) <= 0.001F) {
            return;
        }
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        Vector3f hand = handPoint(player, event.getPartialTick());
        Vector3f from = shoulderFor(player, hand, event.getPartialTick());
        RechargeAnimation.arm(pose, buffers, event.getPackedLight(), player, renderer, 1.0F, hand, from);
        if (brace > 0.01F) {
            Vector3f wrist = new Vector3f(from).sub(hand).normalize().mul(WRIST_BACK)
                    .add(hand).add(UNDER_WRIST);
            Vector3f grip = new Vector3f(BRACE_REST).lerp(wrist, (float) Ease.smooth(brace));
            RechargeAnimation.arm(pose, buffers, event.getPackedLight(), player, renderer, -1.0F, grip, BRACE_FROM);
        }
    }

    private static float reach(LocalPlayer player) {
        long now = Util.getMillis();
        double seconds = Math.min(0.25, (now - beamAt) / 1000.0);
        float step = 1.0F - (float) Math.exp(-10.0 * seconds);
        beamAt = now;
        beam = Mth.lerp(step, beam, ClientRing.has(player, RingPayload.BEAM) ? 1.0F : 0.0F);
        ClientConstructs.Held held = ClientConstructs.heldBy(player.getId(), false);
        float construct = held == null ? 0.0F : Mth.clamp(held.strength(), 0.0F, 1.0F);
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        boolean pointing = BoltArm.pointing(player, partialTick);
        point = Mth.lerp(1.0F - (float) Math.exp(-(pointing ? POINT_UP : POINT_DOWN) * seconds), point,
                pointing ? 1.0F : 0.0F);
        brace = Mth.lerp(step, brace, BeamArm.brace(player, partialTick));
        float charge = Math.max(0.0F, BeamArm.gathering(player, partialTick)) * CHARGE_REACH;
        return Math.max(Math.max(construct, beam), Math.max(charge, point));
    }

    // Bolt and beam: the arm lies exactly along the view, dead straight ahead, wherever the hand is.
    private static Vector3f shoulderFor(LocalPlayer player, Vector3f hand, float partialTick) {
        ClientConstructs.Held held = ClientConstructs.heldBy(player.getId(), false);
        float construct = held == null ? 0.0F : Mth.clamp(held.strength(), 0.0F, 1.0F);
        float aim = Math.max(Math.max(point, beam), Math.max(0.0F, BeamArm.gathering(player, partialTick)));
        float straight = Mth.clamp(aim, 0.0F, 1.0F) * (1.0F - construct);
        Vector3f behind = new Vector3f(hand).add(0.0F, 0.0F, 1.0F);
        return new Vector3f(RechargeAnimation.SHOULDER_RIGHT).lerp(behind, straight);
    }

    public static Vector3f handPoint(LocalPlayer player, float partialTick) {
        ClientConstructs.Held held = ClientConstructs.heldBy(player.getId(), false);
        float construct = held == null ? 0.0F : Mth.clamp(held.strength(), 0.0F, 1.0F);
        float charge = Math.max(0.0F, BeamArm.gathering(player, partialTick)) * CHARGE_REACH;
        Vector3f hand = new Vector3f(RechargeAnimation.HAND_RIGHT)
                .lerp(new Vector3f(POINT).add(new Vector3f(POINT_KICK).mul(BoltArm.kick(player, partialTick))), point)
                .lerp(REACH, Math.max(Math.max(construct, beam), charge));
        if (held != null && held.smashing()) {
            Vec3 to = held.center().subtract(player.getEyePosition(partialTick));
            double pitch = Math.atan2(to.y, Math.max(0.5, to.horizontalDistance()));
            float up = (float) Mth.clamp((pitch - POUND_LOW) / (POUND_HIGH - POUND_LOW), 0.0, 1.0);
            hand.lerp(new Vector3f(SMASH).lerp(POUND_UP, up), construct);
        }
        float time = player.tickCount + partialTick;
        float tremble = HAND_TREMBLE * BeamArm.tremble(player, partialTick);
        hand.add(tremble * BeamArm.shake(time, 1), tremble * BeamArm.shake(time, 0), 0.0F)
                .add(new Vector3f(HAND_KICK).mul(BeamArm.kick(player, partialTick)));
        return hand.lerp(CallArm.UP_HIGH, CallArm.up(player, partialTick));
    }
}
