package nl.tivek.multiversepowers.character.docock.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import nl.tivek.multiversepowers.character.client.Crosshairs;

// Doctor Octopus's crosshair: a steel ring round a point, like the lens of his goggles; his tentacles' corners
// (TentacleHud) sit round it.
public final class OctoCrosshair {
    private static final int STEEL = 0xE6ECF2;
    private static final int AMBER = 0xFFB547;

    private OctoCrosshair() {
    }

    public static void draw(GuiGraphics graphics, LocalPlayer player, float cx, float cy, float partialTick) {
        Crosshairs.ring(graphics, cx, cy, 2.6F, 0.9F, STEEL, 0.9F);
        Crosshairs.dot(graphics, cx, cy, 0.6F, AMBER, 0.95F);
    }
}
