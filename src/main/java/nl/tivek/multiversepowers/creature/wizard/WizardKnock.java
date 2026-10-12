package nl.tivek.multiversepowers.creature.wizard;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneFamiliar;

// How hard the Wizard's blows and spells throw: KNOCK of what they would, both the pushes his spells give themselves
// (knock) and the game's own knock back of a blow struck in his name.
public final class WizardKnock {
    public static final double KNOCK = 0.3;

    private WizardKnock() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(WizardKnock::onKnockBack);
    }

    public static Vec3 knock(Vec3 push) {
        return push.scale(KNOCK);
    }

    private static void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        LivingEntity by = target.getLastHurtByMob();
        if ((by instanceof Wizard || by instanceof ArcaneFamiliar)
                && target.getLastHurtByMobTimestamp() == target.tickCount) {
            event.setStrength((float) (event.getStrength() * KNOCK));
        }
    }
}
