package nl.tivek.multiversepowers.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import nl.tivek.multiversepowers.faction.mob.MobTargets;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsMixin {
    @Shadow
    @Final
    private boolean isCombat;

    // A goal that picked a target the world refuses would keep running with it, and keep every other target goal out.
    @Inject(method = "test", at = @At("RETURN"), cancellable = true)
    private void welcomescreen$refused(LivingEntity attacker, LivingEntity target,
            CallbackInfoReturnable<Boolean> info) {
        if (this.isCombat && info.getReturnValueZ() && attacker instanceof Mob mob && !mob.level().isClientSide()
                && MobTargets.forbidden(mob, target)) {
            info.setReturnValue(false);
        }
    }
}
