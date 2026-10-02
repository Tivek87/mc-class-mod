package nl.tivek.multiversepowers.engine.client.pose;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A person's shoulder blades follow their arms as real ones do: up as an arm rises over the shoulder, ahead as it
// reaches out in front, back as it pulls behind (Limbs.shoulder). Stance turns them as it reaches a hand; follow()
// turns them for a pose that turned the arms itself.
public final class Shoulders {
    // How far a blade turns at most, in radians about where it meets the spine: up under an arm straight overhead,
    // ahead behind one reaching straight out, back behind one pulled straight back. A person's arm hangs 5 pixels out
    // from there, so 0.2 moves the shoulder a pixel.
    private static final float UP = 0.22F;
    private static final float AHEAD = 0.2F;
    private static final float BACK = 0.15F;
    // An arm lifted less than this far from hanging (radians) leaves its blade where it is.
    private static final float RISE_FROM = 0.6F;

    private static final Quaternionf ARM = new Quaternionf();
    private static final Quaternionf TRUNK = new Quaternionf();
    private static final Quaternionf BLADE = new Quaternionf();
    private static final Vector3f WAY = new Vector3f();

    private Shoulders() {
    }

    // The blade's turn, in the trunk's axes, under an arm pointing `way` (a unit way in the trunk's axes: y down, -z
    // ahead) and stretched `stretch` of its length (1 straight).
    public static Quaternionf turn(boolean right, Vector3f way, float stretch, Quaternionf out) {
        float lift = (float) Math.acos(Mth.clamp(way.y, -1.0F, 1.0F));
        float up = UP * (float) Ease.smooth((lift - RISE_FROM) / (Math.PI - RISE_FROM));
        float reach = (float) Ease.smooth((stretch - 0.5F) / 0.5F);
        float ahead = AHEAD * Math.max(0.0F, -way.z) * reach - BACK * Math.max(0.0F, way.z);
        float side = right ? 1.0F : -1.0F;
        return out.rotationY(-side * ahead).rotateZ(side * up);
    }

    // Each blade not turned yet follows its straight arm, `weight` of the way; the arm keeps the way it points.
    public static void follow(HumanoidModel<?> model, float weight) {
        if (weight <= 0.0F) {
            return;
        }
        TRUNK.rotationZYX(model.body.zRot, model.body.yRot, model.body.xRot);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            ModelPart arm = right ? model.rightArm : model.leftArm;
            if (!arm.visible || Limbs.shrugged(model, right)) {
                continue;
            }
            float x = arm.xRot;
            float y = arm.yRot;
            float z = arm.zRot;
            ARM.rotationZYX(z, y, x).transform(WAY.set(0.0F, 1.0F, 0.0F));
            TRUNK.transformInverse(WAY);
            turn(right, WAY, 1.0F, BLADE);
            if (weight < 1.0F) {
                BLADE.set(new Quaternionf().slerp(BLADE, weight));
            }
            Limbs.shoulder(model, right, BLADE);
            arm.setRotation(x, y, z);
        }
    }
}
