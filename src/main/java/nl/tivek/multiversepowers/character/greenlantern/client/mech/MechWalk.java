package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Spring;

// A built mech's walk, worked out on every client from where it stands tick by tick: its feet go as MechGait puts
// them, and the body rides on them, bobbing, swaying and twisting above the legs. Walking it is heavy and slow: hunched
// forward and low, its whole weight rolling over onto the planted leg and sinking into every footfall. Running it
// lumbers, leaning into its long strides, rising onto each and dropping onto every footfall with a crash that shakes
// the ground. It stays level with the ground its feet stand on whatever is under its middle, and comes down so no
// planted foot is ever out of its leg's reach.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class MechWalk extends MechGait {
    private static final double CROUCH = 0.6;
    private static final double BOB = 0.06;
    private static final double SWAY = 0.26;
    private static final double ROLL = 0.06;
    private static final double TWIST = 0.1;
    private static final double LEAN = 0.09;
    private static final double BRACE = 2.5;
    private static final double LEVER_PACE = 0.25;
    // The torso follows where its pilot looks like a heavy weight on a spring: it swings round, carries on a touch past
    // and settles, and never twists further than MOST_TWIST over the hips. As the legs turn under it they drag it a
    // little along, and the hips give a little way to the twist above them.
    private static final double TORSO_FREQ = 0.1;
    private static final double TORSO_DAMP = 0.7;
    private static final double MOST_TWIST = 1.3;
    private static final double DRAG = 0.2;
    private static final double HIPS_SHARE = 0.14;
    private static final double HIPS_KICK = 0.9;
    // The head turns the rest of the way to the look, and tilts with it, quicker than the torso.
    private static final double HEAD_FREQ = 0.2;
    private static final double MOST_HEAD_YAW = 1.0;
    private static final double MOST_HEAD_UP = 0.55;
    private static final double MOST_HEAD_DOWN = 0.7;
    // Running, the body is lower and leans into its strides, twists a little with them and the arms swing wider.
    private static final double RUN_LEAN = 0.12;
    private static final double RUN_TORSO_LEAN = 0.07;
    private static final double RUN_BOB = 0.16;
    private static final double RUN_BOB_AT = 0.325;
    private static final double RUN_TWIST = 0.09;
    private static final double RUN_PUMP = 0.35;
    // The hips come down so no planted foot, nor one stepping down more than STEPPING_DOWN, is further off than its leg
    // reaches, at most SQUAT_MOST, and rise back SQUAT_BACK a tick; the body follows the ground its feet stand on, at
    // most SETTLE_MOST from its base.
    private static final double SQUAT_MOST = 2.4;
    private static final double SQUAT_BACK = 0.08;
    private static final double SQUAT_FLOOR = 0.6;
    private static final double STEPPING_DOWN = 0.5;
    private static final double SETTLE_MOST = 2.8;
    private static final double SETTLE_PACE = 0.3;
    private static final int IDLE_BEFORE = 20;
    private static final int PRESS_EVERY = 46;
    private static final int PRESS_TICKS = 18;
    private static final int STEP_SHAKE_TICKS = 7;
    private static final double STEP_SHAKE_RANGE = 36.0;
    private static final int KEEP_BROKEN = 60;
    private static final Vec3 HIPS = new Vec3(0.0, MechScript.HIP.y, 0.0);
    private static final Map<Integer, MechWalk> WALKS = new HashMap<>();
    private static final Map<Integer, Kept> BROKEN = new HashMap<>();

    private record Kept(MechPose pose, int since) {
    }

    private final int seed;
    private MechScript.Stage last;
    private int ticked = Integer.MIN_VALUE;
    private int pilot = -1;
    private double torsoTurn;
    private final Spring torso = new Spring();
    private final Spring headYaw = new Spring();
    private final Spring headPitch = new Spring();
    private final Spring bank = new Spring();
    private final Spring lean = new Spring();
    private double legsWas = Double.NaN;
    private double legsRate;
    private double brace;
    private double settleY;
    private double squat;
    private int idle;

    private MechWalk(int seed, MechScript.Stage stage) {
        this.seed = seed;
        this.reset(stage);
    }

    private void reset(MechScript.Stage stage) {
        this.last = stage;
        this.climbing = null;
        this.falling = false;
        this.squat = 0.0;
        for (int side = 0; side < 2; side++) {
            Leg leg = this.legs[side];
            leg.planted = home(stage, side);
            leg.toes = stage.ahead();
            leg.swinging = false;
        }
        this.now = this.pose(stage, 0.0);
        this.was = this.now;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        ticks++;
        Iterator<Map.Entry<Integer, Kept>> kept = BROKEN.entrySet().iterator();
        while (kept.hasNext()) {
            if (ticks - kept.next().getValue().since() > KEEP_BROKEN) {
                kept.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WALKS.clear();
        BROKEN.clear();
    }

    // Moves a mech's walk on to where it stands this tick; a second call in the same tick does nothing.
    // `look` and `pitch`: where its pilot looks, in the game's degrees (NaN when not known); `blow` the one it strikes,
    // `climb` how far it has got climbing a ledge, packed (0 when it does not climb).
    public static void step(int id, MechScript.Stage stage, int pilot, float look, float pitch,
            MechAttacks.Blow blow, int climb) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        MechWalk walk = WALKS.computeIfAbsent(id, key -> new MechWalk(key, stage));
        if (walk.ticked == ticks) {
            return;
        }
        walk.ticked = ticks;
        walk.pilot = pilot;
        walk.strike(blow, stage);
        // Climbing, the torso turns to face the wall whatever its pilot looks at.
        walk.face(stage, climb != 0 ? stage.yaw() : look, pitch);
        walk.tick(level, stage, climb);
    }

    // What of the blow landed since the last tick shakes the ground and jolts the body.
    private void strike(MechAttacks.Blow blow, MechScript.Stage stage) {
        MechAttacks.Blow was = this.blow;
        this.blow = blow;
        if (!blow.striking()) {
            return;
        }
        int from = was.kind() == blow.kind() && was.age() <= blow.age() ? (int) was.age() + 1 : (int) blow.age();
        for (int age = from; age <= (int) blow.age(); age++) {
            double hard = MechAttacks.impact(blow.kind(), age);
            if (hard > 0.0) {
                this.hitAt = ticks;
                this.hitHard = hard;
                this.hitSpot = blow.kind() == MechAttacks.STOMP ? this.legs[0].planted
                        : stage.point(0.0, 0.0, MechScript.TARGET_AHEAD);
                this.sink.kick(-0.1 * hard);
            }
        }
    }

    @Nullable
    public static MechPose pose(int id, float partialTick) {
        MechWalk walk = WALKS.get(id);
        if (walk == null) {
            Kept kept = BROKEN.get(id);
            return kept == null ? null : kept.pose();
        }
        // A walk not yet moved on this tick shows where it got to last.
        double u = walk.ticked == ticks ? partialTick : 1.0;
        return MechPose.between(walk.was, walk.now, u);
    }

    // A yaw in the game's degrees as the turn MechScript counts (see turnTo).
    private static double turnOf(float yaw) {
        return -Math.toRadians(yaw);
    }

    // The torso swings after the look on its spring, dragged along by the legs as they turn and held within its twist.
    private void face(MechScript.Stage stage, float look, float pitch) {
        double legs = turnOf(stage.yaw());
        if (Double.isNaN(this.legsWas)) {
            this.legsWas = legs;
            this.torso.set(legs + this.torsoTurn);
        }
        this.legsRate = Math.IEEEremainder(legs - this.legsWas, Math.PI * 2.0);
        this.legsWas = legs;
        if (!Float.isNaN(look)) {
            double want = this.torso.value + Math.IEEEremainder(turnOf(look) - this.torso.value, Math.PI * 2.0);
            this.torso.step(want, 1.0, TORSO_FREQ, TORSO_DAMP);
        }
        this.torso.kick(DRAG * this.legsRate);
        double twist = Math.IEEEremainder(this.torso.value - legs, Math.PI * 2.0);
        if (Math.abs(twist) > MOST_TWIST) {
            double over = twist - Math.copySign(MOST_TWIST, twist);
            this.torso.value -= over;
            this.torso.speed *= 0.5;
            twist -= over;
        }
        this.torsoTurn = twist;
        double headWant = Float.isNaN(look) ? 0.0
                : Mth.clamp(Math.IEEEremainder(turnOf(look) - this.torso.value, Math.PI * 2.0), -MOST_HEAD_YAW,
                        MOST_HEAD_YAW);
        double tilt = Float.isNaN(pitch) ? 0.0
                : Mth.clamp(Math.toRadians(pitch), -MOST_HEAD_UP, MOST_HEAD_DOWN);
        this.headYaw.step(headWant, 1.0, HEAD_FREQ, 0.8);
        this.headPitch.step(tilt, 1.0, HEAD_FREQ, 0.85);
    }

    @Nullable
    public static MechPose latest(int id) {
        MechWalk walk = WALKS.get(id);
        return walk == null ? null : walk.now;
    }

    // The mech breaks up: its last pose stays a while for its pieces to fly from.
    public static void stop(int id) {
        MechWalk walk = WALKS.remove(id);
        if (walk != null) {
            BROKEN.put(id, new Kept(walk.now, ticks));
        }
    }

    // How hard the last footfall of a mech shakes the view at from.
    static double shake(int id, Vec3 from, float partialTick, boolean inside) {
        MechWalk walk = WALKS.get(id);
        if (walk == null) {
            return 0.0;
        }
        double most = 0.0;
        double sinceHit = ticks - walk.hitAt + partialTick - 1.0;
        if (sinceHit >= 0.0 && sinceHit < STEP_SHAKE_TICKS * 1.5) {
            double near = inside ? 1.0 : 1.0 - from.distanceTo(walk.hitSpot) / STEP_SHAKE_RANGE;
            double fade = 1.0 - sinceHit / (STEP_SHAKE_TICKS * 1.5);
            most = Math.max(0.0, (inside ? 0.26 : 0.45) * walk.hitHard * fade * fade * Math.min(1.0, near * 1.3));
        }
        for (int side = 0; side < 2; side++) {
            double since = ticks - walk.legs[side].landed + partialTick - 1.0;
            if (since < 0.0 || since >= STEP_SHAKE_TICKS) {
                continue;
            }
            double near = inside ? 1.0 : 1.0 - from.distanceTo(walk.legs[side].planted) / STEP_SHAKE_RANGE;
            if (near <= 0.0) {
                continue;
            }
            double fade = 1.0 - since / STEP_SHAKE_TICKS;
            most = Math.max(most, (inside ? 0.26 : 0.45) * walk.legs[side].hard * fade * fade
                    * Math.min(1.0, near * 1.3));
        }
        return most;
    }

    private void tick(ClientLevel level, MechScript.Stage stage, int climb) {
        Vec3 moved = stage.base().subtract(this.last.base());
        if (moved.lengthSqr() > LOST * LOST) {
            this.reset(stage);
            return;
        }
        this.was = this.now;
        if (climb != 0) {
            this.climb(level, stage, climb);
            this.brace = 0.0;
            this.last = stage;
            this.sink.step(0.0, 1.0, SINK_FREQ, SINK_DAMP);
            this.now = this.pose(stage, 0.0);
            return;
        }
        // Over the top, the walk goes on from where the climb left the feet.
        this.climbing = null;
        double forward = moved.dot(stage.ahead());
        double aside = moved.dot(stage.right());
        Vec3 across = this.last.ahead().cross(stage.ahead());
        double angle = Math.atan2(across.y, this.last.ahead().dot(stage.ahead()));
        double before = this.speed;
        this.speed = Mth.lerp(0.4, this.speed, forward);
        this.side = Mth.lerp(0.4, this.side, aside);
        this.turn = Mth.lerp(0.4, this.turn, angle);
        this.brace = Mth.lerp(0.25, this.brace, this.speed - before);
        this.last = stage;
        if (this.fall(level, stage, -moved.y)) {
            this.sink.step(0.0, 1.0, SINK_FREQ, SINK_DAMP);
            this.now = this.pose(stage, 0.0);
            return;
        }
        double pace = Math.sqrt(forward * forward + aside * aside);
        double effort = pace + TURN_ARC * Math.abs(angle);
        boolean moving = effort > 2.0E-3;
        boolean busy = this.legs[0].swinging || this.legs[1].swinging || this.away(stage, 0) || this.away(stage, 1);
        double runs = Mth.clamp((pace - MechDrive.WALK) / (MechDrive.RUN - MechDrive.WALK), 0.0, 1.0);
        this.running += Mth.clamp(runs - this.running, -0.05, 0.06);
        double rate = moving ? Math.max(effort / this.stride(), SLOWEST) : busy ? SETTLE : 0.0;
        double wanted = moving ? Math.min(1.0, effort / (MechDrive.WALK * 0.85)) : rate > 0.0 ? 0.3 : 0.0;
        if (moving && this.walking < SETTING_OFF && !this.legs[0].swinging && !this.legs[1].swinging) {
            this.lead(stage, moved);
        }
        this.walking += Mth.clamp(wanted - this.walking, -0.06, 0.08);
        this.crouch += Mth.clamp((moving || rate > 0.0 ? 1.0 : 0.0) - this.crouch, -0.04, 0.07);
        double from = this.phase;
        this.phase += rate;
        for (int side = 0; side < 2; side++) {
            this.leg(level, stage, side, from, rate);
        }
        this.sink.step(0.0, 1.0, SINK_FREQ, SINK_DAMP);
        this.now = this.pose(stage, rate);
    }

    private MechPose pose(MechScript.Stage stage, double rate) {
        MechPose pose = new MechPose(stage);
        double w = this.walking;
        double p = this.phase;
        double feet = 0.0;
        for (int side = 0; side < 2; side++) {
            Leg leg = this.legs[side];
            double u = this.swung(leg);
            pose.ankle[side] = leg.planted;
            pose.toes[side] = leg.toes;
            pose.tip[side] = leg.swinging ? tip(u) : 0.0;
            feet += (leg.swinging ? Mth.lerp(Ease.smooth(u), leg.from.y, leg.aim) : leg.planted.y)
                    - MechScript.ANKLE.y;
        }
        // Walking, the body rides on its feet, level with them whatever the ground under its middle; falling or
        // climbing it goes where its base goes.
        boolean carried = !this.falling && this.climbing == null;
        double settle = carried ? Mth.clamp(feet * 0.5 - stage.base().y, -SETTLE_MOST, SETTLE_MOST) : 0.0;
        this.settleY = Mth.lerp(SETTLE_PACE, this.settleY, settle);
        double run = this.running;
        // Running it rises onto each stride and drops onto every footfall.
        double bob = BOB * w * Math.cos(4.0 * Math.PI * (p - 0.2))
                + RUN_BOB * run * Math.sin(4.0 * Math.PI * (p - RUN_BOB_AT));
        double sway = SWAY * w * (1.0 - 0.4 * run) * Math.sin(2.0 * Math.PI * (p - 0.46));
        double roll = ROLL * w * (1.0 - 0.3 * run) * Math.sin(2.0 * Math.PI * (p - 0.46));
        double twist = (TWIST * w + RUN_TWIST * run) * Math.sin(2.0 * Math.PI * (p + 0.04));
        double pitch = -LEAN * Math.min(this.speed, MechDrive.WALK) / MechDrive.WALK - RUN_LEAN * run
                + Mth.clamp(BRACE * this.brace, -0.05, 0.05);
        double climbLow = 0.0;
        if (this.climbing != null) {
            double height = this.climbing.height;
            climbLow = MechClimb.low(this.climbAge, height);
            pitch += MechClimb.lean(this.climbAge, height);
            pose.hold = MechClimb.grip(this.climbAge, height);
            pose.ledge[0] = this.climbing.hands[0];
            pose.ledge[1] = this.climbing.hands[1];
            pose.wall = this.climbing.start.ahead();
        }
        // A blow sinks the hips, stoops and twists the torso over them and lifts the stomping foot.
        MechAttacks.Body blow = MechAttacks.body(this.blow);
        double low = -CROUCH * (1.0 + 0.3 * run) * Ease.smooth(this.crouch) + bob + this.settleY + this.sink.value
                - blow.crouch() + climbLow;
        // The hips give a little way to the twist above them and swing back as the torso swings round.
        double share = HIPS_SHARE * this.torsoTurn - HIPS_KICK * this.torso.speed * 0.25;
        MechScript.Stage hips = stage.turned(HIPS, new Vec3(sway, low, 0.0), twist + share, pitch, roll);
        // Where a planted foot would be out of its leg's reach the hips come down to it, and rise back slowly.
        this.squat = Math.max(carried ? this.squat(hips, pose) : 0.0, this.squat - SQUAT_BACK);
        if (this.squat > 1.0E-4) {
            hips = stage.turned(HIPS, new Vec3(sway, low - this.squat, 0.0), twist + share, pitch, roll);
        }
        pose.hips = hips;
        pose.turn = this.torsoTurn - share + blow.twist() + blow.turn();
        // The torso banks against the legs turning under it and leans into how fast it swings.
        this.bank.step(Mth.clamp(-2.4 * this.legsRate + 0.9 * this.torso.speed, -0.12, 0.12), 1.0, 0.08, 0.6);
        this.lean.step(-0.04 * w - RUN_TORSO_LEAN * run + Mth.clamp(-0.6 * this.brace, -0.04, 0.04), 1.0, 0.07,
                0.75);
        pose.lean = this.lean.value - blow.stoop();
        pose.blow = this.blow;
        if (blow.foot() > 0.0) {
            pose.ankle[0] = pose.ankle[0].add(0.0, blow.foot(), 0.0);
            pose.tip[0] = 0.1 * Math.min(1.0, blow.foot());
        }
        pose.bank = this.bank.value;
        pose.torso = MechScript.upper(pose.hips, pose.turn, pose.lean, pose.bank);
        pose.walking = w;
        pose.running = run;
        pose.swing = -Math.sin(2.0 * Math.PI * (p + 0.04)) * w * (1.0 + RUN_PUMP * run);
        pose.headYaw = this.headYaw.value + 0.4 * twist;
        pose.headPitch = this.headPitch.value - 0.5 * (pitch + pose.lean);
        this.levers(pose, rate);
        return pose;
    }

    // How far the hips must come down for every planted foot, and a foot stepping down off an edge, to be within its
    // leg's reach.
    private double squat(MechScript.Stage hips, MechPose pose) {
        double reach = KEEP_REACH * (MechLegShapes.SHIN + MechLegShapes.THIGH);
        double most = 0.0;
        for (int side = 0; side < 2; side++) {
            Leg leg = this.legs[side];
            if (leg.swinging && leg.aim > leg.from.y - STEPPING_DOWN) {
                continue;
            }
            Vec3 to = hips.point(MechPainter.side(MechScript.HIP, side == 0)).subtract(pose.ankle[side]);
            double flat = to.x * to.x + to.z * to.z;
            double need = flat < reach * reach ? to.y - Math.sqrt(reach * reach - flat) : to.y - SQUAT_FLOOR;
            most = Math.max(most, need);
        }
        return Mth.clamp(most, 0.0, SQUAT_MOST);
    }

    private void levers(MechPose pose, double rate) {
        double push = this.speed / MechDrive.WALK;
        double steer = 0.8 * this.turn / MechDrive.TURN;
        MechPose before = this.now;
        double right = Mth.clamp(push + steer, -1.0, 1.0);
        double left = Mth.clamp(push - steer, -1.0, 1.0);
        pose.leverRight = before == null ? right : before.leverRight + Mth.clamp(right - before.leverRight,
                -LEVER_PACE, LEVER_PACE);
        pose.leverLeft = before == null ? left : before.leverLeft + Mth.clamp(left - before.leverLeft, -LEVER_PACE,
                LEVER_PACE);
        boolean still = rate <= 0.0 && Math.abs(pose.leverRight) < 0.05 && Math.abs(pose.leverLeft) < 0.05;
        this.idle = still ? this.idle + 1 : 0;
        if (this.idle < IDLE_BEFORE) {
            return;
        }
        // Standing still, the pilot keeps busy at the console: one button every few seconds.
        int since = this.idle - IDLE_BEFORE;
        int within = since % PRESS_EVERY;
        if (within < PRESS_TICKS) {
            pose.button = Math.floorMod(this.seed * 7 + since / PRESS_EVERY * 3, MechScript.BUTTONS.length);
            pose.press = within < 6 ? Ease.smooth(within / 6.0) * 0.8 : within < 9 ? 0.8 + 0.2 * Ease.bump(
                    (within - 6) / 3.0) : 0.8 * (1.0 - Ease.smooth((within - 9) / 9.0));
        }
    }
}
