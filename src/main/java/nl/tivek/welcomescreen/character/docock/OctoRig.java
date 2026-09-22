package nl.tivek.welcomescreen.character.docock;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.network.ArmPayload;
import nl.tivek.welcomescreen.network.GrabStatePayload;
import nl.tivek.welcomescreen.spell.HeldMobs;
import nl.tivek.welcomescreen.spell.RobotArm;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;
import nl.tivek.welcomescreen.spell.SpellFx;
import nl.tivek.welcomescreen.spell.SpellTargeting;

/**
 * One player's four Octopus Arms while they are out. Two rise over the shoulders and do the grabbing,
 * striking and blocking; two walk on the ground like legs and carry the player above the ground.
 * Every tentacle works on its own, so several can be busy at the same time: two creatures held while
 * a third tentacle strikes, or the Portal ability running while the rest keeps walking.
 *
 * <p>Each tentacle has a tip that glides towards where its current job wants it, so every move blends
 * smoothly into the next.
 */
final class OctoRig implements SpellEffect {
    private static final int UNFOLD = 14;
    private static final int FOLD = 12;
    private static final double CLAW_REST = 0.3;
    private static final double MAX_STEP = 3.0;
    // How a tip moves: a spring instead of a straight step. Stiffness is how hard it is pulled (on top
    // of each pose's own eagerness), damping how much of its speed it keeps every tick. Together they
    // sit just under critical, so every tip speeds up and eases off by itself and nothing jerks.
    private static final double SPRING = 0.38;
    private static final double DAMPING = 0.70;
    // How quickly a claw turns towards the way it should be looking.
    private static final double AIM_EASE = 0.18;

    // Tentacle Strike and Multi-Tentacle.
    private static final int STRIKE_MAX = 7;
    private static final int MULTI_GAP = 3;

    // Grab and Throw. A tentacle never lets go by itself: only you do (see letGoAll).
    private static final double GRAB_SPEED = 1.3;
    private static final int GRAB_MAX = 26;
    private static final double HOLD_DISTANCE = 3.2;
    // How hard a held creature is whipped along with your view: high enough to smash it into walls.
    private static final double FOLLOW = 0.75;
    private static final double MAX_SPEED = 3.6;
    private static final double THROW_SPEED = 2.6;
    private static final int CRASH_PAUSE = 5;
    private static final int THROWN_TRACK = 40;
    // Smashing everything you hold into the ground (Ground Slam with full claws).
    private static final int HELD_SLAM_TIME = 10;

    // Tentacle Dash.
    // How hard a dash throws you upwards; how far it throws you is a setting of the ability itself.
    private static final double DASH_LIFT = 0.5;
    private static final int PLANT_TIME = 6;

    // Ground Slam.
    private static final int RISE_TIME = 8;
    private static final int MAX_DROP = 40;
    private static final double AIR_DROP = 1.8;
    private static final double SLAM_RADIUS = 6.0;

    private static final double AIR_SLAM_RADIUS = 8.0;


    // Octopus Rampage.
    private static final double RAMPAGE_RANGE = 8.0;
    private static final int RAMPAGE_EVERY = 12;

    // Legs: how far a foot may fall behind before it steps, and how long a step takes.
    private static final double STEP_AFTER = 1.2;
    private static final int STEP_TIME = 7;
    private static final double LEG_SPREAD = 1.45;
    // How far under the player the legs still find ground to stand on.
    private static final double LEG_DROP = 3.2;
    // Walking on tentacles: on 2, 3 or 4 of them. 0 means walking on your own feet.
    private static final int[] STANCES = { 0, 2, 3, 4 };

    // Wall Climb: every tentacle holds its own grip, far away from the others.
    private static final double GRIP_REACH = 3.4;
    private static final double GRIP_MIN_APART = 1.9;
    private static final int GRIP_EVERY = 4;
    // How far out a climbing tentacle that holds nothing reaches, so the four keep their distance
    // even while they are still looking for a new grip.
    private static final double CLIMB_SPREAD = 2.0;
    // How far two claws are always kept apart, whatever pose they are in: without this two
    // tentacles that want the same piece of air end up crossing or sitting inside each other.
    private static final double TIP_APART = 1.15;
    // How much faster the tentacles carry you when you run on them.
    private static final double LEG_RUN_BONUS = 0.4;
    // The chance per tick that a tentacle starts looking around while you stand still.
    private static final double IDLE_CHANCE = 0.004;
    // Blocks and Building is not on a key any more; what is left of it works on these numbers.
    private static final double BUILD_RANGE = 10.0;
    private static final int BUILD_CLUSTER = 27;
    private static final float BUILD_DAMAGE = 8.0F;

    // Block.
    private static final int SHIELD_TIME = 6;
    private static final double SHIELD_SIZE = 1.15;

    // Ground Strike: into the ground at your feet, out of the ground at the creatures you marked.
    // How long the tip takes to reach the hole, how deep it goes in, and how fast it slides.
    // How long the tentacles are first held up in front of you, spikes sliding out of the claws, so
    // you can see the strike coming before anything goes into the ground.
    private static final int SHOW_TIME = 9;
    private static final int DIVE_TIME = 6;
    private static final double SINK_DEPTH = 3.5;
    private static final double SINK_SPEED = 0.8;
    // How long it travels under the ground (a bit longer the further it has to go).
    private static final int TRAVEL_MIN = 5;
    private static final double TRAVEL_PER_BLOCK = 0.35;
    // Coming up: how fast, how far it keeps going above the ground, and how long it stays up.
    private static final double RISE_SPEED = 1.35;
    private static final double RISE_ABOVE = 1.2;
    private static final int SPIKE_HOLD = 6;
    private static final double SPIKE_SINK = 0.9;
    // How deep under the ground the spike starts before it shoots up.
    private static final double SPIKE_BURIED = 3.0;

    private enum Job {
        REST, STRIKE, REACH, HOLD, RISE, SMASH, PLANT, PORTAL, CARRY, BURROW
    }

    /** The steps one tentacle goes through during a Ground Strike, in this order. */
    private enum Dig {
        SHOW, TO_HOLE, SINK, UNDER, UP, DOWN, BACK
    }

    private enum Hit {
        MELEE, MULTI, RAMPAGE
    }

    private enum Slam {
        NONE, RISE, DROP, SMASH
    }

    private static final class Arm {
        private final int id = RobotArm.newId();
        private final int index;
        private final boolean upper;
        private final double side;
        private Vec3 tip;
        private double blend;
        private double claw = CLAW_REST;
        private Job job = Job.REST;
        private int age;
        @Nullable
        private LivingEntity target;
        @Nullable
        private LivingEntity held;
        private int holdTicks;
        private double holdDistance = HOLD_DISTANCE;
        private int crashPause;
        private Hit hit = Hit.MELEE;
        private int delay;
        private int struckAt = -1;
        private Vec3 spot = Vec3.ZERO;
        // Legs: where the foot stands, and a step in progress.
        @Nullable
        private Vec3 foot;
        private Vec3 stepFrom = Vec3.ZERO;
        private Vec3 stepTo = Vec3.ZERO;
        private int step = -1;
        private boolean leg;
        // Wall Climb: the spot this tentacle holds on to, kept until it is out of reach.
        @Nullable
        private Vec3 grip;
        private int gripAge;
        // The blocks this tentacle carries (see TentacleBlocks); null when it carries none.
        @Nullable
        private TentacleBlocks.Load load;
        // Only used while this tentacle runs the Portal ability.
        private double tipOffset;
        private Vec3 clipPoint = Vec3.ZERO;
        private Vec3 clipNormal = Vec3.ZERO;
        private int cut;
        // The Portal ability's tools at the tip: 0 = away, 1 = fully out.
        private double spike;
        private double thrust;
        // How fast the tip is going right now, so it can speed up and slow down instead of jumping.
        private Vec3 speed = Vec3.ZERO;
        // Where the claw should point (zero: straight on out of the tentacle) and where it really
        // points right now, which follows the first one smoothly.
        private Vec3 aim = Vec3.ZERO;
        private Vec3 aimShown = Vec3.ZERO;
        // Standing still: where this claw is looking right now, and between which ticks.
        private Vec3 glance = Vec3.ZERO;
        private boolean lookAtYou;
        private int glanceFrom;
        private int glanceUntil;
        // Ground Strike: which step it is in, and the far part that comes up out of the ground.
        private Dig dig = Dig.SHOW;
        private int farId = -1;
        private Vec3 digAt = Vec3.ZERO;
        private double risen;
        private double digGround;
        private int digTravel;
        private boolean digHit;

        private Arm(int index, Vec3 start) {
            this.index = index;
            this.upper = index < 2;
            this.side = index % 2 == 0 ? 1 : -1;
            this.tip = start;
        }

        /** Nothing to do, so it can take on a new job. A leg counts as busy: it carries you. */
        private boolean free() {
            return this.job == Job.REST && !this.leg;
        }
    }

    private final ServerPlayer caster;
    // The number of walking tentacles and marked creatures the client was last told about.
    private int syncedLegs;
    private int syncedMarks;
    private final Arm[] arms = new Arm[4];
    private int age;
    private double unfold;
    private boolean folding;
    private boolean blocking;
    private int shieldId = -1;
    private double shieldOpen;
    private boolean climbing;
    private Direction climbFace = Direction.NORTH;
    private int lastBlocked = -100;
    private int rampage;
    private int nextArm;
    private boolean delivering;
    private boolean throwRequested;
    // Which stance you walk in: an index in STANCES (starts on two tentacles).
    private int stance = 1;
    // Ground Strike: the creatures you picked out, waiting for you to launch the strike.
    private final List<LivingEntity> marks = new ArrayList<>();
    // Whether you are running on the tentacles right now, so the speed only changes when that does.
    private boolean legsRunning;
    // Counts down while the tentacles smash everything they hold into the ground.
    private int heldSlam;

    @Nullable
    private PortalRun portal;
    private int portalArm = -1;

    private Slam slam = Slam.NONE;
    private int slamAge;
    private boolean airSlam;

    OctoRig(ServerPlayer caster) {
        this.caster = caster;
        for (int i = 0; i < this.arms.length; i++) {
            this.arms[i] = new Arm(i, this.mount(i));
        }
    }

    // ---- State the rest of the mod asks about ----

    boolean isFolding() {
        return this.folding;
    }

    boolean isBlocking() {
        return this.blocking && !this.folding;
    }

    boolean isClimbing() {
        return this.climbing;
    }

    boolean isHolding() {
        for (Arm arm : this.arms) {
            if (arm.held != null) {
                return true;
            }
        }
        return false;
    }

    boolean holds(Entity entity) {
        for (Arm arm : this.arms) {
            if (arm.held == entity) {
                return true;
            }
        }
        return false;
    }

    boolean justBlocked() {
        return this.age - this.lastBlocked <= 1;
    }

    int rampageLeft() {
        return this.rampage;
    }

    /** How many tentacles walk under you right now; 0 when you walk on your own feet. */
    int legCount() {
        if (this.folding || this.climbing) {
            return 0;
        }
        int legs = 0;
        for (Arm arm : this.arms) {
            if (arm.leg) {
                legs++;
            }
        }
        return legs;
    }

    /** How many creatures you have marked for a Ground Strike right now. */
    int markCount() {
        return this.folding ? 0 : this.marks.size();
    }

    /** True when a tentacle holds a creature or a load of blocks, so the attack button throws. */
    private boolean hasThrowable() {
        for (Arm arm : this.arms) {
            if (arm.held != null || arm.load != null) {
                return true;
            }
        }
        return false;
    }

