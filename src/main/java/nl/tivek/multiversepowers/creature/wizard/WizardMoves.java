package nl.tivek.multiversepowers.creature.wizard;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneFamiliar;

// How the Wizard fights: he keeps his foe at a caster's distance, circling them, gliding quickly aside or back when they
// close in and towards them when they fall behind, blinking to them from afar, and casts what is ready and fits. Light
// spells (sparks, bolts) he flicks often; up close he strikes with his staff or blinks away; between his big spells he
// takes a breath, never the same big spell twice running, and an ultimate never soon after another. From his second
// phase he flies (WizardFlight) and now and then, or when struck, blinks to another spot in the air (AIRSTEP). When a
// phase breaks he drops whatever he was doing and the next phase takes him (ASCEND, UNBIND, AWAKEN, APOTHEOSIS), and
// its own great spell comes first; caught out among his mirror images, or when his ward breaks, he is stunned and drops
// to the ground. Called by the ritual, he first comes down out of his rift (ARRIVE). Among several players he turns
// on another now and then.
final class WizardMoves extends Goal {
    // The breath after a big spell, in each phase.
    private static final int[] BREATH = { 0, 55, 46, 38, 31, 25 };
    private static final int ULTIMATE_GAP = 180;
    // How much sooner his spells are ready again in each phase.
    private static final double[] QUICKER = { 1.0, 1.0, 0.88, 0.77, 0.67, 0.58 };
    static final double NEAR = 6.5;
    private static final double FAR = 15.0;
    private static final double BLINK_FROM = 22.0;
    private static final int MOST_FAMILIARS = 2;
    // How fast a skip glides (blocks a tick at its start).
    private static final double SKIP_SPEED = 0.95;
    // Ticks out of sight of his target before he blinks to where he sees it.
    private static final int BLIND = 40;
    // Ticks at least before he may turn on another player.
    private static final int FOCUS = 120;

    private final Wizard wizard;
    private final WizardFlight flight;
    private final int[] ready = new int[WizardMove.KINDS + 1];
    private int kind;
    private int t;
    private int nextLight = 20;
    // Ticks he has not seen his target: walled in, he blinks out once it lasts.
    private int blind;
    private int nextBig = 40;
    private int nextFocus = FOCUS;
    private int nextUltimate;
    private int lastBig;
    private int side = 1;
    private int sideUntil;
    private int pendingPhase;
    private int opening;
    // Struck in the air: he blinks away at the next chance.
    private boolean dodge;
    private Vec3 aim = Vec3.ZERO;
    private Vec3 skip = Vec3.ZERO;
    @Nullable
    private WizardMirrors mirrors;

