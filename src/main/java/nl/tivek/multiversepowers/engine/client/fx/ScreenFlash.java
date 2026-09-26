package nl.tivek.multiversepowers.engine.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import nl.tivek.multiversepowers.MultiversePowers;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ScreenFlash {
    private static int color;
    private static float strength;
    private static float fade;

    private ScreenFlash() {
    }

    // The whole screen washes over in one colour and clears again over the given ticks.
    public static void add(int rgb, float amount, int ticks) {
        if (amount > strength && ticks > 0) {
            color = rgb & 0xFFFFFF;
            strength = Math.min(1.0F, amount);
            fade = strength / ticks;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Minecraft.getInstance().isPaused()) {
            strength = Math.max(0.0F, strength - fade);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (strength <= 0.0F) {
            return;
        }
        float now = Math.max(0.0F, strength - fade * event.getPartialTick().getGameTimeDeltaPartialTick(false));
        int alpha = Math.round(255.0F * now * now);
        if (alpha <= 0) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | color);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        strength = 0.0F;
    }
}
