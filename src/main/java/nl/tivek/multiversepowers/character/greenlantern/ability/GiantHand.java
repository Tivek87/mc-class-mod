package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.FlattenPayload;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.GRABBED;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.GRAB_TALL;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.GRAB_WIDE;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.SCALE;

final class GiantHand extends GiantHandPair {
    private static final double BURST_REACH = 4.5;
    private static final double BURST_DAMAGE = 2.5;
    private static final double BURST_OUT = 1.8;
    private static final double BURST_UP = 1.7;
    private static final int FLAT_TICKS = 50;

    GiantHand(GiantHands storm, int variant, Vec3 base, LivingEntity target) {
        super(storm, variant, base, target);
    }

    boolean tick(ServerLevel level, boolean fuels) {
        this.t++;
        if (!fuels) {
            this.end(level);
            return true;
        }
        this.home(level);
        if (this.t == HandPose.ARRIVES && this.move != HandPose.AXE) {
            this.burstOut(level);
        }
        switch (this.move) {
            case HandPose.SMACK -> {
                if (this.t == HandPose.SMACK_HITS) {
                    this.smack(level);
                }
            }
            case HandPose.GRAB -> this.grab(level);
            case HandPose.FINGER -> {
                if (this.t == HandPose.FINGER_BURSTS) {
                    this.burst(level);
                }
                if (this.t == HandPose.FINGER_UP) {
                    Vec3 tip = this.place().at(new Vec3(-0.38, 6.3, 0.0));
                    ParticleFx.send(level, ParticleTypes.ANGRY_VILLAGER, tip.x, tip.y + 0.5, tip.z, 4, 0.6, 0.3,
                            0.6, 0.0);
                    this.storm.sound(level, tip, SoundEvents.VILLAGER_NO, 1.6F, 0.5F);
                    this.storm.sound(level, tip, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.7F);
                }
            }
            case HandPose.SLAM -> this.slam(level);
            case HandPose.AXE -> this.pair(level);
            default -> {
                for (int hit : HandPose.POUND_HITS) {
                    if (this.t == hit) {
                        this.pound(level);
                    }
                }
            }
        }
        if (this.t == HandPose.sinks(this.variant) && this.move != HandPose.AXE) {
            this.storm.sound(level, this.base, SoundEvents.ROOTED_DIRT_BREAK, 1.2F, 0.6F);
            this.dust(level, 12);
        }
        if (this.t >= HandPose.life(this.variant)) {
            this.end(level);
            return true;
        }
        this.send(level);
        return false;
    }

