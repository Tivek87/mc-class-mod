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

final class PlaneGuns {
    private static final double BARREL_SPIN = 1.1;
    private static final double BARREL_DIES = 18.0;
    private static final double GUN_CATCHES_UP = 2.5;
    private static final double GUN_LEVELS = 8.0;

    static final Map<Integer, Guns> GUNS = new HashMap<>();

    private PlaneGuns() {
    }

    static final class Guns {
        private final int plane;
        final PlanePath path;
        final PlanePath.Turret[] turrets;
        final double[] turned = new double[2];
        private final double[] spin = new double[2];
        final Vec3[] drawn = new Vec3[2];
        final Vec3[] up = new Vec3[2];
        private final Vec3[] catching = new Vec3[2];
        private final double[] caughtFrom = new double[2];
        private double last = Double.NaN;

        Guns(int plane, PlanePath path) {
            this.plane = plane;
            this.path = path;
            this.turrets = new PlanePath.Turret[] { new PlanePath.Turret(path, 0), new PlanePath.Turret(path, 1) };
        }
    }

    static Guns guns(int owner, int plane, PlanePath path) {
        Guns guns = GUNS.get(owner);
        if (guns == null || guns.plane != plane) {
            guns = new Guns(plane, path);
            GUNS.put(owner, guns);
        }
        return guns;
    }

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
            // Where it was told to point came in late: swing over from where it was drawn, not snap to it.
            if (turret.revised() && state.drawn[gun] != null) {
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
            Vec3 up = PlanePath.carried(state.up[gun] == null ? frame.up() : state.up[gun], aim);
            Vec3 level = frame.up().subtract(aim.scale(frame.up().dot(aim)));
            double clear = Mth.clamp((level.lengthSqr() - 0.05) / 0.2, 0.0, 1.0);
            if (clear > 0.0) {
                up = PlanePath.carried(up.lerp(level.normalize(), clear * (1.0 - Math.exp(-step / GUN_LEVELS))), aim);
            }
            state.up[gun] = up;
            int fired = turret.lastFired(t);
            double since = fired < 0 ? Double.MAX_VALUE : t - fired;
            double wantSpin = since < 8.0 ? BARREL_SPIN : 0.0;
            state.spin[gun] = Mth.lerp(1.0 - Math.exp(-step / (wantSpin > state.spin[gun] ? 3.0 : BARREL_DIES)),
                    state.spin[gun], wantSpin);
            state.turned[gun] += state.spin[gun] * step;
            ConstructPainter.Frame mount = ConstructPainter.Frame.of(pivot, aim, up, frame.scale());
            painter.shape(GUN, mount, 1.0, 1.0);
            painter.shape(BARRELS, mount.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, state.turned[gun]), 1.0, 1.0);
            if (since < 3.0) {
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
