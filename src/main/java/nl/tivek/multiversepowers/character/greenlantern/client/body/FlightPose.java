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
 * <li>An empty ring lets him sink with his arms up; landing is a short dip, and a landing at full speed a slam, the
 * way heroes land: just before the ground he swings upright, feet first, ring fist cocked high; then he comes down
 * low on one knee, the other leg forward, and smashes that fist into the ground in front of him, the other arm flung
 * out behind, until the construct has struck and he rises again. Dropping down to a slam without flying (the
 * shockwave key while he jumps or falls) he is upright with his fist cocked the whole way down.</li>
 * <li>On top of that, standing or flying: the ring arm points straight where he aims while he shoots bolts, and goes
 * down again after the last one (see {@link BoltArm}); the ring hand points along the beam, trembling and kicking
 * with it, the other hand bracing its wrist (see {@link BeamArm}); both hands hold the dome open,
 * and in flight the shield hand goes out in front, fist first, into the ram cone.</li>
 * </ul>
 * How the whole body turns is worked out in {@link #pre}, before the game draws it, and turned while the game draws
 * its model ({@link #turnModel}), so his name stays upright; the limbs are set in {@link #pose}, which the game calls
 * while it poses the arms.
 */
public final class FlightPose {
    // Part of the top speed at which the body lies fully along the way it flies.
    private static final double LINED_UP = 0.74;
    // How long a landing dip lasts, in ticks.
    private static final int LAND_TICKS = 8;
    // How quickly the arms blend from one thing to the next, per second.
    private static final float BLEND = 9.0F;
    // How far the ring arm comes up towards where he aims while the ring gathers light for the beam, once it is full.
    private static final float CHARGE_AIM = 0.8F;
    // The ring arm with the bolts (see BoltArm): how quickly it comes up to point and goes down again, per second
    // (quickly up, so a bolt leaves a hand that is already up), how far each bolt kicks it up, in radians, and how far
    // it turns in towards the middle of his chest, so it points at what he aims at.
    private static final float POINT_UP = 30.0F;
    private static final float POINT_DOWN = 7.0F;
    private static final float BOLT_KICK = 0.14F;
    private static final float POINT_IN = 0.08F;
    // The ring arm with the beam (see BeamArm), in radians: how far it trembles at its hardest, how far the beam
    // breaking loose kicks it up, and how far it swings in towards the middle of his chest once the other hand braces
    // it. That hand then comes across (turned in towards the ring arm) and a little lower, under its wrist.
    private static final float BEAM_TREMBLE = 0.05F;
    private static final float BEAM_KICK = 0.42F;
    private static final float BRACE_IN = 0.2F;
    private static final float BRACE_ACROSS = 0.72F;
    private static final float BRACE_DROP = 0.14F;
    // How long the pose of a landing slam lasts, in ticks; your own fist shows in first person this long.
    private static final float SLAM_TICKS = 23.0F;
    private static final float SLAM_HAND_TICKS = 21.0F;
    // The tick of a slam he starts to rise again from his knee, and how long that takes.
    private static final float KNEEL_HOLD = 15.0F;
    private static final float KNEEL_RISE = 7.0F;
    // The landing on one knee: how far his hips come down, in blocks (from 0.7 to under 0.4, so the right knee is on
    // the ground), and how far his body leans forward over them, in radians.
    private static final float KNEEL_DROP = 0.32F;
    private static final float KNEEL_LEAN = 1.0F;
    // What the crouch he is in already lowers his body by, in blocks: the rest of the drop is ours.
    private static final float CROUCH_DROP = 0.125F;
    // Where his limbs point in the world while he kneels, in radians from straight down, positive swinging back: the
    // right thigh straight down to the knee on the ground, the left thigh out in front almost flat, the ring arm
    // straight down into the ground, the other arm flung back and up.
    private static final float DOWN_THIGH = 0.05F;
    private static final float STEP_THIGH = -1.4F;
    private static final float FIST_ARM = -0.1F;
    private static final float BACK_ARM = 2.15F;
    // How far each knee bends: the right shin lies back along the ground, the left one stands straight down to the
    // foot planted in front of him.
    private static final float DOWN_KNEE = 1.52F;
    private static final float STEP_KNEE = 1.4F;
    // How high his hips are over his feet, in blocks: 12 pixels of the model, which the game draws a little smaller
    // for players.
    private static final float HIP_HEIGHT = 12.0F * 0.9375F / 16.0F;
    // Your own ring fist in first person, in blocks in front of your eyes: cocked high on the right before a slam, and
    // smashed into the ground in front of you (low on your screen, while your view dips down to it).
    private static final Vector3f COCKED = new Vector3f(0.55F, 0.42F, -0.72F);
    private static final Vector3f PLANTED = new Vector3f(0.18F, -0.95F, -0.9F);
    private static final Vector3f ARM_FROM = new Vector3f(1.5F, -0.4F, 0.35F);

    private static final Map<Integer, Blend> BLENDS = new HashMap<>();
    // The player being drawn right now, and how his body turns while the game draws its model (null: it does not).
    @Nullable
    private static Frame frame;
    @Nullable
    private static BodyTurn body;

    private FlightPose() {
    }

    /** How far each of the arm shapes has come in for one player, eased from frame to frame. */
    private static final class Blend {
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

    /**
     * Everything one frame of one player needs.
     *
     * @param t      ticks since he took off, or -1 when he does not fly
     * @param fast   0 hovering, 1 flying fully lined up
     * @param tilt   how far the body tips forward, in radians (a quarter turn lies flat)
     * @param roll   how far it leans sideways, in radians: positive to his right
     * @param land   0 to 1: the dip of a landing
     * @param slam   ticks since he landed with a slam, or -1
     * @param brace  0 to 1: how far he has swung upright for a slam just ahead
     * @param time   ticks, for everything that sways
     */
    private record Frame(int entity, float t, float fast, float tilt, float roll, float land, boolean sinking,
            float slam, float brace, float time, Blend blend, Vec3 pivot, Vec3 forward, Vec3 left, Quaternionf turn,
            float kneel) {
    }

    /**
     * How one player's whole body turns while its model is drawn (see {@link #turnModel}).
     *
     * @param dip   how far it dips, in blocks
     * @param drop  how far his hips come down in a kneel, in blocks, and {@code lean} how far his body then leans
     *              forward over them, in radians
     */
    private record BodyTurn(int entity, Vec3 pivot, float dip, Quaternionf turn, boolean kneel, float drop,
            float lean, Vec3 left) {
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
        // Gathering light for the beam, the ring arm already comes up towards where he aims.
        float charge = BeamArm.gathering(player, partialTick);
        float aim = ClientRing.has(player, RingPayload.BEAM) ? 1.0F : CHARGE_AIM * Math.max(0.0F, charge);
        boolean beaming = aim > 0.0F;
        // Shooting bolts, the ring arm points where he aims.
        boolean pointing = BoltArm.pointing(player, partialTick);
        boolean domed = ClientRing.has(player, RingPayload.DOME);
        boolean ramming = flying && ClientRing.has(player, RingPayload.SHIELD);
        float land = motion == null || motion.sinceEnd >= LAND_TICKS ? 0.0F
                : Mth.sin((motion.sinceEnd + partialTick) / LAND_TICKS * Mth.PI);
        float slam = ClientFlight.slam(player, partialTick);
        boolean slamming = slam >= 0.0F && slam < SLAM_TICKS;
        // Dropping down to a slam he is upright, fist cocked, the whole way down.
        boolean dropping = ClientFlight.dropping(player);
        float brace = flying || dropping ? ClientFlight.brace(player, partialTick) : 0.0F;
        if (slamming) {
            // A slam has a landing of its own.
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
        // Only forgotten once nothing is wanted any more and everything has blended back out.
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
            // Hovering he only leans a little into where he drifts; fast, his body lies along the way he flies.
            float drift = (float) Mth.clamp(ahead * 1.1, -0.3, 0.45);
            float along = (float) Math.atan2(Math.max(ahead, 0.0), v.y);
            tilt = Mth.lerp(fast, drift, along);
            roll = motion.bank + (float) Mth.clamp(side * 0.8, -0.4, 0.4) * (1.0F - 0.5F * fast);
            // The take-off itself stays upright; the lean fades in as he flies on.
            if (t < ClientFlight.ARISE) {
                float in = (float) Ease.smooth((t - ClientFlight.SWEEP) / (ClientFlight.ARISE - ClientFlight.SWEEP));
                tilt *= in;
                roll *= in;
                fast *= in;
            }
            // Swinging upright for a slam into the ground just ahead: he comes down feet first.
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
        // The body is turned while the game draws the model (see turnModel), so his name over his head stays upright.
        body = Math.abs(tilt) > 1.0E-3F || Math.abs(roll) > 1.0E-3F || dip > 1.0E-3F || kneel > 0.0F
                ? new BodyTurn(player.getId(), pivot, dip, turn, kneel > 0.0F, drop, lean, left) : null;
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        model.leftArmPose = LanternPose.POSE.getValue();
        model.rightArmPose = LanternPose.POSE.getValue();
        if (flying || kneel > 0.0F) {
            model.crouching = false;
        }
        if (kneel > 0.0F) {
            // His legs bend at the knee now, which the game's legs cannot: they are drawn in two halves instead (see
            // KneelLegs). Armour copies the legs as they are, so it shrinks to the upper half with them.
            model.rightLeg.visible = false;
            model.leftLeg.visible = false;
            model.rightPants.visible = false;
            model.leftPants.visible = false;
            model.rightLeg.yScale = 0.5F;
            model.leftLeg.yScale = 0.5F;
        }
        return true;
    }

    /**
     * While the game draws the model of the player being drawn (see PlayerRendererMixin): his whole body turns with his
     * flight, dips, and kneels with his body leaning over his hips. Only the model: his name over his head, drawn after
     * it, stays upright.
     */
    static void turnModel(AbstractClientPlayer player, PoseStack pose) {
        BodyTurn turn = body;
        if (turn == null || turn.entity() != player.getId()) {
            return;
        }
        pose.translate(turn.pivot().x, turn.pivot().y - turn.dip(), turn.pivot().z);
        pose.mulPose(turn.turn());
        pose.translate(-turn.pivot().x, -turn.pivot().y, -turn.pivot().z);
        if (turn.kneel()) {
            // Down onto his knee, and his body leaning forward over his hips.
            pose.translate(0.0F, HIP_HEIGHT - turn.drop(), 0.0F);
            pose.mulPose(new Quaternionf().rotateAxis(turn.lean(), (float) turn.left().x, 0.0F,
                    (float) turn.left().z));
            pose.translate(0.0F, -HIP_HEIGHT, 0.0F);
        }
    }

    /** After the game drew the player: legs made short for a kneel are whole again. */
    static void post(RenderPlayerEvent.Post event) {
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        model.rightLeg.yScale = 1.0F;
        model.leftLeg.yScale = 1.0F;
        body = null;
        frame = null;
    }

    /**
     * How far this player's knees bend right now, right and left, in radians, while he kneels in the landing of a slam;
     * null while he does not, and his legs are the game's own straight ones.
     */
    @Nullable
    static float[] knees(LivingEntity entity) {
        Frame f = frame;
        if (f == null || f.entity() != entity.getId() || f.kneel() <= 0.0F) {
            return null;
        }
        return new float[] { DOWN_KNEE * f.kneel(), STEP_KNEE * f.kneel() };
    }

    /** How far the body dips: a small crouch as the fists come to the chest, and as he lands. */
    private static float dip(float t, float land) {
        float gather = t < 0.0F ? 0.0F
                : (float) (Ease.smooth(t / ClientFlight.GATHER)
                        * (1.0 - Ease.smooth((t - ClientFlight.GATHER) / 3.0)));
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
        // What the ring does with his hands comes on top. Shooting bolts, the ring arm points straight where he aims,
        // so every bolt leaves the ring on his outstretched hand, and kicks up a little with each one.
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
        if (f.brace() > 0.0F) {
            brace(model, limb, right, f.brace());
        }
        if (f.slam() >= 0.0F) {
            slam(model, limb, right, f.slam(), f.kneel());
        }
    }

    /**
     * The beam (see {@link BeamArm}): the ring arm points where he looks, trembling and kicking with the light; once
     * the other hand braces it, the ring arm swings in a little towards the middle of his chest and the other arm
     * comes across under it, its hand at the wrist, trembling and kicking along.
     */
    private static void beam(HumanoidModel<?> model, ModelPart limb, boolean right, Blend blend, LivingEntity entity,
            float partialTick, float time) {
        float tremble = BEAM_TREMBLE * BeamArm.tremble(entity, partialTick);
        float lift = model.head.xRot - BEAM_KICK * BeamArm.kick(entity, partialTick)
                + tremble * BeamArm.shake(time, 0);
        float turn = model.head.yRot + tremble * BeamArm.shake(time, 1);
        if (right) {
            limb.xRot = Mth.lerp(blend.beam, limb.xRot, -Mth.HALF_PI + lift);
            limb.yRot = Mth.lerp(blend.beam, limb.yRot, -0.08F - BRACE_IN * blend.brace + turn);
            limb.zRot = Mth.lerp(blend.beam, limb.zRot, 0.0F);
        } else if (blend.brace > 0.0F) {
            limb.xRot = Mth.lerp(blend.brace, limb.xRot, -Mth.HALF_PI + BRACE_DROP + lift);
            limb.yRot = Mth.lerp(blend.brace, limb.yRot, BRACE_ACROSS + turn);
            limb.zRot = Mth.lerp(blend.brace, limb.zRot, 0.0F);
        }
    }

    /**
     * Swinging upright for a slam into the ground just ahead: the ring fist cocked high over his head, the other arm
     * out for balance, one knee drawn up and the other leg back, looking down at where he will hit.
     */
    private static void brace(HumanoidModel<?> model, ModelPart limb, boolean right, float weight) {
        if (right) {
            // Raised, an arm swings out the other way round than hanging down, hence the minus.
            limb.xRot = Mth.lerp(weight, limb.xRot, -2.9F);
            limb.yRot = Mth.lerp(weight, limb.yRot, 0.0F);
            limb.zRot = Mth.lerp(weight, limb.zRot, -0.12F);
            // Once for both legs and the head (this runs for each arm).
            model.rightLeg.xRot = Mth.lerp(weight, model.rightLeg.xRot, 0.55F);
            model.leftLeg.xRot = Mth.lerp(weight, model.leftLeg.xRot, -0.95F);
            model.head.xRot = Mth.clamp(model.head.xRot + 0.4F * weight, -1.35F, 1.1F);
        } else {
            limb.xRot = Mth.lerp(weight, limb.xRot, -0.3F);
            limb.yRot = Mth.lerp(weight, limb.yRot, 0.0F);
            limb.zRot = Mth.lerp(weight, limb.zRot, -1.25F);
        }
    }

    /**
     * The landing slam, the way heroes land: his body drops and leans forward over his hips (see {@link #pre}), his
     * right knee comes down on the ground with the shin lying back along it, his left foot is planted out in front,
     * the ring fist comes down out of its cocked position straight into the ground before him, and the other arm is
     * flung back and up. He stays down there while the construct strikes; then he rises again. The legs bend at the
     * knee, which the game's own legs cannot: their thighs are posed here and their shins hung on them in
     * {@link KneelLegs}.
     *
     * <p>Every angle is set where the limb has to point in the world, less the lean of his body, since the limbs lean
     * along with it.
     *
     * @param age   ticks since he hit the ground
     * @param kneel 0 to 1: how far down he is
     */
    private static void slam(HumanoidModel<?> model, ModelPart limb, boolean right, float age, float kneel) {
        float lean = KNEEL_LEAN * kneel;
        if (right) {
            // The fist: from cocked high over his head, straight down into the ground.
            float strike = (float) Ease.smooth(age / 1.4);
            float shudder = age < 5.0F ? 0.05F * (1.0F - age / 5.0F) * Mth.sin(age * 8.0F) : 0.0F;
            limb.xRot = Mth.lerp(kneel, limb.xRot, Mth.lerp(strike, -2.9F, FIST_ARM - lean) + shudder);
            limb.yRot = Mth.lerp(kneel, limb.yRot, 0.0F);
            limb.zRot = Mth.lerp(kneel, limb.zRot, Mth.lerp(strike, -0.12F, 0.1F));
            // Once for both legs and the head (this runs for each arm).
            model.rightLeg.xRot = Mth.lerp(kneel, model.rightLeg.xRot, DOWN_THIGH - lean);
            model.rightLeg.yRot = Mth.lerp(kneel, model.rightLeg.yRot, 0.0F);
            model.rightLeg.zRot = Mth.lerp(kneel, model.rightLeg.zRot, 0.04F);
            model.leftLeg.xRot = Mth.lerp(kneel, model.leftLeg.xRot, STEP_THIGH - lean);
            model.leftLeg.yRot = Mth.lerp(kneel, model.leftLeg.yRot, -0.12F);
            model.leftLeg.zRot = Mth.lerp(kneel, model.leftLeg.zRot, -0.1F);
            // Looking ahead, a little down, over the fist in the ground.
            model.head.xRot = Mth.lerp(kneel, model.head.xRot, Mth.clamp(0.25F - lean, -1.35F, 1.1F));
            model.head.yRot = Mth.lerp(kneel, model.head.yRot, 0.0F);
        } else {
            // Flung back and up as the fist comes down, and out to the side for balance.
            float fling = (float) Ease.smooth(age / 2.0);
            limb.xRot = Mth.lerp(kneel, limb.xRot, Mth.lerp(fling, 0.4F, BACK_ARM) - lean);
            limb.yRot = Mth.lerp(kneel, limb.yRot, 0.0F);
            limb.zRot = Mth.lerp(kneel, limb.zRot, -0.45F);
        }
    }

    /** 0 to 1: how far down he is in the landing of a slam: straight down on impact, a hold, and up again. */
    private static float kneel(float age) {
        return (float) (Ease.smooth(age / 1.0) * (1.0 - Ease.smooth((age - KNEEL_HOLD) / KNEEL_RISE)));
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
            float gather = (float) Ease.smooth(t / ClientFlight.GATHER);
            // ... then down along the sides, a little back and out ...
            float sweep = (float) Ease.smooth((t - ClientFlight.GATHER) / (ClientFlight.SWEEP - ClientFlight.GATHER));
            // ... and from there into the flight.
            float fly = (float) Ease.smooth(
                    (t - ClientFlight.SWEEP) / (ClientFlight.ARISE + 6.0F - ClientFlight.SWEEP));
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
                float rise = (float) (Ease.smooth((t - ClientFlight.GATHER) / 4.0)
                        * (1.0 - Ease.smooth((t - ClientFlight.ARISE) / 6.0)));
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
        float gather = (float) (Ease.smooth(t / ClientFlight.GATHER)
                * (1.0 - Ease.smooth((t - ClientFlight.GATHER) / 3.0)));
        float up = (float) (Ease.smooth((t - ClientFlight.GATHER) / 4.0)
                * (1.0 - Ease.smooth((t - ClientFlight.ARISE) / 6.0)));
        return 0.3F * gather - 0.45F * up;
    }

    /**
     * A point on someone's body as it really is drawn this frame: turned with the flight pose. Used to aim an arm
     * at something (see {@link LanternArms}): the arm has to reach from where its shoulder really is.
     */
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

    /** A way in the world, as the upright body sees it: undoes the flight pose's turn. */
    static Vec3 untilted(LivingEntity entity, Vec3 way) {
        Frame f = frame;
        if (f == null || f.entity() != entity.getId() || body == null) {
            return way;
        }
        Vector3f local = new Vector3f((float) way.x, (float) way.y, (float) way.z);
        new Quaternionf(f.turn()).conjugate().transform(local);
        return new Vec3(local.x, local.y, local.z);
    }

    /**
     * How the flight pose turns this player's body, in the model's own directions (x to the left, y down, -z
     * ahead), or null when it is not turned. Undoing it keeps something he holds upright in the world.
     */
    @Nullable
    static Quaternionf bodyTurn(LivingEntity entity) {
        Frame f = frame;
        if (f == null || f.entity() != entity.getId() || body == null) {
            return null;
        }
        return new Quaternionf().rotateAxis(f.roll(), 0.0F, 0.0F, -1.0F).rotateAxis(f.tilt(), 1.0F, 0.0F, 0.0F);
    }

    /** Forgets everyone (you left the world). */
    public static void clear() {
        BLENDS.clear();
        frame = null;
        body = null;
    }

    // ---- First person ----

    /**
     * Your own hands in first person: during the take-off both fists come up to your chest at the bottom of your
     * screen, then sweep down and out of sight as you rise; diving into a slam your ring fist comes up cocked high on
     * the right, and on the landing it smashes into the ground in front of you (see {@link #slamHand}).
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

    /**
     * Your own landing slam: your ring fist comes down out of its cocked position on the right and smashes into the
     * ground in front of you, right where the cracks run out from (whichever way you look, while your view dips down
     * to it); it shudders from the blow, stays there while the construct strikes, and comes back.
     */
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

    /**
     * Where your fist is in the ground in first person, in blocks in front of your eyes: the spot beside you where the
     * slam's cracks run out from, seen from your camera as it is turned right now. It is kept on your screen: looking
     * too far up, it shows at the bottom edge instead, and behind your eyes it stays low on your screen.
     */
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
