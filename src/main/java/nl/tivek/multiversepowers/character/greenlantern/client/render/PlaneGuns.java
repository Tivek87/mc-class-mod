package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.BARRELS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.GUN;

/** The two miniguns of the plane (see {@link PlanePainter}): how they swing and spin, and drawing them. */
final class PlaneGuns {
    // How fast the barrels of a minigun spin while it fires, in radians per tick, and how long they take to run down.
    private static final double BARREL_SPIN = 1.1;
    private static final double BARREL_DIES = 18.0;
    // How long a minigun takes to swing over to where it really points when news of where it was told to point came in
    // late, and how long it takes to level its roll out again after pointing straight down (in ticks, most of the way).
    private static final double GUN_CATCHES_UP = 2.5;
    private static final double GUN_LEVELS = 8.0;

    // How the miniguns of each player's plane swing and spin, by the id of the player.
    static final Map<Integer, Guns> GUNS = new HashMap<>();

    private PlaneGuns() {
    }

    /**
     * How the two miniguns of one player's plane swing and spin (0 the left one, 1 the right one): how each swings,
     * worked out just as the server works it out (see {@link PlanePath.Turret}), and what is kept from frame to frame
     * so they move smoothly.
     */
    static final class Guns {
        private final int plane;
        final PlanePath path;
        final PlanePath.Turret[] turrets;
        // How far its barrels have turned, in radians, and how fast they turn now.
        final double[] turned = new double[2];
        private final double[] spin = new double[2];
        // The way each was drawn pointing last and its up (null until it is first drawn), and what is left of a swing
        // over to where it really points after news came in late (its axis times its angle; null when there is none),
        // from when.
        final Vec3[] drawn = new Vec3[2];
        final Vec3[] up = new Vec3[2];
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
    static Guns guns(int owner, int plane, PlanePath path) {
        Guns guns = GUNS.get(owner);
        if (guns == null || guns.plane != plane) {
            guns = new Guns(plane, path);
            GUNS.put(owner, guns);
        }
        return guns;
    }

    /**
     * The two miniguns: each swings smoothly round on its ball to where it is told to fire, and back to rest when it
     * has nothing to fire at (see {@link PlanePath.Turret}), its barrels spinning as it fires and its muzzle flashing
     * with every round. Its roll is carried along as it swings, so it never flips over, however far down it points.
     */
    static void guns(LanternPainter painter, int id, int owner, PlanePath path, ConstructPainter.Frame frame,
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
}
