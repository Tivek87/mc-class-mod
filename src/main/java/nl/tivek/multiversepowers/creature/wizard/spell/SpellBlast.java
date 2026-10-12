package nl.tivek.multiversepowers.creature.wizard.spell;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardDecoy;
import nl.tivek.multiversepowers.creature.wizard.WizardKnock;

// Where one of the Wizard's spells bursts: an explosion that breaks nothing and harms no one by itself, only throwing
// what stands near (never him or his decoy), and the spell's own harm on his foes within reach, less towards its edge.
public final class SpellBlast {
    private static final ExplosionDamageCalculator THROWS = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return false;
        }

        @Override
        public float getKnockbackMultiplier(Entity entity) {
            return entity instanceof Wizard || entity instanceof WizardDecoy || entity instanceof ArcaneFamiliar ? 0.0F
                    : (float) WizardKnock.KNOCK;
        }
    };

    private SpellBlast() {
    }

    // Who it struck.
    public static List<LivingEntity> burst(ServerLevel level, Entity spell, @Nullable Entity owner, Vec3 at, float power,
            boolean fire, double reach, float damage, ResourceKey<DamageType> type) {
        level.explode(spell, null, THROWS, at.x, at.y, at.z, power, fire, Level.ExplosionInteraction.NONE);
        List<LivingEntity> struck = new ArrayList<>();
        if (!(owner instanceof Mob mob)) {
            return struck;
        }
        for (LivingEntity living : MonsterBlows.foes(mob, new AABB(at, at).inflate(reach))) {
            double far = living.getBoundingBox().getCenter().distanceTo(at);
            if (far > reach || living instanceof WizardDecoy) {
                continue;
            }
            double share = 1.0 - far / reach;
            if (living.hurt(Wizard.source(spell, owner, type), (float) (damage * (0.4 + 0.6 * share)))) {
                struck.add(living);
            }
        }
        return struck;
    }
}
