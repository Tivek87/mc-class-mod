package nl.tivek.multiversepowers.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import nl.tivek.multiversepowers.faction.mob.MobTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestAttackableTargetGoalMixin extends TargetGoal {
    private NearestAttackableTargetGoalMixin(Mob mob) {
        super(mob, false);
    }

    // A goal of higher rank that starts ends the one below it and its fight: a player walking by would take a
    // creature off whoever it was fighting.
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void welcomescreen$inFight(CallbackInfoReturnable<Boolean> info) {
        if (MobTargets.inFight(this.mob)) {
            info.setReturnValue(false);
        }
    }
}
