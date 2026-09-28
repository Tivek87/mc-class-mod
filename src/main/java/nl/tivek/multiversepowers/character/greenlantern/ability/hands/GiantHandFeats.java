package nl.tivek.multiversepowers.character.greenlantern.ability.hands;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandGroup;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandVictimPayload;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.GRABBED;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.GRAB_TALL;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.GRAB_WIDE;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.SCALE;

// What the ragdoll and its catch, the ring hold, the clap, the finger gun, the scissors, the swallow, the ring beam and
// the scoop do.
abstract class GiantHandFeats extends GiantHandTricks {
    private static final double RAGDOLL_DAMAGE = 0.35;
    private static final double LAUNCH_OUT = 1.8;
    private static final double LAUNCH_UP = 1.1;
    private static final int CATCH_WATCH = 40;
    private static final double CATCH_REACH = 3.0;
    private static final int SQUEEZE_EVERY = 10;
    private static final double SQUEEZE_DAMAGE = 0.15;
    private static final double HOLD_REACH = 2.6;
    private static final double BLAST_DAMAGE = 2.4;
    private static final double BLAST_REACH = 5.0;
    private static final double CLAP_DAMAGE = 1.6;
    private static final double GUN_DAMAGE = 0.5;
    private static final double GUN_RANGE = 20.0;
    private static final double SNIP_REACH = 2.0;
    private static final double BEAM_DAMAGE = 0.35;
    private static final double BEAM_RANGE = 24.0;
    private static final double BEAM_WIDE = 1.3;
    private static final double SCOOP_REACH = 2.6;

    @Nullable
    private LivingEntity flung;
    private int flungAt = -1;

    GiantHandFeats(GiantHands storm, int variant, Vec3 base, LivingEntity target) {
        super(storm, variant, base, target);
    }

    GiantHandFeats(GiantHands storm, int variant, Vec3 base, LivingEntity target, Vec3 facing) {
        super(storm, variant, base, target, facing);
    }

    @Override
    void feat(ServerLevel level) {
        switch (this.move) {
            case HandPose.RAGDOLL -> this.ragdoll(level);
            case HandPose.CATCH -> this.catchHand(level);
            case HandPose.RINGHOLD -> this.ringHold(level);
            case HandPose.CLAP -> this.clap(level);
            case HandPose.FINGERGUN -> this.fingerGun(level);
            case HandPose.SCISSORS -> this.scissors(level);
            case HandPose.SWALLOW -> this.swallow(level);
            case HandPose.RINGBEAM -> this.ringBeam(level);
            case HandPose.SCOOP -> this.scoop(level);
            default -> {
            }
        }
    }

    private Vec3 root() {
        return HandPose.rootNormal(this.variant, this.aim.subtract(this.base));
    }

    private double kick() {
        return this.storm.ability.value("knockback")
                * this.storm.ability.value(HandPose.HANDS[HandPose.settingsOf(this.move)] + "Knockback");
    }

    private double damage(double share) {
        return this.storm.ability.getDamage() * share;
    }

