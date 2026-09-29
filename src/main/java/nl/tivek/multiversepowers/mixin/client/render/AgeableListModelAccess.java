package nl.tivek.multiversepowers.mixin.client.render;

import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

// A ragdoll finds the parts of most four-legged and many other creatures' models through these, and how a young one's
// head and body are drawn smaller.
@Mixin(AgeableListModel.class)
public interface AgeableListModelAccess {
    @Invoker("headParts")
    Iterable<ModelPart> welcomescreen$headParts();

    @Invoker("bodyParts")
    Iterable<ModelPart> welcomescreen$bodyParts();

    @Accessor("scaleHead")
    boolean welcomescreen$scaleHead();

    @Accessor("babyYHeadOffset")
    float welcomescreen$babyYHeadOffset();

    @Accessor("babyZHeadOffset")
    float welcomescreen$babyZHeadOffset();

    @Accessor("babyHeadScale")
    float welcomescreen$babyHeadScale();

    @Accessor("babyBodyScale")
    float welcomescreen$babyBodyScale();

    @Accessor("bodyYOffset")
    float welcomescreen$bodyYOffset();
}
