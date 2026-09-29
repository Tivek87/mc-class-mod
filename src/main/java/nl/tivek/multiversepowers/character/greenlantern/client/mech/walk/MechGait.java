package nl.tivek.multiversepowers.character.greenlantern.client.mech.walk;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechLegShapes;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.fx.ParticleAmount;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Spring;

// A built mech's feet, worked out on every client from where it stands tick by tick: each foot stays where it came
// down until its turn to swing and lands on firm ground where the body will be over it; a foot left behind steps at
// once, and setting off the foot furthest behind goes first. Phases count in strides: a full one is both legs, the
// right lifting at 0 and the left at a half. Walking it hauls each foot up, carries it over and stamps it straight
// down; running it takes strides twice as long at hardly a quicker beat, each foot pushed off low behind and driven far
// ahead. Dropping off an edge the feet hang until it lands hard, and climbing a ledge (MechClimb) the climb places them.
// MechWalk carries the body over them.
class MechGait {
    static final double CYCLE = 4.6;
    static final double[] LIFTS = { 0.0, 0.5 };
    static final double TURN_ARC = 4.0;
    static final double SLOWEST = 1.0 / 40.0;
    static final double SETTLE = 1.0 / 20.0;
    static final double LOST = CYCLE * 1.5;
    static final double SINK_FREQ = 0.1;
    static final double SINK_DAMP = 0.45;
    // Setting off, a foot steps first straight away until the walk is this far under way.
    static final double SETTING_OFF = 0.12;
    // A planted foot's leg reaches no further than this share of its length.
    static final double KEEP_REACH = 0.97;
    private static final double SWING = 0.42;
    private static final double HOME = 0.35;
    private static final double HOME_TURN = 0.22;
    private static final double LIFT = 1.05;
    // How far into a swing a walking foot is highest; after it the foot drops, faster and faster, onto the ground.
    private static final double PEAK = 0.42;
    // Every footfall sinks the body under its weight, and it springs back up heavily.
    private static final double SINK_WALK = 0.22;
    private static final double SINK_RUN = 0.5;
    // A running foot pushes off low behind before it swings through.
    private static final double HEEL_BACK = 0.45;
    private static final double HEEL_UP = 0.3;
    private static final double STAMP = 1.2;
    private static final int DUST = 10;
    // A running stride is twice as long at hardly a quicker beat, its feet as long off the ground as on it and not much
    // higher, swung through more evenly than walking.
    private static final double RUN_STRIDE = 1.0;
    private static final double RUN_SWING = 0.08;
    private static final double RUN_LIFT = 0.3;
    private static final double RUN_EVEN = 0.4;
    // How long after lifting a foot may lift again on its turn, and how many times further behind than a stride
    // leaves it a planted foot must be left to step out of turn.
    private static final double RESTEP = 0.75;
    private static final double DRAGGED = 1.35;
    // A foot steps down no further than this; before a deeper hole it comes down on firm ground this far past or
    // short of where it was going, the nearest first.
    private static final double STEP_DOWN = 2.2;
    private static final double[] SHIFTS = { 0.0, 0.8, -0.8, 1.6, -1.6, -2.4, -3.2 };
    // It falls once its base drops this fast this far under its feet; they hang drawn up under it and come down hard.
    private static final double FALL_SPEED = 0.2;
    private static final double FALL_GAP = 0.5;
    private static final double LANDED = 0.02;
    private static final Vec3 HANG = new Vec3(MechScript.ANKLE.x, MechScript.ANKLE.y + 0.9, 0.3);
    private static final double LAND_HARD = 2.2;
    private static final double LAND_SINK = 0.8;
    private static final double CLIMB_STEP = 1.5;
    private static final double CLIMB_SLAM = 0.8;
    private static final SoundEvent STEP = Sounds.of("mech.step");
    static int ticks;

    static final class Leg {
        Vec3 planted;
        Vec3 toes;
        Vec3 from;
        Vec3 fromToes;
        boolean swinging;
        double lift;
        // The phase it last lifted at, the height its ankle is going down to and how far before or past a hole
        // (along the way it goes) it has picked to come down.
        double lifted = -10.0;
        double aim;
        double shift;
        int landed = -100;
        // How hard it came down last: 1 walking, more running.
        double hard = 1.0;
    }

    final Leg[] legs = { new Leg(), new Leg() };
    MechPose was;
    MechPose now;
    final Spring sink = new Spring();
    double running;
    double phase;
    double speed;
    double side;
    double turn;
    double walking;
    double crouch;
    boolean falling;
    @Nullable
    MechClimb.Hold climbing;
    double climbAge;
    MechAttacks.Blow blow = MechAttacks.Blow.NONE;
    int hitAt = -100;
    double hitHard;
    Vec3 hitSpot = Vec3.ZERO;

