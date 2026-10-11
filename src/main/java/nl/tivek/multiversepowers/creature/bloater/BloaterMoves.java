package nl.tivek.multiversepowers.creature.bloater;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import nl.tivek.multiversepowers.creature.MonsterBlows;

// The Bloater's moves on what it hunts. From 7 blocks and further it stands and SPITs a ball of acid slime every 1.5 to
// 2 seconds, coming nearer only while it is far or out of sight. Closer than 7 blocks, or after 4 to 7 spits, it
// SHRIEKs, then RUSHes at it at full speed, swelling and hissing, and bursts 3 seconds later wherever it is. Every game
// draws each move from its start on the same timeline.
public final class BloaterMoves extends Goal {
    public static final int SPIT = 1;
    public static final int SHRIEK = 2;
    public static final int RUSH = 3;
    public static final int KINDS = 3;
    // The spit: its belly heaves, the ball leaves its mouth at SPAT, over at SPIT_END.
    public static final int SPAT = 14;
    public static final int SPIT_END = 22;
    public static final int SHRIEK_END = 16;
    // The rush lasts BLOW ticks, then it bursts.
    public static final int BLOW = 60;
    private static final double CLOSE = 7.0;
    private static final double SPIT_MOST = 24.0;
    private static final double NEARER = 12.0;
    private static final double RUSH_SPEED = 2.3;

    private final Bloater bloater;
    private int kind;
    private int t;
    private int spitReady = 20;
    private int spits;
    private int spitsBeforeRush;
    private int repath;

    BloaterMoves(Bloater bloater) {
        this.bloater = bloater;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.spitsBeforeRush = 4 + bloater.getRandom().nextInt(4);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.bloater.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.kind != 0 || this.canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.bloater.setAggressive(true);
    }

    @Override
    public void stop() {
        if (this.kind != 0 && this.kind != RUSH) {
            this.bloater.start(0);
            this.kind = 0;
        }
        this.bloater.setAggressive(false);
        this.bloater.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.bloater.getTarget();
        if (target != null) {
            this.bloater.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (this.kind != 0) {
            this.t++;
            this.go(target);
            return;
        }
        if (target == null) {
            return;
        }
        double far = MonsterBlows.gap(this.bloater, target);
        boolean seen = this.bloater.getSensing().hasLineOfSight(target);
        int now = this.bloater.tickCount;
        if (seen && (far < CLOSE || this.spits >= this.spitsBeforeRush)) {
            this.begin(SHRIEK);
        } else if (seen && far <= SPIT_MOST && now >= this.spitReady) {
            this.begin(SPIT);
        } else if ((far > NEARER || !seen) && --this.repath <= 0) {
            this.repath = 8;
            this.bloater.getNavigation().moveTo(target, 1.0);
        } else if (far <= NEARER && seen) {
            this.bloater.getNavigation().stop();
        }
    }

    private void begin(int kind) {
        this.kind = kind;
        this.t = 0;
        this.bloater.start(kind);
        if (kind != RUSH) {
            this.bloater.getNavigation().stop();
        }
        switch (kind) {
            case SPIT -> this.bloater.playSound(SoundEvents.SLIME_JUMP, 1.0F, 0.5F);
            case SHRIEK -> {
                this.bloater.playSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 1.6F, 1.5F);
                this.bloater.playSound(SoundEvents.GHAST_HURT, 1.0F, 0.6F);
            }
            case RUSH -> this.bloater.playSound(SoundEvents.CREEPER_PRIMED, 1.6F, 0.6F);
            default -> {
            }
        }
    }

    private void go(@Nullable LivingEntity target) {
        switch (this.kind) {
            case SPIT -> {
                if (this.t == SPAT && target != null && target.isAlive()) {
                    this.bloater.spit(target);
                    this.spits++;
                }
                if (this.t >= SPIT_END) {
                    this.spitReady = this.bloater.tickCount + 30 + this.bloater.getRandom().nextInt(11);
                    this.end();
                }
            }
            case SHRIEK -> {
                if (this.t >= SHRIEK_END) {
                    this.begin(RUSH);
                }
            }
            case RUSH -> this.rush(target);
            default -> this.end();
        }
    }

    // Swelling, hissing more and more, it runs at what it hunts, and after BLOW ticks it bursts.
    private void rush(@Nullable LivingEntity target) {
        ServerLevel level = (ServerLevel) this.bloater.level();
        if (target != null && target.isAlive() && --this.repath <= 0) {
            this.repath = 4;
            this.bloater.getNavigation().moveTo(target, RUSH_SPEED);
        }
        int often = this.t < BLOW / 2 ? 4 : this.t < BLOW * 5 / 6 ? 2 : 1;
        if (this.t % often == 0) {
            this.bloater.hiss(level);
        }
        if (this.t % 10 == 0) {
            this.bloater.playSound(SoundEvents.FIRE_EXTINGUISH, 0.4F + this.t / (float) BLOW * 0.6F, 0.6F + this.t
                    / (float) BLOW * 0.6F);
        }
        if (this.t >= BLOW) {
            this.kind = 0;
            this.bloater.explode();
        }
    }

    private void end() {
        this.kind = 0;
        this.bloater.start(0);
    }
}
