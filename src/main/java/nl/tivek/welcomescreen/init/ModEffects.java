package nl.tivek.welcomescreen.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.effect.VoidInstabilityEffect;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT,
            WelcomeScreenMod.MODID);

    public static final DeferredHolder<MobEffect, VoidInstabilityEffect> VOID_INSTABILITY = MOB_EFFECTS.register(
            "void_instability", VoidInstabilityEffect::new);

    private ModEffects() {
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
