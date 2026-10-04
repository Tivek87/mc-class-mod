package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.MouseHold;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechBodyShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechHeadShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechPose;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechBeam;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The mech's beams as everyone sees them, aimed where its pilot's crosshair rests: the eye slits blaze and a beam of
// rings shoots out of the visor; for the Unibeam the port on its chest charges, sparks winding in, then a huge beam
// bursts out of it with a ring of light thrown off its rim, and burns there till it dies away. From the cockpit the
// port's light stays a ring round the view and the beam starts a little ahead, so its pilot still sees what it hits.
final class MechBeamFx {
    private static final double EYE_THICK = 1.8;
    private static final double UNIBEAM_THICK = 4.5;
    // A pose's blow runs about two ticks behind the server's: drawn that far ahead, a beam shows as it hits.
    private static final double LEAD = 2.0;
    private static final double OWN_AHEAD = 3.0;
    private static final int FADE = 4;
    private static final int SPARKS = 12;

    private MechBeamFx() {
    }

    static void draw(LanternPainter painter, MechPose pose, double t, int pilotId, boolean own, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Entity pilot = level == null ? null : level.getEntity(pilotId);
        MechAttacks.Blow blow = pose.blow();
        if (blow.kind() == MechAttacks.UNIBEAM) {
            unibeam(painter, pose, blow.age(), pilot, own);
        } else if (minecraft.player != null && minecraft.player.getId() == pilotId) {
            // While its pilot holds the button, the port charges in their own game before the server fires it.
            CharacterAbility shield = GameCharacter.GREEN_LANTERN.byName("light_shield");
            float held = MouseHold.progress(shield, partialTick);
            if (held > 0.1F) {
                charge(painter, pose, 0.7 * Ease.smooth((held - 0.1) / 0.9), own);
            }
        }
        if (blow.kind() == MechAttacks.EYE && pilot != null && level != null) {
            eye(painter, MechPainter.head(pose, t, -1.0, true), blow.age(), level, pilot);
        }
    }

    private static void eye(LanternPainter painter, Frame head, double age, ClientLevel level, Entity pilot) {
        double since = age + LEAD - MechAttacks.EYE_FIRE;
        if (since < -1.0 || since > MechAttacks.EYE_SHOWN) {
            return;
        }
        double on = Ease.smooth(since + 1.0) * (1.0 - Ease.smooth((since - MechAttacks.EYE_SHOWN + FADE) / FADE));
        for (int side = -1; side <= 1; side += 2) {
            painter.flare(head.at(side * (MechHeadShapes.EYE_X[0] + MechHeadShapes.EYE_X[1]) * 0.5,
                    MechHeadShapes.EYE_Y, MechHeadShapes.EYE_Z), 0.5, on);
        }
        if (since < 0.0) {
            return;
        }
        Vec3 from = head.at(0.0, MechHeadShapes.EYE_Y, MechHeadShapes.EYE_Z + 0.05);
        Vec3 end = end(level, pilot, from, MechBeam.EYE_RANGE, true);
        painter.beamOfLight(from, end, on, since + 1.0, EYE_THICK, 0.2);
        painter.flare(from, 1.1, on);
        painter.flare(end, 1.6, on);
    }

