package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import java.util.List;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Anything but a person getting up: it turns upright while it still lies low (a four-legged one rolling onto its belly
// about its spine), its legs folding in under it, then unfolds them and pushes up into its own pose, its front legs a
// little before its hind ones, its limbs coming under it a little behind its trunk. It stays on the ground all the way:
// what would sink into it or hang over it is moved up or down onto it.
final class CreatureRise implements GetUp.Rise {
    // Its trunk is upright this far into getting up and rises onto its legs from RISE_FROM to RISE_TO; its limbs
    // come under it from LIMBS_FROM to LIMBS_TO, hind legs HIND later; its legs fold in at most TUCK radians more at
    // the knee from FOLD_FROM over FOLD_OVER, and unfold again from UNFOLD_FROM to UNFOLD_TO (hind legs HIND later).
    private static final float UPRIGHT = 0.5F;
    private static final float RISE_FROM = 0.25F;
    private static final float RISE_TO = 0.9F;
    private static final float LIMBS_FROM = 0.1F;
    private static final float LIMBS_TO = 0.82F;
    private static final float HIND = 0.07F;
    private static final float TUCK = 1.2F;
    private static final float FOLD_FROM = 0.05F;
    private static final float FOLD_OVER = 0.3F;
    private static final float UNFOLD_FROM = 0.42F;
    private static final float UNFOLD_TO = 0.8F;
    // How much of the ankle's fold the knee's undoes, keeping a foot under the leg.
    private static final float ANKLE_BACK = 0.5F;
    // It is kept on the ground from GROUND_IN on and let go of it again over the last GROUND_IN, so it starts just as
    // it lay and ends just in its own pose.
    private static final float GROUND_IN = 0.1F;
    private static final Quaternionf FOLD = new Quaternionf();

    private final Hanging body;
    // Each part's knee, where it is a leg that has one (null otherwise); its ankle, where it has one; whether it is a
    // hind leg; each part's pieces' boxes (pixels, its own frame) and its joints.
    private final ModelBends.Bend[] knees;
    private final ModelBends.Bend[] ankles;
    private final boolean[] hind;
    private final float[][][] pieces;
    private final ModelBends.Bend[][] chains;
    private final float[] limbs;
    private final Matrix4f frame = new Matrix4f();
    private final Matrix4f trunk = new Matrix4f();
    private final Vector3f v = new Vector3f();

    CreatureRise(Hanging body, List<ModelParts.Part> parts, ModelBends.Bend[][] chains) {
        this.body = body;
        this.chains = chains;
        int n = body.n;
        this.knees = new ModelBends.Bend[n];
        this.ankles = new ModelBends.Bend[n];
        this.hind = new boolean[n];
        this.pieces = new float[n][][];
        this.limbs = new float[n];
        // Its front is the way its head is from its trunk as its model was built (else -z, ahead).
        Matrix4f rest = new Matrix4f();
        float coreZ = ModelParts.rest(parts.get(body.core), rest).m32();
        float ahead = -1.0F;
        for (int i = 0; i < n; i++) {
            if (i != body.core && parts.get(i).role() == ModelParts.Role.HEAD) {
                ahead = Math.signum(ModelParts.rest(parts.get(i), rest).m32() - coreZ);
            }
        }
        for (int i = 0; i < n; i++) {
            ModelBends.Bend[] chain = chains[i];
            this.pieces[i] = pieces(parts.get(i).bounds(), chain);
            boolean leg = i != body.core && parts.get(i).role() == ModelParts.Role.LEG;
            this.knees[i] = leg && chain.length > 0 ? chain[0] : null;
            this.ankles[i] = leg && chain.length > 1 ? chain[1] : null;
            this.hind[i] = leg && (ModelParts.rest(parts.get(i), rest).m32() - coreZ) * ahead < 0.0F;
        }
    }

    // A part's box (pixels, its own frame) cut at its joints into its pieces, near to far.
    private static float[][] pieces(float[] b, ModelBends.Bend[] chain) {
        float[][] pieces = new float[chain.length + 1][];
        float[] rest = b;
        for (int k = 0; k < chain.length; k++) {
            ModelBends.Bend bend = chain[k];
            int a = bend.axis();
            float[] near = rest.clone();
            float[] far = rest.clone();
            near[bend.farSign() > 0.0F ? a + 3 : a] = bend.at();
            far[bend.farSign() > 0.0F ? a : a + 3] = bend.at();
            pieces[k] = near;
            rest = far;
        }
        pieces[chain.length] = rest;
        return pieces;
    }

    @Override
    public void pose(float u, BodyPose lie, BodyPose own, BodyPose out) {
        float upright = (float) Ease.smoother(u / UPRIGHT);
        float rise = (float) Ease.smoother((u - RISE_FROM) / (RISE_TO - RISE_FROM));
        for (int i = 0; i < this.body.n; i++) {
            float late = this.hind[i] ? HIND : 0.0F;
            this.limbs[i] = (float) Ease.smoother((u - LIMBS_FROM - late) / (LIMBS_TO - LIMBS_FROM));
        }
        PoseBlend.blend(this.body, lie, own, upright, rise, this.limbs, out);
        for (int i = 0; i < this.body.n; i++) {
            ModelBends.Bend knee = this.knees[i];
            float late = this.hind[i] ? HIND : 0.0F;
            float tuck = TUCK * (float) (Ease.smoother((u - FOLD_FROM) / FOLD_OVER)
                    - Ease.smoother((u - UNFOLD_FROM - late) / (UNFOLD_TO - UNFOLD_FROM)));
            if (knee == null || tuck < 1.0E-3F) {
                continue;
            }
            float[] h = knee.hinge();
            float fold = Math.min(tuck, (float) knee.max());
            out.joint[0][i].premul(FOLD.setAngleAxis(fold, h[0], h[1], h[2]));
            ModelBends.Bend ankle = this.ankles[i];
            if (ankle != null) {
                float back = Math.max(-fold * ANKLE_BACK, (float) ankle.min());
                out.joint[1][i].premul(FOLD.setAngleAxis(back, h[0], h[1], h[2]));
            }
        }
        float hold = (float) (Ease.smoother(u / GROUND_IN) * (1.0 - Ease.smoother((u - 1.0F + GROUND_IN) / GROUND_IN)));
        if (hold > 0.0F) {
            float sink = (this.lowest(out) - this.lowest(own)) * hold;
            for (int i = 0; i < this.body.n; i++) {
                if (!this.body.inside[i]) {
                    out.pos[i].y -= i == this.body.core ? sink : sink / this.body.scale[i];
                }
            }
        }
    }

    // The lowest point (the largest y: y runs down) of a pose, every piece's corners, in the trunk's parent's pixels.
    private float lowest(BodyPose pose) {
        Hanging body = this.body;
        int core = body.core;
        this.trunk.translation(pose.pos[core]).rotate(pose.rot[core]).scale(body.trunk);
        float low = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < body.n; i++) {
            if (i == core) {
                this.frame.set(this.trunk);
            } else if (body.inside[i]) {
                this.frame.set(this.trunk).translate(pose.pos[i]).rotate(pose.rot[i]);
            } else {
                Vector3f at = this.v.set(pose.pos[i]).mul(body.scale[i]).add(body.move[i]);
                this.frame.translation(at).rotate(pose.rot[i]).scale(body.scale[i]);
            }
            float[][] boxes = this.pieces[i];
            ModelBends.Bend[] chain = this.chains[i];
            for (int k = 0; k < boxes.length; k++) {
                if (k > 0) {
                    float[] knee = chain[k - 1].knee();
                    this.frame.translate(knee[0], knee[1], knee[2]).rotate(pose.joint[k - 1][i])
                            .translate(-knee[0], -knee[1], -knee[2]);
                }
                float[] b = boxes[k];
                for (int c = 0; c < 8; c++) {
                    this.frame.transformPosition(this.v.set((c & 1) == 0 ? b[0] : b[3], (c & 2) == 0 ? b[1] : b[4],
                            (c & 4) == 0 ? b[2] : b[5]));
                    low = Math.max(low, this.v.y);
                }
            }
        }
        return low;
    }
}
