package nl.tivek.multiversepowers.creature.elemental;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.elemental.spell.FrostRay;
import nl.tivek.multiversepowers.creature.elemental.spell.HeavedStone;
import nl.tivek.multiversepowers.creature.elemental.spell.ScorchBall;
import nl.tivek.multiversepowers.creature.elemental.spell.StormArc;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.faction.Factions;

// What each element does when cast, the same for the Elemental Blaze and the Elemental Staff: who it may hurt (the
// Blaze its foes, a player anyone but themselves and their own faction, its lightning leaping on only to who is red to
// them), how hard (a share of the Blaze's damage, the staff's own), where it flies from and where it goes.
public final class BlazeCasts {
    public static final double RANGE = 16.0;
    private static final double GALE_RANGE = 7.0;
    private static final double GALE_CONE = 0.55;
    private static final double NOVA_RANGE = 6.0;

    private BlazeCasts() {
    }

    // Whether what `caster` casts may hurt `target`.
    public static boolean hits(@Nullable Entity caster, Entity target) {
        if (!(target instanceof LivingEntity living) || target == caster || !living.isAlive()
                || living instanceof ArmorStand || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            return false;
        }
        if (caster instanceof ServerPlayer player) {
            return Factions.mayHit(player, target);
        }
        if (caster instanceof Mob mob) {
            return MonsterBlows.foe(mob, living);
        }
        return !(target instanceof ElementalBlaze);
    }

    // Whether its lightning leaps on to `target` by itself.
    public static boolean seeks(@Nullable Entity caster, Entity target) {
        return hits(caster, target) && (!(caster instanceof ServerPlayer player) || Factions.hostile(player, target));
    }

    public static float damage(LivingEntity caster, Element element) {
        return caster instanceof Mob mob ? (float) (MonsterBlows.damage(mob) * element.share) : element.staffDamage;
    }

    public static DamageSource source(Entity direct, @Nullable Entity owner, Element element) {
        return new DamageSource(direct.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(element.harm), direct, owner);
    }

    // The same, struck from `from`: what it hits is thrown back the other way.
    public static DamageSource source(Entity direct, @Nullable Entity owner, Element element, Vec3 from) {
        return new DamageSource(direct.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(element.harm), direct, owner, from);
    }

    // Where its spells leave it: the Blaze's leading rod, a player's staff held out before them.
    public static Vec3 tip(Entity caster, float partialTick) {
        if (caster instanceof ElementalBlaze blaze) {
            return blaze.emitter(partialTick);
        }
        Vec3 look = caster.getViewVector(partialTick);
        return caster.getEyePosition(partialTick).add(look.scale(0.8)).add(side(caster, look).scale(0.35))
                .add(0.0, -0.3, 0.0);
    }

    // The way of the hand a player holds the staff in, level, for one who looks along `look`.
    public static Vec3 side(Entity caster, Vec3 look) {
        Vec3 flat = new Vec3(-look.z, 0.0, look.x);
        Vec3 right = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
        return caster instanceof Player player && player.getMainArm() == HumanoidArm.LEFT ? right.scale(-1.0) : right;
    }

