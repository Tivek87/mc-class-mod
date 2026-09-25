package nl.tivek.multiversepowers.character.greenlantern.client.slam;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamPainter.Moment;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

final class SlamMoney {
    private static final int COINS = 12;
    private static final int NOTES = 7;
    private static final int BARS = 2;
    private static final Shape COIN = Shape.of(Mesh.lathe(12, 1.2, 0.0, -0.024, 0.07, -0.024, 0.08, -0.018, 0.115,
            -0.018, 0.125, -0.008, 0.125, 0.008, 0.115, 0.018, 0.08, 0.018, 0.07, 0.024, 0.0, 0.024));
    private static final Shape NOTE = new Shape(new double[][] { { -0.18, -0.006, -0.085, 0.18, 0.006, 0.085, 1.05 } },
            Mesh.cylinder(8, 0.05, -0.012, 0.012, 1.3));
    private static final Shape GOLD = Shape.of(bar());

    private SlamMoney() {
    }

    static Mesh bar() {
        return Mesh.prism(-0.075, 0.075, 1.2, -0.15, -0.045, 0.15, -0.045, 0.11, 0.045, -0.11, 0.045);
    }

    private record Loot(Vec3 at, Vec3 axis, double angle) {
    }

    static void money(LanternPainter painter, Frame safe, Moment m) {
        double since = m.since() - 1.6;
        if (since <= 0.0) {
            return;
        }
        double sink = Math.max(0.0, m.t() - m.burst()) * 0.05;
        for (int k = 0; k < COINS + NOTES + BARS; k++) {
            double dt = since - 0.32 * k - 0.3 * Noise.of(k, 61, 0);
            if (dt <= 0.0) {
                continue;
            }
            Vec3 start = new Vec3(Mth.lerp(Noise.of(k, 61, 1), -0.42, 0.42),
                    Mth.lerp(Noise.of(k, 61, 2), 0.45, 1.35), -0.35);
            Vec3 thrown = new Vec3((Noise.of(k, 61, 3) - 0.5) * 0.10,
                    0.07 + 0.07 * Noise.of(k, 61, 4), -(0.05 + 0.05 * Noise.of(k, 61, 5)));
            Loot loot;
            Shape shape;
            if (k < COINS) {
                loot = coin(k, start, thrown, dt);
                shape = COIN;
            } else if (k < COINS + NOTES) {
                loot = note(k, start, thrown, dt);
                shape = NOTE;
            } else {
                loot = gold(k, start, thrown, dt);
                shape = GOLD;
            }
            Vec3 axis = safe.right().scale(loot.axis().x).add(safe.up().scale(loot.axis().y))
                    .add(safe.forward().scale(loot.axis().z)).normalize();
            painter.shape(shape, SlamPainter.loose(safe, loot.at().subtract(0.0, sink, 0.0), axis, loot.angle()), 1.0,
                    1.0);
        }
    }

    private static Vec3 tumble(int k) {
        return new Vec3(Noise.of(k, 62, 0) - 0.5, Noise.of(k, 62, 1) - 0.5,
                Noise.of(k, 62, 2) - 0.5).add(0.0, 0.0, 0.01).normalize();
    }

    private static Loot coin(int k, Vec3 start, Vec3 thrown, double dt) {
        double g = 0.03;
        double floor = 0.024;
        double turn = 0.45 + 0.3 * Noise.of(k, 63, 0);
        double land = (thrown.y + Math.sqrt(thrown.y * thrown.y + 2.0 * g * (start.y - floor))) / g;
        if (dt < land) {
            return new Loot(start.add(thrown.scale(dt)).subtract(0.0, 0.5 * g * dt * dt, 0.0), tumble(k), turn * dt);
        }
        double up = 0.28 * (g * land - thrown.y);
        double hop = Math.max(0.5, 2.0 * up / g);
        double after = dt - land;
        double air = Math.min(after, hop);
        double skid = Math.min(after, hop + 3.0);
        double height = floor + Math.max(0.0, up * air - 0.5 * g * air * air);
        Vec3 at = new Vec3(start.x + thrown.x * (land + 0.4 * skid), height, start.z + thrown.z * (land + 0.4 * skid));
        return new Loot(at, tumble(k), turn * land * (1.0 - Ease.smooth(after / hop)));
    }

    private static Loot note(int k, Vec3 start, Vec3 thrown, double dt) {
        double floor = 0.008;
        double phase = Noise.of(k, 64, 0) * Math.PI * 2.0;
        double landed = dt;
        for (double s = 0.0; s < dt; s += 0.25) {
            if (noteHeight(start, thrown, s) <= floor) {
                landed = s;
                break;
            }
        }
        double s = Math.min(dt, landed);
        double spread = (1.0 - Math.exp(-0.2 * s)) / 0.2;
        double sway = 0.09 * Math.sin(0.9 * s + phase) * Math.min(1.0, s / 3.0);
        Vec3 at = new Vec3(start.x + thrown.x * spread + sway, Math.max(floor, noteHeight(start, thrown, s)),
                start.z + thrown.z * spread);
        double flutter = 0.9 * Math.sin(1.3 * s + phase) * (1.0 - Ease.smooth(dt - landed));
        return new Loot(at, new Vec3(1.0, 0.0, 0.35).normalize(), flutter);
    }

    private static double noteHeight(Vec3 start, Vec3 thrown, double s) {
        double spread = (1.0 - Math.exp(-0.2 * s)) / 0.2;
        return start.y + thrown.y * spread - 0.02 * (s - spread);
    }

    private static Loot gold(int k, Vec3 start, Vec3 thrown, double dt) {
        double g = 0.04;
        double floor = 0.045;
        double land = (thrown.y + Math.sqrt(thrown.y * thrown.y + 2.0 * g * (start.y - floor))) / g;
        Vec3 axis = new Vec3(1.0, 0.0, 0.0);
        if (dt < land) {
            return new Loot(start.add(thrown.scale(dt)).subtract(0.0, 0.5 * g * dt * dt, 0.0), axis, 0.25 * dt);
        }
        double skid = Math.min(dt - land, 2.0);
        Vec3 at = new Vec3(start.x + thrown.x * (land + 0.3 * skid), floor, start.z + thrown.z * (land + 0.3 * skid));
        double settle = 1.0 - Ease.smooth((dt - land) / 1.5);
        return new Loot(at, axis, 0.25 * land * settle);
    }
}
