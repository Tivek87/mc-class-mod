package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A sword or an axe in a person's hand, as the game draws it there, and the person leaning on it as it gets up: its
// hand turned so the blade stands straight down, the tip on the ground, its fingers to the front.
final class Brace {
    // Along the diagonal of the item's picture (from its bottom left corner to its top right): where the blade's tip
    // is, and where the hand grips it.
    private static final float TIP = 0.94F;
    private static final float GRIP = 0.25F;
    private static final Vector3f DOWN = new Vector3f(0.0F, 1.0F, 0.0F);

    // The arm that holds it (0 the right, 1 the left), and its blade's tip and way (from the grip to the tip) in that
    // arm's own pixels.
    final int limb;
    private final Vector3f tip;
    private final Vector3f way;

    private Brace(int limb, Vector3f tip, Vector3f way) {
        this.limb = limb;
        this.tip = tip;
        this.way = way;
    }

    // What a grown person holds in its main hand to lean on, or null.
    @Nullable
    static Brace of(LivingEntity entity) {
        ItemStack stack = entity.getMainHandItem();
        if (entity.isBaby() || !stack.is(ItemTags.SWORDS) && !stack.is(ItemTags.AXES)) {
            return null;
        }
        boolean left = entity.getMainArm() == HumanoidArm.LEFT;
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, entity.level(), entity, 0);
        // As ItemInHandLayer and ItemRenderer place it from the arm's own frame (blocks).
        PoseStack pose = new PoseStack();
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.translate((left ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
        model.applyTransform(left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, pose, left);
        pose.translate(-0.5F, -0.5F, -0.5F);
        Matrix4f item = pose.last().pose();
        Vector3f tip = item.transformPosition(new Vector3f(TIP, TIP, 0.5F)).mul(16.0F);
        Vector3f grip = item.transformPosition(new Vector3f(GRIP, GRIP, 0.5F)).mul(16.0F);
        Vector3f way = new Vector3f(tip).sub(grip);
        return way.lengthSquared() < 1.0E-4F ? null : new Brace(left ? 1 : 0, tip, way.normalize());
    }

    // The arm of `pose` (placed) holding the blade straight down with its tip at `at`, its fingers towards `ahead` as
    // far as that leaves, its elbow out towards `toward`.
    void hold(Skeleton body, Skeleton.Pose pose, Vector3f at, Vector3f ahead, Vector3f toward) {
        Quaternionf hand = turn(body.bone(this.limb, new Vector3f()), ahead);
        Vector3f fingers = hand.transform(new Vector3f(body.tipAt[this.limb]).sub(this.tip)).add(at);
        body.reach(pose, this.limb, fingers, hand, toward);
    }

    // The hand's turn taking the blade's way to straight down and its bone (the way its fingers run) as near `ahead`.
    private Quaternionf turn(Vector3f bone, Vector3f ahead) {
        Vector3f a1 = new Vector3f(this.way);
        Vector3f a2 = new Vector3f(bone).sub(new Vector3f(a1).mul(bone.dot(a1)));
        Vector3f b1 = new Vector3f(DOWN);
        Vector3f b2 = new Vector3f(ahead).sub(new Vector3f(b1).mul(ahead.dot(b1)));
        if (a2.lengthSquared() < 1.0E-6F || b2.lengthSquared() < 1.0E-6F) {
            return new Quaternionf().rotationTo(a1, b1);
        }
        a2.normalize();
        b2.normalize();
        Matrix3f from = new Matrix3f(a1, a2, new Vector3f(a1).cross(a2));
        Matrix3f to = new Matrix3f(b1, b2, new Vector3f(b1).cross(b2));
        return to.mul(from.transpose()).getNormalizedRotation(new Quaternionf());
    }
}
