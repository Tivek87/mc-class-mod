package nl.tivek.multiversepowers.engine.client.ragdoll;

import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A thrown creature getting up from where it lies. A person first pushes up onto one knee, the other foot planted
// and a hand on the ground, then stands; anything else rises straight into its own pose. Every part moves with the
// trunk it hangs from (blended in the trunk's frame), so arms and legs stay on the body the whole way up. Model space,
// in pixels: y runs down, -z ahead.
final class GetUp {
    // The most parts a model a ragdoll moves can have.
    static final int MOST = 32;
    static final int PERSON_TICKS = 32;
    static final int OTHER_TICKS = 20;
    private static final String[] NAMES = { "head", "body", "right_arm", "left_arm", "right_leg", "left_leg" };
    // The kneel, for each part as named above: where it hangs, its turn (x, y, z as a model part turns) and how far its
    // elbow or knee is bent. The hips sink six pixels, the trunk leans over the forward knee.
    private static final float[][] KNEEL = {
            { 0.0F, 6.73F, -4.12F, -0.25F, 0.0F, 0.0F, 0.0F },
            { 0.0F, 6.73F, -4.12F, 0.35F, 0.0F, 0.0F, 0.0F },
            { -5.0F, 8.61F, -3.44F, -0.62F, 0.1F, 0.1F, 0.25F },
            { 5.0F, 8.61F, -3.44F, -0.45F, -0.1F, -0.2F, 0.7F },
            { -1.9F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5708F },
            { 1.9F, 18.0F, 0.0F, -1.5708F, 0.0F, 0.0F, 1.5708F } };
    // Onto the knee by this far into getting up, and from it to standing from that far on.
    private static final float KNEELED = 0.45F;
    private static final float STANDS = 0.52F;

    private static final Vector3f[] MID_POS = vectors();
    private static final Quaternionf[] MID_ROT = turns();
    private static final Vector3f[] K_POS = vectors();
    private static final Quaternionf[] K_ROT = turns();
    private static final Quaternionf[] K_KNEE = turns();
    private static final Quaternionf[] MID_KNEE = turns();
    private static final Vector3f V = new Vector3f();
    private static final Vector3f W = new Vector3f();
    private static final Quaternionf CORE = new Quaternionf();
    private static final Quaternionf A = new Quaternionf();
    private static final Quaternionf B = new Quaternionf();
    private static final Vector3f CORE_POS = new Vector3f();
    private static final Quaternionf BACK_A = new Quaternionf();
    private static final Quaternionf BACK_B = new Quaternionf();

    private GetUp() {
    }

    static Vector3f[] vectors() {
        Vector3f[] all = new Vector3f[MOST];
        for (int i = 0; i < all.length; i++) {
            all[i] = new Vector3f();
        }
        return all;
    }

    static Quaternionf[] turns() {
        Quaternionf[] all = new Quaternionf[MOST];
        for (int i = 0; i < all.length; i++) {
            all[i] = new Quaternionf();
        }
        return all;
    }

    // A person's model, with the six parts a kneel is made of.
    static boolean person(EntityModel<?> model, List<ModelParts.Part> parts) {
        if (!(model instanceof HumanoidModel<?>)) {
            return false;
        }
        for (String name : NAMES) {
            if (index(parts, name) < 0) {
                return false;
            }
        }
        return true;
    }

    private static int index(List<ModelParts.Part> parts, String name) {
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).name().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    static int ticks(boolean person) {
        return person ? PERSON_TICKS : OTHER_TICKS;
    }

    // How far from lying (1) to its own pose (0) a creature that is not a person is, `u` into getting up.
    static float limp(float u) {
        return 1.0F - (float) Ease.smoother(u);
    }

    // A person `u` of the way up (0 to 1), from how it lay (lie*) through the kneel to its own pose (own*); knees as
    // the turn of the far half in the near half's axes. Writes out*.
    static void person(List<ModelParts.Part> parts, int core, ModelParts.Bend[] bends, float u, Vector3f[] liePos,
            Quaternionf[] lieRot, Quaternionf[] lieKnee, Vector3f[] ownPos, Quaternionf[] ownRot, Vector3f[] outPos,
            Quaternionf[] outRot, Quaternionf[] outKnee) {
        int n = parts.size();
        for (int i = 0; i < n; i++) {
            int k = kneelIndex(parts.get(i).name());
            float[] kneel = k < 0 ? null : KNEEL[k];
            if (kneel == null) {
                K_POS[i].set(ownPos[i]);
                K_ROT[i].set(ownRot[i]);
                K_KNEE[i].identity();
            } else {
                K_POS[i].set(kneel[0], kneel[1], kneel[2]);
                K_ROT[i].rotationZYX(kneel[5], kneel[4], kneel[3]);
                ModelParts.Bend bend = bends[i];
                if (bend == null) {
                    K_KNEE[i].identity();
                } else {
                    K_KNEE[i].setAngleAxis(kneel[6], bend.hinge()[0], bend.hinge()[1], bend.hinge()[2]);
                }
            }
        }
        float down = (float) Ease.smoother(u / KNEELED);
        float up = (float) Ease.smoother((u - STANDS) / (1.0F - STANDS));
        blend(n, core, liePos, lieRot, K_POS, K_ROT, down, MID_POS, MID_ROT);
        blend(n, core, MID_POS, MID_ROT, ownPos, ownRot, up, outPos, outRot);
        for (int i = 0; i < n; i++) {
            MID_KNEE[i].set(lieKnee[i]).slerp(K_KNEE[i], down);
            outKnee[i].set(MID_KNEE[i]).slerp(B.identity(), up);
        }
    }

    private static int kneelIndex(String name) {
        for (int k = 0; k < NAMES.length; k++) {
            if (NAMES[k].equals(name)) {
                return k;
            }
        }
        return -1;
    }

    // Blends pose a into pose b by w, the core as it is and every other part in the core's own frame.
    static void blend(int n, int core, Vector3f[] aPos, Quaternionf[] aRot, Vector3f[] bPos, Quaternionf[] bRot,
            float w, Vector3f[] outPos, Quaternionf[] outRot) {
        CORE_POS.set(aPos[core]).lerp(bPos[core], w);
        CORE.set(aRot[core]).slerp(bRot[core], w);
        BACK_A.set(aRot[core]).conjugate();
        BACK_B.set(bRot[core]).conjugate();
        for (int i = 0; i < n; i++) {
            if (i == core) {
                continue;
            }
            BACK_A.transform(V.set(aPos[i]).sub(aPos[core]));
            BACK_B.transform(W.set(bPos[i]).sub(bPos[core]));
            V.lerp(W, w);
            CORE.transform(V);
            outPos[i].set(CORE_POS).add(V);
            A.set(BACK_A).mul(aRot[i]);
            B.set(BACK_B).mul(bRot[i]);
            A.slerp(B, w);
            outRot[i].set(CORE).mul(A);
        }
        outPos[core].set(CORE_POS);
        outRot[core].set(CORE);
    }
}
