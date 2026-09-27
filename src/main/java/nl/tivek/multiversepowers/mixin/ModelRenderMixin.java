package nl.tivek.multiversepowers.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.AgeableHierarchicalModel;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.CamelModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// A layer that draws its own copy of a creature's model (a sheep's wool, a saddle, a drowned's outer skin) takes the
// limp pose of the creature it is drawn on.
@Mixin({ AgeableListModel.class, HierarchicalModel.class, AgeableHierarchicalModel.class, CamelModel.class })
public abstract class ModelRenderMixin {
    @Inject(method = "renderToBuffer", at = @At("HEAD"))
    private void welcomescreen$limp(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color,
            CallbackInfo info) {
        Ragdolls.layer((EntityModel<?>) (Object) this);
    }
}
