package nl.tivek.multiversepowers.creature.elemental.spell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// Frozen, from the frost ray: held fast in ice for a few seconds, unable to walk, fly or jump, the screen rimmed with
// frost as powder snow does it (kept just short of where the cold starts to hurt), flakes of snow falling off.
public final class Frozen extends MobEffect {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT,
            MultiversePowers.MODID);
    public static final DeferredHolder<MobEffect, Frozen> FROZEN = EFFECTS.register("frozen", Frozen::new);
    private static final ResourceLocation HELD = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "frozen");
    private static final int ICE = 0x9ADFFF;
    // Powder snow's frost on the screen starts to hurt at 140.
    private static final int RIM = 136;

    private Frozen() {
        super(MobEffectCategory.HARMFUL, ICE);
        AttributeModifier.Operation all = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, HELD, -1.0, all);
        this.addAttributeModifier(Attributes.FLYING_SPEED, HELD, -1.0, all);
        this.addAttributeModifier(Attributes.JUMP_STRENGTH, HELD, -1.0, all);
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity victim, int amplifier) {
        if (victim.canFreeze()) {
            victim.setTicksFrozen(Math.max(victim.getTicksFrozen(), RIM));
        }
        if (victim.level() instanceof ServerLevel level && victim.tickCount % 4 == 0) {
            ParticleFx.send(level, ParticleTypes.SNOWFLAKE, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5,
                    victim.getZ(), 2, victim.getBbWidth() * 0.4, victim.getBbHeight() * 0.3, victim.getBbWidth() * 0.4,
                    0.0);
            ParticleFx.send(level, ParticleFx.dust(ICE, 0.8F), victim.getX(), victim.getY() + victim.getBbHeight()
                    * 0.3, victim.getZ(), 1, victim.getBbWidth() * 0.4, victim.getBbHeight() * 0.3,
                    victim.getBbWidth() * 0.4, 0.0);
        }
        return true;
    }
}
