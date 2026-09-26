package nl.tivek.multiversepowers.spell;

import java.util.UUID;
import javax.annotation.Nullable;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.faction.Factions;

final class FireballSpell {
    private static final double SPEED = 1.25;
    private static final int MAX_FLIGHT = 90;
    private static final int IMPACT_DURATION = 50;
    private static final double BLAST_RADIUS = 2.5;
    private static final float BLAST_DAMAGE = 4.0F;
    private static final int BURN_TICKS = 60;

    private static final int CORE = 0xFFE27A;
    private static final int FLAME = 0xFF7A1A;
    private static final int EMBER = 0x5A1A0A;
    private static final int GOLD = 0xFFC23A;
    private static final int SCORCH = 0x2A1A12;

    private FireballSpell() {
    }

    static boolean cast(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        Vec3 hand = Targeting.handPoint(player);
        UUID caster = player.getUUID();

        SmallFireball fireball = new SmallFireball(level, player, look) {
            @Override
            protected void onHitBlock(BlockHitResult result) {
                BlockPos spot = result.getBlockPos().relative(result.getDirection());
                // Spawn protection: the block still feels the hit, but no fire is lit there.
                if (this.mayInteract(this.level(), spot)) {
                    super.onHitBlock(result);
                } else {
                    BlockState block = this.level().getBlockState(result.getBlockPos());
                    block.onProjectileHit(this.level(), block, result, this);
                }
                spreadFire(this, spot);
            }

            @Override
            protected void onHitEntity(EntityHitResult result) {
                // Check before the hit: a kill would make the victim untargetable afterward.
                boolean burn = mayBurnAt(this, result.getEntity());
                super.onHitEntity(result);
                if (burn) {
                    spreadFire(this, result.getEntity().blockPosition());
                }
            }

            @Override
            protected void onHit(HitResult result) {
                Entity struck = result instanceof EntityHitResult hit ? hit.getEntity() : null;
                super.onHit(result);
                if (this.level() instanceof ServerLevel serverLevel) {
                    Vec3 at = result.getLocation();
                    blast(serverLevel, this, serverLevel.getServer().getPlayerList().getPlayer(caster), at, struck);
                    SpellFxPayload.send(serverLevel, SpellFxPayload.FIRE_BURST, at, at, -1, 0);
                    Effects.start(serverLevel, impact(at));
                }
            }
        };
        fireball.setPos(hand.x, hand.y, hand.z);
        fireball.setDeltaMovement(look.scale(SPEED));
        level.addFreshEntity(fireball);
        SpellFxPayload.send(level, SpellFxPayload.FIREBALL, hand, hand, fireball.getId(), 0);

        Effects.start(level, ignition(hand, look));
        Effects.start(level, trail(fireball));
        float pitch = 0.85F + player.getRandom().nextFloat() * 0.15F;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.PLAYERS, 0.7F, 1.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 1.0F, pitch);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, 0.8F, 0.7F);
        return true;
    }

    // Sparks gather in the palm, catch, and the fire leaves with a burst from a turning ring of runes.
    private static Effect ignition(Vec3 hand, Vec3 look) {
        return (level, age) -> {
            double size = 0.8 * (1.0 - age / 8.0);
            double spin = age * 0.5;
            Vec3 center = hand.add(look.scale(0.2));
            ParticleFx.disc(level, ParticleFx.dust(GOLD, 0.8F), center, look, size, 18, spin);
            ParticleFx.discStar(level, ParticleFx.dust(FLAME, 0.7F), center, look, size, 5, 2, -spin, 0.12);
            if (age == 0) {
                ParticleFx.implosion(level, ParticleTypes.SMALL_FLAME, center, 0.9, 10, 0.12);
                ParticleFx.cloud(level, ParticleTypes.FLAME, center, 16, 0.15, 0.1);
                ParticleFx.cloud(level, ParticleTypes.LAVA, center, 3, 0.1, 0.0);
            }
            if (age == 2) {
                ParticleFx.cloud(level, ParticleTypes.SMOKE, center, 8, 0.12, 0.02);
            }
            return age < 7;
        };
    }

    private static Effect trail(SmallFireball fireball) {
        return (level, age) -> {
            if (!fireball.isAlive()) {
                return false;
            }
            Vec3 center = fireball.getBoundingBox().getCenter();
            Vec3 velocity = fireball.getDeltaMovement();
            if (fireball.isInWater()) {
                douse(level, center);
                fireball.discard();
                return false;
            }
            if (age >= MAX_FLIGHT) {
                ParticleFx.cloud(level, ParticleTypes.LARGE_SMOKE, center, 12, 0.2, 0.03);
                ParticleFx.cloud(level, ParticleTypes.SMALL_FLAME, center, 8, 0.2, 0.02);
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
            Vec3[] b = ParticleFx.basis(direction);
            for (int strand = 0; strand < 2; strand++) {
                for (int step = 0; step < 3; step++) {
                    double angle = (age + step / 3.0) * 1.1 + strand * Math.PI;
                    Vec3 back = center.subtract(velocity.scale(step / 3.0));
                    Vec3 point = back.add(b[0].scale(Math.cos(angle) * 0.45)).add(b[1].scale(Math.sin(angle) * 0.45));
                    ParticleFx.at(level, strand == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, point);
                }
            }
            // A tail of heat that thins out behind it, then smoke.
            for (int k = 1; k <= 3; k++) {
                Vec3 tail = center.subtract(velocity.scale(0.35 * k));
                ParticleFx.at(level, ParticleFx.fade(GOLD, EMBER, 1.3F - 0.25F * k), tail.add(ParticleFx.spread(0.15),
                        ParticleFx.spread(0.15), ParticleFx.spread(0.15)));
            }
            ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, center.x - direction.x * 0.9,
                    center.y - direction.y * 0.9, center.z - direction.z * 0.9, 1, 0.1, 0.1, 0.1, 0.01);
            if (ParticleFx.chance(0.3)) {
                ParticleFx.at(level, ParticleTypes.LAVA, center);
            }
            if (ParticleFx.chance(0.25)) {
                ParticleFx.at(level, ParticleTypes.FALLING_LAVA, center.add(0.0, -0.2, 0.0));
            }
            if (age % 5 == 0) {
                level.playSound(null, center.x, center.y, center.z, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS,
                        0.7F, 1.4F);
            }
            if (age % 8 == 3) {
                level.playSound(null, center.x, center.y, center.z, SoundEvents.BLAZE_BURN, SoundSource.PLAYERS,
                        0.35F, 0.8F);
            }
            return true;
        };
    }

    // Water puts it out with a hiss and a burst of steam.
    private static void douse(ServerLevel level, Vec3 at) {
        ParticleFx.cloud(level, ParticleTypes.CLOUD, at, 14, 0.25, 0.05);
        ParticleFx.cloud(level, ParticleTypes.BUBBLE, at, 16, 0.3, 0.1);
        ParticleFx.cloud(level, ParticleTypes.SMOKE, at.add(0.0, 0.3, 0.0), 8, 0.2, 0.03);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.8F, 1.1F);
    }

    // The burst hurts and lights what is hostile round the hit, less the further off; what it struck head-on already
    // took the fireball's own hit.
    private static void blast(ServerLevel level, SmallFireball fireball, @Nullable ServerPlayer caster, Vec3 at,
            @Nullable Entity struck) {
        if (caster == null) {
            return;
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(BLAST_RADIUS
                + 1.0), entity -> entity != struck && SpellTargets.hits(caster, entity))) {
            Vec3 middle = target.getBoundingBox().getCenter();
            double far = middle.distanceTo(at) - target.getBbWidth() * 0.5;
            if (far > BLAST_RADIUS || !Targeting.clearPath(level, at, middle, fireball)) {
                continue;
            }
            float damage = BLAST_DAMAGE * (float) (1.0 - 0.6 * Math.max(0.0, far) / BLAST_RADIUS);
            target.hurt(level.damageSources().fireball(fireball, caster), damage);
            target.igniteForTicks(BURN_TICKS);
            Vec3 away = middle.subtract(at);
            away = away.horizontalDistanceSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 0.0) : away.normalize();
            SpellTargets.push(target, away, 0.45, 0.25);
            ParticleFx.cloud(level, ParticleTypes.FLAME, middle, 6, 0.25, 0.03);
        }
    }

    private static Effect impact(Vec3 at) {
        return (level, age) -> {
            if (age == 0) {
                ParticleFx.at(level, ParticleTypes.FLASH, at);
                ParticleFx.cloud(level, ParticleTypes.EXPLOSION, at, 3, 0.5, 0.0);
                ParticleFx.sphereOut(level, ParticleTypes.FLAME, at, 70, 0.3);
                ParticleFx.sphereOut(level, ParticleTypes.LARGE_SMOKE, at, 22, 0.1);
                ParticleFx.cloud(level, ParticleTypes.LAVA, at, 14, 0.4, 0.0);
                ParticleFx.sphere(level, ParticleFx.dust(CORE, 2.5F), at, 0.6, 24, 0);
                float pitch = 1.25F + ParticleFx.RANDOM.nextFloat() * 0.2F;
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                        0.9F, pitch);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.6F);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.BLAZE_HURT, SoundSource.PLAYERS, 0.4F, 0.5F);
            }
            if (age == 1) {
                ParticleFx.shockwave(level, ParticleTypes.FLAME, at.add(0, 0.1, 0), 44, 0.38);
                ParticleFx.shockwave(level, ParticleTypes.SMOKE, at.add(0, 0.2, 0), 28, 0.22);
            }
            if (age <= 10) {
                double radius = 0.5 + age * 0.3;
                ParticleOptions ring = ParticleFx.fade(GOLD, FLAME, 1.4F - age * 0.08F);
                ParticleFx.ring(level, ring, at.add(0, 0.15, 0), radius, 16 + age * 3, age * 0.2);
            }
            if (age == 6) {
                ParticleFx.ring(level, ParticleFx.dust(SCORCH, 1.6F), at.add(0, 0.05, 0), BLAST_RADIUS * 0.8, 28, 0.3);
            }
            // A column of smoke rises from where it burst.
            if (age >= 2 && age < 34 && age % 3 == 0) {
                ParticleFx.fly(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, at.add(ParticleFx.spread(0.3), 0.2,
                        ParticleFx.spread(0.3)), new Vec3(0, 1, 0), 0.02);
            }
            double fade = 1.0 - (double) age / IMPACT_DURATION;
            if (age % 2 == 0 && ParticleFx.chance(fade)) {
                Vec3 spot = at.add(ParticleFx.spread(1.4), 0.1, ParticleFx.spread(1.4));
                ParticleFx.fly(level, ParticleTypes.FLAME, spot, new Vec3(0, 1, 0), 0.04);
                ParticleFx.fly(level, ParticleTypes.SMOKE, spot.add(0, 0.3, 0), new Vec3(0, 1, 0), 0.03);
            }
            if (age % 7 == 3 && ParticleFx.chance(fade)) {
                ParticleFx.at(level, ParticleTypes.LAVA, at.add(ParticleFx.spread(1.0), 0.1, ParticleFx.spread(1.0)));
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.5F, 1.2F);
            }
            if (age == IMPACT_DURATION - 8) {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.35F, 1.4F);
            }
            return age < IMPACT_DURATION;
        };
    }

    private static boolean mayBurnAt(SmallFireball fireball, Entity hit) {
        if (fireball.getOwner() instanceof ServerPlayer caster) {
            return !Factions.friendly(caster, hit)
                    && (!(hit instanceof Player player) || Targeting.isTargetable(caster, player));
        }
        return !(hit instanceof Player);
    }

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
