package nl.tivek.multiversepowers.classes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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

    public static void requestWelcomeScreen() {
        // Already picking one: he died and came straight back without a death screen, which leaves this one open.
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof WelcomeScreen || screen instanceof GroupSelectionScreen
                || screen instanceof ClassSelectionScreen) {
            return;
        }
        welcomePending = true;
    }

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
        if (player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        welcomePending = false;
        String playerName = player.getGameProfile().getName();
        MultiversePowers.LOGGER.info("Opening welcome screen for {}", playerName);
        minecraft.setScreen(new WelcomeScreen(playerName));
    }
}
