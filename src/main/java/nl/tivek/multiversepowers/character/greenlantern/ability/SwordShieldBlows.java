package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.Construct;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

/**
 * The blows of the {@link SwordShield}: its cuts and thrusts, the stabs of the flurry, and the charge with its rams and
 * the slam of the shield that ends it; with what they need of it: whose it is, the move under way and its clock.
 */
abstract class SwordShieldBlows implements Effect {
    // How high up his body the moves strike from, as a part of his height.
    private static final double CHEST = 0.62;
    // A creature this close is struck whatever the arc; one further above or below his chest than this is out of reach.
    private static final double CLOSE = 0.9;
    private static final double TALL = 2.2;
    // How far to his sides a charge shoves creatures aside, in blocks, and how far ahead of him it looks for them.
    private static final double CHARGE_WIDE = 1.3;
    private static final double CHARGE_AHEAD = 1.4;
    // How far ahead of him the shield strikes the ground when a charge ends.
    private static final double SLAM_AHEAD = 1.3;
    // How close to the line of a stab of the flurry a creature has to be to be struck, in blocks.
    private static final double STAB_WIDE = 0.45;

    final ServerPlayer owner;
    final Set<Integer> shoved = new HashSet<>();
    int age;
    SwordMove move = SwordMove.EQUIP;
    int moveStart;
    boolean charging;
    @Nullable
    SwordMove lastRam;
    Vec3 way = new Vec3(0.0, 0.0, 1.0);

    SwordShieldBlows(ServerPlayer owner) {
        this.owner = owner;
    }

    /** The settings of the sword and shield: those of the Construct Wheel. */
    static CharacterAbility wheel() {
        return GameCharacter.GREEN_LANTERN.byName("construct_wheel");
    }

    /** The charge ends (he let go, or ran into a wall): he slams the shield into the ground before him. */
    boolean stopCharge() {
        if (!this.charging) {
            return false;
        }
        this.charging = false;
        this.move = SwordMove.SLAM;
        this.moveStart = this.age;
        return true;
    }

