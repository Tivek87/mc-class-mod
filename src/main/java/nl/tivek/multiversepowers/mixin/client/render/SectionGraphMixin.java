package nl.tivek.multiversepowers.mixin.client.render;

import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import nl.tivek.multiversepowers.space.client.SpaceView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionOcclusionGraph.class)
public abstract class SectionGraphMixin {
    // The game draws no land further below the eye than its view distance: from high up (SpaceView) it reaches
    // deeper, still no wider, so the land stays in sight until it fades into its haze.
    @Redirect(method = "getRelativeFrom", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ViewArea;getViewDistance()I"))
    private int welcomescreen$deeper(ViewArea area) {
        return area.getViewDistance() * SpaceView.deeper();
    }
}