    // Which way it casts from `from`: at the Blaze's foe (where it will be, for what flies `speed` a tick); a player's
    // at what they look at, so what leaves the staff beside them still lands under their crosshair.
    public static Vec3 aim(LivingEntity caster, Vec3 from, double speed) {
        if (caster instanceof Mob mob) {
            LivingEntity target = mob.getTarget();
            if (target == null) {
                return caster.getLookAngle();
            }
            Vec3 at = target.getBoundingBox().getCenter();
            if (speed > 0.0) {
                double ticks = Math.min(20.0, at.distanceTo(from) / speed);
                at = at.add(target.getDeltaMovement().multiply(1.0, 0.0, 1.0).scale(ticks * 0.6));
            }
            Vec3 way = at.subtract(from);
            return way.lengthSqr() < 1.0E-6 ? caster.getLookAngle() : way.normalize();
        }
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 at = LoadedWorld.clip(caster.level(), new ClipContext(eye, eye.add(look.scale(RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
        EntityHitResult seen = ProjectileUtil.getEntityHitResult(caster.level(), caster, eye, at, new AABB(eye, at)
                .inflate(1.0), entity -> hits(caster, entity), 0.3F);
        Vec3 way = (seen != null ? seen.getLocation() : at).subtract(from);
        return way.lengthSqr() < 4.0 || way.normalize().dot(look) < 0.5 ? look : way.normalize();
    }

    public static void frost(ServerLevel level, LivingEntity caster, int life) {
        FrostRay.fire(level, caster, life);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.HOSTILE, 1.0F, 1.9F);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.HOSTILE, 1.4F, 1.5F);
    }

    // A rock torn up out of the ground below and before it, lifted for `lift` ticks and then hurled.
    public static void heave(ServerLevel level, LivingEntity caster, int lift) {
        Vec3 ahead = caster instanceof ElementalBlaze ? MonsterBlows.ahead((Mob) caster)
                : caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        // A player's comes up beside them, out of their view.
        Vec3 over = caster instanceof ElementalBlaze ? caster.position().add(ahead.scale(1.5))
                : caster.position().add(ahead.scale(2.2)).add(side(caster, ahead).scale(HeavedStone.BESIDE));
        BlockHitResult down = LoadedWorld.clip(level, new ClipContext(over.add(0.0, 1.0, 0.0), over.add(0.0, -10.0,
                0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 ground = down.getType() == HitResult.Type.MISS ? caster.position().add(ahead) : down.getLocation();
        BlockState rock = down.getType() == HitResult.Type.MISS ? null : rock(level, down.getBlockPos());
        if (rock == null) {
            rock = ground.y < 0.0 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState()
                    : Blocks.COBBLESTONE.defaultBlockState();
        }
        HeavedStone.raise(level, caster, ground.add(0.0, -0.6, 0.0), rock, lift);
        BlockPos at = BlockPos.containing(ground);
        level.playSound(null, at, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.6F, 0.5F);
        level.playSound(null, at, SoundEvents.GRINDSTONE_USE, SoundSource.HOSTILE, 1.2F, 0.45F);
    }

    // What the stone is made of: the ground it comes from where that is a whole block of plain rock or earth (null
    // else: then stone, or deepslate deep down). The ground itself stays as it was.
    @Nullable
    private static BlockState rock(ServerLevel level, BlockPos pos) {
        BlockState ground = level.getBlockState(pos);
        float hardness = ground.getDestroySpeed(level, pos);
        return ground.isCollisionShapeFullBlock(level, pos) && !ground.hasBlockEntity() && hardness >= 0.0F
                && hardness < 10.0F ? ground : null;
    }

    // Lightning from `from` to `first` (or into `miss` when nothing stands there), leaping on to up to three more.
    public static void chain(ServerLevel level, LivingEntity caster, @Nullable LivingEntity first, Vec3 miss) {
        StormArc.strike(level, caster, first, miss, damage(caster, Element.STORM));
    }

    public static void scorch(ServerLevel level, LivingEntity caster) {
        Vec3 from = tip(caster, 1.0F);
        ScorchBall ball = new ScorchBall(level, caster, from, aim(caster, from, 1.0), damage(caster, Element.FIRE));
        level.addFreshEntity(ball);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.3F, 0.7F);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.0F, 0.8F);
    }

    // A gust in a cone before it: whoever stands in it is hurt a little and thrown back, arrows and fireballs flying at
    // it are turned round.
    public static void gale(ServerLevel level, LivingEntity caster) {
        Vec3 from = tip(caster, 1.0F);
        Vec3 way = aim(caster, from, 0.0);
        float harm = damage(caster, Element.WIND);
        AABB box = new AABB(from, from).inflate(GALE_RANGE);
        for (Entity entity : level.getEntities(caster, box)) {
            Vec3 to = entity.getBoundingBox().getCenter().subtract(from);
            double far = to.length();
            if (far > GALE_RANGE || far > 1.0E-3 && to.scale(1.0 / far).dot(way) < GALE_CONE) {
                continue;
            }
            if (entity instanceof Projectile projectile && projectile.getOwner() != caster) {
                projectile.setDeltaMovement(way.scale(Math.max(0.8, projectile.getDeltaMovement().length())));
                projectile.hasImpulse = true;
                continue;
            }
            if (!hits(caster, entity) || !(entity instanceof LivingEntity living)) {
                continue;
            }
            double strength = 1.0 - far / GALE_RANGE * 0.5;
            Vec3 push = new Vec3(way.x, 0.0, way.z).normalize().scale(1.35 * strength).add(0.0, 0.45 * strength, 0.0);
            double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            living.hurt(source(caster, caster, Element.WIND), harm);
            living.setDeltaMovement(living.getDeltaMovement().scale(0.2).add(push.scale(1.0 - resist)));
            living.hasImpulse = true;
            living.hurtMarked = true;
            Vec3 at = living.getBoundingBox().getCenter();
            ParticleFx.send(level, ParticleTypes.GUST, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        // A player's starts further out, or it blinds them.
        double out = caster instanceof Player ? 1.4 : 0.4;
        for (int i = 0; i < 18; i++) {
            Vec3 spread = way.add((level.random.nextDouble() - 0.5) * 0.7, (level.random.nextDouble() - 0.5) * 0.5,
                    (level.random.nextDouble() - 0.5) * 0.7).normalize();
            ParticleFx.fly(level, ParticleTypes.WHITE_SMOKE, from.add(spread.scale(out)), spread,
                    0.5 + level.random.nextDouble() * 0.4);
            ParticleFx.fly(level, ParticleTypes.CLOUD, from.add(spread.scale(out + 0.2)), spread,
                    0.35 + level.random.nextDouble() * 0.3);
        }
        for (int i = 1; i <= 3; i++) {
            Vec3 at = from.add(way.scale(i * 1.8));
            ParticleFx.send(level, ParticleTypes.SMALL_GUST, at.x, at.y, at.z, 3, 0.4 * i, 0.3 * i, 0.4 * i, 0.0);
        }
        ParticleFx.send(level, ParticleTypes.GUST_EMITTER_SMALL, from.x + way.x * 2.5, from.y + way.y * 2.5,
                from.z + way.z * 2.5, 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.HOSTILE, 1.2F, 0.8F);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.BREEZE_SHOOT, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    // The awakening's burst: every element at once, throwing back whoever stands near.
    static void nova(ServerLevel level, ElementalBlaze blaze) {
        Vec3 at = blaze.getBoundingBox().getCenter();
        List<LivingEntity> near = MonsterBlows.foes(blaze, blaze.getBoundingBox().inflate(NOVA_RANGE));
        for (LivingEntity living : near) {
            Vec3 away = living.position().subtract(blaze.position()).multiply(1.0, 0.0, 1.0);
            Vec3 way = away.lengthSqr() < 1.0E-4 ? MonsterBlows.ahead(blaze) : away.normalize();
            MonsterBlows.hurt(living, source(blaze, blaze, Element.WIND), damage(blaze, Element.WIND),
                    way.scale(1.1).add(0.0, 0.5, 0.0));
            living.igniteForSeconds(2.0F);
        }
        for (Element element : Element.ALL) {
            ParticleFx.shockwave(level, ParticleFx.dust(element.light, 1.4F), at, 40, 0.5 + element.ordinal() * 0.08);
            ParticleFx.sphereOut(level, element.particle(), at, 16, 0.35);
        }
        ParticleFx.send(level, ParticleTypes.FLAME, at.x, at.y, at.z, 30, 0.3, 0.3, 0.3, 0.25);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 0.7F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE,
                1.4F, 0.6F);
    }
}
