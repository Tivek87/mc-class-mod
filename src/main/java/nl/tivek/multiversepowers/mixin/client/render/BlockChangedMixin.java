package nl.tivek.multiversepowers.mixin.client.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import nl.tivek.multiversepowers.schematic.client.blueprint.ClientBlueprints;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class BlockChangedMixin {
    // Every block this game sees change, placed by the player or sent by the server: a blueprint there is checked.
    @Inject(method = "blockChanged", at = @At("HEAD"))
    private void welcomescreen$blockChanged(BlockGetter level, BlockPos pos, BlockState oldState, BlockState newState,
            int flags, CallbackInfo info) {
        ClientBlueprints.blockChanged(pos);
    }
}
