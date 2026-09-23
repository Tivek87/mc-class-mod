package nl.tivek.welcomescreen.client.character.lantern;

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
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.network.RingPayload;
import org.joml.Vector3f;

/**
 * What Green Lantern's arms do while the ring works. Every ability moves his arms its own way: while he
 * shapes a construct his right arm, the hand with the ring, reaches out towards it, and while he recharges
 * both arms play the lantern's own part (see {@link RechargeAnimation}).
 *
 * <p>Seen from outside the arms take a pose of their own ({@link LanternPose}); in first person the reaching
 * arm is drawn in place of the hand the game would draw.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class LanternArms {
    // Where the right hand ends up in first person while it holds a construct, in blocks in front of your
    // eyes (x to the right, y up, -z ahead): further out and further in than where the game rests it.
    private static final Vector3f REACH = new Vector3f(0.5F, -0.22F, -1.25F);
    // How far out the hand comes while the ring gathers its light for the beam, once it is full: most of the way.
    private static final float CHARGE_REACH = 0.7F;
    // How high on the body the shoulder of the reaching arm sits, as a part of the body's height, and how
    // far it is out to the side, in blocks.
    private static final double SHOULDER_HEIGHT = 0.8;
    private static final double SHOULDER_SIDE = 0.31;
    // How far the hand is from that shoulder, in blocks: the length of the arm.
    private static final double ARM = 0.68;

    // How far your own ring hand reaches out along the beam in first person, eased from frame to frame.
    private static float beam;
    private static long beamAt = Util.getMillis();

    private LanternArms() {
    }

    /**
     * Whoever recharges, flies, holds up or pours out one of the ring's shapes, or holds a construct gets a pose of
     * the mod's own; the game asks anew every frame. This comes last of all: a flight turns the whole body here and
     * turns it back once it is drawn, so nothing may call the drawing off after this.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        HumanoidModel.ArmPose pose = LanternPose.POSE.getValue();
        // A recharge in the air keeps the body in its flight pose; only the arms are the lantern's.
        FlightPose.pre(event);
        // Recharging, or the ring on its way to him: both arms play their part.
        if (ClientRing.recharge(player, event.getPartialTick()) >= 0.0F
                || ClientRing.arrival(player, event.getPartialTick()) >= 0.0F) {
            model.leftArmPose = pose;
            model.rightArmPose = pose;
            return;
        }
        // A flare or a storm: the ring fist thrown up high.
        if (FlareLight.up(player, event.getPartialTick()) > 0.0F) {
            model.rightArmPose = pose;
        }
        ClientConstructs.Held held = ClientConstructs.heldBy(player.getId());
        if (held == null) {
            return;
        }
        // The hand that defends holds up a shield; everything else hangs on the ring hand.
        if (held.defends()) {
            model.leftArmPose = pose;
        } else {
            model.rightArmPose = pose;
        }
    }

    /** The body a flight turned is turned back once it is drawn. */
    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        FlightPose.post(event);
    }

    /**
     * The pose itself, one arm at a time: the flight and the ring's shapes, with on top the lantern's part, or the
     * arm reaching out to a construct it holds.
     */
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
        FlareLight.pose(model, entity, arm);
        ClientConstructs.Held held = ClientConstructs.heldBy(entity.getId());
        if (held != null && arm == (held.defends() ? HumanoidArm.LEFT : HumanoidArm.RIGHT)) {
            reach(held.defends() ? model.leftArm : model.rightArm, entity, held, partialTick);
        }
    }

    /**
     * The arm points at the construct hanging beside its owner, so a hand holds it up instead of the light
     * simply floating there. It swings out as the construct appears and falls back as it goes. In flight it
     * reaches from where the shoulder really is, with the body lying along the way he flies.
     */
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

    /** The right shoulder of someone seen from outside, in the world. */
    private static Vec3 shoulder(LivingEntity entity, Vec3 left, float partialTick) {
        return entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * SHOULDER_HEIGHT, 0.0)
                .subtract(left.scale(SHOULDER_SIDE));
    }

    /**
     * Where the ring sits on someone you see from outside: at the end of the right arm. That arm reaches out
     * towards a construct it is holding ({@link #reach}) and hangs by the side otherwise, and the beam of a
     * construct starts here, so it always leaves the same hand the body reaches with.
     */
    public static Vec3 ringPoint(LivingEntity entity, float partialTick) {
        double yaw = Math.toRadians(Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        Vec3 shoulder = shoulder(entity, left, partialTick);
        Vec3 down = new Vec3(0.0, -1.0, 0.0).add(forward.scale(0.25)).normalize();
        ClientConstructs.Held held = ClientConstructs.heldBy(entity.getId());
        Vec3 way = down;
        // A shield hangs on the other hand, so the ring hand stays where it is and only feeds it.
        if (held != null && !held.defends()) {
            Vec3 out = held.center().subtract(shoulder);
            if (out.lengthSqr() > 1.0E-6) {
                // The arm swings out as the construct comes in, so the hand follows it on the way.
                way = down.lerp(out.normalize(), Mth.clamp(held.strength(), 0.0F, 1.0F));
            }
        }
        return shoulder.add(way.lengthSqr() < 1.0E-6 ? down.scale(ARM) : way.normalize().scale(ARM));
    }

    /**
     * Your own right hand in first person while you hold a construct or pour out the beam: the game draws it
     * resting at the bottom of your screen, so the mod draws it reaching out instead. During the take-off of a
     * flight both your hands play that part (see {@link FlightPose#hands}).
     */
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
        // A shield is held up by the other hand, so the ring hand keeps the pose the game gives it.
        if (reach(player) <= 0.001F) {
            return;
        }
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        RechargeAnimation.arm(pose, buffers, event.getPackedLight(), player, renderer, 1.0F,
                handPoint(player, event.getPartialTick()), RechargeAnimation.SHOULDER_RIGHT);
    }

    /**
     * How far your own ring hand reaches out in first person: towards the construct you hold, or along the beam
     * (most of the way already while the ring gathers its light for it), 0 = resting where the game keeps it, 1 = all
     * the way out.
     */
    private static float reach(LocalPlayer player) {
        long now = Util.getMillis();
        float step = 1.0F - (float) Math.exp(-10.0 * Math.min(0.25, (now - beamAt) / 1000.0));
        beamAt = now;
        beam = Mth.lerp(step, beam, ClientRing.has(player, RingPayload.BEAM) ? 1.0F : 0.0F);
        ClientConstructs.Held held = ClientConstructs.heldBy(player.getId());
        float construct = held == null || held.defends() ? 0.0F : Mth.clamp(held.strength(), 0.0F, 1.0F);
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float charge = Math.max(0.0F, BeamCharge.charge(player, partialTick)) * CHARGE_REACH;
        return Math.max(Math.max(construct, beam), charge);
    }

    /**
     * Where your own right hand is in first person, in blocks in front of your eyes: where the game rests it,
     * or out towards the construct you are holding, or along the beam, or thrown up high for a flare or a storm.
     */
    static Vector3f handPoint(LocalPlayer player, float partialTick) {
        ClientConstructs.Held held = ClientConstructs.heldBy(player.getId());
        float construct = held == null || held.defends() ? 0.0F : Mth.clamp(held.strength(), 0.0F, 1.0F);
        // Gathering light for the beam the hand comes out, the further the fuller the ring.
        float charge = Math.max(0.0F, BeamCharge.charge(player, partialTick)) * CHARGE_REACH;
        Vector3f hand = new Vector3f(RechargeAnimation.HAND_RIGHT).lerp(REACH, Math.max(Math.max(construct, beam),
                charge));
        // Thrown up high for a flare or a storm.
        return hand.lerp(FlareLight.UP_HIGH, FlareLight.up(player, partialTick));
    }
}
