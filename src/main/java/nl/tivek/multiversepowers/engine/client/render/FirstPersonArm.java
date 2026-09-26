package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class FirstPersonArm {
    public static final Vector3f SHOULDER_RIGHT = restPoint(1.0F, -2.0F);
    public static final Vector3f SHOULDER_LEFT = restPoint(-1.0F, -2.0F);
    public static final Vector3f HAND_RIGHT = restPoint(1.0F, 9.0F);
    public static final Vector3f HAND_LEFT = restPoint(-1.0F, 9.0F);

    private FirstPersonArm() {
    }

    public static void arm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            PlayerRenderer renderer, float side, Vector3f hand, Vector3f from) {
        Vector3f restShoulder = side > 0.0F ? SHOULDER_RIGHT : SHOULDER_LEFT;
        Vector3f restHand = new Vector3f(side > 0.0F ? HAND_RIGHT : HAND_LEFT);
        Vector3f restWay = new Vector3f(restHand).sub(restShoulder).normalize();
        Vector3f toHand = new Vector3f(hand).sub(from).normalize();
        pose.pushPose();
        pose.translate(hand.x, hand.y, hand.z);
        pose.mulPose(new Quaternionf().rotationTo(restWay, toHand));
        pose.translate(-restHand.x, -restHand.y, -restHand.z);
        emptyHand(pose, side);
        if (side > 0.0F) {
            renderer.renderRightHand(pose, buffers, light, player);
        } else {
            renderer.renderLeftHand(pose, buffers, light, player);
        }
        pose.popPose();
    }

    // Vanilla's own transform for an empty first-person hand.
    private static void emptyHand(PoseStack pose, float side) {
        pose.translate(side * 0.64000005F, -0.6F, -0.71999997F);
        pose.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
        pose.translate(side * -1.0F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        pose.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
        pose.translate(side * 5.6F, 0.0F, 0.0F);
    }

    private static Vector3f restPoint(float side, float along) {
        PoseStack pose = new PoseStack();
        emptyHand(pose, side);
        pose.translate(side * -5.0F / 16.0F, 2.0F / 16.0F, 0.0F);
        pose.mulPose(Axis.ZP.rotation(side * 0.1F));
        return pose.last().pose().transformPosition(side * -1.0F / 16.0F, along / 16.0F, 0.0F, new Vector3f());
    }
}
