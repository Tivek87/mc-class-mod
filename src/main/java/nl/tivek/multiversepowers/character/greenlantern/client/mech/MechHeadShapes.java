package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.client.render.Surface;

// The helmet round its middle at scale 1 (it is drawn at MechScript.HEAD_SCALE), its face at z: a tall egg of big
// smooth panels, broad at the crown and narrow at the chin, a face plate running to a point at the chin, a crest down
// its middle from the chin over the top, and two thin horns rising from the crown.
final class MechHeadShapes {
    static final double BOTTOM = -MechScript.HEAD_UP / MechScript.HEAD_SCALE;
    private static final Surface DOME = Surface.loft(MechParts.at(BOTTOM - 0.05, 0.0, 0.0, 0.08, 2.0),
            MechParts.at(BOTTOM + 0.03, 0.28, 0.34, 0.08, 2.0), MechParts.at(-0.38, 0.42, 0.52, 0.06, 2.1),
            MechParts.at(-0.05, 0.52, 0.62, 0.03, 2.2), MechParts.at(0.32, 0.55, 0.63, 0.0, 2.2),
            MechParts.at(0.62, 0.48, 0.55, -0.04, 2.1), MechParts.at(0.85, 0.3, 0.36, -0.08, 2.0),
            MechParts.at(0.97, 0.0, 0.0, -0.1, 2.0));
    private static final double[][] CREST = { { 0.4, BOTTOM + 0.1 }, { 0.58, -0.3 }, { 0.67, 0.05 }, { 0.66, 0.35 },
            { 0.52, 0.66 }, { 0.12, 0.94 }, { -0.3, 0.9 }, { -0.56, 0.66 } };

    static final Shape HEAD = Shape.of(head());

    private MechHeadShapes() {
    }

    private static Mesh[] head() {
        double low = BOTTOM;
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.tiled(DOME, true, new Mesh.Tiles(9, 4, MechParts.GAP, 0.045, MechParts.CUT, 0.85, MechParts.GLOW,
                101), 1.0));
        m.addAll(List.of(MechParts.plated(face(DOME.offset(0.04, true)), 2, 3, 102, 0.12, 1.06)));
        for (int k = 0; k + 1 < CREST.length; k++) {
            double[] a = CREST[k];
            double[] b = CREST[k + 1];
            double[] outA = out(a, 0.08);
            double[] outB = out(b, 0.08);
            m.add(MechParts.side(-0.05, 0.05, 0.022, 1.2, a[0], a[1], b[0], b[1], outB[0], outB[1], outA[0],
                    outA[1]));
        }
        MechParts.pair(m, MechParts.horn(0.07, 1.2, new Vec3(0.3, 0.72, 0.02), new Vec3(0.42, 0.98, -0.04),
                new Vec3(0.55, 1.22, -0.12), new Vec3(0.68, 1.45, -0.22)));
        MechParts.pair(m, MechParts.disc(0.17, 0.5, 0.06, 1.12).moved(0.0, 0.1, -0.05));
        MechParts.pair(m, MechParts.ring(0.12, 0.028, 0.55, 1.35).moved(0.0, 0.1, -0.05));
        m.add(Mesh.torus(20, 4, 0.3, 0.055, 1.4).moved(0.0, low + 0.05, 0.08));
        return m.toArray(Mesh[]::new);
    }

    // The face plate: the front of the dome from the brow down to the chin, narrowing to a point at the bottom.
    private static Surface face(Surface dome) {
        double brow = 0.66;
        double chin = 0.1;
        return (u, v) -> {
            double width = 0.05 + 0.1 * v;
            return dome.at(0.25 + (u - 0.5) * 2.0 * width, chin + (brow - chin) * v);
        };
    }

    // A point of the crest's line pushed so far out from the middle of the head.
    private static double[] out(double[] zy, double by) {
        double z = zy[0];
        double y = zy[1] - 0.15;
        double length = Math.sqrt(z * z + y * y);
        return new double[] { zy[0] + z / length * by, zy[1] + y / length * by };
    }
}
