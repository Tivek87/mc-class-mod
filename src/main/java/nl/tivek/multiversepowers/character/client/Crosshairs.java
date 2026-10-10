package nl.tivek.multiversepowers.character.client;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// A character's own crosshair in place of the game's: each draws its own round the middle of the screen, in its own
// colours. The game's comes back with the debug screen, outside first person and without a character.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Crosshairs {
    @FunctionalInterface
    public interface Drawer {
        void draw(GuiGraphics graphics, LocalPlayer player, float cx, float cy, float partialTick);
    }

    private static final Map<GameCharacter, Drawer> DRAWERS = new EnumMap<>(GameCharacter.class);

    private Crosshairs() {
    }

    public static void add(GameCharacter character, Drawer drawer) {
        DRAWERS.put(character, drawer);
    }

    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR) || event.isCanceled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        GameCharacter character = ClientCharacter.active();
        Drawer drawer = character == null ? null : DRAWERS.get(character);
        if (drawer == null || player == null || player.isSpectator() || minecraft.options.hideGui
                || !minecraft.options.getCameraType().isFirstPerson()
                || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        event.setCanceled(true);
        GuiGraphics graphics = event.getGuiGraphics();
        drawer.draw(graphics, player, graphics.guiWidth() / 2.0F, graphics.guiHeight() / 2.0F,
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
        GuiShapes.flush(graphics);
    }

    // A stroke with a dark edge under it, so it reads on snow and sky alike.
    public static void stroke(GuiGraphics graphics, float x0, float y0, float x1, float y1, float width, int rgb,
            float alpha) {
        GuiShapes.stroke(graphics, x0, y0, x1, y1, width + 1.0F, GuiShapes.fade(0x000000, 0.4F * alpha));
        GuiShapes.stroke(graphics, x0, y0, x1, y1, width, GuiShapes.fade(rgb, alpha));
    }

    public static void ring(GuiGraphics graphics, float cx, float cy, float radius, float width, int rgb,
            float alpha) {
        GuiShapes.ring(graphics, cx, cy, radius, width + 1.0F, GuiShapes.fade(0x000000, 0.4F * alpha));
        GuiShapes.ring(graphics, cx, cy, radius, width, GuiShapes.fade(rgb, alpha));
    }

    public static void dot(GuiGraphics graphics, float cx, float cy, float radius, int rgb, float alpha) {
        GuiShapes.disc(graphics, cx, cy, radius + 0.5F, GuiShapes.fade(0x000000, 0.4F * alpha));
        GuiShapes.disc(graphics, cx, cy, radius, GuiShapes.fade(rgb, alpha));
    }
}
