package nl.tivek.multiversepowers.creature.lurker;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.Bleeding;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The Lurker's moves on what it hunts. Unwatched and further than 6 blocks it STALKs, low, slow and unheard; seen, it
// runs. From 6 blocks it crouches and POUNCEs at the head, LANDing after; up close it slashes with one CLAW and the
// other by turns. Stared at for a second, aimed at with a bow, crossbow or trident, or shot, it DASHes aside in a puff
// of black smoke. Every claw that tears leaves its target bleeding a step deeper; one caught on a shield still tears
// through, less. The Screamer's ferals use its claws (claw) and its kinds of move.
public final class LurkerMoves extends Goal {
    public static final int CLAW_RIGHT = 1;
    public static final int CLAW_LEFT = 2;
    public static final int POUNCE = 3;
    public static final int LANDED = 4;
    public static final int DASH_RIGHT = 5;
    public static final int DASH_LEFT = 6;
    public static final int KINDS = 6;
    public static final int CLAW_HIT = 6;
    public static final int CLAW_END = 12;
    public static final int SPRING = 9;
    public static final int LAND_END = 8;
    public static final int DASH_END = 8;
    public static final int RISE_END = 30;
    private static final int AIR_MOST = 30;
    private static final double REACH = 1.6;
    private static final double POUNCE_FROM = 6.0;
    private static final double POUNCE_LEAST = 2.5;
    private static final int STARE = 20;
    private static final double STARED = 0.975;
    private static final double WATCHED = 0.85;

    private final Lurker lurker;
    private int kind;
    private int t;
    private int clawReady;
    private int pounceReady = 30;
    private int dashReady;
    private int stared;
    private int repath;
    private boolean right;
    private boolean struck;

    LurkerMoves(Lurker lurker) {
        this.lurker = lurker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.lurker.getTarget();
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
        this.lurker.setAggressive(true);
    }

    @Override
    public void stop() {
        if (this.kind != 0) {
            this.lurker.start(0);
        }
        this.kind = 0;
        this.lurker.stalk(false);
        this.lurker.setAggressive(false);
        this.lurker.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.lurker.getTarget();
        boolean flying = this.kind == POUNCE && this.t >= SPRING || this.kind == DASH_RIGHT || this.kind == DASH_LEFT;
        if (target != null && !flying) {
            this.lurker.getLookControl().setLookAt(target, 40.0F, 40.0F);
        }
        if (target != null) {
            this.watch(target);
        }
        if (this.kind != 0) {
            this.t++;
            this.go(target);
            return;
        }
        if (target == null) {
            return;
        }
        int now = this.lurker.tickCount;
        double far = MonsterBlows.gap(this.lurker, target);
        boolean seen = this.lurker.getSensing().hasLineOfSight(target);
        boolean standing = this.lurker.onGround();
        if (standing && now >= this.dashReady && (this.lurker.shotAt || this.stared >= STARE || aimed(target,
                this.lurker))) {
            this.dash(target);
            return;
        }
        if (far <= REACH && now >= this.clawReady) {
            this.right = !this.right;
            this.begin(this.right ? CLAW_RIGHT : CLAW_LEFT);
        } else if (far >= POUNCE_LEAST && far <= POUNCE_FROM && seen && standing && now >= this.pounceReady) {
            this.begin(POUNCE);
        } else if (far > REACH * 0.8 && --this.repath <= 0) {
            this.repath = 5;
            boolean creep = far > POUNCE_FROM && !watched(target, this.lurker, WATCHED);
            this.lurker.stalk(creep);
            this.lurker.getNavigation().moveTo(target, creep ? 0.6 : 1.35);
        }
    }

    // How long it has been stared at, counting down as fast once looked away from.
    private void watch(LivingEntity target) {
        if (watched(target, this.lurker, STARED)) {
            this.stared++;
        } else {
            this.stared = Math.max(0, this.stared - 2);
        }
    }

