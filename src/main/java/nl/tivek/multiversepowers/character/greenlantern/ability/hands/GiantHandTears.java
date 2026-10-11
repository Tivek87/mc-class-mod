package nl.tivek.multiversepowers.character.greenlantern.ability.hands;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandCosmos;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandGroup;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandRift;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandVictimPayload;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// What the tear and the rift do: two hands out of portals, above and below, draw a creature out until it tears in two;
// or two tear open a rift whose tentacle pulls a creature in, burns it, hurls out what is left and shuts in a blast.
abstract class GiantHandTears extends GiantHandRings {
    private static final double CATCH_REACH = 3.0;
    private static final double STRAIN_DAMAGE = 0.12;
    private static final double TEAR_DAMAGE = 3.0;
    private static final double THROW_OUT = 1.2;
    private static final double THROW_DOWN = -0.8;
    private static final double BURN_DAMAGE = 0.65;
    private static final double EJECT_DAMAGE = 0.3;
    private static final double EJECT_OUT = 0.8;
    private static final double EJECT_UP = 1.7;
    private static final double EJECT_WALL_UP = 1.0;
    private static final double BLAST_REACH = 4.5;
    private static final double BLAST_DAMAGE = 1.0;
    private static final double BLAST_OUT = 1.2;
    private static final double BLAST_UP = 0.8;

