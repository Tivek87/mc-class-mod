package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.RingScan;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * The air strike as everyone sees it (see {@link AirStrike}). A pillar of light shoots out of the ring into the sky and
 * a big gunship grows out of the top of it, white-hot at first and cooling to the green of hard light, high over the
 * battlefield. It is solid hard light like every construct, and made in detail:
 * <ul>
 * <li>a long, round body with a radome nose, a windscreen and side windows burning bright, a crew door, paratroop doors
 * and rows of windows, bands of plating, a cargo ramp under its upswept tail, landing gear pods on its sides, blade
 * antennas and pitot tubes;</li>
 * <li>a high wing on top of it with flaps and ailerons, four engines in long nacelles slung under it, each with an air
 * intake, an exhaust stack and a big four-bladed propeller turning in front of it;</li>
 * <li>a tall fin with a rudder and a dorsal fillet, tailplanes with elevators, and the lantern emblem on its fin and on
 * top of its wings, with lights on its wingtips and its tail;</li>
 * <li>two miniguns on ball mounts in sponsons on its sides, that swing smoothly round to what they fire at, their six
 * barrels spinning and their muzzles flashing as they fire;</li>
 * <li>a bomb bay in its belly, whose two doors swing open on their hinges before a missile is lowered out and drops,
 * and swing shut behind it;</li>
 * <li>a sensor ball under its nose, that shines its scan down onto the ground in a cone of light.</li>
 * </ul>
 * Two jets fly with it (see {@link #jets}), sleek fighters with a bubble canopy, twin tails and a small missile under each
 * wing, that grow out of the ring's light beside it and race round it, banking into their turns. Then an engine bursts
 * into flame, its propeller dies and the plane shudders and struggles: its jets break away, their flames roaring, a cone
 * of mist forms round them as they break the sound barrier with a ring of light, and they are gone in a star of light,
 * breaking into solid pieces. The plane's nose drops: it plunges into the ground, rolling over, trailing fire and light,
 * and crashes in a blast like a small sun: a flash, a fireball rising on a stem of light into a mushroom cloud, a ring of
 * light racing out round its middle, a shell of light and rings running out over the ground, while the plane breaks into
 * solid pieces that are flung far. Only that blast is see-through: it is light, not a construct. If its maker stops being
 * Green Lantern the plane and its jets break apart in the air (see {@link #broken}).
 */
public final class PlanePainter {
    // How long the plane takes to grow out of the light, from when, in ticks after the call; and how long it takes to
    // break up once it has crashed (or its maker lets go of it).
    private static final double GROW_FROM = 6.0;
    private static final double GROW_TICKS = 30.0;
    private static final double BREAK_TICKS = 40.0;
    // How far its pieces are flung when it crashes, and when it breaks up in the air.
    private static final double CRASH_FLING = 10.0;
    private static final double AIR_FLING = 5.0;
    // The pillar of light: how thick it is, and until when it pours up into the plane.
    private static final double PILLAR_THICK = 3.6;
    private static final double PILLAR_UNTIL = PlanePath.FORM + 4.0;
    // How fast the propellers turn at full speed, in radians per tick, and how long the one that bursts takes to run
    // down, in ticks.
    private static final double PROP_SPIN = 0.55;
    private static final double PROP_DIES = 12.0;
    // How long the tip of a propeller blade is from the hub, in blocks at scale 1.
    private static final double PROP_RADIUS = 3.25;
    // How far to its right the hubs of its four propellers are: the left outer one, the left inner one, the right inner
    // one (the one that bursts) and the right outer one.
    private static final double[] ENGINES = { -AirStrike.ENGINE_OUTER_X, -AirStrike.ENGINE_X, AirStrike.ENGINE_X,
            AirStrike.ENGINE_OUTER_X };
    // How fast the barrels of a minigun spin while it fires, in radians per tick, and how long they take to run down.
    private static final double BARREL_SPIN = 1.1;
    private static final double BARREL_DIES = 18.0;
    // How long a minigun takes to swing over to where it really points when news of where it was told to point came in
    // late, and how long it takes to level its roll out again after pointing straight down (in ticks, most of the way).
    private static final double GUN_CATCHES_UP = 2.5;
    private static final double GUN_LEVELS = 8.0;
    /** How long the blast of a missile goes on, in ticks. */
    public static final int BLAST_TICKS = 30;
    /** How long a missile takes to break into pieces once it has struck, in ticks. */
    public static final double MISSILE_BREAKS = 12.0;
    // How long a round that struck nothing takes to break into crumbs once it is out of reach, in ticks.
    private static final double SLUG_BREAKS = 4.0;
    // How fast a jet's pieces fly on once it is gone, and how soon they slow down (ticks' worth of its speed).
    private static final double JET_FLIES_ON = 5.0;
    // A big missile's motor fires this many ticks after it dropped at the earliest: until then it only falls.
    private static final int EARLIEST_IGNITION = 4;
    // The blast of the crash: how far its shell of light races out, how high its fireball climbs, how big it gets, and how
    // far the ring of light round its middle races out.
    private static final double SHELL = 46.0;
    private static final double FIREBALL_HIGH = 40.0;
    private static final double FIREBALL = 18.5;
    private static final double COLLAR = 64.0;
    // The bomb bay: how far round the body its doors reach either way from the bottom of its belly, in degrees, and how
    // far they swing open.
    private static final double BAY_HALF_ANGLE = 25.0;
    private static final double DOOR_OPEN = Math.toRadians(95.0);
    // Where a missile waits in the bay, how high in the body, before it is lowered out.
    private static final double IN_BAY_Y = -1.95;
    // A jet's size, next to the blocks its model is made in; how long its pieces take to fly apart as it goes; and how
    // long the star of light it goes in shines, in ticks.
    private static final double JET_SCALE = 1.0;
    private static final double JET_BREAKS = 9.0;
    private static final double STAR_TICKS = 18.0;

    /**
     * The body of the plane, as sections along z (see {@link Mesh#loft}): z, half width, half height, and how high its
     * middle is. A radome nose, the cockpit, the long cabin, and the tail sweeping up over the cargo ramp.
     */
    private static final double[][] HULL = { { 22.4, 0.0, 0.0, -0.55 }, { 22.0, 0.78, 0.66, -0.5 },
            { 21.2, 1.48, 1.28, -0.38 }, { 20.0, 2.08, 1.88, -0.18 }, { 18.4, 2.52, 2.32, 0.0 },
            { 16.2, 2.8, 2.6, 0.1 }, { 13.0, 2.92, 2.72, 0.12 }, { 6.0, 2.96, 2.74, 0.12 },
            { -4.0, 2.96, 2.74, 0.12 }, { -9.0, 2.9, 2.64, 0.3 }, { -13.0, 2.62, 2.22, 0.85 },
            { -16.5, 2.12, 1.62, 1.45 }, { -19.5, 1.46, 1.06, 1.95 }, { -21.8, 0.76, 0.56, 2.3 },
            { -22.6, 0.0, 0.0, 2.4 } };

    /** The plane itself, everything on it that does not move by itself: at scale 1, x to its right, y up, z ahead. */
    private static final ConstructPainter.Shape BODY = ConstructPainter.Shape.of(body());
    /** One propeller round its hub, turning about z: its spinner and four twisted blades. */
    private static final ConstructPainter.Shape PROPELLER = ConstructPainter.Shape.of(propeller());
    /** A minigun round the ball it turns on, pointing along z: the ball, the housing, its motor and its ammunition. */
    private static final ConstructPainter.Shape GUN = ConstructPainter.Shape.of(gun());
    /** The six barrels of a minigun, with their clamps and muzzle ring, turning about z. */
    private static final ConstructPainter.Shape BARRELS = ConstructPainter.Shape.of(barrels());
    /**
     * A homing missile of hard light along z, its nose ahead: 4.6 blocks long at scale 1, drawn {@link #MISSILE_SCALE}
     * times that, on its rail and in the air alike.
     */
    private static final ConstructPainter.Shape MISSILE = ConstructPainter.Shape.of(missile(0.36));
    private static final double MISSILE_TAIL = -2.3;
    static final double MISSILE_SCALE = AirStrike.MISSILE_SCALE;
    /** How big a jet's small missile is drawn, next to the model of a missile. */
    static final double SMALL_MISSILE_SCALE = AirStrike.SMALL_MISSILE_SCALE;
    /** A round from a minigun: a slug of hard light along z, its nose ahead, drawn {@link #SLUG_SCALE} times as big. */
    private static final ConstructPainter.Shape SLUG = ConstructPainter.Shape.of(Mesh.lathe(8, 1.35, 0.0, -0.45, 0.09,
            -0.4, 0.11, 0.15, 0.07, 0.4, 0.0, 0.5).alongZ());
    private static final double SLUG_SCALE = 1.6;
    /** One door of the bomb bay, the right one, on its hinge along the body: the left one is its mirror image. */
    private static final ConstructPainter.Shape BAY_DOOR = ConstructPainter.Shape.of(bayDoor());
    /** The inside of the bomb bay, deep in the belly, that shows while its doors are open. */
    private static final ConstructPainter.Shape BAY_INSIDE = ConstructPainter.Shape.of(bayInside());
    /** A jet: at scale 1 some 13.5 blocks long and 11 wide, x to its right, y up, z ahead. */
    private static final ConstructPainter.Shape JET = ConstructPainter.Shape.of(jet());
    // How much light from within the plane's hard light gets on top of the sky's, so its belly high over you still reads.
    private static final double GLOWS = 0.3;

    // The drone of every plane in the air, by the id of its construct.
    private static final Map<Integer, PlaneSound> SOUNDS = new HashMap<>();
    // How the miniguns of each player's plane swing and spin, by the id of the player.
    private static final Map<Integer, Guns> GUNS = new HashMap<>();

    private PlanePainter() {
    }

    // ---- The shapes ----

    /** The body's section at {@code z}, worked out between the sections of {@link #HULL}. */
    private static double[] hull(double z) {
        for (int i = 0; i + 1 < HULL.length; i++) {
            double[] a = HULL[i];
            double[] b = HULL[i + 1];
            if (z <= a[0] && z >= b[0]) {
                double t = (a[0] - z) / (a[0] - b[0]);
                return new double[] { z, Mth.lerp(t, a[1], b[1]), Mth.lerp(t, a[2], b[2]), Mth.lerp(t, a[3], b[3]) };
            }
        }
        double[] end = z > HULL[0][0] ? HULL[0] : HULL[HULL.length - 1];
        return new double[] { z, end[1], end[2], end[3] };
    }

    /** A plate on the body (see {@link Mesh#panel}) from {@code z0} to {@code z1}, between two angles in degrees. */
    private static Mesh plate(double z0, double z1, double from, double to, double thick, double bright) {
        int pieces = Math.max(1, (int) Math.ceil(Math.abs(z1 - z0) / 1.2));
        double[][] sections = new double[pieces + 1][];
        for (int i = 0; i <= pieces; i++) {
            sections[i] = hull(Mth.lerp((double) i / pieces, z1, z0));
        }
        int steps = Math.max(2, (int) Math.ceil(Math.abs(to - from) / 9.0));
        return Mesh.panel(steps, bright, Math.toRadians(from), Math.toRadians(to), thick, sections);
    }

    /** The same plate on both sides of the body: as given, and mirrored to the other side. */
    private static void pair(List<Mesh> parts, Mesh right) {
        parts.add(right);
        parts.add(right.mirrored());
    }

    private static Mesh[] body() {
        List<Mesh> parts = new ArrayList<>();
        // The body, and the fairing on its back where the wing sits.
        parts.add(Mesh.loft(32, 1.0, HULL));
        parts.add(Mesh.loft(20, 1.0, new double[] { 6.8, 0.0, 0.0, 2.7 }, new double[] { 5.4, 1.5, 0.5, 2.72 },
                new double[] { -4.6, 1.5, 0.5, 2.72 }, new double[] { -6.8, 0.0, 0.0, 2.7 }));
        // The windscreen and the side windows of the cockpit, burning bright, like glass full of light.
        parts.add(plate(18.5, 20.1, 58.0, 122.0, 0.05, 1.75));
        pair(parts, plate(17.2, 18.4, 26.0, 50.0, 0.05, 1.6));
        // A seam round the radome, and bands of plating round the body.
        parts.add(plate(20.55, 20.75, 0.0, 360.0, 0.025, 1.35));
        for (double z : new double[] { 14.5, 4.0, -6.0, -11.5 }) {
            parts.add(plate(z - 0.12, z + 0.12, 0.0, 360.0, 0.03, 1.2));
        }
        // A row of windows along each side, the crew door up front on the left, the paratroop doors at the back, and the
        // cargo ramp under the tail.
        for (double z : new double[] { 11.6, 8.2, 4.8, 1.4, -2.0 }) {
            pair(parts, plate(z, z + 0.75, 6.0, 16.0, 0.04, 1.5));
        }
        parts.add(plate(14.8, 16.1, 162.0, 202.0, 0.03, 1.2));
        pair(parts, plate(-9.6, -8.0, -14.0, 22.0, 0.03, 1.22));
        parts.add(plate(-19.0, -11.5, 232.0, 308.0, 0.035, 1.18));
        // The wings, high on top of the body, each with a flap and an aileron along its trailing edge.
        pair(parts, Mesh.wing(27.0, -3.6, 4.4, -1.8, 2.2, 0.5, 1.15, 0.42, 1.0).moved(1.0, 2.95, 0.0));
        pair(parts, Mesh.wing(13.0, -4.55, -3.5, -3.62, -2.62, 0.24, 0.26, 0.18, 1.08).moved(3.0, 2.86, 0.0));
        pair(parts, Mesh.wing(9.5, -3.4, -2.52, -2.72, -1.9, 0.18, 0.2, 0.14, 1.08).moved(17.0, 3.25, 0.0));
        // Four engines slung under the wings, each with its intake under the propeller and its exhaust stack outboard.
        for (double x : new double[] { AirStrike.ENGINE_X, AirStrike.ENGINE_OUTER_X }) {
            pair(parts, Mesh.loft(18, 1.0, new double[] { 8.0, 0.95, 1.0, 0.0 },
                    new double[] { 7.2, 1.08, 1.12, -0.05 }, new double[] { 4.2, 1.1, 1.22, -0.12 },
                    new double[] { 0.5, 1.0, 1.12, -0.1 }, new double[] { -2.6, 0.78, 0.85, 0.05 },
                    new double[] { -4.8, 0.3, 0.32, 0.18 }, new double[] { -5.3, 0.0, 0.0, 0.2 })
                    .moved(x, AirStrike.ENGINE_Y, 0.0));
            pair(parts, Mesh.box(-0.42, -1.42, 5.0, 0.42, -0.95, 7.7, 1.15).moved(x, AirStrike.ENGINE_Y, 0.0));
            pair(parts, Mesh.tube(false, 8, 0.2, 1.1, new Vec3(x + 0.95, AirStrike.ENGINE_Y + 0.35, 1.6),
                    new Vec3(x + 1.2, AirStrike.ENGINE_Y + 0.42, -0.7)));
        }
        // The tail: a tall fin with its rudder and a fillet running forward from it, and the tailplanes with their
        // elevators.
        parts.add(Mesh.wing(10.5, -22.2, -13.5, -23.4, -19.6, 0.0, 0.7, 0.35, 1.0).turned(0.0, 0.0, 1.0, 90.0)
                .moved(0.0, 2.6, 0.0));
        parts.add(Mesh.wing(9.8, -23.3, -22.15, -24.3, -23.35, 0.0, 0.3, 0.2, 1.08).turned(0.0, 0.0, 1.0, 90.0)
                .moved(0.0, 3.0, 0.0));
        parts.add(Mesh.wing(2.3, -14.0, -7.0, -14.2, -13.3, 0.0, 0.45, 0.3, 1.0).turned(0.0, 0.0, 1.0, 90.0)
                .moved(0.0, 2.7, 0.0));
        pair(parts, Mesh.wing(9.0, -21.6, -16.0, -21.2, -18.8, 0.3, 0.55, 0.25, 1.0).moved(0.9, 2.2, 0.0));
        pair(parts, Mesh.wing(8.4, -22.5, -21.55, -21.95, -21.15, 0.28, 0.25, 0.15, 1.08).moved(1.0, 2.2, 0.0));
        // The landing gear pods on its lower sides, each with the seam of its doors.
        pair(parts, Mesh.loft(16, 1.0, new double[] { 6.6, 0.0, 0.0, 0.0 }, new double[] { 5.8, 0.82, 0.86, 0.0 },
                new double[] { -3.4, 0.86, 0.9, 0.0 }, new double[] { -4.8, 0.0, 0.0, 0.0 }).moved(2.55, -1.75, 0.0));
        pair(parts, Mesh.box(-0.05, -0.02, -3.0, 0.05, 0.05, 5.4, 1.45).moved(2.62, -2.62, 0.0));
        // The sponsons of the miniguns: a long blister on each side of the belly, a stub reaching out of it and the big
        // round collar the gun's ball turns in, with a glowing ring round its lip.
        pair(parts, Mesh.loft(18, 1.0, new double[] { 15.4, 0.0, 0.0, 0.0 }, new double[] { 14.2, 0.7, 0.8, 0.0 },
                new double[] { 11.8, 0.95, 1.05, 0.0 }, new double[] { 7.6, 0.95, 1.05, 0.0 },
                new double[] { 5.6, 0.6, 0.7, 0.0 }, new double[] { 4.4, 0.0, 0.0, 0.0 })
                .moved(2.45, AirStrike.GUN_Y + 0.35, 0.0));
        pair(parts, Mesh.cylinder(20, 1.22, 0.0, 0.9, 1.08).alongX().moved(AirStrike.GUN_X - 1.2, AirStrike.GUN_Y,
                AirStrike.GUN_Z));
        pair(parts, Mesh.torus(24, 6, 1.22, 0.11, 1.55).alongX().moved(AirStrike.GUN_X - 0.3, AirStrike.GUN_Y,
                AirStrike.GUN_Z));
        // The bomb bay in its belly: a glowing rim round its hatch, at its front and back and along the hinges of its
        // two doors.
        double bayBack = AirStrike.BAY_Z - AirStrike.BAY_LENGTH * 0.5;
        double bayFront = AirStrike.BAY_Z + AirStrike.BAY_LENGTH * 0.5;
        for (double z : new double[] { bayBack - 0.18, bayFront }) {
            parts.add(plate(z, z + 0.18, 270.0 - BAY_HALF_ANGLE - 3.0, 270.0 + BAY_HALF_ANGLE + 3.0, 0.07, 1.55));
        }
        pair(parts, plate(bayBack, bayFront, 270.0 + BAY_HALF_ANGLE, 270.0 + BAY_HALF_ANGLE + 3.0, 0.07, 1.55));
        // The ring's light running through it: glowing strips along the leading edges of its wings and tailplanes, a
        // spine along its back and two seams along its belly, broken off round the hatch.
        pair(parts, Mesh.tube(false, 6, 0.13, 1.7, new Vec3(3.0, 2.99, 4.32), new Vec3(28.0, 3.45, 2.24)));
        pair(parts, Mesh.tube(false, 5, 0.09, 1.65, new Vec3(1.3, 2.2, -16.0), new Vec3(9.9, 2.5, -18.8)));
        parts.add(plate(-13.5, 17.2, 87.0, 93.0, 0.035, 1.55));
        pair(parts, plate(-11.5, bayBack - 0.3, 256.0, 260.0, 0.035, 1.6));
        pair(parts, plate(bayFront + 0.3, 17.0, 256.0, 260.0, 0.035, 1.6));
        // Flap track fairings: slim pods under the trailing edge of each wing, where the flaps run out on their tracks.
        for (double x : new double[] { 5.0, 9.5, 14.2, 19.5, 24.5 }) {
            double rise = 2.95 + 0.5 * (x - 1.0) / 27.0;
            pair(parts, Mesh.loft(10, 1.02, new double[] { -6.0 + 0.07 * x, 0.0, 0.0, 0.0 },
                    new double[] { -5.3 + 0.07 * x, 0.16, 0.2, 0.0 }, new double[] { -3.2 + 0.07 * x, 0.25, 0.32, 0.0 },
                    new double[] { -0.8 + 0.07 * x, 0.2, 0.24, 0.05 }, new double[] { 0.4 + 0.07 * x, 0.0, 0.0, 0.1 })
                    .moved(x, rise - 0.45, 0.0));
        }
        // Landing lights under its nose and at the roots of its wings, burning bright.
        pair(parts, Mesh.ball(10, 6, 0.26, 2.0).moved(0.85, -2.2, 17.0));
        pair(parts, Mesh.ball(10, 6, 0.3, 2.0).moved(4.2, 2.62, 3.4));
        // The sensor ball under the nose, on its mount, with its lens looking forward and down.
        parts.add(Mesh.cylinder(14, 0.36, -0.55, 0.05, 1.0).moved(0.0, AirStrike.SENSOR_Y + 0.72, AirStrike.SENSOR_Z));
        parts.add(Mesh.ball(16, 10, 0.55, 1.1).moved(0.0, AirStrike.SENSOR_Y, AirStrike.SENSOR_Z));
        parts.add(Mesh.cylinder(12, 0.26, 0.0, 0.08, 1.8).pointing(0.0, -0.55, 0.84)
                .moved(0.0, AirStrike.SENSOR_Y - 0.28, AirStrike.SENSOR_Z + 0.42));
        // Blade antennas on its back and belly, and pitot tubes on its nose.
        for (double z : new double[] { 9.0, -2.5 }) {
            parts.add(Mesh.wing(1.0, z - 0.45, z + 0.35, z - 0.6, z - 0.1, 0.0, 0.12, 0.06, 1.2)
                    .turned(0.0, 0.0, 1.0, 90.0).moved(0.0, 2.8, 0.0));
        }
        parts.add(Mesh.wing(0.8, 5.4, 6.1, 5.3, 5.7, 0.0, 0.1, 0.05, 1.2).turned(0.0, 0.0, 1.0, -90.0)
                .moved(0.0, -2.55, 0.0));
        pair(parts, Mesh.cylinder(6, 0.05, 0.0, 1.3, 1.3).alongZ().moved(1.45, 0.35, 19.6));
        // Lights on the wingtips and the tail.
        pair(parts, Mesh.ball(8, 6, 0.24, 1.9).moved(28.0, 3.45, 0.4));
        parts.add(Mesh.ball(8, 6, 0.22, 1.9).moved(0.0, 2.45, -22.75));
        // The lantern emblem: on both sides of the fin, its bars along the fin, and on top of each wing.
        pair(parts, emblem(1.25).turned(0.0, 1.0, 0.0, 90.0).turned(0.0, 0.0, 1.0, -90.0).moved(0.4, 7.4, -20.3));
        pair(parts, emblem(1.6).moved(21.0, 3.62, 0.3));
        // ... and under each wing, big, where you see it from the ground.
        pair(parts, emblem(1.9).moved(19.0, 2.8, 1.0));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * The lantern emblem as a roundel lying flat round y: a ring with a bar over and under its middle, the way the ring
     * marks what it makes.
     */
    private static Mesh emblem(double radius) {
        double bar = radius * 0.24;
        return Mesh.merged(Mesh.torus(24, 5, radius, radius * 0.12, 1.75),
                Mesh.box(-radius, -0.07, bar * 0.9, radius, 0.1, bar * 1.6, 1.75),
                Mesh.box(-radius, -0.07, -bar * 1.6, radius, 0.1, -bar * 0.9, 1.75));
    }

    private static Mesh[] propeller() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.lathe(16, 1.2, 0.0, 0.0, 0.58, 0.0, 0.56, 0.45, 0.44, 0.95, 0.24, 1.32, 0.0, 1.5).alongZ());
        for (int k = 0; k < 4; k++) {
            // Each blade twisted a little along its length against the air, and turned a quarter from the last.
            Mesh blade = Mesh.wing(PROP_RADIUS - 0.5, -0.34, 0.3, -0.2, 0.2, 0.0, 0.16, 0.07, 1.12)
                    .turned(1.0, 0.0, 0.0, 24.0).moved(0.5, 0.0, 0.55);
            parts.add(blade.turned(0.0, 0.0, 1.0, 90.0 * k));
        }
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A minigun round the ball it turns on, pointing along z: the ball, the receiver with a glowing sight on top, the
     * cooling jacket round the root of the barrels, the motor behind it, a big ammunition drum on its left side with the
     * feed chute into the receiver, and two spade grips at its back.
     */
    private static Mesh[] gun() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.ball(18, 10, 1.0, 1.0));
        parts.add(Mesh.box(-0.66, -0.62, -2.1, 0.66, 0.62, 1.05, 1.0));
        parts.add(Mesh.cylinder(14, 0.5, -2.1, 1.05, 1.04).alongZ().moved(0.0, 0.5, 0.0));
        parts.add(Mesh.box(-0.12, 1.0, -0.9, 0.12, 1.28, 0.6, 1.7));
        parts.add(Mesh.cylinder(18, 0.66, 0.9, 2.3, 1.02).alongZ());
        parts.add(Mesh.torus(20, 5, 0.66, 0.07, 1.5).alongZ().moved(0.0, 0.0, 2.3));
        parts.add(Mesh.cylinder(14, 0.44, -2.9, -2.1, 0.96).alongZ());
        parts.add(Mesh.torus(16, 4, 0.44, 0.06, 1.4).alongZ().moved(0.0, 0.0, -2.9));
        parts.add(Mesh.cylinder(20, 0.8, -0.45, 0.45, 1.0).alongX().moved(-1.25, -0.25, -0.9));
        parts.add(Mesh.torus(20, 5, 0.8, 0.06, 1.5).alongX().moved(-0.8, -0.25, -0.9));
        parts.add(Mesh.tube(false, 8, 0.16, 1.05, new Vec3(-1.0, 0.45, -0.7), new Vec3(-0.75, 0.75, -0.45),
                new Vec3(-0.45, 0.62, -0.2)));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.tube(false, 6, 0.08, 1.1, new Vec3(0.3 * side, 0.2, -2.1), new Vec3(0.34 * side, 0.1, -2.7),
                    new Vec3(0.34 * side, -0.35, -2.8)));
        }
        return parts.toArray(Mesh[]::new);
    }

    /** The six barrels of a minigun, with the clamps that hold them together and a muzzle brake, turning about z. */
    private static Mesh[] barrels() {
        List<Mesh> parts = new ArrayList<>();
        for (int k = 0; k < 6; k++) {
            double angle = Math.PI * 2.0 * k / 6.0;
            parts.add(Mesh.cylinder(8, 0.13, 1.0, AirStrike.GUN_LENGTH - 0.05, 1.05).alongZ()
                    .moved(0.37 * Math.cos(angle), 0.37 * Math.sin(angle), 0.0));
        }
        parts.add(Mesh.cylinder(10, 0.13, 1.0, AirStrike.GUN_LENGTH - 0.3, 1.0).alongZ());
        for (double z : new double[] { 3.0, 4.6 }) {
            parts.add(Mesh.torus(20, 5, 0.52, 0.08, 1.2).alongZ().moved(0.0, 0.0, z));
        }
        parts.add(Mesh.cylinder(20, 0.6, AirStrike.GUN_LENGTH - 0.45, AirStrike.GUN_LENGTH, 1.1).alongZ());
        parts.add(Mesh.torus(20, 5, 0.6, 0.09, 1.5).alongZ().moved(0.0, 0.0, AirStrike.GUN_LENGTH));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A missile along z, {@code r} thick: its bright seeker in the nose, canards behind it, a band round its body and
     * four fins at its tail.
     */
    private static Mesh[] missile(double r) {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.loft(14, 1.1, new double[] { 2.3, 0.0, 0.0, 0.0 }, new double[] { 1.95, r * 0.55, r * 0.55, 0.0 },
                new double[] { 1.5, r * 0.9, r * 0.9, 0.0 }, new double[] { 1.15, r, r, 0.0 },
                new double[] { -2.05, r, r, 0.0 }, new double[] { -2.3, r * 0.8, r * 0.8, 0.0 }));
        parts.add(Mesh.ball(10, 6, r * 0.42, 1.85).moved(0.0, 0.0, 2.12));
        parts.add(Mesh.torus(16, 4, r + 0.02, 0.03, 1.4).alongZ().moved(0.0, 0.0, -0.6));
        for (int k = 0; k < 4; k++) {
            parts.add(Mesh.wing(r * 1.7, -2.3, -1.2, -2.3, -1.85, 0.0, 0.08, 0.04, 1.1).moved(r * 0.7, 0.0, 0.0)
                    .turned(0.0, 0.0, 1.0, 90.0 * k));
            parts.add(Mesh.wing(r * 0.9, 0.85, 1.35, 0.95, 1.15, 0.0, 0.05, 0.03, 1.1).moved(r * 0.7, 0.0, 0.0)
                    .turned(0.0, 0.0, 1.0, 45.0 + 90.0 * k));
        }
        return parts.toArray(Mesh[]::new);
    }

    /**
     * The right door of the bomb bay: a curved plate from the middle of the belly out to its hinge, stiffened by three
     * glowing ribs across it and a glowing lip along its free edge.
     */
    private static Mesh[] bayDoor() {
        double back = AirStrike.BAY_Z - AirStrike.BAY_LENGTH * 0.5 + 0.05;
        double front = AirStrike.BAY_Z + AirStrike.BAY_LENGTH * 0.5 - 0.05;
        List<Mesh> parts = new ArrayList<>();
        parts.add(plate(back, front, 270.0, 270.0 + BAY_HALF_ANGLE, 0.06, 1.08));
        for (double z : new double[] { back + 1.6, AirStrike.BAY_Z, front - 1.6 }) {
            parts.add(plate(z - 0.08, z + 0.08, 271.0, 270.0 + BAY_HALF_ANGLE - 1.0, 0.085, 1.45));
        }
        parts.add(plate(back, front, 270.2, 271.6, 0.08, 1.6));
        return parts.toArray(Mesh[]::new);
    }

    /** The inside of the bomb bay: a dark hollow under the doors, and the glowing clamps a missile hangs from. */
    private static Mesh[] bayInside() {
        double back = AirStrike.BAY_Z - AirStrike.BAY_LENGTH * 0.5;
        double front = AirStrike.BAY_Z + AirStrike.BAY_LENGTH * 0.5;
        List<Mesh> parts = new ArrayList<>();
        parts.add(plate(back, front, 270.0 - BAY_HALF_ANGLE, 270.0 + BAY_HALF_ANGLE, 0.012, 0.32));
        for (double z : new double[] { AirStrike.BAY_Z - 2.2, AirStrike.BAY_Z + 2.2 }) {
            parts.add(Mesh.box(-0.5, -1.4, z - 0.14, 0.5, -1.2, z + 0.14, 1.5));
        }
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A jet: a long, slim body with a pointed nose and a seam round it, a bubble canopy of bright light, two air intakes
     * with glowing lips, swept wings with glowing leading edges, the lantern emblem and a light on each tip, a pylon under
     * each wing, two tails canted outward, tailplanes, two small fins under its tail, a glowing spine along its back and
     * two exhaust nozzles with a ring of light in each.
     */
    private static Mesh[] jet() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.loft(20, 1.0, new double[] { 7.4, 0.0, 0.0, -0.05 }, new double[] { 6.9, 0.2, 0.18, -0.04 },
                new double[] { 6.0, 0.42, 0.38, 0.0 }, new double[] { 4.6, 0.62, 0.55, 0.05 },
                new double[] { 3.0, 0.8, 0.62, 0.08 }, new double[] { 1.2, 1.1, 0.62, 0.02 },
                new double[] { -1.8, 1.25, 0.6, 0.0 }, new double[] { -4.2, 1.2, 0.55, 0.02 },
                new double[] { -5.5, 1.05, 0.48, 0.05 }, new double[] { -5.9, 0.0, 0.0, 0.05 }));
        parts.add(Mesh.torus(20, 4, 0.47, 0.035, 1.45).alongZ().scaled(1.0, 0.9, 1.0).moved(0.0, 0.0, 5.7));
        // The canopy, like glass full of light, with a frame across it.
        parts.add(Mesh.loft(16, 1.8, new double[] { 5.2, 0.0, 0.0, 0.42 }, new double[] { 4.6, 0.28, 0.26, 0.47 },
                new double[] { 3.4, 0.4, 0.36, 0.52 }, new double[] { 2.0, 0.34, 0.3, 0.5 },
                new double[] { 1.2, 0.0, 0.0, 0.45 }));
        parts.add(Mesh.tube(false, 6, 0.045, 1.25, new Vec3(-0.38, 0.5, 3.2), new Vec3(0.0, 0.9, 3.25),
                new Vec3(0.38, 0.5, 3.2)));
        // The intakes on its sides, each with a glowing lip.
        pair(parts, Mesh.loft(10, 1.0, new double[] { 3.2, 0.0, 0.0, 0.0 }, new double[] { 3.1, 0.34, 0.4, 0.0 },
                new double[] { 0.5, 0.36, 0.42, 0.0 }, new double[] { -1.2, 0.0, 0.0, 0.0 }).moved(1.05, -0.15, 0.0));
        pair(parts, Mesh.torus(12, 4, 0.34, 0.05, 1.7).alongZ().scaled(1.0, 1.18, 1.0).moved(1.05, -0.15, 3.12));
        // The wings, their glowing leading edges, the emblem on each, the lights on their tips and a pylon under each.
        pair(parts, Mesh.wing(4.4, -3.8, 1.6, -4.4, -2.9, -0.15, 0.26, 0.08, 1.0).moved(1.0, -0.05, 0.0));
        pair(parts, Mesh.tube(false, 5, 0.06, 1.7, new Vec3(1.1, 0.0, 1.55), new Vec3(5.35, -0.18, -2.9)));
        pair(parts, emblem(0.7).moved(3.0, 0.08, -1.7));
        pair(parts, Mesh.ball(8, 6, 0.14, 1.9).moved(5.42, -0.2, -3.6));
        pair(parts, Mesh.box(-0.07, -0.34, -0.9, 0.07, -0.08, 0.8, 1.05).moved(AirStrike.PYLON_X, 0.0, 0.0));
        // The tails, canted outward, with glowing leading edges, and the tailplanes.
        pair(parts, Mesh.wing(2.6, -5.6, -3.4, -6.0, -5.0, 0.0, 0.2, 0.07, 1.0).turned(0.0, 0.0, 1.0, 70.0)
                .moved(0.75, 0.35, 0.0));
        pair(parts, Mesh.tube(false, 5, 0.05, 1.65, new Vec3(0.76, 0.4, -3.45), new Vec3(1.63, 2.78, -5.0)));
        pair(parts, Mesh.wing(2.3, -6.1, -4.4, -6.3, -5.6, 0.0, 0.16, 0.06, 1.0).moved(1.05, -0.05, 0.0));
        pair(parts, Mesh.wing(0.7, -5.2, -3.9, -5.4, -4.9, 0.0, 0.1, 0.05, 1.0).turned(0.0, 0.0, 1.0, -80.0)
                .moved(0.7, -0.45, 0.0));
        parts.add(Mesh.tube(false, 5, 0.05, 1.6, new Vec3(0.0, 0.62, 1.0), new Vec3(0.0, 0.58, -4.5)));
        // The nozzles, each with a ring of light deep inside it.
        pair(parts, Mesh.lathe(16, 1.05, 0.0, -0.9, 0.46, -0.9, 0.5, -0.2, 0.44, 0.35, 0.0, 0.35).alongZ()
                .moved(0.55, 0.0, -5.6));
        pair(parts, Mesh.torus(16, 4, 0.38, 0.06, 2.0).alongZ().moved(0.55, 0.0, -6.46));
        return parts.toArray(Mesh[]::new);
    }

    // ---- The plane ----

    /** The way the plane of this air strike flies, from what the server said about it. */
    public static PlanePath path(ConstructPayload plane) {
        return new PlanePath(plane.center(), plane.facing(), plane.size(), Math.round(plane.charge()),
                plane.variant() / 100.0);
    }

    /**
     * One air strike.
     *
     * @param clock ticks since it was called, by the client's own clock
     * @param ring  where its maker's ring is, or null when he is out of sight
     */
    public static void draw(LanternPainter painter, int id, ConstructPayload plane, double clock, @Nullable Vec3 ring,
            float partialTick) {
        PlanePath path = path(plane);
        double crash = path.crashTick();
        double t = Math.min(clock, crash);
        sound(id, path, t, clock < crash ? path.down(t) : -1.0);
        if (clock < crash) {
            flying(painter, id, plane.owner(), path, clock, ring, partialTick);
            dropping(painter, plane.owner(), path, clock, partialTick);
            jets(painter, plane.owner(), path, clock, ring, partialTick);
        } else {
            crashed(painter, plane.owner(), path, clock - crash);
        }
    }

    /** How far the plane has grown out of the light, 0 to a hair over 1. */
    private static double grown(double t) {
        return Ease.backOut((t - GROW_FROM) / GROW_TICKS);
    }

    /** Where the plane is and how it is turned at {@code t}, at the size it has grown to. */
    private static ConstructPainter.Frame frame(PlanePath path, double t, double scale) {
        Vec3[] axes = path.axes(t);
        return new ConstructPainter.Frame(path.at(t), axes[0], axes[1], axes[2], scale);
    }

    private static void flying(LanternPainter painter, int id, int owner, PlanePath path, double t,
            @Nullable Vec3 ring, float partialTick) {
        double grown = grown(t);
        double down = path.down(t);
        ConstructPainter.Frame frame = frame(path, t, Math.max(grown, 1.0E-3));
        // The pillar of light pours up out of the ring into it while it takes shape; after that a thread of the ring's
        // light keeps hanging on it, as on every construct, and gives out as the plane struggles, before its nose
        // drops.
        if (ring != null) {
            if (t < PILLAR_UNTIL) {
                double fade = 1.0 - Ease.smooth((t - PlanePath.FORM) / 4.0);
                painter.beamOfLight(ring, path.at(t), fade, t, PILLAR_THICK);
            } else {
                double holds = 1.0 - Ease.smooth(path.failing(t) * 1.25);
                if (holds > 0.01) {
                    painter.beam(ring, frame.at(0.0, -2.6, 2.0), 0.55 * holds, 2.0);
                }
            }
        }
        // Its scan and the trail of its burning engine reach far out from it: they show even with the plane itself out
        // of view.
        scanCone(painter, owner, frame, partialTick);
        if (t >= path.failTick()) {
            burning(painter, path, frame, t);
        }
        if (grown <= 0.01 || !painter.visible(frame.center(), 40.0 * grown)) {
            return;
        }
        // White-hot as it grows out of the light, cooling to green; plunging down, its hull flickers, more and more.
        double flicker = Ease.smooth(down / 0.15)
                * (0.25 + 0.45 * down * Math.max(0.0, Math.sin(t * 1.7) * Math.sin(t * 0.63 + 2.0)));
        double hot = 0.6 * (1.0 - Ease.smooth((t - GROW_FROM) / GROW_TICKS));
        painter.glare(Math.max(hot, flicker));
        painter.ambient(GLOWS);
        painter.shape(BODY, frame, 1.0, 1.0 + 0.3 * flicker);
        propellers(painter, path, frame, t, grown);
        guns(painter, id, owner, path, frame, t, grown);
        hatch(painter, path, frame, t);
        painter.ambient(0.0);
        painter.glare(0.0);
        lights(painter, frame, t, down);
        engines(painter, path, frame, t, grown);
    }

    /** How many ticks there are between two missiles out of the hatch, as the world's settings have it. */
    private static int missileEvery() {
        CharacterAbility strike = GameCharacter.GREEN_LANTERN.byName("air_strike");
        return Math.max(4, strike == null ? 40 : strike.intValue("missileTicks"));
    }

    /**
     * The bomb bay: its two doors swing open on their hinges (and the dark hollow behind them shows) before a missile
     * drops, the missile is lowered out of it, and the doors swing shut again once it is clear.
     */
    private static void hatch(LanternPainter painter, PlanePath path, ConstructPainter.Frame frame, double t) {
        int every = missileEvery();
        double open = path.hatch(t, every);
        if (open > 0.0) {
            painter.shape(BAY_INSIDE, frame, 1.0, 1.0);
        }
        // Each door turns about its hinge, where it meets the body, outwards and down.
        double[] hull = hull(AirStrike.BAY_Z);
        double hinge = Math.toRadians(270.0 + BAY_HALF_ANGLE);
        double hingeX = hull[1] * Math.cos(hinge);
        double hingeY = hull[3] + hull[2] * Math.sin(hinge);
        double swing = DOOR_OPEN * Ease.smoother(open);
        for (int side = -1; side <= 1; side += 2) {
            ConstructPainter.Frame door = side > 0 ? frame : frame.stretched(-1.0, 1.0, 1.0);
            painter.shape(BAY_DOOR, door.turned(hingeX, hingeY, 0.0, 0.0, 0.0, 1.0, swing), 1.0, 1.0);
        }
        double lowered = path.lowered(t, every);
        if (open > 0.0 && lowered >= 0.0) {
            double y = Mth.lerp(lowered, IN_BAY_Y, AirStrike.DROP_Y);
            ConstructPainter.Frame missile = new ConstructPainter.Frame(frame.at(0.0, y, AirStrike.BAY_Z),
                    frame.right(), frame.up(), frame.forward(), frame.scale() * MISSILE_SCALE);
            painter.shape(MISSILE, missile, 1.0, 1.0);
        }
    }

    /**
     * The big missiles that just dropped out of the hatch, falling away under the plane with their motors dead, the way
     * the server moves them (see {@link PlanePath#fall}), from the very spot they hung in the hatch: until each one's
     * motor fires or it strikes, when it is drawn as a missile of its own (see {@link ClientConstructs#launch}).
     */
    private static void dropping(LanternPainter painter, int owner, PlanePath path, double t, float partialTick) {
        int every = missileEvery();
        for (int release = path.lastRelease(t, every); release >= 0 && t - release <= AirStrike.IGNITE_LATEST + 2;
                release -= every) {
            ClientConstructs.Launch launch = ClientConstructs.launch(owner, AirStrike.BIG_MISSILE, release,
                    partialTick);
            double since = launch == null ? t - release : launch.since();
            double until = launch == null ? EARLIEST_IGNITION : launch.leaves();
            if (since >= 0.0 && since <= until) {
                Vec3[] state = falling(path.dropsOut(release), false, since);
                missile(painter, false, state[0], state[1], state[2], -1.0, -1.0);
            }
            if (release - every < PlanePath.FORM + PlanePath.MISSILE_FIRST) {
                break;
            }
        }
    }

    /**
     * A missile let go of {@code since} ticks ago, still falling with its motor dead (see {@link PlanePath#fall}), from
     * how it was let go: {where it is, its nose, its up}, smooth between the ticks.
     */
    private static Vec3[] falling(Vec3[] released, boolean small, double since) {
        int ticks = (int) Math.floor(Math.max(0.0, since));
        Vec3[] state = released;
        for (int k = 0; k < ticks; k++) {
            state = PlanePath.fall(state, small);
        }
        Vec3[] next = PlanePath.fall(state, small);
        double u = Mth.clamp(since - ticks, 0.0, 1.0);
        Vec3 nose = state[2].lerp(next[2], u).normalize();
        return new Vec3[] { state[0].lerp(next[0], u), nose, PlanePath.carried(state[3].lerp(next[3], u), nose) };
    }

    /**
     * The four propellers, spinning up as it takes shape. The one that bursts runs down and stands still; the others
     * roar on into the ground.
     */
    private static void propellers(LanternPainter painter, PlanePath path, ConstructPainter.Frame frame, double t,
            double grown) {
        double failed = path.failTick();
        for (int e = 0; e < ENGINES.length; e++) {
            double speed = PROP_SPIN * Ease.smooth((t - GROW_FROM) / 40.0);
            if (e == 2 && t > failed) {
                speed = PROP_SPIN * Math.exp(-(t - failed) / PROP_DIES);
            }
            ConstructPainter.Frame hub = frame.moved(ENGINES[e], AirStrike.ENGINE_Y, AirStrike.ENGINE_Z);
            painter.shape(PROPELLER, hub.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, propeller(path, e, t)), 1.0, 1.0);
            // Turning fast, the air round its blades shimmers: a faint ring of light where their tips run.
            double blur = Mth.clamp(speed / PROP_SPIN, 0.0, 1.0) * grown;
            if (blur > 0.05) {
                Vec3 middle = hub.at(0.0, 0.0, 0.62);
                double radius = PROP_RADIUS * frame.scale();
                painter.circle(middle, frame.right(), frame.up(), radius, 0.05 * radius, 0.35 * radius,
                        Colors.alpha(0.3 * blur), Colors.alpha(0.12 * blur));
            }
        }
    }

    /** How far the propellers have turned {@code t} ticks after the call, in radians. */
    private static double spun(double t) {
        return PROP_SPIN * Math.max(0.0, t - GROW_FROM) * Ease.smooth((t - GROW_FROM) / 40.0);
    }

    /**
     * How far propeller {@code e} (see {@link #ENGINES}) is turned about its hub {@code t} ticks after the call, in
     * radians: the one that bursts runs down after the burst and stands still, and the left ones turn the other way
     * round, as they would on a real plane.
     */
    private static double propeller(PlanePath path, int e, double t) {
        double failed = path.failTick();
        double spun = spun(t);
        if (e == 2 && t > failed) {
            spun = spun(failed) + PROP_SPIN * PROP_DIES * (1.0 - Math.exp(-(t - failed) / PROP_DIES));
        }
        return (e < 2 ? -spun : spun) + e * 0.7;
    }

    /**
     * The plane's own moving parts breaking up with it (see {@link ConstructPainter#shattered}), as they were when it
     * broke: its propellers, its miniguns as they pointed, and the doors of its hatch.
     */
    private static void partsBroken(LanternPainter painter, int owner, PlanePath path, ConstructPainter.Frame frame,
            double t, double apart, double bright) {
        for (int e = 0; e < ENGINES.length; e++) {
            painter.shattered(PROPELLER, frame.moved(ENGINES[e], AirStrike.ENGINE_Y, AirStrike.ENGINE_Z)
                    .turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, propeller(path, e, t)), apart, bright);
        }
        Guns guns = GUNS.get(owner);
        for (int gun = 0; gun < 2; gun++) {
            double side = gun == 0 ? -1.0 : 1.0;
            Vec3 aim = guns != null && guns.drawn[gun] != null ? guns.drawn[gun] : path.gunRest(gun, t);
            Vec3 up = guns != null && guns.up[gun] != null ? guns.up[gun] : frame.up();
            ConstructPainter.Frame mount = ConstructPainter.Frame.of(frame.at(side * AirStrike.GUN_X, AirStrike.GUN_Y,
                    AirStrike.GUN_Z), aim, up, frame.scale());
            painter.shattered(GUN, mount, apart, bright);
            double turned = guns == null ? 0.0 : guns.turned[gun];
            painter.shattered(BARRELS, mount.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, turned), apart, bright);
        }
        double[] hull = hull(AirStrike.BAY_Z);
        double hinge = Math.toRadians(270.0 + BAY_HALF_ANGLE);
        double swing = DOOR_OPEN * Ease.smoother(path.hatch(t, missileEvery()));
        for (int side = -1; side <= 1; side += 2) {
            ConstructPainter.Frame door = side > 0 ? frame : frame.stretched(-1.0, 1.0, 1.0);
            painter.shattered(BAY_DOOR, door.turned(hull[1] * Math.cos(hinge), hull[3] + hull[2] * Math.sin(hinge), 0.0,
                    0.0, 0.0, 1.0, swing), apart, bright);
        }
    }

    /**
     * How the two miniguns of one player's plane swing and spin (0 the left one, 1 the right one): how each swings,
     * worked out just as the server works it out (see {@link PlanePath.Turret}), and what is kept from frame to frame
     * so they move smoothly.
     */
    private static final class Guns {
        private final int plane;
        private final PlanePath path;
        private final PlanePath.Turret[] turrets;
        // How far its barrels have turned, in radians, and how fast they turn now.
        private final double[] turned = new double[2];
        private final double[] spin = new double[2];
        // The way each was drawn pointing last and its up (null until it is first drawn), and what is left of a swing
        // over to where it really points after news came in late (its axis times its angle; null when there is none),
        // from when.
        private final Vec3[] drawn = new Vec3[2];
        private final Vec3[] up = new Vec3[2];
        private final Vec3[] catching = new Vec3[2];
        private final double[] caughtFrom = new double[2];
        // The plane's clock when they were drawn last.
        private double last = Double.NaN;

        Guns(int plane, PlanePath path) {
            this.plane = plane;
            this.path = path;
            this.turrets = new PlanePath.Turret[] { new PlanePath.Turret(path, 0), new PlanePath.Turret(path, 1) };
        }
    }

    /** The miniguns of this player's plane, new with every plane he calls. */
    private static Guns guns(int owner, int plane, PlanePath path) {
        Guns guns = GUNS.get(owner);
        if (guns == null || guns.plane != plane) {
            guns = new Guns(plane, path);
            GUNS.put(owner, guns);
        }
        return guns;
    }

    /**
     * A round one of the miniguns of this plane fired: from then on its gun swings to where the round says it is to
     * point next, just as it does on the server.
     */
    public static void fired(ConstructPayload plane, ConstructPayload round) {
        Vec3 next = round.facing();
        if (next.lengthSqr() < 1.0E-6 || round.variant() < 0 || round.variant() > 1) {
            return;
        }
        guns(round.owner(), plane.id(), path(plane)).turrets[round.variant()].fired(Math.round(round.charge()),
                next.normalize());
    }

    /**
     * The two miniguns: each swings smoothly round on its ball to where it is told to fire, and back to rest when it
     * has nothing to fire at (see {@link PlanePath.Turret}), its barrels spinning as it fires and its muzzle flashing
     * with every round. Its roll is carried along as it swings, so it never flips over, however far down it points.
     */
    private static void guns(LanternPainter painter, int id, int owner, PlanePath path, ConstructPainter.Frame frame,
            double t, double grown) {
        Guns state = guns(owner, id, path);
        double step = Double.isNaN(state.last) ? 0.0 : Mth.clamp(t - state.last, 0.0, 5.0);
        state.last = t;
        for (int gun = 0; gun < 2; gun++) {
            double side = gun == 0 ? -1.0 : 1.0;
            Vec3 pivot = frame.at(side * AirStrike.GUN_X, AirStrike.GUN_Y, AirStrike.GUN_Z);
            PlanePath.Turret turret = state.turrets[gun];
            Vec3 aim = turret.aim(t);
            if (turret.revised() && state.drawn[gun] != null) {
                // Where it was told to point came in late: it swings over from where it was drawn, smoothly.
                Vec3 axis = aim.cross(state.drawn[gun]);
                double angle = Math.atan2(axis.length(), aim.dot(state.drawn[gun]));
                state.catching[gun] = axis.lengthSqr() < 1.0E-12 ? null : axis.normalize().scale(angle);
                state.caughtFrom[gun] = t;
            }
            if (state.catching[gun] != null) {
                double left = state.catching[gun].length() * Math.exp(-(t - state.caughtFrom[gun]) / GUN_CATCHES_UP);
                if (left < 1.0E-3) {
                    state.catching[gun] = null;
                } else {
                    aim = Vectors.spin(aim, state.catching[gun].normalize(), left).normalize();
                }
            }
            state.drawn[gun] = aim;
            // Its up is carried along as it swings, and levels out with the plane again at its ease wherever that is
            // clear (not while it points straight down).
            Vec3 up = PlanePath.carried(state.up[gun] == null ? frame.up() : state.up[gun], aim);
            Vec3 level = frame.up().subtract(aim.scale(frame.up().dot(aim)));
            double clear = Mth.clamp((level.lengthSqr() - 0.05) / 0.2, 0.0, 1.0);
            if (clear > 0.0) {
                up = PlanePath.carried(up.lerp(level.normalize(), clear * (1.0 - Math.exp(-step / GUN_LEVELS))), aim);
            }
            state.up[gun] = up;
            int fired = turret.lastFired(t);
            double since = fired < 0 ? Double.MAX_VALUE : t - fired;
            // The barrels spin up as it fires and run down after.
            double wantSpin = since < 8.0 ? BARREL_SPIN : 0.0;
            state.spin[gun] = Mth.lerp(1.0 - Math.exp(-step / (wantSpin > state.spin[gun] ? 3.0 : BARREL_DIES)),
                    state.spin[gun], wantSpin);
            state.turned[gun] += state.spin[gun] * step;
            ConstructPainter.Frame mount = ConstructPainter.Frame.of(pivot, aim, up, frame.scale());
            painter.shape(GUN, mount, 1.0, 1.0);
            painter.shape(BARRELS, mount.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, state.turned[gun]), 1.0, 1.0);
            if (since < 3.0) {
                // The muzzle flashes with the round going out: a burst of light, a star of flame and a puff of light
                // blown out ahead of it.
                double flash = 1.0 - since / 3.0;
                Vec3 muzzle = mount.at(0.0, 0.0, AirStrike.GUN_LENGTH + 0.4);
                painter.flare(muzzle, 3.4 * grown * (0.6 + 0.4 * flash), flash);
                for (int k = 0; k < 5; k++) {
                    Vec3 ray = Noise.direction(fired, 211 + k).scale(0.55).add(aim);
                    painter.edge(muzzle, muzzle.add(ray.normalize().scale(3.6 * flash)), 0.3, flash);
                }
                painter.edge(muzzle, muzzle.add(aim.scale(5.5 * flash)), 0.55, 0.9 * flash);
            }
        }
    }

    /**
     * Lights on its wingtips and tail blinking in turn, beacons on its back and belly flashing, its landing lights
     * burning, and a faint glow on the tips of its wings.
     */
    private static void lights(LanternPainter painter, ConstructPainter.Frame frame, double t, double down) {
        double blink = Math.max(0.0, 1.0 - Mth.frac(t / 30.0) * 6.0);
        double tail = Math.max(0.0, 1.0 - Mth.frac(t / 30.0 + 0.5) * 6.0);
        double beacon = Math.max(0.0, 1.0 - Mth.frac(t / 18.0 + 0.25) * 4.0);
        double s = frame.scale();
        for (int side = -1; side <= 1; side += 2) {
            painter.flare(frame.at(side * 28.0, 3.45, 0.4), (0.6 + 1.6 * blink) * s, 0.4 + 0.6 * blink);
            painter.flare(frame.at(side * 0.85, -2.3, 17.0), 1.3 * s, 0.7);
        }
        painter.flare(frame.at(0.0, 2.45, -22.75), (0.5 + 1.4 * tail) * s, 0.35 + 0.6 * tail);
        painter.flare(frame.at(0.0, -2.75, 7.0), (0.4 + 2.2 * beacon) * s, 0.3 + 0.7 * beacon);
        painter.flare(frame.at(0.0, 3.5, 1.5), (0.4 + 1.8 * beacon) * s, 0.3 + 0.7 * beacon);
        if (down > 0.0) {
            // Going down, the tips of its wings trail streaks of light.
            for (int side = -1; side <= 1; side += 2) {
                Vec3 tip = frame.at(side * 28.0, 3.45, 0.4);
                painter.edge(tip, tip.subtract(frame.forward().scale(8.0 * down * s)), 0.3 * s, 0.6 * down);
            }
        }
    }

    /** Every engine breathes a faint flame of light out of its exhaust stack; the one that burst only burns. */
    private static void engines(LanternPainter painter, PlanePath path, ConstructPainter.Frame frame, double t,
            double grown) {
        double power = 0.55 * Ease.smooth((t - GROW_FROM - 10.0) / 20.0) * Math.min(1.0, grown);
        if (power <= 0.01) {
            return;
        }
        double failed = path.failTick();
        Vec3 back = frame.forward().scale(-1.0);
        double s = frame.scale();
        double[] xs = { -AirStrike.ENGINE_OUTER_X, -AirStrike.ENGINE_X, AirStrike.ENGINE_X, AirStrike.ENGINE_OUTER_X };
        for (int e = 0; e < xs.length; e++) {
            if (e == 2 && t > failed) {
                continue;
            }
            Vec3 stack = frame.at(xs[e] + Math.signum(xs[e]) * 1.2, AirStrike.ENGINE_Y + 0.42, -0.75);
            painter.exhaust(stack, back, 2.6 * s, 0.22 * s, power);
        }
    }

    /**
     * While its sensor scans, a cone of light shines out of the ball under its nose down onto the ground, where the wave
     * of the scan rolls out.
     */
    private static void scanCone(LanternPainter painter, int owner, ConstructPainter.Frame frame, float partialTick) {
        Vec3 sensor = frame.at(0.0, AirStrike.SENSOR_Y, AirStrike.SENSOR_Z);
        for (ClientConstructs.Scan scan : ClientConstructs.scans(partialTick)) {
            if (scan.owner() != owner || !scan.hostileOnly()) {
                continue;
            }
            double rolling = scan.radius() / RingScan.SPEED;
            double clock = scan.reached() / RingScan.SPEED;
            double strength = (1.0 - Ease.smooth((clock - rolling * 0.7) / (rolling * 0.3)))
                    * Ease.smooth(clock / 3.0);
            if (strength <= 0.01) {
                continue;
            }
            painter.flare(sensor, 1.4 * frame.scale(), strength);
            int lines = 16;
            double reach = Math.max(1.0, scan.reached());
            for (int k = 0; k < lines; k++) {
                double angle = Math.PI * 2.0 * k / lines + painter.time() * 0.02;
                Vec3 foot = scan.center().add(Math.cos(angle) * reach, 0.1, Math.sin(angle) * reach);
                painter.edge(sensor, foot, 0.12, 0.35 * strength);
            }
            painter.edge(sensor, scan.center(), 0.5, 0.6 * strength);
        }
    }

    /**
     * An engine bursts: a flash and a spray of sparks, and from then on flames of light blow back out of it and a trail
     * of light and sparks follows the plane down.
     */
    private static void burning(LanternPainter painter, PlanePath path, ConstructPainter.Frame frame, double t) {
        double failed = path.failTick();
        double after = t - failed;
        double s = frame.scale();
        Vec3 engine = frame.at(AirStrike.ENGINE_X, AirStrike.ENGINE_Y, 1.0);
        if (after < 6.0) {
            double flash = 1.0 - after / 6.0;
            painter.flare(engine, 7.0 * flash * s, flash);
            for (int k = 0; k < 14; k++) {
                Vec3 way = Noise.direction(k, 223);
                painter.edge(engine.add(way.scale(after * 0.9 * s)), engine.add(way.scale((after * 0.9 + 2.2) * s)),
                        0.2 * s, flash);
            }
        }
        double flicker = 0.75 + 0.25 * Math.sin(t * 2.9) * Math.sin(t * 1.3 + 0.4);
        Vec3 back = frame.forward().scale(-1.0).add(frame.up().scale(0.25)).normalize();
        painter.exhaust(frame.at(AirStrike.ENGINE_X, AirStrike.ENGINE_Y + 0.4, -3.8), back, 9.0 * s, 1.2 * s,
                flicker);
        painter.exhaust(frame.at(AirStrike.ENGINE_X, AirStrike.ENGINE_Y + 0.9, 3.0), back, 5.0 * s, 0.8 * s,
                0.6 * flicker);
        // Its trail: light and sparks along where the burning engine was on the last ticks.
        Vec3 last = path.point(t, AirStrike.ENGINE_X, AirStrike.ENGINE_Y, -4.0);
        for (int k = 1; k <= 12; k++) {
            double then = Math.max(failed, t - 2.0 * k);
            Vec3 next = path.point(then, AirStrike.ENGINE_X, AirStrike.ENGINE_Y, -4.0);
            double fade = 1.0 - k / 13.0;
            painter.edge(last, next, 1.6 * fade * s, 0.8 * fade);
            last = next;
        }
        int flick = (int) (t / 2.0);
        for (int k = 0; k < 6; k++) {
            Vec3 from = frame.at((Noise.of(flick, k, 231) - 0.5) * 30.0, 2.0,
                    (Noise.of(flick, k, 232) - 0.5) * 30.0);
            painter.edge(from, from.add(Noise.direction(flick, 233 + k).scale(3.0 * s)), 0.2 * s, 0.9);
        }
    }

    /**
     * A plane whose maker let go of it in the air: it breaks into solid pieces where it was, flung apart, and they tumble
     * down.
     *
     * @param since ticks since it was let go
     */
    public static void broken(LanternPainter painter, ConstructPayload plane, double clock, double since) {
        PlanePath path = path(plane);
        double t = Math.min(clock - since, path.crashTick());
        double apart = since / BREAK_TICKS;
        ConstructPainter.Frame frame = frame(path, t, Math.max(grown(t), 1.0E-3));
        painter.glare(0.5 * Math.max(0.0, 1.0 - since / 6.0));
        painter.fling(AIR_FLING);
        painter.shattered(BODY, frame, apart, 1.2);
        partsBroken(painter, plane.owner(), path, frame, t, apart, 1.2);
        // Its jets break up where they were too, with the missiles under their wings.
        painter.fling(2.5);
        for (int k = 0; k < PlanePath.JETS; k++) {
            if (!path.hasJet(k) || t < path.jetFrom(k) || path.jetsFled(t) >= PlanePath.JET_GONE) {
                continue;
            }
            Vec3[] axes = path.jetAxes(k, t);
            ConstructPainter.Frame jet = ConstructPainter.Frame.of(path.jetAt(k, t), axes[0], axes[1],
                    JET_SCALE * Math.max(path.jetGrown(k, t), 1.0E-3));
            painter.shattered(JET, jet, apart, 1.2);
            for (int side = -1; side <= 1; side += 2) {
                painter.shattered(MISSILE, new ConstructPainter.Frame(jet.at(side * AirStrike.PYLON_X,
                        AirStrike.PYLON_Y, AirStrike.PYLON_Z), jet.right(), jet.up(), jet.forward(),
                        jet.scale() * SMALL_MISSILE_SCALE), apart, 1.2);
            }
        }
        painter.fling(1.0);
        painter.glare(0.0);
    }

    /**
     * The crash: the plane breaks into solid pieces flung far out from where it struck, and a blast like a small sun goes
     * up (see {@link #blast}). Views close by shake with it (see {@link ClientConstructs#shake}).
     *
     * @param since ticks since it struck the ground
     */
    private static void crashed(LanternPainter painter, int owner, PlanePath path, double since) {
        double apart = since / BREAK_TICKS;
        if (apart < 1.0) {
            ConstructPainter.Frame frame = frame(path, path.crashTick(), 1.0);
            painter.glare(Math.max(0.0, 0.9 - since / 10.0));
            painter.fling(CRASH_FLING);
            painter.shattered(BODY, frame, apart, 1.4);
            partsBroken(painter, owner, path, frame, path.crashTick(), apart, 1.4);
            painter.fling(1.0);
            painter.glare(0.0);
        }
        blast(painter, path.crash(), since);
    }

    /**
     * The blast of the crash, light and nothing else, the only part of the air strike that is see-through: a flash, a
     * fireball rising on a stem of light and rolling into a mushroom cloud, a ring of light racing out round its middle,
     * a shell of light racing out, rings running out over the ground and rays shooting out of it.
     *
     * @param since ticks since the plane struck
     */
    private static void blast(LanternPainter painter, Vec3 ground, double since) {
        double life = Math.max(0.0, 1.0 - since / AirStrike.BLAST_TICKS);
        if (life <= 0.0) {
            return;
        }
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        Vec3 up = Vectors.UP;
        // The flash, blinding for a moment: a ball of white light bursting out.
        double flash = Math.max(0.0, 1.0 - since / 10.0);
        painter.flare(ground.add(0.0, 3.0, 0.0), 6.0 + 40.0 * flash * flash, Math.min(1.0, 0.4 + flash));
        if (flash > 0.0) {
            double burst = 4.0 + 18.0 * (1.0 - flash * flash);
            painter.haze(ground.add(0.0, 2.0, 0.0), east.scale(burst), up.scale(burst * 0.8), south.scale(burst),
                    LanternPainter.HOT, 0.8 * flash);
        }
        // The fireball: it swells fast, then climbs slowly on its stem, burning down as it goes; a white-hot heart in a
        // glowing green cloud.
        double swell = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 22.0), 3.0);
        double climb = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 70.0), 2.0);
        double radius = 2.5 + FIREBALL * swell;
        Vec3 ball = ground.add(0.0, 2.0 + FIREBALL_HIGH * climb, 0.0);
        double heat = Math.max(0.0, 1.0 - since / 45.0);
        painter.haze(ball, east.scale(radius), up.scale(radius * 0.92), south.scale(radius), LanternPainter.GREEN,
                0.55 * life);
        painter.haze(ball, east.scale(radius * 0.62), up.scale(radius * 0.58), south.scale(radius * 0.62),
                LanternPainter.HOT, 0.6 * heat * life);
        // Its cap spreads into a mushroom as it climbs, and the dust of the blast surges out low over the ground.
        if (since > 6.0) {
            double cap = Ease.smooth((since - 6.0) / 30.0);
            double wide = radius * (1.25 + 0.55 * cap);
            painter.haze(ball.subtract(0.0, radius * 0.25, 0.0), east.scale(wide), up.scale(radius * 0.5),
                    south.scale(wide), LanternPainter.GREEN, 0.4 * cap * life);
        }
        double surge = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 26.0), 2.0);
        double spread = 5.0 + (SHELL - 4.0) * surge;
        painter.haze(ground.add(0.0, 1.2, 0.0), east.scale(spread), up.scale(1.6 + 2.2 * surge), south.scale(spread),
                LanternPainter.GREEN, 0.45 * life * (1.0 - 0.6 * surge));
        int shell = Colors.alpha(0.85 * life);
        int haze = Colors.alpha(0.4 * life);
        for (int k = -3; k <= 3; k++) {
            double lat = k / 3.5 * (Math.PI * 0.5);
            double turn = painter.time() * 0.01 + k;
            Vec3 a = new Vec3(Math.cos(turn), 0.0, Math.sin(turn));
            painter.circle(ball.add(0.0, radius * Math.sin(lat), 0.0), a, a.cross(up), radius * Math.cos(lat),
                    0.3, 2.4, shell, haze);
        }
        for (int k = 0; k < 4; k++) {
            double turn = k * Math.PI / 4.0 + painter.time() * 0.015;
            Vec3 across = new Vec3(Math.cos(turn), 0.0, Math.sin(turn));
            painter.circle(ball, across, up, radius, 0.2, 1.8, shell, haze);
        }
        painter.flare(ball, radius * 1.9, 0.55 * life);
        // Its cap rolls: a ring of rolling light round its lower half, turning over and over.
        if (since > 6.0) {
            double cap = Ease.smooth((since - 6.0) / 24.0);
            double around = radius * (1.05 + 0.35 * cap);
            double thick = radius * (0.25 + 0.2 * cap);
            Vec3 middle = ball.subtract(0.0, radius * 0.35, 0.0);
            int rolls = 14;
            for (int k = 0; k < rolls; k++) {
                double turn = Math.PI * 2.0 * k / rolls;
                Vec3 out = new Vec3(Math.cos(turn), 0.0, Math.sin(turn));
                double spin = painter.time() * 0.08 + k;
                Vec3 a = out.scale(Math.cos(spin)).add(up.scale(Math.sin(spin)));
                Vec3 b = out.scale(-Math.sin(spin)).add(up.scale(Math.cos(spin)));
                painter.circle(middle.add(out.scale(around)), a, b, thick, 0.18, 1.4,
                        Colors.alpha(0.7 * cap * life), Colors.alpha(0.3 * cap * life));
            }
        }
        // The stem of light it climbs on, and a skirt of light round its foot.
        Vec3 foot = ground.add(0.0, 0.3, 0.0);
        Vec3 top = ball.subtract(0.0, radius * 0.7, 0.0);
        if (top.y > foot.y + 1.0) {
            painter.beamOfLight(foot, top, life, 20.0 + since, 3.2 + 1.6 * (1.0 - climb));
            double stem = 2.2 + 1.6 * (1.0 - climb);
            painter.haze(foot.lerp(top, 0.5), east.scale(stem), up.scale((top.y - foot.y) * 0.5), south.scale(stem),
                    LanternPainter.GREEN, 0.4 * life);
            for (int k = 0; k < 3; k++) {
                double skirt = 2.5 + 3.5 * climb + k * 1.4;
                painter.circle(foot.add(0.0, 0.4 + k * 0.8, 0.0), east, south, skirt, 0.2, 1.6, shell, haze);
            }
        }
        // A ring of light races out round its middle, the way a cloud rings the stem of a great blast.
        if (since > 3.0 && since < 40.0) {
            double ring = 1.0 - Math.pow(1.0 - Math.min(1.0, (since - 3.0) / 30.0), 2.0);
            double fade = (1.0 - Ease.smooth((since - 22.0) / 18.0)) * life;
            painter.circle(ground.add(0.0, FIREBALL_HIGH * 0.45, 0.0), east, south, 4.0 + COLLAR * ring, 0.3, 3.0,
                    Colors.alpha(0.8 * fade), Colors.alpha(0.35 * fade));
        }
        // A shell of light racing out: rings round it at five heights, and four more over its top.
        double out = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 16.0), 3.0);
        double dome = Math.max(0.5, SHELL * out);
        double domeFade = life * (1.0 - Ease.smooth((since - 14.0) / 20.0));
        int domeEdge = Colors.alpha(0.8 * domeFade);
        int domeHaze = Colors.alpha(0.35 * domeFade);
        for (int k = 0; k < 5; k++) {
            double lift = k / 5.0;
            painter.circle(ground.add(0.0, dome * lift, 0.0), east, south, dome * Math.sqrt(1.0 - lift * lift),
                    0.3 - 0.04 * k, 2.6 - 0.35 * k, domeEdge, domeHaze);
        }
        for (int k = 0; k < 4; k++) {
            double turn = k * Math.PI / 4.0;
            Vec3 across = new Vec3(Math.cos(turn), 0.0, Math.sin(turn));
            painter.circle(ground, across, up, dome * 0.97, 0.18, 1.6, domeEdge, domeHaze);
        }
        // Rings running out over the ground one after the other.
        for (int k = 0; k < 5; k++) {
            double ring = since - k * 6.0;
            if (ring < 0.0) {
                continue;
            }
            double wave = 1.0 - Math.pow(1.0 - Math.min(1.0, ring / 20.0), 2.0);
            double fade = Math.max(0.0, 1.0 - ring / 34.0) * life;
            painter.circle(ground.add(0.0, 0.2, 0.0), east, south, 3.0 + (SHELL + 8.0) * wave, 0.35, 2.0,
                    Colors.alpha(fade), Colors.alpha(0.5 * fade));
        }
        // Rays of light shooting out of it.
        if (since < 30.0) {
            double rays = 1.0 - since / 30.0;
            Vec3 heart = ground.add(0.0, 2.0, 0.0);
            for (int k = 0; k < 24; k++) {
                Vec3 way = Noise.direction(k, 241);
                way = new Vec3(way.x, Math.abs(way.y) * 0.8 + 0.1, way.z).normalize();
                double reach = (10.0 + 22.0 * Noise.of(k, 241, 5)) * Math.min(1.0, since / 4.0 + 0.2);
                painter.edge(heart.add(way.scale(2.0)), heart.add(way.scale(reach)), 0.6 * rays, rays);
            }
        }
        if (since < 1.0) {
            painter.flare(ground.add(0.0, 2.0, 0.0), 40.0, 1.0);
        }
    }

    // ---- The jets ----

    /**
     * The two jets: each grows out of a thread of the ring's light beside the plane, white-hot and cooling to green, and
     * races round it, banking into its turns, its flames burning, the air streaming off its wingtips and a small missile
     * under each wing (the next one growing out of the light once it has fired). Once the plane's engine bursts they
     * break away: their flames roar out, a cone of mist forms round them as they break the sound barrier with a ring of
     * light, and a moment later they are gone in a star of light, breaking into solid pieces.
     */
    private static void jets(LanternPainter painter, int owner, PlanePath path, double t, @Nullable Vec3 ring,
            float partialTick) {
        double fled = path.jetsFled(t);
        for (int k = 0; k < PlanePath.JETS; k++) {
            if (!path.hasJet(k) || t < path.jetFrom(k)) {
                continue;
            }
            if (fled >= PlanePath.JET_GONE) {
                gone(painter, path, k, fled - PlanePath.JET_GONE);
                continue;
            }
            double since = t - path.jetFrom(k);
            double grown = path.jetGrown(k, t);
            Vec3 at = path.jetAt(k, t);
            Vec3[] axes = path.jetAxes(k, t);
            ConstructPainter.Frame frame = ConstructPainter.Frame.of(at, axes[0], axes[1],
                    JET_SCALE * Math.max(grown, 1.0E-3));
            if (since < PlanePath.JET_GROWS + 6.0) {
                // The ring's light pours into it as it takes shape (the plane's own, with its maker out of sight).
                double fade = 1.0 - Ease.smooth((since - PlanePath.JET_GROWS) / 6.0);
                Vec3 from = ring != null ? ring : path.point(t, (k == 0 ? -1.0 : 1.0) * PlanePath.JET_WING, 3.2, 0.4);
                painter.beamOfLight(from, at, fade, since, 1.3);
            }
            double speed = path.jetSpeed(k, t);
            if (painter.visible(at, 12.0 + 2.0 * speed)) {
                double hot = 0.65 * (1.0 - Ease.smooth(since / (PlanePath.JET_GROWS + 4.0)));
                painter.glare(hot);
                painter.ambient(GLOWS);
                painter.shape(JET, frame, 1.0, 1.0);
                pylons(painter, owner, path, k, frame, t, hot, partialTick);
                painter.ambient(0.0);
                painter.glare(0.0);
            }
            flames(painter, frame, speed, fled, grown);
            if (fled > -4.0) {
                soundBarrier(painter, path, k, frame, fled);
            }
        }
    }

    /**
     * The small missile under each wing of a jet. One it fires drops off its pylon and falls away under it with its
     * motor dead, the way the server moves it (see {@link PlanePath#fall}), until its motor fires (see
     * {@link ClientConstructs#launch}); meanwhile the next one grows out of the light on the pylon, timed from when the
     * last one was fired however soon that one struck.
     */
    private static void pylons(LanternPainter painter, int owner, PlanePath path, int k, ConstructPainter.Frame frame,
            double t, double hot, float partialTick) {
        for (int side = 0; side < 2; side++) {
            int variant = AirStrike.JET_MISSILE + 2 * k + side;
            int fired = ClientConstructs.launched(owner, variant);
            ClientConstructs.Launch launch = fired < 0 ? null
                    : ClientConstructs.launch(owner, variant, fired, partialTick);
            double since = fired < 0 ? -1.0 : launch == null ? t - fired : launch.since();
            if (since >= 0.0 && since <= (launch == null ? AirStrike.SMALL_IGNITES : launch.leaves())) {
                Vec3[] state = falling(path.firedOff(k, side, fired), true, since);
                painter.glare(0.0);
                missile(painter, true, state[0], state[1], state[2], -1.0, -1.0);
            }
            double load = since < 0.0 ? 1.0 : Ease.backOut((since - 2.0) / AirStrike.RELOAD_TICKS);
            if (load <= 0.01) {
                continue;
            }
            Vec3 at = frame.at((side == 0 ? -1.0 : 1.0) * AirStrike.PYLON_X, AirStrike.PYLON_Y, AirStrike.PYLON_Z);
            ConstructPainter.Frame missile = new ConstructPainter.Frame(at, frame.right(), frame.up(),
                    frame.forward(), frame.scale() * load * SMALL_MISSILE_SCALE);
            boolean growing = since >= 0.0 && since < AirStrike.RELOAD_TICKS + 2.0;
            painter.glare(growing ? Math.max(hot, 0.6 * (1.0 - load)) : hot);
            painter.ambient(GLOWS);
            painter.shape(MISSILE, missile, 1.0, 1.0);
        }
        painter.glare(hot);
        painter.ambient(GLOWS);
    }

    /**
     * The flames out of a jet's two nozzles, longer the faster it goes and roaring out once it breaks away; the air
     * streaming off its wingtips as it banks; and a long streak of light behind it once it races off.
     */
    private static void flames(LanternPainter painter, ConstructPainter.Frame frame, double speed, double fled,
            double grown) {
        double s = frame.scale();
        Vec3 back = frame.forward().scale(-1.0);
        double burn = (fled > 0.0 ? Math.min(1.0, 0.6 + fled / 6.0) : 0.55) * Math.min(1.0, grown);
        double length = (3.0 + 1.6 * Math.min(speed, 12.0)) * s;
        for (int side = -1; side <= 1; side += 2) {
            painter.exhaust(frame.at(side * 0.55, 0.0, -6.5), back, length, 0.4 * s, burn);
        }
        double bank = Mth.clamp(1.0 - frame.up().y, 0.0, 1.0);
        if (bank > 0.05 && fled < 0.0) {
            for (int side = -1; side <= 1; side += 2) {
                Vec3 tip = frame.at(side * 5.42, -0.2, -3.6);
                painter.edge(tip, tip.add(back.scale((4.0 + 10.0 * bank) * s)), 0.07 * s, Math.min(0.7, 1.4 * bank));
            }
        }
        if (fled > 0.0) {
            Vec3 tail = frame.at(0.0, 0.0, -6.5);
            double streak = Math.min(60.0, 2.5 * speed * Math.min(1.0, fled / 4.0));
            painter.edge(tail, tail.add(back.scale(streak)), 0.5 * s, 0.7);
            painter.edge(tail, tail.add(back.scale(streak * 0.5)), 1.1 * s, 0.35);
        }
    }

    /**
     * A jet breaking the sound barrier: a cone of mist builds round its body as it nears the speed of sound and is torn
     * off it with a boom, a flash where it broke through and rings of light racing out from there.
     *
     * @param fled ticks since it broke away
     */
    private static void soundBarrier(LanternPainter painter, PlanePath path, int k, ConstructPainter.Frame frame,
            double fled) {
        double since = fled - PlanePath.JET_BOOM;
        if (since < -4.0 || since > 12.0) {
            return;
        }
        double s = frame.scale();
        double mist = since < 0.0 ? Ease.smooth((since + 4.0) / 4.0) : 1.0 - Ease.smooth(since / 5.0);
        if (mist > 0.01) {
            for (int c = 0; c < 7; c++) {
                double fade = mist * (1.0 - c / 8.0);
                painter.circle(frame.at(0.0, 0.0, 2.4 - c * 0.95), frame.right(), frame.up(), (1.1 + c * 0.42) * s,
                        0.06 * s, 0.9 * s, Colors.alpha(0.55 * fade), Colors.alpha(0.25 * fade));
            }
            painter.haze(frame.at(0.0, 0.0, -0.6), frame.right().scale(2.8 * s), frame.up().scale(2.8 * s),
                    frame.forward().scale(3.6 * s), LanternPainter.HOT, 0.35 * mist);
        }
        if (since >= 0.0) {
            double boom = path.failTick() + PlanePath.JET_BOOM;
            Vec3 where = path.jetAt(k, boom);
            Vec3[] across = Vectors.across(path.jetAxes(k, boom)[0]);
            double ring = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 9.0), 2.0);
            double fade = 1.0 - since / 12.0;
            painter.circle(where, across[0], across[1], 2.0 + 26.0 * ring, 0.35, 2.6, Colors.alpha(0.9 * fade),
                    Colors.alpha(0.4 * fade));
            painter.circle(where, across[0], across[1], 1.0 + 16.0 * ring, 0.2, 1.6, Colors.alpha(0.6 * fade),
                    Colors.alpha(0.3 * fade));
            painter.flare(where, (10.0 + 14.0 * ring) * fade, fade);
        }
    }

    /**
     * A jet that raced off is gone: it breaks into solid pieces in a star of light, and the pieces and the star fly on
     * the way it went, slowing down, instead of stopping dead where it broke.
     *
     * @param since ticks since it went
     */
    private static void gone(LanternPainter painter, PlanePath path, int k, double since) {
        double t = path.failTick() + PlanePath.JET_GONE;
        Vec3 at = path.jetAt(k, t).add(path.jetVelocity(k, t).scale(JET_FLIES_ON
                * (1.0 - Math.exp(-since / JET_FLIES_ON))));
        if (since < JET_BREAKS) {
            Vec3[] axes = path.jetAxes(k, t);
            painter.glare(0.9 - 0.5 * since / JET_BREAKS);
            painter.fling(2.5);
            painter.shattered(JET, ConstructPainter.Frame.of(at, axes[0], axes[1], JET_SCALE), since / JET_BREAKS,
                    1.4);
            painter.fling(1.0);
            painter.glare(0.0);
        }
        star(painter, at, since);
    }

    /**
     * A star of light that flashes up sharp and bright and dies away, turning a little: four long spikes and four short
     * ones, a flare and a ring widening round it. It grows with how far off it is, so it shows from afar too.
     */
    private static void star(LanternPainter painter, Vec3 at, double since) {
        double life = 1.0 - since / STAR_TICKS;
        if (life <= 0.0) {
            return;
        }
        Vec3 view = at.subtract(painter.camera());
        double far = view.length();
        Vec3[] across = Vectors.across(far < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : view.scale(1.0 / far));
        double size = 3.0 + far * 0.045;
        double pop = Ease.smooth(since / 1.5);
        double shine = pop * life * life;
        double turn = since * 0.04;
        Vec3 a = across[0].scale(Math.cos(turn)).add(across[1].scale(Math.sin(turn)));
        Vec3 b = across[0].scale(-Math.sin(turn)).add(across[1].scale(Math.cos(turn)));
        double spike = size * 3.2 * pop * (0.4 + 0.6 * life);
        for (Vec3 way : new Vec3[] { a, b }) {
            painter.edge(at.subtract(way.scale(spike)), at.add(way.scale(spike)), size * 0.12 * shine, shine);
        }
        for (Vec3 way : new Vec3[] { a.add(b).normalize(), a.subtract(b).normalize() }) {
            painter.edge(at.subtract(way.scale(spike * 0.4)), at.add(way.scale(spike * 0.4)), size * 0.07 * shine,
                    0.8 * shine);
        }
        painter.flare(at, size * (1.2 + 1.4 * life), shine);
        painter.circle(at, a, b, size * (0.6 + 2.2 * (1.0 - life)), 0.1 * size, 0.6 * size, Colors.alpha(0.7 * shine),
                Colors.alpha(0.3 * shine));
    }

    // ---- What it fires ----

    /**
     * A missile of hard light, a big one out of the hatch or a jet's {@code small} one, its middle at {@code at}, its
     * nose along {@code nose} and its roll given by {@code up} (carried along as it swings, so it never flips over). A
     * big one falls with its motor dead until the motor bursts into life with a flash; a jet's small one lights its
     * motor almost at once. Burning ({@code burning} ticks since its motor fired, below 0 before), it streaks on
     * trailing its own flame and a long, thinning streak of light. Once it has struck ({@code broken} ticks ago, below
     * 0 before) it bursts into solid pieces flung out from where it was.
     */
    public static void missile(LanternPainter painter, boolean small, Vec3 at, Vec3 nose, Vec3 up, double burning,
            double broken) {
        double scale = small ? SMALL_MISSILE_SCALE : MISSILE_SCALE;
        Vec3 forward = nose.lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : nose.normalize();
        ConstructPainter.Frame frame = ConstructPainter.Frame.of(at, forward, up, scale);
        if (broken >= 0.0) {
            // Fast at first, then slowing: out of the blast's flash.
            double apart = 1.0 - Math.pow(1.0 - Math.min(1.0, broken / MISSILE_BREAKS), 2.0);
            if (apart < 1.0) {
                painter.glare(Math.max(0.0, 0.8 - broken / 6.0));
                painter.fling(small ? 1.6 : 2.6);
                painter.shattered(MISSILE, frame, apart, 1.3);
                painter.fling(1.0);
                painter.glare(0.0);
            }
            return;
        }
        painter.ambient(GLOWS);
        painter.shape(MISSILE, frame, 1.0, 1.15);
        painter.ambient(0.0);
        if (burning < 0.0) {
            return;
        }
        Vec3 tail = frame.at(0.0, 0.0, MISSILE_TAIL);
        double lit = Ease.smooth(burning / 2.0);
        if (burning < 4.0) {
            // Its motor bursts into life.
            double flash = 1.0 - burning / 4.0;
            painter.flare(tail, (small ? 1.2 : 3.6) * flash + 0.6, flash);
        }
        painter.exhaust(tail, forward.scale(-1.0), small ? 2.6 : 6.0, small ? 0.2 : 0.5, lit);
        // A long streak of light behind it, thinning out.
        Vec3 last = tail;
        int streak = 9;
        double step = small ? 1.1 : 1.9;
        for (int k = 1; k <= streak; k++) {
            Vec3 next = tail.subtract(forward.scale(step * k));
            double fade = 1.0 - (double) k / (streak + 1);
            painter.edge(last, next, (small ? 0.35 : 0.75) * fade, 0.75 * fade * lit);
            last = next;
        }
    }

    /**
     * A round from a minigun: a solid slug of hard light with a long, bright tracer streak behind it, flying out of the
     * muzzle of its gun as that pointed the tick it fired (see {@link PlanePath.Turret}) to where it strikes, and
     * splashing there in a flash, a spray of sparks and a ripple of light. One that strikes nothing flies off into the
     * air and breaks into solid crumbs out of reach.
     *
     * @param since ticks since it was fired, by the plane's clock (below 0: not yet)
     */
    public static void bullet(LanternPainter painter, ConstructPayload round, double since) {
        Guns guns = GUNS.get(round.owner());
        int gun = round.variant();
        if (since < 0.0 || guns == null || gun < 0 || gun > 1) {
            return;
        }
        int fired = Math.round(round.charge());
        Vec3 to = round.center();
        Vec3 muzzle = guns.path.pivot(gun, fired).add(guns.turrets[gun].aim(fired).scale(AirStrike.GUN_LENGTH));
        double distance = muzzle.distanceTo(to);
        double travel = Math.max(1.0, round.size());
        Vec3 way = distance < 1.0E-6 ? Vectors.UP.scale(-1.0) : to.subtract(muzzle).scale(1.0 / distance);
        if (since < travel) {
            double u = since / travel;
            Vec3 head = muzzle.lerp(to, u);
            painter.shape(SLUG, ConstructPainter.Frame.of(head, way, Vectors.UP, SLUG_SCALE), 1.0, 1.3);
            Vec3 tail = head.subtract(way.scale(Math.min(7.0, distance * u)));
            painter.edge(tail, head, 0.3, 1.0);
            painter.edge(head.subtract(way.scale(Math.min(2.2, distance * u))), head, 0.55, 0.8);
            painter.flare(head, 0.7, 0.6);
            return;
        }
        double after = since - travel;
        if (round.held()) {
            if (after < SLUG_BREAKS) {
                painter.shattered(SLUG, ConstructPainter.Frame.of(to, way, Vectors.UP, SLUG_SCALE),
                        after / SLUG_BREAKS, 1.3);
            }
            return;
        }
        if (after > 8.0) {
            return;
        }
        double fade = 1.0 - after / 8.0;
        Vec3 hit = to.add(0.0, 0.15, 0.0);
        painter.flare(hit, 0.6 + 1.6 * fade, fade);
        painter.circle(to.add(0.0, 0.06, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0),
                0.25 + 1.4 * (1.0 - fade), 0.06, 0.4, Colors.alpha(fade), Colors.alpha(0.5 * fade));
        if (after < 4.0) {
            double sparks = 1.0 - after / 4.0;
            for (int k = 0; k < 6; k++) {
                Vec3 spark = Noise.direction(round.id(), 261 + k);
                spark = new Vec3(spark.x, Math.abs(spark.y) + 0.35, spark.z).normalize();
                Vec3 from = hit.add(spark.scale(0.3 + 1.3 * (1.0 - sparks)));
                painter.edge(from, from.add(spark.scale(0.7 * sparks)), 0.09, sparks);
            }
        }
    }

    /**
     * How long a round stays on screen, from when it was fired: its flight and its splash, in ticks, and a few more for
     * a round that is heard of late.
     */
    public static int bulletTicks(ConstructPayload round) {
        return Math.round(round.size()) + 14;
    }

    /**
     * The small blast of a missile, light and nothing else (the missile itself breaks into solid pieces on its own, see
     * {@link #missile}): a white-hot flash, a fireball of light that swells and climbs as it burns out, a ring of light
     * racing out over the ground and rays and sparks shooting out.
     *
     * @param since ticks since it burst (below 0: not yet, its missile is still on its way)
     */
    public static void missileBlast(LanternPainter painter, ConstructPayload blast, double since) {
        double life = 1.0 - since / BLAST_TICKS;
        if (since < 0.0 || life <= 0.0) {
            return;
        }
        Vec3 heart = blast.center();
        double reach = Math.max(1.0, blast.size());
        if (!painter.visible(heart, reach * 3.0)) {
            return;
        }
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        Vec3 up = Vectors.UP;
        // The flash.
        double flash = Math.max(0.0, 1.0 - since / 5.0);
        if (flash > 0.0) {
            painter.flare(heart, reach * (1.2 + 3.0 * flash), flash);
        }
        // The fireball: it swells fast, climbs a little and burns out.
        double swell = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 7.0), 3.0);
        double radius = reach * (0.3 + 0.75 * swell);
        Vec3 ball = heart.add(0.0, 0.35 * reach * Math.min(1.0, since / 20.0), 0.0);
        double burn = life * life;
        painter.haze(ball, east.scale(radius), up.scale(radius * 0.9), south.scale(radius), LanternPainter.GREEN,
                0.55 * burn);
        painter.haze(ball, east.scale(radius * 0.55), up.scale(radius * 0.5), south.scale(radius * 0.55),
                LanternPainter.HOT, 0.7 * burn * Math.max(0.0, 1.0 - since / 14.0));
        int shell = Colors.alpha(0.85 * burn);
        int haze = Colors.alpha(0.4 * burn);
        for (int k = -2; k <= 2; k++) {
            double lat = k / 2.5 * (Math.PI * 0.5);
            double turn = painter.time() * 0.03 + k;
            Vec3 a = new Vec3(Math.cos(turn), 0.0, Math.sin(turn));
            painter.circle(ball.add(0.0, radius * Math.sin(lat), 0.0), a, a.cross(up), radius * Math.cos(lat), 0.08,
                    0.7, shell, haze);
        }
        for (int k = 0; k < 3; k++) {
            double turn = k * Math.PI / 3.0 + painter.time() * 0.02;
            painter.circle(ball, new Vec3(Math.cos(turn), 0.0, Math.sin(turn)), up, radius, 0.06, 0.55, shell, haze);
        }
        painter.flare(ball, radius * 1.6, 0.6 * burn);
        // A ring of light racing out over the ground, and a second one after it.
        for (int k = 0; k < 2; k++) {
            double ring = since - k * 3.0;
            if (ring < 0.0) {
                continue;
            }
            double wave = 1.0 - Math.pow(1.0 - Math.min(1.0, ring / 10.0), 2.0);
            double fade = Math.max(0.0, 1.0 - ring / 14.0);
            painter.circle(heart.subtract(0.0, 0.45, 0.0), east, south, 0.4 + reach * 1.9 * wave, 0.1, 0.8,
                    Colors.alpha(fade), Colors.alpha(0.5 * fade));
        }
        // Rays and sparks shooting out of it.
        if (since < 10.0) {
            double rays = 1.0 - since / 10.0;
            for (int k = 0; k < 14; k++) {
                Vec3 way = Noise.direction(k + blast.id(), 251);
                way = new Vec3(way.x, Math.abs(way.y) * 0.9 + 0.1, way.z).normalize();
                double out = reach * (0.8 + 2.4 * Noise.of(k, blast.id(), 5)) * Math.min(1.0,
                        since / 3.0 + 0.3);
                painter.edge(heart.add(way.scale(out * 0.35)), heart.add(way.scale(out)), 0.12 * rays, rays);
            }
        }
    }

    // ---- His arm ----

    /**
     * How far this player's ring fist is thrown up high to call the plane, 0 to 1: straight up as he calls it, down
     * again once the plane has taken shape.
     */
    public static float raised(Entity player, float partialTick) {
        float age = ClientConstructs.planeAge(player.getId(), partialTick);
        if (age < 0.0F) {
            return 0.0F;
        }
        return (float) (Ease.smooth(age / 3.0) * (1.0 - Ease.smooth((age
                - AirStrike.CALL_TICKS) / 6.0)));
    }

    // ---- Its sound ----

    /**
     * Keeps the drone of this plane where it is. {@code down} is how far it has plunged (0 to 1), or below 0 once it has
     * crashed: the drone stops.
     */
    private static void sound(int id, PlanePath path, double t, double down) {
        if (down < 0.0) {
            PlaneSound gone = SOUNDS.remove(id);
            if (gone != null) {
                gone.stopAll();
            }
            return;
        }
        Vec3 at = path.at(t);
        PlaneSound sound = SOUNDS.get(id);
        if (sound == null || sound.stopped()) {
            // Drones of planes that are gone some other way (broken up in the air, out of reach) have stopped by now.
            SOUNDS.values().removeIf(PlaneSound::stopped);
            sound = new PlaneSound(at);
            SOUNDS.put(id, sound);
        }
        sound.update(path, t, at, down);
    }

    /** The planes are gone (the world was left): so is their sound. */
    public static void clear() {
        for (PlaneSound sound : SOUNDS.values()) {
            sound.stopAll();
        }
        SOUNDS.clear();
        GUNS.clear();
    }

    /**
     * The sound of one plane: the deep, droning buzz of its four propellers and the rush of the air past it, following
     * it; as it plunges the rush rises into a scream. Its jets each whoosh by with a rush of their own, higher the faster
     * they go.
     */
    private static final class PlaneSound {
        private final Loop drone;
        private final Loop wind;
        private final Loop[] jets = new Loop[PlanePath.JETS];

        PlaneSound(Vec3 at) {
            this.drone = new Loop(SoundEvents.BEE_LOOP, at);
            this.wind = new Loop(SoundEvents.ELYTRA_FLYING, at);
            Minecraft.getInstance().getSoundManager().play(this.drone);
            Minecraft.getInstance().getSoundManager().play(this.wind);
        }

        void update(PlanePath path, double t, Vec3 at, double down) {
            long seen = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
            this.drone.follow(at, seen, 7.0F, (float) (0.5 - 0.1 * down), 1.0F - (float) down * 0.5F);
            this.wind.follow(at, seen, (float) (2.0 + 5.0 * down), (float) (0.5 + 0.9 * down * down), 1.0F);
            double fled = path.jetsFled(t);
            for (int k = 0; k < PlanePath.JETS; k++) {
                if (!path.hasJet(k) || t < path.jetFrom(k) || fled >= PlanePath.JET_GONE) {
                    if (this.jets[k] != null) {
                        this.jets[k].done = true;
                    }
                    continue;
                }
                Vec3 jet = path.jetAt(k, t);
                if (this.jets[k] == null) {
                    this.jets[k] = new Loop(SoundEvents.ELYTRA_FLYING, jet);
                    Minecraft.getInstance().getSoundManager().play(this.jets[k]);
                }
                double speed = path.jetSpeed(k, t);
                this.jets[k].follow(jet, seen, fled > 0.0 ? 7.0F : 4.0F, (float) Mth.clamp(1.1 + 0.05 * speed, 1.1, 2.0),
                        1.0F);
            }
        }

        boolean stopped() {
            return this.drone.isStopped() && this.wind.isStopped();
        }

        void stopAll() {
            this.drone.done = true;
            this.wind.done = true;
            for (Loop jet : this.jets) {
                if (jet != null) {
                    jet.done = true;
                }
            }
        }
    }

    /** One looping sound that follows a plane about. */
    private static final class Loop extends AbstractTickableSoundInstance {
        private Vec3 at;
        private long seen;
        private float loud;
        private float tone;
        private float share;
        private boolean done;

        Loop(SoundEvent event, Vec3 at) {
            super(event, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.at = at;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.05F;
            this.pitch = 0.5F;
            this.x = at.x;
            this.y = at.y;
            this.z = at.z;
        }

        void follow(Vec3 at, long seen, float loud, float tone, float share) {
            this.at = at;
            this.seen = seen;
            this.loud = loud;
            this.tone = tone;
            this.share = share;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (this.done || minecraft.level == null || minecraft.level.getGameTime() - this.seen > 10L) {
                this.stop();
                return;
            }
            this.x = this.at.x;
            this.y = this.at.y;
            this.z = this.at.z;
            this.volume = Mth.lerp(0.08F, this.volume, this.loud * this.share);
            this.pitch = Mth.lerp(0.1F, this.pitch, this.tone);
        }
    }
}
