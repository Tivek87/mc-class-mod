package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

public final class LandingSlam implements Effect {
    public static final int FORM_TICKS = 6;
    public static final int HANG_TICKS = 9;
    public static final int IMPACT_TICK = 13;
    public static final int BURST_TICK = 30;
    public static final int END_TICK = 46;
    public static final double AHEAD = 4.2;
    public static final double EMBLEM_HALF = 2.4;
    private static final double VIEW_RANGE = 128.0;
    private static final double WAVE_HEIGHT = 2.5;
    private static final double LIFT = 0.35;
    private static final int TNT_LANDS = 9;
    private static final int PICK_DELAY = 20;

    private static final Map<UUID, LandingSlam> RUNNING = new HashMap<>();

    private final int id = PowerRing.newId();
    private final ServerPlayer owner;
    private final int variant;
    private final Vec3 center;
    private final Vec3 facing;
    private final double radius;
    private final float damage;
    private final double knockback;
    private final double pace;
    private final double size;
    private int age;

    private LandingSlam(ServerPlayer owner, CharacterAbility shockwave, int variant, Vec3 center, Vec3 facing) {
        this.owner = owner;
        this.variant = variant;
        this.center = center;
        this.facing = facing;
        this.radius = shockwave.value("radiusBlocks");
        this.damage = shockwave.getDamage();
        this.knockback = shockwave.value("knockback");
        this.pace = shockwave.value("slowMotion");
        this.size = shockwave.value("constructScale");
    }

    public static double ahead(int variant, double size) {
        double ahead = switch (variant) {
            case ConstructPayload.SLAM_EMBLEM -> AHEAD + EMBLEM_HALF;
            case ConstructPayload.SLAM_PALM -> 5.0;
            case ConstructPayload.SLAM_PILLAR -> 5.4;
            case ConstructPayload.SLAM_PIANO, ConstructPayload.SLAM_BARBELL -> 4.6;
            default -> AHEAD;
        };
        return ahead * size;
    }

    static boolean start(ServerPlayer owner, ServerLevel level, CharacterAbility shockwave) {
        float cost = (float) shockwave.value("powerCost");
        float power = PowerRing.power(owner);
        if (power + 1.0E-4F < cost || Recharge.busy(owner) || running(owner)) {
            return false;
        }
        PowerRing.setPower(owner, power - cost);
        begin(owner, level, shockwave, owner.getRandom().nextInt(ConstructPayload.SLAM_KINDS));
        return true;
    }

    public static boolean pick(ServerPlayer owner, int variant) {
        CharacterAbility shockwave = GameCharacter.GREEN_LANTERN.byName("shockwave");
        if (!owner.hasPermissions(2) || variant < 0 || variant >= ConstructPayload.SLAM_KINDS || shockwave == null) {
            return false;
        }
        Effects.start(owner.serverLevel(), new Effect() {
            @Override
            public boolean tick(ServerLevel level, int age) {
                if (age < PICK_DELAY) {
                    return owner.isAlive() && owner.level() == level;
                }
                if (running(owner)) {
                    PowerRing.tell(owner, "slam_busy");
                } else {
                    begin(owner, level, shockwave, variant);
                }
                return false;
            }
        });
        return true;
    }