    // Where the rift's tentacle caught its creature, which it drags from there.
    @Nullable
    private Vec3 caughtAt;

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
        } else if (this.move == HandPose.RIFT) {
            this.rift(level);
        } else if (this.move == HandPose.COSMOS) {
            this.cosmos(level);
        } else {
            super.feat(level);
        }
    }

    // The Cosmos Test's sounds, heard by everyone near: the circle chiming as it is drawn, the hole opening, the hand
    // diving in, the moon taken, cracking as it is crushed and bursting, and the moon coming back.
    private void cosmos(ServerLevel level) {
        Vec3 facing = this.aim.subtract(this.base);
        if (this.t == 2) {
            this.storm.sound(level, this.base, SoundEvents.BEACON_ACTIVATE, 1.6F, 1.4F);
        }
        if (this.t >= HandCosmos.DRAWS && this.t < HandCosmos.OPENS && (this.t - HandCosmos.DRAWS) % 4 == 0) {
            float rise = (this.t - HandCosmos.DRAWS) / (float) (HandCosmos.OPENS - HandCosmos.DRAWS);
            this.storm.sound(level, HandCosmos.tip(this.base, facing, this.t), SoundEvents.AMETHYST_BLOCK_CHIME,
                    1.2F, 0.8F + 1.0F * rise);
        }
        if (this.t == HandCosmos.OPENS) {
            this.storm.sound(level, this.base, SoundEvents.END_PORTAL_SPAWN, 0.7F, 1.6F);
            this.storm.sound(level, this.base, SoundEvents.BEACON_POWER_SELECT, 1.6F, 0.7F);
        }
        if (this.t == HandCosmos.DIVES) {
            this.storm.sound(level, this.base.add(0.0, 8.0, 0.0), SoundEvents.PHANTOM_SWOOP, 3.0F, 0.6F);
        }
        if (this.t == HandCosmos.PLUNGES) {
            this.storm.sound(level, this.base, SoundEvents.ENDERMAN_TELEPORT, 2.0F, 0.5F);
            this.storm.sound(level, this.base, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.6F);
        }
        if (this.t == HandCosmos.SHUTS) {
            this.storm.sound(level, this.base, SoundEvents.BEACON_DEACTIVATE, 1.6F, 1.2F);
        }
        if (this.t == HandCosmos.TRACES) {
            this.storm.sound(level, this.base, SoundEvents.AMETHYST_BLOCK_RESONATE, 4.0F, 0.6F);
        }
        if (this.t == HandCosmos.TEARS) {
            this.storm.sound(level, this.base, SoundEvents.BEACON_ACTIVATE, 4.0F, 0.5F);
        }
        if (this.t == HandCosmos.GRABS) {
            this.storm.sound(level, this.base, SoundEvents.ANVIL_LAND, 3.0F, 0.5F);
        }
        if (this.t >= HandCosmos.CRUSHES && this.t < HandCosmos.BURSTS) {
            int every = Math.max(2, 12 - (this.t - HandCosmos.CRUSHES) / 8);
            if ((this.t - HandCosmos.CRUSHES) % every == 0) {
                float rise = (this.t - HandCosmos.CRUSHES) / (float) (HandCosmos.BURSTS - HandCosmos.CRUSHES);
                this.storm.sound(level, this.base, SoundEvents.DEEPSLATE_BREAK, 2.0F + 2.0F * rise,
                        0.5F + 0.3F * rise);
            }
        }
        if (this.t == HandCosmos.BURSTS) {
            this.storm.sound(level, this.base, SoundEvents.GENERIC_EXPLODE.value(), 8.0F, 0.5F);
            this.storm.sound(level, this.base, SoundEvents.LIGHTNING_BOLT_THUNDER, 8.0F, 0.6F);
            this.storm.sound(level, this.base, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 8.0F, 0.5F);
        }
        if (this.t == HandCosmos.RETURNS) {
            this.storm.sound(level, this.base, SoundEvents.BEACON_POWER_SELECT, 4.0F, 1.6F);
            this.storm.sound(level, this.base, SoundEvents.AMETHYST_CLUSTER_BREAK, 3.0F, 1.4F);
        }
    }

    private void rift(ServerLevel level) {
        HandRift.Frame frame = HandRift.frame(this.variant, this.base, this.aim.subtract(this.base));
        Vec3 mouth = frame.at(0.0, 0.0, 0.6);
        if (this.t == 2 || this.t == HandRift.LEAVES + 6) {
            boolean opening = this.t == 2;
            for (HandGroup.Sub sub : HandGroup.at(this.variant, this.base, this.aim.subtract(this.base), this.t)) {
                if (opening) {
                    this.opens(level, sub.portal(), sub.left() ? 1.2F : 1.4F);
                } else {
                    this.shuts(level, sub.portal(), sub.left() ? 1.5F : 1.7F);
                }
            }
        }
        if (this.t == HandRift.DIGS) {
            this.dustAt(level, frame.at(0.0, -0.4, 0.0), 20, 0.6);
            this.dustAt(level, frame.at(0.0, 0.4, 0.0), 20, 0.6);
            this.storm.sound(level, mouth, SoundEvents.ANVIL_LAND, 1.6F, 0.5F);
            this.storm.sound(level, mouth, SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.5F);
            this.storm.sound(level, mouth, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.4F);
        }
        for (int k = 0; k < HandRift.TEARS.length; k++) {
            if (this.t == HandRift.TEARS[k]) {
                this.tearOpen(level, frame, k);
            }
        }
        if (this.t == HandRift.LASHES) {
            this.storm.sound(level, mouth, SoundEvents.ENDER_DRAGON_FLAP, 1.6F, 1.3F);
            this.storm.sound(level, mouth, SoundEvents.SLIME_ATTACK, 1.6F, 0.5F);
        }
        if (this.t == HandRift.SNARES) {
            this.snare(level, mouth);
        }
        this.drag(level, frame);
        if (this.t == HandRift.SHUTS) {
            this.blast(level, frame);
        }
    }

    // Each jerk tears the rift a third wider: the ground groans and its sky shows deeper.
    private void tearOpen(ServerLevel level, HandRift.Frame frame, int k) {
        Vec3 middle = frame.at(0.0, 0.0, 0.2);
        for (double along = -1.0; along <= 1.0; along += 1.0) {
            this.dustAt(level, frame.at(along * HandRift.HALF * 0.6, 0.0, 0.0), 10, 0.8);
        }
        ParticleFx.cloud(level, ParticleTypes.END_ROD, middle, 6 + 4 * k, 1.2, 0.08);
        float rise = 0.6F + 0.2F * k;
        this.storm.sound(level, middle, SoundEvents.ROOTED_DIRT_BREAK, 2.2F, rise);
        this.storm.sound(level, middle, SoundEvents.DEEPSLATE_BREAK, 2.0F, 0.5F + 0.1F * k);
        this.storm.sound(level, middle, SoundEvents.WOOL_BREAK, 2.0F, rise);
        if (k == HandRift.TEARS.length - 1) {
            this.storm.sound(level, middle, SoundEvents.PORTAL_TRIGGER, 1.0F, 1.6F);
            this.storm.sound(level, middle, SoundEvents.BEACON_ACTIVATE, 1.6F, 0.6F);
        }
    }

    // The tentacle winds round the creature nearest the rift within its reach, small enough to hold and out to hurt
    // the caster, its own creature first.
    private void snare(ServerLevel level, Vec3 mouth) {
        LivingEntity caught = null;
        double best = HandRift.LASH_REACH;
        for (LivingEntity living : this.near(level, HandRift.LASH_REACH + 2.0)) {
            Vec3 middle = living.getBoundingBox().getCenter();
            double far = middle.distanceTo(mouth) - (living == this.target ? 2.0 : 0.0);
            if (far < best && GiantHands.fair(this.storm.owner, living) && this.holdable(living)
                    && GiantHandSpots.sees(level, mouth, living)) {
                best = far;
                caught = living;
            }
        }
        this.storm.sound(level, mouth, SoundEvents.PLAYER_ATTACK_SWEEP, 1.8F, 0.5F);
        if (caught == null) {
            return;
        }
        this.caughtAt = caught.getBoundingBox().getCenter();
        this.take(caught);
        this.storm.sound(level, this.caughtAt, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 2.0F, 0.6F);
        this.storm.sound(level, this.caughtAt, SoundEvents.SLIME_SQUISH, 1.6F, 0.6F);
    }

    // Dragged over and pulled in, burned while inside, and hurled back out if still alive: one dead in there stays.
    private void drag(ServerLevel level, HandRift.Frame frame) {
        LivingEntity held = this.held;
        if (held == null || this.caughtAt == null) {
            return;
        }
        if (held.isRemoved() || held.level() != level) {
            this.letGo();
            return;
        }
        double extent = HandRift.extent(this.variant, held.getBbWidth(), held.getBbHeight());
        Vec3 middle = HandRift.held(frame, this.caughtAt, extent, this.t);
        if (this.t == HandRift.SINKS) {
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), frame.at(0.0, 0.0, 0.4), 24, 0.4);
            this.storm.sound(level, middle, SoundEvents.ENDERMAN_TELEPORT, 1.8F, 0.5F);
            this.storm.sound(level, middle, SoundEvents.PORTAL_TRAVEL, 0.4F, 1.8F);
        }
        for (int burn : HandRift.BURNS_AT) {
            if (this.t == burn && held.isAlive()) {
                this.hit(level, held, this.storm.ability.getDamage() * BURN_DAMAGE, Vec3.ZERO, 0.0, 0.0);
                Vec3 glow = frame.at(0.0, 0.0, 0.3);
                ParticleFx.cloud(level, ParticleTypes.END_ROD, glow, 10, 0.6, 0.12);
                this.storm.sound(level, glow, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
                this.storm.sound(level, glow, SoundEvents.BEACON_POWER_SELECT, 1.4F, 0.6F);
            }
        }
        if (this.t < HandRift.EJECTS || !held.isAlive()) {
            this.hold(middle);
            return;
        }
        this.letGo();
        boolean wall = HandPose.wall(this.variant);
        hold(held, frame.at(0.0, 0.0, extent + 0.3));
        this.hit(level, held, this.storm.ability.getDamage() * EJECT_DAMAGE, wall ? this.alongWall(frame, held)
                : frame.along().scale(-1.0), EJECT_OUT, wall ? EJECT_WALL_UP : EJECT_UP);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), frame.at(0.0, 0.0, 0.5), 30, 0.6);
        ParticleFx.cloud(level, ParticleTypes.END_ROD, frame.at(0.0, 0.0, 0.5), 16, 0.5, 0.25);
        this.storm.sound(level, middle, SoundEvents.FIREWORK_ROCKET_LAUNCH, 2.0F, 0.6F);
        this.storm.sound(level, middle, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.0F, 0.5F);
    }

    // Straight out of a wall would fly at a caster facing it, which a hand never does: out and along the wall, on the
    // side of the caster's line the creature is on.
    private Vec3 alongWall(HandRift.Frame frame, LivingEntity held) {
        double side = held.position().subtract(this.storm.owner.position()).dot(frame.across());
        return frame.across().scale(side < 0.0 ? -1.0 : 1.0).add(frame.normal().scale(0.4)).normalize();
    }

    // The rift slams shut: a burst of starlight that throws everything round it away.
    private void blast(ServerLevel level, HandRift.Frame frame) {
        Vec3 middle = frame.at(0.0, 0.0, 0.6);
        for (LivingEntity living : this.near(level, BLAST_REACH + 4.0)) {
            if (living == this.held) {
                continue;
            }
            Vec3 to = living.getBoundingBox().getCenter().subtract(middle);
            double far = to.length();
            if (far > BLAST_REACH + living.getBbWidth() * 0.5) {
                continue;
            }
            Vec3 out = new Vec3(to.x, 0.0, to.z);
            out = out.lengthSqr() < 1.0E-4 ? frame.along() : out.normalize();
            double share = 1.0 - 0.5 * Math.min(1.0, far / BLAST_REACH);
            this.hit(level, living, this.storm.ability.getDamage() * BLAST_DAMAGE * share, out, BLAST_OUT, BLAST_UP);
        }
        ParticleFx.send(level, ParticleTypes.FLASH, middle.x, middle.y, middle.z, 1, 0.0, 0.0, 0.0, 0.0);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.8F), middle, 48, 0.8);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.6F), frame.at(0.0, 0.0, 0.15), 40, 0.7);
        ParticleFx.cloud(level, ParticleTypes.END_ROD, middle, 30, 1.2, 0.3);
        this.storm.sound(level, middle, SoundEvents.END_PORTAL_SPAWN, 1.2F, 1.4F);
        this.storm.sound(level, middle, SoundEvents.GENERIC_EXPLODE.value(), 2.4F, 0.7F);
        this.storm.sound(level, middle, SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.6F);
        this.storm.sound(level, middle, SoundEvents.AMETHYST_CLUSTER_BREAK, 2.0F, 0.5F);
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
