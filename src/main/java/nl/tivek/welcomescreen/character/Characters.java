package nl.tivek.welcomescreen.character;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.docock.OctopusArms;
import nl.tivek.welcomescreen.character.lantern.Arrival;
import nl.tivek.welcomescreen.character.lantern.AirStrike;
import nl.tivek.welcomescreen.character.lantern.PowerRing;
import nl.tivek.welcomescreen.network.CharacterLookPayload;
import nl.tivek.welcomescreen.network.CharacterStatePayload;

/**
 * Who every player is right now, and their cooldowns. The wheel sends which character a player wants
 * to be; an ability key sends only its slot. This class looks up what that slot means for that
 * character and asks the right code to do it, so no key is ever tied to one character.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID)
public final class Characters {
    /** Bit in a key press's {@code data}: the player was crouching while they pressed it. */
    public static final int SNEAKING = 1;
    /** Bit in a mouse ability's {@code data}: the button was tapped, so its quick version goes off. */
    public static final int TAP = 2;
    /**
     * Bit in a mouse ability's {@code data}: the button has been held down long enough, so its hold version
     * starts. It lasts until the button comes up again (a press with {@code on} false).
     */
    public static final int HOLD = 4;
    /**
     * Bit in the flight key's {@code data}, sent by your own game rather than by the key: you flew into the ground at
     * full speed, so you land with a slam instead of simply landing.
     */
    public static final int SLAM = 8;
    /**
     * Bit in the flight key's {@code data}, sent by your own game rather than by the key: you have reached top speed
     * ({@code on} true) or dropped below it again ({@code on} false). Everyone sees the jets that come with it.
     */
    public static final int BOOST = 16;
    /**
     * Bit in a mouse ability's {@code data} with the button of the hand that defends coming up: it did not come up, the
     * charge of the shield ran into a wall and stopped by itself.
     */
    public static final int WALL = 32;
    /**
     * Where in a mouse ability's {@code data} the move of the sword or shield his client picked for a tap sits (see
     * {@link nl.tivek.welcomescreen.character.lantern.SwordMove}): {@code data >> MOVE_SHIFT}.
     */
    public static final int MOVE_SHIFT = 8;

    private static final Map<UUID, GameCharacter> ACTIVE = new HashMap<>();
    // Per player, per character: the server tick each slot is ready again on, in AbilitySlot order.
    private static final Map<UUID, Map<GameCharacter, int[]>> READY_AT = new HashMap<>();

    private Characters() {
    }

    /** Who this player is right now, or null when they are just themselves. */
    @Nullable
    public static GameCharacter of(ServerPlayer player) {
        return ACTIVE.get(player.getUUID());
    }

    /**
     * From the wheel: turn into {@code character}. Picking the character you already are changes you
     * back; {@code null} always changes you back.
     */
    public static void select(ServerPlayer player, @Nullable GameCharacter character) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        GameCharacter now = ACTIVE.get(player.getUUID());
        GameCharacter wanted = character == now ? null : character;
        if (now != null) {
            leave(player, now);
        }
        if (wanted != null) {
            ACTIVE.put(player.getUUID(), wanted);
            enter(player, wanted);
        } else {
            ACTIVE.remove(player.getUUID());
        }
        sync(player);
        showLook(player, true);
    }

    private static void enter(ServerPlayer player, GameCharacter character) {
        ServerLevel level = player.serverLevel();
        switch (character) {
            case DOC_OCK -> {
                transformEffect(player, level, character);
                OctopusArms.armsOut(player, level);
            }
            // The ring comes for him from afar, and dresses him in the uniform (see Arrival); it has light and sound of
            // its own.
            case GREEN_LANTERN -> Arrival.begin(player);
        }
        player.displayClientMessage(Component.translatable("character." + WelcomeScreenMod.MODID + ".became",
                character.getDisplayName()).withColor(character.getColor()), false);
    }

    private static void leave(ServerPlayer player, GameCharacter character) {
        switch (character) {
            case DOC_OCK -> OctopusArms.armsIn(player);
            case GREEN_LANTERN -> {
                // His constructs fall apart by themselves once he is no longer Green Lantern; so does a ring still on
                // its way to him.
                Arrival.end(player);
            }
        }
        if (!player.hasDisconnected()) {
            player.displayClientMessage(Component.translatable("character." + WelcomeScreenMod.MODID + ".normal"),
                    true);
        }
    }

    /** A short flash of the character's own colour, so everyone around sees the change. */
    private static void transformEffect(ServerPlayer player, ServerLevel level, GameCharacter character) {
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.8,
                0.4, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 1.0F, character == GameCharacter.GREEN_LANTERN ? 1.4F : 0.9F);
    }

    // ---- Ability keys ----

    /**
     * A key press from a client. {@code slot} is an AbilitySlot index; {@code on} says whether a held
     * key went down or up, and {@code data} carries anything extra the slot needs.
     */
    public static void action(ServerPlayer player, int slotIndex, boolean on, int data) {
        AbilitySlot slot = AbilitySlot.byIndex(slotIndex);
        GameCharacter character = ACTIVE.get(player.getUUID());
        if (slot == null || character == null) {
            if (slot != null && !on) {
                return;
            }
            player.displayClientMessage(Component.translatable("character." + WelcomeScreenMod.MODID + ".none"),
                    true);
            return;
        }
        CharacterAbility ability = character.ability(slot);
        if (ability == null) {
            if (on) {
                player.displayClientMessage(Component.translatable("character." + WelcomeScreenMod.MODID + ".empty",
                        character.getDisplayName(), slot.getDisplayName()), true);
            }
            return;
        }
        // Two things are never blocked by a cooldown: letting go of a key you hold down, and the
        // crouching version of an ability that says crouching is its undo (let go, put back down).
        boolean letGo = ability.isHeld() && !on;
        boolean undo = (data & SNEAKING) != 0 && ability.crouchDoes() == CharacterAbility.Crouch.UNDO;
        if (left(player, character, slot) > 0 && !letGo && !undo) {
            sync(player);
            return;
        }
        boolean used = switch (character) {
            case DOC_OCK -> OctopusArms.use(player, ability, on, data);
            case GREEN_LANTERN -> PowerRing.use(player, ability, on, data);
        };
        // The cooldown starts once the ability is done: for a key you hold down, when you let go of it,
        // so holding it never eats into the cooldown. An undo never starts one.
        boolean done = (!ability.isHeld() || letGo) && !undo;
        if (used && ability.getCooldown() > 0 && done) {
            READY_AT.computeIfAbsent(player.getUUID(), id -> new HashMap<>())
                    .computeIfAbsent(character, id -> new int[AbilitySlot.values().length])[slot
                            .ordinal()] = player.server.getTickCount() + ability.getCooldown();
        }
        sync(player);
    }

    /**
     * Starts the cooldown of this ability now, for an ability that decides by itself when it is done (a bubble that
     * holds its creature a while): its key said nothing was used, so none started on the press. Only for the character
     * the player still is.
     */
    public static void startCooldown(ServerPlayer player, CharacterAbility ability) {
        if (ability.getCooldown() <= 0 || ACTIVE.get(player.getUUID()) != ability.character()) {
            return;
        }
        READY_AT.computeIfAbsent(player.getUUID(), id -> new HashMap<>())
                .computeIfAbsent(ability.character(), id -> new int[AbilitySlot.values().length])[ability.slot()
                        .ordinal()] = player.server.getTickCount() + ability.getCooldown();
        sync(player);
    }

    private static int left(ServerPlayer player, GameCharacter character, AbilitySlot slot) {
        Map<GameCharacter, int[]> perCharacter = READY_AT.get(player.getUUID());
        int[] ready = perCharacter == null ? null : perCharacter.get(character);
        return ready == null ? 0 : Math.max(0, ready[slot.ordinal()] - player.server.getTickCount());
    }

    // ---- Telling the client ----

    /** Sends this player their character, their cooldowns and the state their client moves along with. */
    public static void sync(ServerPlayer player) {
        if (player.hasDisconnected()) {
            return;
        }
        GameCharacter character = ACTIVE.get(player.getUUID());
        int[] cooldowns = new int[AbilitySlot.values().length];
        if (character != null) {
            for (AbilitySlot slot : AbilitySlot.values()) {
                cooldowns[slot.ordinal()] = left(player, character, slot);
            }
        }
        boolean docOck = character == GameCharacter.DOC_OCK;
        PacketDistributor.sendToPlayer(player, new CharacterStatePayload(
                character == null ? -1 : character.ordinal(), cooldowns,
                docOck ? OctopusArms.ultimateLeft(player)
                        : character == GameCharacter.GREEN_LANTERN ? AirStrike.left(player) : 0,
                docOck ? OctopusArms.legCount(player) : 0,
                docOck ? OctopusArms.markCount(player) : 0));
    }

    /**
     * A character's own code says it has ended by itself (the tentacles are gone because the player
     * changed dimension, for instance). You are yourself again, unless you are already someone else.
     */
    public static void lost(ServerPlayer player, GameCharacter character) {
        if (ACTIVE.get(player.getUUID()) == character) {
            ACTIVE.remove(player.getUUID());
            if (!player.hasDisconnected()) {
                player.displayClientMessage(
                        Component.translatable("character." + WelcomeScreenMod.MODID + ".normal"), true);
            }
            sync(player);
            showLook(player, true);
        }
    }

    /** Tells everyone who can see this player (and the player) who they are now, so they are drawn that way. */
    private static void showLook(ServerPlayer player, boolean animate) {
        CharacterLookPayload look = look(player, animate);
        PacketDistributor.sendToPlayersTrackingEntity(player, look);
        if (!player.hasDisconnected()) {
            PacketDistributor.sendToPlayer(player, look);
        }
    }

    private static CharacterLookPayload look(ServerPlayer player, boolean animate) {
        GameCharacter character = ACTIVE.get(player.getUUID());
        return new CharacterLookPayload(player.getId(), character == null ? -1 : character.ordinal(), animate);
    }

    /** Everyone back to themselves (the server is stopping). */
    public static void clear() {
        ACTIVE.clear();
        READY_AT.clear();
    }

    // ---- Events ----

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            forget(player);
        }
    }

    /** Someone comes into view: their client learns who they are, without playing the change. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, look(target, false));
            if (of(target) == GameCharacter.GREEN_LANTERN) {
                PowerRing.showTo(viewer, target);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            forget(player);
        }
    }

    /** You are yourself again: dying or logging out always takes the character off. */
    private static void forget(ServerPlayer player) {
        GameCharacter character = ACTIVE.remove(player.getUUID());
        if (character != null) {
            leave(player, character);
            showLook(player, true);
        }
        READY_AT.remove(player.getUUID());
        sync(player);
    }
}
