package nl.tivek.welcomescreen.character.lantern;

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
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.network.ConstructPayload;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;
import nl.tivek.welcomescreen.spell.SpellFx;

/**
 * A landing at full speed, or the shockwave key (see {@link Shockwave}). Green Lantern smashes his ring fist into the
 * ground, and the ring throws up a huge construct in front of him that strikes the ground and sends a shockwave over
 * it. Which one is picked at random every time, out of {@link ConstructPayload#SLAM_KINDS}: things that drop out of
 * the sky (a fist, a hammer, an anvil, a boot, a ton weight, his lantern, a safe, an anchor, a spiked ball, a
 * barbell, a bell, a meteor, a slapping hand, a sword, a piano, a toy brick, a stamp, TNT), things that clap shut
 * (two hands, two fists, cymbals, a bear trap, a book), things that burst out of the ground (an uppercut, spikes, a
 * pillar that topples), things swung down (a fly swatter, a gavel, a pickaxe, drumsticks on a drum), the lantern
 * emblem falling flat, and a volley of rockets. Every creature the wave reaches is hurt (most near the middle) and
 * thrown away from it.
 *
 * <p>Clients play the construct from its age (see {@link ConstructPayload#SLAM}), so the timing below is shared. It is
 * counted at the pace the constructs were made for; a slam plays the setting {@code slowMotion} times as slowly, so
 * every one of these ticks lasts that many real ticks.
 */
public final class LandingSlam implements SpellEffect {
    /** Ticks the construct takes to take shape, in the air before him where he can see it. */
    public static final int FORM_TICKS = 6;
    /** The tick it has wound up (risen a little, like a fist drawn back) and sets off to strike. */
    public static final int HANG_TICKS = 9;
    /** The tick it strikes and the shockwave goes out. */
    public static final int IMPACT_TICK = 13;
    /** The tick it starts to break up into pieces (or sink away). */
    public static final int BURST_TICK = 30;
    /** The tick it is all over, the wave included. */
    public static final int END_TICK = 46;
    /** How far in front of him most constructs strike, in blocks. */
    public static final double AHEAD = 4.2;
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
    private static final int TNT_LANDS = 9;
    // Ticks between picking a construct with /constructshockwave and it taking shape.
    private static final int PICK_DELAY = 20;

    // Everyone whose slam is still going: one at a time, so his pose follows the one construct.
    private static final Map<UUID, LandingSlam> RUNNING = new HashMap<>();

    private final int id = PowerRing.newId();
    private final ServerPlayer owner;
    private final int variant;
    // Where it strikes.
    private final Vec3 center;
    private final Vec3 facing;
    private final double radius;
    private final float damage;
    private final double knockback;
    // How slowly it plays (every tick of the timeline above lasts this many real ticks), and how big its construct is.
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

    /**
     * How far in front of him this construct strikes, in blocks: the big ones strike further out, so they never land
     * on top of him, and all of them the further the bigger the constructs are.
     *
     * @param size how big the constructs are (the setting {@code constructScale})
     */
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

    /**
     * He hits the ground: a random construct takes shape in front of him, as long as the ring can pay for it, is not
     * busy at the lantern and has no slam of his going already.
     *
     * @return true when it came; false leaves it at a hard landing
     */
    static boolean start(ServerPlayer owner, ServerLevel level, CharacterAbility shockwave) {
        float cost = (float) shockwave.value("powerCost");
        float power = PowerRing.power(owner);
        if (power + 1.0E-4F < cost || Lantern.busy(owner) || running(owner)) {
            return false;
        }
        PowerRing.setPower(owner, power - cost);
        begin(owner, level, shockwave, owner.getRandom().nextInt(ConstructPayload.SLAM_KINDS));
        return true;
    }

    /**
     * The command {@code /constructshockwave}: a second after he picked it, this construct strikes in front of him,
     * whoever he is, for free. Only for players who may cheat.
     *
     * @return false when he may not, or the variant does not exist
     */
    public static boolean pick(ServerPlayer owner, int variant) {
        CharacterAbility shockwave = GameCharacter.GREEN_LANTERN.byName("shockwave");
        if (!owner.hasPermissions(2) || variant < 0 || variant >= ConstructPayload.SLAM_KINDS || shockwave == null) {
            return false;
        }
        SpellCasting.start(owner.serverLevel(), new SpellEffect() {
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

    /** A construct takes shape in front of him: the one numbered {@code variant}. */
    private static void begin(ServerPlayer owner, ServerLevel level, CharacterAbility shockwave, int variant) {
        Vec3 look = owner.getLookAngle();
        Vec3 facing = new Vec3(look.x, 0.0, look.z);
        facing = facing.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : facing.normalize();
        Vec3 center = ground(level, owner, owner.position().add(facing.scale(ahead(variant,
                shockwave.value("constructScale")))));
        LandingSlam slam = new LandingSlam(owner, shockwave, variant, center, facing);
        RUNNING.put(owner.getUUID(), slam);
        SpellCasting.start(level, slam);
        // His fist hits the ground: a first, smaller thud before the construct's own.
        Vec3 fist = owner.position().add(facing.scale(0.5)).add(right(facing).scale(0.3));
        level.playSound(null, fist.x, fist.y, fist.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, 1.1F);
        dust(level, fist, 0.4, 14);
        dust(level, fist, 1.1, 22);
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 1.4F), fist.add(0.0, 0.2, 0.0), 14, 0.2);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                1.0F, 1.4F);
        slam.send(level);
    }

    /** True while this player's slam is still going, from his fist hitting the ground until its wave has died out. */
    static boolean running(ServerPlayer player) {
        return RUNNING.containsKey(player.getUUID());
    }

    /** The server stops: no slam is going any more. */
    static void clear() {
        RUNNING.clear();
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
        // Every tick of the timeline that went by on this real tick: none, one, or more when it plays fast.
        int from = (int) Math.floor((this.age - 1) / this.pace) + 1;
        int to = (int) Math.floor(this.age / this.pace);
        for (int step = from; step <= to; step++) {
            this.before(level, step);
            if (step == IMPACT_TICK) {
                this.impact(level);
            }
        }
        if (this.age >= END_TICK * this.pace || this.owner.level() != level) {
            PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
            RUNNING.remove(this.owner.getUUID(), this);
            return false;
        }
        this.send(level);
        return true;
    }

    /**
     * What some constructs do on their way to the strike: the ground rumbles, a fuse hisses, rockets come down.
     *
     * @param step the tick of the timeline (see {@link #IMPACT_TICK}) that just went by
     */
    private void before(ServerLevel level, int step) {
        switch (this.variant) {
            case ConstructPayload.SLAM_UPPERCUT, ConstructPayload.SLAM_SPIKES, ConstructPayload.SLAM_PILLAR -> {
                // Something pushes up from below: the ground shakes and bits of it jump.
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
                // Five rockets, one after the other; the middle one is the strike itself.
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
                        (float) this.size, (float) this.pace, false, ConstructPayload.SLAM, this.variant, this.age,
                        null));
    }
}
