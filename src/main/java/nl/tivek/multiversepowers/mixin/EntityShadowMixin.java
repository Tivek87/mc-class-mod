package nl.tivek.multiversepowers.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// A creature's round shadow stays where it stood; a limp body lying elsewhere casts none there.
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityShadowMixin {
    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void welcomescreen$noShadow(PoseStack pose, MultiBufferSource buffer, Entity entity, float weight,
            float partialTick, LevelReader level, float size, CallbackInfo info) {
        if (Ragdolls.lying(entity)) {
            info.cancel();
        }
    }
}
