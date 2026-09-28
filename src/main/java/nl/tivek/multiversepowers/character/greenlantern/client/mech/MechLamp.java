package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.fx.Spotlight;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;

// The lamp on the mech's chest: it swivels to where the mech looks and, in the dark, throws a beam and a patch of light
// on the ground and walls ahead. Each player's own game decides when it is on (ClientSettings.mechLamp).
final class MechLamp {
    private static final double HALF_ANGLE = 0.36;
    // It points a little below where the mech looks, so its light lands on the ground ahead.
    private static final double DOWN = 0.16;
    private static final double LENS = 0.44;
    private static final int RINGS = 6;
    private static final int SPOKES = 24;
    private static final int LIGHT = 0xE4FFE8;
    // On in the dark below this light level, off again above the other (so it does not flicker at dusk).
    private static final int DARK = 8;
    private static final int BRIGHT = 10;
    private static final Int2ObjectOpenHashMap<Spotlight> LAMPS = new Int2ObjectOpenHashMap<>();
    private static final IntOpenHashSet LIT = new IntOpenHashSet();

    private MechLamp() {
    }

    static Vec3 aim(MechPose pose, double t, boolean walking) {
        MechScript.Stage torso = pose.torso();
        Vec3 look = walking ? MechPainter.head(pose, t, -1.0, true).forward() : torso.ahead();
        return look.subtract(torso.up().scale(DOWN)).normalize();
    }

    private static Frame housing(MechPose pose, Vec3 aim) {
        return MechPainter.limb(pose.torso().point(MechBodyShapes.LAMP), aim, pose.torso().up());
    }

    // Once a tick for a walking mech, with the pose it has reached.
    static void tick(int id, Level level, MechPose pose) {
        int mode = ClientSettings.mechLamp();
        Vec3 aim = aim(pose, MechScript.SETTLED, true);
        Vec3 lens = housing(pose, aim).at(0.0, LENS, 0.0);
        int light = Spotlight.lightAt(level, BlockPos.containing(lens));
        if (light < DARK) {
            LIT.add(id);
        } else if (light > BRIGHT) {
            LIT.remove(id);
        }
        boolean on = mode == 2 || mode == 1 && LIT.contains(id);
        Spotlight spot = LAMPS.get(id);
        int step = ClientSettings.detailStep();
        if (spot == null || spot.spokes() != SPOKES / step) {
            if (!on) {
                return;
            }
            spot = new Spotlight(Math.max(2, RINGS / step), SPOKES / step);
            LAMPS.put(id, spot);
        }
        spot.cast(level, lens.add(aim.scale(0.1)), aim, HALF_ANGLE, ClientSettings.get(ClientSettings.MECH_LAMP_REACH),
                on);
    }

    static void drawHousing(LanternPainter painter, MechPose pose, double t, boolean walking, double apart,
            int seed) {
        MechParts.draw(painter, MechBodyShapes.LAMP_MOUNT, MechPainter.body(pose.torso()), 1.0, apart, seed);
        MechParts.draw(painter, MechBodyShapes.LAMP_HOUSING, housing(pose, aim(pose, t, walking)), 1.0, apart,
                seed + 1);
    }

    static void drawLight(LanternPainter painter, int id, MechPose pose, double t, float partialTick, boolean own) {
        Spotlight spot = LAMPS.get(id);
        double on = spot == null ? 0.0 : spot.strength(partialTick);
        Vec3 aim = aim(pose, t, true);
        Vec3 lens = housing(pose, aim).at(0.0, LENS + 0.02, 0.0);
        painter.flare(lens, 0.28 + 0.3 * on, (own ? 0.2 : 0.3) + (own ? 0.35 : 0.65) * on);
        if (spot != null && on > 0.0) {
            double bright = ClientSettings.get(ClientSettings.MECH_LAMP_BRIGHTNESS);
            spot.draw(painter, partialTick, LIGHT, 0.8 * bright);
            // From the cockpit the beam would lie across the view: its pilot sees only where it lands.
            boolean inside = own && Minecraft.getInstance().options.getCameraType().isFirstPerson();
            if (!inside) {
                spot.beam(painter, partialTick, LIGHT, 0.46, HALF_ANGLE, bright, aim);
            }
        }
    }

    static void forget(int id) {
        LAMPS.remove(id);
        LIT.remove(id);
    }

    static void clear() {
        LAMPS.clear();
        LIT.clear();
    }
}