    // Whether `target` looks at the mob, within the cone `dot` sets (1 straight at it), and sees it.
    static boolean watched(LivingEntity target, Mob mob, double dot) {
        if (!(target instanceof Player)) {
            return false;
        }
        Vec3 to = mob.getBoundingBox().getCenter().subtract(target.getEyePosition());
        double far = to.length();
        if (far > 48.0) {
            return false;
        }
        if (far < 1.0E-3) {
            return true;
        }
        return target.getViewVector(1.0F).dot(to.scale(1.0 / far)) > dot && target.hasLineOfSight(mob);
    }

    // A bow drawn, a crossbow loaded or a trident raised, pointed at it.
    private static boolean aimed(LivingEntity target, Mob mob) {
        ItemStack held = target.getMainHandItem();
        boolean drawn = target.isUsingItem() && (target.getUseItem().getItem() instanceof BowItem
                || target.getUseItem().getItem() instanceof TridentItem || target.getUseItem().getItem()
                        instanceof CrossbowItem);
        boolean loaded = held.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(held);
        return (drawn || loaded) && watched(target, mob, 0.95);
    }

    private void begin(int kind) {
        this.kind = kind;
        this.t = 0;
        this.struck = false;
        this.lurker.getNavigation().stop();
        this.lurker.stalk(false);
        this.lurker.start(kind);
        switch (kind) {
            case POUNCE -> this.lurker.playSound(SoundEvents.SPIDER_AMBIENT, 1.0F, 0.5F);
            case CLAW_RIGHT, CLAW_LEFT -> this.lurker.playSound(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.4F, 1.6F);
            default -> {
            }
        }
    }

    private void go(@Nullable LivingEntity target) {
        int now = this.lurker.tickCount;
        switch (this.kind) {
            case CLAW_RIGHT, CLAW_LEFT -> {
                if (this.t == CLAW_HIT && target != null) {
                    this.slash(target);
                }
                if (this.t >= CLAW_END) {
                    this.clawReady = now + 3;
                    this.end();
                }
            }
            case POUNCE -> this.pounce(target);
            case LANDED -> {
                if (this.t >= LAND_END) {
                    this.pounceReady = now + 60;
                    this.end();
                }
            }
            case DASH_RIGHT, DASH_LEFT -> {
                if (this.t == DASH_END - 2) {
                    this.smoke(8);
                }
                if (this.t >= DASH_END) {
                    this.end();
                }
            }
            default -> this.end();
        }
    }

    private void end() {
        this.kind = 0;
        this.lurker.start(0);
    }

