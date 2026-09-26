package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.character.greenlantern.client.body.FlameArms;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// The flamethrower's heat: ten cells filling as the inferno pours towards overheating, green to yellow to red, the
// last three blinking ever faster; all red while the gun vents, draining as it cools.
final class HeatGauge {
    private static final int CELLS = 10;
    private static final float GAP = 2.0F;
    private static final int YELLOW = 0xFFD84A;
    private static final int ORANGE = 0xFF8A2A;
    private static final int RED = 0xFF3B2E;
    private static final float HOT = 0.7F;
    private static final float FADE_MS = 300.0F;
    private static long lastSeen;
    private static float lastCharge;
    private static boolean lastVenting;

    private HeatGauge() {
    }

    static boolean render(GuiGraphics graphics, float x, float y, float partialTick, List<Runnable> labels) {
        float charge = FlameArms.overcharge(partialTick);
        boolean venting = FlameArms.overheated();
        long now = Util.getMillis();
        float appear = 1.0F;
        if (charge > 0.001F || venting) {
            lastSeen = now;
            lastCharge = Math.max(0.0F, charge);
            lastVenting = venting;
        } else {
            // A stream let go of fades out rather than blinking off.
            appear = 1.0F - (now - lastSeen) / FADE_MS;
            if (lastSeen == 0L || appear <= 0.0F) {
                return false;
            }
            charge = lastCharge;
            venting = lastVenting;
        }
        float blink = charge > HOT || venting ? 0.5F + 0.5F * Mth.sin((now % 100000L) / (venting ? 90.0F
                : 120.0F - 80.0F * charge)) : 1.0F;
        float piece = ArcGauge.piece(CELLS, GAP);
        for (int k = 0; k < CELLS; k++) {
            float from = ArcGauge.pieceFrom(k, CELLS, GAP);
            float to = from + piece;
            float fill = Mth.clamp(charge * CELLS - k, 0.0F, 1.0F);
            int color = venting ? RED : cell(k);
            ArcGauge.empty(graphics, x, y, ArcGauge.INNER, ArcGauge.OUTER, from, to, appear);
            float lit = k >= CELLS - 3 || venting ? appear * (0.55F + 0.45F * blink) : appear;
            ArcGauge.filled(graphics, x, y, ArcGauge.INNER, ArcGauge.OUTER, from, to, fill, color, color, lit);
        }
        if (charge > HOT || venting) {
            float hot = venting ? 1.0F : (charge - HOT) / (1.0F - HOT);
            GuiShapes.arc(graphics, x, y, ArcGauge.OUTER + 1.5F, ArcGauge.OUTER + 3.0F, ArcGauge.FROM,
                    ArcGauge.FROM + ArcGauge.SPAN, GuiShapes.fade(RED, hot * blink * appear));
        }
        GuiShapes.flush(graphics);
        int percent = Math.round(charge * 100.0F);
        Component title = venting ? Component.translatable(ArcGauge.PREFIX + "overheat")
                : Component.translatable(ArcGauge.PREFIX + "heat", percent);
        int titleColor = venting ? GuiShapes.mix(RED, 0xFFFFFF, 0.3F * blink) : charge > HOT
                ? GuiShapes.mix(ORANGE, RED, blink) : GuiShapes.mix(ArcGauge.BRIGHT, YELLOW, charge / HOT);
        Component line = venting ? Component.translatable(ArcGauge.PREFIX + "cooling")
                : charge > HOT ? Component.translatable(ArcGauge.PREFIX + "heat_hot") : null;
        int lineColor = venting ? 0xFFB0A0 : 0xFFD8B0;
        float shown = appear;
        labels.add(() -> ArcGauge.label(graphics, Minecraft.getInstance().font, x, y, title, titleColor, line,
                lineColor, shown));
        return true;
    }

    private static int cell(int k) {
        if (k < 4) {
            return GuiShapes.mix(ArcGauge.GREEN, YELLOW, k / 4.0F);
        }
        if (k < 7) {
            return GuiShapes.mix(YELLOW, ORANGE, (k - 4) / 3.0F);
        }
        return GuiShapes.mix(ORANGE, RED, (k - 7) / 2.0F);
    }
}
