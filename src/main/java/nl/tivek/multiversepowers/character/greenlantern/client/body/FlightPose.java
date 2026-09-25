package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
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
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientFlight;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.FlightLimbs.beam;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.FlightLimbs.brace;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.FlightLimbs.flightArm;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.FlightLimbs.headLift;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.FlightLimbs.legs;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.FlightLimbs.slam;

public final class FlightPose {
    private static final double LINED_UP = 0.74;
    private static final int LAND_TICKS = 8;
    private static final float BLEND = 9.0F;
    private static final float CHARGE_AIM = 0.8F;
    private static final float POINT_UP = 30.0F;
    private static final float POINT_DOWN = 7.0F;
    private static final float BOLT_KICK = 0.14F;
    private static final float POINT_IN = 0.08F;
    private static final float SLAM_TICKS = 23.0F;
    private static final float SLAM_HAND_TICKS = 21.0F;
    private static final float KNEEL_HOLD = 15.0F;
    private static final float KNEEL_RISE = 7.0F;
    private static final float KNEEL_DROP = 0.32F;
    static final float KNEEL_LEAN = 1.0F;
    private static final float CROUCH_DROP = 0.125F;
    private static final float DOWN_KNEE = 1.52F;
    private static final float STEP_KNEE = 1.4F;
    private static final float HIP_HEIGHT = 12.0F * 0.9375F / 16.0F;
    private static final Vector3f COCKED = new Vector3f(0.55F, 0.42F, -0.72F);
    private static final Vector3f PLANTED = new Vector3f(0.18F, -0.95F, -0.9F);
    private static final Vector3f ARM_FROM = new Vector3f(1.5F, -0.4F, 0.35F);

    private static final Map<Integer, Blend> BLENDS = new HashMap<>();
    @Nullable
    private static Frame frame;
    @Nullable
    private static BodyTurn body;

    private FlightPose() {
    }

    static final class Blend {
        float fly;
        float beam;
        float brace;
        float point;
        float dome;
        float ram;
        long last = Util.getMillis();

        void toward(boolean flying, float beaming, float bracing, boolean pointing, boolean domed, boolean ramming) {
            long now = Util.getMillis();
            float seconds = Math.min(0.25F, (now - this.last) / 1000.0F);
            float step = 1.0F - (float) Math.exp(-BLEND * seconds);
            this.last = now;
            this.fly = Mth.lerp(step, this.fly, flying ? 1.0F : 0.0F);
            this.beam = Mth.lerp(step, this.beam, beaming);
            this.brace = Mth.lerp(step, this.brace, bracing);
            this.point = Mth.lerp(1.0F - (float) Math.exp(-(pointing ? POINT_UP : POINT_DOWN) * seconds), this.point,
                    pointing ? 1.0F : 0.0F);
            this.dome = Mth.lerp(step, this.dome, domed ? 1.0F : 0.0F);
            this.ram = Mth.lerp(step, this.ram, ramming ? 1.0F : 0.0F);
        }

        boolean idle() {
            return this.fly < 0.01F && this.beam < 0.01F && this.brace < 0.01F && this.point < 0.01F
                    && this.dome < 0.01F && this.ram < 0.01F;
        }
    }

    record Frame(int entity, float t, float fast, float tilt, float roll, float land, boolean sinking,
            float slam, float brace, float time, Blend blend, Vec3 pivot, Vec3 forward, Vec3 left, Quaternionf turn,
            float kneel) {
    }

    private record BodyTurn(int entity, Vec3 pivot, float dip, Quaternionf turn, boolean kneel, float drop,
            float lean, Vec3 left) {
    }