    private void ragdoll(ServerLevel level) {
        int extra = HandPose.extra(this.variant);
        Vec3 grip = this.place().at(HandPose.GRIP);
        if (this.t == HandPose.GRAB_CATCHES) {
            this.snatch(level, grip);
        }
        if (this.flung != null && this.flungAt >= 0) {
            this.watchFall(level);
        }
        if (this.held == null) {
            return;
        }
        if (!this.held.isAlive() || this.held.level() != level) {
            this.letGo();
            return;
        }
        int launch = HandPose.ragdollLaunch(extra);
        if (this.t < launch) {
            this.holdClear(level, grip);
            for (int k = 0; k < HandPose.ragdollSlams(extra); k++) {
                if (this.t == HandPose.ragdollSlam(k)) {
                    this.slammed(level, this.held);
                }
            }
            int wind = HandPose.ragdollWinds(extra);
            if (this.t >= wind && (this.t - wind) % Math.max(2, 6 - (this.t - wind) / 3) == 0) {
                this.storm.sound(level, grip, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F,
                        0.6F + 0.9F * (this.t - wind) / (float) (launch - wind));
            }
            return;
        }
        LivingEntity thrown = this.held;
        this.letGo();
        Vec3 way = this.root().y > 0.5 ? flat(this.aim.subtract(this.base)) : this.root();
        way = this.storm.awayFromHim(thrown, way);
        this.hit(level, thrown, this.damage(RAGDOLL_DAMAGE * 1.5), way, 0.0, 0.0);
        double kick = this.kick();
        thrown.setDeltaMovement(way.x * LAUNCH_OUT * kick, LAUNCH_UP * Math.min(1.0, kick), way.z * LAUNCH_OUT * kick);
        thrown.hurtMarked = true;
        thrown.hasImpulse = true;
        this.flung = thrown;
        this.flungAt = this.t;
        this.storm.sound(level, grip, SoundEvents.ENDER_DRAGON_FLAP, 2.0F, 0.8F);
        this.storm.sound(level, grip, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.4F, 0.4F);
        this.storm.sound(level, grip, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.6F, 0.7F);
    }

    // Held at the fingers, but never into whatever it is slammed on, ground or wall: it stops at the first block
    // between the hand's wrist and its fingers.
    private void holdClear(ServerLevel level, Vec3 grip) {
        LivingEntity living = this.held;
        Vec3 wrist = this.place().wrist();
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(wrist, grip, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 at = grip;
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 face = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            double half = Math.abs(face.y) > 0.5 ? living.getBbHeight() * 0.5 : living.getBbWidth() * 0.5 + 0.05;
            at = hit.getLocation().add(face.scale(half));
        }
        Vec3 out = this.root();
        double half = out.y > 0.5 ? living.getBbHeight() * 0.5 : living.getBbWidth() * 0.5;
        double above = at.subtract(this.base).dot(out);
        if (above < half) {
            at = at.add(out.scale(half - above));
        }
        this.holdOnGround(level, at);
    }

