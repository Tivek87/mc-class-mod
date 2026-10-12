package nl.tivek.multiversepowers.mixin.client;

import net.minecraft.world.entity.Entity;
import nl.tivek.multiversepowers.space.client.SpaceView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @Unique
    private static boolean welcomescreen$turning;

    @Shadow
    public abstract void turn(double yRot, double xRot);

    // While the player's view is rolled over (SpaceView), the mouse turns them as the screen lies, not as the world
    // does: right on the mouse stays right on the screen.
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void welcomescreen$rolled(double yRot, double xRot, CallbackInfo info) {
        if (welcomescreen$turning) {
            return;
        }
        double roll = SpaceView.roll((Entity) (Object) this);
        if (roll == 0.0) {
            return;
        }
        double c = Math.cos(roll);
        double s = Math.sin(roll);
        welcomescreen$turning = true;
        try {
            this.turn(yRot * c - xRot * s, yRot * s + xRot * c);
        } finally {
            welcomescreen$turning = false;
        }
        info.cancel();
    }
}
