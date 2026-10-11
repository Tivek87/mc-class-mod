package nl.tivek.multiversepowers.creature;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.mob.MobDefaults;
import nl.tivek.multiversepowers.faction.mob.MobRules;

// What the mod's zombies share when they strike: who their blows land on, a blow that throws, the ground's dust and
// their own kinds of harm (claws that tear past a shield, bleeding, acid).
public final class MonsterBlows {
    public static final ResourceKey<DamageType> REND = key("rend");
    public static final ResourceKey<DamageType> BLEED = key("bleed");
    public static final ResourceKey<DamageType> ACID = key("acid");
    public static final ResourceKey<DamageType> SCREAM = key("scream");
    public static final ResourceKey<DamageType> SHRAPNEL = key("shrapnel");

    private MonsterBlows() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
                name));
    }

    public static DamageSource source(Entity by, ResourceKey<DamageType> type) {
        return new DamageSource(by.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type), by);
    }

    // Who its blows land on: what it hunts, players, creatures that are no monster and monsters it hunts by the game's
    // own rules (the Wizard hunts them all), as the Mobs page sets them.
    public static List<LivingEntity> foes(Mob mob, AABB box) {
        LivingEntity target = mob.getTarget();
        return mob.level().getEntitiesOfClass(LivingEntity.class, box, living -> {
            if (living == mob || !living.isAlive() || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living)) {
                return false;
            }
            if (living == target) {
                return true;
            }
            Standing set = MobRules.set(mob.getType(), living.getType());
            if (set != null) {
                return set == Standing.HOSTILE;
            }
            if (living instanceof Player || !(living instanceof Enemy)) {
                return true;
            }
            return mob.level() instanceof ServerLevel level
                    && MobDefaults.of(level, mob.getType(), living.getType()) == Standing.HOSTILE;
        });
    }

    // A blow that throws: the target is sent along `push`, less as it stands firm. Whether it landed.
    public static boolean hurt(LivingEntity target, DamageSource source, double damage, Vec3 push) {
        if (!target.hurt(source, (float) damage)) {
            return false;
        }
        double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        target.setDeltaMovement(push.scale(1.0 - resist));
        target.hasImpulse = true;
        target.hurtMarked = true;
        return true;
    }

    public static double damage(Mob mob) {
        return mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    // From its body's side to its target's.
    public static double gap(Mob mob, LivingEntity target) {
        return Math.sqrt(mob.distanceToSqr(target)) - (target.getBbWidth() + mob.getBbWidth()) * 0.5;
    }

    // The way its body faces, flat.
    public static Vec3 ahead(Mob mob) {
        double yaw = Math.toRadians(mob.yBodyRot);
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    // Turns it at once to face along `way`.
    public static void face(Mob mob, Vec3 way) {
        if (way.horizontalDistanceSqr() < 1.0E-6) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-way.x, way.z));
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
    }

    // The flat way from `from` to `to`, of length 1 (ahead of `mob` when they stand together).
    public static Vec3 toward(Mob mob, Vec3 from, Vec3 to) {
        Vec3 flat = new Vec3(to.x - from.x, 0.0, to.z - from.z);
        return flat.lengthSqr() < 1.0E-6 ? ahead(mob) : flat.normalize();
    }

    // The ground's own dust thrown up round `at`.
    public static void dust(ServerLevel level, Vec3 at, int count, double spread) {
        BlockState ground = level.getBlockState(BlockPos.containing(at.x, at.y - 0.2, at.z));
        if (ground.isAir()) {
            return;
        }
        ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z, count,
                spread, 0.1, spread, 0.15);
    }
}
