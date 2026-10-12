package nl.tivek.multiversepowers.creature.wizard;

import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import nl.tivek.multiversepowers.MultiversePowers;

// His spells strike many times a second (a beam, a blizzard, a drain), and the game wears armor down a little with
// every hit: the Wizard's harm wears it as one blow a second would, and only a quarter as hard, so a good suit of armor
// lasts the fight.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class WizardWear {
    private static final int EVERY = 20;
    private static final float SHARE = 0.25F;
    private static final Map<LivingEntity, Integer> WORN = new WeakHashMap<>();
    // The creature his harm is reaching right now, between the blow's start and its end.
    @Nullable
    private static LivingEntity struck;

    private WizardWear() {
    }

    private static boolean his(DamageSource source) {
        Entity owner = source.getEntity();
        Entity direct = source.getDirectEntity();
        return owner != null && Wizard.ally(owner) || direct != null && Wizard.ally(direct);
    }

    // Every blow starts here, so one that never got as far as the end does not linger.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        struck = !event.isCanceled() && !event.getEntity().level().isClientSide && his(event.getSource())
                ? event.getEntity() : null;
    }

    @SubscribeEvent
    public static void onArmor(ArmorHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity != struck) {
            return;
        }
        int now = entity.tickCount;
        Integer last = WORN.get(entity);
        boolean wears = last == null || now - last >= EVERY || now < last;
        if (wears) {
            WORN.put(entity, now);
        }
        for (EquipmentSlot slot : event.getArmorMap().keySet()) {
            event.setNewDamage(slot, wears ? event.getOriginalDamage(slot) * SHARE : 0.0F);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() == struck) {
            struck = null;
        }
    }
}
