package nl.tivek.multiversepowers.creature.wizard;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.staff.WizardStaff;

// How the Wizard fights: he keeps his foe at a caster's distance, circling them, gliding quickly aside or back when
// they close in and towards them when they fall behind, going to them from afar, and casts what is ready and fits
// (WizardPicks). Light spells (sparks, bolts) he flicks often; up close he strikes with his staff, its combo or whirl,
// or casts a close spell (repulse, frost cone, flame lash: CloseSpells), else goes elsewhere (WizardSteps); a foe far
// off he may tether in; between his big spells he takes a breath, never the same big spell twice running, and an
// ultimate never soon after another. From his second phase he flies (WizardFlight), coming down to fight on his feet a
// while now and then and taking off again (ALIGHT, TAKEOFF), and now and then, or when struck, blinks to another spot
// in the air (AIRSTEP). His staff he may hurl as a spear or set free to fight on its own (staff/WizardStaff), and calls
// it home when it sticks fast or at a whim. When a phase breaks he drops whatever he was doing and the next phase takes
// him (ASCEND, UNBIND, AWAKEN, APOTHEOSIS) up into the air; its special comes first (WizardMove.special, PhaseSpecials;
// the first phase's once he has arrived), then its own great spell; caught out among his mirror images, or when his
// ward breaks, he is stunned and drops to the ground. Called by the ritual, he first comes down out of his rift
// (ARRIVE). Among several players he turns on another now and then.
final class WizardMoves extends WizardPicks {
    // The breath after a big spell, in each phase.
    private static final int[] BREATH = { 0, 36, 31, 27, 23, 20 };
    private static final int ULTIMATE_GAP = 180;
    // How fast a skip glides (blocks a tick at its start).
    private static final double SKIP_SPEED = 0.95;
    // Ticks at least before he may turn on another player.
    private static final int FOCUS = 120;
    // How long he stays up in the air, and down on his feet, before the other (ticks, at least and more at random).
    private static final int ALOFT_FOR = 400;
    private static final int GROUND_FOR = 240;
    // His staff back in his hand this long (and as much again at random) before it leaves it once more.
    private static final int STAFF_HOME = 200;

    private final WizardFlight flight;
    private final WizardSteps steps;
    private int kind;
    private int t;
    private int nextLight = 20;
    private int nextFocus = FOCUS;
    private int side = 1;
    private int sideUntil;
    private int pendingPhase;
    // When he next comes down to his feet or takes off again.
    private int switchAt = ALOFT_FOR;
    // The special his phase opens with (or his landing), cast as soon as he has a foe.
    private int special;
    // Struck in the air: he blinks away at the next chance.
    private boolean dodge;
    // How long he leaves his hurled staff where it stuck.
    private int leftFor = 60;
    private boolean staffWasOut;
    private Vec3 aim = Vec3.ZERO;
    @Nullable
    private WizardMirrors mirrors;