    // One claw rakes what stands in front of it.
    private void slash(LivingEntity target) {
        if (MonsterBlows.gap(this.lurker, target) <= REACH + 0.7) {
            Vec3 ahead = MonsterBlows.ahead(this.lurker);
            claw(this.lurker, target, MonsterBlows.damage(this.lurker), 1, ahead.scale(0.35).add(0.0, 0.2, 0.0));
        }
        this.lurker.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.5F);
    }

    // A claw's tear: the target bleeds a step deeper; caught on its shield, the claw still tears through, less hard.
    public static void claw(Mob by, LivingEntity target, double damage, int steps, Vec3 push) {
        DamageSource blow = by.damageSources().mobAttack(by);
        boolean landed;
        if (target.isDamageSourceBlocked(blow)) {
            landed = MonsterBlows.hurt(target, MonsterBlows.source(by, MonsterBlows.REND), damage * 0.4, push.scale(
                    0.5));
            by.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.7F);
        } else {
            landed = MonsterBlows.hurt(target, blow, damage, push);
        }
        if (landed) {
            Bleeding.tear(target, 100, steps);
            if (by.level() instanceof ServerLevel level) {
                Vec3 at = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
                ParticleFx.send(level, ParticleFx.dust(0x8A0B0B, 1.1F), at.x, at.y, at.z, 10, 0.25, 0.25, 0.25, 0.0);
                ParticleFx.send(level, ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.2);
            }
        }
    }

    // Crouched it springs at the head of what it hunts; whatever it hits in the air it tears, deep, and it drops off.
    private void pounce(@Nullable LivingEntity target) {
        if (this.t == SPRING) {
            Vec3 at = target == null ? this.lurker.position().add(MonsterBlows.ahead(this.lurker).scale(5.0))
                    : target.getEyePosition();
            Vec3 to = at.subtract(this.lurker.position());
            double flat = to.horizontalDistance();
            Vec3 way = flat < 1.0E-3 ? MonsterBlows.ahead(this.lurker) : new Vec3(to.x / flat, 0.0, to.z / flat);
            double speed = Mth.clamp(flat * 0.16, 0.5, 1.25);
            double up = Mth.clamp(0.42 + (to.y - 1.0) * 0.07, 0.3, 0.75);
            MonsterBlows.face(this.lurker, way);
            this.lurker.setDeltaMovement(way.x * speed, up, way.z * speed);
            this.lurker.hasImpulse = true;
            this.lurker.playSound(SoundEvents.RAVAGER_ATTACK, 0.8F, 1.6F);
            this.lurker.playSound(SoundEvents.PHANTOM_SWOOP, 0.9F, 1.3F);
            return;
        }
        if (this.t < SPRING) {
            return;
        }
        if (!this.struck && target != null && this.lurker.getBoundingBox().inflate(0.45).intersects(
                target.getBoundingBox())) {
            this.struck = true;
            Vec3 speed = this.lurker.getDeltaMovement();
            claw(this.lurker, target, MonsterBlows.damage(this.lurker) * 1.4, 2, new Vec3(speed.x * 0.7, 0.3,
                    speed.z * 0.7));
            this.lurker.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
            this.lurker.setDeltaMovement(speed.x * -0.25, 0.2, speed.z * -0.25);
        }
        if (this.t > SPRING + 2 && this.lurker.onGround() || this.t > SPRING + AIR_MOST) {
            this.begin(LANDED);
            if (this.lurker.level() instanceof ServerLevel level) {
                MonsterBlows.dust(level, this.lurker.position(), 8, 0.4);
            }
        }
    }

    // A dart to whichever side is free, out of a puff of black smoke that it leaves behind.
    private void dash(LivingEntity target) {
        Vec3 toward = MonsterBlows.toward(this.lurker, this.lurker.position(), target.position());
        Vec3 side = new Vec3(-toward.z, 0.0, toward.x);
        boolean right = this.lurker.getRandom().nextBoolean();
        AABB box = this.lurker.getBoundingBox();
        if (!this.lurker.level().noCollision(this.lurker, box.move(side.scale(right ? 2.5 : -2.5)))) {
            right = !right;
        }
        Vec3 way = side.scale(right ? 1.0 : -1.0);
        this.lurker.shotAt = false;
        this.stared = 0;
        this.dashReady = this.lurker.tickCount + 70;
        this.begin(right ? DASH_RIGHT : DASH_LEFT);
        this.smoke(22);
        this.lurker.setDeltaMovement(way.x * 1.25 + toward.x * 0.15, 0.25, way.z * 1.25 + toward.z * 0.15);
        this.lurker.hasImpulse = true;
        this.lurker.playSound(SoundEvents.PHANTOM_FLAP, 1.0F, 1.4F);
        this.lurker.playSound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 0.6F);
    }

    private void smoke(int count) {
        if (this.lurker.level() instanceof ServerLevel level) {
            Vec3 at = this.lurker.position().add(0.0, 0.6, 0.0);
            ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, count, 0.4, 0.35, 0.4, 0.02);
            ParticleFx.send(level, ParticleTypes.SQUID_INK, at.x, at.y, at.z, count / 2, 0.3, 0.3, 0.3, 0.05);
            ParticleFx.send(level, ParticleFx.dust(0x101010, 2.2F), at.x, at.y, at.z, count, 0.5, 0.4, 0.5, 0.0);
        }
    }
}