    private static void begin(ServerPlayer owner, ServerLevel level, CharacterAbility shockwave, int variant) {
        Vec3 look = owner.getLookAngle();
        Vec3 facing = new Vec3(look.x, 0.0, look.z);
        facing = facing.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : facing.normalize();
        Vec3 center = ground(level, owner, owner.position().add(facing.scale(ahead(variant,
                shockwave.value("constructScale")))));
        LandingSlam slam = new LandingSlam(owner, shockwave, variant, center, facing);
        RUNNING.put(owner.getUUID(), slam);
        Effects.start(level, slam);
        Vec3 fist = owner.position().add(facing.scale(0.5)).add(right(facing).scale(0.3));
        level.playSound(null, fist.x, fist.y, fist.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, 1.1F);
        dust(level, fist, 0.4, 14);
        dust(level, fist, 1.1, 22);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), fist.add(0.0, 0.2, 0.0), 14, 0.2);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                1.0F, 1.4F);
        slam.send(level);
    }

    static boolean running(ServerPlayer player) {
        return RUNNING.containsKey(player.getUUID());
    }

    public static void clear() {
        RUNNING.clear();
    }

    private static Vec3 right(Vec3 facing) {
        return facing.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
    }

    private static Vec3 ground(ServerLevel level, ServerPlayer owner, Vec3 at) {
        double feet = owner.getY();
        Vec3 from = new Vec3(at.x, feet + 2.0, at.z);
        Vec3 to = new Vec3(at.x, feet - 4.0, at.z);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                owner));
        return hit.getType() == HitResult.Type.MISS ? new Vec3(at.x, feet, at.z) : hit.getLocation();
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        this.age++;
        // Real ticks scaled by pace into timeline ticks: may skip or repeat a step, staying in sync with clients.
        int from = (int) Math.floor((this.age - 1) / this.pace) + 1;
        int to = (int) Math.floor(this.age / this.pace);
        for (int step = from; step <= to; step++) {
            this.before(level, step);
            if (step == IMPACT_TICK) {
                this.impact(level);
            }
        }
        if (this.age >= END_TICK * this.pace || this.owner.level() != level) {
            ConstructPayload.sendRemove(level, this.id, this.center);
            RUNNING.remove(this.owner.getUUID(), this);
            return false;
        }
        this.send(level);
        return true;
    }

    private void before(ServerLevel level, int step) {
        switch (this.variant) {
            case ConstructPayload.SLAM_UPPERCUT, ConstructPayload.SLAM_SPIKES, ConstructPayload.SLAM_PILLAR -> {
                if (step >= 2 && step < IMPACT_TICK && step % 2 == 0) {
                    dust(level, this.center, 0.6 + 0.1 * step, 10);
                    this.sound(level, SoundEvents.ROOTED_DIRT_BREAK, 0.8F, 0.5F + 0.05F * step);
                }
            }
            case ConstructPayload.SLAM_TNT -> {
                if (step == TNT_LANDS) {
                    dust(level, this.center, 0.9, 16);
                    this.sound(level, SoundEvents.ANVIL_LAND, 0.6F, 1.3F);
                    this.sound(level, SoundEvents.TNT_PRIMED, 1.2F, 1.0F);
                }
            }
            case ConstructPayload.SLAM_ROCKETS -> {
                int rocket = step - (IMPACT_TICK - 2);
                if (rocket >= 0 && rocket < 5 && rocket != 2) {
                    Vec3 at = rocketTarget(this.center, this.facing, rocket);
                    level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.4, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                    dust(level, at, 0.8, 12);
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                            0.7F, 1.4F);
                }
            }
            default -> {
            }
        }
    }

    public static Vec3 rocketTarget(Vec3 center, Vec3 facing, int rocket) {
        Vec3 right = right(facing);
        return switch (rocket) {
            case 0 -> center.add(right.scale(2.3)).add(facing.scale(0.6));
            case 1 -> center.subtract(right.scale(2.3)).add(facing.scale(0.6));
            case 3 -> center.add(facing.scale(2.2));
            case 4 -> center.subtract(facing.scale(0.9)).add(right.scale(1.2));
            default -> center;
        };
    }

    private void impact(ServerLevel level) {
        Set<Integer> hit = new HashSet<>();
        AABB area = new AABB(this.center, this.center).inflate(this.radius, WAVE_HEIGHT, this.radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> PowerRing.canHit(this.owner, entity))) {
            Vec3 at = target.position();
            Vec3 away = new Vec3(at.x - this.center.x, 0.0, at.z - this.center.z);
            double distance = away.length();
            if (distance > this.radius || !hit.add(target.getId())) {
                continue;
            }
            double near = 1.0 - 0.5 * distance / this.radius;
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) (this.damage * near));
            Vec3 way = distance < 1.0E-3 ? this.facing : away.scale(1.0 / distance);
            double push = this.knockback * (0.6 + 0.4 * near);
            double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            target.setDeltaMovement(new Vec3(way.x * push, LIFT + 0.3 * push, way.z * push).scale(1.0 - resist));
            target.hasImpulse = true;
            // Players move themselves on their own client, so they have to be told about the push.
            target.hurtMarked = true;
            ParticleFx.cloud(level, ParticleTypes.CRIT, target.getBoundingBox().getCenter(), 8, 0.3, 0.3);
        }
        dust(level, this.center, this.radius * 0.6, 40);
        dust(level, this.center, this.radius, 50);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.8F), this.center.add(0.0, 0.5, 0.0), 40, 0.45);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.GREEN, 2.2F), this.center.add(0.0, 0.3, 0.0), 30, 0.3);
        level.sendParticles(this.variant == ConstructPayload.SLAM_TNT ? ParticleTypes.EXPLOSION_EMITTER
                : ParticleTypes.EXPLOSION, this.center.x, this.center.y + 0.5, this.center.z, 1, 0.0, 0.0, 0.0, 0.0);
        this.sounds(level);
    }

    private void sounds(ServerLevel level) {
        this.sound(level, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.4F, 0.8F);
        this.sound(level, SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 1.3F);
        switch (this.variant) {
            case ConstructPayload.SLAM_HANDS -> this.sound(level, SoundEvents.WIND_CHARGE_BURST.value(), 1.4F, 0.7F);
            case ConstructPayload.SLAM_HAMMER, ConstructPayload.SLAM_ANVIL -> this.sound(level,
                    SoundEvents.ANVIL_LAND, 1.2F, 0.55F);
            case ConstructPayload.SLAM_EMBLEM -> this.sound(level, SoundEvents.BEACON_ACTIVATE, 1.2F, 1.2F);
            case ConstructPayload.SLAM_CYMBALS -> {
                this.sound(level, SoundEvents.BELL_BLOCK, 1.5F, 1.8F);
                this.sound(level, SoundEvents.BELL_RESONATE, 1.2F, 1.6F);
            }
            case ConstructPayload.SLAM_UPPERCUT -> {
                this.sound(level, SoundEvents.PLAYER_ATTACK_STRONG, 1.4F, 0.5F);
                this.sound(level, SoundEvents.ROOTED_DIRT_BREAK, 1.5F, 0.6F);
            }
            case ConstructPayload.SLAM_SPIKES -> {
                this.sound(level, SoundEvents.EVOKER_FANGS_ATTACK, 1.5F, 0.7F);
                this.sound(level, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.4F, 0.8F);
            }
            case ConstructPayload.SLAM_BOOT -> this.sound(level, SoundEvents.RAVAGER_STEP, 1.6F, 0.5F);
            case ConstructPayload.SLAM_WEIGHT -> {
                this.sound(level, SoundEvents.ANVIL_LAND, 1.4F, 0.4F);
                this.sound(level, SoundEvents.CHAIN_FALL, 1.2F, 0.6F);
            }
            case ConstructPayload.SLAM_SWORD -> {
                this.sound(level, SoundEvents.TRIDENT_HIT_GROUND, 1.5F, 0.6F);
                this.sound(level, SoundEvents.ANVIL_PLACE, 0.8F, 1.6F);
            }
            case ConstructPayload.SLAM_ROCKETS -> {
                this.sound(level, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.5F, 0.6F);
                this.sound(level, SoundEvents.FIREWORK_ROCKET_BLAST_FAR, 1.5F, 0.8F);
            }
            case ConstructPayload.SLAM_SWATTER, ConstructPayload.SLAM_PALM -> {
                this.sound(level, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.5F, 0.5F);
                this.sound(level, SoundEvents.GENERIC_BIG_FALL, 1.5F, 0.6F);
            }
            case ConstructPayload.SLAM_LANTERN -> {
                this.sound(level, SoundEvents.BEACON_POWER_SELECT, 1.5F, 0.8F);
                this.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 0.7F);
            }
            case ConstructPayload.SLAM_SAFE -> {
                this.sound(level, SoundEvents.IRON_DOOR_CLOSE, 1.5F, 0.5F);
                this.sound(level, SoundEvents.ANVIL_LAND, 1.0F, 0.7F);
            }
            case ConstructPayload.SLAM_ANCHOR -> {
                this.sound(level, SoundEvents.CHAIN_BREAK, 1.4F, 0.6F);
                this.sound(level, SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
            }
            case ConstructPayload.SLAM_MACE -> {
                this.sound(level, SoundEvents.MACE_SMASH_AIR, 1.5F, 0.6F);
                this.sound(level, SoundEvents.IRON_GOLEM_ATTACK, 1.4F, 0.6F);
            }
            case ConstructPayload.SLAM_BARBELL -> {
                this.sound(level, SoundEvents.ANVIL_PLACE, 1.3F, 0.5F);
                this.sound(level, SoundEvents.CHAIN_FALL, 1.3F, 0.8F);
            }
            case ConstructPayload.SLAM_BELL -> {
                this.sound(level, SoundEvents.BELL_BLOCK, 2.0F, 0.6F);
                this.sound(level, SoundEvents.BELL_RESONATE, 1.5F, 0.7F);
            }
            case ConstructPayload.SLAM_METEOR -> {
                this.sound(level, SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.6F);
                this.sound(level, SoundEvents.FIRECHARGE_USE, 1.4F, 0.6F);
            }
            case ConstructPayload.SLAM_GAVEL -> {
                this.sound(level, SoundEvents.WOOD_HIT, 1.6F, 0.5F);
                this.sound(level, SoundEvents.ANVIL_PLACE, 0.8F, 1.3F);
            }
            case ConstructPayload.SLAM_PICKAXE -> {
                this.sound(level, SoundEvents.STONE_BREAK, 1.6F, 0.6F);
                this.sound(level, SoundEvents.ANVIL_LAND, 0.8F, 1.4F);
            }
            case ConstructPayload.SLAM_TRAP -> {
                this.sound(level, SoundEvents.IRON_TRAPDOOR_CLOSE, 1.6F, 0.5F);
                this.sound(level, SoundEvents.EVOKER_FANGS_ATTACK, 1.2F, 0.9F);
            }
            case ConstructPayload.SLAM_BOOK -> {
                this.sound(level, SoundEvents.BOOK_PUT, 1.6F, 0.5F);
                this.sound(level, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.4F, 0.6F);
            }
            case ConstructPayload.SLAM_DRUM -> {
                this.sound(level, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 2.0F, 0.5F);
                this.sound(level, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.5F);
            }
            case ConstructPayload.SLAM_PILLAR -> {
                this.sound(level, SoundEvents.STONE_BREAK, 1.6F, 0.5F);
                this.sound(level, SoundEvents.GENERIC_BIG_FALL, 1.5F, 0.5F);
            }
            case ConstructPayload.SLAM_TNT -> this.sound(level, SoundEvents.GENERIC_EXPLODE.value(), 1.8F, 0.8F);
            case ConstructPayload.SLAM_PIANO -> {
                this.sound(level, SoundEvents.WOOD_BREAK, 1.6F, 0.6F);
                this.sound(level, SoundEvents.NOTE_BLOCK_HARP.value(), 1.4F, 0.5F);
                this.sound(level, SoundEvents.NOTE_BLOCK_HARP.value(), 1.4F, 0.63F);
                this.sound(level, SoundEvents.NOTE_BLOCK_HARP.value(), 1.4F, 0.75F);
            }
            case ConstructPayload.SLAM_BRICK -> {
                this.sound(level, SoundEvents.STONE_HIT, 1.6F, 1.6F);
                this.sound(level, SoundEvents.ANVIL_PLACE, 0.8F, 1.8F);
            }
            case ConstructPayload.SLAM_STAMP -> {
                this.sound(level, SoundEvents.PISTON_EXTEND, 1.6F, 0.5F);
                this.sound(level, SoundEvents.ANVIL_PLACE, 1.0F, 0.8F);
            }
            default -> this.sound(level, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.4F, 0.6F);
        }
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, this.center.x, this.center.y, this.center.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void dust(ServerLevel level, Vec3 at, double radius, int count) {
        BlockPos below = BlockPos.containing(at.x, at.y - 0.5, at.z);
        BlockState state = level.getBlockState(below);
        if (state.isAir()) {
            return;
        }
        BlockParticleOption bits = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            ParticleFx.sendNear(level, bits, at.x + Math.cos(angle) * radius, at.y + 0.1,
                    at.z + Math.sin(angle) * radius, 2, 0.2, 0.1, 0.2, 0.15);
        }
    }

    private void send(ServerLevel level) {
        PacketDistributor.sendToPlayersNear(level, null, this.center.x, this.center.y, this.center.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), this.center, this.facing, (float) this.radius,
                        (float) this.size, (float) this.pace, false, ConstructPayload.SLAM, this.variant, this.age,
                        null));
    }
}
