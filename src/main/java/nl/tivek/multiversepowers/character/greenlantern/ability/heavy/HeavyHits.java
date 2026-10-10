package nl.tivek.multiversepowers.character.greenlantern.ability.heavy;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;

// What the heavy weapons' blows catch and how they land: an arc ahead, a ring all round, a line along the ground and
// the one creature straight ahead.
abstract class HeavyHits {
    // A blow catches what its blade passes within this of, besides the reach.
    private static final double SLACK = 0.5;

    final ServerPlayer owner;
    Vec3 ahead = new Vec3(0.0, 0.0, 1.0);

    HeavyHits(ServerPlayer owner) {
        this.owner = owner;
    }

    static CharacterAbility wheel() {
        return GameCharacter.GREEN_LANTERN.byName("construct_wheel");
    }

    static double value(String key) {
        return wheel().value(key);
    }

    // Everything within `reach` ahead and within the arc (cosine of its half) to either side of his look.
    final List<LivingEntity> arc(ServerLevel level, double reach, double arc) {
        Vec3 eye = this.owner.getEyePosition();
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity living : this.near(level, eye, reach + SLACK)) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(eye);
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            boolean close = living.getBoundingBox().inflate(SLACK).contains(eye);
            if (close || to.length() <= reach + SLACK + living.getBbWidth() * 0.5 && flat.lengthSqr() > 1.0E-6
                    && flat.normalize().dot(this.ahead) >= arc) {
                hit.add(living);
            }
        }
        return hit;
    }

    // Everything round `middle` within `radius`, on the ground's level.
    final List<LivingEntity> around(ServerLevel level, Vec3 middle, double radius) {
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity living : this.near(level, middle, radius + 1.0)) {
            Vec3 to = living.position().subtract(middle);
            if (Math.abs(to.y) < 2.5 && Math.hypot(to.x, to.z) <= radius + living.getBbWidth() * 0.5) {
                hit.add(living);
            }
        }
        return hit;
    }

    // Everything within `wide` of the line from `from` to `to`.
    final List<LivingEntity> line(ServerLevel level, Vec3 from, Vec3 to, double wide) {
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(wide + 1.0),
                living -> living != this.owner && living.isAlive() && PowerRing.canHit(this.owner, living))) {
            AABB box = living.getBoundingBox().inflate(wide);
            if (box.contains(from) || box.clip(from, to).isPresent()) {
                hit.add(living);
            }
        }
        return hit;
    }

    // The nearest creature straight ahead of his eyes within `reach`.
    @Nullable
    final LivingEntity ahead(ServerLevel level, double reach) {
        Vec3 eye = this.owner.getEyePosition();
        Vec3 end = eye.add(this.owner.getLookAngle().scale(reach));
        LivingEntity best = null;
        double bestFar = Double.MAX_VALUE;
        for (LivingEntity living : this.near(level, eye, reach + SLACK)) {
            AABB box = living.getBoundingBox().inflate(SLACK);
            if (!box.contains(eye) && box.clip(eye, end).isEmpty()) {
                continue;
            }
            double far = living.distanceToSqr(eye);
            if (far < bestFar) {
                bestFar = far;
                best = living;
            }
        }
        return best;
    }

    private List<LivingEntity> near(ServerLevel level, Vec3 middle, double reach) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(middle, middle).inflate(reach),
                living -> living != this.owner && living.isAlive() && PowerRing.canHit(this.owner, living));
    }

    // A hit as the owner's own (which knocks back by itself): its own push is set after it, and `down` knocks it
    // down first.
    final boolean strike(ServerLevel level, LivingEntity living, double damage, Vec3 push, boolean down) {
        living.invulnerableTime = 0;
        if (!living.hurt(level.damageSources().playerAttack(this.owner), (float) damage)) {
            return false;
        }
        if (down && living.isAlive()) {
            Knockdowns.knock(living);
        }
        double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        living.setDeltaMovement(push.scale(1.0 - resist));
        living.hasImpulse = true;
        living.hurtMarked = true;
        return true;
    }

    // Holds a creature where it is: it neither walks nor is thrown off while the blade is in it.
    static void pin(LivingEntity living) {
        living.setDeltaMovement(0.0, Math.min(0.0, living.getDeltaMovement().y), 0.0);
        living.hurtMarked = true;
    }

    final void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