    // Setting off from standing, the foot furthest behind where it is going steps first, straight away.
    void lead(MechScript.Stage stage, Vec3 moved) {
        Vec3 way = moved.lengthSqr() < 1.0E-8 ? stage.ahead() : moved.normalize();
        double right = this.legs[0].planted.subtract(home(stage, 0)).dot(way);
        double left = this.legs[1].planted.subtract(home(stage, 1)).dot(way);
        double start = LIFTS[right <= left ? 0 : 1];
        this.phase = start + Math.ceil(this.phase - start) - 1.0E-6;
    }

    double stride() {
        return CYCLE * (1.0 + RUN_STRIDE * this.running);
    }

    double swing() {
        return SWING + RUN_SWING * this.running;
    }

    // How far through its swing a lifted foot is, 0 to 1.
    double swung(Leg leg) {
        return Mth.clamp((this.phase - leg.lifted) / this.swing(), 0.0, 1.0);
    }

    boolean away(MechScript.Stage stage, int side) {
        Leg leg = this.legs[side];
        Vec3 home = home(stage, side);
        double dx = leg.planted.x - home.x;
        double dz = leg.planted.z - home.z;
        return dx * dx + dz * dz > HOME * HOME || leg.toes.dot(stage.ahead()) < Math.cos(HOME_TURN);
    }

    // Whether a planted foot has been left further off than a stride lets it go, or out of its leg's reach: it steps
    // at once then, out of turn.
    private boolean stretched(MechScript.Stage stage, int side) {
        Leg leg = this.legs[side];
        Vec3 home = home(stage, side);
        double dx = leg.planted.x - home.x;
        double dz = leg.planted.z - home.z;
        double most = DRAGGED * (1.0 - this.swing()) * 0.5 * this.stride() + 0.4;
        if (dx * dx + dz * dz > most * most) {
            return true;
        }
        Vec3 hip = this.now.hips().point(MechPainter.side(MechScript.HIP, side == 0));
        return hip.distanceTo(leg.planted) > KEEP_REACH * (MechLegShapes.SHIN + MechLegShapes.THIGH);
    }

