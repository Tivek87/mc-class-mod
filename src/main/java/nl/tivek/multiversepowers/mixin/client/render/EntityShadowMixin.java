package nl.tivek.multiversepowers.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// A limp body lying beside where its creature stands: drawn while the body is in view, even with the creature's own
// box out of it, and no round shadow where it stood.
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityShadowMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void welcomescreen$limpInView(Entity entity, Frustum frustum, double camX, double camY, double camZ,
            CallbackInfoReturnable<Boolean> info) {
        if (Ragdolls.inView(entity, frustum)) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void welcomescreen$noShadow(PoseStack pose, MultiBufferSource buffer, Entity entity, float weight,
            float partialTick, LevelReader level, float size, CallbackInfo info) {
        if (Ragdolls.lying(entity)) {
            info.cancel();
        }
    }
}
