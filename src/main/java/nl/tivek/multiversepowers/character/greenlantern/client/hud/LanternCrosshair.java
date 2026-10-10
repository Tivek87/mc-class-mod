package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.Crosshairs;
import nl.tivek.multiversepowers.character.greenlantern.client.body.heavy.ClientHeavy;
import nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// Green Lantern's crosshair: the lantern's emblem, a ring between two bars, in the ring's green. It blooms and springs
// back with what he does and closes in on a creature under it, its middle in that creature's colour; within the fists'
// reach four knuckle marks close in round it, so you know a punch will land. A landed blow slams the bars in. The
// shotgun adds the ring its pellets spread over, the rocket launcher a drop mark under it.
final class LanternCrosshair {
    private static final int GREEN = 0x7CFF9C;
    private static final int PALE = 0xE4FFE9;
    private static final float RING = 3.4F;
    private static final float BAR = 6.2F;
    private static final float BAR_HALF = 4.6F;
    private static final float WIDTH = 1.1F;
    private static final float MARKS = 8.5F;
    // About how far the shotgun's pellets stray (radians), as a ring round the emblem.
    private static final double PELLET_SPREAD = 0.09;

    private LanternCrosshair() {
    }

    static float draw(GuiGraphics graphics, LocalPlayer player, float cx, float cy, Crosshairs.Feel feel) {
        float spread = feel.spread();
        float aim = feel.aim();
        float ring = RING + 1.6F * spread - 0.6F * aim;
        float bar = BAR + 2.8F * spread - 1.4F * aim - 1.2F * feel.hit();
        float half = BAR_HALF - 0.8F * aim;
        int own = GuiShapes.mix(GREEN, PALE, 0.5F * aim);
        int color = Crosshairs.tint(own, feel, 0.0F);
        Crosshairs.ring(graphics, cx, cy, ring, WIDTH, color, 0.9F);
        Crosshairs.stroke(graphics, cx - half, cy - bar, cx + half, cy - bar, WIDTH, color, 0.9F);
        Crosshairs.stroke(graphics, cx - half, cy + bar, cx + half, cy + bar, WIDTH, color, 0.9F);
        Crosshairs.dot(graphics, cx, cy, 0.7F + 0.3F * aim, Crosshairs.tint(own, feel, 1.0F), 0.95F);
        float reach = bar;
        if (inReach(player) && aim > 0.05F) {
            float in = MARKS - 1.5F * aim;
            for (int corner = 0; corner < 4; corner++) {
                float sx = corner % 2 == 0 ? 1.0F : -1.0F;
                float sy = corner < 2 ? 1.0F : -1.0F;
                float x = cx + sx * in;
                float y = cy + sy * in * 0.7F;
                Crosshairs.stroke(graphics, x, y, x - sx * 2.0F, y - sy * 1.4F, 1.4F, Crosshairs.tint(PALE, feel,
                        0.6F), 0.95F * aim);
            }
        }
        int heavy = ClientHeavy.holding();
        if (heavy == HeavyMoves.SHOTGUN) {
            float spreadRing = pixels(graphics, PELLET_SPREAD) * (1.0F + 0.5F * spread);
            for (int dash = 0; dash < 8; dash++) {
                float from = dash * 45.0F + 8.0F;
                GuiShapes.arc(graphics, cx, cy, spreadRing - 1.0F, spreadRing + 1.0F, from, from + 29.0F,
                        GuiShapes.fade(0x000000, 0.3F));
                GuiShapes.arc(graphics, cx, cy, spreadRing - 0.45F, spreadRing + 0.45F, from, from + 29.0F,
                        GuiShapes.fade(color, 0.75F));
            }
            reach = Math.max(reach, spreadRing);
        } else if (heavy == HeavyMoves.RPG) {
            float y = cy + bar + 3.0F;
            Crosshairs.stroke(graphics, cx - 2.6F, y, cx, y + 2.0F, WIDTH, color, 0.85F);
            Crosshairs.stroke(graphics, cx, y + 2.0F, cx + 2.6F, y, WIDTH, color, 0.85F);
            Crosshairs.stroke(graphics, cx - 1.4F, y + 5.0F, cx + 1.4F, y + 5.0F, WIDTH, color, 0.6F);
        }
        return reach;
    }

    // An angle off the middle of the view as gui pixels from the crosshair.
    private static float pixels(GuiGraphics graphics, double angle) {
        double fov = Math.toRadians(Minecraft.getInstance().options.fov().get());
        return (float) (Math.tan(angle) / Math.tan(fov * 0.5) * graphics.guiHeight() * 0.5);
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
