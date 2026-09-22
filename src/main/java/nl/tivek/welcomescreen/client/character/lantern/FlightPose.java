package nl.tivek.welcomescreen.client.character.lantern;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
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
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import nl.tivek.welcomescreen.network.RingPayload;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * How Green Lantern's body moves while the ring works, seen from outside: the take-off, the flight itself, and
 * what his arms do with the shapes he holds up or pours out, all blended into each other so one flows into the
 * next.
 * <ul>
 * <li><b>Take-off</b>: a small dip with both fists brought to the chest, then the arms sweep down along the sides,
 * the head goes up and he rises.</li>
 * <li><b>Hovering</b>: upright, arms a little out, legs hanging loose and drifting.</li>
 * <li><b>Flying</b>: the faster he goes the more his body lines up with the way he flies, arms back along his
 * sides and legs together, like the pictures. He banks into his turns and leans into sideways slides, dives head
 * first and climbs head up, and his head keeps looking where he looks.</li>
 * <li>An empty ring lets him sink with his arms up; landing is a short dip, and a landing at full speed a slam:
 * down on one knee with his ring fist in the ground.</li>
 * <li>On top of that, standing or flying: the ring hand points along the beam, both hands hold the dome open,
 * and in flight the shield hand goes out in front, fist first, into the ram cone.</li>
 * </ul>
 * The whole body is turned in {@link #pre}, before the game draws it; the limbs are set in {@link #pose}, which
 * the game calls while it poses the arms.
 */
final class FlightPose {
    // Speed at which the body lies fully along the way it flies, in blocks per tick.
    private static final double LINED_UP = 1.3;
    // How long a landing dip lasts, in ticks.
    private static final int LAND_TICKS = 8;
    // How quickly the arms blend from one thing to the next, per second.
    private static final float BLEND = 9.0F;
    // How long the pose of a landing slam lasts, in ticks; your own fist shows in first person this long.
    private static final float SLAM_TICKS = 20.0F;
    private static final float SLAM_HAND_TICKS = 14.0F;

    private static final Map<Integer, Blend> BLENDS = new HashMap<>();
    // The player being drawn right now, and whether his body was turned (so it is turned back afterwards).
    @Nullable
    private static Frame frame;
    private static boolean pushed;

    private FlightPose() {
    }

    /** How far each of the arm shapes has come in for one player, eased from frame to frame. */
    private static final class Blend {
        float fly;
        float beam;
        float dome;
        float ram;
        long last = Util.getMillis();

        void toward(boolean flying, boolean beaming, boolean domed, boolean ramming) {
            long now = Util.getMillis();
            float step = 1.0F - (float) Math.exp(-BLEND * Math.min(0.25F, (now - this.last) / 1000.0F));
            this.last = now;
            this.fly = Mth.lerp(step, this.fly, flying ? 1.0F : 0.0F);
            this.beam = Mth.lerp(step, this.beam, beaming ? 1.0F : 0.0F);
            this.dome = Mth.lerp(step, this.dome, domed ? 1.0F : 0.0F);
            this.ram = Mth.lerp(step, this.ram, ramming ? 1.0F : 0.0F);
        }

        boolean idle() {
            return this.fly < 0.01F && this.beam < 0.01F && this.dome < 0.01F && this.ram < 0.01F;
        }
    }

    /**
     * Everything one frame of one player needs.
     *
     * @param t      ticks since he took off, or -1 when he does not fly
     * @param fast   0 hovering, 1 flying fully lined up
     * @param tilt   how far the body tips forward, in radians (a quarter turn lies flat)
     * @param roll   how far it leans sideways, in radians: positive to his right
     * @param land   0 to 1: the dip of a landing
     * @param time   ticks, for everything that sways
     */
    private record Frame(int entity, float t, float fast, float tilt, float roll, float land, boolean sinking,
            float slam, float time, Blend blend, Vec3 pivot, Vec3 forward, Vec3 left, Quaternionf turn) {
    }

    /**
     * Before the game draws a player: works out his flight pose and turns his whole body with it.
     *
     * @return true when the mod poses his arms this frame
     */
    static boolean pre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        float partialTick = event.getPartialTick();
        float t = ClientRing.flight(player, partialTick);
        ClientFlight.Motion motion = ClientFlight.motion(player);
        boolean flying = t >= 0.0F;
        boolean beaming = ClientRing.has(player, RingPayload.BEAM);
        boolean domed = ClientRing.has(player, RingPayload.DOME);
        boolean ramming = flying && ClientRing.has(player, RingPayload.SHIELD);
        float land = motion == null || motion.sinceEnd >= LAND_TICKS ? 0.0F
                : Mth.sin((motion.sinceEnd + partialTick) / LAND_TICKS * Mth.PI);
        float slam = ClientFlight.slam(player, partialTick);
        boolean slamming = slam >= 0.0F && slam < SLAM_TICKS;
        frame = null;
        Blend blend = BLENDS.get(player.getId());
        if (blend == null) {
            if (!flying && !beaming && !domed && !slamming && land <= 0.0F) {
                return false;
            }
            blend = new Blend();
            BLENDS.put(player.getId(), blend);
        }
        blend.toward(flying, beaming, domed, ramming);
        // Only forgotten once nothing is wanted any more and everything has blended back out.
        if (!flying && !beaming && !domed && !slamming && blend.idle() && land <= 0.0F) {
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
            fast = (float) ClientFlight.smooth((speed - 0.25) / (LINED_UP - 0.25));
            // Hovering he only leans a little into where he drifts; fast, his body lies along the way he flies.
            float drift = (float) Mth.clamp(ahead * 1.1, -0.3, 0.45);
            float along = (float) Math.atan2(Math.max(ahead, 0.0), v.y);
            tilt = Mth.lerp(fast, drift, along);
            roll = motion.bank + (float) Mth.clamp(side * 0.8, -0.4, 0.4) * (1.0F - 0.5F * fast);
            // The take-off itself stays upright; the lean fades in as he flies on.
            if (t < ClientFlight.ARISE) {
                float in = (float) ClientFlight.smooth((t - ClientFlight.SWEEP) / (ClientFlight.ARISE - ClientFlight.SWEEP));
                tilt *= in;
                roll *= in;
                fast *= in;
            }
        }
        Vec3 pivot = new Vec3(0.0, player.getBbHeight() * 0.55, 0.0);
        Quaternionf turn = new Quaternionf().rotateAxis(roll, (float) forward.x, 0.0F, (float) forward.z)
                .rotateAxis(tilt, (float) left.x, 0.0F, (float) left.z);
        frame = new Frame(player.getId(), t, fast, tilt, roll, land, ClientRing.has(player, RingPayload.DESCENT),
                slamming ? slam : -1.0F, player.tickCount + partialTick, blend, pivot, forward, left, turn);
        PoseStack pose = event.getPoseStack();
        float dip = dip(t, land);
        pushed = Math.abs(tilt) > 1.0E-3F || Math.abs(roll) > 1.0E-3F || dip > 1.0E-3F;
        if (pushed) {
            pose.pushPose();
            pose.translate(pivot.x, pivot.y - dip, pivot.z);
            pose.mulPose(turn);
            pose.translate(-pivot.x, -pivot.y, -pivot.z);
        }
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        model.leftArmPose = LanternPose.POSE.getValue();
        model.rightArmPose = LanternPose.POSE.getValue();
        if (flying) {
            model.crouching = false;
        }
        return true;
    }

    /** After the game drew the player: his body is turned back. */
    static void post(RenderPlayerEvent.Post event) {
        if (pushed && frame != null && frame.entity() == event.getEntity().getId()) {
            event.getPoseStack().popPose();
        }
        pushed = false;
        frame = null;
    }

    /** How far the body dips: a small crouch as the fists come to the chest, and as he lands. */
    private static float dip(float t, float land) {
        float gather = t < 0.0F ? 0.0F
                : (float) (ClientFlight.smooth(t / ClientFlight.GATHER)
                        * (1.0 - ClientFlight.smooth((t - ClientFlight.GATHER) / 3.0)));
        return 0.12F * gather + 0.14F * land;
    }

    /** True when this player's arms are posed here this frame. */
    static boolean posing(LivingEntity entity) {
        return frame != null && frame.entity() == entity.getId();
    }

    /**
     * One arm (and the head and legs along with it) while the game poses the model: the flight pose, and on top
     * of it whatever the ring makes him do with his hands.
     */
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
        // The head keeps looking where he looks, however the body lies.
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
        // What the ring does with his hands comes on top.
        if (right && blend.beam > 0.0F) {
            limb.xRot = Mth.lerp(blend.beam, limb.xRot, -Mth.HALF_PI + model.head.xRot);
            limb.yRot = Mth.lerp(blend.beam, limb.yRot, -0.08F + model.head.yRot);
            limb.zRot = Mth.lerp(blend.beam, limb.zRot, 0.0F);
        }
        if (!right && blend.ram > 0.0F) {
            // Superman's punch: the fist goes out in front along the way he flies, into the tip of the cone.
            limb.xRot = Mth.lerp(blend.ram, limb.xRot, -2.95F);
            limb.yRot = Mth.lerp(blend.ram, limb.yRot, 0.12F);
            limb.zRot = Mth.lerp(blend.ram, limb.zRot, -0.05F);
        }
        if (blend.dome > 0.0F) {
            // Both hands hold the dome open, palms out.
            limb.xRot = Mth.lerp(blend.dome, limb.xRot, -0.55F);
            limb.yRot = Mth.lerp(blend.dome, limb.yRot, 0.0F);
            limb.zRot = Mth.lerp(blend.dome, limb.zRot, side * 1.2F);
        }
        if (f.slam() >= 0.0F) {
            slam(model, limb, right, f.slam());
        }
    }

    /**
     * The landing slam, on top of the crouch the game already gives him: the ring fist comes up and smashes down
     * into the ground in front of him and stays there a moment, the other arm swings out and back, one knee comes
     * up and the other goes down; then he rises again.
     *
     * @param age ticks since he hit the ground
     */
    private static void slam(HumanoidModel<?> model, ModelPart limb, boolean right, float age) {
        float weight = (float) (ClientFlight.smooth(age / 1.5) * (1.0 - ClientFlight.smooth((age - 12.0) / 8.0)));
        if (right) {
            float strike = (float) ClientFlight.smooth(age / 3.0);
            limb.xRot = Mth.lerp(weight, limb.xRot, Mth.lerp(strike, -2.7F, -1.15F));
            limb.yRot = Mth.lerp(weight, limb.yRot, 0.1F);
            limb.zRot = Mth.lerp(weight, limb.zRot, 0.1F);
            // Once for both legs (this runs for each arm).
            model.rightLeg.xRot = Mth.lerp(weight, model.rightLeg.xRot, -1.1F);
            model.leftLeg.xRot = Mth.lerp(weight, model.leftLeg.xRot, 0.55F);
            model.rightLeg.zRot = Mth.lerp(weight, model.rightLeg.zRot, 0.05F);
            model.leftLeg.zRot = Mth.lerp(weight, model.leftLeg.zRot, -0.1F);
        } else {
            limb.xRot = Mth.lerp(weight, limb.xRot, 0.5F);
            limb.yRot = Mth.lerp(weight, limb.yRot, 0.0F);
            limb.zRot = Mth.lerp(weight, limb.zRot, -1.0F);
        }
    }

    /**
     * Where one arm goes in flight, as xRot, yRot and zRot: the take-off (fists to the chest, then swept down
     * along the sides), hovering (a little out, drifting) or flying fast (back along the sides), or up in the air
     * while an empty ring lets him sink.
     *
     * @param side 1 for the right arm, -1 for the left
     */
    private static float[] flightArm(Frame f, float side) {
        float time = f.time();
        if (f.sinking()) {
            // Arms up and wide, like someone hanging from a chute, with a slow sway. Raised, an arm swings out
            // the other way round than hanging down, hence the minus.
            float sway = 0.12F * Mth.sin(time * 0.2F + side);
            return new float[] { -2.95F + sway, 0.0F, -side * (0.6F + sway) };
        }
        float hoverX = -0.12F + 0.07F * Mth.sin(time * 0.08F + side);
        float hoverZ = side * (0.3F + 0.05F * Mth.sin(time * 0.06F));
        float x = Mth.lerp(f.fast(), hoverX, 0.1F);
        float z = Mth.lerp(f.fast(), hoverZ, side * 0.09F);
        float y = 0.0F;
        float t = f.t();
        if (t >= 0.0F && t < ClientFlight.ARISE + 6.0F) {
            // Fists to the chest ...
            float gather = (float) ClientFlight.smooth(t / ClientFlight.GATHER);
            // ... then down along the sides, a little back and out ...
            float sweep = (float) ClientFlight.smooth((t - ClientFlight.GATHER) / (ClientFlight.SWEEP - ClientFlight.GATHER));
            // ... and from there into the flight.
            float fly = (float) ClientFlight.smooth((t - ClientFlight.SWEEP) / (ClientFlight.ARISE + 6.0F - ClientFlight.SWEEP));
            float ax = Mth.lerp(sweep, Mth.lerp(gather, hoverX, -1.28F), 0.42F);
            float ay = Mth.lerp(sweep, gather * -side * 0.78F, 0.0F);
            float az = Mth.lerp(sweep, gather * side * 0.05F, side * 0.32F);
            x = Mth.lerp(fly, ax, x);
            y = Mth.lerp(fly, ay, y);
            z = Mth.lerp(fly, az, z);
        }
        return new float[] { x, y, z };
    }

    /** The legs: hanging loose and drifting while he hovers, straight together and fluttering when fast. */
    private static void legs(HumanoidModel<?> model, Frame f, float weight) {
        float time = f.time();
        float rightX;
        float leftX;
        float spread;
        if (f.sinking()) {
            rightX = 0.2F * Mth.sin(time * 0.2F);
            leftX = -rightX;
            spread = 0.12F;
        } else {
            float flutter = 0.05F * Mth.sin(time * 0.7F) * f.fast();
            rightX = Mth.lerp(f.fast(), -0.22F + 0.09F * Mth.sin(time * 0.07F), 0.1F + flutter);
            leftX = Mth.lerp(f.fast(), 0.14F + 0.09F * Mth.sin(time * 0.07F + 2.0F), 0.1F - flutter);
            spread = Mth.lerp(f.fast(), 0.08F, 0.025F);
            float t = f.t();
            if (t >= 0.0F && t < ClientFlight.ARISE + 6.0F) {
                // Together and pointed down as he shoots up.
                float rise = (float) (ClientFlight.smooth((t - ClientFlight.GATHER) / 4.0)
                        * (1.0 - ClientFlight.smooth((t - ClientFlight.ARISE) / 6.0)));
                rightX = Mth.lerp(rise, rightX, 0.12F);
                leftX = Mth.lerp(rise, leftX, 0.12F);
                spread = Mth.lerp(rise, spread, 0.02F);
            }
        }
        model.rightLeg.xRot = Mth.lerp(weight, model.rightLeg.xRot, rightX);
        model.leftLeg.xRot = Mth.lerp(weight, model.leftLeg.xRot, leftX);
        model.rightLeg.yRot = Mth.lerp(weight, model.rightLeg.yRot, 0.0F);
        model.leftLeg.yRot = Mth.lerp(weight, model.leftLeg.yRot, 0.0F);
        model.rightLeg.zRot = Mth.lerp(weight, model.rightLeg.zRot, spread);
        model.leftLeg.zRot = Mth.lerp(weight, model.leftLeg.zRot, -spread);
    }

    /** The head during the take-off: down while the fists come to the chest, then up as he rises. */
    private static float headLift(Frame f) {
        float t = f.t();
        if (t < 0.0F || t > ClientFlight.ARISE + 6.0F) {
            return 0.0F;
        }
        float gather = (float) (ClientFlight.smooth(t / ClientFlight.GATHER)
                * (1.0 - ClientFlight.smooth((t - ClientFlight.GATHER) / 3.0)));
        float up = (float) (ClientFlight.smooth((t - ClientFlight.GATHER) / 4.0)
                * (1.0 - ClientFlight.smooth((t - ClientFlight.ARISE) / 6.0)));
        return 0.3F * gather - 0.45F * up;
    }

    /**
     * A point on someone's body as it really is drawn this frame: turned with the flight pose. Used to aim an arm
     * at something (see {@link LanternArms}): the arm has to reach from where its shoulder really is.
     */
    static Vec3 turned(LivingEntity entity, Vec3 point, float partialTick) {
        Frame f = frame;
        if (f == null || f.entity() != entity.getId() || !pushed) {
            return point;
        }
        Vec3 base = entity.getPosition(partialTick).add(f.pivot());
        Vector3f local = new Vector3f((float) (point.x - base.x), (float) (point.y - base.y),
                (float) (point.z - base.z));
        f.turn().transform(local);
        return base.add(local.x, local.y, local.z);
    }

    /** A way in the world, as the upright body sees it: undoes the flight pose's turn. */
    static Vec3 untilted(LivingEntity entity, Vec3 way) {
        Frame f = frame;
        if (f == null || f.entity() != entity.getId() || !pushed) {
            return way;
        }
        Vector3f local = new Vector3f((float) way.x, (float) way.y, (float) way.z);
        new Quaternionf(f.turn()).conjugate().transform(local);
        return new Vec3(local.x, local.y, local.z);
    }

    /** Forgets everyone (you left the world). */
    static void clear() {
        BLENDS.clear();
        frame = null;
        pushed = false;
    }

    // ---- First person ----

    /**
     * Your own hands during the take-off: both fists come up to your chest at the bottom of your screen, then
     * sweep down and out of sight as you rise.
     */
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
        float t = ClientRing.flight(player, event.getPartialTick());
        if (t < 0.0F || t > ClientFlight.SWEEP + 2.0F) {
            return false;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return true;
        }
        float gather = (float) ClientFlight.smooth(t / ClientFlight.GATHER);
        float sweep = (float) ClientFlight.smooth((t - ClientFlight.GATHER) / (ClientFlight.SWEEP + 2.0F - ClientFlight.GATHER));
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

    /**
     * Your own landing slam: your ring fist comes up at the right of your screen and smashes down out of sight into
     * the ground, stays there a moment, and comes back.
     */
    private static void slamHand(RenderHandEvent event, LocalPlayer player, float age) {
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        float down = (float) ClientFlight.smooth(age / 2.5);
        float back = (float) ClientFlight.smooth((age - 9.0) / 5.0);
        Vector3f raised = new Vector3f(0.5F, 0.1F, -0.75F);
        Vector3f ground = new Vector3f(0.3F, -1.2F, -0.9F);
        Vector3f hand = new Vector3f(raised).lerp(ground, down).lerp(new Vector3f(RechargeAnimation.HAND_RIGHT), back);
        RechargeAnimation.arm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player,
                renderer, 1.0F, hand, new Vector3f(1.5F, -0.4F, 0.35F));
    }
}
