package nl.tivek.multiversepowers.spell.client;

import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.client.AbilityKeys;
import nl.tivek.multiversepowers.character.client.PowerWheelScreen;
import nl.tivek.multiversepowers.spell.Spell;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientSpellCooldowns {
    private static final int[] REMAINING = new int[Spell.values().length];

    private ClientSpellCooldowns() {
    }

    public static void set(Spell spell, int ticks) {
        REMAINING[spell.ordinal()] = ticks;
    }

    public static int remaining(Spell spell) {
        return REMAINING[spell.ordinal()];
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        for (int i = 0; i < REMAINING.length; i++) {
            if (REMAINING[i] > 0) {
                REMAINING[i]--;
            }
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        while (AbilityKeys.SPELL_WHEEL.consumeClick()) {
            if (player != null && !player.isSpectator() && minecraft.screen == null) {
                minecraft.setScreen(new PowerWheelScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Arrays.fill(REMAINING, 0);
    }
}
