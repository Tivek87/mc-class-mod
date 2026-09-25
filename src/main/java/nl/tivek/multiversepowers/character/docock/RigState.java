package nl.tivek.multiversepowers.character.docock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;

/**
 * The state of one player's Octopus Arms (see {@link OctoRig}): the numbers they work with, the four
 * tentacles and what each one is doing, what the rest of the mod asks about them, and where they are
 * fixed on the body.
 */
abstract class RigState {
    static final int UNFOLD = 14;
    static final int FOLD = 12;
    static final double CLAW_REST = 0.3;
    static final double MAX_STEP = 3.0;
    // How a tip moves: a spring instead of a straight step. Stiffness is how hard it is pulled (on top
    // of each pose's own eagerness), damping how much of its speed it keeps every tick. Together they
    // sit just under critical, so every tip speeds up and eases off by itself and nothing jerks.
    static final double SPRING = 0.38;
    static final double DAMPING = 0.70;
    // How quickly a claw turns towards the way it should be looking.
    static final double AIM_EASE = 0.18;

    // Tentacle Strike and Multi-Tentacle.
    static final int STRIKE_MAX = 7;
    static final int MULTI_GAP = 3;

    // Grab and Throw. A tentacle never lets go by itself: only you do (see letGoAll).
    static final double GRAB_SPEED = 1.3;
    static final int GRAB_MAX = 26;
    static final double HOLD_DISTANCE = 3.2;
    // How hard a held creature is whipped along with your view: high enough to smash it into walls.
    static final double FOLLOW = 0.75;
    static final double MAX_SPEED = 3.6;
    static final double THROW_SPEED = 2.6;
    static final int CRASH_PAUSE = 5;
    static final int THROWN_TRACK = 40;
    // Smashing everything you hold into the ground (Ground Slam with full claws).
    static final int HELD_SLAM_TIME = 10;

    // Tentacle Dash.
    // How hard a dash throws you upwards; how far it throws you is a setting of the ability itself.
    static final double DASH_LIFT = 0.5;
    static final int PLANT_TIME = 6;

    // Ground Slam.
    static final int RISE_TIME = 8;
    static final int MAX_DROP = 40;
    static final double AIR_DROP = 1.8;
    static final double SLAM_RADIUS = 6.0;

    static final double AIR_SLAM_RADIUS = 8.0;


    // Octopus Rampage.
    static final double RAMPAGE_RANGE = 8.0;
    static final int RAMPAGE_EVERY = 12;

    // Legs: how far a foot may fall behind before it steps, and how long a step takes.
    static final double STEP_AFTER = 1.2;
    static final int STEP_TIME = 7;
    static final double LEG_SPREAD = 1.45;
    // How far under the player the legs still find ground to stand on.
    static final double LEG_DROP = 3.2;
    // Walking on tentacles: on 2, 3 or 4 of them. 0 means walking on your own feet.
    static final int[] STANCES = { 0, 2, 3, 4 };

    // Wall Climb: every tentacle holds its own grip, far away from the others.
    static final double GRIP_REACH = 3.4;
    static final double GRIP_MIN_APART = 1.9;
    static final int GRIP_EVERY = 4;
    // How far around your body a wall or a roof may be for the server to believe your client that you
    // hold on to it: a little further than the client itself ever holds on from.
    static final double HOLD_REACH = 2.0;
    // How far out a climbing tentacle that holds nothing reaches, so the four keep their distance
    // even while they are still looking for a new grip.
    static final double CLIMB_SPREAD = 2.0;
    // How far two claws are always kept apart, whatever pose they are in: without this two
    // tentacles that want the same piece of air end up crossing or sitting inside each other.
    static final double TIP_APART = 1.15;
    // How much faster the tentacles carry you when you run on them.
    static final double LEG_RUN_BONUS = 0.4;
    // The chance per tick that a tentacle starts looking around while you stand still.
    static final double IDLE_CHANCE = 0.004;
    // Blocks and Building is not on a key any more; what is left of it works on these numbers.
    static final double BUILD_RANGE = 10.0;
    static final int BUILD_CLUSTER = 27;
    static final float BUILD_DAMAGE = 8.0F;

