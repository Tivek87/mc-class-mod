package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechBeam;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// What every move of a built mech keeps (which it is and how far in), and its moves of no blow of the combo: the
// rocket boots and the jump off the ground and down again, the held eye beam, the missile arm and the flamethrower.
abstract class MechAttackMoves {
    // A dive or a fall still falling this long lands where it is.
    static final int PLUNGE_MOST = 100;

    int kind;
    int t;
    int from;
    double turn;
    boolean held;
    int plunged;
    private int fired;
    private int firedAt;
    private int powered;
    // The eyes fired along with the missile arm: since when (-1 not), whether held as a beam, whether still held.
    private int eyesAt = -1;
    private boolean eyesBeam;
    private boolean eyesHeld;

    MechAttackMoves(int kind) {
        this.kind = kind;
    }

    int kind() {
        return this.kind;
    }

    MechAttacks.Blow blow() {
        return new MechAttacks.Blow(this.kind, this.t, this.shownFrom(), this.turn, this.eyes());
    }

    int packed() {
        return MechAttacks.pack(this.kind, this.t, this.shownFrom(), this.turn, this.eyes());
    }

    private int eyes() {
        return this.eyesAt < 0 ? 0 : MechAttacks.eyes(this.eyesBeam, this.t - this.eyesAt);
    }

    // Whether its eyes can fire along with this move now: the missile arm up, no ray or beam already going.
    boolean eyesFree() {
        return this.kind == MechAttacks.AIM && this.eyesAt < 0 && this.t < MechAttacks.AIM_MOST;
    }

    void eyesOn(boolean beam) {
        this.eyesAt = this.t;
        this.eyesBeam = beam;
        this.eyesHeld = beam;
    }

    // The eyes along with this move, a tick on: the ray strikes at its moment, the beam burns while held and paid.
    void eyesTick(ServerLevel level, ServerPlayer owner, MechScript.Stage torso, CharacterAbility ability) {
        if (this.eyesAt < 0) {
            return;
        }
        int age = this.t - this.eyesAt;
        if (!this.eyesBeam) {
            if (age == MechAttacks.EYE_FIRE) {
                MechBeams.eye(level, owner, torso, ability);
            } else if (age >= MechAttacks.length(MechAttacks.EYE)) {
                this.eyesAt = -1;
            }
            return;
        }
        if (age >= MechAttacks.length(MechAttacks.GLARE)) {
            this.eyesAt = -1;
            return;
        }
        if (age < MechAttacks.GLARE_FIRE || age >= MechAttacks.GLARE_MOST) {
            return;
        }
        if (!this.eyesHeld || !PowerRing.upkeep(owner, age - MechAttacks.GLARE_FIRE,
                ability.value("mechGlarePowerPerSecond"))) {
            this.eyesAt = this.t - MechAttacks.GLARE_MOST;
            return;
        }
        MechBeams.glare(level, owner, torso, ability, age - MechAttacks.GLARE_FIRE);
    }

    private int shownFrom() {
        return this.kind == MechAttacks.AIM ? MechAttacks.rockets(this.fired, this.t - this.firedAt) : this.from;
    }

    // The torso as this blow bends and turns it over the upright one.
    MechScript.Stage torso(MechScript.Stage upright) {
        MechAttacks.Body body = MechAttacks.body(this.blow());
        return MechAttacks.torso(MechAttacks.aimed(upright, body), body);
    }

    // The rocket boots: it springs up at the launch and flies while their thrust lasts (mechRocketSeconds); out of
    // it, or cut, it throws its arms out and falls; on the ground again, it lands.
    void fly(ServerLevel level, MechScript.Stage legs, CharacterAbility ability, boolean grounded) {
        Vec3 feet = legs.base();
        if (this.t == MechAttacks.FLY_LAUNCH) {
            MechBlows.stomp(level, feet, true);
            Sounds.play(level, feet, SoundEvents.FIREWORK_ROCKET_LAUNCH, 4.0F, 0.5F);
            Sounds.play(level, feet, SoundEvents.BLAZE_SHOOT, 3.0F, 0.5F);
        }
        int thrust = MechAttacks.FLY_LAUNCH + Math.min(MechAttacks.FLY_MOST,
                (int) Math.round(ability.value("mechRocketSeconds") * 20.0));
        if (this.t < MechAttacks.FLY_FALL - 6 && this.t >= thrust) {
            this.cut(level, feet);
        }
        if (this.t > MechAttacks.FLY_LAUNCH && this.t < MechAttacks.FLY_FALL - 6) {
            this.powered++;
            if (this.powered % 4 == 0) {
                Sounds.play(level, feet, SoundEvents.FIREWORK_ROCKET_LAUNCH, 2.5F, 0.4F + 0.1F * level.random
                        .nextFloat());
            }
        }
        this.land(level, feet, grounded, MechAttacks.FLY_LAUNCH, MechAttacks.FLY_FALL, MechAttacks.FLY_LAND);
    }

    // Off the ground from `launch` on, it waits at `fall` until its feet touch the ground and lands at `land`.
    void land(ServerLevel level, Vec3 feet, boolean grounded, int launch, int fall, int land) {
        if (this.t == land && !grounded && ++this.plunged < PLUNGE_MOST) {
            this.t = fall;
        } else if (grounded && this.t > launch + 8 && this.t < land) {
            this.t = land;
        }
        if (this.t == land) {
            MechBlows.stomp(level, feet, true);
        }
    }

