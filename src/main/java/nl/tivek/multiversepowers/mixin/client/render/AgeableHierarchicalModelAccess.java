package nl.tivek.multiversepowers.mixin.client.render;

import net.minecraft.client.model.AgeableHierarchicalModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// How a young one of these models is drawn smaller, for a ragdoll that must put its parts where they are seen.
@Mixin(AgeableHierarchicalModel.class)
public interface AgeableHierarchicalModelAccess {
    @Accessor("youngScaleFactor")
    float welcomescreen$youngScaleFactor();

    @Accessor("bodyYOffset")
    float welcomescreen$bodyYOffset();
}
