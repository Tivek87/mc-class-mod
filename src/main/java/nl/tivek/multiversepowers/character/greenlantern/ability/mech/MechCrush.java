package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.construct.FlattenPayload;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// What a walking mech's foot comes down on: a creature small and weak enough (a chicken, a rabbit) is crushed flat
// under it. Its pilot's own pets are stepped round.
final class MechCrush {
    // The sole round the point under its ankle, in blocks: to either side, ahead to the toes' claws, back to the heel's.
    private static final double ACROSS = 0.9;
    private static final double AHEAD = 1.5;
    private static final double BACK = 1.4;
    // How far above the ground the foot comes down onto a creature, and how far below it one may stand.
    private static final double HIGH = 1.5;
    private static final double LOW = 0.5;
    // No bigger than this every way, in blocks.
    private static final double SMALL = 1.0;

    private MechCrush() {
    }

    static void under(ServerLevel level, ServerPlayer owner, CharacterAbility ability, Vec3 sole, Vec3 ahead,
            @Nullable Entity held) {
        double weakest = ability.value("mechCrushHealth");
        if (weakest <= 0.0) {
            return;
        }
        Vec3 across = new Vec3(-ahead.z, 0.0, ahead.x);
        double reach = Math.max(AHEAD, BACK) + SMALL;
        AABB around = new AABB(sole.x - reach, sole.y - LOW, sole.z - reach, sole.x + reach, sole.y + HIGH,
                sole.z + reach);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, around,
                entity -> entity != held && PowerRing.canHit(owner, entity))) {
            if (living.getMaxHealth() > weakest || living.getBbWidth() > SMALL || living.getBbHeight() > SMALL
                    || HeldMobs.isHeldByAnyone(living)
                    || living instanceof OwnableEntity pet && owner.getUUID().equals(pet.getOwnerUUID())) {
                continue;
            }
            Vec3 to = living.position().subtract(sole);
            double half = living.getBbWidth() * 0.5;
            double along = to.x * ahead.x + to.z * ahead.z;
            double side = to.x * across.x + to.z * across.z;
            if (along <= AHEAD + half && along >= -BACK - half && Math.abs(side) <= ACROSS + half) {
                crush(level, owner, living);
            }
        }
    }

    private static void crush(ServerLevel level, ServerPlayer owner, LivingEntity living) {
        FlattenPayload.send(living);
        living.invulnerableTime = 0;
        living.hurt(level.damageSources().playerAttack(owner),
                (living.getMaxHealth() + living.getAbsorptionAmount()) * 10.0F);
        // The blow would throw it out from under the foot: it stays where it was pressed flat.
        living.setDeltaMovement(Vec3.ZERO);
        living.hurtMarked = true;
        Vec3 at = living.position();
        Sounds.play(level, at, SoundEvents.SLIME_SQUISH, 1.2F, 0.6F);
        ParticleFx.cloud(level, ParticleTypes.POOF, at.add(0.0, 0.2, 0.0), 6, 0.3, 0.05);
    }
}
