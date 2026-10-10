package nl.tivek.multiversepowers.engine.client.escape;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import org.lwjgl.glfw.GLFW;

// The escape game on screen, low in the middle, and its input: the left button plays it and does nothing else, and
// a held player neither walks nor jumps.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class EscapeHud {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "escape");
    private static final float WIDTH = 180.0F;
    private static final float HEIGHT = 12.0F;
    private static final float LOW = 0.66F;

    private EscapeHud() {
    }

    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, EscapeHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!EscapeGames.shown() || minecraft.options.hideGui) {
            return;
        }
        EscapeGames.tick();
        float width = Math.min(WIDTH, graphics.guiWidth() - 40.0F);
        EscapeGames.draw(graphics, (graphics.guiWidth() - width) / 2.0F, graphics.guiHeight() * LOW, width, HEIGHT);
        graphics.flush();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!EscapeGames.active() || minecraft.screen != null || minecraft.player == null) {
            return;
        }
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_PRESS) {
            EscapeGames.click();
        }
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMovement(MovementInputUpdateEvent event) {
        if (!EscapeGames.active()) {
            return;
        }
        event.getInput().forwardImpulse = 0.0F;
        event.getInput().leftImpulse = 0.0F;
        event.getInput().up = false;
        event.getInput().down = false;
        event.getInput().left = false;
        event.getInput().right = false;
        event.getInput().jumping = false;
        event.getInput().shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (EscapeGames.active() && minecraft.player != null) {
            // Every attack and use the game might still see waits: the left button plays here.
            while (minecraft.options.keyAttack.consumeClick()) {
            }
            while (minecraft.options.keyUse.consumeClick()) {
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        EscapeGames.told(0);
    }
}