    /** True while at least one tentacle carries blocks, so a right click can set them down. */
    private boolean hasLoad() {
        for (Arm arm : this.arms) {
            if (arm.load != null) {
                return true;
            }
        }
        return false;
    }

    void fold() {
        if (!this.folding) {
            this.folding = true;
            this.marks.clear();
            this.dropLoads(this.caster.serverLevel());
            this.letGo();
            this.setBlocking(false);
            this.endRampage();
            this.sound(this.caster.position(), SoundEvents.PISTON_CONTRACT, 1.0F, 0.6F);
            OctopusArms.sync(this.caster);
        }
    }

    // ---- Ticking ----

    @Override
    public boolean tick(ServerLevel level, int age) {
        this.age = age;
        if (this.caster.isRemoved() || !this.caster.isAlive() || this.caster.level() != level
                || this.caster.isSpectator()) {
            this.shutDown();
            return false;
        }
        // The tentacles carry the player through the air; the server must not see that as flying.
        this.caster.connection.aboveGroundTickCount = 0;
        if (this.climbing) {
            this.caster.resetFallDistance();
        }
        if (this.folding) {
            this.unfold -= 1.0 / FOLD;
            if (this.unfold <= 0.0) {
                this.shutDown();
                return false;
            }
        } else {
            this.unfold = Math.min(1.0, this.unfold + 1.0 / UNFOLD);
        }
        this.assignLegs();
        this.tickMarks(level);
        this.tickPortal(level);
        this.tickHold(level);
        this.tickSlam(level);
        this.tickHeldSlam();
        this.tickRampage(level);
        for (Arm arm : this.arms) {
            this.tickArm(level, arm);
        }
        this.spreadTips();
        this.tickAims();
        this.tickLegSpeed();
        this.tickShield(level);
        this.draw(level);
        // A tentacle that starts holding something stops walking. The player's own client lifts them up
        // on the legs, so it has to hear about that at once, not only at the next key press.
        int legs = this.legCount();
        if (legs != this.syncedLegs || this.marks.size() != this.syncedMarks) {
            this.syncedLegs = legs;
            this.syncedMarks = this.marks.size();
            OctopusArms.sync(this.caster);
        }
        return true;
    }

    /** Everything off and gone, at once. */
    void shutDown() {
        this.letGo();
        this.dropMarks();
        ServerLevel level = this.caster.serverLevel();
        this.dropLoads(level);
        if (this.portal != null) {
            this.portal.stop(level);
            this.portal = null;
        }
        for (Arm arm : this.arms) {
            RobotArm.remove(level, arm.id);
            if (arm.farId >= 0) {
                RobotArm.remove(level, arm.farId);
                arm.farId = -1;
            }
        }
        if (this.shieldId >= 0) {
            RobotArm.removePortal(level, this.shieldId);
            this.shieldId = -1;
        }
        OctopusArms.removed(this.caster, this);
    }

    private void tickArm(ServerLevel level, Arm arm) {
        arm.age++;
        Vec3 goal;
        double follow;
        double blendTo;
        Job job = arm.job;
        if (job == Job.STRIKE && arm.delay > 0) {
            arm.delay--;
            job = Job.REST;
        }
        switch (job) {
            case STRIKE -> {
                LivingEntity target = arm.target;
                if (target == null || !target.isAlive() || target.level() != level) {
                    this.toRest(arm);
                    return;
                }
                goal = target.getBoundingBox().getCenter();
                follow = 0.7;
                blendTo = 1;
                if (arm.struckAt < 0) {
                    arm.claw = 0.6;
                    double reach = 1.0 + target.getBbWidth() * 0.5;
                    if (arm.tip.distanceTo(goal) <= reach || arm.age > STRIKE_MAX) {
                        this.deliver(level, arm, target);
                        arm.struckAt = arm.age;
                        arm.claw = 0.12;
                    }
                } else if (arm.age - arm.struckAt >= 2) {
                    this.toRest(arm);
                    return;
                }
            }
            case REACH -> {
                LivingEntity target = arm.target;
                if (target == null || !target.isAlive() || target.level() != level || arm.age > GRAB_MAX) {
                    this.toRest(arm);
                    return;
                }
                Vec3 center = target.getBoundingBox().getCenter();
                Vec3 toGoal = center.subtract(arm.tip);
                double distance = toGoal.length();
                goal = distance > GRAB_SPEED ? arm.tip.add(toGoal.scale(GRAB_SPEED / distance)) : center;
                follow = 1.0;
                blendTo = 1;
                arm.claw = 0.7;
                if (arm.tip.distanceTo(center) <= 0.9 + target.getBbWidth() * 0.5) {
                    this.seize(level, arm, target);
                }
            }
            case HOLD -> {
                goal = arm.held != null ? arm.held.getBoundingBox().getCenter() : arm.tip;
                follow = 1.0;
                blendTo = 1;
                arm.claw = arm.held != null ? arm.held.getBbWidth() * 0.5 + 0.25 : CLAW_REST;
            }
            case PORTAL -> {
                PortalRun run = this.portal;
                if (run == null) {
                    this.toRest(arm);
                    return;
                }
                goal = run.tip();
                follow = run.snap() ? 1.0 : 0.4;
                blendTo = run.snap() ? 1.0 : 0.6;
                arm.claw = run.claw();
                arm.tipOffset = run.tipOffset();
                arm.clipPoint = run.clipPoint();
                arm.clipNormal = run.clipNormal();
                arm.spike = run.spike();
                arm.thrust = run.thrust();
            }
            case BURROW -> {
                this.tickDig(level, arm);
                return;
            }
            case CARRY -> {
                // Blocks in the claw: held out beside you, ready to place or to throw.
                goal = this.carryPose(arm);
                follow = 0.5;
                blendTo = 0.2;
                arm.claw = 0.55;
                if (arm.load == null) {
                    this.toRest(arm);
                    return;
                }
            }
            case RISE -> {
                goal = this.caster.position().add(0, 3.3, 0)
                        .add(this.right().scale(arm.side * (arm.upper ? 0.9 : 1.7)))
                        .add(this.forward().scale(arm.upper ? 0.5 : -0.5));
                follow = 0.4;
                blendTo = 0;
                arm.claw = 0.75;
            }
            case SMASH -> {
                goal = arm.spot;
                follow = 0.85;
                blendTo = 1;
                arm.claw = 0.5;
                if (arm.age > 10) {
                    this.toRest(arm);
                    return;
                }
            }
            case PLANT -> {
                goal = arm.spot;
                follow = 0.9;
                blendTo = 1;
                arm.claw = 0.2;
                if (arm.age > PLANT_TIME) {
                    this.toRest(arm);
                    return;
                }
            }
            default -> {
                // Resting: climbing, blocking, walking on the ground, or waving over the shoulder.
                if (this.climbing) {
                    goal = this.climbPose(level, arm);
                    follow = 0.45;
                    blendTo = 0.35;
                    arm.claw = 0.18;
                    // The claw bites into the surface it holds.
                    arm.aim = Vec3.atLowerCornerOf(this.climbFace.getNormal()).scale(-1);
                } else if (arm.leg) {
                    this.walk(level, arm);
                    return;
                } else if (this.isBlocking()) {
                    goal = this.blockPose(arm);
                    follow = 0.55;
                    blendTo = 0;
                    arm.claw = 0.1;
                    // Claws turned on whatever is in front of you.
                    arm.aim = this.forward();
                } else {
                    goal = this.restPose(arm);
                    follow = 0.4;
                    blendTo = 0;
                    arm.claw += (CLAW_REST - arm.claw) * 0.14;
                }
                arm.foot = null;
                arm.step = -1;
                if (!this.climbing) {
                    arm.grip = null;
                }
            }
        }
        // Reaching for something or holding it: the claw turns to look straight at it.
        if (job == Job.STRIKE || job == Job.REACH || job == Job.HOLD) {
            arm.aim = goal.subtract(arm.tip);
        }
        this.glide(arm, goal, follow, blendTo);
    }

