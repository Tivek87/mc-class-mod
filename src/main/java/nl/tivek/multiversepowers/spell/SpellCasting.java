package nl.tivek.multiversepowers.spell;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.ability.Cooldowns;

/**
 * Server side of the spells: checks the cooldown and casts the spell (what each one does is its own, see
 * {@link Spell#cast}). What a spell keeps going runs as an effect (see
 * {@link nl.tivek.multiversepowers.engine.effect.Effects}). Spells never break blocks; only the fireball and the
 * lightning strike can leave fire behind.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class SpellCasting {
    // Per player: when each spell is ready again. A spell has one cooldown.
    private static final Cooldowns<Spell> COOLDOWNS = new Cooldowns<>(1);

    private SpellCasting() {
    }

    public static void tryCast(ServerPlayer player, Spell spell) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        int left = COOLDOWNS.left(player, spell, 0);
        if (left > 0) {
            // The client thought it was ready; send the real time back.
            syncCooldown(player, spell, left);
            return;
        }
        if (spell.cast(player, player.serverLevel())) {
            COOLDOWNS.start(player, spell, 0, spell.getCooldown());
            syncCooldown(player, spell, spell.getCooldown());
        }
    }

    private static void syncCooldown(ServerPlayer player, Spell spell, int ticks) {
        PacketDistributor.sendToPlayer(player, new SpellCooldownPayload(spell.getId(), ticks));
    }

    /** The server stops: every spell ready again, and nobody walking in the void any more. */
    public static void clear() {
        COOLDOWNS.clear();
        VoidWalkSpell.clear();
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
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VoidWalkSpell.leave(player);
        }
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String key = "spell." + MultiversePowers.MODID + ".welcome_";
            player.sendSystemMessage(Component.translatable(key + "tag").withStyle(ChatFormatting.AQUA).append(" ")
                    .append(Component.translatable(key + "tip").withStyle(ChatFormatting.YELLOW)));
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VoidWalkSpell.leave(player);
        }
    }
}
