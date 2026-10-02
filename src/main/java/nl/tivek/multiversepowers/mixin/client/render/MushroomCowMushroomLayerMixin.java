package nl.tivek.multiversepowers.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.layers.MushroomCowMushroomLayer;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// The mushroom on a mooshroom's head is placed from its head, which a limp body moves itself: the trunk's carrying of
// the two on its back (Ragdolls.carry) is taken off it first.
@Mixin(MushroomCowMushroomLayer.class)
public abstract class MushroomCowMushroomLayerMixin {
    @Redirect(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
            + "ILnet/minecraft/world/entity/animal/MushroomCow;FFFFFF)V", at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;translateAndRotate("
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void welcomescreen$head(ModelPart head, PoseStack pose) {
        Ragdolls.uncarry(pose);
        head.translateAndRotate(pose);
    }
}
