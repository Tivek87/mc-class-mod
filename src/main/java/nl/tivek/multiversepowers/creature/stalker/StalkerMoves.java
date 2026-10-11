package nl.tivek.multiversepowers.creature.stalker;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.Bleeding;
import nl.tivek.multiversepowers.creature.MonsterBlows;

// How the Stalker hunts. Watched, it never stays in sight: it runs for the nearest tree trunk or bush that hides it
// from the watcher's eye, presses itself behind the trunk or folds down small in the bush, and blinks away behind their
// back if there is none or they keep looking. Unwatched it creeps closer from cover to cover, now and then taking the
// shape of an animal grazing near by and ambling up as one. Close behind someone who is not looking (or within reach of
// its bush or its borrowed shape) it LUNGEs: a leap and a rending blow that leaves them bleeding; it fights a moment
// with SWIPEs of its claws, then BLINKs away to start again. REVEAL throws off a borrowed shape; FOLD takes it down
// into a bush and RISE up out of it again.
public final class StalkerMoves extends Goal {
    public static final int LUNGE = 1;
    public static final int SWIPE = 2;
    public static final int BLINK = 3;
    public static final int REVEAL = 4;
    public static final int FOLD = 5;
    public static final int RISE = 6;
    public static final int KINDS = 6;
    public static final int LEAP = 6;
    public static final int LUNGE_HIT = 9;
    public static final int LUNGE_END = 22;
    public static final int SWIPE_FIRST = 7;
    public static final int SWIPE_SECOND = 13;
    public static final int SWIPE_END = 20;
    public static final int VANISH = 8;
    public static final int BLINK_END = 16;
    public static final int REVEAL_END = 10;
    public static final int FOLD_END = 8;
    public static final int RISE_END = 10;
    // Seen within this angle of where someone looks (its cosine), it counts as watched.
    private static final double WATCHED = 0.72;
    // Someone looking further away from it than this (the cosine) has their back to it.
    private static final double BACK_TURNED = 0.1;
    private static final double STRIKE_RANGE = 6.5;
    private static final double REACH = 2.4;
    private static final double SHAPE_RANGE = 22.0;
    private static final int FIGHT = 70;
    // This near the cover it made for, it is there.
    private static final double ARRIVED = 0.35;

    private final Stalker stalker;
    private int kind;
    private int t;
    private int watched;
    private int unwatched;
    private boolean landed;
    private boolean lungeNext;
    private int fightUntil;
    private int hiddenSince;
    private int nextSearch;
    private int headingSince;
    private int nextShape = 200;
    private int swipeReady;
    @Nullable
    private StalkerCover.Spot heading;