    WizardMoves(Wizard wizard) {
        this.wizard = wizard;
        this.flight = new WizardFlight(wizard);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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

    @Override
    public void stop() {
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
            if (this.t == WizardMove.release(this.kind)) {
                if (this.kind == WizardMove.MIRRORS && target != null) {
                    this.mirrors = WizardMirrors.begin(level, this.wizard, target);
                    return;
                }
                if (this.kind == WizardMove.AIRSTEP) {
                    this.flight.airstep(level, target);
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
        int now = this.wizard.tickCount;
        double far = this.wizard.distanceTo(target);
        double gap = MonsterBlows.gap(this.wizard, target);
        boolean sees = this.wizard.getSensing().hasLineOfSight(target);
        this.blind = sees ? 0 : this.blind + 1;
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
    // the moves that carry him (the comet, the phantom strike, the rise before the supernova) and the fall as he reels.
    private void fly(ServerLevel level, @Nullable LivingEntity target) {
        switch (this.kind) {
            case WizardMove.SKIP -> this.glide();
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

    private boolean ready(int kind) {
        return this.wizard.tickCount >= this.ready[kind] && WizardMove.phase(kind) <= this.wizard.phase();
    }

    private int pick(LivingEntity target, double gap, double far, boolean sees, int now) {
        RandomSource random = this.wizard.getRandom();
        if (this.wizard.flies()) {
            int air = this.pickInAir(target, gap, sees, random);
            if (air >= 0) {
                return air;
            }
        } else if (gap < 2.2) {
            if (this.ready(WizardMove.STRIKE) && (random.nextInt(4) != 0 || !this.ready(WizardMove.BLINK))) {
                return WizardMove.STRIKE;
            }
            if (this.ready(WizardMove.BLINK)) {
                return WizardMove.BLINK;
            }
        }
        if (!this.wizard.flies()) {
            if (gap < 3.5 && this.ready(WizardMove.SKIP) && random.nextInt(2) == 0) {
                return this.skipFrom(target, true);
            }
            if (gap < 3.5 && this.ready(WizardMove.BLINK) && random.nextInt(3) == 0) {
                return WizardMove.BLINK;
            }
        }
        if (far > BLINK_FROM && this.ready(WizardMove.BLINK)) {
            return WizardMove.BLINK;
        }
        if (!sees) {
            return this.blind > BLIND && this.ready(WizardMove.BLINK) ? WizardMove.BLINK : 0;
        }
        if (now >= this.nextBig) {
            int big = this.big(far, random);
            if (big != 0) {
                return big;
            }
        }
        if (!this.wizard.flies() && this.ready(WizardMove.SKIP) && (far > FAR + 2.0 || random.nextInt(6) == 0)) {
            return this.skipFrom(target, far < NEAR);
        }
        boolean bolt = this.ready(WizardMove.BOLT) && far < 26.0;
        boolean spark = this.ready(WizardMove.SPARK) && far < 24.0;
        if (bolt && (!spark || random.nextInt(2) == 0)) {
            return WizardMove.BOLT;
        }
        return spark ? WizardMove.SPARK : 0;
    }

    // In the air: his staff only on a foe at his height, else away into the air from one who comes close, and now and
    // then a blink elsewhere for no reason they can see. -1 to pick as on the ground.
    private int pickInAir(LivingEntity target, double gap, boolean sees, RandomSource random) {
        double below = this.wizard.getY() + this.wizard.hover() - target.getY();
        if (gap < 2.4 && Math.abs(below) < 1.6 && this.ready(WizardMove.STRIKE) && random.nextInt(3) != 0) {
            return WizardMove.STRIKE;
        }
        if (gap < 4.0 && this.ready(WizardMove.AIRSTEP) && random.nextInt(2) == 0) {
            return WizardMove.AIRSTEP;
        }
        if (sees && this.ready(WizardMove.AIRSTEP) && random.nextInt(14) == 0) {
            return WizardMove.AIRSTEP;
        }
        return -1;
    }

    // A quick glide: away and aside when they are close, towards and aside when they are far, else round them.
    private int skipFrom(LivingEntity target, boolean away) {
        Vec3 to = target.position().subtract(this.wizard.position()).multiply(1.0, 0.0, 1.0);
        double far = to.length();
        Vec3 ahead = far < 1.0E-3 ? new Vec3(0.0, 0.0, 1.0) : to.scale(1.0 / far);
        Vec3 aside = new Vec3(-ahead.z, 0.0, ahead.x).scale(this.wizard.getRandom().nextBoolean() ? 1.0 : -1.0);
        double toward = away ? -0.75 : far > FAR ? 0.8 : 0.0;
        this.skip = ahead.scale(toward).add(aside.scale(away ? 0.65 : 1.0)).normalize();
        return WizardMove.SKIP;
    }

    // The glide itself: fast at first, slowing to a stop.
    private void glide() {
        double speed = SKIP_SPEED * (1.0 - this.t / (double) WizardMove.length(WizardMove.SKIP));
        Vec3 motion = this.wizard.getDeltaMovement();
        this.wizard.setDeltaMovement(this.skip.x * speed, Math.max(motion.y, 0.0), this.skip.z * speed);
        this.wizard.hurtMarked = true;
    }

    // A big spell that is ready and fits, picked by weight; none twice running. A phase's own spell comes first.
    private int big(double far, RandomSource random) {
        int now = this.wizard.tickCount;
        if (this.opening != 0 && this.ready(this.opening) && this.fits(this.opening, far)) {
            int opening = this.opening;
            this.opening = 0;
            return opening;
        }
        double[] weight = new double[WizardMove.KINDS + 1];
        double total = 0.0;
        for (int kind = 1; kind <= WizardMove.KINDS; kind++) {
            if (!WizardMove.big(kind) || kind == this.lastBig || !this.ready(kind) || !this.fits(kind, far)
                    || WizardMove.ultimate(kind) && now < this.nextUltimate) {
                continue;
            }
            weight[kind] = this.weight(kind);
            total += weight[kind];
        }
        if (total <= 0.0) {
            return 0;
        }
        double roll = random.nextDouble() * total;
        for (int kind = 1; kind <= WizardMove.KINDS; kind++) {
            roll -= weight[kind];
            if (weight[kind] > 0.0 && roll < 0.0) {
                return kind;
            }
        }
        return 0;
    }

    private double weight(int kind) {
        double base = switch (kind) {
            case WizardMove.FIREBALL, WizardMove.NOVA -> 3.0;
            case WizardMove.LIGHTNING, WizardMove.MIRRORS, WizardMove.ORBIT, WizardMove.LANCES, WizardMove.COMET,
                    WizardMove.INFERNO, WizardMove.PHANTOM, WizardMove.TENDRILS, WizardMove.SPIRAL,
                    WizardMove.SUPERNOVA -> 2.5;
            case WizardMove.FAMILIARS, WizardMove.WARP -> 1.5;
            default -> 2.0;
        };
        return WizardMove.ultimate(kind) ? base * 1.5 : base;
    }

    private boolean fits(int kind, double far) {
        return switch (kind) {
            case WizardMove.FIREBALL -> far > 4.0 && far < 24.0;
            case WizardMove.BLIZZARD -> far > 3.0 && far < 16.0;
            case WizardMove.CHAINS, WizardMove.RUNES, WizardMove.MIRRORS -> far < 16.0;
            case WizardMove.LIGHTNING, WizardMove.LIFT -> far < 14.0;
            case WizardMove.STARFALL -> far < 28.0;
            case WizardMove.FAMILIARS -> ArcaneFamiliar.owned(this.wizard) < MOST_FAMILIARS;
            case WizardMove.BEAM -> far > 4.0 && far < 24.0;
            case WizardMove.METEOR -> far < 30.0;
            case WizardMove.WARP -> far < 10.0;
            case WizardMove.ORBIT, WizardMove.INFERNO, WizardMove.SPIRAL -> far < 19.0;
            case WizardMove.LANCES -> far > 4.0 && far < 28.0;
            case WizardMove.NOVA -> far < 8.5;
            case WizardMove.TORNADO -> far > 5.0 && far < 20.0;
            case WizardMove.COMET -> far > 4.0 && far < 22.0 && this.wizard.flies();
            case WizardMove.PHANTOM -> far < 20.0 && this.wizard.flies();
            case WizardMove.TENDRILS, WizardMove.DRAIN -> far < 16.0;
            case WizardMove.VOID_ORB -> far > 5.0 && far < 24.0;
            case WizardMove.SUPERNOVA -> far < 36.0 && this.wizard.flies();
            default -> false;
        };
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

    private int again(int kind) {
        double quicker = QUICKER[Mth.clamp(this.wizard.phase(), 1, WizardMove.PHASES)]
                * (this.wizard.hasted() ? 0.5 : 1.0);
        return (int) (WizardMove.again(kind) * quicker);
    }

    // The move is over: when he may cast it again and his next spells.
    private void finish() {
        int now = this.wizard.tickCount;
        int done = this.kind;
        this.ready[done] = now + this.again(done);
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
            this.nextUltimate = now;
            this.nextBig = now + 20;
        } else if (done == WizardMove.STUNNED) {
            this.wizard.setNoGravity(this.wizard.flies());
        } else if (done == WizardMove.ARRIVE) {
            this.nextLight = now + 10;
            this.nextBig = now + 40;
        }
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
            this.kind = 0;
            this.wizard.start(0);
        }
    }
}