    WizardMoves(Wizard wizard) {
        super(wizard);
        this.flight = new WizardFlight(wizard);
        this.steps = new WizardSteps(wizard, this.flight);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    WizardFlight flight() {
        return this.flight;
    }

    // Whether he is in the middle of a move of his own (his arrival too).
    boolean busy() {
        return this.kind != 0 || this.mirrors != null || this.pendingPhase != 0;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.wizard.getTarget();
        return this.kind != 0 || this.pendingPhase != 0 || target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.kind != 0 || this.mirrors != null || this.canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    // Fighting he is aggressive: every game knows it (his face, his rest).
    @Override
    public void start() {
        this.wizard.setAggressive(true);
    }

    @Override
    public void stop() {
        this.wizard.setAggressive(false);
        if (this.kind == WizardMove.ARRIVE) {
            return;
        }
        this.interrupt();
        this.wizard.getNavigation().stop();
        this.wizard.getMoveControl().strafe(0.0F, 0.0F);
    }

    // Called by the ritual: he comes down out of his rift before anything else.
    void arrive() {
        this.begin(WizardMove.ARRIVE, null);
    }

    // A phase broke: the next one takes him as soon as he can be stopped.
    void phaseUp() {
        if (this.pendingPhase == 0 && this.wizard.phase() < WizardMove.PHASES) {
            this.pendingPhase = this.wizard.phase() + 1;
        }
    }

    // Something struck him: if it found him out among his mirrors, they all shatter and he is stunned.
    void struck(@Nullable Entity by) {
        if (this.mirrors != null && by instanceof LivingEntity && this.mirrors.racing()) {
            this.mirrors.foundOut();
            this.mirrors = null;
            this.ready[WizardMove.MIRRORS] = this.wizard.tickCount + this.again(WizardMove.MIRRORS);
            this.kind = 0;
            this.begin(WizardMove.STUNNED, this.wizard.getTarget());
            return;
        }
        if (by instanceof LivingEntity && this.wizard.flies() && this.kind == 0
                && this.wizard.getRandom().nextInt(3) == 0) {
            this.dodge = true;
        }
    }

    // His ward broke: whatever he was doing ends and he reels, dropping to the ground.
    void reel() {
        if (this.kind == WizardMove.DEATH || WizardMove.carries(this.kind) != 0 || this.kind == WizardMove.ARRIVE) {
            return;
        }
        this.interrupt();
        this.begin(WizardMove.STUNNED, this.wizard.getTarget());
    }

    // Blinking in the air: somewhere in the air round `middle` rather than on the ground; whether he found a spot.
    boolean blinkNear(ServerLevel level, Vec3 middle, @Nullable LivingEntity target) {
        return this.wizard.flies() && this.flight.blinkNear(level, middle, target);
    }

    // He dies: whatever he was doing ends, his images with it.
    void dying() {
        this.interrupt();
        this.kind = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.wizard.getTarget();
        if (this.pendingPhase != 0 && WizardMove.carries(this.kind) == 0 && this.kind != WizardMove.ARRIVE) {
            this.interrupt();
            this.begin(WizardMove.into(this.pendingPhase), target);
            return;
        }
        if (target != null && this.kind != WizardMove.BEAM && this.kind != WizardMove.ARRIVE) {
            this.wizard.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (this.mirrors != null) {
            if (!this.mirrors.tick(target)) {
                this.mirrors = null;
                this.finish();
            }
            return;
        }
        if (this.kind != 0) {
            this.t++;
            ServerLevel level = (ServerLevel) this.wizard.level();
            if (target != null && target.isAlive()) {
                this.aim = target.position();
            }
            this.fly(level, target);
            WizardCasts.during(level, this.wizard, this.kind, this.t, target);
            this.steps.during(level, this.kind, this.t, target);
            if (this.t == WizardMove.release(this.kind)) {
                if (this.kind == WizardMove.MIRRORS && target != null) {
                    this.mirrors = WizardMirrors.begin(level, this.wizard, target);
                    return;
                }
                // Whatever goes straight out of him goes at his foe, from the ground or the air.
                if (target != null && target.isAlive() && this.kind != WizardMove.BEAM) {
                    MonsterBlows.face(this.wizard, target.position().subtract(this.wizard.position()));
                }
                if (this.kind == WizardMove.AIRSTEP) {
                    this.flight.airstep(level, target);
                } else if (WizardSteps.takes(this.kind)) {
                    this.steps.release(level, this.kind, target);
                } else {
                    WizardCasts.release(level, this.wizard, this.kind, target, this.aim);
                }
                if (WizardMove.carries(this.kind) != 0) {
                    this.pendingPhase = 0;
                }
            }
            if (this.t >= this.length()) {
                this.finish();
            }
            return;
        }
        if (target == null) {
            return;
        }
        if (this.special != 0) {
            int special = this.special;
            this.special = 0;
            this.begin(special, target);
            return;
        }
        int now = this.wizard.tickCount;
        double far = this.wizard.distanceTo(target);
        double gap = MonsterBlows.gap(this.wizard, target);
        boolean sees = this.wizard.getSensing().hasLineOfSight(target);
        this.blind = sees ? 0 : this.blind + 1;
        if (this.manage(target, now)) {
            return;
        }
        if (this.wizard.flies()) {
            if (now >= this.sideUntil) {
                this.side = -this.side;
                this.sideUntil = now + 40 + this.wizard.getRandom().nextInt(50);
            }
            this.wizard.getNavigation().stop();
            this.flight.steer(target, far, sees, this.side, now);
            if (this.dodge && this.ready(WizardMove.AIRSTEP)) {
                this.dodge = false;
                this.begin(WizardMove.AIRSTEP, target);
                return;
            }
        } else {
            this.keepAway(target, far, sees, now);
        }
        this.refocus(target, now);
        if (now >= this.nextLight) {
            int pick = this.pick(target, gap, far, sees, now);
            if (pick != 0) {
                this.begin(pick, target);
            }
        }
    }

    // Between moves: his hurled staff left where it stuck a while, then called home or woken to fight on its own; one
    // fighting on its own called home now and then (one lost counts as back in his hand); and from his second phase
    // down to his feet and up into the air again in turn (up at once for a foe high over him or a great spell he casts
    // in the air). Whether he began a move.
    private boolean manage(LivingEntity target, int now) {
        WizardStaff staff = WizardStaff.of(this.wizard);
        RandomSource random = this.wizard.getRandom();
        if (this.wizard.staffOut() && staff == null) {
            this.wizard.setStaffOut(false);
        }
        if (staff != null) {
            int mode = staff.mode();
            if (mode == WizardStaff.STUCK && staff.inMode() > this.leftFor) {
                this.begin(WizardMove.STAFF_CALL, target);
                return true;
            }
            if (mode >= WizardStaff.PLANTED) {
                staff.putAway();
            }
        }
        boolean out = this.wizard.staffOut();
        if (this.staffWasOut && !out) {
            this.nextStaff = now + STAFF_HOME + random.nextInt(STAFF_HOME);
        }
        this.staffWasOut = out;
        if (this.wizard.phase() < 2) {
            return false;
        }
        boolean up = this.wizard.aloft();
        boolean air = this.opening != 0 && airborne(this.opening) || target.getY() - this.wizard.getY() > 6.0;
        if (up ? now >= this.switchAt && !air : now >= this.switchAt || air) {
            this.begin(up ? WizardMove.ALIGHT : WizardMove.TAKEOFF, target);
            return true;
        }
        return false;
    }

    // Among several players he turns on another now and then rather than staying on one.
    private void refocus(LivingEntity target, int now) {
        if (now < this.nextFocus) {
            return;
        }
        this.nextFocus = now + FOCUS + this.wizard.getRandom().nextInt(FOCUS);
        List<LivingEntity> others = WizardCasts.others((ServerLevel) this.wizard.level(), this.wizard, target);
        if (!others.isEmpty()) {
            this.wizard.setTarget(others.get(this.wizard.getRandom().nextInt(others.size())));
        }
    }

    private int length() {
        if (this.kind == WizardMove.BOLT && this.wizard.phase() > 1) {
            return WizardMove.length(WizardMove.BOLT) + WizardMove.BOLT_AGAIN * (WizardCasts.bolts(this.wizard) - 1);
        }
        return WizardMove.length(this.kind);
    }

    // How he moves while a move lasts: on the ground only a skip moves him; in the air he hangs where he is, but for
    // the moves that carry him (the comet, the phantom strike, the rise before the supernova, coming down to his feet
    // and taking off again) and the fall as he reels.
    private void fly(ServerLevel level, @Nullable LivingEntity target) {
        if (WizardMove.close(this.kind) && !this.wizard.flies()) {
            this.lunge(target);
            return;
        }
        switch (this.kind) {
            case WizardMove.SKIP -> this.glide();
            case WizardMove.ALIGHT -> this.flight.alight(level, this.t);
            case WizardMove.TAKEOFF -> this.flight.takeoff(this.t);
            case WizardMove.COMET -> this.flight.comet(level, this.t, target);
            case WizardMove.PHANTOM -> this.flight.phantom(level, this.t, target);
            case WizardMove.SUPERNOVA -> this.flight.rise(level, this.t);
            case WizardMove.STUNNED, WizardMove.ARRIVE, WizardMove.DEATH -> {
            }
            default -> {
                if (this.wizard.flies()) {
                    this.flight.hover();
                }
            }
        }
    }

    // Circles them at a caster's distance: closes in from afar or when he cannot see them, backs off when they come
    // near, and otherwise steps round them, now this way, now that.
    private void keepAway(LivingEntity target, double far, boolean sees, int now) {
        if (!sees || far > FAR) {
            if (this.wizard.getNavigation().isDone() || now % 10 == 0) {
                this.wizard.getNavigation().moveTo(target, 1.15);
            }
            return;
        }
        this.wizard.getNavigation().stop();
        if (now >= this.sideUntil) {
            this.side = -this.side;
            this.sideUntil = now + 30 + this.wizard.getRandom().nextInt(40);
        }
        float back = far < NEAR ? -0.8F : far > FAR - 3.0 ? 0.4F : 0.0F;
        this.wizard.getMoveControl().strafe(back, 0.6F * this.side);
    }

    // On his feet his staff's blows carry him in: a step after each blow of the combo, a slow drift through the whirl.
    private void lunge(@Nullable LivingEntity target) {
        double step = switch (this.kind) {
            case WizardMove.COMBO -> this.t == WizardMove.COMBO_SMASH - 3 || this.t == WizardMove.COMBO_SWEEP - 3
                    || this.t == WizardMove.COMBO_THRUST - 3 ? 0.42 : 0.0;
            case WizardMove.WHIRL -> this.t >= WizardMove.release(WizardMove.WHIRL) && this.t < WizardMove.WHIRL_STOP
                    ? 0.1 : 0.0;
            default -> 0.0;
        };
        if (step <= 0.0 || target == null || MonsterBlows.gap(this.wizard, target) < 0.8) {
            return;
        }
        Vec3 way = target.position().subtract(this.wizard.position()).multiply(1.0, 0.0, 1.0);
        if (way.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 motion = this.wizard.getDeltaMovement();
        way = way.normalize().scale(step);
        this.wizard.setDeltaMovement(way.x, motion.y, way.z);
        this.wizard.hurtMarked = true;
    }

    // The glide itself: fast at first, slowing to a stop.
    private void glide() {
        double speed = SKIP_SPEED * (1.0 - this.t / (double) WizardMove.length(WizardMove.SKIP));
        Vec3 motion = this.wizard.getDeltaMovement();
        this.wizard.setDeltaMovement(this.skip.x * speed, Math.max(motion.y, 0.0), this.skip.z * speed);
        this.wizard.hurtMarked = true;
    }

    private void begin(int kind, @Nullable LivingEntity target) {
        this.kind = kind;
        this.t = 0;
        if (target != null) {
            this.aim = target.position();
        }
        this.wizard.getNavigation().stop();
        this.wizard.getMoveControl().strafe(0.0F, 0.0F);
        this.flight.reset();
        if (kind == WizardMove.STUNNED) {
            this.wizard.setNoGravity(false);
        }
        this.wizard.start(kind);
        WizardCasts.begin((ServerLevel) this.wizard.level(), this.wizard, kind);
    }

    // The move is over: when he may cast it again and his next spells.
    private void finish() {
        int now = this.wizard.tickCount;
        int done = this.kind;
        this.ready[done] = now + this.again(done);
        this.castAt[done] = now;
        RandomSource random = this.wizard.getRandom();
        double hurry = this.wizard.hasted() ? 0.6 : 1.0;
        if (WizardMove.big(done)) {
            this.lastBig = done;
            this.nextBig = now + (int) (BREATH[Mth.clamp(this.wizard.phase(), 1, WizardMove.PHASES)] * hurry)
                    + random.nextInt(15);
            this.nextLight = now + 8 + random.nextInt(6);
        } else {
            this.nextLight = now + (int) ((4 + random.nextInt(6)) * hurry);
        }
        if (WizardMove.ultimate(done)) {
            this.nextUltimate = now + ULTIMATE_GAP;
        }
        int phase = WizardMove.carries(done);
        if (phase != 0) {
            // Each phase opens with its own great spell.
            int opening = switch (phase) {
                case 2 -> WizardMove.STARFALL;
                case 3 -> WizardMove.MIRRORS;
                case 4 -> WizardMove.TENDRILS;
                default -> WizardMove.SUPERNOVA;
            };
            this.ready[opening] = now + (WizardMove.ultimate(opening) ? 50 : 30);
            this.opening = opening;
            this.special = WizardMove.special(phase);
            this.nextUltimate = now;
            this.nextBig = now + 20;
            this.switchAt = now + ALOFT_FOR;
        } else if (done == WizardMove.ALIGHT) {
            this.switchAt = now + GROUND_FOR + random.nextInt(160);
        } else if (done == WizardMove.TAKEOFF) {
            this.switchAt = now + ALOFT_FOR + random.nextInt(300);
        } else if (done == WizardMove.STUNNED) {
            this.wizard.setNoGravity(this.wizard.flies());
        } else if (done == WizardMove.ARRIVE) {
            this.special = WizardMove.special(1);
            this.nextLight = now + 10;
            this.nextBig = now + 40;
        } else if (done == WizardMove.STAFF_THROW) {
            this.leftFor = 40 + random.nextInt(120);
        } else if (done == WizardMove.TETHER) {
            // Dragged in, they meet his staff or a short spell at once.
            this.nextLight = now + 2;
        } else if (WizardMove.isSpecial(done)) {
            this.nextLight = now + 16;
        }
        this.steps.end(done);
        this.kind = 0;
        this.wizard.start(0);
    }

    // Stops whatever he is doing: a spell cut short counts as cast.
    private void interrupt() {
        if (this.mirrors != null) {
            this.mirrors.end();
            this.mirrors = null;
        }
        if (this.kind != 0) {
            if (this.kind == WizardMove.ARRIVE) {
                WizardCasts.landed(this.wizard);
            }
            if (this.kind == WizardMove.STUNNED) {
                this.wizard.setNoGravity(this.wizard.flies());
            }
            this.ready[this.kind] = this.wizard.tickCount + this.again(this.kind);
            this.castAt[this.kind] = this.wizard.tickCount;
            this.steps.end(this.kind);
            this.kind = 0;
            this.wizard.start(0);
        }
    }
}
