package nl.tivek.multiversepowers.character.greenlantern;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.Fear;
import nl.tivek.multiversepowers.character.greenlantern.ability.Flight;
import nl.tivek.multiversepowers.character.greenlantern.ability.GiantFist;
import nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands;
import nl.tivek.multiversepowers.character.greenlantern.ability.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightBeam;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightBolt;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightDome;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightShield;
import nl.tivek.multiversepowers.character.greenlantern.ability.Recharge;
import nl.tivek.multiversepowers.character.greenlantern.ability.RingScan;
import nl.tivek.multiversepowers.character.greenlantern.ability.Shockwave;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordShield;
import nl.tivek.multiversepowers.engine.effect.Effects;

/**
 * Green Lantern's power ring. Everything it makes is hard light: green energy shaped by willpower, that
 * only lasts while the one who made it keeps it going. Stop being Green Lantern (die, log out, change
 * dimension, pick someone else) and every construct of yours falls apart.
 *
 * <p>The ring holds {@link #MAX_POWER} power, and every construct costs some of it. Once it runs low it
 * has to be recharged at the lantern (see {@link Recharge}). What is left stays on the player: also while
 * they are someone else, and through dying and logging out, so changing character never fills it for free.
 */
public final class PowerRing {
    /** A full ring. */
    public static final float MAX_POWER = 100.0F;
    /** How long recharging at the lantern takes in all, in ticks. Clients play the same timeline. */
    public static final int RECHARGE_TICKS = 36;
    /** The tick of a recharge on which the fist smacks the lantern and the ring drinks its light. */
    public static final int RECHARGE_HIT = 13;
    /** The tick on which the light dies down again and the fist comes off the lantern. */
    public static final int RECHARGE_BACK = 26;

    /** The green of the ring's light. */
    public static final int GREEN = 0x3CE86A;
    /** The bright middle of that light. */
    public static final int BRIGHT = 0xB8FFC8;
    /** Pale green: still clearly green, only lighter. For sparks that should not read as white. */
    public static final int PALE = 0x8CFF9E;

    // Kept in the part of a player's saved data that the game carries over when they respawn.
    private static final String POWER_KEY = MultiversePowers.MODID + ":ring_power";

    private static int nextId;
    // Rings whose power changed this tick, and rings whose onlookers have not heard their latest power yet: told at the
    // end of the tick, the onlookers once every few ticks (see sendChanged).
    private static final Set<ServerPlayer> CHANGED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<ServerPlayer> UNSEEN = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final int ONLOOKERS_EVERY = 5;
    private static int syncTicks;

    static {
        Effects.atTickEnd(PowerRing::sendChanged);
    }

    private PowerRing() {
    }

