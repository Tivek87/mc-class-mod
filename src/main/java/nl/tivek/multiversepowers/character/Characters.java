package nl.tivek.multiversepowers.character;

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
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.engine.ability.Cooldowns;
import nl.tivek.multiversepowers.engine.ability.Throttle;
import nl.tivek.multiversepowers.engine.entity.PlayerKnockdowns;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Characters {
    public static final int SNEAKING = 1;
    public static final int TAP = 2;
    public static final int HOLD = 4;
    public static final int SLAM = 8;
    // A hold being wound up or let go before it is done: only shown to the others, never refused by a cooldown.
    public static final int CHARGE = 16;
    public static final int WALL = 32;
    public static final int MOVE_SHIFT = 8;

    private static final Map<UUID, GameCharacter> ACTIVE = new HashMap<>();
    private static final Cooldowns<GameCharacter> COOLDOWNS = new Cooldowns<>(AbilitySlot.values().length);
    private static final Throttle PICKS = new Throttle(10);

    private Characters() {
    }

    @Nullable
    public static GameCharacter of(ServerPlayer player) {
        return ACTIVE.get(player.getUUID());
    }

    public static void pick(ServerPlayer player, @Nullable GameCharacter character) {
        if (PICKS.allow(player)) {
            select(player, character);
        } else {
            sync(player);
        }
    }

    public static void select(ServerPlayer player, @Nullable GameCharacter character) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        GameCharacter now = ACTIVE.get(player.getUUID());
        GameCharacter wanted = character == now ? null : character;
        if (wanted != null && !PowerRules.character(wanted.getId())) {
            player.displayClientMessage(Component.translatable("character." + MultiversePowers.MODID + ".switched_off",
                    wanted.getDisplayName()), true);
            return;
        }
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
        character.powers().enter(player);
        player.displayClientMessage(Component.translatable("character." + MultiversePowers.MODID + ".became",
                character.getDisplayName()).withColor(character.getColor()), false);
    }

    private static void leave(ServerPlayer player, GameCharacter character) {
        character.powers().leave(player);
        if (!player.hasDisconnected()) {
            player.displayClientMessage(Component.translatable("character." + MultiversePowers.MODID + ".normal"),
                    true);
        }
    }

    public static void transformFlash(ServerPlayer player, float pitch) {
        ServerLevel level = player.serverLevel();
        ParticleFx.sendNear(level, ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4,
                0.8, 0.4, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 1.0F, pitch);
    }

    public static void action(ServerPlayer player, int slotIndex, boolean on, int data) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        AbilitySlot slot = AbilitySlot.byIndex(slotIndex);
        GameCharacter character = ACTIVE.get(player.getUUID());
        if (slot == null || character == null) {
            if (slot != null && !on) {
                return;
            }
            player.displayClientMessage(Component.translatable("character." + MultiversePowers.MODID + ".none"),
                    true);
            return;
        }
        CharacterAbility ability = character.ability(slot);
        if (ability == null || ability.isPlaceholder()) {
            return;
        }
        // Knocked down, a player can only let go of what they held.
        if (on && PlayerKnockdowns.isDown(player)) {
            sync(player);
            return;
        }
        boolean letGo = ability.isHeld() && !on;
        boolean undo = (data & SNEAKING) != 0 && ability.crouchDoes() == CharacterAbility.Crouch.UNDO;
        // A key's hold version keeps a cooldown of its own; the slot's belongs to the tap.
        boolean ownCooldown = !ability.isHeld() && ability.holdTicks() > 0
                && ability.tapWhen() != CharacterAbility.Tap.NEVER && (data & HOLD) != 0;
        // A move's landing (SLAM) belongs to the move already under way: its cooldown never refuses it.
        boolean follow = (data & (SLAM | CHARGE)) != 0;
        if (COOLDOWNS.left(player, character, slot.ordinal()) > 0 && !letGo && !undo && !ownCooldown && !follow) {
            sync(player);
            return;
        }
        boolean used = character.powers().use(player, ability, on, data);
        boolean done = (!ability.isHeld() || letGo) && !undo && !ownCooldown;
        if (used && ability.getCooldown() > 0 && done) {
            COOLDOWNS.start(player, character, slot.ordinal(), ability.getCooldown());
        }
        sync(player);
    }

    public static void knockedDown(ServerPlayer player) {
        GameCharacter character = ACTIVE.get(player.getUUID());
        if (character != null) {
            character.powers().knockedDown(player);
        }
    }

    public static int cooldownLeft(ServerPlayer player, CharacterAbility ability) {
        return COOLDOWNS.left(player, ability.character(), ability.slot().ordinal());
    }

    public static void startCooldown(ServerPlayer player, CharacterAbility ability) {
        if (ability.getCooldown() <= 0 || ACTIVE.get(player.getUUID()) != ability.character()) {
            return;
        }
        COOLDOWNS.start(player, ability.character(), ability.slot().ordinal(), ability.getCooldown());
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        if (player.hasDisconnected()) {
            return;
        }
        GameCharacter character = ACTIVE.get(player.getUUID());
        int[] cooldowns = new int[AbilitySlot.values().length];
        if (character == null) {
            PacketDistributor.sendToPlayer(player, new CharacterStatePayload(-1, cooldowns, 0, 0, 0, -1));
            return;
        }
        CharacterPowers powers = character.powers();
        for (AbilitySlot slot : AbilitySlot.values()) {
            CharacterAbility ability = character.ability(slot);
            cooldowns[slot.ordinal()] = Math.max(COOLDOWNS.left(player, character, slot.ordinal()),
                    ability == null ? 0 : powers.waitLeft(player, ability));
        }
        PacketDistributor.sendToPlayer(player, new CharacterStatePayload(character.ordinal(), cooldowns,
                powers.ultimateLeft(player), powers.stance(player), powers.marks(player), powers.limbs(player)));
    }

    public static void lost(ServerPlayer player, GameCharacter character) {
        if (ACTIVE.get(player.getUUID()) == character) {
            ACTIVE.remove(player.getUUID());
            if (!player.hasDisconnected()) {
                player.displayClientMessage(
                        Component.translatable("character." + MultiversePowers.MODID + ".normal"), true);
            }
            sync(player);
            showLook(player, true);
        }
    }

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

    public static void clear() {
        for (GameCharacter character : GameCharacter.values()) {
            character.powers().clear();
        }
        ACTIVE.clear();
        COOLDOWNS.clear();
        PICKS.clear();
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            forget(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, look(target, false));
            GameCharacter character = of(target);
            if (character != null) {
                character.powers().showTo(viewer, target);
            }
        }
    }

    // A character switched off in the world settings is taken away from whoever is it.
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            GameCharacter now = ACTIVE.get(player.getUUID());
            if (now != null && !PowerRules.character(now.getId())) {
                select(player, null);
                player.displayClientMessage(Component.translatable("character." + MultiversePowers.MODID
                        + ".switched_off", now.getDisplayName()), true);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            forget(player);
        }
    }

    private static void forget(ServerPlayer player) {
        GameCharacter character = ACTIVE.remove(player.getUUID());
        if (character != null) {
            leave(player, character);
            showLook(player, true);
        }
        COOLDOWNS.forgetReady(player);
        sync(player);
    }

    @SubscribeEvent
    public static void onGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getNewGameMode() == GameType.SPECTATOR && event.getEntity() instanceof ServerPlayer player) {
            forget(player);
        }
    }
}
