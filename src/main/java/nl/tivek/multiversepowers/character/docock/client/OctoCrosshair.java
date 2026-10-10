package nl.tivek.multiversepowers.character.docock.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.character.client.Crosshairs;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// Doctor Octopus's crosshair: a steel ring round a point, like the lens of his goggles; his tentacles' corners
// (TentacleHud) sit round it. It widens as he strikes or runs; on a creature four lens arcs turn in and lock round it,
// the point in its colour, a thin arc of its health round them; a landed blow flashes it. It reacts as every crosshair
// does (Crosshairs.Feel).
public final class OctoCrosshair {
    private static final int STEEL = 0xE6ECF2;
    private static final int AMBER = 0xFFB547;

    private OctoCrosshair() {
    }

    public static float draw(GuiGraphics graphics, LocalPlayer player, float cx, float cy, Crosshairs.Feel feel) {
        float aim = feel.aim();
        float ring = feel.radius(2.6F + 2.0F * feel.spread() - 0.4F * aim);
        float rx = cx + feel.swayX();
        float ry = cy + feel.swayY();
        Crosshairs.ring(graphics, rx, ry, ring, 0.9F + feel.thick(), Crosshairs.tint(STEEL, feel, 0.0F), 0.9F);
        Crosshairs.dot(graphics, cx, cy, 0.6F + 0.3F * aim, Crosshairs.tint(AMBER, feel, 1.0F), 0.95F);
        if (aim < 0.05F) {
            return ring;
        }
        float lens = ring + 3.0F - 1.0F * aim;
        Crosshairs.health(graphics, rx, ry, lens + 2.2F, feel);
        cx = rx;
        cy = ry;
        float turn = 45.0F * (1.0F - aim) + feel.turn() * Mth.RAD_TO_DEG;
        int color = Crosshairs.tint(STEEL, feel, 0.7F);
        for (int arc = 0; arc < 4; arc++) {
            float from = arc * 90.0F + 20.0F + turn;
            GuiShapes.arc(graphics, cx, cy, lens - 1.0F, lens + 1.0F, from, from + 50.0F,
                    GuiShapes.fade(0x000000, 0.35F * aim));
            GuiShapes.arc(graphics, cx, cy, lens - 0.45F, lens + 0.45F, from, from + 50.0F,
                    GuiShapes.fade(color, 0.9F * aim));
        }
        return lens;
    }
}