    /**
     * One of Green Lantern's abilities, whichever key slot it sits in. The character system looks up which
     * ability a key means; this only has to do it.
     *
     * @return true when the ability really ran, so its cooldown should start
     */
    public static boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data) {
        ServerLevel level = player.serverLevel();
        // The ring is still on its way to him, or dressing him: it does nothing else until that is done. Letting go of
        // a key is always fine.
        if (Arrival.busy(player)) {
            if (on) {
                tell(player, "arriving");
            }
            return false;
        }
        return switch (ability.id()) {
            // Hold the key to charge the fist; it flies when you let go.
            case "giant_fist" -> on ? GiantFist.launch(player, level, ability) : GiantFist.letGo(player);
            // Raise the lantern and smack the ring into it.
            case "recharge" -> Recharge.recharge(player, level, ability);
            // What the mouse always does. Tap: a bolt, or the shield up or away. Hold: the beam, or the dome. With the
            // sword and shield of the construct wheel in his hands the mouse is theirs instead.
            case "light_bolt" -> SwordShield.equipped(player) ? SwordShield.attack(player, level, on, data)
                    : LightBolt.use(player, level, ability, on, data);
            case "light_shield" -> SwordShield.equipped(player) ? SwordShield.defend(player, level, on, data)
                    : LightShield.use(player, level, ability, on, data);
            // Smash the ring fist into the ground for a shockwave; in the air he goes down to the ground first.
            case "shockwave" -> Shockwave.use(player, level, ability);
            // A wave of the ring's light rolls out and marks every creature it passes.
            case "ring_scan" -> RingScan.use(player, level, ability);
            // He waves his ring hand this way and that: giant hands burst up out of the ground at his enemies.
            case "giant_hands" -> GiantHands.use(player, level, ability);
            // The ultimate: a big gunship of hard light drones over the battlefield, fires and crashes in a blast.
            case "air_strike" -> AirStrike.use(player, level, ability);
            // A bubble of hard light round a creature, lifted up; again to smash it down, crouching to let it go.
            case "light_bubble" -> LightBubble.use(player, level, ability, data);
            // Take off, or land again; flying into the ground at full speed lands with a slam.
            case "flight" -> on && ((data & Characters.SLAM) != 0 ? Flight.slam(player, level, ability)
                    : Flight.toggle(player, level, ability));
            default -> false;
        };
    }

    /** The server stops: forget every construct that was still held, every recharge, flight and slam. */
    public static void clear() {
        CHANGED.clear();
        UNSEEN.clear();
        GiantFist.clear();
        Recharge.clear();
        LightBolt.clear();
        LightBeam.clear();
        LightShield.clear();
        LightDome.clear();
        Flight.clear();
        Shockwave.clear();
        LandingSlam.clear();
        Arrival.clear();
        Fear.clear();
        GiantHands.clear();
        AirStrike.clear();
        LightBubble.clear();
        SwordShield.clear();
    }

    /** True while this player can keep a construct going in this level: alive, here, and still Green Lantern. */
    public static boolean fuels(ServerPlayer player, ServerLevel level) {
        return !player.isRemoved() && player.isAlive() && player.level() == level
                && Characters.of(player) == GameCharacter.GREEN_LANTERN;
    }

    /**
     * Whether the ring's light may hurt this creature: anything alive except its owner, armour stands and those
     * who are only watching, and other players only where players may fight each other. Bosses too: hard light
     * is a weapon, not a grip.
     */
    public static boolean canHit(ServerPlayer owner, Entity entity) {
        if (entity == owner || !(entity instanceof LivingEntity living) || !living.isAlive() || entity.isSpectator()
                || entity instanceof ArmorStand) {
            return false;
        }
        if (entity instanceof Player other) {
            return owner.server.isPvpAllowed() && !other.isCreative() && owner.canHarmPlayer(other);
        }
        return true;
    }

    /** A fresh id for one construct; send it every tick, and remove it when done. */
    public static int newId() {
        nextId++;
        return nextId;
    }

    // ---- Power ----

    /** What this player's ring holds, 0 up to {@link #MAX_POWER}. A ring that was never used is full. */
    public static float power(ServerPlayer player) {
        CompoundTag saved = saved(player);
        return saved.contains(POWER_KEY, Tag.TAG_FLOAT) ? Mth.clamp(saved.getFloat(POWER_KEY), 0.0F, MAX_POWER)
                : MAX_POWER;
    }

    /**
     * Puts this much in the ring (never below empty or above full), and shows it: its owner at the end of this tick,
     * everyone who sees him a moment later (see {@link #sendChanged}).
     */
    public static void setPower(ServerPlayer player, float power) {
        saved(player).putFloat(POWER_KEY, Mth.clamp(power, 0.0F, MAX_POWER));
        CHANGED.add(player);
        UNSEEN.add(player);
    }

    private static CompoundTag saved(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    // ---- Telling the clients ----

    /** Tells this player and everyone who can see them how full their ring is and what they do with it, right now. */
    public static void sync(ServerPlayer player) {
        CHANGED.remove(player);
        UNSEEN.remove(player);
        if (!player.hasDisconnected()) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, state(player));
        }
    }

    /**
     * The end of a tick: the owner of every ring whose power changed hears of it once, however often it changed (a
     * flight, a shield and a scrape all drain it on the same tick), and everyone who sees him every few ticks: his
     * glow does not need more. Whatever else the ring does is told the moment it happens (see {@link #sync}).
     */
    private static void sendChanged() {
        if (CHANGED.isEmpty() && UNSEEN.isEmpty()) {
            return;
        }
        boolean onlookers = ++syncTicks % ONLOOKERS_EVERY == 0;
        for (ServerPlayer player : CHANGED) {
            if (!player.hasDisconnected() && !(onlookers && UNSEEN.contains(player))) {
                PacketDistributor.sendToPlayer(player, state(player));
            }
        }
        CHANGED.clear();
        if (onlookers) {
            for (ServerPlayer player : UNSEEN) {
                if (!player.hasDisconnected()) {
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, state(player));
                }
            }
            UNSEEN.clear();
        }
    }

    /** Someone comes into view of this Green Lantern: they learn how brightly his ring glows. */
    public static void showTo(ServerPlayer viewer, ServerPlayer owner) {
        PacketDistributor.sendToPlayer(viewer, state(owner));
    }

    private static RingPayload state(ServerPlayer player) {
        int state = (LightShield.up(player) ? RingPayload.SHIELD : 0) | (LightDome.up(player) ? RingPayload.DOME : 0)
                | (LightBeam.firing(player) ? RingPayload.BEAM : 0)
                | (Flight.descending(player) ? RingPayload.DESCENT : 0)
                | (Flight.diving(player) || Shockwave.dropping(player) ? RingPayload.DIVE : 0);
        return new RingPayload(player.getId(), power(player), GiantFist.pending(player), Recharge.ticks(player),
                Flight.ticks(player), state, Arrival.ticks(player), Arrival.from(player), LightBeam.charging(player));
    }

    /** A line about the ring on the player's action bar. */
    public static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("ring." + MultiversePowers.MODID + "." + key), true);
    }
}