    private void slammed(ServerLevel level, LivingEntity living) {
        this.hit(level, living, this.damage(RAGDOLL_DAMAGE), Vec3.ZERO, 0.0, 0.0);
        Vec3 at = living.getBoundingBox().getCenter();
        for (LivingEntity near : this.near(level, 8.0)) {
            if (near != living && near.getBoundingBox().getCenter().distanceTo(at) < 2.5) {
                this.hit(level, near, this.damage(RAGDOLL_DAMAGE * 0.6), flat(near.position().subtract(at)), 0.6,
                        0.3);
            }
        }
        this.dustAt(level, living.position(), 24, 1.0);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), at, 18, 0.35);
        this.storm.sound(level, at, SoundEvents.ANVIL_LAND, 1.6F, 0.6F);
        this.storm.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.4F);
        this.storm.sound(level, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.8F, 0.5F);
    }

    // Once the thrown creature falls again, well clear of the ground, half the time a hand comes up to catch it.
    private void watchFall(ServerLevel level) {
        LivingEntity living = this.flung;
        if (!living.isAlive() || this.t - this.flungAt > CATCH_WATCH) {
            this.flung = null;
            return;
        }
        if (living.getDeltaMovement().y > -0.1) {
            return;
        }
        Vec3 feet = living.position();
        BlockHitResult under = LoadedWorld.clip(level, new ClipContext(feet, feet.subtract(0.0, 4.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        if (under.getType() == HitResult.Type.MISS) {
            this.storm.catchFalling(level, living);
            this.flung = null;
        }
    }

    // Up under the falling creature: a fist round it, squeezing, till it lets go after 3 seconds at most.
    private void catchHand(ServerLevel level) {
        Vec3 grip = this.place().at(HandPose.GRIP);
        if (this.t == HandPose.CATCH_CATCHES) {
            LivingEntity caught = null;
            double best = CATCH_REACH * SCALE;
            for (LivingEntity living : this.near(level, 10.0)) {
                double distance = living.getBoundingBox().getCenter().distanceTo(grip);
                if (distance < best && !HeldMobs.isHeldByAnyone(living) && !LightBubble.trapped(living)) {
                    best = distance;
                    caught = living;
                }
            }
            if (caught != null) {
                this.take(caught);
                this.storm.sound(level, grip, SoundEvents.AMETHYST_BLOCK_PLACE, 2.0F, 0.5F);
                this.storm.sound(level, grip, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.8F, 0.6F);
            }
        }
        if (this.held == null) {
            return;
        }
        if (!this.held.isAlive() || this.held.level() != level || this.t >= HandPose.CATCH_LETS_GO) {
            this.letGo();
            this.storm.sound(level, grip, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 1.3F);
            return;
        }
        this.hold(grip);
        if ((this.t - HandPose.CATCH_CATCHES) % SQUEEZE_EVERY == SQUEEZE_EVERY - 1) {
            this.hit(level, this.held, this.damage(SQUEEZE_DAMAGE), Vec3.ZERO, 0.0, 0.0);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), grip, 10, 0.2);
            this.storm.sound(level, grip, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.6F);
            this.storm.sound(level, grip, SoundEvents.SLIME_SQUISH, 1.0F, 0.6F);
        }
    }

    void take(LivingEntity living) {
        this.held = living;
        GRABBED.put(living.getId(), this);
        if (living instanceof Mob mob) {
            HeldMobs.hold(mob);
        }
    }

    boolean holdable(LivingEntity living) {
        return living.getBbWidth() <= GRAB_WIDE && living.getBbHeight() <= GRAB_TALL
                && !HeldMobs.isHeldByAnyone(living) && !LightBubble.trapped(living);
    }

    // Four hands hold the creature spread out, a fifth sets its ring on it; the ring charges and blasts it away.
    private void ringHold(ServerLevel level) {
        Vec3 facing = this.aim.subtract(this.base);
        Vec3 touch = HandGroup.ringPoint(this.base, facing, this.t);
        if (this.t == HandGroup.HOLD_GRABS) {
            Vec3 middle = HandGroup.held(this.base, this.t);
            LivingEntity caught = null;
            double best = HOLD_REACH;
            for (LivingEntity living : this.near(level, 8.0)) {
                double distance = living.getBoundingBox().getCenter().distanceTo(middle);
                if (distance < best && this.holdable(living)) {
                    best = distance;
                    caught = living;
                }
            }
            if (caught != null) {
                this.take(caught);
                HandVictimPayload.send(caught, this.id(), HandVictimPayload.SPREAD);
            }
            this.storm.sound(level, middle, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 2.0F, 0.6F);
            this.storm.sound(level, middle, SoundEvents.AMETHYST_BLOCK_PLACE, 2.0F, 0.7F);
        }
        if (this.t == 2 || this.t == (int) Math.round(11.0 * 1.6)) {
            this.storm.sound(level, this.base, SoundEvents.BEACON_ACTIVATE, 1.6F, this.t == 2 ? 1.5F : 1.1F);
            this.storm.sound(level, this.base, SoundEvents.ENDER_DRAGON_FLAP, 1.2F, 1.4F);
        }
        if (this.t == HandGroup.RING_PRESSES) {
            this.storm.sound(level, touch, SoundEvents.BEACON_POWER_SELECT, 1.8F, 0.8F);
            this.storm.sound(level, touch, SoundEvents.ANVIL_PLACE, 0.8F, 1.6F);
        }
        if (this.t > HandGroup.RING_PRESSES && this.t < HandGroup.RING_BLASTS
                && (this.t - HandGroup.RING_PRESSES) % 5 == 0) {
            float rise = (this.t - HandGroup.RING_PRESSES) / (float) (HandGroup.RING_BLASTS - HandGroup.RING_PRESSES);
            this.storm.sound(level, touch, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F, 0.6F + 1.2F * rise);
            ParticleFx.cloud(level, ParticleTypes.END_ROD, touch, 4, 0.5, 0.02);
        }
        if (this.held != null && this.t < HandGroup.RING_BLASTS) {
            if (!this.held.isAlive() || this.held.level() != level) {
                this.letGo();
            } else {
                this.hold(HandGroup.held(this.base, this.t));
            }
        }
        if (this.t == HandGroup.RING_BLASTS) {
            LivingEntity blasted = this.held;
            this.letGo();
            this.blast(level, touch, blasted, flat(facing));
        }
    }

    private void blast(ServerLevel level, Vec3 at, @Nullable LivingEntity blasted, Vec3 ahead) {
        for (LivingEntity living : this.near(level, BLAST_REACH + 6.0)) {
            double distance = living.getBoundingBox().getCenter().distanceTo(at);
            if (living == blasted) {
                this.hit(level, living, this.damage(BLAST_DAMAGE), ahead, 2.6, 1.0);
            } else if (distance < BLAST_REACH) {
                this.hit(level, living, this.damage(BLAST_DAMAGE * 0.25), flat(living.position().subtract(at)), 1.4,
                        0.6);
            }
        }
        ParticleFx.send(level, ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.8F), at, 60, 1.0);
        ParticleFx.cloud(level, ParticleTypes.END_ROD, at, 30, 0.8, 0.3);
        this.storm.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 0.6F);
        this.storm.sound(level, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 2.0F, 1.2F);
        this.storm.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 2.0F, 0.5F);
        this.storm.sound(level, at, SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.7F);
    }

    // Two hands out of portals either side clap the creature between their palms.
    private void clap(ServerLevel level) {
        Vec3 facing = this.aim.subtract(this.base);
        Vec3 side = flat(facing).cross(Vectors.UP).normalize();
        Vec3 middle = HandGroup.clapPoint(this.base);
        if (this.t == 2) {
            this.storm.sound(level, this.base, SoundEvents.BEACON_ACTIVATE, 1.6F, 1.3F);
        }
        if (this.t == HandGroup.CLAP_HITS - 8) {
            this.storm.sound(level, middle, SoundEvents.ENDER_DRAGON_FLAP, 1.6F, 1.1F);
        }
        if (this.t != HandGroup.CLAP_HITS) {
            return;
        }
        for (LivingEntity living : this.near(level, 10.0)) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(middle);
            boolean between = Math.abs(to.dot(side)) < 1.5 + living.getBbWidth() * 0.5 && Math.abs(to.y) < 2.5
                    && Math.abs(to.dot(flat(facing))) < 2.0;
            if (between) {
                this.hit(level, living, this.damage(CLAP_DAMAGE), Vec3.ZERO, 0.0, 0.0);
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3), this.storm.owner);
                living.setDeltaMovement(0.0, 0.3, 0.0);
                living.hurtMarked = true;
            } else if (to.length() < 5.0) {
                this.hit(level, living, this.damage(CLAP_DAMAGE * 0.2), flat(to), 0.8, 0.3);
            }
        }
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), middle, 40, 0.8);
        ParticleFx.cloud(level, ParticleTypes.CLOUD, middle, 16, 0.6, 0.15);
        this.storm.sound(level, middle, SoundEvents.PLAYER_ATTACK_CRIT, 2.6F, 0.5F);
        this.storm.sound(level, middle, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 1.3F);
        this.storm.sound(level, middle, SoundEvents.ANVIL_LAND, 1.2F, 1.6F);
        this.storm.sound(level, middle, SoundEvents.AMETHYST_BLOCK_BREAK, 2.0F, 0.6F);
    }

    // Bang, bang, bang: a bolt of light out of the fingertip at each shot, hitting the first creature in its line.
    private void fingerGun(ServerLevel level) {
        HandPose.Place place = this.place();
        Vec3 tip = place.at(HandPose.GUN_TIP);
        for (int shot : HandPose.GUN_SHOTS) {
            if (this.t != shot) {
                continue;
            }
            Vec3 way = HandPose.aimed(place, tip, this.base, this.aim.subtract(this.base));
            BlockHitResult wall = LoadedWorld.clip(level, new ClipContext(tip, tip.add(way.scale(GUN_RANGE)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            double far = wall.getType() == HitResult.Type.MISS ? GUN_RANGE : wall.getLocation().distanceTo(tip);
            LivingEntity first = null;
            double nearest = far;
            for (LivingEntity living : this.near(level, GUN_RANGE + 6.0)) {
                Vec3 to = living.getBoundingBox().getCenter().subtract(tip);
                double along = to.dot(way);
                if (along < 0.0 || along > nearest) {
                    continue;
                }
                if (to.subtract(way.scale(along)).length() <= 1.0 + living.getBbWidth() * 0.5) {
                    nearest = along;
                    first = living;
                }
            }
            if (first != null) {
                this.hit(level, first, this.damage(GUN_DAMAGE), flat(way), 0.4, 0.15);
            }
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), tip, 10, 0.2);
            this.storm.sound(level, tip, SoundEvents.AMETHYST_BLOCK_HIT, 2.0F, 1.8F);
            this.storm.sound(level, tip, SoundEvents.FIREWORK_ROCKET_BLAST, 1.4F, 1.6F);
        }
        if (this.t == HandPose.GUN_SMOKES) {
            this.storm.sound(level, tip, SoundEvents.CANDLE_EXTINGUISH, 1.6F, 0.8F);
        }
    }

    private void scissors(ServerLevel level) {
        for (int k = 0; k < HandPose.SNIPS.length; k++) {
            if (this.t != HandPose.SNIPS[k]) {
                continue;
            }
            HandPose.Place place = this.place();
            Vec3 cut = place.at(HandPose.SNIP_POINT);
            Vec3 way = flat(place.up());
            boolean last = k == HandPose.SNIPS.length - 1;
            for (LivingEntity living : this.near(level, 12.0)) {
                if (living.getBoundingBox().getCenter().distanceTo(cut) > SNIP_REACH + living.getBbWidth() * 0.5) {
                    continue;
                }
                this.hit(level, living, this.damage(last ? 0.9 : 0.6), way, last ? 1.0 : 0.0, last ? 0.4 : 0.0);
                if (!last) {
                    living.setDeltaMovement(0.0, Math.min(0.0, living.getDeltaMovement().y), 0.0);
                    living.hurtMarked = true;
                }
            }
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), cut, 14, 0.3);
            this.storm.sound(level, cut, SoundEvents.SHEEP_SHEAR, 2.2F, 0.6F);
            this.storm.sound(level, cut, SoundEvents.AMETHYST_BLOCK_HIT, 1.6F, 1.3F);
        }
    }

    // Up out of a portal in the ground, it grabs the creature and pulls it down in; it drops out of a portal in the
    // sky above.
    private void swallow(ServerLevel level) {
        Vec3 grip = this.place().at(HandPose.GRIP);
        if (this.t == HandPose.SWALLOW_CATCHES) {
            this.snatch(level, grip);
        }
        if (this.held == null) {
            return;
        }
        if (!this.held.isAlive() || this.held.level() != level) {
            this.letGo();
            return;
        }
        if (this.t < HandPose.SWALLOW_GONE) {
            this.hold(grip);
            return;
        }
        LivingEntity dropped = this.held;
        this.letGo();
        Vec3 sky = this.base.add(0.0, HandPose.SKY_HEIGHT, 0.0);
        double y = sky.y - dropped.getBbHeight();
        if (dropped instanceof ServerPlayer player) {
            player.teleportTo(sky.x, y, sky.z);
        } else {
            dropped.teleportTo(sky.x, y, sky.z);
        }
        dropped.resetFallDistance();
        dropped.setDeltaMovement(0.0, -0.8, 0.0);
        dropped.hurtMarked = true;
        this.hit(level, dropped, this.damage(0.4), Vec3.ZERO, 0.0, 0.0);
        ParticleFx.cloud(level, ParticleTypes.END_ROD, sky, 20, 0.8, 0.1);
        this.storm.sound(level, this.base, SoundEvents.ENDERMAN_TELEPORT, 1.6F, 0.6F);
        this.storm.sound(level, sky, SoundEvents.ENDERMAN_TELEPORT, 1.6F, 0.8F);
        this.storm.sound(level, sky, SoundEvents.BEACON_POWER_SELECT, 1.4F, 1.4F);
    }

    // The ring on the fist gathers light, then fires a beam along the way the knuckles point.
    private void ringBeam(ServerLevel level) {
        HandPose.Place place = this.place();
        Vec3 ring = place.at(HandPose.RING_POINT);
        if (this.t == HandPose.BEAM_FIRES - 12) {
            this.storm.sound(level, ring, SoundEvents.BEACON_POWER_SELECT, 1.6F, 0.7F);
            this.storm.sound(level, ring, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F, 0.8F);
        }
        if (this.t == HandPose.BEAM_FIRES) {
            this.storm.sound(level, ring, SoundEvents.BEACON_ACTIVATE, 2.0F, 1.4F);
            this.storm.sound(level, ring, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2F, 1.8F);
        }
        if (this.t < HandPose.BEAM_FIRES || this.t > HandPose.BEAM_STOPS || (this.t - HandPose.BEAM_FIRES) % 4 != 0) {
            return;
        }
        Vec3 way = HandPose.aimed(place, ring, this.base, this.aim.subtract(this.base));
        BlockHitResult wall = LoadedWorld.clip(level, new ClipContext(ring, ring.add(way.scale(BEAM_RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? ring.add(way.scale(BEAM_RANGE)) : wall.getLocation();
        List<LivingEntity> near = this.near(level, BEAM_RANGE + 6.0);
        for (LivingEntity living : near) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(ring);
            double along = Mth.clamp(to.dot(way), 0.0, end.distanceTo(ring));
            if (to.subtract(way.scale(along)).length() <= BEAM_WIDE + living.getBbWidth() * 0.5) {
                this.hit(level, living, this.damage(BEAM_DAMAGE), flat(way), 0.0, 0.0);
                // Held in the beam: only a slow push along it, not the hop of the blow's own knockback.
                living.setDeltaMovement(way.x * 0.15, Math.min(0.0, living.getDeltaMovement().y), way.z * 0.15);
                living.hurtMarked = true;
            }
        }
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), end, 12, 0.3);
        this.storm.sound(level, ring, SoundEvents.BEACON_AMBIENT, 1.6F, 1.2F);
    }

    // The palm under the creature swings up and tosses it high over the hand, away from the caster.
    private void scoop(ServerLevel level) {
        if (this.t != HandPose.SCOOP_TOSSES) {
            return;
        }
        Vec3 palm = this.pose(this.t - 3.0).place(this.base, this.aim.subtract(this.base), SCALE)
                .at(HandPose.SCOOP_POINT);
        Vec3 way = this.root().y > 0.5 ? flat(this.base.subtract(this.aim)) : this.root();
        double kick = this.kick();
        for (LivingEntity living : this.near(level, 12.0)) {
            if (living.getBoundingBox().getCenter().distanceTo(palm) > SCOOP_REACH + living.getBbWidth() * 0.5) {
                continue;
            }
            Vec3 toss = this.storm.awayFromHim(living, way);
            this.hit(level, living, this.damage(0.5), toss, 0.0, 0.0);
            living.setDeltaMovement(toss.x * 0.7 * kick, 1.4 * Math.min(1.0, kick), toss.z * 0.7 * kick);
            living.hurtMarked = true;
            living.hasImpulse = true;
        }
        this.dustAt(level, new Vec3(palm.x, this.base.y, palm.z), 20, 1.0);
        this.storm.sound(level, palm, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.7F);
        this.storm.sound(level, palm, SoundEvents.ENDER_DRAGON_FLAP, 1.4F, 1.2F);
        this.storm.sound(level, palm, SoundEvents.ROOTED_DIRT_BREAK, 1.6F, 0.8F);
    }

    private static Vec3 flat(Vec3 way) {
        Vec3 flat = new Vec3(way.x, 0.0, way.z);
        return flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }
}
