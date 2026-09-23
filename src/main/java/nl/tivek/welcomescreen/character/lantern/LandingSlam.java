package nl.tivek.welcomescreen.character.lantern;

import java.util.HashSet;
import java.util.Set;
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
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.network.ConstructPayload;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;
import nl.tivek.welcomescreen.spell.SpellFx;

/**
 * A landing at full speed. Green Lantern smashes his ring fist into the ground, and the ring throws up a huge
 * construct in front of him that strikes the ground and sends a shockwave over it. Which one is picked at random every
 * time, out of {@link ConstructPayload#SLAM_KINDS}: things that drop out of the sky (a fist, a hammer, an anvil, a
 * boot, a ton weight, his lantern, a safe, an anchor, a spiked ball, a barbell, a bell, a meteor, a slapping hand, a
 * sword, a piano, a toy brick, a stamp, TNT), things that clap shut (two hands, two fists, cymbals, a bear trap, a
 * book), things that burst out of the ground (an uppercut, spikes, a pillar that topples), things swung down (a fly
 * swatter, a gavel, a pickaxe, drumsticks on a drum), the lantern emblem falling flat, and a volley of rockets. Every
 * creature the wave reaches is hurt (most near the middle) and thrown away from it.
 *
 * <p>Clients play the construct from its age (see {@link ConstructPayload#SLAM}), so the timing below is shared.
 */
public final class LandingSlam implements SpellEffect {
    /** Ticks the construct takes to take shape. */
    public static final int FORM_TICKS = 4;
    /** The tick it strikes and the shockwave goes out. */
    public static final int IMPACT_TICK = 10;
    /** The tick it starts to break up into pieces (or sink away). */
    public static final int BURST_TICK = 18;
    /** The tick it is all over, the wave included. */
    public static final int END_TICK = 34;
    /** How far in front of him most constructs strike, in blocks. */
    public static final double AHEAD = 3.0;
    /**
     * Half the height of the lantern emblem, in blocks: it stands up in front of him and falls flat away from him, so
     * the middle of where it lands (and of its wave) lies this much further out.
     */
    public static final double EMBLEM_HALF = 2.4;
    private static final double VIEW_RANGE = 128.0;
    // How high above and below the middle of the wave a creature can be and still be caught, in blocks.
    private static final double WAVE_HEIGHT = 2.5;
    // A creature caught by the wave always flies up at least this much.
    private static final double LIFT = 0.35;
    // The tick the TNT lands, before it blows up.
    private static final int TNT_LANDS = 7;

    private final int id = PowerRing.newId();
    private final ServerPlayer owner;
    private final int variant;
    private final Vec3 center;
    private final Vec3 facing;
    private final double radius;
    private final float damage;
    private final double knockback;
    private int age;

    private LandingSlam(ServerPlayer owner, CharacterAbility flight, int variant, Vec3 center, Vec3 facing) {
        this.owner = owner;
        this.variant = variant;
        this.center = center;
        this.facing = facing;
        this.radius = flight.value("slamRadiusBlocks");
        this.damage = (float) flight.value("slamDamage");
        this.knockback = flight.value("slamKnockback");
    }

    /**
     * How far in front of him this construct strikes, in blocks: the big ones strike further out, so they never land
     * on top of him.
     */
    public static double ahead(int variant) {
        return switch (variant) {
            case ConstructPayload.SLAM_EMBLEM -> AHEAD + EMBLEM_HALF;
            case ConstructPayload.SLAM_PALM -> 3.8;
            case ConstructPayload.SLAM_PILLAR -> 4.2;
            case ConstructPayload.SLAM_PIANO, ConstructPayload.SLAM_BARBELL -> 3.4;
            default -> AHEAD;
        };
    }

