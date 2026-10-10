package nl.tivek.multiversepowers.character.thor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.character.client.Crosshairs;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.engine.math.Noise;

// Thor's crosshair: a small rune diamond round a point, like the knot on Mjolnir's handle, with four short ticks
// outside it, in storm blue. The ticks spread as he strikes or runs and jolt out as a blow lands; on a creature the
// diamond turns into a square locked on it, in its colour. With the hammer charged it burns white and sparks crackle off
// its corners. His charge ring (ThunderGauge) runs round it.
public final class ThorCrosshair {
    private static final int BLUE = 0x9FD8FF;
    private static final int WHITE = 0xF4FBFF;
    private static final float RUNE = 2.8F;
    private static final float GAP = 2.2F;
    private static final float TICK = 2.6F;
    private static final float WIDTH = 0.9F;

    private ThorCrosshair() {
    }

    public static float draw(GuiGraphics graphics, LocalPlayer player, float cx, float cy, Crosshairs.Feel feel) {
        boolean charged = ClientThor.has(player, ThorStatePayload.HAMMER_CHARGED);
        float time = player.tickCount + feel.partialTick();
        float aim = feel.aim();
        int own = charged ? WHITE : BLUE;
        int color = Crosshairs.tint(own, feel, 0.35F);
        float rune = RUNE + 0.8F * feel.spread() - 0.5F * aim;
        float turn = Mth.HALF_PI * 0.5F * aim;
        for (int side = 0; side < 4; side++) {
            float a = side * Mth.HALF_PI + turn;
            float b = a + Mth.HALF_PI;
            Crosshairs.stroke(graphics, cx + Mth.sin(a) * rune, cy - Mth.cos(a) * rune, cx + Mth.sin(b) * rune,
                    cy - Mth.cos(b) * rune, WIDTH, color, 0.9F);
        }
        float in = rune + GAP + 3.0F * feel.spread() - 0.8F * aim + 1.6F * feel.hit();
        float out = in + TICK;
        for (int side = 0; side < 4; side++) {
            float a = side * Mth.HALF_PI;
            float dx = Mth.sin(a);
            float dy = -Mth.cos(a);
            Crosshairs.stroke(graphics, cx + dx * in, cy + dy * in, cx + dx * out, cy + dy * out, WIDTH, color, 0.9F);
        }
        if (charged) {
            sparks(graphics, cx, cy, rune, time);
        }
        Crosshairs.dot(graphics, cx, cy, 0.6F + 0.25F * aim, Crosshairs.tint(own, feel, 1.0F), 0.95F);
        return out;
    }

    // Short forked sparks off the diamond's corners, each flaring for a moment now and then.
    private static void sparks(GuiGraphics graphics, float cx, float cy, float rune, float time) {
        int beat = (int) (time * 0.5F);
        for (int corner = 0; corner < 4; corner++) {
            if (Noise.of(beat, corner, 11) < 0.55) {
                continue;
            }
            float a = (corner + 0.5F) * Mth.HALF_PI + (float) (Noise.of(beat, corner, 12) - 0.5) * 0.5F;
            float dx = Mth.sin(a);
            float dy = -Mth.cos(a);
            float kink = (float) (Noise.of(beat, corner, 13) - 0.5) * 1.6F;
            float from = rune * 0.75F;
            float x1 = cx + dx * (from + 1.4F) - dy * kink;
            float y1 = cy + dy * (from + 1.4F) + dx * kink;
            Crosshairs.stroke(graphics, cx + dx * from, cy + dy * from, x1, y1, 0.6F, WHITE, 0.85F);
            Crosshairs.stroke(graphics, x1, y1, cx + dx * (from + 2.8F), cy + dy * (from + 2.8F), 0.6F, WHITE, 0.7F);
        }
    }
}
