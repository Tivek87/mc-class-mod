package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import java.util.List;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;

// Anything but a person getting up: it rolls upright while it still lies low, then pushes up onto its legs into its
// own pose, its limbs coming under it a little behind its trunk and its legs folding at the knees on the way up.
final class CreatureRise implements GetUp.Rise {
    // Its trunk is upright this far into getting up and rises onto its legs from RISE_FROM to RISE_TO; its limbs
    // come under it from LIMBS_FROM to LIMBS_TO, its legs folded at most TUCK radians more halfway up.
    private static final float UPRIGHT = 0.55F;
    private static final float RISE_FROM = 0.2F;
    private static final float RISE_TO = 0.9F;
    private static final float LIMBS_FROM = 0.1F;
    private static final float LIMBS_TO = 0.85F;
    private static final float TUCK = 0.35F;
    private static final Quaternionf FOLD = new Quaternionf();

    private final Hanging body;
    // Each part's knee, where it is a leg that has one: its joint (null otherwise).
    private final ModelBends.Bend[] knees;

    CreatureRise(Hanging body, List<ModelParts.Part> parts, ModelBends.Bend[][] chains) {
        this.body = body;
        this.knees = new ModelBends.Bend[body.n];
        for (int i = 0; i < body.n; i++) {
            boolean leg = i != body.core && parts.get(i).role() == ModelParts.Role.LEG;
            this.knees[i] = leg && chains[i].length > 0 ? chains[i][0] : null;
        }
    }

    @Override
    public void pose(float u, float turned, BodyPose lie, BodyPose own, BodyPose out) {
        float upright = (float) Ease.smoother(u / UPRIGHT);
        float rise = (float) Ease.smoother((u - RISE_FROM) / (RISE_TO - RISE_FROM));
        float limbs = (float) Ease.smoother((u - LIMBS_FROM) / (LIMBS_TO - LIMBS_FROM));
        PoseBlend.blend(this.body, lie, own, upright, rise, limbs, out);
        float tuck = TUCK * Mth.sin(rise * Mth.PI);
        for (int i = 0; i < this.body.n; i++) {
            ModelBends.Bend knee = this.knees[i];
            if (knee == null || tuck < 1.0E-3F) {
                continue;
            }
            float[] h = knee.hinge();
            out.joint[0][i].premul(FOLD.setAngleAxis(Math.min(tuck, (float) knee.max()), h[0], h[1], h[2]));
        }
    }
}
