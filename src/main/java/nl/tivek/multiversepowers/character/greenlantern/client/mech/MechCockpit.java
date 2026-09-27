package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.MechScript;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.math.Ease;

// The cockpit in the chest: its back and floor, the seat, the console with its buttons, the two levers and the glass
// of the port. While the chest is still open the pilot hangs in a bubble of light that shrinks away as the glass forms.
final class MechCockpit {
    static final int GLAZED = MechScript.ARMOR + 16;
    private static final double BUBBLE_FAINT = 0.12;
    private static final double BUBBLE_FAINT_OWN = 0.05;
    private static final double GLASS_FAINT = 0.2;
    private static final double GLASS_FAINT_OWN = 0.035;
    private static final double SEAM = 0.6;
    private static final double BUTTON_DIP = 0.03;

    private MechCockpit() {
    }

    static void draw(LanternPainter painter, MechPose pose, Frame body, double t, double apart, boolean own,
            int seed) {
        MechScript.Stage torso = pose.torso();
        double floor = MechScript.COCKPIT.y;
        if (t >= MechScript.ARMOR) {
            MechPainter.rising(painter, torso, floor - 0.3, MechBodyShapes.CHEST_TOP,
                    Mth.clamp((t - MechScript.ARMOR) / 16.0, 0.0, 1.0), apart);
            MechParts.draw(painter, MechBodyShapes.CABIN, body, 1.0, apart, seed);
            if (!own) {
                MechParts.draw(painter, MechBodyShapes.WALLS, body, 1.0, apart, seed + 1);
            }
            painter.noClip();
        }
        if (t >= MechScript.SIT - 8) {
            MechPainter.rising(painter, torso, floor, floor + 2.0, Mth.clamp((t - MechScript.SIT + 8.0) / 8.0, 0.0,
                    1.0), apart);
            MechParts.draw(painter, MechBodyShapes.SEAT, body.moved(0.0, floor, 0.0), 1.0, apart, seed + 2);
            painter.noClip();
        }
        if (t >= MechScript.GRIP - 8) {
            double grown = Mth.clamp((t - MechScript.GRIP + 8.0) / 6.0, 0.0, 1.0);
            MechPainter.rising(painter, torso, floor, floor + 1.1, grown, apart);
            MechParts.draw(painter, MechBodyShapes.CONSOLE, body, 1.0, apart, seed + 3);
            for (int k = 0; k < MechScript.BUTTONS.length; k++) {
                boolean pressed = pose.button == k;
                Vec3 at = MechScript.BUTTONS[k].subtract(0.0, pressed ? BUTTON_DIP * pose.press : 0.0, 0.0);
                MechParts.draw(painter, MechBodyShapes.BUTTON, body.moved(at.x, at.y, at.z), pressed
                        ? 1.1 + 0.8 * pose.press : 1.1, apart, seed + 4 + k);
            }
            painter.noClip();
            for (int side = -1; side <= 1; side += 2) {
                double push = pose.push(side) * MechScript.LEVER_THROW;
                Vec3 pivot = torso.point(side * MechScript.LEVER.x, MechScript.LEVER.y, MechScript.LEVER.z);
                Vec3 up = torso.dir(new Vec3(0.0, Math.cos(push), Math.sin(push)));
                Vec3 ahead = torso.dir(new Vec3(0.0, -Math.sin(push), Math.cos(push)));
                if (grown < 1.0 && apart < 0.0) {
                    painter.clip(pivot.add(up.scale(MechScript.LEVER_LENGTH * grown)), up.scale(-1.0), SEAM);
                }
                MechParts.draw(painter, MechBodyShapes.LEVER, Frame.of(pivot, ahead, up, 1.0), 1.0, apart,
                        seed + 10 + side);
                painter.noClip();
            }
        }
        if (apart >= 0.0) {
            return;
        }
        if (t >= MechScript.CORE && t < GLAZED) {
            double grown = Ease.backOut(Mth.clamp((t - MechScript.CORE) / MechScript.FORM_TICKS, 0.0, 1.0))
                    * (1.0 - Ease.smooth((t - GLAZED + 6.0) / 6.0));
            Frame bubble = Frame.of(torso.point(MechBodyShapes.CORE), torso.ahead(), torso.up(),
                    Math.max(0.02, grown));
            painter.creases(0.0);
            painter.seeThrough(MechBodyShapes.BUBBLE, bubble, own ? BUBBLE_FAINT_OWN : BUBBLE_FAINT, 1.25);
            painter.creases(MechPainter.CREASES);
        }
        if (t >= GLAZED - 6) {
            double grown = Ease.backOut(Mth.clamp((t - GLAZED + 6.0) / 6.0, 0.0, 1.0));
            Frame glass = Frame.of(torso.point(0.0, MechBodyShapes.PORT_Y, MechBodyShapes.GLASS_Z), torso.ahead(),
                    torso.up(), Math.max(0.02, grown));
            painter.creases(0.0);
            painter.seeThrough(MechBodyShapes.GLASS, glass, own ? GLASS_FAINT_OWN : GLASS_FAINT, 1.2);
            painter.creases(MechPainter.CREASES);
        }
    }
}