    private void burstOut(ServerLevel level) {
        this.dust(level, 36);
        ParticleFx.send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.base.x, this.base.y + 0.4, this.base.z, 5,
                1.2, 0.3, 1.2, 0.015);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.4F), this.base.add(0.0, 0.2, 0.0), 24, 0.45);
        this.storm.sound(level, this.base, SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 1.5F);
        this.storm.sound(level, this.base, SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.5F);
        this.storm.sound(level, this.base, SoundEvents.BEACON_POWER_SELECT, 1.2F, 1.7F);
    }

    private void burst(ServerLevel level) {
        Vec3 ahead = this.aim.subtract(this.base);
        ahead = new Vec3(ahead.x, 0.0, ahead.z);
        ahead = ahead.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : ahead.normalize();
        double reach = BURST_REACH * SCALE;
        for (LivingEntity living : this.near(level, BURST_REACH + 3.0)) {
            Vec3 to = living.position().subtract(this.base);
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            if (flat.length() > reach + living.getBbWidth() * 0.5 || to.y < -2.5 || to.y > 5.0 * SCALE) {
                continue;
            }
            Vec3 away = flat.lengthSqr() < 1.0E-4 ? ahead : flat.normalize();
            this.hit(level, living, this.storm.ability.getDamage() * BURST_DAMAGE, away, BURST_OUT, BURST_UP);
            ParticleFx.send(level, ParticleTypes.EXPLOSION, living.getX(), living.getY() + 0.6, living.getZ(), 1,
                    0.0, 0.0, 0.0, 0.0);
        }
        Vec3 fist = this.base.add(0.0, 0.3, 0.0);
        this.dust(level, 60);
        ParticleFx.send(level, ParticleTypes.EXPLOSION_EMITTER, fist.x, fist.y, fist.z, 1, 0.0, 0.0, 0.0, 0.0);
        ParticleFx.send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, fist.x, fist.y + 0.4, fist.z, 10, 1.6, 0.5, 1.6,
                0.03);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.8F), fist, 48, 0.9);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), fist.add(0.0, 1.5, 0.0), 28, 0.55);
        this.storm.sound(level, fist, SoundEvents.GENERIC_EXPLODE.value(), 2.4F, 0.7F);
        this.storm.sound(level, fist, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.4F, 0.5F);
        this.storm.sound(level, fist, SoundEvents.ANVIL_LAND, 1.2F, 0.5F);
        this.storm.sound(level, fist, SoundEvents.ROOTED_DIRT_BREAK, 2.4F, 0.4F);
    }

    private void smack(ServerLevel level) {
        List<Vec3> path = new ArrayList<>();
        double from = this.t - HandPose.SWING_TICKS * 0.6;
        for (double at = from; at <= this.t + HandPose.SWING_TICKS * 0.25; at += 0.5) {
            path.add(this.pose(at).place(this.base, this.aim.subtract(this.base), SCALE).at(HandPose.PALM));
        }
        HandPose.Place now = this.place();
        Vec3 swat = new Vec3(now.forward().x, 0.0, now.forward().z);
        swat = swat.lengthSqr() < 1.0E-6 ? now.forward() : swat.normalize();
        double reach = 2.3 * SCALE;
        for (LivingEntity living : this.near(level, 12.0)) {
            Vec3 middle = living.getBoundingBox().getCenter();
            if (distance(path, middle) > reach + living.getBbWidth() * 0.5) {
                continue;
            }
            this.hit(level, living, this.storm.ability.getDamage(), swat, 1.2, 0.55);
        }
        Vec3 palm = now.at(HandPose.PALM);
        this.storm.sound(level, palm, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.0F, 0.6F);
        this.storm.sound(level, palm, SoundEvents.ANVIL_LAND, 0.8F, 1.6F);
        this.storm.sound(level, palm, SoundEvents.AMETHYST_BLOCK_HIT, 1.6F, 0.7F);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), palm, 16, 0.3);
    }

    private void grab(ServerLevel level) {
        HandPose.Place place = this.place();
        Vec3 grip = place.at(HandPose.GRIP);
        if (this.t == HandPose.GRAB_CATCHES) {
            LivingEntity caught = null;
            double best = 2.4 * SCALE;
            for (LivingEntity living : this.near(level, 6.0)) {
                double distance = living.getBoundingBox().getCenter().distanceTo(grip);
                if (distance < best && living.getBbWidth() <= GRAB_WIDE && living.getBbHeight() <= GRAB_TALL
                        && !HeldMobs.isHeldByAnyone(living) && !LightBubble.trapped(living)) {
                    best = distance;
                    caught = living;
                }
            }
            if (caught != null) {
                this.held = caught;
                GRABBED.put(caught.getId(), this);
                if (caught instanceof Mob mob) {
                    HeldMobs.hold(mob);
                }
                this.storm.sound(level, grip, SoundEvents.AMETHYST_BLOCK_PLACE, 2.0F, 0.6F);
                this.storm.sound(level, grip, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.6F, 0.7F);
            }
        }
        if (this.held == null) {
            return;
        }
        if (!this.held.isAlive() || this.held.level() != level) {
            this.letGo();
            return;
        }
        if (this.t < HandPose.GRAB_THROWS) {
            this.hold(grip);
            return;
        }
        LivingEntity thrown = this.held;
        this.letGo();
        Vec3 away = this.aim.subtract(this.base);
        away = new Vec3(away.x, 0.0, away.z);
        away = away.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : away.normalize();
        this.hit(level, thrown, this.storm.ability.getDamage() * 0.6, away, 1.0, 0.9);
        this.storm.sound(level, grip, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
        this.storm.sound(level, grip, SoundEvents.ENDER_DRAGON_FLAP, 1.4F, 1.2F);
    }

    private void hold(Vec3 grip) {
        LivingEntity living = this.held;
        double y = grip.y - living.getBbHeight() * 0.5;
        living.setDeltaMovement(Vec3.ZERO);
        living.resetFallDistance();
        if (living instanceof ServerPlayer player) {
            player.teleportTo(grip.x, y, grip.z);
            player.connection.aboveGroundTickCount = 0;
        } else {
            living.setPos(grip.x, y, grip.z);
        }
    }

    private void slam(ServerLevel level) {
        if (this.t == HandPose.SLAM_HITS) {
            HandPose.Place place = this.place();
            Vec3 along = new Vec3(place.up().x, 0.0, place.up().z);
            along = along.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : along.normalize();
            Vec3 across = along.cross(Vectors.UP);
            for (LivingEntity living : this.near(level, 10.0)) {
                Vec3 to = living.position().subtract(place.wrist());
                double wide = 2.0 * SCALE + living.getBbWidth() * 0.5;
                if (to.dot(along) < -0.4 || to.dot(along) > 6.6 * SCALE || Math.abs(to.dot(across)) > wide
                        || to.y > 1.6 * SCALE || to.y < -2.5) {
                    continue;
                }
                this.hit(level, living, this.storm.ability.getDamage() * 1.3, along, 0.0, 0.0);
                // The blow's own knockback would pop it up into the palm coming down: it stays pressed flat.
                living.setDeltaMovement(0.0, Math.min(0.0, living.getDeltaMovement().y), 0.0);
                living.hurtMarked = true;
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FLAT_TICKS, 3),
                        this.storm.owner);
                this.pressed.add(living);
                FlattenPayload.send(living);
            }
            Vec3 palm = place.at(HandPose.PALM);
            ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.6F), this.base.add(along.scale(3.0))
                    .add(0.0, 0.2, 0.0), 40, 0.6);
            this.dustAt(level, this.base.add(along.scale(3.0)), 30);
            this.storm.sound(level, palm, SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 1.2F);
            this.storm.sound(level, palm, SoundEvents.SLIME_SQUISH, 2.0F, 0.5F);
            this.storm.sound(level, palm, SoundEvents.ANVIL_LAND, 0.9F, 0.8F);
        }
        if (this.t > HandPose.SLAM_HITS && this.t <= HandPose.SLAM_PRESSES) {
            for (LivingEntity living : this.pressed) {
                if (living.isAlive()) {
                    living.setDeltaMovement(0.0, Math.min(0.0, living.getDeltaMovement().y), 0.0);
                    living.hurtMarked = true;
                }
            }
        }
    }

    private void pound(ServerLevel level) {
        Vec3 strike = this.place().at(HandPose.FIST);
        double reach = 2.8 * SCALE;
        for (LivingEntity living : this.near(level, 12.0)) {
            Vec3 to = living.position().subtract(strike);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            if (flat > reach + living.getBbWidth() * 0.5 || Math.abs(to.y) > 2.5) {
                continue;
            }
            Vec3 out = new Vec3(to.x, 0.0, to.z);
            out = out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize();
            this.hit(level, living, this.storm.ability.getDamage() * 0.55, out, 0.35, 0.5);
        }
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.5F), strike.add(0.0, 0.2, 0.0), 32, 0.5);
        this.dustAt(level, strike, 22);
        this.storm.sound(level, strike, SoundEvents.ANVIL_LAND, 1.2F, 0.6F);
        this.storm.sound(level, strike, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.6F);
    }

    private static double distance(List<Vec3> path, Vec3 point) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i + 1 < path.size(); i++) {
            Vec3 a = path.get(i);
            Vec3 ab = path.get(i + 1).subtract(a);
            double length = ab.lengthSqr();
            double u = length < 1.0E-9 ? 0.0 : Mth.clamp(point.subtract(a).dot(ab) / length, 0.0, 1.0);
            best = Math.min(best, a.add(ab.scale(u)).distanceTo(point));
        }
        return best;
    }
}