    /**
     * A cut or a thrust lands: everything fair game in its arc and within its reach, not behind a wall, is struck and
     * thrown the way the move goes. The end of a charge slams the shield into the ground instead.
     */
    void strike(ServerLevel level) {
        CharacterAbility wheel = wheel();
        if (this.move.kind() == SwordMove.Kind.SLAM) {
            this.slam(level, wheel);
            return;
        }
        Vec3 origin = this.owner.position().add(0.0, this.owner.getBbHeight() * CHEST, 0.0);
        Vec3 look = flat(this.owner.getLookAngle());
        Vec3 right = new Vec3(-look.z, 0.0, look.x);
        double damage = wheel.value("swordDamage") * this.move.power();
        double reach = this.move.reach() * wheel.value("swordReach") / 3.2;
        int struck = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin)
                .inflate(reach + 1.5), this::fair)) {
            Vec3 middle = target.getBoundingBox().getCenter();
            Vec3 to = middle.subtract(origin);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z) - target.getBbWidth() * 0.5;
            if (flat > reach || Math.abs(to.y) > TALL + target.getBbHeight() * 0.5) {
                continue;
            }
            double angle = Math.toDegrees(Math.atan2(to.dot(right), to.dot(look)));
            if (flat > CLOSE && !this.move.inArc(angle)) {
                continue;
            }
            if (level.clip(new ClipContext(origin, middle, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    this.owner)).getType() != HitResult.Type.MISS) {
                continue;
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) damage);
            push(target, this.move, look, right, to, 0.35);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, middle.x, middle.y, middle.z, 8, 0.2, 0.2, 0.2, 0.25);
            struck++;
        }
        Vec3 front = origin.add(look.scale(1.6));
        if (this.move != SwordMove.STAB && this.move != SwordMove.LUNGE) {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        this.sound(struck > 0 ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F,
                struck > 0 ? 1.0F : 1.5F);
        if (struck > 0) {
            this.sound(SoundEvents.AMETHYST_BLOCK_HIT, 1.0F, 1.2F);
        }
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 0.8F), front, 5, 0.3, 0.05);
    }

    /**
     * Throws a creature a cut or thrust struck the way the move goes: a cut across sweeps it aside, an uppercut throws
     * it up, a chop from above knocks it down, anything else straight away.
     */
    private static void push(LivingEntity target, SwordMove move, Vec3 look, Vec3 right, Vec3 to, double strength) {
        Vec3 away = new Vec3(to.x, 0.0, to.z);
        away = away.lengthSqr() < 1.0E-4 ? look : away.normalize();
        Vec3 push = switch (move) {
            case SLASH, LOW_SWEEP -> away.subtract(right).normalize();
            case BACKHAND -> away.add(right).normalize();
            case UPPERCUT -> away.scale(0.3).add(0.0, 1.0, 0.0);
            case OVERHEAD -> away.scale(0.6).add(0.0, -0.2, 0.0);
            default -> away;
        };
        double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        double lift = move == SwordMove.OVERHEAD ? 0.0 : 0.15;
        target.setDeltaMovement(target.getDeltaMovement().add(push.scale(strength * (1.0 - resist)))
                .add(0.0, lift * strength * (1.0 - resist), 0.0));
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    /**
     * Throws a creature in the way of a charge off to the side of him it stood on ({@code side} 1 for his right, -1 for
     * his left), the way the ram he threw at it goes: a punch of the shield sends it ahead and aside, a sweep flings it
     * far aside, from under it throws it up, the rim from above knocks it down and slows it, and a shoulder behind the
     * shield bowls it over hardest.
     */
    private static void ram(LivingEntity target, SwordMove ram, Vec3 way, Vec3 right, double side, double strength) {
        Vec3 aside = right.scale(side);
        Vec3 push = switch (ram) {
            case BASH -> way.scale(0.9).add(aside.scale(0.6)).add(0.0, 0.3, 0.0);
            case BASH_SWEEP, BASH_BACKHAND -> aside.scale(1.25).add(way.scale(0.3)).add(0.0, 0.32, 0.0);
            case BASH_UP -> aside.scale(0.6).add(way.scale(0.2)).add(0.0, 1.05, 0.0);
            case BASH_DOWN -> aside.scale(0.95).add(way.scale(0.2)).add(0.0, 0.05, 0.0);
            default -> aside.scale(1.35).add(way.scale(0.55)).add(0.0, 0.45, 0.0);
        };
        double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        target.setDeltaMovement(target.getDeltaMovement().add(push.scale(strength * (1.0 - resist))));
        target.hasImpulse = true;
        target.hurtMarked = true;
        if (ram == SwordMove.BASH_DOWN) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
        }
    }

    /** One stab of the flurry: straight along its own way, striking the first few creatures along it. */
    void stab(ServerLevel level, int k) {
        CharacterAbility wheel = wheel();
        double[] turn = SwordMove.stab(k);
        Vec3 way = Vec3.directionFromRotation(this.owner.getXRot() * 0.3F - (float) turn[1],
                this.owner.getYRot() + (float) turn[0]);
        Vec3 origin = this.owner.position().add(0.0, this.owner.getBbHeight() * CHEST, 0.0);
        Vec3 tip = origin.add(way.scale(SwordMove.FLURRY.reach() * wheel.value("swordReach") / 3.2));
        boolean hit = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, tip).inflate(1.0),
                this::fair)) {
            if (target.getBoundingBox().inflate(STAB_WIDE).clip(origin, tip).isEmpty()) {
                continue;
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) wheel.value("flurryDamage"));
            target.setDeltaMovement(target.getDeltaMovement().add(way.x * 0.12, 0.02, way.z * 0.12));
            target.hurtMarked = true;
            Vec3 at = target.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 4, 0.15, 0.15, 0.15, 0.2);
            hit = true;
        }
        this.sound(hit ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_WEAK, 0.55F,
                1.4F + 0.05F * k);
    }

    /**
     * A tick of the charge: whoever stands in his way is rammed aside to the side of him it stood on, with one of the
     * shield's six rams (see {@link #ram}) and a light hit. Clients play the ram from the move it sets.
     */
    void charge(ServerLevel level, int t) {
        CharacterAbility wheel = wheel();
        if (t >= Math.round(wheel.value("chargeSeconds") * 20.0) + 4) {
            // His own game stops it by itself; this only makes sure a charge never runs on for ever.
            this.stopCharge();
            return;
        }
        Vec3 at = this.owner.position();
        Vec3 right = new Vec3(-this.way.z, 0.0, this.way.x);
        AABB box = this.owner.getBoundingBox().expandTowards(this.way.scale(CHARGE_AHEAD)).inflate(CHARGE_WIDE, 0.3,
                CHARGE_WIDE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, this::fair)) {
            Vec3 to = target.position().subtract(at);
            if (to.dot(this.way) < -0.4 || this.shoved.contains(target.getId())) {
                continue;
            }
            this.shoved.add(target.getId());
            double side = to.dot(right);
            double sign = Math.abs(side) < 0.05 ? (this.owner.getRandom().nextBoolean() ? 1.0 : -1.0) : Math.signum(side);
            SwordMove ram = SwordMove.randomRam(this.owner.getRandom(), sign > 0.0, this.lastRam);
            this.lastRam = ram;
            this.move = ram;
            this.moveStart = this.age;
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner),
                    (float) (wheel.value("chargeDamage") * ram.power()));
            ram(target, ram, this.way, right, sign, wheel.value("bashKnockback"));
            Vec3 middle = target.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.CRIT, middle.x, middle.y, middle.z, 12, 0.3, 0.3, 0.3, 0.35);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), middle, 8, 0.35, 0.08);
            this.sound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.7F);
            this.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.9F);
            this.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F, 1.3F);
        }
        if (t % 3 == 0) {
            BlockState ground = level.getBlockState(BlockPos.containing(at.subtract(0.0, 0.2, 0.0)));
            if (!ground.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z, 4,
                        0.3, 0.05, 0.3, 0.1);
            }
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.GREEN, 1.0F),
                    at.add(0.0, 0.9, 0.0).add(this.way.scale(0.9)), 3, 0.3, 0.02);
        }
    }

    /**
     * The end of a charge: the shield slammed into the ground before him. A small shockwave runs out over the ground:
     * what stands in it is hurt (most in the middle) and thrown away.
     */
    private void slam(ServerLevel level, CharacterAbility wheel) {
        Vec3 at = this.owner.position().add(flat(this.owner.getLookAngle()).scale(SLAM_AHEAD));
        double radius = wheel.value("slamRadius");
        double damage = wheel.value("slamDamage");
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius + 1.0,
                2.5, radius + 1.0), this::fair)) {
            Vec3 to = target.position().subtract(at);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            if (flat > radius + target.getBbWidth() * 0.5 || Math.abs(to.y) > 2.0) {
                continue;
            }
            double near = 1.0 - 0.5 * Mth.clamp(flat / Math.max(0.1, radius), 0.0, 1.0);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) (damage * near));
            Vec3 away = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(to.x / flat, 0.0, to.z / flat);
            double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            target.setDeltaMovement(target.getDeltaMovement().add(away.scale(1.0 * near * (1.0 - resist)))
                    .add(0.0, 0.4 * near * (1.0 - resist), 0.0));
            target.hasImpulse = true;
            target.hurtMarked = true;
        }
        BlockState ground = level.getBlockState(BlockPos.containing(at.subtract(0.0, 0.2, 0.0)));
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z, 40,
                    radius * 0.4, 0.1, radius * 0.4, 0.3);
        }
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.GREEN, 1.8F), at.add(0.0, 0.2, 0.0), 56, 0.5);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.3F), at.add(0.0, 0.4, 0.0), 40, 0.35);
        this.sound(SoundEvents.ANVIL_LAND, 0.9F, 0.8F);
        this.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.6F, 1.4F);
        this.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.1F, 0.9F);
    }

    /**
     * Who the sword and shield may strike: anything the ring may hurt, but never his own pets.
     */
    private boolean fair(LivingEntity living) {
        return PowerRing.canHit(this.owner, living)
                && !(living instanceof OwnableEntity pet && pet.getOwner() == this.owner);
    }

    /** The way flat along the ground, one long. */
    static Vec3 flat(Vec3 way) {
        Vec3 flat = new Vec3(way.x, 0.0, way.z);
        return flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    void sound(SoundEvent sound, float volume, float pitch) {
        this.owner.level().playSound(null, this.owner.getX(), this.owner.getY() + 1.0, this.owner.getZ(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }
}
