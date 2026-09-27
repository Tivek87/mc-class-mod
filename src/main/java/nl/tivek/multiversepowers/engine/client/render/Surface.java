package nl.tivek.multiversepowers.engine.client.render;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Vectors;

// A surface given point by point: u runs across it (round it when it is closed), v along it, both from 0 to 1. Its
// outside is the side that (point further along v) x (point further across u) points to.
@FunctionalInterface
public interface Surface {
    Vec3 at(double u, double v);

    // Round the y axis through (radius, y) pairs from bottom to top, smooth between them; u turns from +x towards +z.
    static Surface lathe(double... profile) {
        double[][] sections = new double[profile.length / 2][];
        for (int i = 0; i < sections.length; i++) {
            sections[i] = new double[] { profile[2 * i + 1], profile[2 * i], profile[2 * i], 0.0 };
        }
        return loft(sections);
    }

    // Round the y axis through sections of { y, half width along x, half depth along z, how far along z its middle
    // sits, and optionally how square it is (2 round, 4 a rounded box) }, smooth between them.
    static Surface loft(double[]... sections) {
        SurfacePath path = new SurfacePath(sections);
        return (u, v) -> {
            double[] s = path.at(v);
            double angle = Math.PI * 2.0 * u;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double square = s[4];
            if (square != 2.0) {
                double power = 2.0 / square;
                cos = Math.copySign(Math.pow(Math.abs(cos), power), cos);
                sin = Math.copySign(Math.pow(Math.abs(sin), power), sin);
            }
            return new Vec3(cos * s[1], s[0], s[3] + sin * s[2]);
        };
    }

    // Which way is out at (u, v): the v step crossed with the u step, found a little to one side at a pole.
    default Vec3 outward(double u, double v, boolean round) {
        double nudge = 1.0E-3;
        for (double shift = 0.0; shift <= 0.05; shift += 0.01) {
            double at = v < 0.5 ? v + shift : v - shift;
            double va = Math.max(0.0, at - nudge);
            double vb = Math.min(1.0, at + nudge);
            double ua = round ? u - nudge : Math.max(0.0, u - nudge);
            double ub = round ? u + nudge : Math.min(1.0, u + nudge);
            Vec3 alongStep = this.at(u, vb).subtract(this.at(u, va));
            Vec3 acrossStep = this.at(ub, at).subtract(this.at(ua, at));
            Vec3 out = alongStep.cross(acrossStep);
            if (out.lengthSqr() > 1.0E-20) {
                return out.normalize();
            }
        }
        return new Vec3(0.0, v < 0.5 ? -1.0 : 1.0, 0.0);
    }

    // The surface pushed out along its outward by so much (in by a negative amount).
    default Surface offset(double by, boolean round) {
        return (u, v) -> this.at(u, v).add(this.outward(u, v, round).scale(by));
    }

    // The same surface turned about the axis (x, y, z) through the origin by so many degrees.
    default Surface turned(double x, double y, double z, double degrees) {
        Vec3 axis = new Vec3(x, y, z).normalize();
        double angle = Math.toRadians(degrees);
        return (u, v) -> Vectors.spin(this.at(u, v), axis, angle);
    }

    default Surface moved(double x, double y, double z) {
        return (u, v) -> this.at(u, v).add(x, y, z);
    }

    // The same surface stretched along its own axes, by positive amounts (a mirror would turn it inside out).
    default Surface scaled(double x, double y, double z) {
        return (u, v) -> this.at(u, v).multiply(x, y, z);
    }
}