    static boolean pre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        float partialTick = event.getPartialTick();
        float t = ClientRing.flight(player, partialTick);
        ClientFlight.Motion motion = ClientFlight.motion(player);
        boolean flying = t >= 0.0F;
        float charge = BeamArm.gathering(player, partialTick);
        float aim = ClientRing.has(player, RingPayload.BEAM) ? 1.0F : CHARGE_AIM * Math.max(0.0F, charge);
        boolean beaming = aim > 0.0F;
        boolean pointing = BoltArm.pointing(player, partialTick);
        boolean domed = ClientRing.has(player, RingPayload.DOME);
        boolean ramming = flying && ClientRing.has(player, RingPayload.SHIELD);
        float land = motion == null || motion.sinceEnd >= LAND_TICKS ? 0.0F
                : Mth.sin((motion.sinceEnd + partialTick) / LAND_TICKS * Mth.PI);
        float slam = ClientFlight.slam(player, partialTick);
        boolean slamming = slam >= 0.0F && slam < SLAM_TICKS;
        boolean dropping = ClientFlight.dropping(player);
        float brace = flying || dropping ? ClientFlight.brace(player, partialTick) : 0.0F;
        if (slamming) {
            land = 0.0F;
        }
        frame = null;
        Blend blend = BLENDS.get(player.getId());
        if (blend == null) {
            if (!flying && !dropping && !beaming && !pointing && !domed && !slamming && land <= 0.0F) {
                return false;
            }
            blend = new Blend();
            BLENDS.put(player.getId(), blend);
        }
        blend.toward(flying, aim, beaming ? BeamArm.brace(player, partialTick) : 0.0F, pointing, domed, ramming);
        if (!flying && !dropping && !beaming && !pointing && !domed && !slamming && blend.idle() && land <= 0.0F) {
            BLENDS.remove(player.getId());
            return false;
        }
        double yaw = Math.toRadians(Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        float fast = 0.0F;
        float tilt = 0.0F;
        float roll = 0.0F;
        if (flying && motion != null && !ClientRing.has(player, RingPayload.DESCENT)) {
            Vec3 v = motion.velocity;
            double ahead = v.dot(forward);
            double side = -v.dot(left);
            double speed = v.length();
            double lined = LINED_UP * ClientFlight.fullSpeed();
            double slow = 0.19 * lined;
            fast = (float) Ease.smooth((speed - slow) / (lined - slow));
            float drift = (float) Mth.clamp(ahead * 1.1, -0.3, 0.45);
            float along = (float) Math.atan2(Math.max(ahead, 0.0), v.y);
            tilt = Mth.lerp(fast, drift, along);
            roll = motion.bank + (float) Mth.clamp(side * 0.8, -0.4, 0.4) * (1.0F - 0.5F * fast);
            if (t < ClientFlight.ARISE) {
                float in = (float) Ease.smooth((t - ClientFlight.SWEEP) / (ClientFlight.ARISE - ClientFlight.SWEEP));
                tilt *= in;
                roll *= in;
                fast *= in;
            }
            tilt *= 1.0F - brace;
            roll *= 1.0F - brace;
            fast *= 1.0F - brace;
        }
        Vec3 pivot = new Vec3(0.0, player.getBbHeight() * 0.55, 0.0);
        Quaternionf turn = new Quaternionf().rotateAxis(roll, (float) forward.x, 0.0F, (float) forward.z)
                .rotateAxis(tilt, (float) left.x, 0.0F, (float) left.z);
        float kneel = slamming ? kneel(slam) : 0.0F;
        frame = new Frame(player.getId(), t, fast, tilt, roll, land, ClientRing.has(player, RingPayload.DESCENT),
                slamming ? slam : -1.0F, brace, player.tickCount + partialTick, blend, pivot, forward, left, turn,
                kneel);
        float dip = dip(t, land);
        float lean = KNEEL_LEAN * kneel;
        float drop = kneel <= 0.0F ? 0.0F
                : Math.max(0.0F, KNEEL_DROP - (player.isCrouching() ? CROUCH_DROP : 0.0F)) * kneel;
        // Kept here for turnModel, not applied now: the model turns before the name tag is drawn, which must not.
        body = Math.abs(tilt) > 1.0E-3F || Math.abs(roll) > 1.0E-3F || dip > 1.0E-3F || kneel > 0.0F
                ? new BodyTurn(player.getId(), pivot, dip, turn, kneel > 0.0F, drop, lean, left) : null;
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        model.leftArmPose = LanternPose.POSE.getValue();
        model.rightArmPose = LanternPose.POSE.getValue();
        if (flying || kneel > 0.0F) {
            model.crouching = false;
        }
        if (kneel > 0.0F) {
            // The game's legs cannot bend at the knee: hidden here and drawn in two halves instead (see
            // KneelLegs). Armour copies these legs, so it shrinks to the upper half along with them.
            model.rightLeg.visible = false;
            model.leftLeg.visible = false;
            model.rightPants.visible = false;
            model.leftPants.visible = false;
            model.rightLeg.yScale = 0.5F;
            model.leftLeg.yScale = 0.5F;
        }
        return true;
    }

    static void turnModel(AbstractClientPlayer player, PoseStack pose) {
        BodyTurn turn = body;
        if (turn == null || turn.entity() != player.getId()) {
            return;
        }
        pose.translate(turn.pivot().x, turn.pivot().y - turn.dip(), turn.pivot().z);
        pose.mulPose(turn.turn());
        pose.translate(-turn.pivot().x, -turn.pivot().y, -turn.pivot().z);
        if (turn.kneel()) {
            pose.translate(0.0F, HIP_HEIGHT - turn.drop(), 0.0F);
            pose.mulPose(new Quaternionf().rotateAxis(turn.lean(), (float) turn.left().x, 0.0F,
                    (float) turn.left().z));
            pose.translate(0.0F, -HIP_HEIGHT, 0.0F);
        }
    }

