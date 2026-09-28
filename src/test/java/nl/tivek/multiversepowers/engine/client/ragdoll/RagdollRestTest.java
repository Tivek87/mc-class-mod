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
        HumanoidModel<LivingEntity> model = new HumanoidModel<>(
                LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64).bakeRoot());
        // A model is young until its renderer says otherwise.
        model.young = false;
        List<ModelParts.Part> parts = ModelParts.of(model);
        Random random = new Random(seed);
        double yaw = random.nextDouble() * Math.PI * 2.0;
        Vec3 feet = new Vec3(0.5, 0.0, 0.5);
        Matrix4f drawn = new Matrix4f().translation((float) feet.x, (float) feet.y, (float) feet.z)
                .rotateY((float) (Math.PI - yaw)).scale(-1.0F, -1.0F, 1.0F).translate(0.0F, -1.501F, 0.0F);
        Ragdoll doll = RagdollBuild.build(null, model, parts, drawn, Vec3.ZERO, feet, Ragdoll.State.DEAD, false,
                profile, Vec3.ZERO);
        double side = random.nextBoolean() ? 1.0 : -1.0;
        double ax = -Math.sin(yaw) * side;
        double az = Math.cos(yaw) * side;
        doll.tip(ax * 2.0, 0.0, az * 2.0, feet);
        doll.giveWay(RandomSource.create(seed), ax, az, 1.5);
        return doll;
    }

    @Test
    void aBodyLyingOnTheFloorStaysWhereItLies() {
        lies(RagdollProfiles.Profile.NONE);
    }

    @Test
    void aBodyWithAStiffTrunkLyingOnTheFloorStaysWhereItLies() {
        lies(new RagdollProfiles.Profile(false, false, Map.of("waist",
                new RagdollProfiles.Tuning(Optional.empty(), Optional.of(0.0), Optional.empty()))));
    }

    // Bodies fallen every which way, left to lie 4 seconds, then watched 10 more: none may creep over the floor.
    private static void lies(RagdollProfiles.Profile profile) {
        double worst = 0.0;
        boolean awake = false;
        StringBuilder report = new StringBuilder();
        for (long seed = 1; seed <= 16; seed++) {
            Ragdoll doll = fallen(seed, profile);
            for (int t = 0; t < 80; t++) {
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
        assertTrue(worst < 0.02 && !awake, "a lying body creeps over the floor or never sleeps:\n" + report);
    }
}
