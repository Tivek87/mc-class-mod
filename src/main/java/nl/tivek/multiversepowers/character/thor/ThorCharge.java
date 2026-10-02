package nl.tivek.multiversepowers.character.thor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// Charged: lightning fills Thor for a while (without the hammer in hand) or the hammer (with it). Charged he hits
// harder and throws further, and runs faster; a charged hammer hits harder and throws further.
final class ThorCharge {
    private static final float STRONGER = 1.5F;
    private static final int SPEED_LEVEL = 1;
    private static final Map<UUID, ThorCharge> ALL = new HashMap<>();

    private long thorUntil;
    private long hammerUntil;

    private ThorCharge() {
    }

    static boolean charge(ServerPlayer player, double seconds) {
        ServerLevel level = player.serverLevel();
        int ticks = (int) Math.round(seconds * 20.0);
        long until = level.getGameTime() + ticks;
        boolean hammer = Mjolnir.inHand(player);
        ThorCharge charge = ALL.computeIfAbsent(player.getUUID(), id -> new ThorCharge());
        boolean was = charge.on(level);
        if (hammer) {
            charge.hammerUntil = until;
        } else {
            charge.thorUntil = until;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, SPEED_LEVEL, false, false,
                    true));
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(player.getX(), player.getY(), player.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        Vec3 chest = player.position().add(0.0, player.getScale(), 0.0);
        ParticleFx.sphereOut(level, ParticleFx.dust(ThorMoves.GLOW, 1.4F), chest, 40, 0.35);
        level.playSound(null, chest.x, chest.y, chest.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS,
                1.2F, 1.2F);
        ThorMoves.tell(player, ThorStatePayload.NONE, 0);
        if (!was) {
            UUID id = player.getUUID();
            Effects.start(level, (lvl, age) -> tick(lvl, id));
        }
        return true;
    }

    private boolean on(ServerLevel level) {
        long now = level.getGameTime();
        return now < this.thorUntil || now < this.hammerUntil;
    }

    // Static crawls over him or the hammer while it lasts; once it wears off everyone is told.
    private static boolean tick(ServerLevel level, UUID id) {
        ThorCharge charge = ALL.get(id);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
        if (charge == null || player == null || player.level() != level) {
            ALL.remove(id);
            return false;
        }
        long now = level.getGameTime();
        if (now < charge.thorUntil && now % 3 == 0) {
            double size = player.getScale();
            Vec3 body = player.position().add(ParticleFx.spread(0.6 * size),
                    (0.2 + ParticleFx.RANDOM.nextDouble() * 1.6) * size, ParticleFx.spread(0.6 * size));
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, body, 2, 0.2, 0.1);
        }
        if (now < charge.hammerUntil && now % 3 == 0) {
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, Mjolnir.where(player), 2, 0.12, 0.08);
        }
        if (!charge.on(level)) {
            ALL.remove(id);
            ThorMoves.tell(player, ThorStatePayload.NONE, 0);
            return false;
        }
        return true;
    }

    static float fists(ServerPlayer player) {
        ThorCharge charge = ALL.get(player.getUUID());
        return charge != null && player.level().getGameTime() < charge.thorUntil ? STRONGER : 1.0F;
    }

    static float hammer(ServerPlayer player) {
        ThorCharge charge = ALL.get(player.getUUID());
        return charge != null && player.level().getGameTime() < charge.hammerUntil ? STRONGER : 1.0F;
    }

    static int flags(ServerPlayer player) {
        return (fists(player) > 1.0F ? ThorStatePayload.CHARGED : 0)
                | (hammer(player) > 1.0F ? ThorStatePayload.HAMMER_CHARGED : 0);
    }

    static void leave(ServerPlayer player) {
        ThorCharge charge = ALL.remove(player.getUUID());
        if (charge != null && player.level().getGameTime() < charge.thorUntil) {
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
        }
    }

    static void clear() {
        ALL.clear();
    }
}
