package nl.tivek.multiversepowers.engine.client.ragdoll;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.physics.Blocks;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class RagdollRestTest {
    private static final int SUBSTEPS = 20;

    // A floor of whole blocks, their tops at y = 0, as the game hands them over.
    private static final Blocks FLOOR = (minX, minY, minZ, maxX, maxY, maxZ, out) -> {
        if (minY > 0.0 || maxY < -1.0) {
            return 0;
        }
        int count = 0;
        for (int x = (int) Math.floor(minX); x <= (int) Math.floor(maxX); x++) {
            for (int z = (int) Math.floor(minZ); z <= (int) Math.floor(maxZ); z++) {
                if (count * 6 + 6 > out.length) {
                    return count;
                }
                int o = count++ * 6;
                out[o] = x;
                out[o + 1] = -1.0;
                out[o + 2] = z;
                out[o + 3] = x + 1.0;
                out[o + 4] = 0.0;
                out[o + 5] = z + 1.0;
            }
        }
        return count;
    };

    // A person's body standing at (0.5, 0, 0.5) facing `yaw`, killed: it tips over and its limbs give way, as
    // Ragdolls does to a creature dying where it stands.
    private static Ragdoll fallen(long seed, RagdollProfiles.Profile profile) {
        return fallen(seed, profile, new Vec3(0.5, 0.0, 0.5));
    }

    private static Ragdoll fallen(long seed, RagdollProfiles.Profile profile, Vec3 feet) {
        Random random = new Random(seed);
        double yaw = random.nextDouble() * Math.PI * 2.0;
        Ragdoll doll = standing(yaw, profile, feet);
        double side = random.nextBoolean() ? 1.0 : -1.0;
        double ax = -Math.sin(yaw) * side;
        double az = Math.cos(yaw) * side;
        RagdollFalls.tip(doll, ax * 2.0, az * 2.0, feet);
        RagdollFalls.giveWay(doll, RandomSource.create(seed), ax, az, 1.5);
        return doll;
    }

    // A person's body gone limp dead standing upright at `feet`, facing `yaw`, nothing yet tipping it over.
    private static Ragdoll standing(double yaw, RagdollProfiles.Profile profile, Vec3 feet) {
        HumanoidModel<LivingEntity> model = new HumanoidModel<>(
                LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64).bakeRoot());
        // A model is young until its renderer says otherwise.
        model.young = false;
        List<ModelParts.Part> parts = ModelParts.of(model);
        Matrix4f drawn = new Matrix4f().translation((float) feet.x, (float) feet.y, (float) feet.z)
                .rotateY((float) (Math.PI - yaw)).scale(-1.0F, -1.0F, 1.0F).translate(0.0F, -1.501F, 0.0F);
        return RagdollBuild.build(null, model, parts, drawn, Vec3.ZERO, feet, Ragdoll.State.DEAD, false, profile,
                Vec3.ZERO);
    }

    // A body left standing as it dies, nothing tipping it over, never stays up on its limbs: it comes to lie with its
    // trunk on the floor (given way again, should it come to rest standing) before it sleeps.
    @Test
    void aDeadBodyLeftStandingGivesWayUntilItLies() {
        StringBuilder report = new StringBuilder();
        boolean upright = false;
        for (long seed = 1; seed <= 8; seed++) {
            Ragdoll doll = standing(seed * 0.8, RagdollProfiles.Profile.NONE, new Vec3(0.5, 0.0, 0.5));
            for (int t = 0; t < 300; t++) {
                doll.step(SUBSTEPS, FLOOR);
                RagdollFalls.settle(doll, t);
            }
            report.append(String.format("seed %d: lying %b, sleeping %b, gave way %d times, rested %d%n", seed,
                    doll.slumped(), doll.world.sleeping(), doll.slumps, doll.rested));
            upright |= !doll.slumped();
        }
        assertTrue(!upright, "a body stays up on its limbs:\n" + report);
    }

    // Where a limb's bone (+y in its own axes) points, seen from the body it hangs on: -z ahead, +x the model's left.
    private static Vector3d bone(Ragdoll doll, int from, int limb) {
        double[] a = new double[7];
        double[] b = new double[7];
        doll.world.pose(from, a);
        doll.world.pose(limb, b);
        Quaterniond seen = new Quaterniond(a[3], a[4], a[5], a[6]).conjugate().mul(new Quaterniond(b[3], b[4], b[5],
                b[6]));
        return seen.transform(new Vector3d(0.0, 1.0, 0.0));
    }

    // How deep any corner or face middle of box b lies inside box c.
    private static double overlap(Ragdoll doll, int b, int c) {
        double[] p = new double[3];
        double[] pc = new double[7];
        doll.world.pose(c, pc);
        Quaterniond qc = new Quaterniond(pc[3], pc[4], pc[5], pc[6]).conjugate();
        double deepest = 0.0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    doll.world.point(b, i * doll.world.half(b, 0), j * doll.world.half(b, 1),
                            k * doll.world.half(b, 2), p);
                    Vector3d local = qc.transform(new Vector3d(p[0] - pc[0], p[1] - pc[1], p[2] - pc[2]));
                    double inside = Math.min(doll.world.half(c, 0) - Math.abs(local.x), Math.min(
                            doll.world.half(c, 1) - Math.abs(local.y), doll.world.half(c, 2) - Math.abs(local.z)));
                    deepest = Math.max(deepest, inside);
                }
            }
        }
        return deepest;
    }

    // Whether joints tie bodies b and d together: two pieces of one part (at a knee and an ankle, the waist), a limb on
    // the trunk's piece it hangs from or on its shoulder blade, a blade on the chest.
    private static boolean tied(Ragdoll doll, int b, int d) {
        if (doll.partOf[b] == doll.partOf[d] && !trunk(doll, b) && !trunk(doll, d)) {
            return true;
        }
        for (int i = 0; i < doll.parts.size(); i++) {
            int[] pieces = { doll.body[i], doll.lower[i], doll.tip[i] };
            for (int k = 1; k < 3; k++) {
                if (pieces[k] >= 0 && pair(b, d, pieces[k - 1], pieces[k])) {
                    return true;
                }
            }
            if (i == doll.core) {
                continue;
            }
            int parent = doll.blade[i] >= 0 ? doll.blade[i] : doll.piece(doll.core, doll.hang[i]);
            if (pair(b, d, doll.body[i], parent)
                    || doll.blade[i] >= 0 && pair(b, d, doll.blade[i], doll.body[doll.core])) {
                return true;
            }
        }
        return false;
    }

    private static boolean pair(int b, int d, int one, int other) {
        return b == one && d == other || b == other && d == one;
    }

    // Whether body b is a piece of the trunk or a shoulder blade (which lies inside the chest).
    private static boolean trunk(Ragdoll doll, int b) {
        int core = doll.core;
        boolean blade = false;
        for (int i = 0; i < doll.parts.size(); i++) {
            blade |= doll.blade[i] == b;
        }
        return blade || b == doll.body[core] || b == doll.lower[core] || b == doll.tip[core];
    }

    @Test
    void aBodyLyingOnTheFloorStaysWhereItLies() {
        lies(RagdollProfiles.Profile.NONE);
    }

    // Two bodies killed where they stand side by side fall on each other and come to lie on each other, not in.
    @Test
    void bodiesFallingOnEachOtherLieOnEachOther() {
        double worst = 0.0;
        double worstApart = 0.0;
        StringBuilder report = new StringBuilder();
        for (long seed = 1; seed <= 12; seed++) {
            double deepest = pile(seed, true);
            double apart = pile(seed, false);
            worst = Math.max(worst, deepest);
            worstApart = Math.max(worstApart, apart);
            report.append(String.format("seed %d: in each other %.3f, each on its own %.3f%n", seed, deepest, apart));
        }
        assertTrue(worstApart > 0.15, "the bodies would not even meet:\n" + report);
        assertTrue(worst < 0.08, "bodies lie in each other:\n" + report);
    }

    // Creatures dying in a crowd stand inside each other: their bodies are never flung off.
    @Test
    void bodiesBegunInsideEachOtherAreNotFlungOff() {
        StringBuilder report = new StringBuilder();
        double worst = 0.0;
        for (long seed = 1; seed <= 8; seed++) {
            double together = crowd(seed, true);
            double alone = crowd(seed, false);
            worst = Math.max(worst, together - alone);
            report.append(String.format("seed %d: furthest %.2f, falling alone %.2f%n", seed, together, alone));
        }
        // Sliding off each other they may end up a little further apart than each falling alone (a body with feet and a
        // pelvis settles near where it fell); flung, blocks away.
        assertTrue(worst < 1.5, "a body is flung off:\n" + report);
    }

    // Three bodies falling from inside each other, keeping out of each other or each on its own: how far the middle of
    // any got from where they stood.
    private static double crowd(long seed, boolean keepOut) {
        List<Ragdoll> crowd = List.of(fallen(seed, RagdollProfiles.Profile.NONE, new Vec3(0.5, 0.0, 0.5)),
                fallen(seed + 50, RagdollProfiles.Profile.NONE, new Vec3(0.6, 0.0, 0.55)),
                fallen(seed + 90, RagdollProfiles.Profile.NONE, new Vec3(0.4, 0.0, 0.7)));
        double went = 0.0;
        double fastest = 0.0;
        for (int t = 0; t < 100; t++) {
            if (keepOut) {
                RagdollCrowd.gather(crowd, List.of());
            }
            for (Ragdoll doll : crowd) {
                if (keepOut) {
                    RagdollCrowd.among(doll);
                }
                Vec3 was = doll.coreAt(1.0);
                doll.step(SUBSTEPS, FLOOR);
                Vec3 at = doll.coreAt(1.0);
                went = Math.max(went, Math.sqrt((at.x - 0.5) * (at.x - 0.5) + (at.z - 0.5) * (at.z - 0.5)));
                fastest = Math.max(fastest, Math.sqrt((at.x - was.x) * (at.x - was.x) + (at.z - was.z) * (at.z
                        - was.z)));
            }
        }
        // Flung off, a body shoots away level with the ground; one sliding off another only falls.
        assertTrue(fastest < 0.3, "seed " + seed + ": a body shoots off at " + fastest + " blocks a tick");
        return went;
    }

    // Two bodies falling on each other, keeping out of each other or each falling on its own: how deep they end up in
    // each other.
    private static double pile(long seed, boolean crowd) {
        Ragdoll a = fallen(seed, RagdollProfiles.Profile.NONE, new Vec3(0.5, 0.0, 0.5));
        Ragdoll b = fallen(seed + 100, RagdollProfiles.Profile.NONE, new Vec3(0.8, 0.0, 0.7));
        List<Ragdoll> both = List.of(a, b);
        for (int t = 0; t < 120; t++) {
            if (crowd) {
                RagdollCrowd.gather(both, List.of());
                RagdollCrowd.among(a);
            }
            a.step(SUBSTEPS, FLOOR);
            if (crowd) {
                RagdollCrowd.among(b);
            }
            b.step(SUBSTEPS, FLOOR);
        }
        double deepest = 0.0;
        for (int i = 0; i < a.world.count(); i++) {
            for (int j = 0; j < b.world.count(); j++) {
                deepest = Math.max(deepest, Math.max(overlap(a, b, i, j), overlap(b, a, j, i)));
            }
        }
        return deepest;
    }

    // How deep any corner or face middle of body i of one doll lies inside body j of another.
    private static double overlap(Ragdoll one, Ragdoll other, int i, int j) {
        double[] p = new double[3];
        double[] pc = new double[7];
        other.world.pose(j, pc);
        Quaterniond qc = new Quaterniond(pc[3], pc[4], pc[5], pc[6]).conjugate();
        double deepest = 0.0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    one.world.point(i, x * one.world.half(i, 0), y * one.world.half(i, 1), z * one.world.half(i, 2), p);
                    Vector3d local = qc.transform(new Vector3d(p[0] - pc[0], p[1] - pc[1], p[2] - pc[2]));
                    deepest = Math.max(deepest, Math.min(other.world.half(j, 0) - Math.abs(local.x), Math.min(
                            other.world.half(j, 1) - Math.abs(local.y), other.world.half(j, 2) - Math.abs(local.z))));
                }
            }
        }
        return deepest;
    }

    // Bodies fallen every which way: legs never cross into each other, an arm hardly ever lies far behind the back
    // (only when the body lies on it, pushing it out of the chest there) and never straight back, and parts not
    // joined keep (nearly) out of each other.
    @Test
    void aFallenBodyLiesAsABodyCan() {
        double overlaps = 0.0;
        int behind = 0;
        StringBuilder report = new StringBuilder();
        int seeds = 24;
        for (long seed = 1; seed <= seeds; seed++) {
            Ragdoll doll = fallen(seed, RagdollProfiles.Profile.NONE);
            for (int t = 0; t < 120; t++) {
                doll.step(SUBSTEPS, FLOOR);
            }
            int chest = doll.body[1];
            int hips = doll.piece(1, 2);
            // The right leg hangs at -x, the left at +x: crossing, each points past the middle towards the other.
            double rightIn = bone(doll, hips, doll.body[4]).x;
            double leftIn = -bone(doll, hips, doll.body[5]).x;
            double back = Math.max(bone(doll, chest, doll.body[2]).z, bone(doll, chest, doll.body[3]).z);
            double deepest = 0.0;
            for (int b = 0; b < doll.world.count(); b++) {
                for (int d = 0; d < doll.world.count(); d++) {
                    if (b != d && !(trunk(doll, b) && trunk(doll, d)) && !tied(doll, b, d)) {
                        deepest = Math.max(deepest, overlap(doll, b, d));
                    }
                }
            }
            overlaps += deepest;
            report.append(String.format("seed %d: legs in %.2f %.2f, arm back %.2f, overlap %.3f%n", seed, rightIn,
                    leftIn, back, deepest));
            assertTrue(rightIn < 0.3 && leftIn < 0.3, "legs cross:\n" + report);
            assertTrue(back < 0.99, "an arm lies straight behind the back:\n" + report);
            behind += back > 0.87 ? 1 : 0;
        }
        assertTrue(behind <= 1, "arms lie behind the back:\n" + report);
        assertTrue(overlaps / seeds < 0.07, "parts lie in each other:\n" + report);
    }

    @Test
    void aBodyWithAStiffTrunkLyingOnTheFloorStaysWhereItLies() {
        lies(new RagdollProfiles.Profile(false, false, Map.of("waist",
                new RagdollProfiles.Tuning(Optional.empty(), Optional.of(0.0), Optional.empty()))));
    }

    // Bodies fallen every which way, left to settle 6 seconds, then watched 10 more: none may creep over the floor.
    private static void lies(RagdollProfiles.Profile profile) {
        double worst = 0.0;
        boolean awake = false;
        StringBuilder report = new StringBuilder();
        for (long seed = 1; seed <= 16; seed++) {
            Ragdoll doll = fallen(seed, profile);
            for (int t = 0; t < 120; t++) {
                doll.step(SUBSTEPS, FLOOR);
            }
            double[] before = doll.now.clone();
            int slept = -1;
            for (int t = 0; t < 200; t++) {
                doll.step(SUBSTEPS, FLOOR);
                if (slept < 0 && doll.world.sleeping()) {
                    slept = t;
                }
            }
            double most = 0.0;
            int mover = -1;
            for (int b = 0; b < doll.world.count(); b++) {
                int o = b * 7;
                double dx = doll.now[o] - before[o];
                double dy = doll.now[o + 1] - before[o + 1];
                double dz = doll.now[o + 2] - before[o + 2];
                double moved = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (moved > most) {
                    most = moved;
                    mover = b;
                }
            }
            worst = Math.max(worst, most);
            awake |= !doll.world.sleeping();
            report.append(String.format("seed %d: moved %.4f (body %d), slept at %d, sleeping %b%n", seed, most, mover,
                    slept, doll.world.sleeping()));
        }
        // A limb may still settle a hair; one creeping went a block or more.
        assertTrue(worst < 0.05 && !awake, "a lying body creeps over the floor or never sleeps:\n" + report);
    }
}
