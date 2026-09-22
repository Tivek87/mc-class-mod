package nl.tivek.welcomescreen.client.character.lantern;

import net.minecraft.client.model.HumanoidModel;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * The arm pose of a Green Lantern whose ring is at work: recharging, or reaching out to a construct (see
 * {@link LanternArms}). The game only lets a mod add an arm pose while it loads, through
 * {@code META-INF/enumextensions.json}, which points at the field below.
 *
 * <p>Kept in a class of its own with nothing else in it: the game reads it while the arm poses themselves
 * are still being set up.
 */
public final class LanternPose {
    public static final EnumProxy<HumanoidModel.ArmPose> POSE = new EnumProxy<>(HumanoidModel.ArmPose.class, false,
            (IArmPoseTransformer) (model, entity, arm) -> LanternArms.pose(model, entity, arm));

    private LanternPose() {
    }
}
