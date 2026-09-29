package nl.tivek.multiversepowers.engine.client.ragdoll;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A thrown creature getting up from where it lies. A person first pushes up onto one knee, the other foot planted
// and a hand on the ground, then stands; anything else rises straight into its own pose. Every part moves with the
// half of the trunk it hangs from (blended in that half's frame), so arms and legs stay on the body the whole way up.
// Model space, in pixels: y runs down, -z ahead.
final class GetUp {
    // The most parts a model a ragdoll moves can have.
    static final int MOST = 32;
    static final int PERSON_TICKS = 36;
    static final int OTHER_TICKS = 24;
    private static final String[] NAMES = { "head", "body", "right_arm", "left_arm", "right_leg", "left_leg" };
    // The kneel, for each part as named above: its turn (x, y, z as a model part turns) and how far its elbow, knee
    // or waist is bent. The hips sink six pixels over the kneeling knee, the chest leans over the forward knee and
    // the belly less, bent at the waist; the head, shoulders and hips go where the trunk puts them.
    private static final float[][] KNEEL = {
            { -0.2F, 0.0F, 0.0F, 0.0F },
            { 0.55F, 0.0F, 0.0F, 0.4F },
            { -0.62F, 0.1F, 0.1F, 0.25F },
            { -0.45F, -0.1F, -0.2F, 0.7F },
            { 0.0F, 0.0F, 0.0F, 1.5708F },
            { -1.5708F, 0.0F, 0.0F, 1.5708F } };
    private static final float KNEEL_HIPS = 18.0F;
    // A trunk that does not bend leans this far over the knee instead.
    private static final float KNEEL_STRAIGHT = 0.35F;
    // Neck to waist and waist to hips; shoulder and hip from the middle of the trunk.
    private static final float HALF_TRUNK = 6.0F;
    private static final float SHOULDER_X = 5.0F;
    private static final float SHOULDER_Y = 2.0F;
    private static final float HIP_X = 1.9F;
    // Onto the knee by this far into getting up; it already rises from that far on, so it never stops on the knee.
    private static final float KNEELED = 0.5F;
    private static final float STANDS = 0.36F;
    // Anything else rights its trunk first, its limbs coming under it a little later.
    private static final float TRUNK_UP = 0.75F;
    private static final float LIMBS_FROM = 0.15F;

    private static final Vector3f[] MID_POS = vectors();
    private static final Quaternionf[] MID_ROT = turns();
    private static final Vector3f[] K_POS = vectors();
    private static final Quaternionf[] K_ROT = turns();
    private static final Quaternionf[] K_KNEE = turns();
    private static final Quaternionf[] MID_KNEE = turns();
    private static final Vector3f V = new Vector3f();
    private static final Vector3f W = new Vector3f();
    private static final Vector3f NECK = new Vector3f();
    private static final Quaternionf CORE = new Quaternionf();
    private static final Quaternionf A = new Quaternionf();
    private static final Quaternionf B = new Quaternionf();
    private static final Quaternionf BACK = new Quaternionf();
    private static final Vector3f CORE_POS = new Vector3f();

    // How a body's parts hang together, for blending two of its poses (each part as it hangs in its parent: where,
    // how turned, and how its knee, elbow or waist is bent). A part hangs from the trunk's near half or, when it has a
    // waist, its far half (`low`); it is drawn beside the trunk, its place taken into the trunk's parent's pixels by a
    // scale and a move (a young creature's head is drawn bigger), or inside it, placed in the trunk's own frame.
    static final class Body {
        final int n;
        final int core;
        final boolean[] inside;
        final boolean[] low;
        @Nullable
        final float[] waist;
        final float[] scale = new float[MOST];
        final Vector3f[] move = vectors();
        final Vector3f trunk = new Vector3f(1.0F, 1.0F, 1.0F);

        Body(int n, int core, boolean[] inside, boolean[] low, @Nullable float[] waist) {
            this.n = n;
            this.core = core;
            this.inside = inside;
            this.low = low;
            this.waist = waist;
            Arrays.fill(this.scale, 1.0F);
        }
    }

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

    // How far from lying (1) to its own pose (0) a creature that is not a person is, `u` into getting up: its trunk,
    // and its limbs.
    static float limp(float u) {
        return 1.0F - (float) Ease.smoother(u / TRUNK_UP);
    }

    static float limbsLimp(float u) {
        return 1.0F - (float) Ease.smoother((u - LIMBS_FROM) / (1.0F - LIMBS_FROM));
    }

