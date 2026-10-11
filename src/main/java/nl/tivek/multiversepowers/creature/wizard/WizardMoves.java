package nl.tivek.multiversepowers.creature.wizard;

import java.util.EnumSet;
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
// takes a breath, never the same big spell twice running, and an ultimate never soon after another. When a phase breaks
// he drops whatever he was doing and the next phase takes him (ASCEND, UNBIND), and its own great spell comes first;
// caught out among his mirror images he is stunned. Called by the ritual, he first comes down out of his rift (ARRIVE).
final class WizardMoves extends Goal {
    // The breath after a big spell, in each phase.
    private static final int[] BREATH = { 0, 55, 45, 34 };
    private static final int ULTIMATE_GAP = 180;
    // How much sooner his spells are ready again in each phase.
    private static final double[] QUICKER = { 1.0, 1.0, 0.85, 0.7 };
    private static final double NEAR = 6.5;
    private static final double FAR = 15.0;
    private static final double BLINK_FROM = 22.0;
    private static final int MOST_FAMILIARS = 2;
    // How fast a skip glides (blocks a tick at its start).
    private static final double SKIP_SPEED = 0.95;
    // Ticks out of sight of his target before he blinks to where he sees it.
    private static final int BLIND = 40;

    private final Wizard wizard;
    private final int[] ready = new int[WizardMove.KINDS + 1];
    private int kind;
    private int t;
    private int nextLight = 20;
    // Ticks he has not seen his target: walled in, he blinks out once it lasts.
    private int blind;
    private int nextBig = 40;
    private int nextUltimate;
    private int lastBig;
    private int side = 1;
    private int sideUntil;
    private int pendingPhase;
    private int opening;
    private Vec3 aim = Vec3.ZERO;
    private Vec3 skip = Vec3.ZERO;
    @Nullable
    private WizardMirrors mirrors;

    WizardMoves(Wizard wizard) {
        this.wizard = wizard;
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
        if (this.pendingPhase == 0 && this.wizard.phase() < 3) {
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
        }
    }

    // He dies: whatever he was doing ends, his images with it.
    void dying() {
        this.interrupt();
        this.kind = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.wizard.getTarget();
        if (this.pendingPhase != 0 && this.kind != WizardMove.ASCEND && this.kind != WizardMove.UNBIND
                && this.kind != WizardMove.ARRIVE) {
            this.interrupt();
            this.begin(this.pendingPhase == 2 ? WizardMove.ASCEND : WizardMove.UNBIND, target);
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
            if (this.kind == WizardMove.SKIP) {
                this.glide();
            }
            WizardCasts.during(level, this.wizard, this.kind, this.t, target);
            if (this.t == WizardMove.release(this.kind)) {
                if (this.kind == WizardMove.MIRRORS && target != null) {
                    this.mirrors = WizardMirrors.begin(level, this.wizard, target);
                    return;
                }
                WizardCasts.release(level, this.wizard, this.kind, target, this.aim);
                if (this.kind == WizardMove.ASCEND || this.kind == WizardMove.UNBIND) {
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
        this.keepAway(target, far, sees, now);
        if (now >= this.nextLight) {
            int pick = this.pick(target, gap, far, sees, now);
            if (pick != 0) {
                this.begin(pick, target);
            }
        }
    }

    private int length() {
        if (this.kind == WizardMove.BOLT && this.wizard.phase() > 1) {
            return WizardMove.length(WizardMove.BOLT) + WizardMove.BOLT_AGAIN * (this.wizard.phase() - 1);
        }
        return WizardMove.length(this.kind);
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
        if (gap < 2.2) {
            if (this.ready(WizardMove.STRIKE) && (random.nextInt(4) != 0 || !this.ready(WizardMove.BLINK))) {
                return WizardMove.STRIKE;
            }
            if (this.ready(WizardMove.BLINK)) {
                return WizardMove.BLINK;
            }
        }
        if (gap < 3.5 && this.ready(WizardMove.SKIP) && random.nextInt(2) == 0) {
            return this.skipFrom(target, true);
        }
        if (gap < 3.5 && this.ready(WizardMove.BLINK) && random.nextInt(3) == 0) {
            return WizardMove.BLINK;
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
        if (this.ready(WizardMove.SKIP) && (far > FAR + 2.0 || random.nextInt(6) == 0)) {
            return this.skipFrom(target, far < NEAR);
        }
        boolean bolt = this.ready(WizardMove.BOLT) && far < 26.0;
        boolean spark = this.ready(WizardMove.SPARK) && far < 24.0;
        if (bolt && (!spark || random.nextInt(2) == 0)) {
            return WizardMove.BOLT;
        }
        return spark ? WizardMove.SPARK : 0;
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
        for (int kind = WizardMove.FIREBALL; kind <= WizardMove.MIRRORS; kind++) {
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
        for (int kind = WizardMove.FIREBALL; kind <= WizardMove.MIRRORS; kind++) {
            roll -= weight[kind];
            if (weight[kind] > 0.0 && roll < 0.0) {
                return kind;
            }
        }
        return 0;
    }

    private double weight(int kind) {
        double base = switch (kind) {
            case WizardMove.FIREBALL -> 3.0;
            case WizardMove.LIGHTNING, WizardMove.MIRRORS -> 2.5;
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
        this.wizard.start(kind);
        WizardCasts.begin((ServerLevel) this.wizard.level(), this.wizard, kind);
    }

    private int again(int kind) {
        double quicker = QUICKER[Mth.clamp(this.wizard.phase(), 1, 3)] * (this.wizard.hasted() ? 0.5 : 1.0);
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
            this.nextBig = now + (int) (BREATH[Mth.clamp(this.wizard.phase(), 1, 3)] * hurry) + random.nextInt(15);
            this.nextLight = now + 8 + random.nextInt(6);
        } else {
            this.nextLight = now + (int) ((4 + random.nextInt(6)) * hurry);
        }
        if (WizardMove.ultimate(done)) {
            this.nextUltimate = now + ULTIMATE_GAP;
        }
        if (done == WizardMove.ASCEND) {
            this.ready[WizardMove.STARFALL] = now + 30;
            this.opening = WizardMove.STARFALL;
            this.nextBig = now + 20;
        } else if (done == WizardMove.UNBIND) {
            this.ready[WizardMove.MIRRORS] = now + 60;
            this.opening = WizardMove.MIRRORS;
            this.nextUltimate = now;
            this.nextBig = now + 20;
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
            this.ready[this.kind] = this.wizard.tickCount + this.again(this.kind);
            this.kind = 0;
            this.wizard.start(0);
        }
    }
}
