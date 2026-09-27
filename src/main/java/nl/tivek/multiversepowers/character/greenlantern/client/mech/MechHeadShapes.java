package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.client.render.Surface;

// The helmet round its middle at scale 1 (it is drawn at MechScript.HEAD_SCALE), its face at z: a heavy knight's helm
// as in the clip, a squared-off crown of big solid plates under a tall fin, a thick brow jutting over the eye slits, a
// faceted mask down to a pointed chin with a grille for a mouth, cheek guards swept back, and horns from the temples.
final class MechHeadShapes {
    static final double BOTTOM = -MechScript.HEAD_UP / MechScript.HEAD_SCALE;
    static final double EYE_Y = 0.16;
    static final double EYE_Z = 0.6;
    static final double[] EYE_X = { 0.12, 0.36 };
    private static final Surface CROWN = Surface.loft(MechParts.at(BOTTOM + 0.02, 0.0, 0.0, -0.04, 2.0),
            MechParts.at(BOTTOM + 0.08, 0.3, 0.34, -0.04, 3.0), MechParts.at(-0.38, 0.46, 0.5, -0.02, 3.2),
            MechParts.at(-0.02, 0.54, 0.57, 0.0, 3.4), MechParts.at(0.36, 0.56, 0.6, -0.02, 3.4),
            MechParts.at(0.68, 0.5, 0.54, -0.05, 3.1), MechParts.at(0.9, 0.34, 0.38, -0.08, 2.7),
            MechParts.at(0.99, 0.0, 0.0, -0.1, 2.0));

    static final Shape HEAD = Shape.of(head());

    private MechHeadShapes() {
    }

    private static Mesh[] head() {
        double low = BOTTOM;
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(CROWN, 8, 4, 101, 1.0, 0.05));
        // The brow: two thick wedges meeting low over the nose, jutting out over the eyes.
        MechParts.pair(m, MechParts.front(0.46, 0.74, 0.05, 1.12, 0.0, 0.2, 0.44, 0.3, 0.42, 0.42, 0.0, 0.38));
        MechParts.pair(m, MechParts.front(0.7, 0.78, 0.02, 1.3, 0.0, 0.2, 0.4, 0.29, 0.39, 0.32, 0.0, 0.23));
        // The mask from under the brow to the chin, and a ridge down its middle.
        m.add(MechParts.front(0.4, 0.66, 0.05, 1.04, -0.45, 0.12, 0.45, 0.12, 0.37, -0.24, 0.15, -0.54, -0.15,
                -0.54, -0.37, -0.24));
        m.add(MechParts.side(-0.05, 0.05, 0.02, 1.2, 0.62, 0.1, 0.71, 0.06, 0.71, -0.42, 0.62, -0.5));
        for (int k = 0; k < 3; k++) {
            double y = -0.2 - 0.09 * k;
            double half = 0.22 - 0.045 * k;
            m.add(Mesh.bevel(-half, y - 0.022, 0.64, half, y + 0.022, 0.7, 0.01, 1.3));
        }
        m.add(MechParts.front(0.5, 0.7, 0.03, 1.1, -0.16, -0.46, 0.16, -0.46, 0.1, -0.6, -0.1, -0.6));
        // Cheek guards swept back from the mask, and plates over the ears.
        MechParts.pair(m, MechParts.side(0.44, 0.6, 0.04, 1.02, 0.52, 0.12, 0.58, -0.3, 0.2, -0.46, 0.04, 0.02));
        MechParts.pair(m, MechParts.disc(0.2, 0.53, 0.08, 1.08).moved(0.0, 0.1, -0.08));
        MechParts.pair(m, MechParts.ring(0.13, 0.03, 0.6, 1.35).moved(0.0, 0.1, -0.08));
        // The fin over the crown from brow to nape.
        m.add(MechParts.side(-0.07, 0.07, 0.03, 1.15, 0.5, 0.7, 0.3, 1.14, -0.1, 1.24, -0.46, 0.98, -0.56, 0.74));
        m.add(MechParts.side(-0.025, 0.025, 0.01, 1.35, 0.28, 1.12, -0.08, 1.22, -0.1, 1.28, 0.3, 1.18));
        // Horns from the temples, swept back and up.
        MechParts.pair(m, MechParts.horn(0.1, 1.15, new Vec3(0.48, 0.5, -0.02), new Vec3(0.64, 0.66, -0.18),
                new Vec3(0.76, 0.9, -0.36), new Vec3(0.8, 1.14, -0.52)));
        // The neck guard: plates stepping down the back of the head.
        m.add(MechParts.side(-0.38, 0.38, 0.04, 1.0, -0.5, 0.0, -0.66, -0.36, -0.52, -0.62, -0.3, -0.3));
        m.add(Mesh.torus(20, 4, 0.3, 0.055, 1.4).moved(0.0, low + 0.08, 0.0));
        return m.toArray(Mesh[]::new);
    }
}
