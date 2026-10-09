package nl.tivek.multiversepowers.character.greenlantern.client.mech.shape;

import java.util.ArrayList;
import java.util.List;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.client.render.mesh.Surface;

// The helmet round its middle at scale 1 (it is drawn at MechScript.HEAD_SCALE), its face at z: a jaeger's small head
// sunk between its shoulders, a rounded crown under a low crest, a heavy brow over one long glowing visor slit, a jaw
// plate tapering to the chin, cheek plates and vanes swept back from its sides.
public final class MechHeadShapes {
    static final double BOTTOM = -MechScript.HEAD_UP / MechScript.HEAD_SCALE;
    public static final double EYE_Y = 0.05;
    public static final double EYE_Z = 0.62;
    public static final double[] EYE_X = { 0.05, 0.4 };
    private static final Surface CROWN = Surface.loft(MechParts.at(BOTTOM + 0.02, 0.0, 0.0, -0.02, 2.0),
            MechParts.at(BOTTOM + 0.1, 0.4, 0.42, -0.02, 3.2), MechParts.at(-0.3, 0.55, 0.56, 0.0, 3.6),
            MechParts.at(0.1, 0.58, 0.6, 0.0, 3.4), MechParts.at(0.4, 0.52, 0.55, -0.03, 3.0),
            MechParts.at(0.58, 0.32, 0.38, -0.06, 2.6), MechParts.at(0.64, 0.0, 0.0, -0.08, 2.0));

    public static final Shape HEAD = Shape.of(head());

    private MechHeadShapes() {
    }

    private static Mesh[] head() {
        double low = BOTTOM;
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(CROWN, 10, 4, 101, 1.0, 0.05));
        // The visor: a dark band across the face that the eyes light (MechLight), under a heavy brow.
        m.add(Mesh.bevel(-0.5, -0.03, 0.44, 0.5, 0.14, 0.6, 0.02, 0.7));
        m.add(MechParts.front(0.5, 0.7, 0.05, 1.12, -0.54, 0.42, 0.54, 0.42, 0.48, 0.14, -0.48, 0.14));
        m.add(MechParts.front(0.66, 0.74, 0.02, 1.3, -0.3, 0.36, 0.3, 0.36, 0.26, 0.2, -0.26, 0.2));
        // The jaw plate down to the chin, with vents across it and a ridge down its middle.
        m.add(MechParts.front(0.44, 0.64, 0.05, 1.04, -0.46, -0.04, 0.46, -0.04, 0.36, -0.36, 0.14, -0.55, -0.14,
                -0.55, -0.36, -0.36));
        for (int k = 0; k < 3; k++) {
            double y = -0.16 - 0.1 * k;
            double half = 0.26 - 0.05 * k;
            m.add(Mesh.bevel(-half, y - 0.025, 0.62, half, y + 0.025, 0.68, 0.01, 1.3));
        }
        // Cheek plates and vanes swept back from the sides.
        MechParts.pair(m, MechParts.side(0.5, 0.62, 0.04, 1.02, -0.1, -0.45, 0.5, -0.4, 0.58, 0.05, 0.0, 0.1));
        MechParts.pair(m, MechParts.side(0.54, 0.64, 0.03, 1.12, -0.55, -0.1, 0.2, 0.05, 0.15, 0.3, -0.75, 0.42));
        // A low crest from brow to nape, two small lamps either side of it and the guard down the back of the neck.
        m.add(MechParts.side(-0.06, 0.06, 0.02, 1.2, -0.5, 0.5, 0.35, 0.52, 0.25, 0.7, -0.4, 0.66));
        MechParts.pair(m, Mesh.bevel(0.24, 0.5, 0.18, 0.36, 0.6, 0.34, 0.02, 1.5));
        m.add(MechParts.side(-0.36, 0.36, 0.04, 1.0, -0.62, -0.5, -0.3, -0.58, -0.42, -0.05, -0.6, -0.12));
        m.add(Mesh.torus(20, 4, 0.32, 0.06, 1.4).moved(0.0, low + 0.08, 0.0));
        return m.toArray(Mesh[]::new);
    }
}
