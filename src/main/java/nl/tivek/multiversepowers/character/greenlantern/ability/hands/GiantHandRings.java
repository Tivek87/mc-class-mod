package nl.tivek.multiversepowers.character.greenlantern.ability.hands;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.SCALE;

// What the ring hammer and the ring chains do.
abstract class GiantHandRings extends GiantHandMarvels {
    private static final double SMASH_REACH = 2.6;
    private static final double SMASH_DAMAGE = 2.2;
    private static final double QUAKE_REACH = 5.5;
    private static final double QUAKE_DAMAGE = 0.7;
    private static final double CHAIN_CATCH = 3.0;
    private static final double SQUEEZE_DAMAGE = 0.35;
    private static final double THROW_DAMAGE = 1.0;
    private static final double YANK_ARC = 1.5;

    @Nullable
    private Vec3 chainedFrom;

    GiantHandRings(GiantHands storm, int variant, Vec3 base, LivingEntity target) {
        super(storm, variant, base, target);
    }

    GiantHandRings(GiantHands storm, int variant, Vec3 base, LivingEntity target, Vec3 facing) {
        super(storm, variant, base, target, facing);
    }

    @Override
    void feat(ServerLevel level) {
        switch (this.move) {
            case HandPose.RINGHAMMER -> this.ringHammer(level);
            case HandPose.RINGCHAINS -> this.ringChains(level);
            default -> super.feat(level);
        }
    }