    /**
     * Keeps the four claws out of each other. Two tentacles that want the same piece of air are pushed
     * apart along the line between them, so they never cross, sit inside one another, or look like one
     * mirrored pair. A tentacle that must be exactly somewhere (holding, hitting, digging, a planted
     * foot or a grip on a wall) is left alone.
     */
    private void spreadTips() {
        // Twice, so a tentacle pushed away from one neighbour is still checked against the others.
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < this.arms.length; i++) {
                for (int j = i + 1; j < this.arms.length; j++) {
                    this.pushApart(this.arms[i], this.arms[j]);
                }
            }
        }
    }

    /**
     * Turns every claw a little further towards the way it should be looking. Even a tentacle that
     * suddenly looks back at you swings its last stretch over instead of flipping round.
     */
    private void tickAims() {
        for (Arm arm : this.arms) {
            Vec3 wanted = arm.aim.lengthSqr() > 1.0E-6 ? arm.aim.normalize() : this.naturalAim(arm);
            if (arm.aimShown.lengthSqr() < 1.0E-6) {
                arm.aimShown = wanted;
                continue;
            }
            Vec3 next = arm.aimShown.add(wanted.subtract(arm.aimShown).scale(AIM_EASE));
            arm.aimShown = next.lengthSqr() < 1.0E-6 ? wanted : next.normalize();
        }
    }

    /** The way a claw points when nothing asks it to look anywhere: on out of the bend of the arm. */
    private Vec3 naturalAim(Arm arm) {
        Vec3 mount = this.mount(arm.index);
        Vec3 out = arm.tip.subtract(this.elbow(arm, mount));
        return out.lengthSqr() < 1.0E-6 ? this.forward() : out.normalize();
    }

    /** The bend just before the claw, when the tentacle is left to hang the way it likes. */
    private Vec3 elbow(Arm arm, Vec3 mount) {
        double reach = mount.distanceTo(arm.tip);
        double bow = arm.upper ? Math.min(1.5, 0.35 + reach * 0.12) : Math.min(1.5, 0.9 + reach * 0.1);
        return arm.tip.add(mount.subtract(arm.tip).scale(0.35)).add(0, bow, 0);
    }

    private void pushApart(Arm a, Arm b) {
        if (!mayBeNudged(a) || !mayBeNudged(b)) {
            return;
        }
        Vec3 between = b.tip.subtract(a.tip);
        double distance = between.length();
        if (distance >= TIP_APART) {
            return;
        }
        // Exactly on top of each other: split them left and right instead of dividing by nothing.
        // Only part of the way each tick: they drift apart over a few ticks instead of snapping
        // apart and shivering against one another.
        Vec3 push = distance < 1.0E-4
                ? this.right().scale(TIP_APART * 0.3)
                : between.scale((TIP_APART - distance) * 0.3 / distance);
        a.tip = a.tip.subtract(push);
        b.tip = b.tip.add(push);
    }

    /** True when this tentacle may be nudged: it is not holding, hitting or standing on anything. */
    private static boolean mayBeNudged(Arm arm) {
        return (arm.job == Job.REST || arm.job == Job.CARRY) && !arm.leg && arm.grip == null
                && arm.foot == null && arm.step < 0;
    }

    /**
     * Moves a tip towards where its pose wants it. Not a straight step every tick but a spring: the tip
     * builds up speed, carries it, and eases off again, so it never starts or stops with a jerk and it
     * swings through a turn instead of cornering. A pose that has to be exactly somewhere (an eagerness
     * of 1, like a claw around a creature) is still set straight down, or what it holds would drift.
     */
    private void glide(Arm arm, Vec3 goal, double follow, double blendTo) {
        if (follow >= 0.999) {
            arm.speed = goal.subtract(arm.tip);
            arm.tip = goal;
        } else {
            arm.speed = arm.speed.add(goal.subtract(arm.tip).scale(follow * SPRING)).scale(DAMPING);
            double fast = arm.speed.length();
            if (fast > MAX_STEP) {
                arm.speed = arm.speed.scale(MAX_STEP / fast);
            }
            arm.tip = arm.tip.add(arm.speed);
        }
        arm.blend += Mth.clamp(blendTo - arm.blend, -0.12, 0.12);
    }

    private void toRest(Arm arm) {
        // A tentacle that still carries blocks goes back to carrying them, not to resting.
        arm.job = arm.load != null ? Job.CARRY : Job.REST;
        arm.age = 0;
        arm.target = null;
        arm.struckAt = -1;
        arm.delay = 0;
        arm.tipOffset = 0;
        arm.clipNormal = Vec3.ZERO;
        arm.spike = 0;
        arm.thrust = 0;
        arm.aim = Vec3.ZERO;
        // Keep a little of the speed it had, so the next job carries on from the swing it was in
        // instead of starting again from nothing.
        double fast = arm.speed.length();
        if (fast > 0.5) {
            arm.speed = arm.speed.scale(0.5 / fast);
        }
    }

    /**
     * The legs walk: each foot stays planted where it stands until the player has moved too far from
     * it, then lifts and steps to a new spot a little ahead (one leg at a time). In the air they hang.
     */
    private void walk(ServerLevel level, Arm arm) {
        Vec3 wanted = this.footSpot(level, arm);
        if (wanted == null) {
            arm.foot = null;
            arm.step = -1;
            // Each one hangs in a place of its own, or the four swing through each other in the air.
            Vec3 hang = this.mount(arm.index)
                    .add(this.right().scale(arm.side * (arm.upper ? 0.9 : 1.4)))
                    .add(0, arm.upper ? -1.15 : -1.95, 0)
                    .subtract(this.forward().scale(arm.upper ? 0.05 : 0.5));
            this.glide(arm, hang, 0.3, 0);
            arm.claw += (0.45 - arm.claw) * 0.14;
            arm.aim = new Vec3(0, -1, 0);
            return;
        }
        if (arm.step >= 0) {
            double t = (arm.step + 1.0) / STEP_TIME;
            Vec3 was = arm.tip;
            // Lifts off and sets down with no speed at all, and swings over in a smooth arc between.
            arm.tip = arm.stepFrom.lerp(arm.stepTo, smoother(t)).add(0, Math.sin(Math.PI * t) * 0.55, 0);
            arm.speed = arm.tip.subtract(was);
            arm.step++;
            if (t >= 1.0) {
                arm.foot = arm.stepTo;
                arm.step = -1;
                this.sound(arm.foot, SoundEvents.IRON_GOLEM_STEP, 0.3F, 1.7F);
                SpellFx.cloud(level, ParticleTypes.POOF, arm.foot.add(0, 0.05, 0), 2, 0.1, 0.01);
            }
        } else {
            double behind = arm.foot == null ? Double.MAX_VALUE
                    : Math.max(horizontal(arm.foot, wanted), Math.abs(arm.foot.y - wanted.y) * 1.3);
            // Never all legs in the air at once: at most half of them step together.
            int stepping = 0;
            int legs = 0;
            for (Arm other : this.arms) {
                if (other.leg) {
                    legs++;
                    if (other != arm && other.step >= 0) {
                        stepping++;
                    }
                }
            }
            boolean mayStep = stepping < Math.max(1, legs / 2) || behind > STEP_AFTER * 1.8;
            if (behind > STEP_AFTER && mayStep) {
                arm.stepFrom = arm.tip;
                arm.stepTo = wanted;
                arm.step = 0;
            } else if (arm.foot != null) {
                arm.tip = arm.foot;
                arm.speed = Vec3.ZERO;
            }
        }
        arm.blend += Mth.clamp(1.0 - arm.blend, -0.12, 0.12);
        arm.claw += (0.4 - arm.claw) * 0.14;
        // A foot stands on the ground, so its claw points straight down at it.
        arm.aim = new Vec3(0, -1, 0);
    }

    /**
     * Where a foot wants to stand: its own spot in the ring of legs around the player, a bit ahead
     * when moving; null when the ground is too far below.
     */
    @Nullable
    private Vec3 footSpot(ServerLevel level, Arm arm) {
        int legs = Math.max(1, this.legCount());
        Vec3 motion = this.caster.position().subtract(this.caster.xo, this.caster.yo, this.caster.zo);
        // Every leg stands on the side of you it grows out of: shoulder legs a little ahead, hip legs
        // a little behind. Two legs can then never swap sides and walk through each other.
        double ahead = (arm.upper ? 1.0 : -1.0) * (legs >= 3 ? 0.62 : 0.18);
        Vec3 around = this.right().scale(arm.side).add(this.forward().scale(ahead)).normalize();
        double reach = 4.0;
        Vec3 spot = this.caster.position().add(around.scale(LEG_SPREAD))
                .subtract(this.forward().scale(0.15)).add(motion.x * reach, 0, motion.z * reach);
        double ground = SpellTargeting.floorBelow(level, BlockPos.containing(spot.x, this.caster.getY() + 1.5, spot.z));
        if (this.caster.getY() - ground > LEG_DROP || ground - this.caster.getY() > 1.6) {
            return null;
        }
        return new Vec3(spot.x, ground, spot.z);
    }

    /**
     * Who walks and who is free. You pick how many tentacles carry you (Stance); the lowest ones that
     * have nothing else to do take the job, so a tentacle that is holding something keeps holding it.
     */
    private void assignLegs() {
        int wanted = this.climbing || this.folding ? 0 : STANCES[this.stance];
        int slot = 0;
        // From the hips up: the lower tentacles walk first, the shoulder ones stay free longest.
        for (int i = this.arms.length - 1; i >= 0; i--) {
            Arm arm = this.arms[i];
            // Anything with a job of its own keeps it; a tentacle only walks when it is free.
            boolean canWalk = slot < wanted && arm.held == null && arm.load == null && arm.job == Job.REST;
            if (canWalk) {
                arm.leg = true;
                slot++;
            } else if (arm.leg) {
                arm.leg = false;
                arm.foot = null;
                arm.step = -1;
            }
        }
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ---- Poses ----

    private Vec3 forward() {
        return RobotArm.forward(this.caster);
    }

    private Vec3 right() {
        return RobotArm.right(this.caster);
    }

    /** Where arm {@code i} is fixed on the back: two at the shoulders, two at the hips. */
    private Vec3 mount(int i) {
        boolean upper = i < 2;
        double side = i % 2 == 0 ? 1 : -1;
        return this.caster.position().add(0, upper ? 1.35 : 0.75, 0).subtract(this.forward().scale(0.34))
                .add(this.right().scale(side * (upper ? 0.24 : 0.2)));
    }

    /**
     * Resting: every tentacle arcs up out of your back and reaches out in front of you, claws ahead
     * where you can see them. Each one has a lane of its own: the shoulder pair higher and closer in,
     * the hip pair lower and wider, and the right two a little further ahead than the left two, so the
     * four never end up in the same place or look like one mirrored pair.
     */
    private Vec3 restPose(Arm arm) {
        double sway = Math.sin(this.age * 0.05 + arm.index * 1.9);
        double drift = Math.cos(this.age * 0.037 + arm.index * 2.6);
        // Forward counts for much more than up: otherwise the claws hang above your head instead of
        // in front of you.
        double ahead = (arm.upper ? 1.7 : 1.2) + (arm.side > 0 ? 0.2 : 0.0) + 0.12 * sway;
        double high = (arm.upper ? 1.25 : -0.05) + 0.1 * sway;
        double out = (arm.upper ? 1.0 : 1.35) + 0.07 * drift;
        return this.mount(arm.index).add(this.forward().scale(ahead)).add(0, high, 0)
                .add(this.right().scale(arm.side * out)).add(this.idleGlance(arm));
    }

    /**
     * Standing still, a claw now and then turns to look somewhere for a moment: around itself, or back
     * at you over its own shoulder. It fades in and out, so nothing ever snaps.
     */
    private Vec3 idleGlance(Arm arm) {
        if (this.age >= arm.glanceUntil) {
            arm.aim = Vec3.ZERO;
            if (!this.standingStill() || !SpellFx.chance(IDLE_CHANCE)) {
                return Vec3.ZERO;
            }
            arm.glanceFrom = this.age;
            arm.glanceUntil = this.age + 45 + SpellFx.RANDOM.nextInt(50);
            arm.lookAtYou = SpellFx.chance(0.35);
            if (arm.lookAtYou) {
                arm.glance = this.forward().scale(-0.8).add(0, 0.4, 0);
            } else {
                double angle = SpellFx.RANDOM.nextDouble() * Math.PI * 2;
                arm.glance = this.right().scale(Math.cos(angle) * 0.75).add(0, Math.sin(angle) * 0.5, 0);
            }
        }
        // Fades in and out with no speed at either end, so a glance never starts or stops with a tug.
        double fade = smoother(Math.min(this.age - arm.glanceFrom, arm.glanceUntil - this.age) / 14.0);
        // The whole claw turns along with it: looking at you means the side the spikes come out of is
        // the side you see. Turning it is smoothed out on its own (see tickAims).
        arm.aim = arm.lookAtYou ? this.caster.getEyePosition().subtract(arm.tip) : arm.glance;
        return arm.glance.scale(fade);
    }

    /** True when the player is barely moving, so the tentacles have time to look around. */
    private boolean standingStill() {
        Vec3 motion = this.caster.position().subtract(this.caster.xo, this.caster.yo, this.caster.zo);
        return motion.horizontalDistanceSqr() < 0.0016 && !this.blocking && !this.climbing;
    }

    /**
     * Blocking: the four tentacles weave a cross in front of you, right behind the energy shield. Each
     * one takes its own corner of that cross, so no two ever end up in the same spot.
     */
    private Vec3 blockPose(Arm arm) {
        Vec3 eye = this.caster.getEyePosition();
        // Each one takes the corner on its own side of the shield: shoulders high, hips low. Every
        // tentacle stays on the side it grows out of, so their paths never cross in front of you.
        return eye.add(this.forward().scale(arm.upper ? 1.2 : 1.0))
                .add(this.right().scale(arm.side * (arm.upper ? 0.62 : 0.88)))
                .add(0, arm.upper ? 0.42 : -0.55, 0);
    }

    /**
     * Climbing: every tentacle holds its own spot on the wall, in its own quarter around your body and
     * far away from the other three, preferably clamped on an edge or a corner. A grip is only let go
     * when it is out of reach or the block is gone, and only one tentacle reaches for a new one at a
     * time, so the others keep holding you.
     */
    private Vec3 climbPose(ServerLevel level, Arm arm) {
        Vec3 body = this.body();
        if (arm.grip != null && (arm.grip.distanceTo(body) > GRIP_REACH || !this.stillThere(level, arm.grip))) {
            arm.grip = null;
        }
        if (arm.grip == null && Math.floorMod(this.age + arm.index, GRIP_EVERY) == 0) {
            arm.grip = this.findGrip(level, arm, body);
            if (arm.grip != null) {
                arm.gripAge = this.age;
                this.sound(arm.grip, SoundEvents.NETHERITE_BLOCK_HIT, 0.35F, 1.7F);
                SpellFx.cloud(level, ParticleTypes.CRIT, arm.grip, 3, 0.1, 0.03);
            }
        }
        if (arm.grip != null) {
            // A small pull towards the body: the claw looks like it is really carrying you.
            double pull = Math.min(0.08, Math.max(0.0, (this.age - arm.gripAge) * 0.002));
            return arm.grip.add(body.subtract(arm.grip).normalize().scale(pull));
        }
        return body.add(this.sector(arm, 0.0).scale(CLIMB_SPREAD));
    }

    /** The best free spot on the surface for this tentacle: high, wide apart, and on an edge. */
    @Nullable
    private Vec3 findGrip(ServerLevel level, Arm arm, Vec3 body) {
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 15; i++) {
            Vec3 direction = this.sector(arm, (i % 5 - 2) * 0.2).add(0, 0.9 - i / 5 * 0.9, 0);
            if (direction.lengthSqr() < 1.0E-6) {
                continue;
            }
            direction = direction.normalize();
            BlockHitResult hit = level.clip(new ClipContext(body, body.add(direction.scale(GRIP_REACH)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.caster));
            if (hit.getType() == HitResult.Type.MISS) {
                continue;
            }
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            Vec3 point = hit.getLocation().add(normal.scale(0.08));
            double apart = this.apart(arm, point);
            if (apart < GRIP_MIN_APART) {
                continue;
            }
            // Higher is better (that is where you are going), wide apart is better, an edge is best.
            double score = (point.y - body.y) * 1.3 + Math.min(apart, 3.0) + (onEdge(level, hit) ? 1.6 : 0.0);
            if (score > bestScore) {
                bestScore = score;
                best = point;
            }
        }
        return best;
    }

    /** How far the nearest other tentacle's grip is; huge when the others hold nothing. */
    private double apart(Arm arm, Vec3 point) {
        double nearest = Double.MAX_VALUE;
        for (Arm other : this.arms) {
            if (other != arm && other.grip != null) {
                nearest = Math.min(nearest, other.grip.distanceTo(point));
            }
        }
        return nearest;
    }

    /** The direction of this tentacle's own quarter around the surface you hang on. */
    private Vec3 sector(Arm arm, double extra) {
        Vec3 into = Vec3.atLowerCornerOf(this.climbFace.getNormal()).scale(-1);
        Vec3 up = this.climbFace.getAxis().isVertical() ? this.forward() : new Vec3(0, 1, 0);
        Vec3 side = up.cross(into).normalize();
        // "side" always points to your own right, whether you hang on a wall or under a ceiling, so a
        // right tentacle stays right and a left one stays left instead of swapping over.
        if (side.dot(this.right()) < 0) {
            side = side.scale(-1);
        }
        // Its own quarter: the shoulder pair high, the hip pair low, each on the side it grows out of.
        double angle = (arm.upper ? Math.PI / 4 : Math.PI * 0.75) * arm.side + extra;
        return into.scale(0.55).add(up.scale(Math.cos(angle))).add(side.scale(Math.sin(angle))).normalize();
    }

    /** True when the block that was hit has open air beside it: an edge or a corner to clamp on. */
    private static boolean onEdge(ServerLevel level, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        for (Direction side : Direction.values()) {
            if (side.getAxis() == hit.getDirection().getAxis()) {
                continue;
            }
            BlockPos next = pos.relative(side);
            if (level.getBlockState(next).getCollisionShape(level, next).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** True when there is still something solid where this tentacle holds on. */
    private boolean stillThere(ServerLevel level, Vec3 grip) {
        Vec3 body = this.body();
        Vec3 away = grip.subtract(body);
        if (away.lengthSqr() < 1.0E-6) {
            return false;
        }
        Vec3 end = grip.add(away.normalize().scale(0.35));
        BlockHitResult hit = level.clip(new ClipContext(body, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this.caster));
        return hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceTo(grip) < 0.7;
    }

    private Vec3 body() {
        return this.caster.position().add(0, this.caster.getBbHeight() * 0.5, 0);
    }

    /** Blocks in the claw hang out beside you, where you can see them and aim with them. */
    private Vec3 carryPose(Arm arm) {
        Vec3 eye = this.caster.getEyePosition();
        return eye.add(this.forward().scale(1.5)).add(this.right().scale(arm.side * 1.25))
                .add(0, arm.upper ? 0.2 : -0.7, 0);
    }

    /** The tentacle's line: out of the back, bending over, to its tip. */
    private List<Vec3> path(Arm arm) {
        Vec3 mount = this.mount(arm.index);
        Vec3 forward = this.forward();
        Vec3 right = this.right();
        Vec3 c1 = arm.upper
                ? mount.subtract(forward.scale(0.45)).add(0, 0.8, 0).add(right.scale(arm.side * 0.45))
                : mount.subtract(forward.scale(0.35)).add(right.scale(arm.side * 0.8)).add(0, 0.35, 0);
        double reach = mount.distanceTo(arm.tip);
        // The last piece comes in along the way the claw is really looking, so the whole tentacle
        // bends round with it. More points than the bend really needs, for a round line.
        Vec3 c2 = arm.aimShown.lengthSqr() > 1.0E-6
                ? arm.tip.subtract(arm.aimShown.scale(Math.min(1.7, 0.6 + reach * 0.25)))
                : this.elbow(arm, mount);
        return RobotArm.curve(mount, c1, c2, arm.tip, Math.max(18, (int) (reach * 4)));
    }

    private void draw(ServerLevel level) {
        for (Arm arm : this.arms) {
            List<Vec3> path = this.path(arm);
            if (this.unfold < 1.0) {
                // Unfolding and folding: the tentacle grows out of (or shrinks into) its mount.
                Vec3 mount = path.get(0);
                double grown = ease(this.unfold);
                List<Vec3> scaled = new ArrayList<>(path.size());
                for (Vec3 point : path) {
                    scaled.add(mount.add(point.subtract(mount).scale(grown)));
                }
                path = scaled;
            }
            RobotArm.Shape shape = RobotArm.arm(arm.id, path).claw(arm.claw).anchor(this.caster, arm.blend)
                    .holding(arm.held).cut(arm.cut).tipOffset(arm.tipOffset)
                    .tools(arm.spike, arm.thrust).carrying(carried(arm))
                    .lamps(this.rampage > 0 ? ArmPayload.LAMPS_RAGE : ArmPayload.LAMPS_NORMAL);
            if (arm.clipNormal.lengthSqr() > 1.0E-6) {
                shape.clip(arm.clipPoint, arm.clipNormal);
            }
            shape.send(level);
        }
        if (this.rampage > 0 && SpellFx.chance(0.4)) {
            Arm arm = this.arms[SpellFx.RANDOM.nextInt(this.arms.length)];
            SpellFx.at(level, ParticleTypes.SMALL_FLAME, arm.tip);
        }
    }

    /** The blocks in this claw, as the client needs them: where each one sits and what it is. */
    private static List<ArmPayload.Carried> carried(Arm arm) {
        if (arm.load == null) {
            return List.of();
        }
        List<ArmPayload.Carried> blocks = new ArrayList<>(arm.load.size());
        for (TentacleBlocks.Piece piece : arm.load.pieces()) {
            blocks.add(new ArmPayload.Carried(
                    new Vec3(piece.offset().getX(), piece.offset().getY(), piece.offset().getZ()),
                    Block.getId(piece.state())));
        }
        return blocks;
    }

    // ---- Tentacle Strike and Multi-Tentacle ----

    /**
     * A melee hit: a free tentacle lashes out and the hit lands when its claw arrives.
     *
     * @return false when this hit is not taken over (it is then a normal hit)
     */
    boolean meleeStrike(LivingEntity target) {
        if (this.delivering || this.folding || this.unfold < 1.0 || this.climbing) {
            return false;
        }
        // Upper tentacles in turns; the legs join in only when both are busy.
        int[] order = this.nextArm++ % 2 == 0 ? new int[] { 0, 1, 2, 3 } : new int[] { 1, 0, 3, 2 };
        for (int i : order) {
            Arm arm = this.arms[i];
            if (arm.free()) {
                this.strike(arm, target, Hit.MELEE, 0);
                this.sound(arm.tip, SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F, 1.6F);
                return true;
            }
        }
        return false;
    }

    private void strike(Arm arm, LivingEntity target, Hit hit, int delay) {
        arm.job = Job.STRIKE;
        arm.age = 0;
        arm.target = target;
        arm.hit = hit;
        arm.delay = delay;
        arm.struckAt = -1;
    }

    /** Every free tentacle hits the target you aim at (or the nearest enemy in front), in turns. */
    boolean multiStrike(ServerLevel level) {
        double range = ability("multi_tentacle").value("rangeBlocks");
        LivingEntity target = SpellTargeting.aimLiving(this.caster, level, range);
        if (target == null) {
            target = this.nearest(level, range, true);
        }
        if (target == null) {
            SpellTargeting.noTarget(this.caster);
            return false;
        }
        int delay = 0;
        for (Arm arm : this.arms) {
            if (arm.free() || arm.job == Job.STRIKE) {
                this.strike(arm, target, Hit.MULTI, delay);
                delay += MULTI_GAP;
            }
        }
        if (delay == 0) {
            return false;
        }
        this.sound(this.caster.position(), SoundEvents.PISTON_EXTEND, 1.0F, 1.3F);
        this.sound(this.caster.position(), SoundEvents.CHAIN_PLACE, 1.0F, 1.4F);
        return true;
    }

    private void deliver(ServerLevel level, Arm arm, LivingEntity target) {
        Vec3 at = target.getBoundingBox().getCenter();
        switch (arm.hit) {
            case MELEE -> {
                // The normal hit, with everything it brings: enchantments, crits, sweeping.
                this.delivering = true;
                this.caster.attack(target);
                this.delivering = false;
            }
            case MULTI -> this.hurt(target, damageOf("multi_tentacle"), 0.5);
            case RAMPAGE -> this.hurt(target, damageOf("rampage"), 0.3);
        }
        SpellFx.cloud(level, ParticleTypes.CRIT, at, 8, 0.25, 0.3);
        SpellFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at, 6, 0.2);
        this.sound(at, SoundEvents.IRON_GOLEM_ATTACK, 0.6F, 1.4F);
    }

    /** Damage that counts as your attack; hits in quick succession all land. */
    private boolean hurt(LivingEntity target, float damage, double knockback) {
        DamageSource source = this.caster.serverLevel().damageSources().playerAttack(this.caster);
        target.invulnerableTime = 0;
        if (!target.hurt(source, damage)) {
            return false;
        }
        Vec3 away = target.position().subtract(this.caster.position());
        if (knockback > 0.0 && away.horizontalDistanceSqr() > 1.0E-4) {
            target.knockback(knockback, -away.x, -away.z);
        }
        return true;
    }

    /**
     * Every enemy around you worth grabbing, nearest first: monsters, other players you are allowed to
     * hurt, anything that is after you, and whoever hit you last. Peaceful animals are left alone; you
     * grab those by aiming at them.
     */
    private List<LivingEntity> threats(ServerLevel level, double range) {
        Vec3 eye = this.caster.getEyePosition();
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                this.caster.getBoundingBox().inflate(range),
                entity -> SpellTargeting.isTargetable(this.caster, entity))) {
            boolean threat = living instanceof Enemy || living instanceof Player
                    || (living instanceof Mob mob && mob.getTarget() == this.caster)
                    || living == this.caster.getLastHurtByMob();
            Vec3 center = living.getBoundingBox().getCenter();
            if (threat && center.distanceTo(eye) <= range
                    && SpellTargeting.clearPath(level, eye, center, this.caster)) {
                found.add(living);
            }
        }
        found.sort((a, b) -> Double.compare(a.distanceToSqr(this.caster), b.distanceToSqr(this.caster)));
        return found;
    }

    /**
     * The nearest creature to hit: enemies, players you are allowed to hurt, anything after you, or
     * whoever hit you last.
     */
    @Nullable
    private LivingEntity nearest(ServerLevel level, double range, boolean inFront) {
        Vec3 eye = this.caster.getEyePosition();
        Vec3 look = this.caster.getLookAngle();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        AABB box = this.caster.getBoundingBox().inflate(range);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> SpellTargeting.isTargetable(this.caster, entity))) {
            boolean threat = living instanceof Enemy || living instanceof Player
                    || (living instanceof Mob mob && mob.getTarget() == this.caster)
                    || living == this.caster.getLastHurtByMob();
            Vec3 center = living.getBoundingBox().getCenter();
            double distance = center.distanceTo(eye);
            if (!threat || distance > range || (inFront && look.dot(center.subtract(eye).normalize()) < 0.5)
                    || !SpellTargeting.clearPath(level, eye, center, this.caster)) {
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = living;
            }
        }
        return best;
    }

    // ---- Grab and Throw ----

    /**
     * Grabs one creature: the one you aim at, or else the nearest enemy in front of you. One press is
     * one tentacle, so you decide yourself how many you hold; nothing is ever grabbed by itself. Only
     * you let go (crouch + the same key).
     */
    boolean grab(ServerLevel level) {
        double range = ability("grab").value("rangeBlocks");
        Arm arm = this.freeArm();
        if (arm == null) {
            this.caster.displayClientMessage(
                    Component.translatable("octopus." + WelcomeScreenMod.MODID + ".busy"), true);
            return false;
        }
        LivingEntity wanted = SpellTargeting.aimLiving(this.caster, level, range);
        if (wanted == null || this.targeted(wanted)) {
            wanted = null;
            // Not aiming at anything: the nearest enemy no tentacle has yet.
            for (LivingEntity other : this.threats(level, Math.min(range, 12.0))) {
                if (!this.targeted(other)) {
                    wanted = other;
                    break;
                }
            }
        }
        if (wanted == null) {
            SpellTargeting.noTarget(this.caster);
            return false;
        }
        this.reach(arm, wanted);
        this.caster.displayClientMessage(Component.translatable("octopus." + WelcomeScreenMod.MODID + ".grabbing",
                this.busyArms()), true);
        this.sound(arm.tip, SoundEvents.PISTON_EXTEND, 1.0F, 0.8F);
        this.sound(arm.tip, SoundEvents.CHAIN_PLACE, 1.0F, 1.2F);
        return true;
    }

    /** How many tentacles are holding a creature or on their way to one. */
    private int busyArms() {
        int busy = 0;
        for (Arm arm : this.arms) {
            if (arm.held != null || (arm.job == Job.REACH && arm.target != null)) {
                busy++;
            }
        }
        return busy;
    }

    /** How many tentacles could take on a new job right now, legs that may lift off included. */
    private int freeArms() {
        int free = 0;
        for (Arm arm : this.arms) {
            if (arm.free()) {
                free++;
            }
        }
        // Walking on three or four, every leg above the second may lift off for a job.
        return free + Math.max(0, this.legCount() - 2);
    }

    /** This tentacle shoots out to that creature. */
    private void reach(Arm arm, LivingEntity target) {
        arm.job = Job.REACH;
        arm.age = 0;
        arm.target = target;
        arm.struckAt = -1;
    }

    /** True when a tentacle is already reaching for this creature or holding it. */
    private boolean targeted(LivingEntity entity) {
        for (Arm arm : this.arms) {
            if (arm.held == entity || (arm.job == Job.REACH && arm.target == entity)) {
                return true;
            }
        }
        return false;
    }

    /** Crouch + the grab key: lets go of everything the tentacles hold. */
    boolean letGoAll() {
        if (!this.isHolding()) {
            return false;
        }
        this.letGo();
        this.sound(this.caster.position(), SoundEvents.PISTON_CONTRACT, 0.9F, 1.2F);
        this.caster.displayClientMessage(
                Component.translatable("octopus." + WelcomeScreenMod.MODID + ".let_go"), true);
        return true;
    }

    /**
     * A tentacle that can take on a job: a free one first (the shoulders before the hips). Walking on
     * three or four, one leg lifts off to do the job, as long as two keep carrying you.
     */
    @Nullable
    private Arm freeArm() {
        for (int i : new int[] { 0, 1, 2, 3 }) {
            if (this.arms[i].free()) {
                return this.arms[i];
            }
        }
        if (this.legCount() > 2) {
            for (int i = this.arms.length - 1; i >= 0; i--) {
                Arm arm = this.arms[i];
                if (arm.leg && arm.job == Job.REST) {
                    arm.leg = false;
                    arm.foot = null;
                    arm.step = -1;
                    return arm;
                }
            }
        }
        return null;
    }

    private void seize(ServerLevel level, Arm arm, LivingEntity target) {
        if (target instanceof Mob mob && !HeldMobs.hold(mob)) {
            this.toRest(arm);
            return;
        }
        arm.held = target;
        arm.holdTicks = 0;
        arm.holdDistance = Mth.clamp(this.caster.getEyePosition().distanceTo(target.getBoundingBox().getCenter()),
                HOLD_DISTANCE, 10.0);
        arm.job = Job.HOLD;
        arm.age = 0;
        arm.target = null;
        PacketDistributor.sendToPlayer(this.caster, new GrabStatePayload(true, this.hasLoad()));
        this.caster.displayClientMessage(Component.translatable("octopus." + WelcomeScreenMod.MODID + ".grab.hint"),
                true);
        Vec3 at = target.getBoundingBox().getCenter();
        SpellFx.cloud(level, ParticleTypes.CRIT, at, 14, 0.3, 0.3);
        this.sound(at, SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.8F);
        this.sound(at, SoundEvents.CHAIN_HIT, 1.0F, 0.8F);
    }

    void requestThrow() {
        this.throwRequested = true;
    }

    /**
     * Held creatures are whipped along with where you look, hard enough to smash them into walls and
     * into the ground. A tentacle never lets go by itself: only when the creature is gone, or when you
     * let go, throw, or fold the arms in.
     */
    private void tickHold(ServerLevel level) {
        boolean before = this.hasThrowable();
        if (this.throwRequested) {
            this.throwLoads(level);
        }
        for (Arm arm : this.arms) {
            LivingEntity target = arm.held;
            if (target == null) {
                continue;
            }
            arm.holdTicks++;
            // Only a creature that is gone ends a hold; a tentacle never lets go by itself.
            if (!target.isAlive() || target.isRemoved() || target.level() != level) {
                this.letGo(arm);
                continue;
            }
            if (this.throwRequested) {
                this.fling(level, arm, target);
                continue;
            }
            if (this.heldSlam > 0) {
                this.slamHeld(level, arm, target);
                continue;
            }
            arm.holdDistance += (HOLD_DISTANCE - arm.holdDistance) * 0.08;
            // Each tentacle holds its catch in its own spot, so several never sit inside each other.
            Vec3 goal = this.caster.getEyePosition().add(this.caster.getLookAngle().scale(arm.holdDistance))
                    .add(this.right().scale(arm.side * 0.85)).add(0, arm.upper ? 0.35 : -0.35, 0);
            Vec3 center = target.getBoundingBox().getCenter();
            Vec3 wanted = goal.subtract(center).scale(FOLLOW);
            if (wanted.length() > MAX_SPEED) {
                wanted = wanted.normalize().scale(MAX_SPEED);
            }
            // Swinging your view whips it along; running it into a wall or the ground hurts it.
            // Creatures and players are dragged exactly the same way.
            double smashSpeed = ability("grab").value("smashSpeed");
            if (arm.crashPause > 0) {
                arm.crashPause--;
            }
            Vec3 blocked = this.drag(target, wanted);
            if (arm.crashPause <= 0 && wanted.length() > smashSpeed && blocked.length() > smashSpeed * 0.6) {
                crash(level, this.caster, target, blocked.normalize().scale(wanted.length()));
                arm.crashPause = CRASH_PAUSE;
            }
            target.resetFallDistance();
            if (arm.holdTicks % 12 == 6) {
                this.sound(center, SoundEvents.CHAIN_STEP, 0.6F, 0.8F);
            }
        }
        this.throwRequested = false;
        if (before && !this.hasThrowable() && !this.caster.hasDisconnected()) {
            PacketDistributor.sendToPlayer(this.caster, new GrabStatePayload(false, this.hasLoad()));
        }
    }

    /**
     * Ground Slam with full claws: every tentacle drives what it holds straight into the ground (or
     * into the wall right behind it) and lets the impact do the damage.
     */
    private void slamHeld(ServerLevel level, Arm arm, LivingEntity target) {
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 step = new Vec3(0, -1.4, 0);
        Vec3 blocked = this.drag(target, step);
        if (blocked.length() > step.length() * 0.35 && arm.crashPause <= 0) {
            this.hurt(target, (float) ability("ground_slam").value("heldSlamDamage"), 0.0);
            smashFx(level, target, step);
            arm.crashPause = CRASH_PAUSE;
            this.heldSlam = Math.min(this.heldSlam, 1);
        }
        arm.tip = center.add(0, target.getBbHeight() * 0.5 + 0.2, 0);
        arm.claw = target.getBbWidth() * 0.5 + 0.25;
    }

    /** Counts down the ground smash of everything the tentacles hold. */
    private void tickHeldSlam() {
        if (this.heldSlam > 0) {
            this.heldSlam--;
            if (this.heldSlam == 0) {
                this.sound(this.caster.position(), SoundEvents.PISTON_CONTRACT, 0.8F, 1.1F);
            }
        }
    }

    /**
     * Moves what a tentacle holds by {@code wanted}, through the world, so it really bumps into blocks.
     * A held player is pushed the same way and then told where they now are, so their own client
     * cannot walk out of the claw.
     *
     * @return the part of the move that a block stopped
     */
    private Vec3 drag(LivingEntity target, Vec3 wanted) {
        Vec3 was = target.position();
        target.move(MoverType.SELF, wanted);
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        if (target instanceof ServerPlayer player) {
            player.connection.teleport(target.getX(), target.getY(), target.getZ(), player.getYRot(),
                    player.getXRot());
            player.connection.aboveGroundTickCount = 0;
        }
        return wanted.subtract(target.position().subtract(was));
    }

    /** Lets go of everything held. */
    void letGo() {
        for (Arm arm : this.arms) {
            this.letGo(arm);
        }
    }

    private void letGo(Arm arm) {
        LivingEntity target = arm.held;
        if (target == null) {
            return;
        }
        arm.held = null;
        if (target instanceof Mob mob) {
            HeldMobs.release(mob);
        }
        this.toRest(arm);
        if (!this.caster.hasDisconnected() && !this.hasThrowable()) {
            PacketDistributor.sendToPlayer(this.caster, new GrabStatePayload(false, this.hasLoad()));
        }
    }

    private void fling(ServerLevel level, Arm arm, LivingEntity target) {
        this.letGo(arm);
        target.setDeltaMovement(this.caster.getLookAngle().scale(THROW_SPEED).add(0, 0.25, 0));
        target.hasImpulse = true;
        target.hurtMarked = true;
        Vec3 center = target.getBoundingBox().getCenter();
        SpellFx.cloud(level, ParticleTypes.CLOUD, center, 12, 0.3, 0.15);
        this.sound(center, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
        this.sound(center, SoundEvents.PISTON_EXTEND, 1.0F, 1.4F);
        SpellCasting.start(level, thrown(this.caster, target));
    }

    /** A creature hit a block hard: damage by speed, and a crunch of that block's pieces. */
    private static void crash(ServerLevel level, ServerPlayer caster, LivingEntity target, Vec3 blocked) {
        double speed = blocked.length();
        float damage = (float) Math.min(damageOf("grab"), 2.0 + speed * 4.0);
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().playerAttack(caster), damage);
        smashFx(level, target, blocked);
    }

    /** The dust, the sparks and the bang of a body hitting a block hard. */
    private static void smashFx(ServerLevel level, LivingEntity target, Vec3 blocked) {
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 contact = center.add(blocked.normalize().scale(target.getBbWidth() / 2 + 0.3));
        BlockState hit = level.getBlockState(BlockPos.containing(contact));
        if (!hit.isAir()) {
            SpellFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, hit), contact.x, contact.y, contact.z,
                    30, 0.3, 0.3, 0.3, 0.3);
        }
        SpellFx.cloud(level, ParticleTypes.CRIT, center, 12, 0.3, 0.4);
        SpellFx.cloud(level, ParticleTypes.POOF, contact, 6, 0.2, 0.05);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.PLAYERS,
                0.8F, 0.8F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4F, 1.3F);
    }

    /** After a throw: the first hard crash into a wall or the ground still hurts. */
    private static SpellEffect thrown(ServerPlayer caster, LivingEntity target) {
        double[] lastSpeed = { THROW_SPEED };
        return (level, age) -> {
            if (!target.isAlive() || target.isRemoved() || age >= THROWN_TRACK) {
                return false;
            }
            Vec3 velocity = target.getDeltaMovement();
            // Skip the first ticks: the collision flags still describe the moment it was held.
            if (age >= 2 && (target.horizontalCollision || target.verticalCollision)
                    && lastSpeed[0] > ability("grab").value("smashSpeed")) {
                Vec3 direction = velocity.lengthSqr() > 1.0E-4 ? velocity.normalize() : new Vec3(0, -1, 0);
                crash(level, caster, target, direction.scale(lastSpeed[0]));
                return false;
            }
            if (age % 2 == 0) {
                SpellFx.at(level, ParticleTypes.CLOUD, target.getBoundingBox().getCenter());
            }
            lastSpeed[0] = velocity.length();
            return true;
        };
    }

    // ---- Tentacle Dash ----

    /**
     * The tentacles push off the ground and launch you the way you are going: forward, sideways or
     * backwards, whichever way you are pressing. Standing still, they throw you where you look.
     */
    boolean dash(ServerLevel level) {
        Vec3 input = this.forward().scale(this.caster.zza).subtract(this.right().scale(this.caster.xxa));
        Vec3 way = input.lengthSqr() > 1.0E-4 ? input.normalize() : this.caster.getLookAngle();
        this.caster.setDeltaMovement(way.scale(ability("dash").value("speed")).add(0, DASH_LIFT, 0));
        this.caster.hurtMarked = true;
        OctopusArms.safeFall(this.caster, 80);
        Vec3 flat = new Vec3(way.x, 0, way.z);
        Vec3 push = flat.lengthSqr() < 1.0E-4 ? this.forward() : flat.normalize();
        Vec3 behind = this.caster.position().subtract(push.scale(0.9));
        double ground = SpellTargeting.floorBelow(level,
                BlockPos.containing(behind.x, this.caster.getY() + 0.5, behind.z));
        for (Arm arm : this.arms) {
            if (arm.free()) {
                arm.job = Job.PLANT;
                arm.age = 0;
                arm.spot = new Vec3(behind.x, Math.max(ground, this.caster.getY() - 2.5), behind.z)
                        .add(this.right().scale(arm.side * (arm.upper ? 0.5 : 0.9)));
            }
        }
        SpellFx.cloud(level, ParticleTypes.POOF, this.caster.position(), 12, 0.4, 0.05);
        SpellFx.cloud(level, ParticleTypes.CLOUD, this.caster.position(), 6, 0.3, 0.1);
        this.sound(this.caster.position(), SoundEvents.BREEZE_JUMP, 1.0F, 0.8F);
        this.sound(this.caster.position(), SoundEvents.PISTON_EXTEND, 1.0F, 0.6F);
        return true;
    }

    /**
     * Running on the tentacles: they take far longer strides than your own legs, so running on them is
     * quicker than running on foot. Only while they really carry you.
     */
    private void tickLegSpeed() {
        boolean running = this.legCount() > 0 && this.caster.isSprinting() && !this.climbing;
        if (running == this.legsRunning) {
            return;
        }
        this.legsRunning = running;
        OctopusArms.modifier(this.caster, Attributes.MOVEMENT_SPEED, OctopusArms.LEG_RUN_ID, LEG_RUN_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, running);
    }

    // ---- Stance ----

    /**
     * Walk on your own feet, or on 2, 3 or 4 tentacles. Every tentacle that is not walking is free for
     * grabbing, fighting and building, so on your feet all four are free. Crouching goes back a step
     * instead of forward.
     */
    boolean cycleStance(boolean back) {
        this.stance = Math.floorMod(this.stance + (back ? -1 : 1), STANCES.length);
        int legs = STANCES[this.stance];
        this.sound(this.caster.position(), legs == 0 ? SoundEvents.PISTON_CONTRACT : SoundEvents.PISTON_EXTEND,
                0.9F, legs == 0 ? 1.2F : 0.8F);
        this.caster.displayClientMessage(
                Component.translatable("octopus." + WelcomeScreenMod.MODID + (legs == 0 ? ".stance.feet"
                        : ".stance.legs"), legs, this.arms.length - legs),
                true);
        OctopusArms.sync(this.caster);
        return true;
    }

    // ---- Blocks and building ----

    /**
     * The blocks key: every press takes another block (or a whole cluster while crouching), so all four
     * tentacles can carry something at once. Setting down is the right mouse button; only when every
     * tentacle is full does the key set the first load down, so you are never stuck.
     */
    boolean blockAction(ServerLevel level, boolean cluster) {
        Arm arm = this.freeArm();
        if (arm == null) {
            return this.placeBlocks(level);
        }
        double range = BUILD_RANGE;
        TentacleBlocks.Load load = TentacleBlocks.pickUp(level, this.caster, cluster, BUILD_CLUSTER, range);
        if (load == null) {
            this.caster.displayClientMessage(
                    Component.translatable("octopus." + WelcomeScreenMod.MODID + ".build.nothing"), true);
            return false;
        }
        arm.load = load;
        arm.job = Job.CARRY;
        arm.age = 0;
        this.caster.displayClientMessage(
                Component.translatable("octopus." + WelcomeScreenMod.MODID + ".build.taken", load.size()), true);
        this.afterCarryChange();
        return true;
    }

    /**
     * Sets down what the first carrying tentacle holds, in exactly the shape it was taken. Blocks that do
     * not fit stay in the claw, so nothing is ever lost.
     */
    boolean placeBlocks(ServerLevel level) {
        double range = BUILD_RANGE;
        for (Arm arm : this.arms) {
            if (arm.load == null) {
                continue;
            }
            TentacleBlocks.Result result = TentacleBlocks.place(level, this.caster, arm.load, range);
            if (result.placed() <= 0) {
                this.caster.displayClientMessage(
                        Component.translatable("octopus." + WelcomeScreenMod.MODID + ".build.no_room"), true);
                return false;
            }
            arm.load = result.left();
            if (arm.load == null) {
                this.toRest(arm);
            }
            this.caster.displayClientMessage(Component.translatable(
                    "octopus." + WelcomeScreenMod.MODID + ".build.placed", result.placed()), true);
            this.afterCarryChange();
            return true;
        }
        this.caster.displayClientMessage(
                Component.translatable("octopus." + WelcomeScreenMod.MODID + ".build.empty"), true);
        return false;
    }

    /** Throws away everything the tentacles carry in blocks, at whatever you are looking at. */
    private boolean throwLoads(ServerLevel level) {
        boolean any = false;
        for (Arm arm : this.arms) {
            if (arm.load == null) {
                continue;
            }
            TentacleBlocks.hurl(level, this.caster, arm.load, arm.tip, this.caster.getLookAngle(),
                    BUILD_DAMAGE, TentacleBlocks.THROW_SPEED);
            arm.load = null;
            this.toRest(arm);
            any = true;
        }
        if (any) {
            this.afterCarryChange();
        }
        return any;
    }

    /** The arms fold in or stop: every load is set down instead of disappearing. */
    private void dropLoads(ServerLevel level) {
        for (Arm arm : this.arms) {
            if (arm.load != null) {
                TentacleBlocks.drop(level, this.caster, arm.load, arm.tip);
                arm.load = null;
            }
        }
    }

    /** Keeps the client's "the attack button throws" state right after picking up or letting go. */
    private void afterCarryChange() {
        if (!this.caster.hasDisconnected()) {
            PacketDistributor.sendToPlayer(this.caster, new GrabStatePayload(this.hasThrowable(), this.hasLoad()));
        }
    }

    // ---- Ground Strike ----

    /**
     * The Ground Strike key. Looking at a creature marks it, up to one mark for every free tentacle;
     * pressing while you aim at nothing (or with every tentacle already spoken for) launches the
     * strike at everything you marked. Crouching drops the marks again.
     *
     * @return true only when the strike really launches, so marking never starts the cooldown
     */
    boolean groundStrike(ServerLevel level) {
        double range = ability("ground_strike").value("rangeBlocks");
        int room = this.freeArms();
        LivingEntity aimed = SpellTargeting.aimLiving(this.caster, level, range);
        if (aimed != null && !this.marks.contains(aimed) && this.marks.size() < room) {
            this.marks.add(aimed);
            // Marked creatures light up, so you can see who is on the list, walls or no walls.
            aimed.setGlowingTag(true);
            Vec3 at = aimed.getBoundingBox().getCenter();
            SpellFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 12, 0.35, 0.1);
            this.sound(at, SoundEvents.NOTE_BLOCK_BELL.value(), 0.6F, 1.9F);
            this.caster.displayClientMessage(Component.translatable(
                    "octopus." + WelcomeScreenMod.MODID + ".strike.marked", this.marks.size(), room), true);
            return false;
        }
        if (this.marks.isEmpty()) {
            this.caster.displayClientMessage(Component.translatable("octopus." + WelcomeScreenMod.MODID
                    + (room == 0 ? ".busy" : ".strike.none")), true);
            return false;
        }
        return this.launchStrike(level);
    }

    /** Crouch + the Ground Strike key: forget every mark, without a cooldown. */
    boolean clearMarks() {
        if (this.marks.isEmpty()) {
            return false;
        }
        this.dropMarks();
        this.sound(this.caster.position(), SoundEvents.NOTE_BLOCK_BASS.value(), 0.6F, 0.8F);
        this.caster.displayClientMessage(
                Component.translatable("octopus." + WelcomeScreenMod.MODID + ".strike.cleared"), true);
        return true;
    }

    /** One tentacle per marked creature dives into the ground at your feet. */
    private boolean launchStrike(ServerLevel level) {
        int launched = 0;
        for (LivingEntity target : this.marks) {
            Arm arm = this.freeArm();
            if (arm == null) {
                break;
            }
            this.startDig(level, arm, target);
            launched++;
        }
        this.dropMarks();
        if (launched == 0) {
            this.caster.displayClientMessage(
                    Component.translatable("octopus." + WelcomeScreenMod.MODID + ".busy"), true);
            return false;
        }
        this.sound(this.caster.position(), SoundEvents.PISTON_EXTEND, 1.0F, 0.6F);
        this.sound(this.caster.position(), SoundEvents.IRON_GOLEM_ATTACK, 0.9F, 1.4F);
        this.caster.displayClientMessage(Component.translatable(
                "octopus." + WelcomeScreenMod.MODID + ".strike.launched", launched), true);
        return true;
    }

    /** This tentacle starts digging: it picks its own hole in the ground beside you. */
    private void startDig(ServerLevel level, Arm arm, LivingEntity target) {
        arm.job = Job.BURROW;
        arm.age = 0;
        arm.dig = Dig.SHOW;
        arm.target = target;
        arm.risen = 0;
        arm.digHit = false;
        arm.tipOffset = 0;
        arm.spike = 0;
        arm.thrust = 0;
        Vec3 spot = this.caster.position().add(this.right().scale(arm.side * 0.9))
                .add(this.forward().scale(arm.upper ? 1.3 : 0.6));
        double ground = SpellTargeting.floorBelow(level,
                BlockPos.containing(spot.x, this.caster.getY() + 1.0, spot.z));
        arm.spot = new Vec3(spot.x, ground + 0.05, spot.z);
        arm.digTravel = TRAVEL_MIN + (int) (this.caster.distanceTo(target) * TRAVEL_PER_BLOCK);
    }

    /**
     * A digging tentacle, step by step: to its hole, into the ground, along under it, up out of the
     * ground at the creature, back down, and back out of the hole.
     */
    private void tickDig(ServerLevel level, Arm arm) {
        LivingEntity target = arm.target;
        boolean gone = target == null || !target.isAlive() || target.isRemoved() || target.level() != level;
        if (gone && arm.dig == Dig.SHOW) {
            // It never even went into the ground: simply put the tentacle back.
            this.toRest(arm);
            return;
        }
        if (gone && arm.dig.ordinal() < Dig.DOWN.ordinal()) {
            // Whatever it was after is gone: come straight back instead of hanging in the ground.
            arm.dig = arm.risen > 0 ? Dig.DOWN : Dig.BACK;
            arm.age = 0;
        }
        switch (arm.dig) {
            case SHOW -> {
                // First up in front of you, claw open and the spikes sliding out of the middle of it,
                // so you and whatever is in front of you see the strike coming.
                Vec3 show = this.caster.getEyePosition().add(this.forward().scale(2.0))
                        .add(this.right().scale(arm.side * (arm.upper ? 0.5 : 0.95)))
                        .add(0, arm.upper ? 0.3 : -0.35, 0);
                arm.claw = 0.6;
                arm.spike = Math.min(1.0, arm.spike + 1.0 / SHOW_TIME);
                // Turned towards you, so you see the spikes come out of the front of the claw.
                arm.aim = this.caster.getEyePosition().subtract(arm.tip);
                this.glide(arm, show, 0.55, 0.9);
                if (arm.age == 1) {
                    this.sound(arm.tip, SoundEvents.NETHERITE_BLOCK_PLACE, 0.8F, 1.5F);
                }
                if (arm.age % 3 == 0) {
                    SpellFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, arm.tip, 3, 0.15, 0.02);
                }
                if (arm.age >= SHOW_TIME) {
                    arm.dig = Dig.TO_HOLE;
                    arm.age = 0;
                }
            }
            case TO_HOLE -> {
                arm.claw = 0.05;
                arm.spike = Math.min(1.0, arm.spike + 1.0 / DIVE_TIME);
                this.glide(arm, arm.spot, 0.55, 1.0);
                // Once it is really at the hole, or after far too long (no ground under you at all).
                if (arm.tip.distanceTo(arm.spot) < 0.5 || arm.age > DIVE_TIME * 4) {
                    arm.dig = Dig.SINK;
                    arm.age = 0;
                    arm.clipPoint = arm.spot;
                    arm.clipNormal = new Vec3(0, -1, 0);
                    this.groundBurst(level, arm.spot, 18);
                    this.sound(arm.spot, SoundEvents.NETHERITE_BLOCK_BREAK, 1.0F, 0.6F);
                }
            }
            case SINK -> {
                arm.tip = arm.tip.add(0, -SINK_SPEED, 0);
                arm.tipOffset += SINK_SPEED;
                if (arm.tipOffset >= SINK_DEPTH) {
                    arm.dig = Dig.UNDER;
                    arm.age = 0;
                }
            }
            case UNDER -> {
                // Everything of it is under the ground now; only the piece into the hole shows.
                arm.tip = arm.spot.add(0, -SINK_DEPTH, 0);
                arm.tipOffset = SINK_DEPTH;
                if (!gone && this.age % 2 == 0) {
                    this.rumble(level, arm, target);
                }
                if (arm.age >= arm.digTravel && !gone) {
                    Vec3 at = target.position();
                    arm.digAt = at;
                    arm.digGround = SpellTargeting.floorBelow(level,
                            BlockPos.containing(at.x, at.y + 1.0, at.z));
                    arm.dig = Dig.UP;
                    arm.age = 0;
                    this.groundBurst(level, new Vec3(at.x, arm.digGround, at.z), 30);
                    this.sound(at, SoundEvents.NETHERITE_BLOCK_BREAK, 1.2F, 0.5F);
                }
            }
            case UP -> {
                double top = gone ? RISE_ABOVE
                        : target.getBoundingBox().getCenter().y - arm.digGround + RISE_ABOVE;
                top = Math.max(1.4, top);
                // Shoots up and eases into its highest point instead of stopping dead.
                arm.risen += Math.min(RISE_SPEED, Math.max(0.35, (top - arm.risen) * 0.55));
                if (!arm.digHit && !gone && arm.risen >= top - RISE_ABOVE) {
                    arm.digHit = true;
                    this.spikeHit(level, arm, target);
                }
                if (arm.risen >= top) {
                    arm.risen = top;
                    arm.dig = Dig.DOWN;
                    arm.age = 0;
                }
            }
            case DOWN -> {
                if (arm.age >= SPIKE_HOLD) {
                    // Slides back into the ground, slowing down as it goes.
                    arm.risen -= Math.max(SPIKE_SINK * 0.3, arm.risen * 0.35);
                    if (arm.risen <= 0) {
                        arm.risen = 0;
                        arm.dig = Dig.BACK;
                        arm.age = 0;
                    }
                }
            }
            case BACK -> {
                arm.tipOffset -= SINK_SPEED;
                arm.spike = Math.max(0.0, arm.spike - 0.12);
                arm.tip = arm.spot.add(0, -Math.max(0.0, arm.tipOffset), 0);
                if (arm.tipOffset <= 0) {
                    this.toRest(arm);
                }
            }
        }
        this.drawSpike(level, arm);
    }

    /** The ground shakes along the way while a tentacle travels underneath it. */
    private void rumble(ServerLevel level, Arm arm, LivingEntity target) {
        double t = Math.min(1.0, (arm.age + 1.0) / Math.max(1, arm.digTravel));
        Vec3 at = arm.spot.lerp(target.position(), t);
        double ground = SpellTargeting.floorBelow(level, BlockPos.containing(at.x, at.y + 1.0, at.z));
        this.groundBurst(level, new Vec3(at.x, ground, at.z), 4);
    }

    /** Dust and broken-block bits out of the ground at {@code at}. */
    private void groundBurst(ServerLevel level, Vec3 at, int count) {
        BlockState ground = level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (!ground.isAir()) {
            SpellFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z,
                    count, 0.35, 0.15, 0.35, 0.25);
        }
        SpellFx.cloud(level, ParticleTypes.POOF, at.add(0, 0.15, 0), Math.max(2, count / 4), 0.3, 0.05);
    }

    /** The spike bursts out under the creature and throws it up. */
    private void spikeHit(ServerLevel level, Arm arm, LivingEntity target) {
        this.hurt(target, damageOf("ground_strike"), 0.0);
        double up = ability("ground_strike").value("knockUp");
        target.setDeltaMovement(target.getDeltaMovement().x, up, target.getDeltaMovement().z);
        target.hurtMarked = true;
        target.resetFallDistance();
        Vec3 at = target.getBoundingBox().getCenter();
        SpellFx.cloud(level, ParticleTypes.CRIT, at, 18, 0.4, 0.4);
        SpellFx.shockwave(level, ParticleTypes.LARGE_SMOKE, new Vec3(at.x, arm.digGround + 0.15, at.z), 24, 0.35);
        this.sound(at, SoundEvents.ANVIL_LAND, 0.9F, 1.3F);
        this.sound(at, SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.7F);
    }

    /** The part that stands out of the ground, drawn as an arm of its own. */
    private void drawSpike(ServerLevel level, Arm arm) {
        if (arm.risen <= 0.01) {
            if (arm.farId >= 0) {
                RobotArm.remove(level, arm.farId);
                arm.farId = -1;
            }
            return;
        }
        if (arm.farId < 0) {
            arm.farId = RobotArm.newId();
        }
        Vec3 bottom = new Vec3(arm.digAt.x, arm.digGround - SPIKE_BURIED, arm.digAt.z);
        Vec3 top = bottom.add(0, SPIKE_BURIED + arm.risen, 0);
        RobotArm.arm(arm.farId, List.of(bottom, top)).claw(0.3).tools(1.0, 0.0)
                .clip(new Vec3(arm.digAt.x, arm.digGround, arm.digAt.z), new Vec3(0, -1, 0)).send(level);
    }

    /** Lets every mark go again and takes the glow back off them. */
    private void dropMarks() {
        for (LivingEntity target : this.marks) {
            target.setGlowingTag(false);
        }
        this.marks.clear();
    }

    /** Marked creatures glow; one that dies or walks out of range is forgotten. */
    private void tickMarks(ServerLevel level) {
        if (this.marks.isEmpty()) {
            return;
        }
        double range = ability("ground_strike").value("rangeBlocks") + 4.0;
        this.marks.removeIf(target -> {
            if (target.isAlive() && !target.isRemoved() && target.level() == level
                    && target.distanceTo(this.caster) <= range) {
                return false;
            }
            target.setGlowingTag(false);
            return true;
        });
        if (this.age % 4 != 0) {
            return;
        }
        for (LivingEntity target : this.marks) {
            Vec3 at = target.getBoundingBox().getCenter();
            SpellFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 2, target.getBbWidth() * 0.5 + 0.2, 0.01);
        }
    }

    // ---- Block ----

    void setBlocking(boolean on) {
        on = on && !this.folding;
        if (on == this.blocking) {
            return;
        }
        this.blocking = on;
        OctopusArms.modifier(this.caster, Attributes.MOVEMENT_SPEED, OctopusArms.BLOCKING_ID, -0.4,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, on);
        this.sound(this.caster.position(), on ? SoundEvents.SHIELD_BLOCK : SoundEvents.IRON_TRAPDOOR_OPEN, 0.7F,
                on ? 1.4F : 0.9F);
    }

    /** The energy shield in front of you: flares open while you block, and fades when you stop. */
    private void tickShield(ServerLevel level) {
        double wanted = this.isBlocking() ? 1.0 : 0.0;
        this.shieldOpen += Mth.clamp(wanted - this.shieldOpen, -1.0 / SHIELD_TIME, 1.0 / SHIELD_TIME);
        if (this.shieldOpen <= 0.001) {
            if (this.shieldId >= 0) {
                RobotArm.removePortal(level, this.shieldId);
                this.shieldId = -1;
            }
            return;
        }
        if (this.shieldId < 0) {
            this.shieldId = RobotArm.newId();
        }
        Vec3 look = this.caster.getLookAngle();
        Vec3 center = this.caster.getEyePosition().add(look.scale(1.05)).add(0, -0.15, 0);
        RobotArm.shield(level, this.shieldId, center, look, SHIELD_SIZE, smoother(this.shieldOpen));
    }

    /** A hit was caught: sparks and a clang on the tentacles. */
    void blocked(ServerLevel level, Vec3 from) {
        this.lastBlocked = this.age;
        Vec3 eye = this.caster.getEyePosition();
        Vec3 at = eye.add(from.subtract(eye).normalize().scale(1.05));
        SpellFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at, 14, 0.25);
        SpellFx.cloud(level, ParticleTypes.CRIT, at, 6, 0.2, 0.2);
        this.sound(at, SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F);
        this.sound(at, SoundEvents.ANVIL_LAND, 0.5F, 1.7F);
    }

    // ---- Wall Climb ----

    /** @param face the side of the surface the tentacles hold on to (its normal) */
    void setClimbing(boolean on, Direction face) {
        on = on && !this.folding;
        if (on && !this.climbing) {
            this.sound(this.caster.position(), SoundEvents.CHAIN_PLACE, 0.8F, 1.1F);
        }
        this.climbing = on;
        this.climbFace = face;
    }

    // ---- Ground Slam ----

    /**
     * The Ground Slam key. While the tentacles hold creatures, every one of them drives its catch
     * straight into the ground; with empty claws it is the slam around you. Crouching always gives the
     * slam around you, so you can use it without letting your catch go.
     */
    boolean heavy(ServerLevel level, boolean areaOnly) {
        if (this.isHolding() && !areaOnly) {
            this.heldSlam = HELD_SLAM_TIME;
            for (Arm arm : this.arms) {
                if (arm.held != null) {
                    arm.crashPause = 0;
                }
            }
            SpellFx.cloud(level, ParticleTypes.CRIT, this.caster.position().add(0, 1.0, 0), 14, 0.5, 0.3);
            this.sound(this.caster.position(), SoundEvents.PISTON_EXTEND, 1.2F, 0.5F);
            this.sound(this.caster.position(), SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.7F);
            return true;
        }
        return this.groundSlam(level);
    }

    /** On the ground: the tentacles rise and smash down around you. In the air: you dive down first. */
    private boolean groundSlam(ServerLevel level) {
        if (this.slam != Slam.NONE) {
            return false;
        }
        this.airSlam = !this.caster.onGround();
        this.slam = this.airSlam ? Slam.DROP : Slam.RISE;
        this.slamAge = 0;
        if (this.airSlam) {
            Vec3 motion = this.caster.getDeltaMovement();
            this.caster.setDeltaMovement(motion.x * 0.3, -AIR_DROP, motion.z * 0.3);
            this.caster.hurtMarked = true;
            OctopusArms.safeFall(this.caster, MAX_DROP + 20);
        }
        for (Arm arm : this.arms) {
            if (arm.free()) {
                arm.job = Job.RISE;
                arm.age = 0;
            }
        }
        this.sound(this.caster.position(), SoundEvents.PISTON_EXTEND, 1.0F, 0.5F);
        this.sound(this.caster.position(), SoundEvents.WARDEN_ATTACK_IMPACT, 0.6F, 1.4F);
        return true;
    }

    private void tickSlam(ServerLevel level) {
        if (this.slam == Slam.NONE) {
            return;
        }
        this.slamAge++;
        switch (this.slam) {
            case RISE -> {
                if (this.slamAge >= RISE_TIME) {
                    this.smashDown(level);
                }
            }
            case DROP -> {
                if (this.caster.onGround() || this.slamAge > MAX_DROP) {
                    this.smashDown(level);
                }
            }
            case SMASH -> {
                if (this.slamAge >= 3) {
                    this.impact(level);
                    this.slam = Slam.NONE;
                }
            }
            default -> {
            }
        }
    }

    private void smashDown(ServerLevel level) {
        this.slam = Slam.SMASH;
        this.slamAge = 0;
        double radius = this.airSlam ? 2.8 : 2.3;
        for (Arm arm : this.arms) {
            if (arm.job != Job.RISE && !arm.free()) {
                continue;
            }
            double angle = Math.toRadians(this.caster.getYRot()) + Math.PI / 4 + arm.index * Math.PI / 2;
            Vec3 spot = this.caster.position().add(-Math.sin(angle) * radius, 0, Math.cos(angle) * radius);
            double ground = SpellTargeting.floorBelow(level,
                    BlockPos.containing(spot.x, this.caster.getY() + 1.0, spot.z));
            arm.job = Job.SMASH;
            arm.age = 0;
            arm.spot = new Vec3(spot.x, Math.max(ground, this.caster.getY() - 3.0), spot.z);
        }
        this.sound(this.caster.position(), SoundEvents.PISTON_EXTEND, 1.2F, 0.4F);
    }

    /** Everything around you is hurt and thrown back and up; closer means harder. */
    private void impact(ServerLevel level) {
        double radius = this.airSlam ? AIR_SLAM_RADIUS : SLAM_RADIUS;
        float damage = this.airSlam ? (float) ability("ground_slam").value("airDamage")
                : damageOf("ground_slam");
        Vec3 center = this.caster.position();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                this.caster.getBoundingBox().inflate(radius, 3.0, radius),
                entity -> !this.holds(entity) && SpellTargeting.isTargetable(this.caster, entity))) {
            Vec3 away = living.position().subtract(center);
            double distance = Math.sqrt(away.horizontalDistanceSqr());
            double strength = 1.0 - distance / radius;
            if (strength <= 0.0 || !this.hurt(living, (float) (damage * (0.5 + 0.5 * strength)), 0.0)) {
                continue;
            }
            Vec3 push = distance < 1.0E-3 ? Vec3.ZERO : new Vec3(away.x / distance, 0, away.z / distance);
            living.setDeltaMovement(living.getDeltaMovement().add(push.scale(1.3 * strength))
                    .add(0, 0.45 + 0.35 * strength, 0));
            living.hasImpulse = true;
            living.hurtMarked = true;
        }
        double groundY = SpellTargeting.floorBelow(level, BlockPos.containing(center.x, center.y + 0.5, center.z));
        BlockState ground = level.getBlockState(BlockPos.containing(center.x, groundY - 0.5, center.z));
        if (!ground.isAir()) {
            for (double ring = 1.0; ring <= radius; ring += 1.5) {
                for (int i = 0; i < 16; i++) {
                    double angle = Math.PI * 2 * i / 16;
                    SpellFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground),
                            center.x + Math.cos(angle) * ring, groundY + 0.1, center.z + Math.sin(angle) * ring,
                            4, 0.2, 0.1, 0.2, 0.25);
                }
            }
        }
        Vec3 at = new Vec3(center.x, groundY, center.z);
        SpellFx.cloud(level, ParticleTypes.EXPLOSION, at.add(0, 0.4, 0), this.airSlam ? 5 : 3, 1.0, 0.0);
        SpellFx.shockwave(level, ParticleTypes.CLOUD, at.add(0, 0.2, 0), 40, 0.6);
        SpellFx.shockwave(level, ParticleTypes.ELECTRIC_SPARK, at.add(0, 0.3, 0), 30, 0.8);
        this.sound(at, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.4F, 0.8F);
        this.sound(at, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.9F);
        this.sound(at, SoundEvents.ANVIL_LAND, 0.7F, 0.6F);
    }

    // ---- Portal ----

    /** One tentacle goes portal hunting; the other three keep doing their own work. */
    boolean startPortal(ServerLevel level) {
        if (this.portal != null) {
            return false;
        }
        Arm arm = this.freeArm();
        if (arm == null) {
            this.caster.displayClientMessage(
                    Component.translatable("octopus." + WelcomeScreenMod.MODID + ".busy"), true);
            return false;
        }
        LivingEntity target = SpellTargeting.aimLiving(this.caster, level, PortalRun.RANGE);
        if (target == null) {
            SpellTargeting.noTarget(this.caster);
            return false;
        }
        this.portal = new PortalRun(this.caster, target);
        this.portalArm = arm.index;
        arm.job = Job.PORTAL;
        arm.age = 0;
        this.sound(this.caster.position(), SoundEvents.BEACON_ACTIVATE, 1.0F, 1.6F);
        return true;
    }

    private void tickPortal(ServerLevel level) {
        PortalRun run = this.portal;
        if (run == null) {
            return;
        }
        Arm arm = this.arms[this.portalArm];
        boolean running = run.tick(level, arm.tip);
        // The tentacle is free again as soon as it is back out of the portal, while the portals close.
        if (arm.job == Job.PORTAL && !run.busy()) {
            this.toRest(arm);
        }
        if (!running) {
            this.portal = null;
        }
    }

    // ---- Octopus Rampage ----

    /** For 10 seconds: double attack speed, and all four tentacles attack nearby enemies by themselves. */
    boolean rampage(ServerLevel level) {
        this.rampage = ability("rampage").intValue("durationTicks");
        OctopusArms.modifier(this.caster, Attributes.ATTACK_SPEED, OctopusArms.RAMPAGE_ID, 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
        Vec3 at = this.caster.position().add(0, 1.0, 0);
        SpellFx.sphereOut(level, ParticleTypes.FLAME, at, 40, 0.3);
        SpellFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at, 30, 0.5);
        this.sound(at, SoundEvents.RAVAGER_ROAR, 1.0F, 1.3F);
        this.sound(at, SoundEvents.BEACON_POWER_SELECT, 1.0F, 0.6F);
        this.caster.displayClientMessage(
                Component.translatable("octopus." + WelcomeScreenMod.MODID + ".rampage.start"), true);
        return true;
    }

    private void tickRampage(ServerLevel level) {
        if (this.rampage <= 0) {
            return;
        }
        this.rampage--;
        if (this.rampage == 0) {
            this.endRampage();
            OctopusArms.sync(this.caster);
            return;
        }
        for (Arm arm : this.arms) {
            if (arm.free() && !this.isBlocking() && (this.rampage + arm.index * 3) % RAMPAGE_EVERY == 0) {
                LivingEntity target = this.nearest(level, RAMPAGE_RANGE, false);
                if (target != null) {
                    this.strike(arm, target, Hit.RAMPAGE, 0);
                }
            }
        }
        if (this.rampage % 20 == 0) {
            this.sound(this.caster.position(), SoundEvents.BLAZE_BURN, 0.5F, 0.6F);
        }
    }

    private void endRampage() {
        this.rampage = 0;
        OctopusArms.modifier(this.caster, Attributes.ATTACK_SPEED, OctopusArms.RAMPAGE_ID, 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
    }

    // ---- Helpers ----

    private void sound(Vec3 at, SoundEvent sound, float volume, float pitch) {
        this.caster.level().playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** One of Doctor Octopus's abilities by name, with its numbers from his config file. */
    static CharacterAbility ability(String id) {
        CharacterAbility ability = GameCharacter.DOC_OCK.byName(id);
        if (ability == null) {
            throw new IllegalStateException("Doctor Octopus has no ability named " + id);
        }
        return ability;
    }

    /** The damage of one of Doctor Octopus's abilities, straight from his config file. */
    static float damageOf(String id) {
        return ability(id).getDamage();
    }

    private static double ease(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3 - 2 * t);
    }

    /** Softer than {@link #ease}: it starts and ends with no speed at all, so nothing tugs. */
    private static double smoother(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
}