    // Block.
    static final int SHIELD_TIME = 6;
    static final double SHIELD_SIZE = 1.15;

    // Ground Strike: into the ground at your feet, out of the ground at the creatures you marked.
    // How long the tip takes to reach the hole, how deep it goes in, and how fast it slides.
    // How long the tentacles are first held up in front of you, spikes sliding out of the claws, so
    // you can see the strike coming before anything goes into the ground.
    static final int SHOW_TIME = 9;
    static final int DIVE_TIME = 6;
    static final double SINK_DEPTH = 3.5;
    static final double SINK_SPEED = 0.8;
    // How long it travels under the ground (a bit longer the further it has to go).
    static final int TRAVEL_MIN = 5;
    static final double TRAVEL_PER_BLOCK = 0.35;
    // Coming up: how fast, how far it keeps going above the ground, and how long it stays up.
    static final double RISE_SPEED = 1.35;
    static final double RISE_ABOVE = 1.2;
    static final int SPIKE_HOLD = 6;
    static final double SPIKE_SINK = 0.9;
    // How deep under the ground the spike starts before it shoots up.
    static final double SPIKE_BURIED = 3.0;
    // How long the glow on a marked creature lasts. It is topped up while the mark stands, so it goes
    // out by itself soon after, and is never saved with the creature for good.
    static final int MARK_GLOW = 10;

    // A key press that finds nothing to do starts no cooldown, so a client could send one every tick:
    // the search behind it (aim rays, creatures around you) is then not run again for this many ticks.
    private static final int SEARCH_AGAIN = 3;

    enum Job {
        REST, STRIKE, REACH, HOLD, RISE, SMASH, PLANT, PORTAL, CARRY, BURROW
    }

    /** The steps one tentacle goes through during a Ground Strike, in this order. */
    enum Dig {
        SHOW, TO_HOLE, SINK, UNDER, UP, DOWN, BACK
    }

    enum Hit {
        MELEE, MULTI, RAMPAGE
    }

    enum Slam {
        NONE, RISE, DROP, SMASH
    }

    static final class Arm {
        final int id = RobotArm.newId();
        final int index;
        final boolean upper;
        final double side;
        Vec3 tip;
        double blend;
        double claw = CLAW_REST;
        Job job = Job.REST;
        int age;
        @Nullable
        LivingEntity target;
        @Nullable
        LivingEntity held;
        int holdTicks;
        double holdDistance = HOLD_DISTANCE;
        int crashPause;
        Hit hit = Hit.MELEE;
        int delay;
        int struckAt = -1;
        Vec3 spot = Vec3.ZERO;
        // Legs: where the foot stands, and a step in progress.
        @Nullable
        Vec3 foot;
        Vec3 stepFrom = Vec3.ZERO;
        Vec3 stepTo = Vec3.ZERO;
        int step = -1;
        boolean leg;
        // Wall Climb: the spot this tentacle holds on to, kept until it is out of reach.
        @Nullable
        Vec3 grip;
        int gripAge;
        // The blocks this tentacle carries (see TentacleBlocks); null when it carries none.
        @Nullable
        TentacleBlocks.Load load;
        // Only used while this tentacle runs the Portal ability.
        double tipOffset;
        Vec3 clipPoint = Vec3.ZERO;
        Vec3 clipNormal = Vec3.ZERO;
        int cut;
        // The Portal ability's tools at the tip: 0 = away, 1 = fully out.
        double spike;
        double thrust;
        // How fast the tip is going right now, so it can speed up and slow down instead of jumping.
        Vec3 speed = Vec3.ZERO;
        // Where the claw should point (zero: straight on out of the tentacle) and where it really
        // points right now, which follows the first one smoothly.
        Vec3 aim = Vec3.ZERO;
        Vec3 aimShown = Vec3.ZERO;
        // Standing still: where this claw is looking right now, and between which ticks.
        Vec3 glance = Vec3.ZERO;
        boolean lookAtYou;
        int glanceFrom;
        int glanceUntil;
        // Ground Strike: which step it is in, and the far part that comes up out of the ground.
        Dig dig = Dig.SHOW;
        int farId = -1;
        Vec3 digAt = Vec3.ZERO;
        double risen;
        double digGround;
        int digTravel;
        boolean digHit;

