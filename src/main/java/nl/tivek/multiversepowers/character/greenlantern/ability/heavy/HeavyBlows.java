package nl.tivek.multiversepowers.character.greenlantern.ability.heavy;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import static nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves.*;

// The heavy weapons' blows as they land, tick by tick through each move: the battleaxe's chops, earthbreaker, hook
// and whirlwind, the chainsaw's slashes, rend and impale.
abstract class HeavyBlows extends HeavyHits {
    private static final int WHIRL_EVERY = 5;
    private static final int REND_EVERY = 3;
    private static final int CHEW_EVERY = 2;
    // The earthbreaker drives him down this many ticks before it lands, and lands at the latest this long after.
    private static final int DRIVE = 2;
    private static final int LAND_LATE = 6;

    final int weapon;
    int move = FORM;
    int age;
    @Nullable
    LivingEntity caught;
    private boolean landed;

    HeavyBlows(ServerPlayer owner, int weapon) {
        super(owner);
        this.weapon = weapon;
    }

    final void begin(int next) {
        this.move = next;
        this.age = 0;
        this.landed = false;
        if (next != HOOK && next != REND_OUT && next != IMPALE) {
            this.caught = null;
        }
        Vec3 look = this.owner.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() > 1.0E-6) {
            this.ahead = flat.normalize();
        }
    }

    // What `move` does `age` ticks in.
    final void blows(ServerLevel level) {
        int hit = hit(this.weapon, this.move);
        if (this.weapon == AXE) {
            this.axe(level, hit);
        } else {
            this.saw(level, hit);
        }
    }

    private void axe(ServerLevel level, int hit) {
        switch (this.move) {
            case CHOP, CHOP_BACK, CLEAVE -> {
                if (this.age == hit) {
                    this.chop(level);
                }
            }
            case LEAP -> this.leap(level, hit);
            case HOOK -> {
                if (this.age == hit) {
                    this.hook(level);
                } else if (this.age == YANK) {
                    this.yank();
                }
            }
            case WHIRL -> {
                if (this.age >= LOOP_FROM && (this.age - LOOP_FROM) % WHIRL_EVERY == 0) {
                    this.whirl(level, false);
                }
            }
            case WHIRL_OUT -> {
                if (this.age == hit) {
                    this.whirl(level, true);
                }
            }
            default -> {
            }
        }
    }

    private void saw(ServerLevel level, int hit) {
        switch (this.move) {
            case REV, REV_BACK -> {
                if (this.age == hit) {
                    this.slash(level);
                }
            }
            case REND -> {
                if (this.age >= LOOP_FROM && (this.age - LOOP_FROM) % REND_EVERY == 0) {
                    this.rend(level);
                }
                if (this.age % 4 == 0) {
                    this.sound(level, this.owner.position(), SoundEvents.GRINDSTONE_USE, 0.7F, 1.7F);
                }
            }
            case REND_OUT -> this.caught = null;
            case IMPALE -> this.impale(level, hit);
            case GUARD -> {
                if (this.age % 8 == 0) {
                    this.sound(level, this.owner.position(), SoundEvents.GRINDSTONE_USE, 0.4F, 1.9F);
                }
            }
            default -> {
            }
        }
    }

    // A chop: everything in the arc before him; the overhead cleave a narrower, harder blow.
    private void chop(ServerLevel level) {
        boolean cleave = this.move == CLEAVE;
        double damage = value("axeDamage") * (cleave ? 1.5 : 1.0);
        Vec3 push = this.ahead.scale(cleave ? 0.3 : 0.7).add(0.0, cleave ? -0.1 : 0.2, 0.0);
        boolean any = false;
        for (LivingEntity living : this.arc(level, value("axeReach"), cleave ? 0.6 : 0.15)) {
            any |= this.strike(level, living, damage, push, false);
        }
        Vec3 front = this.owner.position().add(0.0, cleave ? 0.3 : 1.1, 0.0).add(this.ahead.scale(1.6));
        ParticleFx.cloud(level, ParticleTypes.SWEEP_ATTACK, front, 1, 0.1, 0.0);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), front, 8, 0.6, 0.05);
        this.sound(level, front, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, cleave ? 0.6F : 0.8F);
        if (any) {
            this.sound(level, front, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.7F);
            this.sound(level, front, SoundEvents.AMETHYST_BLOCK_HIT, 0.8F, 0.6F);
        }
    }

    // Up off the ground and forward, then driven down: where the head buries itself the ground splits ahead in a line
    // that throws everything on it up and down.
    private void leap(ServerLevel level, int hit) {
        this.owner.fallDistance = 0.0F;
        if (this.age == LEAP_OFF) {
            this.owner.setDeltaMovement(this.ahead.scale(0.75).add(0.0, 0.5, 0.0));
            this.owner.hurtMarked = true;
            this.sound(level, this.owner.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 0.5F);
        }
        if (this.landed || this.age < hit - DRIVE) {
            return;
        }
        boolean ground = !level.noCollision(this.owner, this.owner.getBoundingBox().move(0.0, -0.25, 0.0));
        if (!ground && this.age < hit + LAND_LATE) {
            Vec3 now = this.owner.getDeltaMovement();
            this.owner.setDeltaMovement(now.x * 0.4, -1.1, now.z * 0.4);
            this.owner.hurtMarked = true;
            return;
        }
        this.landed = true;
        Vec3 start = this.owner.position().add(this.ahead.scale(1.2));
        Vec3 end = start.add(this.ahead.scale(value("leapLength")));
        double damage = value("leapDamage");
        Vec3 push = this.ahead.scale(0.3).add(0.0, 0.85, 0.0);
        for (LivingEntity living : this.line(level, start.add(0.0, 0.5, 0.0), end.add(0.0, 0.5, 0.0), 1.1)) {
            this.strike(level, living, damage, push, true);
        }
        for (LivingEntity living : this.around(level, start, 2.0)) {
            this.strike(level, living, damage * 0.6, push, true);
        }
        double length = start.distanceTo(end);
        for (double d = 0.0; d <= length; d += 1.0) {
            Vec3 at = start.add(this.ahead.scale(d)).add(0.0, 0.2, 0.0);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), at, 5, 0.3, 0.12);
            ParticleFx.cloud(level, ParticleTypes.CLOUD, at, 2, 0.3, 0.05);
        }
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), start.add(0.0, 0.15, 0.0), 24, 0.5);
        this.sound(level, start, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.2F, 0.8F);
        this.sound(level, start, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 0.5F);
        this.sound(level, start, SoundEvents.GENERIC_EXPLODE.value(), 0.4F, 1.4F);
    }

    // The beard of the axe catches the creature ahead...
    private void hook(ServerLevel level) {
        LivingEntity target = this.ahead(level, value("hookReach"));
        Vec3 front = this.owner.getEyePosition().add(this.owner.getLookAngle().scale(2.0));
        this.sound(level, front, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.1F);
        if (target == null || !this.strike(level, target, value("hookDamage"), Vec3.ZERO, false)) {
            return;
        }
        this.caught = target;
        this.sound(level, target.position(), SoundEvents.CHAIN_HIT, 1.0F, 0.7F);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), target.getBoundingBox().getCenter(), 8, 0.3,
                0.05);
    }

    // ...and yanks it off its feet to him.
    private void yank() {
        LivingEntity target = this.caught;
        this.caught = null;
        if (target == null || !target.isAlive() || target.distanceTo(this.owner) > value("hookReach") + 3.0) {
            return;
        }
        Vec3 to = this.owner.position().subtract(target.position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        double far = flat.length();
        if (far < 1.2) {
            return;
        }
        target.setDeltaMovement(flat.normalize().scale(Math.min(1.3, 0.25 + far * 0.22)).add(0.0, 0.32, 0.0));
        target.hasImpulse = true;
        target.hurtMarked = true;
        this.sound((ServerLevel) target.level(), target.position(), SoundEvents.CHAIN_BREAK, 0.8F, 1.2F);
    }

    // Round he goes with the axe at his waist, cutting all round; the last swing throws everything far out.
    private void whirl(ServerLevel level, boolean out) {
        double radius = value("axeWhirlRadius") * (out ? 1.3 : 1.0);
        double damage = value(out ? "axeBurstDamage" : "axeWhirlDamage");
        Vec3 middle = this.owner.position();
        for (LivingEntity living : this.around(level, middle, radius)) {
            Vec3 to = living.position().subtract(middle);
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            Vec3 way = flat.lengthSqr() < 1.0E-4 ? this.ahead : flat.normalize();
            this.strike(level, living, damage, way.scale(out ? 1.4 : 0.5).add(0.0, out ? 0.45 : 0.15, 0.0), false);
        }
        this.sound(level, middle, SoundEvents.PLAYER_ATTACK_SWEEP, out ? 1.2F : 0.7F, out ? 0.5F : 0.9F);
        if (out) {
            ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), middle.add(0.0, 0.8, 0.0), 32, 0.6);
            this.sound(level, middle, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 0.7F);
        } else {
            ParticleFx.cloud(level, ParticleTypes.SWEEP_ATTACK, middle.add(this.ahead.scale(1.5)).add(0.0, 0.9, 0.0),
                    1, 0.6, 0.0);
        }
    }

    // A fast diagonal slash: the teeth dig into everything they meet, slowing it a moment.
    private void slash(ServerLevel level) {
        double damage = value("sawDamage");
        Vec3 front = this.owner.position().add(0.0, 1.0, 0.0).add(this.ahead.scale(1.5));
        for (LivingEntity living : this.arc(level, value("sawReach"), 0.3)) {
            if (this.strike(level, living, damage, this.ahead.scale(0.25).add(0.0, 0.05, 0.0), false)) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1), this.owner);
                this.sparks(level, living.getBoundingBox().getCenter(), 10);
                this.sound(level, living.position(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, 1.0F, 1.4F);
            }
        }
        this.sound(level, front, SoundEvents.GRINDSTONE_USE, 1.0F, 1.5F);
        this.sound(level, front, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.3F);
    }

    // Held into the creature ahead, grinding it where it stands, bite after bite.
    private void rend(ServerLevel level) {
        double reach = value("sawReach");
        LivingEntity target = this.caught;
        if (target != null && (!target.isAlive() || target.distanceTo(this.owner) > reach + 1.5)) {
            target = null;
        }
        if (target == null) {
            target = this.ahead(level, reach);
        }
        this.caught = target;
        if (target == null) {
            return;
        }
        this.strike(level, target, value("rendDamage"), Vec3.ZERO, false);
        pin(target);
        this.sparks(level, target.getBoundingBox().getCenter(), 6);
        this.sound(level, target.position(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, 0.8F, 1.2F
                + 0.3F * this.owner.getRandom().nextFloat());
    }

    // Thrust into the creature ahead, chewed a moment, then thrown off the tip.
    private void impale(ServerLevel level, int hit) {
        if (this.age == hit) {
            LivingEntity target = this.ahead(level, value("sawReach") + 0.8);
            this.sound(level, this.owner.position(), SoundEvents.GRINDSTONE_USE, 1.0F, 1.3F);
            if (target != null && this.strike(level, target, value("impaleDamage"), Vec3.ZERO, false)) {
                this.caught = target;
                pin(target);
            }
            return;
        }
        LivingEntity target = this.caught;
        if (target == null || !target.isAlive()) {
            return;
        }
        if (this.age < EJECT) {
            pin(target);
            if ((this.age - hit) % CHEW_EVERY == 0) {
                this.strike(level, target, value("impaleDamage") * 0.25, Vec3.ZERO, false);
                pin(target);
                this.sparks(level, target.getBoundingBox().getCenter(), 5);
            }
        } else if (this.age == EJECT) {
            this.caught = null;
            this.strike(level, target, value("impaleEjectDamage"), this.ahead.scale(1.6).add(0.0, 0.55, 0.0), true);
            this.sparks(level, target.getBoundingBox().getCenter(), 16);
            this.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.7F);
            this.sound(level, target.position(), SoundEvents.GRINDSTONE_USE, 1.0F, 2.0F);
        }
    }

    final void sparks(ServerLevel level, Vec3 at, int count) {
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 0.8F), at, count, 0.2, 0.3);
        ParticleFx.cloud(level, ParticleTypes.CRIT, at, count / 2 + 1, 0.2, 0.4);
    }
}
