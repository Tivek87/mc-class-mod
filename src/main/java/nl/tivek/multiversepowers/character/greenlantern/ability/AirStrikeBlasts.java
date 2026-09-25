package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.world.BlockRules;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.faction.Factions;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.BLAST_TICKS;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.CRASH_SIZE;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.HEIGHT;

abstract class AirStrikeBlasts implements Effect {
    private static final double CRASH_KNOCKBACK = 2.8;
    static final double VIEW_RANGE = 260.0;

    final ServerPlayer owner;
    final CharacterAbility ability;
    final PlanePath path;
    int age;

    AirStrikeBlasts(ServerPlayer owner, CharacterAbility ability, PlanePath path) {
        this.owner = owner;
        this.ability = ability;
        this.path = path;
    }

    void crash(ServerLevel level) {
        Vec3 at = this.path.crash();
        double radius = this.ability.value("crashRadius");
        this.blast(level, at, radius, this.ability.getDamage(), null, CRASH_KNOCKBACK);
        BlockHitResult under = LoadedWorld.clip(level, new ClipContext(at.add(0.0, 0.5, 0.0),
                at.subtract(0.0, 6.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                CollisionContext.empty()));
        Vec3 ground = under.getType() == HitResult.Type.MISS ? at : under.getLocation();
        this.crater(level, ground, this.ability.value("craterRadius"), this.ability.intValue("debrisBlocks"));
        double s = CRASH_SIZE;
        ParticleFx.send(level, ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1.5 * s, at.z, (int) (10 * s), 4.0 * s,
                1.7 * s, 4.0 * s, 0.0);
        ParticleFx.send(level, ParticleTypes.FLASH, at.x, at.y + 2.0 * s, at.z, (int) (2 * s), 2.0 * s, 1.0 * s,
                2.0 * s, 0.0);
        ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, at.x, at.y + 2.0 * s, at.z, (int) (90 * s), 4.6 * s, 2.3 * s,
                4.6 * s, 0.11);
        ParticleFx.send(level, ParticleTypes.FLAME, at.x, at.y + 1.0 * s, at.z, (int) (100 * s), 3.5 * s, 1.2 * s,
                3.5 * s, 0.38);
        ParticleFx.send(level, ParticleTypes.LAVA, at.x, at.y + 0.5 * s, at.z, (int) (34 * s), 2.9 * s, 0.6 * s,
                2.9 * s, 0.0);
        Vec3 heart = at.add(0.0, 1.0 * s, 0.0);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 3.0F), heart, (int) (130 * s), 1.45 * s);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.GREEN, 3.5F), heart, (int) (100 * s), 0.9 * s);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.GREEN, 2.5F), at.add(0.0, 0.3 * s, 0.0),
                (int) (170 * s), 2.15 * s);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 2.0F), at.add(0.0, 0.6 * s, 0.0),
                (int) (130 * s), 1.5 * s);
        this.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 10.0F, 0.45F);
        this.sound(level, at, SoundEvents.DRAGON_FIREBALL_EXPLODE, 8.0F, 0.5F);
        this.sound(level, at, SoundEvents.WARDEN_SONIC_BOOM, 8.0F, 0.5F);
        this.sound(level, at, SoundEvents.BEACON_DEACTIVATE, 6.0F, 0.5F);
        this.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 6.0F, 0.4F);
        this.sound(level, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 8.0F, 0.6F);
        this.sound(level, this.owner.getEyePosition(), SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 0.35F);
    }

    void smoulder(ServerLevel level) {
        Vec3 at = this.path.crash();
        double fade = 1.0 - (this.age - this.path.crashTick()) / BLAST_TICKS;
        if (fade <= 0.0) {
            return;
        }
        RandomSource random = this.owner.getRandom();
        double radius = Math.max(2.0, this.ability.value("craterRadius"));
        for (int k = 0; k < 2; k++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double far = Math.sqrt(random.nextDouble()) * radius * 0.8;
            double x = at.x + Math.cos(angle) * far;
            double z = at.z + Math.sin(angle) * far;
            ParticleFx.send(level, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, at.y - 0.5, z, 0, 0.0, 1.0, 0.0, 0.07);
            ParticleFx.send(level, ParticleTypes.FLAME, x, at.y - 0.3, z, 4, 0.5, 0.3, 0.5, 0.04);
        }
        ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.0, at.z, 2 + (int) (6.0 * fade), radius * 0.5,
                0.8, radius * 0.5, 0.05);
        if (random.nextDouble() < 0.4 * fade) {
            ParticleFx.send(level, ParticleTypes.LAVA, at.x, at.y, at.z, 3, radius * 0.4, 0.2, radius * 0.4, 0.0);
        }
    }

    void crater(ServerLevel level, Vec3 at, double radius, int debris) {
        double hardest = this.ability.value("breakHardness");
        if (hardest < 0.0 || radius <= 0.0) {
            return;
        }
        Crater crater = new Crater(this.owner, at, radius, debris, hardest);
        if (crater.tick(level, 0)) {
            Effects.start(level, crater);
        }
    }

    private static final class Crater implements Effect {
        private static final int WORK = 2400;
        private final ServerPlayer owner;
        private final Vec3 at;
        private final int top;
        private final double hardest;
        private final double hurlChance;
        private final RandomSource random;
        private final List<BlockPos> spots = new ArrayList<>();
        private int next;
        private int hurls;

        Crater(ServerPlayer owner, Vec3 at, double radius, int debris, double hardest) {
            this.owner = owner;
            this.at = at;
            this.hardest = hardest;
            this.random = owner.getRandom();
            this.hurls = Math.max(0, debris);
            this.hurlChance = Math.min(1.0, this.hurls / Math.max(1.0, 3.0 * Math.PI * radius * radius));
            double depth = radius * 0.62;
            BlockPos middle = BlockPos.containing(at.x, at.y - 0.5, at.z);
            this.top = middle.getY() - 2;
            int reach = (int) Math.ceil(radius + 1.0);
            for (int dy = reach; dy >= -(int) Math.ceil(depth + 1.0); dy--) {
                int layer = this.spots.size();
                double down = dy < 0 ? dy / depth : dy / (radius * 0.85);
                for (int dx = -reach; dx <= reach; dx++) {
                    for (int dz = -reach; dz <= reach; dz++) {
                        double rough = 1.0 + 0.2 * (Noise.of(middle.getX() + dx, middle.getZ() + dz, 17) - 0.5);
                        double out = (dx * dx + dz * dz) / (radius * radius) + down * down;
                        if (out <= rough * rough) {
                            this.spots.add(middle.offset(dx, dy, dz));
                        }
                    }
                }
                for (int i = this.spots.size() - 1; i > layer; i--) {
                    Collections.swap(this.spots, i, layer + this.random.nextInt(i - layer + 1));
                }
            }
        }

        @Override
        public boolean tick(ServerLevel level, int age) {
            int work = 0;
            while (this.next < this.spots.size() && work < WORK) {
                BlockPos pos = this.spots.get(this.next++);
                work++;
                // Where nobody has the world loaded (he flew off), skip it: never load chunks to blow them up.
                if (!level.isLoaded(pos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
                    continue;
                }
                float hardness = state.getDestroySpeed(level, pos);
                if (hardness < 0.0F || hardness > this.hardest || !BlockRules.mayBreak(level, this.owner, pos, state)) {
                    work += 2;
                    continue;
                }
                if (this.hurls > 0 && pos.getY() >= this.top && state.isCollisionShapeFullBlock(level, pos)
                        && this.random.nextDouble() < this.hurlChance) {
                    this.hurls--;
                    work += 29;
                    this.hurl(level, pos, state);
                } else {
                    work += 11;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            return this.next < this.spots.size();
        }

        private void hurl(ServerLevel level, BlockPos pos, BlockState state) {
            FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
            block.dropItem = false;
            Vec3 out = new Vec3(pos.getX() + 0.5 - this.at.x, 0.0, pos.getZ() + 0.5 - this.at.z);
            out = out.lengthSqr() < 1.0E-4
                    ? new Vec3(this.random.nextDouble() - 0.5, 0.0, this.random.nextDouble() - 0.5)
                    : out.normalize();
            double speed = 0.45 + 0.55 * this.random.nextDouble();
            block.setDeltaMovement(out.x * speed, 0.7 + 0.8 * this.random.nextDouble(), out.z * speed);
            block.hurtMarked = true;
        }
    }

    void blast(ServerLevel level, Vec3 at, double radius, double damage, @Nullable LivingEntity direct,
            double knockback) {
        AABB area = new AABB(at, at).inflate(radius + 1.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, this::hostile)) {
            Vec3 middle = target.getBoundingBox().getCenter();
            double distance = middle.distanceTo(at);
            boolean hit = target == direct;
            if (!hit && distance > radius + target.getBbWidth() * 0.5) {
                continue;
            }
            double near = hit ? 1.0 : 1.0 - 0.5 * Mth.clamp(distance / Math.max(0.1, radius), 0.0, 1.0);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) (damage * near));
            if (knockback > 0.0) {
                Vec3 away = new Vec3(middle.x - at.x, 0.0, middle.z - at.z);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
                double push = knockback * near;
                target.setDeltaMovement(target.getDeltaMovement().add(new Vec3(away.x * push, 0.25 + 0.3 * push,
                        away.z * push).scale(1.0 - resist)));
                target.hasImpulse = true;
                target.hurtMarked = true;
            }
        }
    }

    boolean hostile(LivingEntity living) {
        return PowerRing.canHit(this.owner, living) && Factions.hostile(this.owner, living);
    }

    Vec3 ground(ServerLevel level, Vec3 at) {
        double top = this.path.start().y + 4.0;
        Vec3 from = new Vec3(at.x, top, at.z);
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(from, from.subtract(0.0, HEIGHT * 3.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? new Vec3(at.x, this.owner.getY(), at.z) : hit.getLocation();
    }

    void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
