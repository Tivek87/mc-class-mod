package nl.tivek.multiversepowers.character.thor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.character.client.Crosshairs;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;

// Thor's crosshair: four small bolts of lightning pointing in at a spark in the middle, in storm blue. With the hammer
// charged they burn white and flicker with it. His charge ring (ThunderGauge) still runs round it.
public final class ThorCrosshair {
    private static final int BLUE = 0x9FD8FF;
    private static final int WHITE = 0xF4FBFF;
    private static final float OUT = 7.0F;
    private static final float IN = 3.2F;
    private static final float KINK = 1.3F;
    private static final float WIDTH = 1.0F;

    private ThorCrosshair() {
    }

    public static void draw(GuiGraphics graphics, LocalPlayer player, float cx, float cy, float partialTick) {
        boolean charged = ClientThor.has(player, ThorStatePayload.HAMMER_CHARGED);
        float time = player.tickCount + partialTick;
        float flicker = charged ? 0.75F + 0.25F * Mth.sin(time * 1.7F) * Mth.sin(time * 0.6F) : 0.9F;
        int color = charged ? WHITE : BLUE;
        for (int arm = 0; arm < 4; arm++) {
            float angle = arm * Mth.HALF_PI;
            float dx = Mth.sin(angle);
            float dy = -Mth.cos(angle);
            // Across the bolt: its zigzag kinks one way and back.
            float sx = -dy;
            float sy = dx;
            float mid = (OUT + IN) * 0.5F;
            float ax = cx + dx * OUT;
            float ay = cy + dy * OUT;
            float bx = cx + dx * (mid + 0.6F) + sx * KINK;
            float by = cy + dy * (mid + 0.6F) + sy * KINK;
            float qx = cx + dx * (mid - 0.6F) - sx * KINK;
            float qy = cy + dy * (mid - 0.6F) - sy * KINK;
            float ex = cx + dx * IN;
            float ey = cy + dy * IN;
            Crosshairs.stroke(graphics, ax, ay, bx, by, WIDTH, color, flicker);
            Crosshairs.stroke(graphics, bx, by, qx, qy, WIDTH, color, flicker);
            Crosshairs.stroke(graphics, qx, qy, ex, ey, WIDTH, color, flicker);
        }
        Crosshairs.dot(graphics, cx, cy, charged ? 1.0F : 0.7F, color, 0.95F);
    }
}
