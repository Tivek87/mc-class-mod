package nl.tivek.welcomescreen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.client.classes.ClientClassData;
import nl.tivek.welcomescreen.client.classes.EffectTestScreen;
import nl.tivek.welcomescreen.client.classes.WelcomeScreen;

@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static boolean welcomePending;

    private ClientEvents() {
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
        WelcomeScreenMod.LOGGER.info("Opening welcome screen for {}", playerName);
        minecraft.setScreen(new WelcomeScreen(playerName));
    }
}
