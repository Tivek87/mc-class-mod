package nl.tivek.multiversepowers.engine.client.ragdoll;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;

// Limp bodies keep out of each other: every body lying or flying about is handed, as boxes, to the bodies near it
// before they step, and each pushes itself out of the others. One getting up is drawn in poses of its own and is left
// out, as is one sinking away.
final class RagdollCrowd {
    // A body keeps out of those whose middle is this near its own (blocks).
    private static final double NEAR = 4.0;
    private static final int BOX = RigidWorld.BOX;
    private static final int MOST = 256;

    private static double[] all = new double[64 * BOX];
    private static final double[] NEARBY = new double[MOST * BOX];
    private static final double[] POSE = new double[7];
    private static final double[] SPEED = new double[6];
    private static int count;

    private RagdollCrowd() {
    }

    // Every body's boxes as they are at the start of this tick.
    static void gather(Iterable<Ragdoll> live, Iterable<Ragdoll> corpses) {
        count = 0;
        for (Ragdoll doll : live) {
            add(doll, doll.phase != Ragdoll.Phase.UP);
        }
        for (Ragdoll doll : corpses) {
            add(doll, doll.sunk < 0);
        }
    }

    private static void add(Ragdoll doll, boolean solid) {
        doll.crowdFrom = count;
        int n = solid ? doll.world.count() : 0;
        if ((count + n) * BOX > all.length) {
            double[] more = new double[Math.max(all.length * 2, (count + n) * BOX)];
            System.arraycopy(all, 0, more, 0, count * BOX);
            all = more;
        }
        for (int b = 0; b < n; b++) {
            int e = count++ * BOX;
            doll.world.pose(b, POSE);
            System.arraycopy(POSE, 0, all, e, 7);
            all[e + 7] = doll.world.half(b, 0);
            all[e + 8] = doll.world.half(b, 1);
            all[e + 9] = doll.world.half(b, 2);
            // A body lying still does not move on; one flying or tumbling does, as its world steps this tick.
            if (doll.world.sleeping()) {
                all[e + 10] = 0.0;
                all[e + 11] = 0.0;
                all[e + 12] = 0.0;
            } else {
                doll.world.velocity(b, SPEED);
                System.arraycopy(SPEED, 0, all, e + 10, 3);
            }
        }
        doll.crowdTo = count;
    }

    // The other bodies' boxes near this one, for its next step.
    static void among(Ragdoll doll) {
        Vec3 at = doll.coreAt(1.0);
        int kept = 0;
        for (int k = 0; k < count && kept < MOST; k++) {
            if (k >= doll.crowdFrom && k < doll.crowdTo) {
                continue;
            }
            int e = k * BOX;
            double dx = all[e] - at.x;
            double dy = all[e + 1] - at.y;
            double dz = all[e + 2] - at.z;
            if (dx * dx + dy * dy + dz * dz <= NEAR * NEAR) {
                System.arraycopy(all, e, NEARBY, kept++ * BOX, BOX);
            }
        }
        doll.world.others(NEARBY, kept);
    }
}