    private static void unibeam(LanternPainter painter, MechPose pose, double age, @Nullable Entity pilot,
            boolean own) {
        double ahead = age + LEAD;
        if (ahead < MechAttacks.UNIBEAM_FROM) {
            charge(painter, pose, 0.7 + 0.3 * Ease.smooth(ahead / MechAttacks.UNIBEAM_FROM), own);
            return;
        }
        double since = ahead - MechAttacks.UNIBEAM_FROM;
        double last = MechAttacks.UNIBEAM_TO - MechAttacks.UNIBEAM_FROM;
        if (since > last + FADE || pilot == null) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        double on = Ease.smooth(since / 2.0) * (1.0 - Ease.smooth((since - last) / FADE));
        Vec3 port = MechBeam.port(pose.torso());
        Vec3 end = end(level, pilot, port, MechBeam.UNIBEAM_RANGE, false);
        Vec3 way = end.subtract(port);
        double length = way.length();
        if (length < 1.0E-3) {
            return;
        }
        way = way.scale(1.0 / length);
        Vec3 from = own ? port.add(way.scale(Math.min(OWN_AHEAD, length * 0.5))) : port;
        double strength = own ? 0.55 * on : on;
        painter.beamOfLight(from, end, strength, since + 1.0, UNIBEAM_THICK, 0.5);
        if (!own) {
            painter.glowLine(from, end, 7.0, LanternPainter.GREEN, Colors.alpha(0.12 * on));
            painter.flare(port, 3.0, on);
        }
        painter.flare(end, 3.2, on);
        painter.glowDisc(end, 2.6, LanternPainter.GREEN, 0.5 * on, 0.3, (int) (since * 1.7));
        Vec3[] across = Vectors.across(way);
        double rim = MechBodyShapes.PORT_IN + 0.3;
        painter.circle(port, across[0], across[1], rim, own ? 0.04 : 0.12, own ? 0.25 : 0.7,
                Colors.alpha(0.9 * on), Colors.alpha(0.5 * on));
        // The ring thrown off the rim as it bursts out.
        if (since < 8.0 && !own) {
            double u = since / 8.0;
            painter.circle(port.add(way.scale(1.5 * u)), across[0], across[1], rim + 5.0 * Ease.smooth(u), 0.18, 1.0,
                    Colors.alpha(0.95 * (1.0 - u)), Colors.alpha(0.5 * (1.0 - u)));
        }
    }

    // The port on its chest gathering light, `power` 0..1: its rim glows and sparks wind in; seen from outside a glare
    // swells in it too.
    private static void charge(LanternPainter painter, MechPose pose, double power, boolean own) {
        Frame chest = MechPainter.body(pose.torso());
        Vec3 port = MechBeam.port(pose.torso());
        Vec3 right = pose.torso().right();
        Vec3 up = pose.torso().up();
        double time = painter.time();
        painter.circle(port, right, up, MechBodyShapes.PORT_IN + 0.05, own ? 0.03 : 0.08, own ? 0.2 : 0.6,
                Colors.alpha((own ? 0.5 : 0.95) * power), Colors.alpha(0.5 * power));
        for (int k = 0; k < SPARKS; k++) {
            double turn = Math.PI * 2.0 * k / SPARKS + time * 0.25;
            double out = 1.0 - ((time * 0.06 + k * 0.37) % 1.0);
            double from = MechBodyShapes.PORT_IN + 2.2 * out;
            double to = from - 0.6;
            Vec3 a = chest.at(Math.cos(turn) * from, MechBeam.PORT.y + Math.sin(turn) * from, MechBeam.PORT.z + 0.2);
            Vec3 b = chest.at(Math.cos(turn + 0.35) * to, MechBeam.PORT.y + Math.sin(turn + 0.35) * to,
                    MechBeam.PORT.z + 0.2);
            painter.lightLine(a, b, 0.06, LanternPainter.HOT, Colors.alpha(0.8 * power * (1.0 - out * 0.5)));
        }
        if (!own) {
            painter.flare(port, 0.6 + 2.0 * power, power);
            painter.glowDisc(port, 1.2 + 1.2 * power, LanternPainter.GREEN, 0.5 * power, 0.15, (int) time);
        }
    }

    // Where a beam from `from` ends: past what the pilot's crosshair rests on till a block stops it, or, for the eye
    // beam, at the first creature in its way.
    private static Vec3 end(ClientLevel level, Entity pilot, Vec3 from, double range, boolean first) {
        Vec3 end = MechBeam.reach(level, from, MechBeam.aim(level, pilot, range), range, pilot);
        if (!first) {
            return end;
        }
        LivingEntity hit = MechBeam.first(level, pilot, from, end);
        return hit == null ? end : hit.getBoundingBox().getCenter();
    }
}
