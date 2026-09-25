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

abstract class RigState {
    static final int UNFOLD = 14;
    static final int FOLD = 12;
    static final double CLAW_REST = 0.3;
    static final double MAX_STEP = 3.0;
    static final double SPRING = 0.38;
    static final double DAMPING = 0.70;
    static final double AIM_EASE = 0.18;

    static final int STRIKE_MAX = 7;
    static final int MULTI_GAP = 3;

    static final double GRAB_SPEED = 1.3;
    static final int GRAB_MAX = 26;
    static final double HOLD_DISTANCE = 3.2;
    static final double FOLLOW = 0.75;
    static final double MAX_SPEED = 3.6;
    static final double THROW_SPEED = 2.6;
    static final int CRASH_PAUSE = 5;
    static final int THROWN_TRACK = 40;
    static final int HELD_SLAM_TIME = 10;

    static final double DASH_LIFT = 0.5;
    static final int PLANT_TIME = 6;

    static final int RISE_TIME = 8;
    static final int MAX_DROP = 40;
    static final double AIR_DROP = 1.8;
    static final double SLAM_RADIUS = 6.0;

    static final double AIR_SLAM_RADIUS = 8.0;


    static final double RAMPAGE_RANGE = 8.0;
    static final int RAMPAGE_EVERY = 12;

    static final double STEP_AFTER = 1.2;
    static final int STEP_TIME = 7;
    static final double LEG_SPREAD = 1.45;
    static final double LEG_DROP = 3.2;
    static final int[] STANCES = { 0, 2, 3, 4 };

    static final double GRIP_REACH = 3.4;
    static final double GRIP_MIN_APART = 1.9;
    static final int GRIP_EVERY = 4;
    // Must stay >= ClimbControl.REACH or the server rejects valid holds.
    static final double HOLD_REACH = 2.0;
    static final double CLIMB_SPREAD = 2.0;
    static final double TIP_APART = 1.15;
    static final double LEG_RUN_BONUS = 0.4;
    static final double IDLE_CHANCE = 0.004;
    static final double BUILD_RANGE = 10.0;
    static final int BUILD_CLUSTER = 27;
    static final float BUILD_DAMAGE = 8.0F;

    static final int SHIELD_TIME = 6;
    static final double SHIELD_SIZE = 1.15;

    static final int SHOW_TIME = 9;
    static final int DIVE_TIME = 6;
    static final double SINK_DEPTH = 3.5;
    static final double SINK_SPEED = 0.8;
    static final int TRAVEL_MIN = 5;
    static final double TRAVEL_PER_BLOCK = 0.35;
    static final double RISE_SPEED = 1.35;
    static final double RISE_ABOVE = 1.2;
    static final int SPIKE_HOLD = 6;
    static final double SPIKE_SINK = 0.9;
    static final double SPIKE_BURIED = 3.0;
    static final int MARK_GLOW = 10;

    // A key with no cooldown could be spammed; throttle its search instead.
    private static final int SEARCH_AGAIN = 3;

    enum Job {
        REST, STRIKE, REACH, HOLD, RISE, SMASH, PLANT, PORTAL, CARRY, BURROW
    }

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
        @Nullable
        Vec3 foot;
        Vec3 stepFrom = Vec3.ZERO;
        Vec3 stepTo = Vec3.ZERO;
        int step = -1;
        boolean leg;
        @Nullable
        Vec3 grip;
        int gripAge;
        @Nullable
        TentacleBlocks.Load load;
        double tipOffset;
        Vec3 clipPoint = Vec3.ZERO;
        Vec3 clipNormal = Vec3.ZERO;
        int cut;
        double spike;
        double thrust;
        Vec3 speed = Vec3.ZERO;
        Vec3 aim = Vec3.ZERO;
        Vec3 aimShown = Vec3.ZERO;
        Vec3 glance = Vec3.ZERO;
        boolean lookAtYou;
        int glanceFrom;
        int glanceUntil;
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

        boolean free() {
            return this.job == Job.REST && !this.leg;
        }
    }

    final ServerPlayer caster;
    final ServerLevel home;
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
    int stance = 1;
    final List<LivingEntity> marks = new ArrayList<>();
    boolean legsRunning;
    int heldSlam;

    @Nullable
    PortalRun portal;
    int portalArm = -1;

    Slam slam = Slam.NONE;
    int slamAge;
    boolean airSlam;

    private final Map<String, Integer> foundNothingAt = new HashMap<>();

    RigState(ServerPlayer caster, ServerLevel home) {
        this.caster = caster;
        this.home = home;
        for (int i = 0; i < this.arms.length; i++) {
            this.arms[i] = new Arm(i, this.mount(i));
        }
    }

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
        return this.portal != null && this.portal.holds(entity);
    }

    ServerLevel level() {
        return this.home;
    }

    boolean justBlocked() {
        return this.age - this.lastBlocked <= 1;
    }

    int rampageLeft() {
        return this.rampage;
    }

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

    int markCount() {
        return this.folding ? 0 : this.marks.size();
    }

    boolean hasThrowable() {
        for (Arm arm : this.arms) {
            if (arm.held != null || arm.load != null) {
                return true;
            }
        }
        return false;
    }

    boolean hasLoad() {
        for (Arm arm : this.arms) {
            if (arm.load != null) {
                return true;
            }
        }
        return false;
    }

    Vec3 forward() {
        return RobotArm.forward(this.caster);
    }

    Vec3 right() {
        return RobotArm.right(this.caster);
    }

    Vec3 mount(int i) {
        boolean upper = i < 2;
        double side = i % 2 == 0 ? 1 : -1;
        return this.caster.position().add(0, upper ? 1.35 : 0.75, 0).subtract(this.forward().scale(0.34))
                .add(this.right().scale(side * (upper ? 0.24 : 0.2)));
    }

    void sound(Vec3 at, SoundEvent sound, float volume, float pitch) {
        this.caster.level().playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    boolean searchedJustNow(String ability) {
        Integer at = this.foundNothingAt.get(ability);
        return at != null && this.age - at < SEARCH_AGAIN;
    }

    boolean foundNothing(String ability) {
        this.foundNothingAt.put(ability, this.age);
        return false;
    }

    static CharacterAbility ability(String id) {
        CharacterAbility ability = GameCharacter.DOC_OCK.byName(id);
        if (ability == null) {
            throw new IllegalStateException("Doctor Octopus has no ability named " + id);
        }
        return ability;
    }

    static float damageOf(String id) {
        return ability(id).getDamage();
    }
}
