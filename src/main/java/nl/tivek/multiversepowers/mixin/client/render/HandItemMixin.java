package nl.tivek.multiversepowers.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.HumanoidArm;
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// What a person holds stays in the hand when a pose bends the arm at the elbow.
@Mixin({ HumanoidModel.class, PlayerModel.class })
public abstract class HandItemMixin {
    @Inject(method = "translateToHand", at = @At("TAIL"))
    private void welcomescreen$elbow(HumanoidArm side, PoseStack pose, CallbackInfo info) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        BentParts.farHalf(side == HumanoidArm.RIGHT ? model.rightArm : model.leftArm, pose);
    }
}