    static void post(RenderPlayerEvent.Post event) {
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        model.rightLeg.yScale = 1.0F;
        model.leftLeg.yScale = 1.0F;
        body = null;
        frame = null;
    }

    @Nullable
    static float[] knees(LivingEntity entity) {
        Frame f = frame;
        if (f == null || f.entity() != entity.getId() || f.kneel() <= 0.0F) {
            return null;
        }
        return new float[] { DOWN_KNEE * f.kneel(), STEP_KNEE * f.kneel() };
    }

    private static float dip(float t, float land) {
        float gather = t < 0.0F ? 0.0F
                : (float) (Ease.smooth(t / ClientFlight.GATHER)
                        * (1.0 - Ease.smooth((t - ClientFlight.GATHER) / 3.0)));
        return 0.12F * gather + 0.14F * land;
    }

    static boolean posing(LivingEntity entity) {
        return frame != null && frame.entity() == entity.getId();
    }

    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        Frame f = frame;
        if (f == null || f.entity() != entity.getId()) {
            return;
        }
        Blend blend = f.blend();
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart limb = right ? model.rightArm : model.leftArm;
        float side = right ? 1.0F : -1.0F;
        float pitch = entity.getViewXRot(partialTick) * Mth.DEG_TO_RAD;
        model.head.xRot = Mth.clamp(pitch - f.tilt(), -1.35F, 1.1F);
        if (blend.fly > 0.0F) {
            float fly = blend.fly;
            float[] base = flightArm(f, side);
            limb.xRot = Mth.lerp(fly, limb.xRot, base[0]);
            limb.yRot = Mth.lerp(fly, limb.yRot, base[1]);
            limb.zRot = Mth.lerp(fly, limb.zRot, base[2]);
            legs(model, f, fly);
            float look = headLift(f);
            model.head.xRot = Mth.clamp(model.head.xRot + look * fly, -1.35F, 1.1F);
        }
        if (right && blend.point > 0.0F) {
            float kick = BOLT_KICK * BoltArm.kick(entity, partialTick);
            limb.xRot = Mth.lerp(blend.point, limb.xRot, -Mth.HALF_PI + model.head.xRot - kick);
            limb.yRot = Mth.lerp(blend.point, limb.yRot, model.head.yRot - POINT_IN);
            limb.zRot = Mth.lerp(blend.point, limb.zRot, 0.0F);
        }
        if (blend.beam > 0.0F) {
            beam(model, limb, right, blend, entity, partialTick, f.time());
        }
        if (!right && blend.ram > 0.0F) {
            limb.xRot = Mth.lerp(blend.ram, limb.xRot, -2.95F);
            limb.yRot = Mth.lerp(blend.ram, limb.yRot, 0.12F);
            limb.zRot = Mth.lerp(blend.ram, limb.zRot, -0.05F);
        }
        if (blend.dome > 0.0F) {
            limb.xRot = Mth.lerp(blend.dome, limb.xRot, -0.55F);
            limb.yRot = Mth.lerp(blend.dome, limb.yRot, 0.0F);
            limb.zRot = Mth.lerp(blend.dome, limb.zRot, side * 1.2F);
        }
        if (f.brace() > 0.0F) {
            brace(model, limb, right, f.brace());
        }
        if (f.slam() >= 0.0F) {
            slam(model, limb, right, f.slam(), f.kneel());
        }
    }

    private static float kneel(float age) {
        return (float) (Ease.smooth(age / 1.0) * (1.0 - Ease.smooth((age - KNEEL_HOLD) / KNEEL_RISE)));
    }

    static Vec3 turned(LivingEntity entity, Vec3 point, float partialTick) {
        Frame f = frame;
        if (f == null || f.entity() != entity.getId() || body == null) {
            return point;
        }
        Vec3 base = entity.getPosition(partialTick).add(f.pivot());
        Vector3f local = new Vector3f((float) (point.x - base.x), (float) (point.y - base.y),
                (float) (point.z - base.z));
        f.turn().transform(local);
        return base.add(local.x, local.y, local.z);
    }

    static Vec3 untilted(LivingEntity entity, Vec3 way) {
        Frame f = frame;
        if (f == null || f.entity() != entity.getId() || body == null) {
            return way;
        }
        Vector3f local = new Vector3f((float) way.x, (float) way.y, (float) way.z);
        new Quaternionf(f.turn()).conjugate().transform(local);
        return new Vec3(local.x, local.y, local.z);
    }

    @Nullable
    static Quaternionf bodyTurn(LivingEntity entity) {
        Frame f = frame;
        if (f == null || f.entity() != entity.getId() || body == null) {
            return null;
        }
        // Model's own axes: x to the left, y down, -z ahead.
        return new Quaternionf().rotateAxis(f.roll(), 0.0F, 0.0F, -1.0F).rotateAxis(f.tilt(), 1.0F, 0.0F, 0.0F);
    }

    public static void clear() {
        BLENDS.clear();
        frame = null;
        body = null;
    }

    static boolean hands(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isInvisible() || !player.getMainHandItem().isEmpty()) {
            return false;
        }
        float slam = ClientFlight.slam(player, event.getPartialTick());
        if (slam >= 0.0F && slam < SLAM_HAND_TICKS) {
            slamHand(event, player, slam);
            return true;
        }
        float brace = ClientFlight.brace(player, event.getPartialTick());
        if (brace > 0.01F) {
            event.setCanceled(true);
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
                RechargeAnimation.arm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
                        player, renderer, 1.0F, new Vector3f(RechargeAnimation.HAND_RIGHT).lerp(COCKED, brace),
                        ARM_FROM);
            }
            return true;
        }
        float t = ClientRing.flight(player, event.getPartialTick());
        if (t < 0.0F || t > ClientFlight.SWEEP + 2.0F) {
            return false;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return true;
        }
        float gather = (float) Ease.smooth(t / ClientFlight.GATHER);
        float sweep = (float) Ease.smooth(
                (t - ClientFlight.GATHER) / (ClientFlight.SWEEP + 2.0F - ClientFlight.GATHER));
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        for (float side = -1.0F; side <= 1.0F; side += 2.0F) {
            Vector3f rest = side > 0.0F ? new Vector3f(RechargeAnimation.HAND_RIGHT)
                    : new Vector3f(-RechargeAnimation.HAND_RIGHT.x, RechargeAnimation.HAND_RIGHT.y,
                            RechargeAnimation.HAND_RIGHT.z);
            Vector3f chest = new Vector3f(side * 0.16F, -0.5F, -0.5F);
            Vector3f down = new Vector3f(side * 0.75F, -1.25F, -0.25F);
            Vector3f hand = new Vector3f(rest).lerp(chest, gather).lerp(down, sweep);
            Vector3f from = new Vector3f(side * 1.5F, -0.4F, 0.35F);
            RechargeAnimation.arm(pose, buffers, event.getPackedLight(), player, renderer, side, hand, from);
        }
        return true;
    }

    private static void slamHand(RenderHandEvent event, LocalPlayer player, float age) {
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        float down = (float) Ease.smooth(age / 1.4);
        float back = (float) Ease.smooth((age - KNEEL_HOLD) / 6.0);
        float shudder = age < 6.0F ? 0.025F * (1.0F - age / 6.0F) * Mth.sin(age * 9.0F) : 0.0F;
        Vector3f hand = new Vector3f(COCKED).lerp(planted(minecraft, player, event.getPartialTick()), down)
                .lerp(new Vector3f(RechargeAnimation.HAND_RIGHT), back).add(shudder, shudder * 0.5F, 0.0F);
        RechargeAnimation.arm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player,
                renderer, 1.0F, hand, ARM_FROM);
    }

    private static Vector3f planted(Minecraft minecraft, LocalPlayer player, float partialTick) {
        Vec3 facing = ClientConstructs.slamFacing(player.getId());
        if (facing == null) {
            facing = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick));
        }
        Vec3 forward = new Vec3(facing.x, 0.0, facing.z);
        if (forward.lengthSqr() < 1.0E-6) {
            return new Vector3f(PLANTED);
        }
        forward = forward.normalize();
        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        Vec3 fist = player.getPosition(partialTick).add(forward.scale(0.5)).add(right.scale(0.3)).add(0.0, 0.1, 0.0);
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 way = fist.subtract(camera.getPosition());
        float ahead = (float) way.dot(new Vec3(camera.getLookVector()));
        if (ahead < 0.3F) {
            return new Vector3f(PLANTED);
        }
        float x = (float) -way.dot(new Vec3(camera.getLeftVector()));
        float y = (float) way.dot(new Vec3(camera.getUpVector()));
        return new Vector3f(Mth.clamp(x, -0.8F * ahead, 0.8F * ahead), Math.max(y, -0.55F * ahead), -ahead);
    }
}
