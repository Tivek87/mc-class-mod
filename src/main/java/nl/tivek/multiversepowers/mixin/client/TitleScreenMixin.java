package nl.tivek.multiversepowers.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import nl.tivek.multiversepowers.update.client.tour.Tour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    // The loading screen fades off the title screen by drawing it without NeoForge's screen events.
    @Inject(method = "render", at = @At("TAIL"))
    private void welcomescreen$tour(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo info) {
        if (Minecraft.getInstance().getOverlay() != null) {
            Tour.underOverlay(graphics);
        }
    }
}
