package nl.tivek.multiversepowers.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;

/**
 * Fireball: a flaming circle flares up at your hand, a blazing comet with a spiralling flame trail
 * flies where you look, and bursts in a ball of fire with a burning shockwave and a small pile of fire.
 */
final class FireballSpell {
    private static final double SPEED = 1.1;
    // The fireball fizzles out after this many ticks if it hits nothing.
    private static final int MAX_FLIGHT = 100;
    private static final int IMPACT_DURATION = 40;

    private static final int CORE = 0xFFE27A;
    private static final int FLAME = 0xFF7A1A;
    private static final int EMBER = 0x5A1A0A;
    private static final int GOLD = 0xFFC23A;

    private FireballSpell() {
    }

    static boolean cast(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        Vec3 hand = Targeting.handPoint(player);

        SmallFireball fireball = new SmallFireball(level, player, look) {
            @Override
            protected void onHitBlock(BlockHitResult result) {
                BlockPos spot = result.getBlockPos().relative(result.getDirection());
                if (this.mayInteract(this.level(), spot)) {
                    super.onHitBlock(result);
                } else {
                    // Where the caster may not build (spawn protection) the block still feels the hit, but the
                    // fireball's own fire is not lit.
                    BlockState block = this.level().getBlockState(result.getBlockPos());
                    block.onProjectileHit(this.level(), block, result, this);
                }
                spreadFire(this, spot);
            }

            @Override
            protected void onHitEntity(EntityHitResult result) {
                // Asked before the hit: a player it kills is no longer one the caster may hit.
                boolean burn = mayBurnAt(this, result.getEntity());
                super.onHitEntity(result);
                if (burn) {
                    spreadFire(this, result.getEntity().blockPosition());
                }
            }

            @Override
            protected void onHit(HitResult result) {
                super.onHit(result);
                if (this.level() instanceof ServerLevel serverLevel) {
                    Effects.start(serverLevel, impact(result.getLocation()));
                }
            }
        };
        fireball.setPos(hand.x, hand.y, hand.z);
        fireball.setDeltaMovement(look.scale(SPEED));
        level.addFreshEntity(fireball);

        Effects.start(level, castCircle(hand, look));
        Effects.start(level, trail(fireball));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 1.0F, 0.9F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, 0.8F, 0.7F);
        return true;
    }

    /** A spinning ring of fire with a five-pointed star at the hand, shrinking away in a few ticks. */
    private static Effect castCircle(Vec3 hand, Vec3 look) {
        return (level, age) -> {
            double size = 0.75 * (1.0 - age / 7.0);
            double spin = age * 0.5;
            Vec3 center = hand.add(look.scale(0.2));
            ParticleFx.disc(level, ParticleFx.dust(GOLD, 0.8F), center, look, size, 18, spin);
            ParticleFx.discStar(level, ParticleFx.dust(FLAME, 0.7F), center, look, size, 5, 2, -spin, 0.12);
            if (age == 0) {
                ParticleFx.cloud(level, ParticleTypes.FLAME, center, 14, 0.15, 0.08);
                ParticleFx.cloud(level, ParticleTypes.LAVA, center, 3, 0.1, 0.0);
            }
            return age < 6;
        };
    }

    /** The comet around the fireball: a glowing core, a double flame spiral and a smoke tail. */
    private static Effect trail(SmallFireball fireball) {
        return (level, age) -> {
            if (!fireball.isAlive()) {
                return false;
            }
            Vec3 center = fireball.getBoundingBox().getCenter();
            Vec3 velocity = fireball.getDeltaMovement();
            if (age >= MAX_FLIGHT) {
                ParticleFx.cloud(level, ParticleTypes.LARGE_SMOKE, center, 12, 0.2, 0.03);
                level.playSound(null, center.x, center.y, center.z, SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.PLAYERS, 0.6F, 1.2F);
                fireball.discard();
                return false;
            }
            Vec3 direction = velocity.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : velocity.normalize();

            ParticleFx.sphere(level, ParticleFx.dust(CORE, 1.1F), center, 0.25, 8, age * 0.7);
            for (int i = 0; i < 5; i++) {
                ParticleFx.at(level, ParticleFx.fade(FLAME, EMBER, 1.5F),
                        center.add(ParticleFx.spread(0.4), ParticleFx.spread(0.4), ParticleFx.spread(0.4)));
            }
            // Two flame strands winding around the flight path, a little behind the ball.
            Vec3[] b = ParticleFx.basis(direction);
            for (int strand = 0; strand < 2; strand++) {
                for (int step = 0; step < 3; step++) {
                    double angle = (age + step / 3.0) * 1.1 + strand * Math.PI;
                    Vec3 back = center.subtract(velocity.scale(step / 3.0));
                    Vec3 point = back.add(b[0].scale(Math.cos(angle) * 0.45)).add(b[1].scale(Math.sin(angle) * 0.45));
                    ParticleFx.at(level, strand == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, point);
                }
            }
            ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, center.x - direction.x * 0.6,
                    center.y - direction.y * 0.6, center.z - direction.z * 0.6, 1, 0.1, 0.1, 0.1, 0.01);
            if (ParticleFx.chance(0.3)) {
                ParticleFx.at(level, ParticleTypes.LAVA, center);
            }
            if (age % 6 == 0) {
                level.playSound(null, center.x, center.y, center.z, SoundEvents.BLAZE_BURN, SoundSource.PLAYERS,
                        0.4F, 0.8F);
            }
            return true;
        };
    }

    /** Ball of fire, a burning shockwave rolling outward, then embers and smoke drifting up. */
    private static Effect impact(Vec3 at) {
        return (level, age) -> {
            if (age == 0) {
                ParticleFx.at(level, ParticleTypes.FLASH, at);
                ParticleFx.cloud(level, ParticleTypes.EXPLOSION, at, 3, 0.5, 0.0);
                ParticleFx.sphereOut(level, ParticleTypes.FLAME, at, 60, 0.28);
                ParticleFx.sphereOut(level, ParticleTypes.LARGE_SMOKE, at, 20, 0.1);
                ParticleFx.cloud(level, ParticleTypes.LAVA, at, 12, 0.4, 0.0);
                ParticleFx.sphere(level, ParticleFx.dust(CORE, 2.5F), at, 0.6, 24, 0);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                        1.0F, 1.4F);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            if (age == 1) {
                ParticleFx.shockwave(level, ParticleTypes.FLAME, at.add(0, 0.1, 0), 40, 0.35);
            }
            if (age <= 10) {
                double radius = 0.5 + age * 0.35;
                ParticleOptions ring = ParticleFx.fade(GOLD, FLAME, 1.4F - age * 0.08F);
                ParticleFx.ring(level, ring, at.add(0, 0.15, 0), radius, 16 + age * 3, age * 0.2);
            }
            double fade = 1.0 - (double) age / IMPACT_DURATION;
            if (age % 2 == 0 && ParticleFx.chance(fade)) {
                Vec3 spot = at.add(ParticleFx.spread(1.2), 0.1, ParticleFx.spread(1.2));
                ParticleFx.fly(level, ParticleTypes.FLAME, spot, new Vec3(0, 1, 0), 0.04);
                ParticleFx.fly(level, ParticleTypes.SMOKE, spot.add(0, 0.3, 0), new Vec3(0, 1, 0), 0.03);
            }
            return age < IMPACT_DURATION;
        };
    }

    /**
     * Fire is only left at the feet of a player the caster could hurt: the game already stops the hit itself when
     * PvP is off, but not this fire.
     */
    private static boolean mayBurnAt(SmallFireball fireball, Entity hit) {
        return !(hit instanceof Player player)
                || fireball.getOwner() instanceof ServerPlayer caster && Targeting.isTargetable(caster, player);
    }

    /**
     * A small pile of fire: the spot itself and about half of the eight spots around it, never where the caster may
     * not build (spawn protection, outside the world border).
     */
    private static void spreadFire(SmallFireball fireball, BlockPos center) {
        Level level = fireball.level();
        if (level.isClientSide) {
            return;
        }
        RandomSource random = fireball.getRandom();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean middle = dx == 0 && dz == 0;
                if (!middle && random.nextFloat() > 0.5F) {
                    continue;
                }
                // Follow the ground one block up or down, so the pile also works on slopes.
                for (int dy : new int[] {0, -1, 1}) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (BaseFireBlock.canBePlacedAt(level, pos, Direction.UP) && fireball.mayInteract(level, pos)) {
                        level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
                        break;
                    }
                }
            }
        }
    }
}