    StalkerMoves(Stalker stalker) {
        this.stalker = stalker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.stalker.getTarget();
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
    public void stop() {
        if (this.kind != 0) {
            this.stalker.start(0);
        }
        this.kind = 0;
        this.heading = null;
        this.stalker.getNavigation().stop();
    }

    // Struck: a borrowed shape or a hiding place is given up and it fights back for a moment.
    void hurt() {
        if (this.kind == BLINK || this.kind == REVEAL) {
            return;
        }
        int now = this.stalker.tickCount;
        if (this.stalker.disguise() != null || this.stalker.hiding() == Stalker.BUSH) {
            this.lungeNext = false;
            this.begin(REVEAL);
        }
        this.stalker.hide(Stalker.OPEN);
        this.heading = null;
        this.fightUntil = this.stalker.getHealth() < this.stalker.getMaxHealth() * 0.4F ? now : now + FIGHT;
    }

    @Override
    public void tick() {
        LivingEntity target = this.stalker.getTarget();
        if (this.kind != 0) {
            this.t++;
            this.go(target);
            return;
        }
        if (target == null || !(this.stalker.level() instanceof ServerLevel level)) {
            return;
        }
        int now = this.stalker.tickCount;
        double far = this.stalker.distanceTo(target);
        boolean seen = watching(target, this.stalker);
        this.watched = seen ? this.watched + 1 : 0;
        // Hidden, it stays put while they look its way, even with the cover between.
        boolean facing = seen || facing(target, this.stalker);
        this.unwatched = facing ? 0 : this.unwatched + 1;
        boolean back = !seen && turnedAway(target, this.stalker);
        if (this.stalker.disguise() != null) {
            this.amble(level, target, far, seen);
        } else if (this.stalker.hiding() == Stalker.BUSH) {
            this.inBush(target, far, seen, back);
        } else if (this.stalker.hiding() == Stalker.TRUNK) {
            this.atTrunk(target, far, seen, back, now);
        } else if (now < this.fightUntil) {
            this.fight(target, far, now);
        } else if (this.fightUntil != 0) {
            // The fight is over: gone again, to start the hunt afresh.
            this.fightUntil = 0;
            this.begin(BLINK);
        } else if (this.arrived(now)) {
            return;
        } else if (seen) {
            this.flee(level, target, far, now);
        } else {
            this.creep(level, target, far, back, now);
        }
    }

    // In an animal's shape: wander up to them as an animal would, slowly and not quite straight, never staring.
    private void amble(ServerLevel level, LivingEntity target, double far, boolean seen) {
        if (far < 3.2) {
            this.lungeNext = true;
            this.begin(REVEAL);
            return;
        }
        if (far > 40.0 && !seen) {
            this.stalker.disguise(null);
            return;
        }
        if (this.stalker.getNavigation().isDone() || this.stalker.tickCount % 40 == 0) {
            Vec3 aside = new Vec3(this.stalker.getRandom().nextDouble() - 0.5, 0.0,
                    this.stalker.getRandom().nextDouble() - 0.5).scale(Math.min(6.0, far * 0.4));
            Vec3 to = target.position().lerp(this.stalker.position(), 0.35).add(aside);
            this.stalker.getNavigation().moveTo(to.x, to.y, to.z, 0.7);
        }
    }

    // Folded in a bush: perfectly still, its eyes on them, until they come too close or turn their back near by.
    private void inBush(LivingEntity target, double far, boolean seen, boolean back) {
        this.stalker.getNavigation().stop();
        this.stalker.getLookControl().setLookAt(target, 10.0F, 10.0F);
        if (far < 3.0 || back && far < STRIKE_RANGE) {
            this.lungeNext = true;
            this.begin(REVEAL);
        } else if (seen && this.watched > 90 && far < 10.0) {
            this.begin(BLINK);
        } else if (this.unwatched > 60 + this.stalker.getRandom().nextInt(80)) {
            this.begin(RISE);
        }
    }

    // Pressed behind a trunk: it peeks round it; it leaves it to strike, or to creep on once they look away long.
    private void atTrunk(LivingEntity target, double far, boolean seen, boolean back, int now) {
        this.stalker.getNavigation().stop();
        this.stalker.getLookControl().setLookAt(target, 20.0F, 20.0F);
        if (back && far < STRIKE_RANGE) {
            this.stalker.hide(Stalker.OPEN);
            this.begin(LUNGE);
        } else if (seen && this.watched > 70 && far < 9.0) {
            this.stalker.hide(Stalker.OPEN);
            this.begin(BLINK);
        } else if (this.unwatched > 40 && now - this.hiddenSince > 40 + this.stalker.getRandom().nextInt(60)) {
            this.stalker.hide(Stalker.OPEN);
        }
    }

    private void fight(LivingEntity target, double far, int now) {
        this.stalker.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (far < REACH + 0.4 && now >= this.swipeReady) {
            this.begin(SWIPE);
        } else if (this.stalker.getNavigation().isDone() || now % 5 == 0) {
            this.stalker.getNavigation().moveTo(target, 1.3);
        }
    }

    // Seen in the open: run for cover, or vanish when there is none or they keep looking; cornered, it fights.
    private void flee(ServerLevel level, LivingEntity target, double far, int now) {
        if (far < 3.5) {
            this.fightUntil = now + FIGHT;
            return;
        }
        if (this.watched > 40) {
            this.begin(BLINK);
            return;
        }
        if (this.heading == null && now >= this.nextSearch) {
            this.nextSearch = now + 10;
            this.head(StalkerCover.find(level, this.stalker, this.stalker.position(), target, 10, 5.0, 40.0));
            if (this.heading == null && this.watched > 12) {
                this.begin(BLINK);
                return;
            }
        }
        this.walk(1.7);
    }

    // Unseen: on to cover it was making for, hiding there; else close in from cover to cover, or take an animal's
    // shape; and strike once close behind them.
    private void creep(ServerLevel level, LivingEntity target, double far, boolean back, int now) {
        if (this.heading != null) {
            this.walk(1.25);
            return;
        }
        if (far <= STRIKE_RANGE && back) {
            this.begin(LUNGE);
            return;
        }
        // Their back is turned close by: no more cover, straight in.
        if (back && far < 12.0) {
            if (this.stalker.getNavigation().isDone() || now % 5 == 0) {
                this.stalker.getNavigation().moveTo(target, 1.0);
            }
            return;
        }
        if (far > 14.0 && now >= this.nextShape && this.takeShape(level, target)) {
            return;
        }
        if (now >= this.nextSearch) {
            this.nextSearch = now + 25;
            Vec3 between = this.stalker.position().lerp(target.position(), 0.5);
            this.head(StalkerCover.find(level, this.stalker, between, target, 7, 4.0, Math.max(5.0, far - 1.5)));
            if (this.heading != null) {
                return;
            }
        }
        if (far > 3.0 && (this.stalker.getNavigation().isDone() || now % 10 == 0)) {
            this.stalker.getNavigation().moveTo(target, far < 10.0 ? 0.8 : 1.1);
        }
    }

    // At the cover it made for: pressed to the trunk, or folding down into the bush.
    private boolean arrived(int now) {
        StalkerCover.Spot spot = this.heading;
        if (spot == null || this.stalker.position().distanceTo(spot.at()) >= ARRIVED) {
            return false;
        }
        Vec3 at = spot.at();
        this.heading = null;
        this.stalker.getNavigation().stop();
        this.stalker.moveTo(at.x, at.y, at.z, this.stalker.getYRot(), this.stalker.getXRot());
        if (spot.how() == Stalker.BUSH) {
            this.begin(FOLD);
        } else {
            this.stalker.hide(Stalker.TRUNK);
        }
        this.hiddenSince = now;
        return true;
    }

    // On to the cover it makes for: by path, and the last steps straight at it (a path only gets within a block).
    // Cover it cannot reach in time is given up.
    private void walk(double speed) {
        StalkerCover.Spot spot = this.heading;
        if (spot == null) {
            return;
        }
        int now = this.stalker.tickCount;
        if (now - this.headingSince > 120) {
            this.heading = null;
            return;
        }
        Vec3 at = spot.at();
        if (this.stalker.position().distanceTo(at) < 1.8) {
            this.stalker.getNavigation().stop();
            this.stalker.getMoveControl().setWantedPosition(at.x, at.y, at.z, speed * 0.7);
        } else if (this.stalker.getNavigation().isDone() || now % 10 == 0) {
            this.stalker.getNavigation().moveTo(at.x, at.y, at.z, speed);
        }
    }

    private void head(@Nullable StalkerCover.Spot spot) {
        this.heading = spot;
        this.headingSince = this.stalker.tickCount;
    }

    // Becomes the nearest grown animal near by, while nobody watches.
    private boolean takeShape(ServerLevel level, LivingEntity target) {
        this.nextShape = this.stalker.tickCount + 300 + this.stalker.getRandom().nextInt(300);
        List<Animal> near = level.getEntitiesOfClass(Animal.class, this.stalker.getBoundingBox().inflate(SHAPE_RANGE),
                animal -> animal.isAlive() && !animal.isBaby());
        Animal model = near.stream().min(Comparator.comparingDouble(a -> a.distanceToSqr(this.stalker))).orElse(null);
        if (model == null) {
            return false;
        }
        this.stalker.smoke(level, this.stalker.position(), 10);
        this.stalker.disguise(model.getType());
        this.stalker.getNavigation().stop();
        return true;
    }

    private void begin(int kind) {
        this.kind = kind;
        this.t = 0;
        this.landed = false;
        this.stalker.getNavigation().stop();
        this.stalker.start(kind);
        if (this.stalker.level() instanceof ServerLevel level) {
            Vec3 at = this.stalker.position();
            switch (kind) {
                case LUNGE -> level.playSound(null, at.x, at.y, at.z, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE,
                        0.8F, 0.6F);
                case REVEAL -> {
                    this.stalker.disguise(null);
                    this.stalker.hide(Stalker.OPEN);
                    this.stalker.smoke(level, at, 18);
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.FOX_SCREECH, SoundSource.HOSTILE, 1.0F,
                            0.45F);
                }
                case BLINK -> {
                    this.stalker.hide(Stalker.OPEN);
                    this.heading = null;
                }
                case RISE -> this.stalker.hide(Stalker.OPEN);
                default -> {
                }
            }
        }
    }

