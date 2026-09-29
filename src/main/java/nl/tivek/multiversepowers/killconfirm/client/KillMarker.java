package nl.tivek.multiversepowers.killconfirm.client;

import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// A kill marked at the crosshair, as Red Dead Redemption 2 does: a small red cross flicks out round it, holds a moment
// and fades, with a soft thud. Only the player who made the kill sees and hears it.
@Mod(value = MultiversePowers.MODID, dist = Dist.CLIENT)
public final class KillMarker {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "kill_marker");
    private static final ResourceLocation SOUND = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "ui.kill_confirm");
    private static final float VOLUME = 0.6F;
    // Kills this close together (milliseconds) are one: one sound, the cross flicked out again.
    private static final long TOGETHER_MS = 60L;
    private static final float SHOWN_MS = 480.0F;
    private static final float POP_MS = 70.0F;
    private static final float HOLD = 0.35F;
    // Each stroke of the cross runs along a diagonal from INNER to OUTER (gui pixels from the middle), starting POP
    // further out.
    private static final float INNER = 3.5F;
    private static final float OUTER = 6.5F;
    private static final float POP = 1.5F;
    private static final float WIDTH = 1.4F;
    private static final float EDGE = 1.2F;
    private static final int RED = 0xE8261C;
    private static long shownAt;

    public KillMarker(IEventBus modEventBus) {
        modEventBus.addListener(KillMarker::onRegisterLayers);
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, LAYER_ID, KillMarker::render);
    }

    public static void confirm() {
        long now = Util.getMillis();
        if (now - shownAt > TOGETHER_MS) {
            Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(SOUND, SoundSource.PLAYERS, VOLUME,
                    1.0F, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0,
                    0.0, true));
        }
        shownAt = now;
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        long since = Util.getMillis() - shownAt;
        if (shownAt == 0L || since >= SHOWN_MS || minecraft.options.hideGui || minecraft.player == null) {
            return;
        }
        float u = since / SHOWN_MS;
        float popped = Mth.clamp(since / POP_MS, 0.0F, 1.0F);
        float out = POP * (1.0F - popped) * (1.0F - popped);
        float left = u < HOLD ? 1.0F : 1.0F - (u - HOLD) / (1.0F - HOLD);
        float alpha = left * left;
        float x = graphics.guiWidth() * 0.5F;
        float y = graphics.guiHeight() * 0.5F;
        float from = (INNER + out) * Mth.SQRT_OF_TWO * 0.5F;
        float to = (OUTER + out) * Mth.SQRT_OF_TWO * 0.5F;
        // A dark edge under the red, so it reads on snow and sky as well.
        for (int pass = 0; pass < 2; pass++) {
            float width = pass == 0 ? WIDTH + EDGE : WIDTH;
            int argb = pass == 0 ? GuiShapes.fade(0x000000, 0.4F * alpha) : GuiShapes.fade(RED, alpha);
            for (int k = 0; k < 4; k++) {
                float dx = (k & 1) == 0 ? 1.0F : -1.0F;
                float dy = (k & 2) == 0 ? 1.0F : -1.0F;
                GuiShapes.stroke(graphics, x + dx * from, y + dy * from, x + dx * to, y + dy * to, width, argb);
            }
        }
        GuiShapes.flush(graphics);
    }
}
