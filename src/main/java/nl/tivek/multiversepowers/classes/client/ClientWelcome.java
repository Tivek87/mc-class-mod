package nl.tivek.multiversepowers.classes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import nl.tivek.multiversepowers.MultiversePowers;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientWelcome {
    private static boolean welcomePending;

    private ClientWelcome() {
    }

    /** Called when the server says this player still has to pick a class. */
    public static void requestWelcomeScreen() {
        welcomePending = true;
    }

    /** /classfx opens the developer menu that plays any class's start ceremony. */
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("classfx").executes(context -> {
            Minecraft minecraft = Minecraft.getInstance();
            // A command runs while the chat screen is still open, so switch screens afterwards.
            minecraft.tell(() -> minecraft.setScreen(new EffectTestScreen()));
            return 1;
        }));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        welcomePending = false;
        ClientClassData.set(null);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!welcomePending) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        // Wait until the world is actually loaded and the loading screen is gone.
        if (player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        welcomePending = false;
        String playerName = player.getGameProfile().getName();
        MultiversePowers.LOGGER.info("Opening welcome screen for {}", playerName);
        minecraft.setScreen(new WelcomeScreen(playerName));
    }
}
