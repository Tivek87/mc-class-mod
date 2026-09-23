package nl.tivek.welcomescreen.character.lantern;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
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
 * Green Lantern's ultimate, the Construct Storm. He throws his ring fist up at the sky: a pillar of light shoots out of
 * the ring and opens into a great ring of light high over his head. Then, for the setting {@code durationTicks}, that
 * ring hangs over him wherever he goes and rains constructs down out of the sky, one every {@code dropTicks}: each
 * takes shape under the ring over a creature within {@code radiusBlocks} that is out to hurt him (or a player, where
 * players may fight each other), keeps over it until it sets off, and drops onto it with a shockwave of its own (see
 * {@link LandingSlam#drop}): the ability's damage in the middle, half of it at the edge, and a throw away from it. The
 * constructs share themselves out over the creatures there are; with none about they crash down round him on empty
 * ground. Once the rain is over the ring bursts.
 *
 * <p>The ring in the sky and the pillar are light, not constructs. What drops are constructs: the landing slam's ones
 * that come down out of the sky, following every rule the others do.
 */
public final class ConstructStorm implements SpellEffect {
    /** How long he holds his ring fist up to call the storm, in ticks: the first construct drops right after. */
    public static final int CALL_TICKS = 24;
    /** The tick the pillar of light reaches the sky and the ring bursts open. */
    public static final int OPEN_TICK = 10;
    /** How long the ring takes to burst once the rain is over, in ticks. */
    public static final int BURST_TICKS = 14;
    /** How wide the ring in the sky is, in blocks. */
    public static final double RING_RADIUS = 6.5;
    // How high over his eyes the ring hangs, and how low it may come under a roof, in blocks.
    private static final double HEIGHT = 15.0;
    private static final double LOW = 3.5;
    // How slowly the constructs play (see the shockwave's slowMotion); how much smaller or bigger than the shockwave's
    // own constructs they are, at random; how far their wave reaches for their size, and how hard it throws.
    private static final double PACE = 1.6;
    private static final double SMALLEST = 0.85;
    private static final double BIGGEST = 1.25;
    private static final double WAVE_PER_SIZE = 2.6;
    private static final double KNOCKBACK = 1.0;
    // How long a creature is left alone after a construct was sent its way, in ticks, so they share themselves out.
    private static final int SPREAD_TICKS = 16;
    // How far below and above him creatures are still in reach, in blocks; how close to him one may be at most (what
    // drops on it would come down on him as well); and how much open sky a creature needs over it to be picked before
    // one under a roof.
    private static final double BELOW = 16.0;
    private static final double ABOVE = 8.0;
    private static final double NEAREST = 4.0;
    private static final double OPEN_SKY = 5.0;
    private static final double VIEW_RANGE = 160.0;
    /** The constructs that drop out of the sky: the only ones that suit a storm. */
    private static final int[] KINDS = { ConstructPayload.SLAM_FIST, ConstructPayload.SLAM_HAMMER,
            ConstructPayload.SLAM_ANVIL, ConstructPayload.SLAM_BOOT, ConstructPayload.SLAM_WEIGHT,
            ConstructPayload.SLAM_SWORD, ConstructPayload.SLAM_LANTERN, ConstructPayload.SLAM_SAFE,
            ConstructPayload.SLAM_ANCHOR, ConstructPayload.SLAM_MACE, ConstructPayload.SLAM_BARBELL,
            ConstructPayload.SLAM_BELL, ConstructPayload.SLAM_PALM, ConstructPayload.SLAM_PIANO,
            ConstructPayload.SLAM_BRICK, ConstructPayload.SLAM_STAMP };

    private static final Map<UUID, ConstructStorm> ACTIVE = new HashMap<>();

    private final int id = PowerRing.newId();
    private final ServerPlayer owner;
    private final CharacterAbility ability;
    // How long the rain lasts, and how many ticks go between two constructs.
    private final int rain;
    private final int every;
    // When a construct was last sent at each creature (by its entity id), in ticks of the storm.
    private final Map<Integer, Integer> sent = new HashMap<>();
    private int age;
    private double height = HEIGHT;
    private int last = -1;

    private ConstructStorm(ServerPlayer owner, CharacterAbility ability) {
        this.owner = owner;
        this.ability = ability;
        this.rain = Math.max(1, ability.intValue("durationTicks"));
        this.every = Math.max(1, ability.intValue("dropTicks"));
    }

    /**
     * The ultimate's key: he throws his ring fist up at the sky and calls the storm, as long as the ring is free and
     * can pay for it.
     *
     * @return true when it began
     */
    static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (ACTIVE.containsKey(owner.getUUID())) {
            return false;
        }
        if (Lantern.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        if (GiantFist.holding(owner)) {
            PowerRing.tell(owner, "busy_fist");
            return false;
        }
        float cost = (float) ability.value("powerCost");
        float power = PowerRing.power(owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        PowerRing.setPower(owner, power - cost);
        // The ring hand goes up: the beam it pours out stops.
        LightBeam.stop(owner);
        ConstructStorm storm = new ConstructStorm(owner, ability);
        storm.height = storm.sky(level);
        ACTIVE.put(owner.getUUID(), storm);
        SpellCasting.start(level, storm);
        PowerRing.tell(owner, "storm");
        Vec3 eye = owner.getEyePosition();
        storm.sound(level, eye, SoundEvents.BEACON_POWER_SELECT, 1.4F, 0.6F);
        storm.sound(level, eye, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F, 0.5F);
        storm.send(level);
        return true;
    }

    /** True while this player holds his ring fist up to call his storm: the ring hand does nothing else then. */
    static boolean calling(ServerPlayer player) {
        ConstructStorm storm = ACTIVE.get(player.getUUID());
        return storm != null && storm.age < CALL_TICKS;
    }

    /** How many ticks this player's storm still goes on, or 0 when he has none: shown as his ultimate. */
    public static int left(ServerPlayer player) {
        ConstructStorm storm = ACTIVE.get(player.getUUID());
        return storm == null ? 0 : Math.max(0, storm.total() - storm.age);
    }

    /** The server stops: no storm is going any more. */
    static void clear() {
        ACTIVE.clear();
    }

    /** How long the whole storm lasts, from calling it to the ring bursting, in ticks. */
    private int total() {
        return CALL_TICKS + this.rain + BURST_TICKS;
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (ACTIVE.get(this.owner.getUUID()) != this) {
            return false;
        }
        if (!PowerRing.fuels(this.owner, level)) {
            this.end(level);
            return false;
        }
        this.age++;
        this.height = this.sky(level);
        Vec3 ring = this.center();
        if (this.age == OPEN_TICK) {
            // The pillar of light reaches the sky and the ring bursts open.
            this.sound(level, ring, SoundEvents.BEACON_ACTIVATE, 3.0F, 0.7F);
            this.sound(level, ring, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 3.0F, 0.5F);
            this.sound(level, this.owner.getEyePosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5F, 0.6F);
            SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 2.5F), ring, 60, 0.7);
            SpellFx.sphereOut(level, SpellFx.dust(PowerRing.GREEN, 3.0F), ring, 40, 0.5);
            level.sendParticles(ParticleTypes.FLASH, ring.x, ring.y, ring.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        int raining = this.age - CALL_TICKS;
        if (raining >= 0 && raining < this.rain) {
            if (raining % this.every == 0) {
                this.drop(level);
            }
            if (raining % 40 == 0) {
                // The ring hums over it all.
                this.sound(level, ring, SoundEvents.BEACON_AMBIENT, 2.5F, 0.6F);
            }
        }
        if (raining == this.rain) {
            // The rain is over: the ring bursts.
            this.sound(level, ring, SoundEvents.BEACON_DEACTIVATE, 3.0F, 0.8F);
            this.sound(level, ring, SoundEvents.AMETHYST_CLUSTER_BREAK, 3.0F, 0.6F);
            SpellFx.sphereOut(level, SpellFx.dust(PowerRing.GREEN, 2.5F), ring, 50, 0.6);
        }
        if (this.age >= this.total()) {
            this.end(level);
            return false;
        }
        this.send(level);
        return true;
    }

    /** One construct takes shape under the ring over a creature (or over empty ground round him) and drops. */
    private void drop(ServerLevel level) {
        RandomSource random = this.owner.getRandom();
        double radius = this.ability.value("radiusBlocks");
        LivingEntity target = this.target(level, radius);
        Vec3 ground;
        if (target != null) {
            ground = LandingSlam.groundUnder(level, target);
            this.sent.put(target.getId(), this.age);
        } else {
            ground = this.emptyGround(level, radius, random);
            if (ground == null) {
                return;
            }
        }
        // Never the same one twice in a row.
        int pick = random.nextInt(KINDS.length);
        if (KINDS[pick] == this.last) {
            pick = (pick + 1 + random.nextInt(KINDS.length - 1)) % KINDS.length;
        }
        this.last = KINDS[pick];
        CharacterAbility shockwave = GameCharacter.GREEN_LANTERN.byName("shockwave");
        double scale = shockwave == null ? 1.35 : shockwave.value("constructScale");
        double size = scale * (SMALLEST + (BIGGEST - SMALLEST) * random.nextDouble());
        LandingSlam.drop(this.owner, level, this.last, ground, target, WAVE_PER_SIZE * size, this.ability.getDamage(),
                KNOCKBACK, PACE, size);
    }

    /**
     * Who the next construct drops on: of the creatures in reach that are fair game (and not right beside him), the one
     * that waited longest for one (never is longest), the nearest of those, and one under open sky before one under a
     * roof. Null when there is nobody.
     */
    @Nullable
    private LivingEntity target(ServerLevel level, double radius) {
        Vec3 at = this.owner.position();
        AABB area = new AABB(at.x - radius, at.y - BELOW, at.z - radius, at.x + radius, at.y + ABOVE, at.z + radius);
        LivingEntity open = null;
        LivingEntity covered = null;
        double openScore = Double.MAX_VALUE;
        double coveredScore = Double.MAX_VALUE;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> PowerRing.canHit(this.owner, entity) && this.fair(entity))) {
            double dx = living.getX() - at.x;
            double dz = living.getZ() - at.z;
            double flat = dx * dx + dz * dz;
            Integer when = this.sent.get(living.getId());
            int since = when == null ? Integer.MAX_VALUE : this.age - when;
            if (flat > radius * radius || flat < NEAREST * NEAREST || since < SPREAD_TICKS) {
                continue;
            }
            double score = (when == null ? 0.0 : 1.0E6 / since) + flat * 1.0E-3;
            if (this.underSky(level, living)) {
                if (score < openScore) {
                    openScore = score;
                    open = living;
                }
            } else if (score < coveredScore) {
                coveredScore = score;
                covered = living;
            }
        }
        return open != null ? open : covered;
    }

    /** Whether a construct may be sent at this creature: it is out to hurt him, or a player he may fight. */
    private boolean fair(LivingEntity living) {
        return living instanceof Enemy || living instanceof Player
                || living instanceof Mob mob && mob.getTarget() == this.owner;
    }

    /** Whether there is open sky over this creature a while up, so what drops on it does not come through a roof. */
    private boolean underSky(ServerLevel level, LivingEntity living) {
        Vec3 head = living.getEyePosition();
        return level.clip(new ClipContext(head, head.add(0.0, OPEN_SKY, 0.0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, living)).getType() == HitResult.Type.MISS;
    }

    /**
     * A spot of ground round him with no creature near it, for a construct to crash down on while there is nobody to
     * drop it on: never on a pet or a villager. Null when none was found.
     */
    @Nullable
    private Vec3 emptyGround(ServerLevel level, double radius, RandomSource random) {
        double clear = WAVE_PER_SIZE * BIGGEST * 1.35 + 1.0;
        for (int tries = 0; tries < 6; tries++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double far = 4.0 + random.nextDouble() * Math.max(1.0, radius * 0.6 - 4.0);
            Vec3 top = this.owner.position().add(Math.cos(angle) * far, 2.0, Math.sin(angle) * far);
            BlockHitResult hit = level.clip(new ClipContext(top, top.subtract(0.0, BELOW + 12.0, 0.0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this.owner));
            if (hit.getType() == HitResult.Type.MISS) {
                continue;
            }
            Vec3 ground = hit.getLocation();
            if (level.getEntitiesOfClass(LivingEntity.class, new AABB(ground, ground).inflate(clear, 3.0, clear),
                    entity -> PowerRing.canHit(this.owner, entity)).isEmpty()) {
                return ground;
            }
        }
        return null;
    }

    /** How high over his eyes the ring hangs: as high as it goes, or under the roof over him. */
    private double sky(ServerLevel level) {
        Vec3 eye = this.owner.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(0.0, HEIGHT + 1.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.owner));
        return hit.getType() == HitResult.Type.MISS ? HEIGHT : Math.max(LOW, eye.distanceTo(hit.getLocation()) - 1.0);
    }

    /** Where the ring hangs: straight over his head. */
    private Vec3 center() {
        return this.owner.getEyePosition().add(0.0, this.height, 0.0);
    }

    private void end(ServerLevel level) {
        ACTIVE.remove(this.owner.getUUID(), this);
        PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
    }

    private void send(ServerLevel level) {
        Vec3 ring = this.center();
        PacketDistributor.sendToPlayersNear(level, null, ring.x, ring.y, ring.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), ring, this.owner.getLookAngle(), (float) RING_RADIUS,
                        (float) this.height, (float) this.total(), false, ConstructPayload.STORM, 0, this.age, null));
    }

    private void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