    // Whether it is off the ground in a jump.
    boolean leaping() {
        return this.kind == MechAttacks.JUMP && this.t >= MechAttacks.JUMP_LAUNCH && this.t < MechAttacks.JUMP_LAND;
    }

    // The rocket boots' thrust runs out, or its pilot cuts it: it falls.
    void cut(ServerLevel level, Vec3 feet) {
        if (this.kind == MechAttacks.FLY && this.t < MechAttacks.FLY_FALL - 6) {
            this.t = MechAttacks.FLY_FALL - 6;
            Sounds.play(level, feet, SoundEvents.FIRE_EXTINGUISH, 3.0F, 0.5F);
        }
    }

    // Whether it flies on its rocket boots with thrust left, high enough off the ground to dive.
    boolean flying() {
        return this.kind == MechAttacks.FLY && this.t >= MechAttacks.FLY_LAUNCH + 2 && this.t < MechAttacks.FLY_LAND;
    }

    // The held beam: it burns from GLARE_FIRE while the button is held, GLARE_MOST at most, paying for itself once a
    // second; let go or out of power, it dies away.
    void glare(ServerLevel level, ServerPlayer owner, MechScript.Stage torso, CharacterAbility ability) {
        if (this.t < MechAttacks.GLARE_FIRE || this.t >= MechAttacks.GLARE_MOST) {
            return;
        }
        if (!this.held || !PowerRing.upkeep(owner, this.t - MechAttacks.GLARE_FIRE,
                ability.value("mechGlarePowerPerSecond"))) {
            this.t = MechAttacks.GLARE_MOST;
            return;
        }
        MechBeams.glare(level, owner, torso, ability, this.t - MechAttacks.GLARE_FIRE);
    }

    // The right button let go: a held beam dies away.
    void letGo() {
        this.held = false;
        this.eyesHeld = false;
    }

    // A left click with the missile arm up: one missile out of the next full tube, once the hand is open and the last
    // one's kick has settled.
    boolean fire(ServerLevel level, ServerPlayer owner, MechScript.Stage upright, CharacterAbility ability) {
        if (this.kind != MechAttacks.AIM || this.t < MechAttacks.AIM_OPEN || this.t >= MechAttacks.AIM_MOST
                || this.fired >= MechAttacks.ROCKETS
                || this.fired > 0 && this.t - this.firedAt < MechAttacks.ROCKET_KICK
                || !PowerRing.pay(owner, ability.value("mechMissilePowerCost"))) {
            return false;
        }
        MechScript.Stage torso = this.torso(upright);
        Vec3 target = MechBeam.aim(level, owner, MechBeam.AIM_RANGE);
        MechMoves.Arm arm = MechAttacks.arm(this.blow(), true, upright, torso, null,
                MechMoves.arm(true, upright, MechScript.SETTLED), target);
        MechMissiles.fire(level, owner, ability, this.fired, torso.point(arm.hand()), torso.dir(arm.way()),
                torso.dir(arm.palm()).scale(-1.0), target);
        this.fired++;
        this.firedAt = this.t;
        return true;
    }

    // Whether the missile arm has fired its last missile and that one's kick has settled.
    boolean spent() {
        return this.fired >= MechAttacks.ROCKETS && this.t - this.firedAt >= MechAttacks.ROCKET_KICK;
    }

    // The missile arm lowered (R again, its missiles spent or its time up): the hand shuts and the arm comes down. The
    // flamethrower shut (V again, its time or power spent): its nozzle sinks back and the arm comes down.
    void close() {
        if (this.kind == MechAttacks.AIM && this.t < MechAttacks.AIM_MOST) {
            this.t = MechAttacks.AIM_MOST;
        } else if (this.kind == MechAttacks.FLAME && this.t < MechAttacks.FLAME_MOST) {
            this.t = MechAttacks.FLAME_MOST;
        }
    }

    // The flamethrower: from FLAME_FIRE it pours fire where its pilot aims, paying for itself once a second, until it
    // is shut, its time (mechFlameSeconds) is up or its power runs out.
    void flame(ServerLevel level, ServerPlayer owner, MechScript.Stage frame, MechScript.Stage torso,
            CharacterAbility ability) {
        int pouring = this.t - MechAttacks.FLAME_FIRE;
        if (pouring < 0 || this.t >= MechAttacks.FLAME_MOST) {
            return;
        }
        if (pouring >= ability.value("mechFlameSeconds") * 20.0
                || !PowerRing.upkeep(owner, pouring, ability.value("mechFlamePowerPerSecond"))) {
            this.close();
            return;
        }
        Vec3 target = MechBeam.aim(level, owner, ability.value("mechFlameReach"));
        MechMoves.Arm arm = MechAttacks.arm(this.blow(), true, frame, torso, null,
                MechMoves.arm(true, frame, MechScript.SETTLED), target);
        Vec3 muzzle = torso.point(arm.hand()).add(torso.dir(arm.way()).scale(MechAttacks.MUZZLE));
        MechFlame.pour(level, owner, muzzle, target.subtract(muzzle), ability, pouring);
    }
}