    // A person `u` of the way up (0 to 1), from how it lay (lie*) through the kneel to its own pose (own*); knees as
    // the turn of the far half in the near half's axes. Writes out*.
    static void person(Body body, List<ModelParts.Part> parts, ModelBends.Bend[] bends, float u, Vector3f[] liePos,
            Quaternionf[] lieRot, Quaternionf[] lieKnee, Vector3f[] ownPos, Quaternionf[] ownRot,
            Quaternionf[] ownKnee, Vector3f[] outPos, Quaternionf[] outRot, Quaternionf[] outKnee) {
        float chest = bends[body.core] != null ? KNEEL[1][0] : KNEEL_STRAIGHT;
        float belly = bends[body.core] != null ? chest - KNEEL[1][3] : chest;
        NECK.set(0.0F, KNEEL_HIPS, 0.0F).sub(0.0F, HALF_TRUNK * Mth.cos(belly), HALF_TRUNK * Mth.sin(belly))
                .sub(0.0F, HALF_TRUNK * Mth.cos(chest), HALF_TRUNK * Mth.sin(chest));
        for (int i = 0; i < body.n; i++) {
            int k = kneelIndex(parts.get(i).name());
            if (k < 0) {
                K_POS[i].set(ownPos[i]);
                K_ROT[i].set(ownRot[i]);
                K_KNEE[i].set(ownKnee[i]);
                continue;
            }
            float[] kneel = KNEEL[k];
            float side = k == 2 || k == 4 ? -1.0F : 1.0F;
            if (k < 2) {
                K_POS[i].set(NECK);
            } else if (k < 4) {
                K_POS[i].set(NECK).add(side * SHOULDER_X, SHOULDER_Y * Mth.cos(chest), SHOULDER_Y * Mth.sin(chest));
            } else {
                K_POS[i].set(side * HIP_X, KNEEL_HIPS, 0.0F);
            }
            // Where it kneels is in the trunk's parent's pixels: a young head drawn bigger has its own.
            K_POS[i].sub(body.move[i]).div(body.scale[i]);
            K_ROT[i].rotationZYX(kneel[2], kneel[1], k == 1 ? chest : kneel[0]);
            ModelBends.Bend bend = bends[i];
            if (bend == null) {
                K_KNEE[i].identity();
            } else {
                K_KNEE[i].setAngleAxis(kneel[3], bend.hinge()[0], bend.hinge()[1], bend.hinge()[2]);
            }
        }
        float down = (float) Ease.smoother(u / KNEELED);
        float up = (float) Ease.smoother((u - STANDS) / (1.0F - STANDS));
        blend(body, liePos, lieRot, lieKnee, K_POS, K_ROT, K_KNEE, down, MID_POS, MID_ROT, MID_KNEE);
        blend(body, MID_POS, MID_ROT, MID_KNEE, ownPos, ownRot, ownKnee, up, outPos, outRot, outKnee);
    }

    private static int kneelIndex(String name) {
        for (int k = 0; k < NAMES.length; k++) {
            if (NAMES[k].equals(name)) {
                return k;
            }
        }
        return -1;
    }

    // Blends pose a into pose b by w: the trunk as it is, its knees, elbows and waist by their own turns, and every
    // other part in the frame of the half of the trunk it hangs from.
    static void blend(Body body, Vector3f[] aPos, Quaternionf[] aRot, Quaternionf[] aKnee, Vector3f[] bPos,
            Quaternionf[] bRot, Quaternionf[] bKnee, float w, Vector3f[] outPos, Quaternionf[] outRot,
            Quaternionf[] outKnee) {
        blend(body, aPos, aRot, aKnee, bPos, bRot, bKnee, w, w, outPos, outRot, outKnee);
    }

    // As above, the trunk (and its waist) by `trunk` and every other part, in the trunk's frame, by `limbs`.
    static void blend(Body body, Vector3f[] aPos, Quaternionf[] aRot, Quaternionf[] aKnee, Vector3f[] bPos,
            Quaternionf[] bRot, Quaternionf[] bKnee, float trunk, float limbs, Vector3f[] outPos,
            Quaternionf[] outRot, Quaternionf[] outKnee) {
        int core = body.core;
        for (int i = 0; i < body.n; i++) {
            outKnee[i].set(aKnee[i]).slerp(bKnee[i], i == core ? trunk : limbs);
        }
        CORE_POS.set(aPos[core]).lerp(bPos[core], trunk);
        CORE.set(aRot[core]).slerp(bRot[core], trunk);
        for (int i = 0; i < body.n; i++) {
            if (i == core) {
                continue;
            }
            toTrunk(body, i, aPos, aRot, aKnee[core], V, A);
            toTrunk(body, i, bPos, bRot, bKnee[core], W, B);
            V.lerp(W, limbs);
            A.slerp(B, limbs);
            fromTrunk(body, i, V, A, outKnee[core], outPos[i], outRot[i]);
        }
        outPos[core].set(CORE_POS);
        outRot[core].set(CORE);
    }

    // Part i's place and turn in the trunk's own frame, and past its waist in its far half's when it hangs from that.
    private static void toTrunk(Body body, int i, Vector3f[] pos, Quaternionf[] rot, Quaternionf waist, Vector3f p,
            Quaternionf r) {
        int core = body.core;
        if (body.inside[i]) {
            p.set(pos[i]);
            r.set(rot[i]);
        } else {
            p.set(pos[i]).mul(body.scale[i]).add(body.move[i]).sub(pos[core]);
            BACK.set(rot[core]).conjugate().transform(p).div(body.trunk);
            r.set(BACK).mul(rot[i]);
        }
        if (body.low[i] && body.waist != null) {
            float[] k = body.waist;
            p.sub(k[0], k[1], k[2]);
            BACK.set(waist).conjugate().transform(p).add(k[0], k[1], k[2]);
            r.premul(BACK);
        }
    }

    // Back from the trunk's frame (the trunk now at CORE_POS, CORE, its waist bent by `waist`) to where part i hangs.
    private static void fromTrunk(Body body, int i, Vector3f p, Quaternionf r, Quaternionf waist, Vector3f outPos,
            Quaternionf outRot) {
        if (body.low[i] && body.waist != null) {
            float[] k = body.waist;
            p.sub(k[0], k[1], k[2]);
            waist.transform(p).add(k[0], k[1], k[2]);
            r.premul(waist);
        }
        if (body.inside[i]) {
            outPos.set(p);
            outRot.set(r);
        } else {
            CORE.transform(p.mul(body.trunk)).add(CORE_POS);
            outPos.set(p).sub(body.move[i]).div(body.scale[i]);
            outRot.set(CORE).mul(r);
        }
    }
}
