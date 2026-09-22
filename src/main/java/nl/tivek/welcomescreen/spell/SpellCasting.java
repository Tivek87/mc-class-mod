package nl.tivek.welcomescreen.spell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.Characters;
import nl.tivek.welcomescreen.character.docock.OctopusArms;
import nl.tivek.welcomescreen.character.lantern.PowerRing;
import nl.tivek.welcomescreen.network.SpellCooldownPayload;

/**
 * Server side of the spells: checks the cooldown, starts the spell, and ticks
 * every running spell
 * effect. Spells never break blocks; only the fireball and the lightning strike
 * can leave fire behind.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID)
public final class SpellCasting {
    // Per player: the server tick on which each spell is ready again, in Spell
    // order.
    private static final Map<UUID, int[]> COOLDOWNS = new HashMap<>();
    private static final List<Running> ACTIVE = new ArrayList<>();

    private SpellCasting() {
    }

    private static final class Running {
        private final ResourceKey<Level> dimension;
        private final SpellEffect effect;
        private int age;

        private Running(ResourceKey<Level> dimension, SpellEffect effect) {
            this.dimension = dimension;
            this.effect = effect;
        }
    }

    /** Starts an effect; its first tick runs on the next server tick. */
    public static void start(ServerLevel level, SpellEffect effect) {
        ACTIVE.add(new Running(level.dimension(), effect));
    }

    public static void tryCast(ServerPlayer player, Spell spell) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        int now = player.server.getTickCount();
        int[] readyAt = COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new int[Spell.values().length]);
        int left = readyAt[spell.ordinal()] - now;
        if (left > 0) {
            // The client thought it was ready; send the real time back.
            syncCooldown(player, spell, left);
            return;
        }

        ServerLevel level = player.serverLevel();
        boolean cast = switch (spell) {
            case WIND_GUST -> WindGustSpell.cast(player, level);
            case FIREBALL -> FireballSpell.cast(player, level);
            case VOID_WALK -> VoidWalkSpell.cast(player, level);
            case LIGHTNING_STRIKE -> LightningSpell.cast(player, level);
            case POISON_AREA -> PoisonSpell.cast(player, level);
        };
        if (cast) {
            readyAt[spell.ordinal()] = now + spell.getCooldown();
            syncCooldown(player, spell, spell.getCooldown());
        }
    }

    /** Left click while an Octopus Arm holds a creature. */
    public static void throwHeld(ServerPlayer player) {
        OctopusArms.throwHeld(player);
    }

    public static void resetAllCooldowns(ServerPlayer player) {
        int[] readyAt = COOLDOWNS.get(player.getUUID());
        if (readyAt != null) {
            java.util.Arrays.fill(readyAt, 0);
        }
        for (Spell s : Spell.values()) {
            syncCooldown(player, s, 0);
        }
    }

    private static void syncCooldown(ServerPlayer player, Spell spell, int ticks) {
        PacketDistributor.sendToPlayer(player, new SpellCooldownPayload(spell.getId(), ticks));
    }

    // Your own lightning never hits you, even when you strike right next to
    // yourself.
    @SubscribeEvent
    public static void onStruckByLightning(EntityStruckByLightningEvent event) {
        ServerPlayer cause = event.getLightning().getCause();
        if (cause != null && event.getEntity() == cause) {
            event.setCanceled(true);
        }
    }

    // Nothing can target a player who walks in the void.
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (VoidWalkSpell.isInVoid(event.getNewAboutToBeSetTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            HeldMobs.restoreSaved(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VoidWalkSpell.leave(player);
        }
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§b[Magic] §eFeel free to send me whatever you want! I can make simple spells within 3min. Think of something and let me know, and I'll try to make it ASAP!"));
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VoidWalkSpell.leave(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        // Tick a copy: an effect may start new effects (the poison vial turns into the
        // cloud).
        List<Running> current = new ArrayList<>(ACTIVE);
        for (Running running : current) {
            ServerLevel level = event.getServer().getLevel(running.dimension);
            if (level == null || !running.effect.tick(level, running.age)) {
                ACTIVE.remove(running);
            }
            running.age++;
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        COOLDOWNS.clear();
        ACTIVE.clear();
        VoidWalkSpell.clear();
        OctopusArms.clear();
        PowerRing.clear();
        Characters.clear();
        // Before the world is saved: held mobs must not be stored with their AI
        // switched off.
        HeldMobs.releaseAll();
    }
}
