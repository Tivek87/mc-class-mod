package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.fx.ParticleAmount;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Spring;

// A built mech's walk, worked out on every client from where it stands tick by tick: each foot stays where it came
// down until its turn to swing, lands where the body will be over it, and the body bobs, sways and twists above the
// legs. Phases count in strides: a full one is both legs, the right lifting at 0 and the left at a half.
// Walking it is heavy and slow: hunched forward and low, it hauls each foot up, carries it over and stamps it straight
// down, its whole weight rolling over onto the planted leg and sinking into every footfall. Running is a gait of its
// own: leaning far into it, each foot kicked back and up behind, then driven far ahead, with a moment where both feet
// are off the ground, the arms pumping and every landing a crash that shakes the ground.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class MechWalk {
    static final double CYCLE = 4.6;
    private static final double SWING = 0.42;
    private static final double[] LIFTS = { 0.0, 0.5 };
    private static final double TURN_ARC = 4.0;
    private static final double SLOWEST = 1.0 / 40.0;
    private static final double SETTLE = 1.0 / 20.0;
    private static final double HOME = 0.35;
    private static final double HOME_TURN = 0.22;
    private static final double LOST = CYCLE * 1.5;
    private static final double LIFT = 1.05;
    // How far into a swing a walking foot is highest; after it the foot drops, faster and faster, onto the ground.
    private static final double PEAK = 0.42;
    private static final double CROUCH = 0.6;
    private static final double BOB = 0.06;
    private static final double SWAY = 0.26;
    private static final double ROLL = 0.06;
    private static final double TWIST = 0.1;
    private static final double LEAN = 0.09;
    // Every footfall sinks the body under its weight, and it springs back up heavily.
    private static final double SINK_WALK = 0.22;
    private static final double SINK_RUN = 0.38;
    private static final double SINK_FREQ = 0.1;
    private static final double SINK_DAMP = 0.45;
    // A running foot is kicked back and up behind before it swings through.
    private static final double HEEL_BACK = 1.1;
    private static final double HEEL_UP = 0.7;
    private static final double RUN_TWIST = 0.14;
    private static final double RUN_PUMP = 1.4;
    private static final int DUST = 10;
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
    // A running stride is longer, its feet longer off the ground and higher, the body lower and further ahead. At
    // full run a foot lands at most a fifth of a stride from under its hip, which a straight leg still reaches.
    private static final double RUN_STRIDE = 1.0;
    private static final double RUN_SWING = 0.26;
    private static final double RUN_LIFT = 1.0;
    private static final double RUN_LEAN = 0.28;
    private static final int IDLE_BEFORE = 20;
    private static final int PRESS_EVERY = 46;
    private static final int PRESS_TICKS = 18;
    private static final double GROUND_ABOVE = 4.0;
    private static final double GROUND_BELOW = 9.0;
    private static final int STEP_SHAKE_TICKS = 7;
    private static final double STEP_SHAKE_RANGE = 36.0;
    private static final int KEEP_BROKEN = 60;
    private static final Vec3 HIPS = new Vec3(0.0, MechScript.HIP.y, 0.0);
    private static final SoundEvent STEP = Sounds.of("mech.step");
    private static final Map<Integer, MechWalk> WALKS = new HashMap<>();
    private static final Map<Integer, Kept> BROKEN = new HashMap<>();
    private static int ticks;

    private record Kept(MechPose pose, int since) {
    }

    private static final class Leg {
        Vec3 planted;
        Vec3 toes;
        Vec3 from;
        Vec3 fromToes;
        boolean swinging;
        double lift;
        int landed = -100;
        // How hard it came down last: 1 walking, more running.
        double hard = 1.0;
    }

    private final int seed;
    private final Leg[] legs = { new Leg(), new Leg() };
    private MechScript.Stage last;
    private MechPose was;
    private MechPose now;
    private int ticked = Integer.MIN_VALUE;
    private int pilot = -1;
    private double torsoTurn;
    private final Spring torso = new Spring();
    private final Spring headYaw = new Spring();
    private final Spring headPitch = new Spring();
    private final Spring bank = new Spring();
    private final Spring lean = new Spring();
    private final Spring sink = new Spring();
    private double legsWas = Double.NaN;
    private double legsRate;
    private double running;
    private double phase;
    private double speed;
    private double side;
    private double turn;
    private double brace;
    private double walking;
    private double crouch;
    private double settleY;
    private int idle;

    private MechWalk(int seed, MechScript.Stage stage) {
        this.seed = seed;
        this.reset(stage);
    }

    private void reset(MechScript.Stage stage) {
        this.last = stage;
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
    // `look` and `pitch`: where its pilot looks, in the game's degrees (NaN when not known).
    public static void step(int id, MechScript.Stage stage, int pilot, float look, float pitch) {
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
        walk.face(stage, look, pitch);
        walk.tick(level, stage);
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

    private void tick(ClientLevel level, MechScript.Stage stage) {
        Vec3 moved = stage.base().subtract(this.last.base());
        if (moved.lengthSqr() > LOST * LOST) {
            this.reset(stage);
            return;
        }
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
        double pace = Math.sqrt(forward * forward + aside * aside);
        double effort = pace + TURN_ARC * Math.abs(angle);
        boolean moving = effort > 2.0E-3;
        boolean busy = this.legs[0].swinging || this.legs[1].swinging || this.away(stage, 0) || this.away(stage, 1);
        double runs = Mth.clamp((pace - MechDrive.WALK) / (MechDrive.RUN - MechDrive.WALK), 0.0, 1.0);
        this.running += Mth.clamp(runs - this.running, -0.05, 0.06);
        double rate = moving ? Math.max(effort / this.stride(), SLOWEST) : busy ? SETTLE : 0.0;
        double wanted = moving ? Math.min(1.0, effort / (MechDrive.WALK * 0.85)) : rate > 0.0 ? 0.3 : 0.0;
        this.walking += Mth.clamp(wanted - this.walking, -0.06, 0.08);
        this.crouch += Mth.clamp((moving || rate > 0.0 ? 1.0 : 0.0) - this.crouch, -0.04, 0.07);
        double from = this.phase;
        this.phase += rate;
        for (int side = 0; side < 2; side++) {
            this.leg(level, stage, side, from, rate);
        }
        this.sink.step(0.0, 1.0, SINK_FREQ, SINK_DAMP);
        this.was = this.now;
        this.now = this.pose(stage, rate);
    }

    private double stride() {
        return CYCLE * (1.0 + RUN_STRIDE * this.running);
    }

    private double swing() {
        return SWING + RUN_SWING * this.running;
    }

    private boolean away(MechScript.Stage stage, int side) {
        Leg leg = this.legs[side];
        Vec3 home = home(stage, side);
        double dx = leg.planted.x - home.x;
        double dz = leg.planted.z - home.z;
        return dx * dx + dz * dz > HOME * HOME || leg.toes.dot(stage.ahead()) < Math.cos(HOME_TURN);
    }

    private void leg(ClientLevel level, MechScript.Stage stage, int side, double from, double rate) {
        Leg leg = this.legs[side];
        double start = LIFTS[side];
        if (!leg.swinging && rate > 0.0 && Math.floor(this.phase - start) > Math.floor(from - start)) {
            leg.swinging = true;
            leg.from = leg.planted;
            leg.fromToes = leg.toes;
            leg.lift = LIFT * (0.55 + 0.45 * Math.min(1.0, this.walking * 1.4)) * (1.0 + RUN_LIFT * this.running);
        }
        if (!leg.swinging) {
            return;
        }
        double into = this.phase - start - Math.floor(this.phase - start);
        double swing = this.swing();
        boolean done = into >= swing;
        double u = done ? 1.0 : into / swing;
        double left = rate > 0.0 ? ((1.0 - u) * swing + (1.0 - swing) * 0.5) / rate : 0.0;
        double ahead = Mth.clamp(this.speed * left, -this.stride() * 0.45, this.stride() * 0.45);
        double across = Mth.clamp(this.side * left, -CYCLE * 0.3, CYCLE * 0.3);
        MechScript.Stage then = stage.turned(Vec3.ZERO, new Vec3(across, 0.0, ahead),
                Mth.clamp(this.turn * left, -0.9, 0.9), 0.0, 0.0);
        Vec3 target = home(then, side);
        Double ground = ground(level, target, stage.base().y);
        target = new Vec3(target.x, (ground == null ? stage.base().y : ground) + MechScript.ANKLE.y, target.z);
        double run = this.running;
        // Walking the foot is carried over early and hangs, running it swings through evenly.
        double carried = 1.0 - (1.0 - u) * (1.0 - u);
        double h = Mth.lerp(run, carried, Ease.smooth(u));
        Vec3 at = leg.from.lerp(target, h);
        leg.toes = leg.fromToes.lerp(then.ahead(), h).normalize();
        double heel = Math.sin(Math.PI * Math.min(1.0, u / 0.55)) * (u < 0.55 ? 1.0 : 0.0) * run;
        Vec3 raised = at.add(0.0, leg.lift * height(u) + HEEL_UP * heel, 0.0)
                .subtract(stage.ahead().scale(HEEL_BACK * heel));
        leg.planted = done ? target : raised;
        if (done) {
            leg.swinging = false;
            leg.toes = then.ahead();
            leg.landed = ticks;
            leg.hard = 1.0 + 0.9 * run;
            this.sink.kick(-(SINK_WALK + (SINK_RUN - SINK_WALK) * run) * Math.min(1.0, this.walking + run));
            Vec3 sole = target.subtract(0.0, MechScript.ANKLE.y, 0.0);
            footfall(level, sole, this.walking + 0.9 * run);
            dust(level, sole, this.walking + run);
        }
    }

    // How high a swinging foot is, 0 to 1 over its swing: hauled up quickly, then stamped down faster and faster.
    private static double height(double u) {
        if (u < PEAK) {
            return Math.sin(Math.PI * 0.5 * u / PEAK);
        }
        double down = (u - PEAK) / (1.0 - PEAK);
        return 1.0 - down * down * down;
    }

    // Dust and bits of the ground thrown up round a foot as it comes down.
    private static void dust(ClientLevel level, Vec3 ground, double hard) {
        int count = (int) Math.round(DUST * Math.min(1.5, hard));
        BlockState below = level.getBlockState(BlockPos.containing(ground.x, ground.y - 0.5, ground.z));
        for (int k = 0; k < count; k++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double out = 0.08 + 0.12 * level.random.nextDouble();
            double x = ground.x + Math.cos(angle) * 0.9;
            double z = ground.z + Math.sin(angle) * 0.9;
            ParticleAmount.add(level, level.random, ParticleTypes.POOF, x, ground.y + 0.1, z,
                    Math.cos(angle) * out, 0.02, Math.sin(angle) * out);
            if (!below.isAir()) {
                ParticleAmount.add(level, level.random, new BlockParticleOption(ParticleTypes.BLOCK, below), x,
                        ground.y + 0.2, z, Math.cos(angle) * 0.2, 0.25, Math.sin(angle) * 0.2);
            }
        }
    }

    // Where a foot stands at rest under the mech, before the ground is found.
    private static Vec3 home(MechScript.Stage stage, int side) {
        return stage.point(MechPainter.side(MechScript.ANKLE, side == 0));
    }

    private static double tip(Leg leg, double phase, double start, double swing) {
        if (!leg.swinging) {
            return 0.0;
        }
        double u = Math.min(1.0, (phase - start - Math.floor(phase - start)) / swing);
        return u < 0.3 ? -0.35 * Math.sin(Math.PI * u / 0.3) : 0.22 * Math.sin(Math.PI * (u - 0.3) / 0.7);
    }

    private MechPose pose(MechScript.Stage stage, double rate) {
        MechPose pose = new MechPose(stage);
        double w = this.walking;
        double p = this.phase;
        double feet = 0.0;
        for (int side = 0; side < 2; side++) {
            Leg leg = this.legs[side];
            pose.ankle[side] = leg.planted;
            pose.toes[side] = leg.toes;
            pose.tip[side] = tip(leg, p, LIFTS[side], this.swing());
            feet += (leg.swinging ? leg.from.y : leg.planted.y) - MechScript.ANKLE.y;
        }
        double settle = Mth.clamp(feet * 0.5 - stage.base().y, -1.5, 1.5);
        this.settleY = Mth.lerp(0.25, this.settleY, settle);
        double run = this.running;
        // Running it springs up off each foot into the stride where both are off the ground.
        double bob = BOB * w * Math.cos(4.0 * Math.PI * (p - 0.2))
                + 0.35 * run * Math.max(0.0, Math.sin(4.0 * Math.PI * (p - 0.05)));
        double sway = SWAY * w * (1.0 - 0.6 * run) * Math.sin(2.0 * Math.PI * (p - 0.46));
        double roll = ROLL * w * (1.0 - 0.4 * run) * Math.sin(2.0 * Math.PI * (p - 0.46));
        double twist = (TWIST * w + RUN_TWIST * run) * Math.sin(2.0 * Math.PI * (p + 0.04));
        double pitch = -LEAN * Math.min(this.speed, MechDrive.WALK) / MechDrive.WALK - RUN_LEAN * run
                + Mth.clamp(BRACE * this.brace, -0.05, 0.05);
        double low = -CROUCH * (1.0 + 0.3 * run) * Ease.smooth(this.crouch) + bob + this.settleY * 0.6
                + this.sink.value;
        // The hips give a little way to the twist above them and swing back as the torso swings round.
        double share = HIPS_SHARE * this.torsoTurn - HIPS_KICK * this.torso.speed * 0.25;
        pose.hips = stage.turned(HIPS, new Vec3(sway, low, 0.0), twist + share, pitch, roll);
        pose.turn = this.torsoTurn - share;
        // The torso banks against the legs turning under it and leans into how fast it swings.
        this.bank.step(Mth.clamp(-2.4 * this.legsRate + 0.9 * this.torso.speed, -0.12, 0.12), 1.0, 0.08, 0.6);
        this.lean.step(-0.04 * w - 0.14 * run + Mth.clamp(-0.6 * this.brace, -0.04, 0.04), 1.0, 0.07, 0.75);
        pose.lean = this.lean.value;
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

    // The top of the ground under at, looking from a little above from down: water counts, the trees and plants a mech
    // wades through do not.
    @Nullable
    static Double ground(ClientLevel level, Vec3 at, double from) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(at.x), 0, Mth.floor(at.z));
        if (!level.isLoaded(pos.setY(Mth.floor(from)))) {
            return null;
        }
        for (int y = Mth.floor(from + GROUND_ABOVE); y >= Mth.floor(from - GROUND_BELOW); y--) {
            pos.setY(y);
            BlockState block = level.getBlockState(pos);
            if (wades(block)) {
                continue;
            }
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.isEmpty()) {
                return y + (double) fluid.getHeight(level, pos);
            }
            VoxelShape shape = block.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return y + shape.max(Direction.Axis.Y);
            }
        }
        return null;
    }

    static boolean wades(BlockState block) {
        return block.is(BlockTags.LEAVES) || block.is(BlockTags.LOGS) || block.is(BlockTags.REPLACEABLE)
                || block.is(BlockTags.FLOWERS) || block.is(BlockTags.SAPLINGS);
    }

    private static void footfall(ClientLevel level, Vec3 ground, double walking) {
        float loud = (float) (0.6 + 0.6 * walking);
        float pitch = 0.9F + 0.2F * level.random.nextFloat();
        level.playLocalSound(ground.x, ground.y, ground.z, STEP, SoundSource.PLAYERS, 1.6F * loud, pitch, false);
        level.playLocalSound(ground.x, ground.y, ground.z, SoundEvents.IRON_GOLEM_STEP, SoundSource.PLAYERS,
                1.4F * loud, 0.5F, false);
    }
}
