package nl.tivek.multiversepowers.creature;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// Bleeding, from the Lurker's claws: a little harm again and again, faster the deeper the wounds (each new tear adds
// one, up to five), past armor; drops of blood fall from the wounds.
public final class Bleeding extends MobEffect {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT,
            MultiversePowers.MODID);
    public static final DeferredHolder<MobEffect, Bleeding> BLEEDING = EFFECTS.register("bleeding", Bleeding::new);
    public static final int DEEPEST = 4;
    private static final int BLOOD = 0x8A0B0B;

    private Bleeding() {
        super(MobEffectCategory.HARMFUL, BLOOD);
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }

    // One more tear: the bleeding gets a step deeper and starts over for `ticks`.
    public static void tear(LivingEntity victim, int ticks, int steps) {
        Holder<MobEffect> bleeding = BLEEDING;
        MobEffectInstance now = victim.getEffect(bleeding);
        int depth = now == null ? steps - 1 : Math.min(DEEPEST, now.getAmplifier() + steps);
        victim.addEffect(new MobEffectInstance(bleeding, ticks, Math.min(DEEPEST, depth)));
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int every = Math.max(8, 30 - amplifier * 5);
        return duration % every == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity victim, int amplifier) {
        DamageSource source = new DamageSource(victim.level().registryAccess().registryOrThrow(
                Registries.DAMAGE_TYPE).getHolderOrThrow(MonsterBlows.BLEED));
        victim.hurt(source, 1.0F);
        if (victim.level() instanceof ServerLevel level) {
            ParticleFx.send(level, ParticleFx.dust(BLOOD, 0.9F), victim.getX(), victim.getY() + victim.getBbHeight()
                    * 0.55, victim.getZ(), 4 + amplifier * 2, victim.getBbWidth() * 0.35, victim.getBbHeight() * 0.25,
                    victim.getBbWidth() * 0.35, 0.0);
        }
        return true;
    }
}
