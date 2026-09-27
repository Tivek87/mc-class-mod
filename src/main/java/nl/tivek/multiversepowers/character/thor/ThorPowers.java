package nl.tivek.multiversepowers.character.thor;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.CharacterPowers;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// Thor: a bolt of lightning strikes as you become him and static crawls over you for a while; holding the attack
// button claps his hands in a thunderclap.
public final class ThorPowers implements CharacterPowers {
    // How long the attack button is held for the thunderclap: 0.75 seconds.
    public static final int CLAP_HOLD = 15;
    private static final int CRACKLE_TICKS = 40;
    private static final int GLOW = 0x9FE8FF;

    @Override
    public void enter(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(player.getX(), player.getY(), player.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        Vec3 chest = player.position().add(0.0, 1.0, 0.0);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, chest, 60, 0.6, 0.5);
        ParticleFx.sphereOut(level, ParticleFx.dust(GLOW, 1.4F), chest, 40, 0.35);
        ParticleFx.shockwave(level, ParticleFx.dust(GLOW, 1.2F), player.position().add(0.0, 0.1, 0.0), 32, 0.5);
        Sounds.play(level, chest, SoundEvents.TRIDENT_THUNDER.value(), 1.6F, 1.0F);
        UUID id = player.getUUID();
        Effects.start(level, (lvl, age) -> {
            ServerPlayer thor = lvl.getServer().getPlayerList().getPlayer(id);
            if (thor == null || thor.level() != lvl || age > CRACKLE_TICKS) {
                return false;
            }
            double left = 1.0 - (double) age / CRACKLE_TICKS;
            Vec3 body = thor.position().add(ParticleFx.spread(0.7), 0.2 + ParticleFx.RANDOM.nextDouble() * 1.7,
                    ParticleFx.spread(0.7));
            ParticleFx.cloud(lvl, ParticleTypes.ELECTRIC_SPARK, body, 2 + (int) (6 * left), 0.3, 0.15);
            if (age % 6 == 0) {
                ParticleFx.at(lvl, ParticleFx.dust(GLOW, 0.8F), body);
                Sounds.play(lvl, body, SoundEvents.COPPER_BULB_TURN_ON, 0.5F, 1.6F + 0.3F * (float) left);
            }
            return true;
        });
    }

    @Override
    public void leave(ServerPlayer player) {
        ParticleFx.cloud(player.serverLevel(), ParticleTypes.ELECTRIC_SPARK, player.position().add(0.0, 1.0, 0.0),
                24, 0.5, 0.3);
    }

    @Override
    public boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data) {
        if (!ability.id().equals("thunderclap") || !on || (data & Characters.HOLD) == 0
                || Characters.cooldownLeft(player, ability) > 0) {
            return false;
        }
        // The held version keeps no cooldown of the button's own (see Characters.action): this one starts it.
        boolean cast = Thunderclap.cast(player, player.serverLevel(), ability.getDamage());
        if (cast) {
            Characters.startCooldown(player, ability);
        }
        return cast;
    }

    @Override
    public void clear() {
    }
}
