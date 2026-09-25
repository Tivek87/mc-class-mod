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

public final class PowerRing {
    public static final float MAX_POWER = 100.0F;
    public static final int RECHARGE_TICKS = 36;
    public static final int RECHARGE_HIT = 13;
    public static final int RECHARGE_BACK = 26;

    public static final int GREEN = 0x3CE86A;
    public static final int BRIGHT = 0xB8FFC8;
    public static final int PALE = 0x8CFF9E;

    // Kept in the part of a player's saved data that the game carries over when they respawn.
    private static final String POWER_KEY = MultiversePowers.MODID + ":ring_power";

    private static int nextId;
    // Rings whose power changed this tick, and rings whose onlookers have not heard their latest power yet: told
    // at the end of the tick, the onlookers once every few ticks (see sendChanged).
    private static final Set<ServerPlayer> CHANGED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<ServerPlayer> UNSEEN = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final int ONLOOKERS_EVERY = 5;
    private static int syncTicks;

    static {
        Effects.atTickEnd(PowerRing::sendChanged);
    }

    private PowerRing() {
    }

    public static boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data) {
        ServerLevel level = player.serverLevel();
        if (Arrival.busy(player)) {
            if (on) {
                tell(player, "arriving");
            }
            return false;
        }
        return switch (ability.id()) {
            case "giant_fist" -> on ? GiantFist.launch(player, level, ability) : GiantFist.letGo(player);
            case "recharge" -> Recharge.recharge(player, level, ability);
            case "light_bolt" -> SwordShield.equipped(player) ? SwordShield.attack(player, level, on, data)
                    : LightBolt.use(player, level, ability, on, data);
            case "light_shield" -> SwordShield.equipped(player) ? SwordShield.defend(player, level, on, data)
                    : LightShield.use(player, level, ability, on, data);
            case "shockwave" -> Shockwave.use(player, level, ability);
            case "ring_scan" -> RingScan.use(player, level, ability);
            case "giant_hands" -> GiantHands.use(player, level, ability);
            case "air_strike" -> AirStrike.use(player, level, ability);
            case "light_bubble" -> LightBubble.use(player, level, ability, data);
            case "flight" -> on && ((data & Characters.SLAM) != 0 ? Flight.slam(player, level, ability)
                    : Flight.toggle(player, level, ability));
            default -> false;
        };
    }

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

    public static boolean fuels(ServerPlayer player, ServerLevel level) {
        return !player.isRemoved() && player.isAlive() && player.level() == level
                && Characters.of(player) == GameCharacter.GREEN_LANTERN;
    }

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

    public static int newId() {
        nextId++;
        return nextId;
    }

    public static float power(ServerPlayer player) {
        CompoundTag saved = saved(player);
        return saved.contains(POWER_KEY, Tag.TAG_FLOAT) ? Mth.clamp(saved.getFloat(POWER_KEY), 0.0F, MAX_POWER)
                : MAX_POWER;
    }

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

    public static void sync(ServerPlayer player) {
        CHANGED.remove(player);
        UNSEEN.remove(player);
        if (!player.hasDisconnected()) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, state(player));
        }
    }

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

    public static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("ring." + MultiversePowers.MODID + "." + key), true);
    }
}