        private Arm(int index, Vec3 start) {
            this.index = index;
            this.upper = index < 2;
            this.side = index % 2 == 0 ? 1 : -1;
            this.tip = start;
        }

        /** Nothing to do, so it can take on a new job. A leg counts as busy: it carries you. */
        boolean free() {
            return this.job == Job.REST && !this.leg;
        }
    }

    final ServerPlayer caster;
    // The level the arms live in (where they are ticked), even once the player has left it.
    final ServerLevel home;
    // The number of walking tentacles and marked creatures the client was last told about.
    int syncedLegs;
    int syncedMarks;
    final Arm[] arms = new Arm[4];
    int age;
    double unfold;
    boolean folding;
    boolean blocking;
    int shieldId = -1;
    double shieldOpen;
    boolean climbing;
    Direction climbFace = Direction.NORTH;
    int lastBlocked = -100;
    int rampage;
    int nextArm;
    boolean delivering;
    boolean throwRequested;
    // Which stance you walk in: an index in STANCES (starts on two tentacles).
    int stance = 1;
    // Ground Strike: the creatures you picked out, waiting for you to launch the strike.
    final List<LivingEntity> marks = new ArrayList<>();
    // Whether you are running on the tentacles right now, so the speed only changes when that does.
    boolean legsRunning;
    // Counts down while the tentacles smash everything they hold into the ground.
    int heldSlam;

    @Nullable
    PortalRun portal;
    int portalArm = -1;

    Slam slam = Slam.NONE;
    int slamAge;
    boolean airSlam;

    // Per ability: the tick its key last found nothing to do (see SEARCH_AGAIN).
    private final Map<String, Integer> foundNothingAt = new HashMap<>();

    RigState(ServerPlayer caster, ServerLevel home) {
        this.caster = caster;
        this.home = home;
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

    /** True when a tentacle holds this creature, or the Portal ability is carrying it. */
    boolean holds(Entity entity) {
        for (Arm arm : this.arms) {
            if (arm.held == entity) {
                return true;
            }
        }
        return this.portal != null && this.portal.holds(entity);
    }

    /** The level the arms live in. */
    ServerLevel level() {
        return this.home;
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
    boolean hasThrowable() {
        for (Arm arm : this.arms) {
            if (arm.held != null || arm.load != null) {
                return true;
            }
        }
        return false;
    }

    /** True while at least one tentacle carries blocks, so a right click can set them down. */
    boolean hasLoad() {
        for (Arm arm : this.arms) {
            if (arm.load != null) {
                return true;
            }
        }
        return false;
    }

    // ---- Where the arms sit ----

    Vec3 forward() {
        return RobotArm.forward(this.caster);
    }

    Vec3 right() {
        return RobotArm.right(this.caster);
    }

    /** Where arm {@code i} is fixed on the back: two at the shoulders, two at the hips. */
    Vec3 mount(int i) {
        boolean upper = i < 2;
        double side = i % 2 == 0 ? 1 : -1;
        return this.caster.position().add(0, upper ? 1.35 : 0.75, 0).subtract(this.forward().scale(0.34))
                .add(this.right().scale(side * (upper ? 0.24 : 0.2)));
    }

    // ---- Helpers ----

    void sound(Vec3 at, SoundEvent sound, float volume, float pitch) {
        this.caster.level().playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** True when this ability's key found nothing to do a moment ago: it does not search again so soon. */
    boolean searchedJustNow(String ability) {
        Integer at = this.foundNothingAt.get(ability);
        return at != null && this.age - at < SEARCH_AGAIN;
    }

    /** Remembers that this ability's key found nothing to do (see searchedJustNow); always false. */
    boolean foundNothing(String ability) {
        this.foundNothingAt.put(ability, this.age);
        return false;
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
}
