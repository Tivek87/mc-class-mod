package nl.tivek.multiversepowers.mixin.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.client.PowerInputs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The fixed power gestures show in Controls greyed out and marked locked, and sharing a button with the game's own
// key on purpose is no clash to paint red.
@Mixin(targets = "net.minecraft.client.gui.screens.options.controls.KeyBindsList$KeyEntry")
public abstract class KeyEntryMixin {
    @Shadow
    @Final
    private KeyMapping key;
    @Shadow
    @Final
    private Button changeButton;
    @Shadow
    @Final
    private Button resetButton;

    @Redirect(method = "refreshEntry", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;same(Lnet/minecraft/client/KeyMapping;)Z"))
    private boolean welcomescreen$clash(KeyMapping self, KeyMapping other) {
        return self.same(other) && !PowerInputs.sharedOnPurpose(self, other);
    }

    @Inject(method = "refreshEntry", at = @At("TAIL"))
    private void welcomescreen$locked(CallbackInfo ci) {
        if (PowerInputs.isLocked(this.key)) {
            this.changeButton.active = false;
            this.resetButton.active = false;
            this.changeButton.setMessage(this.key.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.GRAY));
            this.resetButton.setMessage(Component.translatable("controls." + MultiversePowers.MODID + ".locked_short")
                    .withStyle(ChatFormatting.GRAY));
            Tooltip why = Tooltip.create(Component.translatable("controls." + MultiversePowers.MODID + ".locked"));
            this.changeButton.setTooltip(why);
            this.resetButton.setTooltip(why);
        }
    }
}
