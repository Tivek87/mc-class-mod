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
 * A landing at full speed. Green Lantern hits the ground with his ring fist, and the ring throws up a huge construct
 * in front of him that slams down and sends a shockwave over the ground: a fist from above, two hands or two fists
 * that clap together, a hammer, the lantern emblem, an anvil or two cymbals, a different one every time. Every
 * creature the wave reaches is hurt (most near the middle) and thrown away from it.
 *
 * <p>Clients play the construct from its age (see {@link ConstructPayload#SLAM}), so the timing below is shared.
 */
public final class LandingSlam implements SpellEffect {
    /** Ticks the construct takes to take shape. */
    public static final int FORM_TICKS = 4;
    /** The tick it strikes and the shockwave goes out. */
    public static final int IMPACT_TICK = 8;
    /** The tick it starts to break up into light. */
    public static final int BURST_TICK = 12;
    /** The tick it is all over, the wave included. */
    public static final int END_TICK = 26;
    /** How far in front of him the construct strikes, in blocks. */
    public static final double AHEAD = 2.5;
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

    /** He hits the ground: a random construct takes shape in front of him. */
    static void start(ServerPlayer owner, ServerLevel level, CharacterAbility flight) {
        Vec3 look = owner.getLookAngle();
        Vec3 facing = new Vec3(look.x, 0.0, look.z);
        facing = facing.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : facing.normalize();
        int variant = owner.getRandom().nextInt(ConstructPayload.SLAM_KINDS);
        double ahead = AHEAD + (variant == ConstructPayload.SLAM_EMBLEM ? EMBLEM_HALF : 0.0);
        Vec3 center = ground(level, owner, owner.position().add(facing.scale(ahead)));
        LandingSlam slam = new LandingSlam(owner, flight, variant, center, facing);
        SpellCasting.start(level, slam);
        // His fist hits the ground: a first, smaller thud before the construct's own.
        Vec3 feet = owner.position();
        level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, 1.1F);
        dust(level, feet, 1.2, 18);
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 1.4F), feet.add(0.0, 0.2, 0.0), 14, 0.2);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                1.0F, 1.4F);
        slam.send(level);
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
        level.sendParticles(ParticleTypes.EXPLOSION, this.center.x, this.center.y + 0.5, this.center.z, 1, 0.0, 0.0,
                0.0, 0.0);
        this.sounds(level);
    }

    /** What the construct sounds like when it strikes, on top of the boom of the wave itself. */
    private void sounds(ServerLevel level) {
        this.sound(level, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.4F, 0.8F);
        this.sound(level, SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 1.3F);
        switch (this.variant) {
            case ConstructPayload.SLAM_HANDS -> this.sound(level, SoundEvents.WIND_CHARGE_BURST.value(), 1.4F, 0.7F);
            case ConstructPayload.SLAM_FISTS -> this.sound(level, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.4F, 0.6F);
            case ConstructPayload.SLAM_HAMMER -> this.sound(level, SoundEvents.ANVIL_LAND, 0.8F, 0.6F);
            case ConstructPayload.SLAM_EMBLEM -> this.sound(level, SoundEvents.BEACON_ACTIVATE, 1.2F, 1.2F);
            case ConstructPayload.SLAM_ANVIL -> this.sound(level, SoundEvents.ANVIL_LAND, 1.4F, 0.5F);
            case ConstructPayload.SLAM_CYMBALS -> {
                this.sound(level, SoundEvents.BELL_BLOCK, 1.5F, 1.8F);
                this.sound(level, SoundEvents.BELL_RESONATE, 1.2F, 1.6F);
            }
            default -> this.sound(level, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.7F);
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