    private void go(@Nullable LivingEntity target) {
        if (target != null && this.kind != BLINK) {
            this.stalker.getLookControl().setLookAt(target, 40.0F, 40.0F);
        }
        int now = this.stalker.tickCount;
        switch (this.kind) {
            case LUNGE -> {
                if (target != null && this.t == LEAP) {
                    Vec3 way = target.position().subtract(this.stalker.position());
                    Vec3 flat = new Vec3(way.x, 0.0, way.z);
                    double reach = flat.length();
                    if (reach > 1.0E-3) {
                        Vec3 jump = flat.scale(Math.min(1.25, 0.25 + reach * 0.16) / reach).add(0.0, 0.38, 0.0);
                        this.stalker.setDeltaMovement(jump);
                        this.stalker.hasImpulse = true;
                    }
                }
                if (target != null && !this.landed && this.t >= LUNGE_HIT && this.t <= LUNGE_HIT + 4
                        && MonsterBlows.gap(this.stalker, target) < REACH) {
                    this.landed = true;
                    this.rend(target, 1.6, 2);
                }
                if (this.t >= LUNGE_END) {
                    this.fightUntil = now + FIGHT / 2;
                    this.end();
                }
            }
            case SWIPE -> {
                if (target != null && (this.t == SWIPE_FIRST || this.t == SWIPE_SECOND)
                        && MonsterBlows.gap(this.stalker, target) < REACH + 0.3) {
                    this.rend(target, 1.0, 1);
                }
                if (this.t >= SWIPE_END) {
                    this.swipeReady = now + 6;
                    this.end();
                }
            }
            case BLINK -> {
                if (this.t == VANISH) {
                    this.vanish(target);
                }
                if (this.t >= BLINK_END) {
                    this.fightUntil = 0;
                    this.end();
                }
            }
            case REVEAL -> {
                if (this.t >= REVEAL_END) {
                    this.end();
                    if (this.lungeNext) {
                        this.lungeNext = false;
                        this.begin(LUNGE);
                    } else {
                        this.fightUntil = now + FIGHT;
                    }
                }
            }
            case FOLD -> {
                if (this.t >= FOLD_END) {
                    this.stalker.hide(Stalker.BUSH);
                    this.end();
                }
            }
            case RISE -> {
                if (this.t >= RISE_END) {
                    this.end();
                }
            }
            default -> this.end();
        }
    }

