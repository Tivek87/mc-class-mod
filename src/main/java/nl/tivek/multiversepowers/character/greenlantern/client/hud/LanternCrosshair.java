package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.Crosshairs;

// Green Lantern's crosshair: the lantern's emblem, a ring between two bars, in the ring's green. With a creature
// under it within the fists' reach four knuckle marks close in round it, so you know a punch will land.
final class LanternCrosshair {
    private static final int GREEN = 0x7CFF9C;
    private static final int PALE = 0xE4FFE9;
    private static final float RING = 3.4F;
    private static final float BAR = 6.2F;
    private static final float BAR_HALF = 4.6F;
    private static final float WIDTH = 1.1F;
    private static final float MARKS = 8.5F;

    private LanternCrosshair() {
    }

    static void draw(GuiGraphics graphics, LocalPlayer player, float cx, float cy, float partialTick) {
        boolean near = inReach(player);
        int color = near ? PALE : GREEN;
        Crosshairs.ring(graphics, cx, cy, RING, WIDTH, color, 0.9F);
        Crosshairs.stroke(graphics, cx - BAR_HALF, cy - BAR, cx + BAR_HALF, cy - BAR, WIDTH, color, 0.9F);
        Crosshairs.stroke(graphics, cx - BAR_HALF, cy + BAR, cx + BAR_HALF, cy + BAR, WIDTH, color, 0.9F);
        Crosshairs.dot(graphics, cx, cy, 0.7F, color, 0.95F);
        if (!near) {
            return;
        }
        for (int corner = 0; corner < 4; corner++) {
            float sx = corner % 2 == 0 ? 1.0F : -1.0F;
            float sy = corner < 2 ? 1.0F : -1.0F;
            float x = cx + sx * MARKS;
            float y = cy + sy * MARKS * 0.7F;
            Crosshairs.stroke(graphics, x, y, x - sx * 2.0F, y - sy * 1.4F, 1.4F, PALE, 0.95F);
        }
    }

    // A creature under the crosshair that the fists' next punch reaches.
    private static boolean inReach(LocalPlayer player) {
        Entity target = Minecraft.getInstance().crosshairPickEntity;
        CharacterAbility fists = GameCharacter.GREEN_LANTERN.byName("light_fists");
        if (target == null || fists == null || !player.getMainHandItem().isEmpty()) {
            return false;
        }
        double reach = fists.value("reachBlocks") + 0.5;
        return target.getBoundingBox().distanceToSqr(player.getEyePosition()) <= reach * reach;
    }
}