    /** He hits the ground: a random construct takes shape in front of him. */
    static void start(ServerPlayer owner, ServerLevel level, CharacterAbility flight) {
        Vec3 look = owner.getLookAngle();
        Vec3 facing = new Vec3(look.x, 0.0, look.z);
        facing = facing.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : facing.normalize();
        int variant = owner.getRandom().nextInt(ConstructPayload.SLAM_KINDS);
        Vec3 center = ground(level, owner, owner.position().add(facing.scale(ahead(variant))));
        LandingSlam slam = new LandingSlam(owner, flight, variant, center, facing);
        SpellCasting.start(level, slam);
        // His fist hits the ground: a first, smaller thud before the construct's own.
        Vec3 fist = owner.position().add(facing.scale(0.4)).add(right(facing).scale(0.3));
        level.playSound(null, fist.x, fist.y, fist.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, 1.1F);
        dust(level, fist, 0.4, 14);
        dust(level, fist, 1.1, 22);
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 1.4F), fist.add(0.0, 0.2, 0.0), 14, 0.2);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                1.0F, 1.4F);
        slam.send(level);
    }

    /** His right, from the way he faces. */
    private static Vec3 right(Vec3 facing) {
        return facing.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
    }

    /**
     * The top of the ground at this spot: found from a little above where he landed down to a few blocks below, so
     * the construct strikes the ground and not the air over a slope or a ledge.
     */
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
        this.before(level);
        if (this.age == IMPACT_TICK) {
            this.impact(level);
        }
        if (this.age >= END_TICK || this.owner.level() != level) {
            PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
            return false;
        }
        this.send(level);
        return true;
    }

    /** What some constructs do on their way to the strike: the ground rumbles, a fuse hisses, rockets come down. */
    private void before(ServerLevel level) {
        switch (this.variant) {
            case ConstructPayload.SLAM_UPPERCUT, ConstructPayload.SLAM_SPIKES, ConstructPayload.SLAM_PILLAR -> {
                // Something pushes up from below: the ground shakes and bits of it jump.
                if (this.age >= 2 && this.age < IMPACT_TICK && this.age % 2 == 0) {
                    dust(level, this.center, 0.6 + 0.1 * this.age, 10);
                    this.sound(level, SoundEvents.ROOTED_DIRT_BREAK, 0.8F, 0.5F + 0.05F * this.age);
                }
            }
            case ConstructPayload.SLAM_TNT -> {
                if (this.age == TNT_LANDS) {
                    dust(level, this.center, 0.9, 16);
                    this.sound(level, SoundEvents.ANVIL_LAND, 0.6F, 1.3F);
                    this.sound(level, SoundEvents.TNT_PRIMED, 1.2F, 1.0F);
                }
            }
            case ConstructPayload.SLAM_ROCKETS -> {
                // Five rockets, one after the other; the middle one is the strike itself.
                int rocket = this.age - (IMPACT_TICK - 2);
                if (rocket >= 0 && rocket < 5 && rocket != 2) {
                    Vec3 at = rocketTarget(this.center, this.facing, rocket);
                    level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.4, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                    dust(level, at, 0.8, 12);
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                            0.7F, 1.4F);
                }
            }
            default -> {
                // Nothing on the way.
            }
        }
    }

    /**
     * Where each of the five rockets comes down: in the middle, left and right of it, beyond it and just short of
     * it. Clients draw them on the same spots.
     */
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

    /** The construct strikes: the shockwave goes out over the ground. */
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
            // Full force in the middle, half of it at the edge of the wave.
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
            SpellFx.cloud(level, ParticleTypes.CRIT, target.getBoundingBox().getCenter(), 8, 0.3, 0.3);
        }
        // Dust thrown up in a ring, the ground's own, and green light bursting out of the middle.
        dust(level, this.center, this.radius * 0.6, 40);
        dust(level, this.center, this.radius, 50);
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 1.8F), this.center.add(0.0, 0.5, 0.0), 40, 0.45);
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.GREEN, 2.2F), this.center.add(0.0, 0.3, 0.0), 30, 0.3);
        level.sendParticles(this.variant == ConstructPayload.SLAM_TNT ? ParticleTypes.EXPLOSION_EMITTER
                : ParticleTypes.EXPLOSION, this.center.x, this.center.y + 0.5, this.center.z, 1, 0.0, 0.0, 0.0, 0.0);
        this.sounds(level);
    }

    /** What the construct sounds like when it strikes, on top of the boom of the wave itself. */
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

    /** Bits of the ground itself flying up in a ring around {@code at}. */
    private static void dust(ServerLevel level, Vec3 at, double radius, int count) {
        BlockPos below = BlockPos.containing(at.x, at.y - 0.5, at.z);
        BlockState state = level.getBlockState(below);
        if (state.isAir()) {
            return;
        }
        BlockParticleOption bits = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            level.sendParticles(bits, at.x + Math.cos(angle) * radius, at.y + 0.1, at.z + Math.sin(angle) * radius,
                    2, 0.2, 0.1, 0.2, 0.15);
        }
    }

    private void send(ServerLevel level) {
        PacketDistributor.sendToPlayersNear(level, null, this.center.x, this.center.y, this.center.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), this.center, this.facing, (float) this.radius,
                        1.0F, 0.0F, false, ConstructPayload.SLAM, this.variant, this.age, null));
    }
}
