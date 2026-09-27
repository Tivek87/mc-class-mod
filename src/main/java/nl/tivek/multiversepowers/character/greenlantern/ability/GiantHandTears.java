package nl.tivek.multiversepowers.character.greenlantern.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.HandGroup;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.HandVictimPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// What the tear does: two hands out of portals, above and below, draw a creature out until it tears in two.
abstract class GiantHandTears extends GiantHandRings {
    private static final double CATCH_REACH = 3.0;
    private static final double STRAIN_DAMAGE = 0.12;
    private static final double TEAR_DAMAGE = 3.0;
    private static final double THROW_OUT = 1.2;
    private static final double THROW_DOWN = -0.8;

    GiantHandTears(GiantHands storm, int variant, Vec3 base, LivingEntity target) {
        super(storm, variant, base, target);
    }

    GiantHandTears(GiantHands storm, int variant, Vec3 base, LivingEntity target, Vec3 facing) {
        super(storm, variant, base, target, facing);
    }

    @Override
    void feat(ServerLevel level) {
        if (this.move == HandPose.TEAR) {
            this.tear(level);
        } else {
            super.feat(level);
        }
    }

    private void tear(ServerLevel level) {
        Vec3 middle = HandGroup.tearHeld(this.base, this.t);
        if (this.t == 2) {
            this.storm.sound(level, this.base, SoundEvents.BEACON_ACTIVATE, 1.6F, 1.3F);
            this.storm.sound(level, this.base, SoundEvents.ENDER_DRAGON_FLAP, 1.2F, 1.4F);
        }
        if (this.t == HandGroup.TEAR_GRABS) {
            this.grip(level, middle);
        }
        LivingEntity held = this.held;
        if (held != null && (!held.isAlive() || held.level() != level)) {
            this.letGo();
            return;
        }
        if (held == null) {
            return;
        }
        if (this.t < HandGroup.TEARS) {
            this.hold(middle);
            int pulled = this.t - HandGroup.TEAR_PULLS;
            if (pulled >= 0 && pulled % HandGroup.TEAR_PULL_EVERY == 0) {
                float rise = pulled / (float) (HandGroup.TEARS - HandGroup.TEAR_PULLS);
                this.hit(level, held, this.storm.ability.getDamage() * STRAIN_DAMAGE, Vec3.ZERO, 0.0, 0.0);
                this.storm.sound(level, middle, SoundEvents.CHAIN_STEP, 1.6F, 0.6F + 0.4F * rise);
                this.storm.sound(level, middle, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F, 0.5F + 0.9F * rise);
            }
            return;
        }
        // The last jerk: whatever it does not kill it hurls down and away.
        this.letGo();
        this.hit(level, held, this.storm.ability.getDamage() * TEAR_DAMAGE, Vec3.ZERO, 0.0, 0.0);
        if (held.isDeadOrDying()) {
            // The halves are drawn where it tore: the body, not knocked away, falls straight down under them.
            held.setDeltaMovement(Vec3.ZERO);
            HandVictimPayload.send(held, this.id(), HandVictimPayload.SPLIT);
        } else {
            this.push(held, Vec3.ZERO, THROW_OUT, THROW_DOWN);
        }
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.5F), middle, 30, 0.5);
        ParticleFx.cloud(level, ParticleTypes.CRIT, middle, 16, 0.4, 0.3);
        this.storm.sound(level, middle, SoundEvents.SHIELD_BREAK, 2.0F, 0.6F);
        this.storm.sound(level, middle, SoundEvents.WOOL_BREAK, 2.0F, 0.5F);
        this.storm.sound(level, middle, SoundEvents.AMETHYST_CLUSTER_BREAK, 2.0F, 0.6F);
        this.storm.sound(level, middle, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.6F);
    }

    // Both hands close on the creature nearest to where they meet, small enough to hold.
    private void grip(ServerLevel level, Vec3 middle) {
        LivingEntity caught = null;
        double best = CATCH_REACH;
        for (LivingEntity living : this.near(level, 8.0)) {
            double distance = living.getBoundingBox().getCenter().distanceTo(middle);
            if (distance < best && this.holdable(living)) {
                best = distance;
                caught = living;
            }
        }
        if (caught != null) {
            this.take(caught);
            HandVictimPayload.send(caught, this.id(), HandVictimPayload.STRETCH);
        }
        this.storm.sound(level, middle, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 2.0F, 0.6F);
        this.storm.sound(level, middle, SoundEvents.AMETHYST_BLOCK_PLACE, 2.0F, 0.7F);
    }
}
