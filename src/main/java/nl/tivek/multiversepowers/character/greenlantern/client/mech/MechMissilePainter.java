package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// A missile of the mech's missile arm: a short six-sided seeker of hard light with a cut-crystal warhead, three
// glowing bands round its body, three swept fins at its tail and three canards behind the warhead, rolling as it
// flies; behind it a short flame and two strands of light winding round each other.
public final class MechMissilePainter {
    private static final double SCALE = 0.62;
    private static final double TAIL = -1.75;
    private static final double ROLL = 0.55;
    private static final int STRANDS = 12;
    private static final double STEP = 0.9;
    private static final double SLIM_WIDE = 0.65;
    private static final double SLIM_LONG = 1.7;
    private static final ConstructPainter.Shape SHAPE = ConstructPainter.Shape.of(shape());

    private MechMissilePainter() {
    }

    public static void draw(LanternPainter painter, Vec3 at, Vec3 nose, double burning, double size) {
        draw(painter, at, nose, burning, size, false);
    }

    // `size`: 1 for the mech's own missiles, less for smaller rockets (the Rocket Launcher's); `slim` drawn longer and
    // thinner, as the launcher's rockets are.
    public static void draw(LanternPainter painter, Vec3 at, Vec3 nose, double burning, double size, boolean slim) {
        Vec3 forward = nose.lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : nose.normalize();
        Vec3[] across = Vectors.across(forward);
        double roll = Math.max(0.0, burning) * ROLL;
        Vec3 up = across[0].scale(Math.cos(roll)).add(across[1].scale(Math.sin(roll)));
        ConstructPainter.Frame frame = ConstructPainter.Frame.of(at, forward, up, SCALE * size);
        if (slim) {
            frame = frame.stretched(SLIM_WIDE, SLIM_WIDE, SLIM_LONG);
        }
        painter.ambient(0.35);
        painter.shape(SHAPE, frame, 1.0, 1.2);
        painter.ambient(0.0);
        if (burning < 0.0) {
            return;
        }
        Vec3 tail = frame.at(0.0, 0.0, TAIL);
        double lit = Ease.smooth(burning / 2.0);
        if (burning < 3.0) {
            double flash = 1.0 - burning / 3.0;
            painter.flare(tail, (1.4 * flash + 0.5) * size, flash);
        }
        painter.exhaust(tail, forward.scale(-1.0), 1.6 * size, 0.16 * size, lit);
        for (int side = 0; side < 2; side++) {
            Vec3 last = tail;
            for (int k = 1; k <= STRANDS; k++) {
                double turn = roll * 0.6 + k * 0.7 + side * Math.PI;
                double wide = (0.12 + 0.03 * k) * size;
                Vec3 next = tail.subtract(forward.scale(STEP * k * size)).add(across[0].scale(Math.cos(turn) * wide))
                        .add(across[1].scale(Math.sin(turn) * wide));
                double fade = 1.0 - (double) k / (STRANDS + 1);
                painter.edge(last, next, 0.16 * fade * size, 0.8 * fade * lit);
                last = next;
            }
        }
    }

    private static Mesh[] shape() {
        List<Mesh> parts = new ArrayList<>();
        double r = 0.3;
        // The body: six-sided, tapering to the nozzle.
        parts.add(Mesh.loft(6, 1.0, new double[] { 1.05, r * 0.7, r * 0.7, 0.0 },
                new double[] { 0.85, r, r, 0.0 }, new double[] { -1.3, r, r, 0.0 },
                new double[] { -1.6, r * 0.72, r * 0.72, 0.0 }, new double[] { -1.75, r * 0.78, r * 0.78, 0.0 }));
        // The warhead: a four-sided crystal, bright.
        parts.add(Mesh.loft(4, 1.9, new double[] { 2.0, 0.0, 0.0, 0.0 }, new double[] { 1.45, r * 0.95, r * 0.95, 0.0 },
                new double[] { 1.0, r * 0.6, r * 0.6, 0.0 }));
        for (double z : new double[] { 0.55, 0.05, -0.45 }) {
            parts.add(Mesh.torus(12, 4, r + 0.03, 0.035, 1.7).alongZ().moved(0.0, 0.0, z));
        }
        for (int k = 0; k < 3; k++) {
            parts.add(Mesh.wing(r * 1.8, -1.75, -0.9, -1.85, -1.5, 0.0, 0.07, 0.03, 1.1).moved(r * 0.75, 0.0, 0.0)
                    .turned(0.0, 0.0, 1.0, 120.0 * k));
            parts.add(Mesh.wing(r * 0.8, 0.55, 0.9, 0.62, 0.8, 0.0, 0.05, 0.02, 1.3).moved(r * 0.75, 0.0, 0.0)
                    .turned(0.0, 0.0, 1.0, 60.0 + 120.0 * k));
        }
        return parts.toArray(Mesh[]::new);
    }
}
