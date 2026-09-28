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

// Thor: a bolt of lightning strikes as you become him and static crawls over you for a while. His moves are in
// ThorMoves (dash, super jump, flight and what he does in flight), ThorBlows (his combo), GrabDive and Thunderclap.
public final class ThorPowers implements CharacterPowers {
    // How long the attack button is held to wind up the thunderclap: 0.75 seconds.
    public static final int CLAP_HOLD = 15;
    // How long space is held to fly, right held for the dive and the scroll wheel for lightning speed.
    public static final int FLIGHT_HOLD = 8;
    public static final int DIVE_HOLD = 10;
    public static final int LIGHTNING_HOLD = 4;
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
        ThorMoves.leave(player);
        ThorBlows.forget(player);
        ParticleFx.cloud(player.serverLevel(), ParticleTypes.ELECTRIC_SPARK, player.position().add(0.0, 1.0, 0.0),
                24, 0.5, 0.3);
    }

    // Every move but the thunderclap is his own game's to make (it moves him or picks the blow); the server checks it
    // may, shows it and hits what it hits.
    @Override
    public boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data) {
        boolean flying = ThorMoves.flying(player);
        boolean held = (data & Characters.HOLD) != 0;
        return switch (ability.id()) {
            case "combo" -> on && !flying && (data & Characters.TAP) != 0
                    && ThorBlows.start(player, data >> Characters.MOVE_SHIFT & 0xFF, ability.getDamage());
            case "thunderclap" -> {
                if ((data & Characters.CHARGE) != 0) {
                    ThorMoves.charging(player, on && !flying);
                    yield false;
                }
                yield on && held && !flying && Thunderclap.cast(player, player.serverLevel(), ability.getDamage());
            }
            case "dash" -> on && !flying && ThorMoves.dash(player, data >> Characters.MOVE_SHIFT & 0xFF,
                    data >> Characters.MOVE_SHIFT + 8 & 0xFF);
            case "super_jump" -> on && !flying && ThorMoves.superJump(player);
            case "flight" -> {
                if (on && (data & Characters.SLAM) != 0) {
                    ThorMoves.land(player, true);
                    yield false;
                }
                yield on && held && ThorMoves.takeOff(player);
            }
            case "air_blink" -> on && flying && ThorMoves.blink(player);
            case "grab_dash_dive" -> {
                ThorMoves moves = ThorMoves.find(player);
                if (on && (data & Characters.SLAM) != 0) {
                    if (moves != null && moves.dive != null) {
                        moves.dive.slam(player.serverLevel());
                    }
                    yield false;
                }
                yield on && held && flying && GrabDive.start(player, ability.getDamage());
            }
            case "lightning_flight" -> ThorMoves.lightning(player, on && flying);
            default -> false;
        };
    }

    @Override
    public void showTo(ServerPlayer viewer, ServerPlayer target) {
        int flags = ThorMoves.flags(target);
        if (flags != 0) {
            ThorStatePayload.sendTo(viewer, target, flags);
        }
    }

    @Override
    public void clear() {
        ThorMoves.clear();
        ThorBlows.clear();
    }
}
