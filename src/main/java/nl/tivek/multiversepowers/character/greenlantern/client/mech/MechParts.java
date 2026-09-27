package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.client.render.Surface;

// Building blocks for the mech's parts, all in blocks at scale 1.
final class MechParts {
    static final double GAP = 0.05;
    static final double RISE = 0.06;
    static final double CUT = 0.045;
    static final double MERGE = 0.55;
    static final double GLOW = 1.12;
    static final double FACE = 0.62;

    private MechParts() {
    }

    static Shape mirrored(Shape shape) {
        Mesh[] meshes = new Mesh[shape.meshes().length];
        for (int i = 0; i < meshes.length; i++) {
            meshes[i] = shape.meshes()[i].mirrored();
        }
        return Shape.of(meshes);
    }

    static void pair(List<Mesh> parts, Mesh right) {
        parts.add(right);
        parts.add(right.mirrored());
    }

    // A side view (z, y pairs, convex) given thickness across x, both its rims cut off.
    static Mesh side(double x0, double x1, double cut, double bright, double... zy) {
        double[] outline = new double[zy.length];
        for (int i = 0; i < zy.length; i += 2) {
            outline[i] = -zy[i];
            outline[i + 1] = zy[i + 1];
        }
        return Mesh.slab(x0, x1, cut, bright, outline).turned(0.0, 1.0, 0.0, 90.0);
    }

    // A front view (x, y pairs, convex) given depth along z, its front rim cut off.
    static Mesh front(double z0, double z1, double cut, double bright, double... xy) {
        return Mesh.plate(z0, z1, cut, bright, xy);
    }

    static Mesh hub(double radius, double x0, double x1, double bright) {
        return Mesh.cylinder(16, radius, x0, x1, bright).alongX();
    }

    static Mesh disc(double radius, double x, double thick, double bright) {
        return Mesh.cylinder(16, radius, x, x + thick, bright).alongX();
    }

    static Mesh ring(double radius, double tube, double x, double bright) {
        return Mesh.torus(20, 4, radius, tube, bright).alongX().moved(x, 0.0, 0.0);
    }

    static Mesh bar(double radius, double bright, Vec3 from, Vec3 to) {
        return Mesh.tube(false, 6, radius, bright, from, to);
    }

    static Mesh.Tiles tiles(int across, int along, int seed, double rise) {
        return new Mesh.Tiles(across, along, GAP, rise, CUT, MERGE, GLOW, seed);
    }

    // A closed body covered all over in tiles, like every big part of the clip's mech.
    static Mesh skin(Surface surface, int across, int along, int seed, double bright) {
        return Mesh.tiled(surface, true, tiles(across, along, seed, RISE), bright);
    }

    static Mesh skin(Surface surface, int across, int along, int seed, double bright, double rise) {
        return Mesh.tiled(surface, true, tiles(across, along, seed, rise), bright);
    }

    // A curved plate so thick with tiles on its outer side, for shells open on one side.
    static Mesh[] plated(Surface surface, int across, int along, int seed, double thick, double bright) {
        Surface under = surface.offset(-RISE * 0.75, false);
        return new Mesh[] { Mesh.sheet(under, false, across * 2, along * 2, thick, bright * 0.6),
                Mesh.tiles(surface, false, tiles(across, along, seed, RISE), bright) };
    }

    // A section of a lofted body: its height, half width (x), half depth (z), how far ahead its middle sits and how
    // square it is (2 round, 4 a rounded box).
    static double[] at(double y, double wide, double deep, double ahead, double square) {
        return new double[] { y, wide, deep, ahead, square };
    }

    // Where along a body lofted up y (its v from 0 to 1) it reaches height y.
    static double along(Surface surface, double y) {
        double low = 0.0;
        double high = 1.0;
        for (int k = 0; k < 40; k++) {
            double middle = (low + high) * 0.5;
            if (surface.at(0.25, middle).y < y) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return (low + high) * 0.5;
    }

    // The piece of a surface between these u and v.
    static Surface part(Surface surface, double u0, double u1, double v0, double v1) {
        return (u, v) -> surface.at(u0 + (u1 - u0) * u, v0 + (v1 - v0) * v);
    }

    // A tapered horn along a path, thinning to a point.
    static Mesh horn(double root, double bright, Vec3... path) {
        double[] radius = new double[path.length];
        for (int i = 0; i < path.length; i++) {
            radius[i] = root * (1.0 - (double) i / (path.length - 1)) + 0.012;
        }
        return Mesh.taper(6, bright, new Vec3(1.0, 0.0, 0.0), radius, path);
    }

    // Draws whole, or breaking up into its solid pieces while apart runs from 0 to 1. Its sides are kept dark, as the
    // clip's glassy green is, so the glowing rims of its tiles stand out.
    static void draw(LanternPainter painter, Shape shape, Frame frame, double bright, double apart, int seed) {
        if (apart < 0.0) {
            painter.shape(shape, frame, 1.0, bright * FACE);
        } else {
            painter.shattered(shape, frame, apart, bright * FACE, seed);
        }
    }
}
