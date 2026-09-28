package nl.tivek.multiversepowers.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.ChaseCamera;
import nl.tivek.multiversepowers.engine.client.fx.Cinematic;
import nl.tivek.multiversepowers.engine.client.fx.FirstPersonEye;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot, float roll);

    @Shadow
    protected abstract void setPosition(Vec3 pos);

    // A detached camera also draws its own player, who stands in the shot like anyone else.
    @Inject(method = "setup", at = @At("TAIL"))
    private void welcomescreen$film(BlockGetter level, Entity entity, boolean detached, boolean mirrored,
            float partialTick, CallbackInfo info) {
        Cinematic.Shot shot = Cinematic.shot(partialTick);
        if (shot == null) {
            Vec3 own = detached ? null : FirstPersonEye.eye(partialTick);
            if (own != null) {
                this.setPosition(own);
            }
            if (detached) {
                Vec3 eye = ChaseCamera.eye(new Vec3(((Camera) (Object) this).getLookVector()), partialTick);
                if (eye != null) {
                    this.setPosition(eye);
                }
            }
            return;
        }
        float[] jolt = CameraShake.jolt(partialTick);
        this.detached = true;
        this.setRotation(shot.yaw() + jolt[0], shot.pitch() + jolt[1], shot.roll() + jolt[2]);
        this.setPosition(shot.eye());
    }
}
