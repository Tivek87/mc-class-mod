package nl.tivek.multiversepowers.character.thor;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.CharacterPowers;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.BodySize;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// Thor: a bolt of lightning strikes as you become him and static crawls over you for a while. His moves are in
// ThorMoves (dash, super jump, flight and what he does in flight), ThorBlows (his combo), ThorGrab, GrabDive,
// Thunderclap, Mjolnir (the hammer in hand, thrown, its uppercut), ThorCharge and SkyMoves (the shockwave and bolt
// in flight).
public final class ThorPowers implements CharacterPowers {
    // What his game reports of him for the gestures that need it: hammer in hand or not, running or not.
    public static final int UNARMED = 1;
    public static final int ARMED = 2;
    public static final int WALKING = 4;
    public static final int SPRINTING = 8;
    // How long the attack button is held to wind up the thunderclap or the hammer's uppercut: 0.75 seconds.
    public static final int CLAP_HOLD = 15;
    // How long space is held to fly, right held for the dive, a grab, a grab dash (running), the hammer's throw to
    // fly after, the scroll wheel for a charge, left in flight for the shockwave and shift for lightning speed.
    public static final int FLIGHT_HOLD = 8;
    public static final int DIVE_HOLD = 10;
    public static final int GRAB_HOLD = 15;
    public static final int GRAB_DASH_HOLD = 10;
    public static final int LEAP_HOLD = 20;
    public static final int CHARGE_HOLD = 40;
    public static final int SHOCK_HOLD = 20;
    public static final int LIGHTNING_HOLD = 40;
    private static final int CRACKLE_TICKS = 40;
    private static final int GLOW = 0x9FE8FF;
    // He stands 30% bigger than a player: his bones, hitbox, eyes, step and reach, and his moves with them.
    private static final double SIZE = 1.3;
    private static final ResourceLocation GROWN = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "thor_size");

    @Override
    public void enter(ServerPlayer player) {
        BodySize.set(player, GROWN, SIZE);
        ServerLevel level = player.serverLevel();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(player.getX(), player.getY(), player.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        Vec3 chest = player.position().add(0.0, SIZE, 0.0);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, chest, 60, 0.6 * SIZE, 0.5);
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
            double size = thor.getScale();
            Vec3 body = thor.position().add(ParticleFx.spread(0.7 * size),
                    (0.2 + ParticleFx.RANDOM.nextDouble() * 1.7) * size, ParticleFx.spread(0.7 * size));
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
        ThorGrab.leave(player);
        Mjolnir.leave(player);
        ThorCharge.leave(player);
        ParticleFx.cloud(player.serverLevel(), ParticleTypes.ELECTRIC_SPARK,
                player.position().add(0.0, player.getScale(), 0.0), 24, 0.5 * player.getScale(), 0.3);
        BodySize.reset(player, GROWN);
    }

    // His moves are his own game's to make where they move him or pick the blow; the server checks he may, shows it
    // and hits what it hits.
    @Override
    public boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data) {
        boolean flying = ThorMoves.flying(player);
        boolean held = (data & Characters.HOLD) != 0;
        boolean armed = Mjolnir.inHand(player);
        int move = data >> Characters.MOVE_SHIFT & 0xFF;
        int arg = data >> Characters.MOVE_SHIFT + 8 & 0xFF;
        float fists = ability.getDamage() * ThorCharge.fists(player);
        float hammer = ability.getDamage() * ThorCharge.hammer(player);
        return switch (ability.id()) {
            case "combo" -> on && (data & Characters.TAP) != 0
                    && ThorBlows.start(player, move, ability.getDamage());
            case "thunderclap" -> {
                if ((data & Characters.CHARGE) != 0) {
                    ThorMoves.charging(player, on && !flying && !armed);
                    yield false;
                }
                yield on && held && !flying && !armed && Thunderclap.cast(player, player.serverLevel(), fists);
            }
            case "hammer_uppercut" -> on && held && !flying && Mjolnir.uppercut(player, hammer);
            case "dash" -> on && !flying && !armed && ThorMoves.dash(player, move, arg);
            // SLAM: his pick of how the grab under way ends.
            case "grab" -> (data & Characters.SLAM) != 0 ? on && ThorGrab.pick(player, move)
                    : on && held && !flying && !armed && ThorGrab.grab(player, fists);
            case "grab_dash" -> on && held && !flying && !armed && ThorGrab.dash(player, move, arg, fists);
            case "hammer_throw" -> on && !flying && Mjolnir.fling(player, hammer, false);
            case "hammer_leap" -> on && held && !flying && Mjolnir.fling(player, hammer, true);
            case "mjolnir" -> on && !flying && Mjolnir.toggle(player);
            case "charged" -> on && held && !flying && ThorCharge.charge(player, ability.value("seconds"));
            case "air_shockwave" -> on && held && flying && SkyMoves.shockwave(player, fists);
            case "air_bolt" -> on && flying && SkyMoves.bolt(player, fists);
            case "super_jump" -> on && !flying && ThorMoves.superJump(player);
            case "flight" -> {
                if (on && (data & Characters.SLAM) != 0) {
                    ThorMoves.land(player, true);
                    yield false;
                }
                yield on && held && ThorMoves.takeOff(player);
            }
            case "air_blink" -> on && flying && ThorMoves.blink(player, move, arg);
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
            case "lightning_flight" -> on && held && flying
                    && ThorMoves.lightning(player, (int) Math.round(ability.value("seconds") * 20.0),
                            ability.getDamage(), (float) ability.value("landingDamage"));
            default -> false;
        };
    }

    @Override
    public void knockedDown(ServerPlayer player) {
        ThorMoves.land(player, false);
        ThorGrab.leave(player);
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
        ThorGrab.clear();
        Mjolnir.clear();
        ThorCharge.clear();
    }
}
