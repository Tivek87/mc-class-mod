package nl.tivek.multiversepowers.spell;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.ability.Cooldowns;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.faction.Factions;

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class SpellCasting {
    private static final Cooldowns<Spell> COOLDOWNS = new Cooldowns<>(1);

    private SpellCasting() {
    }

    public static void tryCast(ServerPlayer player, Spell spell) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        int left = COOLDOWNS.left(player, spell, 0);
        if (left > 0) {
            // Client thought it was ready; correct it with the real time left.
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

    public static void clear(MinecraftServer server) {
        COOLDOWNS.clear();
        VoidWalkSpell.clear(server);
    }

    @SubscribeEvent
    public static void onStruckByLightning(EntityStruckByLightningEvent event) {
        // Lightning has no attacker for vanilla's PvP checks, so this covers it by hand.
        ServerPlayer cause = event.getLightning().getCause();
        if (cause == null) {
            return;
        }
        Entity struck = event.getEntity();
        if (struck.getUUID().equals(cause.getUUID()) || Factions.friendly(cause, struck)
                || struck instanceof Player player && !Targeting.isTargetable(cause, player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (VoidWalkSpell.isInVoid(event.getNewAboutToBeSetTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        VoidWalkSpell.ambush(event);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer player && event.getEntity() instanceof ServerPlayer viewer) {
            VoidWalkSpell.seenBy(player, viewer);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VoidWalkSpell.equipmentChanged(player);
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
            for (Spell spell : Spell.values()) {
                int left = COOLDOWNS.left(player, spell, 0);
                if (left > 0) {
                    syncCooldown(player, spell, left);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VoidWalkSpell.leave(player);
        }
    }
}