    private void ringHammer(ServerLevel level) {
        HandPose.Place place = this.place();
        if (this.t == HandPose.HAMMER_GLOWS) {
            Vec3 ring = place.at(HandPose.RING_POINT);
            this.storm.sound(level, ring, SoundEvents.BEACON_POWER_SELECT, 1.6F, 0.8F);
            this.storm.sound(level, ring, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.6F);
        }
        if (this.t == HandPose.HAMMER_FORMED) {
            Vec3 grip = place.at(HandPose.HAMMER_GRIP);
            this.storm.sound(level, grip, SoundEvents.ANVIL_PLACE, 1.2F, 0.7F);
            this.storm.sound(level, grip, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 0.6F);
        }
        if (this.t == HandPose.HAMMER_SWINGS) {
            Vec3 head = place.at(HandPose.HAMMER_HEAD);
            this.storm.sound(level, head, SoundEvents.ENDER_DRAGON_FLAP, 1.8F, 0.7F);
            this.storm.sound(level, head, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
        }
        if (this.t == HandPose.HAMMER_SMASHES) {
            this.smash(level, place.at(HandPose.HAMMER_HEAD));
        }
        if (this.t == HandPose.HAMMER_BREAKS) {
            Vec3 head = place.at(HandPose.HAMMER_HEAD);
            this.storm.sound(level, head, SoundEvents.GLASS_BREAK, 2.0F, 0.6F);
            this.storm.sound(level, head, SoundEvents.AMETHYST_CLUSTER_BREAK, 2.0F, 0.7F);
        }
    }

    // The head comes down: whatever is under it is crushed and thrown up, the ground round it quakes.
    private void smash(ServerLevel level, Vec3 head) {
        BlockHitResult ground = LoadedWorld.clip(level, new ClipContext(head.add(0.0, 1.5, 0.0),
                head.subtract(0.0, 5.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                CollisionContext.empty()));
        Vec3 impact = ground.getType() == HitResult.Type.MISS ? head : ground.getLocation();
        for (LivingEntity living : this.near(level, QUAKE_REACH + 8.0)) {
            Vec3 middle = living.getBoundingBox().getCenter();
            double dx = middle.x - impact.x;
            double dz = middle.z - impact.z;
            double flat = Math.sqrt(dx * dx + dz * dz) - living.getBbWidth() * 0.5;
            if (Math.abs(living.getY() - impact.y) > 3.0 || flat > QUAKE_REACH * SCALE) {
                continue;
            }
            Vec3 away = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(dx, 0.0, dz).normalize();
            if (flat <= SMASH_REACH * SCALE) {
                this.hit(level, living, this.storm.ability.getDamage() * SMASH_DAMAGE, away, 0.3, 0.9);
            } else {
                double left = 1.0 - flat / (QUAKE_REACH * SCALE);
                this.hit(level, living, this.storm.ability.getDamage() * QUAKE_DAMAGE * left, away, 0.9 * left,
                        0.45 * left);
            }
        }
        this.dustAt(level, impact, 40, 1.6);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.6F), impact.add(0.0, 0.2, 0.0), 36, 0.6);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), impact.add(0.0, 0.5, 0.0), 24, 0.45);
        ParticleFx.send(level, ParticleTypes.EXPLOSION, impact.x, impact.y + 0.5, impact.z, 1, 0.0, 0.0, 0.0, 0.0);
        this.storm.sound(level, impact, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.4F, 0.6F);
        this.storm.sound(level, impact, SoundEvents.ANVIL_LAND, 1.6F, 0.5F);
        this.storm.sound(level, impact, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.8F);
    }

    private void ringChains(ServerLevel level) {
        HandPose.Place place = this.place();
        Vec3 ring = place.at(HandPose.RING_POINT);
        Vec3 grip = place.at(HandPose.GRIP);
        if (this.t == HandPose.CHAINS_SHOOT) {
            this.storm.sound(level, ring, SoundEvents.BEACON_ACTIVATE, 1.8F, 1.3F);
            this.storm.sound(level, ring, SoundEvents.CHAIN_PLACE, 2.0F, 0.8F);
            this.storm.sound(level, ring, SoundEvents.CROSSBOW_SHOOT, 1.6F, 0.6F);
        }
        if (this.t == HandPose.CHAINS_REACH) {
            this.bind(level);
        }
        LivingEntity held = this.held;
        if (held != null && (!held.isAlive() || held.level() != level)) {
            this.letGo();
            return;
        }
        if (held == null || this.chainedFrom == null) {
            return;
        }
        if (this.t < HandPose.CHAINS_YANK) {
            this.hold(this.chainedFrom);
            if ((this.t - HandPose.CHAINS_REACH) % 3 == 0) {
                this.storm.sound(level, this.chainedFrom, SoundEvents.CHAIN_STEP, 1.4F, 0.7F);
            }
        } else if (this.t < HandPose.CHAINS_CATCH) {
            double pulled = HandPose.chainsPulled(this.t);
            Vec3 at = this.chainedFrom.lerp(grip, pulled).add(0.0, YANK_ARC * 4.0 * pulled * (1.0 - pulled), 0.0);
            this.hold(at);
            if (this.t == HandPose.CHAINS_YANK) {
                this.storm.sound(level, ring, SoundEvents.CHAIN_BREAK, 2.0F, 0.6F);
                this.storm.sound(level, ring, SoundEvents.ENDER_DRAGON_FLAP, 1.4F, 1.3F);
            }
        } else if (this.t < HandPose.CHAINS_THROW) {
            this.hold(grip);
            if (this.t == HandPose.CHAINS_CATCH) {
                this.storm.sound(level, grip, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 2.0F, 0.6F);
                this.storm.sound(level, grip, SoundEvents.GLASS_BREAK, 1.6F, 0.8F);
            }
            for (int squeeze : HandPose.CHAINS_SQUEEZE) {
                if (this.t == squeeze) {
                    this.hit(level, held, this.storm.ability.getDamage() * SQUEEZE_DAMAGE, Vec3.ZERO, 0.0, 0.0);
                    this.storm.sound(level, grip, SoundEvents.ANVIL_PLACE, 1.0F, 1.5F);
                }
            }
        } else {
            this.letGo();
            Vec3 away = new Vec3(held.getX() - this.storm.owner.getX(), 0.0, held.getZ() - this.storm.owner.getZ());
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : away.normalize();
            this.hit(level, held, this.storm.ability.getDamage() * THROW_DAMAGE, away, 1.6, 0.7);
            this.storm.sound(level, grip, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.0F, 0.6F);
            this.storm.sound(level, grip, SoundEvents.ENDER_DRAGON_FLAP, 1.6F, 1.0F);
        }
    }

    // The chains reach the creature: the nearest one small enough to hold round where the fist aimed is wound up.
    private void bind(ServerLevel level) {
        Vec3 aimed = new Vec3(this.aim.x, this.target.getY() + this.target.getBbHeight() * 0.5, this.aim.z);
        LivingEntity caught = null;
        double best = CHAIN_CATCH * SCALE;
        for (LivingEntity living : this.near(level, 16.0)) {
            double distance = living.getBoundingBox().getCenter().distanceTo(aimed);
            if (distance < best && this.holdable(living)) {
                best = distance;
                caught = living;
            }
        }
        if (caught == null) {
            this.storm.sound(level, aimed, SoundEvents.CHAIN_FALL, 1.4F, 1.0F);
            return;
        }
        this.take(caught);
        this.chainedFrom = caught.getBoundingBox().getCenter();
        this.storm.sound(level, this.chainedFrom, SoundEvents.CHAIN_PLACE, 2.0F, 0.6F);
        this.storm.sound(level, this.chainedFrom, SoundEvents.AMETHYST_BLOCK_PLACE, 1.6F, 0.7F);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), this.chainedFrom, 16, 0.5, 0.05);
    }
}
