package nl.tivek.multiversepowers.character.thor.storm;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.thor.ThorMoves;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.spell.SpellTargets;

// Thor's lightning bomb: he rises slowly into the air with bolts striking round him, charges for a moment and bursts
// like a bomb of lightning over everything under and round him, which it hurts, throws and knocks down; then he sinks
// back down. His own game lifts him on the same times (`client/motion/ThorRise`); the server strikes, hurts and spares
// him the fall.
public final class LightningBomb {
    // He rises this long, charges this long, then bursts.
    public static final int RISE = 50;
    public static final int CHARGE = 20;
    public static final int BURST = RISE + CHARGE;
    // How high he rises.
    public static final double HEIGHT = 7.0;
    // After the burst he sinks back down: this long at most, no fall hurting him.
    public static final int SINK = 100;
    private static final int SPARK_EVERY = 5;
    // The burst reaches this far below him, so it still takes in the ground he rose from.
    private static final double BELOW = HEIGHT + 3.0;
    private static final Map<UUID, LightningBomb> ALL = new HashMap<>();

    private final float damage;
    private final double radius;
    private int age;

    private LightningBomb(float damage, double radius) {
        this.damage = damage;
        this.radius = radius;
    }

    public static boolean start(ServerPlayer player, float damage, double radius) {
        if (ALL.containsKey(player.getUUID()) || player.isPassenger() || player.isSleeping() || player.isFallFlying()) {
            return false;
        }
        LightningBomb bomb = new LightningBomb(damage, radius);
        ALL.put(player.getUUID(), bomb);
        ThorMoves.spare(player, BURST + SINK);
        ThorMoves.tell(player, ThorStatePayload.BOMB, 0);
        ServerLevel level = player.serverLevel();
        sound(level, player, SoundEvents.TRIDENT_THUNDER.value(), 1.2F, 0.5F);
        sound(level, player, SoundEvents.BEACON_ACTIVATE, 1.0F, 0.7F);
        UUID id = player.getUUID();
        Effects.start(level, (lvl, age) -> bomb.tick(lvl, id, age));
        return true;
    }

    // From the bomb's start until he is down again he does nothing else but call his storm's bolts.
    public static boolean busy(ServerPlayer player) {
        return ALL.containsKey(player.getUUID());
    }

    private boolean tick(ServerLevel level, UUID id, int age) {
        if (ALL.get(id) != this) {
            return false;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
        if (player == null || player.level() != level || !player.isAlive() || age > BURST + SINK
                || age > BURST + 5 && player.onGround()) {
            ALL.remove(id, this);
            return false;
        }
        this.age = age;
        player.connection.aboveGroundTickCount = 0;
        player.resetFallDistance();
        if (age < BURST && age % SPARK_EVERY == 0) {
            this.spark(level, player);
        }
        if (age == RISE) {
            sound(level, player, SoundEvents.WARDEN_SONIC_CHARGE, 1.4F, 1.5F);
        } else if (age == BURST) {
            this.burst(level, player);
        }
        return true;
    }

    // A bolt out of the sky onto the ground round him as he rises and charges.
    private void spark(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double reach = 2.5 + random.nextDouble() * 4.0;
        Vec3 over = new Vec3(player.getX() + Math.cos(angle) * reach, player.getY() + 4.0,
                player.getZ() + Math.sin(angle) * reach);
        Vec3 ground = below(level, player, over, 20.0);
        Vec3 top = ground.add((random.nextDouble() - 0.5) * 3.0, 14.0 + random.nextDouble() * 6.0,
                (random.nextDouble() - 0.5) * 3.0);
        StormFxPayload.send(level, StormFxPayload.SPARK, top, ground, 0.5F);
    }

    // The burst: everything within reach round and under him hurt, the nearer the harder, thrown off and knocked down.
    private void burst(ServerLevel level, ServerPlayer player) {
        Vec3 center = player.position().add(0.0, 0.9 * player.getScale(), 0.0);
        Vec3 ground = below(level, player, center, BELOW);
        StormFxPayload.send(level, StormFxPayload.BLAST, center, ground, (float) this.radius);
        sound(level, player, SoundEvents.LIGHTNING_BOLT_THUNDER, 4.0F, 0.8F);
        sound(level, player, SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 0.7F);
        sound(level, player, SoundEvents.LIGHTNING_BOLT_IMPACT, 2.0F, 0.6F);
        AABB reach = new AABB(center.x - this.radius, center.y - BELOW, center.z - this.radius,
                center.x + this.radius, center.y + this.radius * 0.5, center.z + this.radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, reach,
                entity -> Targeting.mayStrike(player, entity))) {
            double dx = target.getX() - center.x;
            double dz = target.getZ() - center.z;
            double flat = Math.sqrt(dx * dx + dz * dz);
            double far = Math.max(0.0, flat - target.getBbWidth() * 0.5);
            if (far > this.radius) {
                continue;
            }
            double close = 1.0 - 0.6 * far / this.radius;
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(player), this.damage * (float) close);
            Knockdowns.knock(target);
            Vec3 way = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(dx / flat, 0.0, dz / flat);
            SpellTargets.push(target, way, 0.3 + 0.3 * close, 0.1 + 0.1 * close);
        }
    }

    // The first thing straight under `at`, at most `deep` down; nothing there, that far down.
    private static Vec3 below(ServerLevel level, ServerPlayer player, Vec3 at, double deep) {
        Vec3 bottom = at.add(0.0, -deep, 0.0);
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(at, bottom, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, player));
        return hit.getType() == HitResult.Type.MISS ? bottom : hit.getLocation();
    }

    private static void sound(ServerLevel level, ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY() + player.getScale(), player.getZ(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }

    // Knocked down, or no longer Thor: no burst, and all who see him are told so, his own game too.
    public static void stop(ServerPlayer player) {
        LightningBomb bomb = ALL.remove(player.getUUID());
        if (bomb != null && bomb.age < BURST) {
            ThorMoves.tell(player, ThorStatePayload.BOMB, ThorStatePayload.PUT_OUT);
        }
    }

    public static void clear() {
        ALL.clear();
    }
}
