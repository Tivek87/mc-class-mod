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
// and fades, with a sharp click, a punchy thump and a bright ring (built by scripts/sounds/kill_confirm.mjs). Only the
// player who made the kill sees and hears it. A character's own crosshair claims the frame and draws the cross round
// itself, at its own size (cross), and flashes with each blow that lands (hit).
@Mod(value = MultiversePowers.MODID, dist = Dist.CLIENT)
public final class KillMarker {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "kill_marker");
    private static final ResourceLocation SOUND = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "ui.kill_confirm");
    private static final float VOLUME = 1.0F;
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
    public static final int RED = 0xE8261C;
    private static final float HIT_MS = 200.0F;
    private static long shownAt;
    private static long hitAt;
    private static boolean claimed;

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

    public static void hit() {
        hitAt = Util.getMillis();
    }

    // How bright the last landed blow still flashes: 1 as it lands, gone in a fifth of a second.
    public static float hitFlash() {
        long since = Util.getMillis() - hitAt;
        if (hitAt == 0L || since >= HIT_MS) {
            return 0.0F;
        }
        float left = 1.0F - since / HIT_MS;
        return left * left;
    }

    // How strongly the last kill still shows: 1 while it holds, then fading out.
    public static float killed() {
        long since = Util.getMillis() - shownAt;
        if (shownAt == 0L || since >= SHOWN_MS) {
            return 0.0F;
        }
        float u = since / SHOWN_MS;
        float left = u < HOLD ? 1.0F : 1.0F - (u - HOLD) / (1.0F - HOLD);
        return left * left;
    }

    // A character's crosshair draws the cross itself this frame.
    public static void claim() {
        claimed = true;
    }

    // The red cross round (x, y), its strokes running out from `inner` to `outer` (gui pixels from the middle).
    public static void cross(GuiGraphics graphics, float x, float y, float inner, float outer) {
        long since = Util.getMillis() - shownAt;
        float alpha = killed();
        if (alpha <= 0.0F) {
            return;
        }
        float popped = Mth.clamp(since / POP_MS, 0.0F, 1.0F);
        float out = POP * (1.0F - popped) * (1.0F - popped);
        float from = (inner + out) * Mth.SQRT_OF_TWO * 0.5F;
        float to = (outer + out) * Mth.SQRT_OF_TWO * 0.5F;
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
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean mine = claimed;
        claimed = false;
        if (mine || minecraft.options.hideGui || minecraft.player == null) {
            return;
        }
        cross(graphics, graphics.guiWidth() * 0.5F, graphics.guiHeight() * 0.5F, INNER, OUTER);
        GuiShapes.flush(graphics);
    }
}