    void leg(ClientLevel level, MechScript.Stage stage, int side, double from, double rate) {
        Leg leg = this.legs[side];
        double start = LIFTS[side];
        // Striking a blow it stands its ground: a foot already swinging comes down, no other lifts.
        if (!leg.swinging && rate > 0.0 && !this.blow.striking()) {
            boolean due = Math.floor(this.phase - start) > Math.floor(from - start)
                    && this.phase - leg.lifted > RESTEP;
            if (due || !this.legs[1 - side].swinging && this.stretched(stage, side)) {
                leg.swinging = true;
                leg.lifted = due ? start + Math.floor(this.phase - start) : this.phase;
                leg.from = leg.planted;
                leg.fromToes = leg.toes;
                leg.shift = 0.0;
                leg.lift = LIFT * (0.55 + 0.45 * Math.min(1.0, this.walking * 1.4)) * (1.0 + RUN_LIFT * this.running);
            }
        }
        if (!leg.swinging) {
            return;
        }
        double u = this.swung(leg);
        boolean done = u >= 1.0;
        double swing = this.swing();
        double left = rate > 0.0 ? ((1.0 - u) * swing + (1.0 - swing) * 0.5) / rate : 0.0;
        double ahead = Mth.clamp(this.speed * left, -this.stride() * 0.45, this.stride() * 0.45);
        double across = Mth.clamp(this.side * left, -CYCLE * 0.3, CYCLE * 0.3);
        MechScript.Stage then = stage.turned(Vec3.ZERO, new Vec3(across, 0.0, ahead),
                Mth.clamp(this.turn * left, -0.9, 0.9), 0.0, 0.0);
        Vec3 target = foothold(level, leg, home(then, side), then.ahead(), stage.base().y);
        leg.aim = target.y;
        double run = this.running;
        // Walking the foot is carried over early and hangs; running it swings through more evenly.
        double carried = 1.0 - (1.0 - u) * (1.0 - u);
        double h = Mth.lerp(RUN_EVEN * run, carried, Ease.smooth(u));
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
            leg.hard = 1.0 + STAMP * run;
            this.sink.kick(-(SINK_WALK + (SINK_RUN - SINK_WALK) * run) * Math.min(1.0, this.walking + run));
            Vec3 sole = target.subtract(0.0, MechScript.ANKLE.y, 0.0);
            footfall(level, sole, this.walking + STAMP * run);
            dust(level, sole, this.walking + run);
        }
    }

    // Where a foot can come down near `at`: on the ground there; where that is a hole deeper than it steps down (a
    // trench, a crack) on firm ground just past or short of it, keeping to the same side of it through its swing;
    // failing that, down into the hole.
    private static Vec3 foothold(ClientLevel level, Leg leg, Vec3 at, Vec3 ahead, double floor) {
        double top = floor + MechDrive.STEP_UP;
        Double deep = null;
        for (int k = leg.shift == 0.0 ? 0 : -1; k < SHIFTS.length; k++) {
            double shift = k < 0 ? leg.shift : SHIFTS[k];
            Vec3 spot = at.add(ahead.scale(shift));
            Double ground = MechGround.foot(level, spot, ahead, floor, top);
            if (ground != null && ground >= floor - STEP_DOWN) {
                leg.shift = shift;
                return new Vec3(spot.x, ground + MechScript.ANKLE.y, spot.z);
            }
            if (k == 0) {
                deep = ground;
            }
        }
        leg.shift = 0.0;
        return new Vec3(at.x, (deep == null ? floor : deep) + MechScript.ANKLE.y, at.z);
    }

    // The body drops away from where the feet stand, off an edge into a pit: the feet leave the ground and hang under
    // it, and where it lands they come down hard. Whether it is still falling.
    boolean fall(ClientLevel level, MechScript.Stage stage, double dropped) {
        if (!this.falling) {
            double feet = Math.min(this.legs[0].planted.y, this.legs[1].planted.y) - MechScript.ANKLE.y;
            if (dropped < FALL_SPEED || feet - stage.base().y < FALL_GAP) {
                return false;
            }
            this.falling = true;
        }
        if (dropped > LANDED) {
            for (int side = 0; side < 2; side++) {
                Leg leg = this.legs[side];
                leg.swinging = false;
                leg.planted = leg.planted.lerp(stage.point(MechPainter.side(HANG, side == 0)), 0.5);
                leg.toes = stage.ahead();
            }
            return true;
        }
        this.falling = false;
        double floor = stage.base().y;
        for (int side = 0; side < 2; side++) {
            Leg leg = this.legs[side];
            Double ground = MechGround.foot(level, leg.planted, stage.ahead(), floor, floor + MechDrive.STEP_UP);
            leg.planted = new Vec3(leg.planted.x, (ground == null ? floor : ground) + MechScript.ANKLE.y,
                    leg.planted.z);
            leg.landed = ticks;
            leg.hard = LAND_HARD;
            Vec3 sole = leg.planted.subtract(0.0, MechScript.ANKLE.y, 0.0);
            footfall(level, sole, LAND_HARD);
            dust(level, sole, LAND_HARD);
        }
        this.sink.kick(-LAND_SINK);
        return false;
    }

    // Climbing, the feet and hands go where the climb puts them, and the walk comes to a stop.
    void climb(ClientLevel level, MechScript.Stage stage, int packed) {
        double age = MechClimb.age(packed);
        double height = MechClimb.height(packed);
        double edge = MechClimb.edge(packed);
        MechScript.Stage start = MechClimb.start(stage, age, height, edge);
        if (this.climbing == null || this.climbing.start.base().distanceToSqr(start.base()) > 0.5) {
            this.climbing = MechClimb.hold(level, start, height, edge,
                    new Vec3[] { this.legs[0].planted, this.legs[1].planted });
            this.falling = false;
        }
        MechClimb.Hold hold = this.climbing;
        this.climbAge = age;
        for (int side = 0; side < 2; side++) {
            Leg leg = this.legs[side];
            leg.swinging = false;
            leg.planted = MechClimb.foot(hold, side, age);
            leg.toes = start.ahead();
            if (MechClimb.lands(hold, side, age)) {
                leg.landed = ticks;
                leg.hard = CLIMB_STEP;
                Vec3 sole = leg.planted.subtract(0.0, MechScript.ANKLE.y, 0.0);
                footfall(level, sole, CLIMB_STEP);
                dust(level, sole, CLIMB_STEP);
                this.sink.kick(-0.2);
            }
        }
        if (MechClimb.slams(age)) {
            this.hitAt = ticks;
            this.hitHard = CLIMB_SLAM;
            this.hitSpot = hold.hands[0].lerp(hold.hands[1], 0.5);
            for (Vec3 hand : hold.hands) {
                Vec3 on = hand.subtract(0.0, MechClimb.GRIP_UP, 0.0);
                footfall(level, on, CLIMB_SLAM);
                dust(level, on, CLIMB_SLAM);
            }
        }
        this.speed = 0.0;
        this.side = 0.0;
        this.turn = 0.0;
        this.running += Mth.clamp(-this.running, -0.05, 0.06);
        this.walking += Mth.clamp(-this.walking, -0.06, 0.08);
        this.crouch += Mth.clamp(1.0 - this.crouch, -0.04, 0.07);
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
    static Vec3 home(MechScript.Stage stage, int side) {
        return stage.point(MechPainter.side(MechScript.ANKLE, side == 0));
    }

    static double tip(double u) {
        return u < 0.3 ? -0.35 * Math.sin(Math.PI * u / 0.3) : 0.22 * Math.sin(Math.PI * (u - 0.3) / 0.7);
    }

    private static void footfall(ClientLevel level, Vec3 ground, double walking) {
        float loud = (float) (0.6 + 0.6 * walking);
        float pitch = 0.9F + 0.2F * level.random.nextFloat();
        level.playLocalSound(ground.x, ground.y, ground.z, STEP, SoundSource.PLAYERS, 1.6F * loud, pitch, false);
        level.playLocalSound(ground.x, ground.y, ground.z, SoundEvents.IRON_GOLEM_STEP, SoundSource.PLAYERS,
                1.4F * loud, 0.5F, false);
    }
}
