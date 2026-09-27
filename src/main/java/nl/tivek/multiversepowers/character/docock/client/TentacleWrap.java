package nl.tivek.multiversepowers.character.docock.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.docock.ArmPayload;

// A tentacle holding a creature winds round it: it comes in to its side and coils down its body from the shoulders,
// the claw ending low on its side. Only how it is drawn; the creature hangs where the server has it.
final class TentacleWrap {
    // Coils per block of the creature's height, and the fewest and most.
    private static final double COILS = 1.1;
    private static final double FEWEST = 1.0;
    private static final double MOST = 2.2;
    private static final int PER_COIL = 24;
    // Where on its height the coil starts and ends (0 its feet, 1 the top of its head).
    private static final double HIGH = 0.85;
    private static final double LOW = 0.2;
    private static final double GAP = 0.04;

    private TentacleWrap() {
    }

    // The arm's points as drawn: the same when it holds no creature, or when the one it holds is the player who
    // looks out of it.
    static List<Vec3> around(ClientLevel level, ArmPayload arm, List<Vec3> points, double girth) {
        if (arm.heldId() < 0 || points.size() < 3) {
            return points;
        }
        Entity held = level.getEntity(arm.heldId());
        Minecraft minecraft = Minecraft.getInstance();
        if (held == null || held == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
            return points;
        }
        Vec3 middle = points.get(points.size() - 1);
        double height = held.getBbHeight();
        double radius = held.getBbWidth() * 0.5 + girth + GAP;
        double bottom = middle.y - height * 0.5;
        double top = bottom + height * HIGH;
        double low = bottom + height * LOW;
        int keep = points.size();
        while (keep > 2 && inside(points.get(keep - 1), middle, radius + girth * 2.0, bottom - 0.3, top + 0.5)) {
            keep--;
        }
        if (keep < 2) {
            return points;
        }
        Vec3 in = points.get(keep - 1);
        Vec3 before = points.get(keep - 2);
        double start = Math.atan2(in.z - middle.z, in.x - middle.x);
        // Coils on the way the arm comes in, so it bends into the coil without a kink.
        double comes = (in.x - before.x) * (middle.z - in.z) - (in.z - before.z) * (middle.x - in.x);
        double way = comes >= 0.0 ? -1.0 : 1.0;
        double turns = Mth.clamp(height * COILS, FEWEST, MOST);
        int steps = Math.max(8, (int) Math.round(turns * PER_COIL));
        List<Vec3> wound = new ArrayList<>(keep + steps + 1);
        wound.addAll(points.subList(0, keep));
        for (int k = 0; k <= steps; k++) {
            double u = (double) k / steps;
            double angle = start + way * Math.PI * 2.0 * turns * u;
            wound.add(new Vec3(middle.x + Math.cos(angle) * radius, Mth.lerp(u, top, low),
                    middle.z + Math.sin(angle) * radius));
        }
        return wound;
    }

    private static boolean inside(Vec3 point, Vec3 middle, double radius, double bottom, double top) {
        double dx = point.x - middle.x;
        double dz = point.z - middle.z;
        return dx * dx + dz * dz < radius * radius && point.y > bottom && point.y < top;
    }
}