    private void end() {
        this.kind = 0;
        this.stalker.start(0);
    }

    private void rend(LivingEntity target, double share, int bleed) {
        Vec3 push = MonsterBlows.ahead(this.stalker).scale(0.5).add(0.0, 0.15, 0.0);
        if (MonsterBlows.hurt(target, MonsterBlows.source(this.stalker, MonsterBlows.REND),
                MonsterBlows.damage(this.stalker) * share, push)) {
            Bleeding.tear(target, 100, bleed);
        }
        this.stalker.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
    }

    // Gone in a puff of dark smoke, out again behind their back.
    private void vanish(@Nullable LivingEntity target) {
        if (!(this.stalker.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = this.stalker.position();
        Vec3 to = target == null ? null : StalkerCover.behindBack(level, this.stalker, target);
        if (to == null) {
            Vec3 away = new Vec3(this.stalker.getRandom().nextDouble() - 0.5, 0.0,
                    this.stalker.getRandom().nextDouble() - 0.5).normalize().scale(14.0);
            to = from.add(away);
        }
        this.stalker.smoke(level, from, 24);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.5F, 0.5F);
        boolean moved = this.stalker.randomTeleport(to.x, to.y, to.z, false);
        for (int tries = 0; !moved && tries < 8; tries++) {
            Vec3 away = new Vec3(this.stalker.getRandom().nextDouble() - 0.5, 0.0,
                    this.stalker.getRandom().nextDouble() - 0.5).normalize().scale(10.0 + tries);
            moved = this.stalker.randomTeleport(from.x + away.x, from.y + 3.0, from.z + away.z, false);
        }
        if (moved) {
            this.stalker.smoke(level, this.stalker.position(), 10);
        }
        this.watched = 0;
        this.nextSearch = 0;
    }

    // Whether `watcher` looks at it now: within their view and with nothing in between.
    static boolean watching(LivingEntity watcher, Stalker stalker) {
        Vec3 to = stalker.getBoundingBox().getCenter().subtract(watcher.getEyePosition());
        double far = to.length();
        if (far < 1.0E-3) {
            return true;
        }
        return to.scale(1.0 / far).dot(watcher.getLookAngle()) > WATCHED && watcher.hasLineOfSight(stalker);
    }

    private static boolean facing(LivingEntity watcher, Stalker stalker) {
        Vec3 to = stalker.getBoundingBox().getCenter().subtract(watcher.getEyePosition());
        return to.lengthSqr() < 1.0E-6 || to.normalize().dot(watcher.getLookAngle()) > WATCHED;
    }

    private static boolean turnedAway(LivingEntity watcher, Stalker stalker) {
        Vec3 to = stalker.position().subtract(watcher.position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        Vec3 look = watcher.getLookAngle();
        Vec3 lookFlat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-4 || lookFlat.lengthSqr() < 1.0E-4) {
            return false;
        }
        return flat.normalize().dot(lookFlat.normalize()) < BACK_TURNED;
    }
}
