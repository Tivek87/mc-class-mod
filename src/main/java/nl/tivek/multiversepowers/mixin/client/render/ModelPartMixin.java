package nl.tivek.multiversepowers.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// A limp creature's arm or leg is drawn bent at its elbow or knee.
@Mixin(ModelPart.class)
public abstract class ModelPartMixin {
    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void welcomescreen$bent(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color,
            CallbackInfo info) {
        if (BentParts.draw((ModelPart) (Object) this, pose, buffer, light, overlay, color)) {
            info.cancel();
        }
    }
}
