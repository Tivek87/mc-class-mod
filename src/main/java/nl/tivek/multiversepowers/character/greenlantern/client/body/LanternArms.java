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

/**
 * What Green Lantern's arms do while the ring works. Every ability moves his arms its own way: while he
 * shapes a construct his right arm, the hand with the ring, reaches out towards it, and while he recharges
 * both arms play the lantern's own part (see {@link RechargeAnimation}).
 *
 * <p>Seen from outside the arms take a pose of their own ({@link LanternPose}); in first person the reaching
 * arm is drawn in place of the hand the game would draw.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class LanternArms {
    // Where the right hand ends up in first person while it holds a construct, in blocks in front of your
    // eyes (x to the right, y up, -z ahead): further out and further in than where the game rests it.
    private static final Vector3f REACH = new Vector3f(0.5F, -0.22F, -1.25F);
    // Pounding a bubble into the ground: where the fist comes down to as it slams, and where it goes up to as the ring
    // swings the bubble up high; and how far over the level of your eyes (as an angle, in radians) the bubble is
    // swung for the fist to be all the way up, and how far below them it lies on the ground for all the way down.
    private static final Vector3f SMASH = new Vector3f(0.36F, -0.72F, -0.95F);
    private static final Vector3f POUND_UP = new Vector3f(0.42F, 0.02F, -1.05F);
    private static final double POUND_HIGH = 0.5;
    private static final double POUND_LOW = -0.08;
    // How far out the hand comes while the ring gathers its light for the beam, once it is full: most of the way.
    private static final float CHARGE_REACH = 0.7F;
    // Your own ring hand shooting bolts (see BoltArm), in blocks in front of your eyes: where it points from, straight
    // ahead under the crosshair, how far each bolt kicks it (up and back towards you), and how quickly it comes up and
    // goes down again, per second.
    private static final Vector3f POINT = new Vector3f(0.36F, -0.24F, -1.3F);
    private static final Vector3f POINT_KICK = new Vector3f(0.0F, 0.04F, 0.12F);
    private static final float POINT_UP = 30.0F;
    private static final float POINT_DOWN = 7.0F;
    // How high on the body the shoulder of the reaching arm sits, as a part of the body's height, and how
    // far it is out to the side, in blocks.
    private static final double SHOULDER_HEIGHT = 0.8;
    private static final double SHOULDER_SIDE = 0.31;
    // How far the hand is from that shoulder, in blocks: the length of the arm.
    private static final double ARM = 0.68;
    // Your own ring hand with the beam in first person (see BeamArm), in blocks in front of your eyes: where the
    // beam breaking loose kicks it (up and back towards you) and how far it trembles at its hardest.
    private static final Vector3f HAND_KICK = new Vector3f(0.0F, 0.09F, 0.2F);
    private static final float HAND_TREMBLE = 0.018F;
    // Your other hand bracing the ring arm's wrist: it grips this far back along the ring arm from the hand and a
    // little under it, comes up from out of sight below, and its arm reaches in from low on your left.
    private static final float WRIST_BACK = 0.17F;
    private static final Vector3f UNDER_WRIST = new Vector3f(-0.03F, -0.075F, 0.0F);
    private static final Vector3f BRACE_REST = new Vector3f(-0.45F, -1.1F, -0.7F);
    private static final Vector3f BRACE_FROM = new Vector3f(-1.25F, -1.15F, 0.25F);

    // How far your own ring hand reaches out along the beam in first person, how far it points for the bolts, and how
    // far your other hand has come over to brace it, eased from frame to frame.
    private static float beam;
    private static float point;
    private static float brace;
    private static long beamAt = Util.getMillis();

    private LanternArms() {
    }

    /**
     * Whoever recharges, flies, holds up or pours out one of the ring's shapes, or holds a construct gets a pose of
     * the mod's own; the game asks anew every frame. This comes last of all, so what it works out is for a player who
     * is really drawn.
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
        // The sword and shield of the construct wheel: both arms, the upper body and the legs are theirs (the upper body
        // bends into the moves once the game has posed it, see SwordArms.lean).
        if (SwordArms.posing(player, event.getPartialTick())) {
            model.leftArmPose = pose;
            model.rightArmPose = pose;
            SwordArms.spin(event);
        }
        // Calling an air strike: the ring fist thrown up high. A scan: the ring fist held out, sweeping. Calling the
        // giant hands: the ring arm flung out towards every one.
        if (CallArm.up(player, event.getPartialTick()) > 0.0F || ScanArm.out(player, event.getPartialTick()) > 0.0F
                || HandsArm.out(player, event.getPartialTick()) > 0.0F) {
            model.rightArmPose = pose;
        }
        // The hand that defends holds up a shield; everything else hangs on the ring hand.
        if (ClientConstructs.heldBy(player.getId(), false) != null) {
            model.rightArmPose = pose;
        }
        if (ClientConstructs.heldBy(player.getId(), true) != null) {
            model.leftArmPose = pose;
        }
    }

    /** Once a player is drawn, the turns of his body for a flight or a spinning cut are done with. */
    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        SwordArms.unspin(event);
        FlightPose.post(event);
    }

    /**
     * While the game draws a player's model (see PlayerRendererMixin): his whole body turns with his flight, then round
     * for a spinning cut. Only the model turns; his name over his head, drawn after it, stays upright.
     *
     * @param scale how much bigger or smaller than usual he is drawn: the game has scaled everything by it already
     */
    public static void turnBody(AbstractClientPlayer player, PoseStack pose, float scale) {
        if (scale <= 0.0F) {
            return;
        }
        // The turns are worked out in the world's own blocks, before his size.
        pose.scale(1.0F / scale, 1.0F / scale, 1.0F / scale);
        FlightPose.turnModel(player, pose);
        SwordArms.turnModel(player, pose);
        pose.scale(scale, scale, scale);
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
        SwordArms.pose(model, entity, arm);
        ScanArm.pose(model, entity, arm);
        CallArm.pose(model, entity, arm);
        HandsArm.pose(model, entity, arm);
        ClientConstructs.Held held = ClientConstructs.heldBy(entity.getId(), arm == HumanoidArm.LEFT);
        if (held != null) {
            reach(arm == HumanoidArm.LEFT ? model.leftArm : model.rightArm, entity, held, partialTick);
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
        // A shield hangs on the other hand, so the ring hand stays where it is and only feeds it.
        ClientConstructs.Held held = ClientConstructs.heldBy(entity.getId(), false);
        // Shooting bolts, the ring arm points where he aims.
        Vec3 way = BoltArm.pointing(entity, partialTick) ? entity.getViewVector(partialTick) : down;
        if (held != null) {
            Vec3 out = held.center().subtract(shoulder);
            if (out.lengthSqr() > 1.0E-6) {
                // The arm swings out as the construct comes in, so the hand follows it on the way.
                way = down.lerp(out.normalize(), Mth.clamp(held.strength(), 0.0F, 1.0F));
            }
        }
        return shoulder.add(way.lengthSqr() < 1.0E-6 ? down.scale(ARM) : way.normalize().scale(ARM));
    }

    /**
     * Your own right hand in first person while you shoot bolts, hold a construct or pour out the beam: the game draws
     * it resting at the bottom of your screen, so the mod draws it reaching out instead. During the take-off of a
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
        Vector3f hand = handPoint(player, event.getPartialTick());
        RechargeAnimation.arm(pose, buffers, event.getPackedLight(), player, renderer, 1.0F, hand,
                RechargeAnimation.SHOULDER_RIGHT);
        // Your other hand comes up to grip the ring arm's wrist while the ring fills and the beam pours.
        if (brace > 0.01F) {
            Vector3f wrist = new Vector3f(RechargeAnimation.SHOULDER_RIGHT).sub(hand).normalize().mul(WRIST_BACK)
                    .add(hand).add(UNDER_WRIST);
            Vector3f grip = new Vector3f(BRACE_REST).lerp(wrist, (float) Ease.smooth(brace));
            RechargeAnimation.arm(pose, buffers, event.getPackedLight(), player, renderer, -1.0F, grip, BRACE_FROM);
        }
    }

    /**
     * How far your own ring hand reaches out in first person: towards the construct you hold, or along the beam
     * (most of the way already while the ring gathers its light for it), 0 = resting where the game keeps it, 1 = all
     * the way out.
     */
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

    /**
     * Where your own right hand is in first person, in blocks in front of your eyes: where the game rests it,
     * or pointing straight ahead while you shoot bolts (kicking with each one), or out towards the construct you are
     * holding, or along the beam (trembling and kicking with it), or thrown up high to call an air strike.
     */
    public static Vector3f handPoint(LocalPlayer player, float partialTick) {
        ClientConstructs.Held held = ClientConstructs.heldBy(player.getId(), false);
        float construct = held == null ? 0.0F : Mth.clamp(held.strength(), 0.0F, 1.0F);
        // Gathering light for the beam the hand comes out, the further the fuller the ring.
        float charge = Math.max(0.0F, BeamArm.gathering(player, partialTick)) * CHARGE_REACH;
        Vector3f hand = new Vector3f(RechargeAnimation.HAND_RIGHT)
                .lerp(new Vector3f(POINT).add(new Vector3f(POINT_KICK).mul(BoltArm.kick(player, partialTick))), point)
                .lerp(REACH, Math.max(Math.max(construct, beam), charge));
        // Pounding a bubble into the ground, the fist goes up and down with it: up as the ring swings it up high, and
        // down with every slam.
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
        // Thrown up high to call an air strike.
        return hand.lerp(CallArm.UP_HIGH, CallArm.up(player, partialTick));
    }
}
