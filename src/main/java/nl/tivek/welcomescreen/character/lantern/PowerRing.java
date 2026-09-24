package nl.tivek.welcomescreen.character.lantern;

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
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.Characters;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.network.RingPayload;

/**
 * Green Lantern's power ring. Everything it makes is hard light: green energy shaped by willpower, that
 * only lasts while the one who made it keeps it going. Stop being Green Lantern (die, log out, change
 * dimension, pick someone else) and every construct of yours falls apart.
 *
 * <p>The ring holds {@link #MAX_POWER} power, and every construct costs some of it. Once it runs low it
 * has to be recharged at the lantern (see {@link Lantern}). What is left stays on the player: also while
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
    static final int GREEN = 0x3CE86A;
    /** The bright middle of that light. */
    static final int BRIGHT = 0xB8FFC8;
    /** Pale green: still clearly green, only lighter. For sparks that should not read as white. */
    static final int PALE = 0x8CFF9E;

    // Kept in the part of a player's saved data that the game carries over when they respawn.
    private static final String POWER_KEY = WelcomeScreenMod.MODID + ":ring_power";

    private static int nextId;

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
            case "recharge" -> Lantern.recharge(player, level, ability);
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
            // The ring fist thrown up high: the lantern takes shape over it and bursts out blinding.
            case "light_flare" -> LightFlare.use(player, level, ability);
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
        GiantFist.clear();
        Lantern.clear();
        LightBolt.clear();
        LightBeam.clear();
        LightShield.clear();
        LightDome.clear();
        Flight.clear();
        Shockwave.clear();
        LandingSlam.clear();
        Arrival.clear();
        Fear.clear();
        LightFlare.clear();
        AirStrike.clear();
        LightBubble.clear();
        SwordShield.clear();
    }

    /** True while this player can keep a construct going in this level: alive, here, and still Green Lantern. */
    static boolean fuels(ServerPlayer player, ServerLevel level) {
        return !player.isRemoved() && player.isAlive() && player.level() == level
                && Characters.of(player) == GameCharacter.GREEN_LANTERN;
    }

    /**
     * Whether the ring's light may hurt this creature: anything alive except its owner, armour stands and those
     * who are only watching, and other players only where players may fight each other. Bosses too: hard light
     * is a weapon, not a grip.
     */
    static boolean canHit(ServerPlayer owner, Entity entity) {
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
    static int newId() {
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

    /** Puts this much in the ring (never below empty or above full), and shows it. */
    static void setPower(ServerPlayer player, float power) {
        saved(player).putFloat(POWER_KEY, Mth.clamp(power, 0.0F, MAX_POWER));
        sync(player);
    }

    private static CompoundTag saved(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    // ---- Telling the clients ----

    /** Tells this player and everyone who can see them how full their ring is and what they do with it. */
    public static void sync(ServerPlayer player) {
        if (!player.hasDisconnected()) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, state(player));
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
        return new RingPayload(player.getId(), power(player), GiantFist.pending(player), Lantern.ticks(player),
                Flight.ticks(player), state, Arrival.ticks(player), Arrival.from(player), LightBeam.charging(player));
    }

    /** A line about the ring on the player's action bar. */
    static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("ring." + WelcomeScreenMod.MODID + "." + key), true);
    }
}
