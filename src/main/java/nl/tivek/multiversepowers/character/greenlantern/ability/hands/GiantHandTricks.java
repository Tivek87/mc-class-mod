package nl.tivek.multiversepowers.character.greenlantern.ability.hands;

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
import nl.tivek.multiversepowers.character.greenlantern.construct.FlattenPayload;
import nl.tivek.multiversepowers.character.greenlantern.duo.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.GRABBED;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.GRAB_TALL;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.GRAB_WIDE;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.SCALE;

// What the flick, the pinch and the snap do, and the portals the first two come through.
abstract class GiantHandTricks extends GiantHandPair {
    private static final int PORTAL_OPENS = 2;
    private static final int PORTAL_SHUTS = 5;
    private static final int TENSION = 10;
    private static final Vec3 NAIL_FROM = new Vec3(-0.38, 4.0, 2.4);
    private static final Vec3 NAIL_TO = new Vec3(-0.38, 6.4, 0.6);
    private static final double FLICK_REACH = 2.2;
    private static final double FLICK_DAMAGE = 0.8;
    private static final double FLICK_OUT = 2.0;
    private static final double FLICK_UP = 0.8;
    private static final double PINCH_REACH = 2.4;
    private static final double SQUEEZE_DAMAGE = 0.35;
    private static final double DROP_DOWN = 0.9;
    private static final double SNAP_REACH = 7.0;
    private static final double SNAP_DAMAGE = 0.45;
    private static final double SNAP_SHOVE = 0.2;
    private static final double SNAP_ABOVE = 3.0;
    private static final double SNAP_BELOW = 3.0;
    private static final int DAZED_TICKS = 60;
    private static final double POKE_REACH = 1.8;
    private static final double POKE_DAMAGE = 0.3;
    private static final double POKE_LAST = 0.8;
    private static final double POKE_SHOVE = 0.25;
    private static final double POKE_OUT = 1.8;
    private static final double POKE_UP = 0.7;
    private static final double HAMMER_CORE = 2.4;
    private static final double HAMMER_RING = 5.5;
    private static final double HAMMER_DAMAGE = 1.4;
    private static final double HAMMER_WAVE = 0.4;
    private static final double RAKE_REACH = 2.2;
    private static final double RAKE_DAMAGE = 1.1;
    private static final double DRAG_REACH = 2.6;
    private static final double DRAG_DAMAGE = 0.5;
    private static final double SCRAPE_DAMAGE = 0.12;
    private static final int SCRAPE_EVERY = 4;
    private static final double DRAG_FLING = 1.2;

    // A drag stops where its portal or the creature would run into a wall, and lets go there.
    private boolean stopped;

    GiantHandTricks(GiantHands storm, int variant, Vec3 base, LivingEntity target) {
        super(storm, variant, base, target);
    }

    GiantHandTricks(GiantHands storm, int variant, Vec3 base, LivingEntity target, Vec3 facing) {
        super(storm, variant, base, target, facing);
    }

    void trick(ServerLevel level) {
        switch (this.move) {
            case HandPose.FLICK -> this.flick(level);
            case HandPose.PINCH -> this.pinch(level);
            case HandPose.SNAP -> this.snap(level);
            case HandPose.POKE -> this.poke(level);
            case HandPose.HAMMER -> this.hammer(level);
            case HandPose.RAKE -> this.rake(level);
            case HandPose.DRAG -> this.drag(level);
            default -> this.feat(level);
        }
        if (HandPose.portal(this.variant)) {
            if (this.t == PORTAL_OPENS) {
                this.opens(level, this.portal(), HandPose.overhead(this.variant) ? 1.2F : 1.5F);
            }
            if (this.t == HandPose.life(this.variant) - PORTAL_SHUTS) {
                this.shuts(level, this.portal(), 1.6F);
            }
        }
    }

    abstract void feat(ServerLevel level);

    private HandDuo.Portal portal() {
        return HandPose.portalOf(this.variant, this.base, this.aim.subtract(this.base), this.t, SCALE);
    }

    private void flick(ServerLevel level) {
        HandPose.Place place = this.place();
        if (this.t == HandPose.FLICK_HITS - TENSION) {
            Vec3 nail = place.at(HandPose.FLICK_POINT);
            this.storm.sound(level, nail, SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), 1.8F, 0.5F);
            this.storm.sound(level, nail, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F, 0.6F);
        }
        if (this.t != HandPose.FLICK_HITS) {
            return;
        }
        Vec3 from = place.at(NAIL_FROM);
        Vec3 to = place.at(NAIL_TO);
        Vec3 way = new Vec3(place.arm().x, 0.0, place.arm().z);
        way = way.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : way.normalize();
        for (LivingEntity living : this.near(level, 14.0)) {
            if (distance(from, to, living.getBoundingBox().getCenter()) > FLICK_REACH * SCALE
                    + living.getBbWidth() * 0.5) {
                continue;
            }
            this.hit(level, living, this.storm.ability.getDamage() * FLICK_DAMAGE, way, FLICK_OUT, FLICK_UP);
        }
        Vec3 nail = place.at(HandPose.FLICK_POINT);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.5F), nail, 24, 0.5);
        ParticleFx.cloud(level, ParticleTypes.END_ROD, nail, 8, 0.4, 0.08);
        this.storm.sound(level, nail, SoundEvents.WOODEN_BUTTON_CLICK_ON, 2.6F, 0.5F);
        this.storm.sound(level, nail, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.4F, 0.5F);
        this.storm.sound(level, nail, SoundEvents.AMETHYST_BLOCK_HIT, 1.8F, 1.2F);
        this.storm.sound(level, nail, SoundEvents.ENDER_DRAGON_FLAP, 1.4F, 1.4F);
    }

    private void pinch(ServerLevel level) {
        Vec3 grip = this.place().at(HandPose.PINCH_GRIP);
        if (this.t == HandPose.PINCH_CATCHES) {
            LivingEntity caught = null;
            double best = PINCH_REACH * SCALE;
            for (LivingEntity living : this.near(level, 12.0)) {
                double distance = living.getBoundingBox().getCenter().distanceTo(grip);
                if (distance < best && living.getBbWidth() <= GRAB_WIDE && living.getBbHeight() <= GRAB_TALL
                        && !HeldMobs.isHeldByAnyone(living) && !LightBubble.trapped(living)) {
                    best = distance;
                    caught = living;
                }
            }
            if (caught == null) {
                this.storm.sound(level, grip, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 0.6F);
                return;
            }
            this.held = caught;
            GRABBED.put(caught.getId(), this);
            if (caught instanceof Mob mob) {
                HeldMobs.hold(mob);
            }
            this.hit(level, caught, this.storm.ability.getDamage() * SQUEEZE_DAMAGE, Vec3.ZERO, 0.0, 0.0);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.1F), grip, 14, 0.25);
            this.storm.sound(level, grip, SoundEvents.AMETHYST_BLOCK_PLACE, 2.0F, 1.2F);
            this.storm.sound(level, grip, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.4F, 1.3F);
        }
        if (this.held == null) {
            return;
        }
        if (!this.held.isAlive() || this.held.level() != level) {
            this.letGo();
            return;
        }
        if (this.t < HandPose.PINCH_DROPS) {
            this.hold(grip);
            if ((HandPose.PINCH_DROPS - this.t) % 12 == 0) {
                this.storm.sound(level, grip, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9F, 1.5F);
            }
            return;
        }
        LivingEntity dropped = this.held;
        this.letGo();
        dropped.setDeltaMovement(0.0, -DROP_DOWN, 0.0);
        dropped.hasImpulse = true;
        dropped.hurtMarked = true;
        this.storm.sound(level, grip, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F, 1.5F);
        this.storm.sound(level, grip, SoundEvents.ENDER_DRAGON_FLAP, 1.0F, 1.6F);
    }

    private void snap(ServerLevel level) {
        HandPose.Place place = this.place();
        Vec3 fingers = place.at(HandPose.SNAP_POINT);
        if (this.t == HandPose.SNAP_HITS - TENSION) {
            this.storm.sound(level, fingers, SoundEvents.BEACON_POWER_SELECT, 1.2F, 1.8F);
            this.storm.sound(level, fingers, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.6F);
        }
        if (this.t != HandPose.SNAP_HITS) {
            return;
        }
        double reach = SNAP_REACH * SCALE;
        // The ring runs out over the ground under the fingers: everything within reach of that spot, low or high.
        for (LivingEntity living : this.near(level, SNAP_REACH + 4.0)) {
            Vec3 middle = living.getBoundingBox().getCenter();
            Vec3 away = new Vec3(middle.x - fingers.x, 0.0, middle.z - fingers.z);
            double distance = away.length();
            if (distance > reach + living.getBbWidth() * 0.5 || living.getY() > fingers.y + SNAP_ABOVE
                    || living.getY() < this.base.y - SNAP_BELOW) {
                continue;
            }
            away = distance < 1.0E-2 ? Vec3.ZERO : away.scale(1.0 / distance);
            double close = 1.0 - 0.5 * Mth.clamp(distance / reach, 0.0, 1.0);
            this.hit(level, living, this.storm.ability.getDamage() * SNAP_DAMAGE * close, away, 0.0, 0.0);
            this.shove(living, away, SNAP_SHOVE);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DAZED_TICKS, 3), this.storm.owner);
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DAZED_TICKS * 2, 1), this.storm.owner);
            if (living instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, middle.add(0.0, living.getBbHeight() * 0.6, 0.0),
                    10, 0.35, 0.05);
        }
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), fingers, 40, 0.8);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.5F), new Vec3(fingers.x, this.base.y + 0.2,
                fingers.z), 40, 0.7);
        this.storm.sound(level, fingers, SoundEvents.WOODEN_BUTTON_CLICK_ON, 3.0F, 0.5F);
        this.storm.sound(level, fingers, SoundEvents.AMETHYST_BLOCK_BREAK, 2.0F, 0.7F);
        this.storm.sound(level, fingers, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.9F);
        this.storm.sound(level, fingers, SoundEvents.BELL_RESONATE, 1.6F, 1.4F);
    }

    // Only a small shove, never the hop the blow's own knockback gives.
    private void shove(LivingEntity living, Vec3 way, double amount) {
        double shove = amount * this.storm.ability.value("knockback")
                * this.storm.ability.value(HandPose.HANDS[HandPose.settingsOf(this.move)] + "Knockback");
        living.setDeltaMovement(way.x * shove, Math.min(0.0, living.getDeltaMovement().y), way.z * shove);
        living.hurtMarked = true;
    }

    private void poke(ServerLevel level) {
        for (int k = 0; k < HandPose.POKE_HITS.length; k++) {
            if (this.t != HandPose.POKE_HITS[k]) {
                continue;
            }
            HandPose.Place place = this.place();
            Vec3 tip = place.at(HandPose.POKE_POINT);
            Vec3 way = flat(place.arm());
            boolean last = k == HandPose.POKE_HITS.length - 1;
            for (LivingEntity living : this.near(level, 14.0)) {
                if (living.getBoundingBox().getCenter().distanceTo(tip) > POKE_REACH * SCALE
                        + living.getBbWidth() * 0.5) {
                    continue;
                }
                if (last) {
                    this.hit(level, living, this.storm.ability.getDamage() * POKE_LAST, way, POKE_OUT, POKE_UP);
                } else {
                    this.hit(level, living, this.storm.ability.getDamage() * POKE_DAMAGE, way, 0.0, 0.0);
                    this.shove(living, way, POKE_SHOVE);
                }
            }
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.1F), tip, last ? 20 : 8, last ? 0.4 : 0.2);
            if (last) {
                this.storm.sound(level, tip, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.2F, 0.6F);
                this.storm.sound(level, tip, SoundEvents.AMETHYST_BLOCK_HIT, 1.8F, 0.9F);
                this.storm.sound(level, tip, SoundEvents.ENDER_DRAGON_FLAP, 1.2F, 1.5F);
            } else {
                this.storm.sound(level, tip, SoundEvents.AMETHYST_BLOCK_HIT, 1.4F, 1.4F + 0.15F * k);
                this.storm.sound(level, tip, SoundEvents.WOODEN_BUTTON_CLICK_ON, 1.6F, 1.2F + 0.1F * k);
            }
        }
    }

    private void hammer(ServerLevel level) {
        HandPose.Place place = this.place();
        Vec3 fist = place.at(HandPose.HAMMER_POINT);
        if (this.t == HandPose.HAMMER_HITS - TENSION) {
            this.storm.sound(level, fist, SoundEvents.BEACON_POWER_SELECT, 1.2F, 0.6F);
            this.storm.sound(level, fist, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F, 0.5F);
        }
        if (this.t == HandPose.HAMMER_HITS) {
            for (LivingEntity living : this.near(level, HAMMER_RING + 4.0)) {
                Vec3 away = new Vec3(living.getX() - fist.x, 0.0, living.getZ() - fist.z);
                double flat = away.length() - living.getBbWidth() * 0.5;
                if (flat > HAMMER_RING * SCALE || Math.abs(living.getY() - fist.y) > 2.5) {
                    continue;
                }
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                if (flat > HAMMER_CORE * SCALE) {
                    this.hit(level, living, this.storm.ability.getDamage() * HAMMER_WAVE, away, 0.9, 0.5);
                    continue;
                }
                this.hit(level, living, this.storm.ability.getDamage() * HAMMER_DAMAGE, away, 0.0, 0.0);
                // Pressed into the ground, not popped up by the blow's own knockback.
                living.setDeltaMovement(0.0, Math.min(0.0, living.getDeltaMovement().y), 0.0);
                living.hurtMarked = true;
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DAZED_TICKS, 3),
                        this.storm.owner);
                this.pressed.add(living);
                FlattenPayload.send(living);
            }
            ParticleFx.send(level, ParticleTypes.EXPLOSION, fist.x, fist.y + 0.5, fist.z, 1, 0.0, 0.0, 0.0, 0.0);
            ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.7F), fist.add(0.0, 0.2, 0.0), 48, 0.8);
            this.dustAt(level, fist, 40, 1.5);
            this.storm.sound(level, fist, SoundEvents.GENERIC_EXPLODE.value(), 2.2F, 0.7F);
            this.storm.sound(level, fist, SoundEvents.ANVIL_LAND, 1.6F, 0.5F);
            this.storm.sound(level, fist, SoundEvents.ROOTED_DIRT_BREAK, 2.2F, 0.5F);
        }
        if (this.t > HandPose.HAMMER_HITS && this.t <= HandPose.HAMMER_LIFTS) {
            for (LivingEntity living : this.pressed) {
                if (living.isAlive()) {
                    living.setDeltaMovement(0.0, Math.min(0.0, living.getDeltaMovement().y), 0.0);
                    living.hurtMarked = true;
                }
            }
        }
    }

    private void rake(ServerLevel level) {
        if (this.t == HandPose.RAKE_HITS - TENSION) {
            Vec3 claws = this.place().at(HandPose.RAKE_POINT);
            this.storm.sound(level, claws, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.8F);
            this.storm.sound(level, claws, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.4F, 0.6F);
        }
        if (this.t != HandPose.RAKE_HITS) {
            return;
        }
        Vec3 reach = this.aim.subtract(this.base);
        Vec3 way = flat(reach);
        Vec3 last = null;
        Vec3[] path = new Vec3[13];
        for (int k = 0; k < path.length; k++) {
            path[k] = this.pose(this.t - 3.0 + 0.5 * k).place(this.base, reach, SCALE).at(HandPose.RAKE_POINT);
        }
        for (LivingEntity living : this.near(level, 14.0)) {
            Vec3 middle = living.getBoundingBox().getCenter();
            double best = Double.MAX_VALUE;
            for (int k = 0; k + 1 < path.length; k++) {
                best = Math.min(best, distance(path[k], path[k + 1], middle));
            }
            if (best > RAKE_REACH * SCALE + living.getBbWidth() * 0.5) {
                continue;
            }
            this.hit(level, living, this.storm.ability.getDamage() * RAKE_DAMAGE, way, 0.9, 0.3);
        }
        for (Vec3 at : path) {
            if (last == null || at.distanceTo(last) > 2.0) {
                this.dustAt(level, new Vec3(at.x, this.base.y, at.z), 10, 0.8);
                last = at;
            }
        }
        Vec3 claws = path[6];
        this.storm.sound(level, claws, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.6F);
        this.storm.sound(level, claws, SoundEvents.GRINDSTONE_USE, 1.6F, 0.8F);
        this.storm.sound(level, claws, SoundEvents.AMETHYST_BLOCK_BREAK, 1.6F, 0.7F);
        this.storm.sound(level, claws, SoundEvents.ROOTED_DIRT_BREAK, 1.8F, 0.8F);
    }

    // The portal races off away from the caster, the creature in its grip scraped along the ground behind it.
    private void drag(ServerLevel level) {
        Vec3 grip = this.place().at(HandPose.GRIP);
        if (this.t == HandPose.DRAG_CATCHES) {
            this.snatch(level, grip);
        }
        Vec3 way = flat(this.base.subtract(this.aim));
        if (this.t >= HandPose.DRAG_STARTS && this.t <= HandPose.DRAG_RELEASES && !this.stopped) {
            Vec3 step = way.scale(HandPose.dragged(this.t) - HandPose.dragged(this.t - 1));
            Vec3 feet = this.held == null ? null : this.held.position().add(step).add(0.0, 0.6, 0.0);
            if (!GiantHands.open(level, this.base.add(step)) || feet != null && !GiantHands.open(level, feet)) {
                this.stopped = true;
            } else {
                this.base = this.base.add(step);
                this.aim = this.aim.add(step);
                this.room = null;
            }
            if (this.t == HandPose.DRAG_STARTS) {
                this.storm.sound(level, this.base, SoundEvents.ENDER_DRAGON_FLAP, 1.8F, 1.3F);
                this.storm.sound(level, this.base, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
            }
        }
        if (this.held == null) {
            return;
        }
        if (!this.held.isAlive() || this.held.level() != level) {
            this.letGo();
            return;
        }
        if (this.t < HandPose.DRAG_RELEASES && !this.stopped) {
            this.holdOnGround(level, this.place().at(HandPose.GRIP));
            if (this.t >= HandPose.DRAG_STARTS && this.t % SCRAPE_EVERY == 0) {
                this.scrape(level, this.held);
            }
            return;
        }
        LivingEntity flung = this.held;
        this.letGo();
        this.hit(level, flung, this.storm.ability.getDamage() * DRAG_DAMAGE, way, 0.0, 0.0);
        double fling = DRAG_FLING * this.storm.ability.value("knockback") * this.storm.ability.value("dragKnockback");
        flung.setDeltaMovement(way.x * fling, 0.35, way.z * fling);
        flung.hurtMarked = true;
        this.storm.sound(level, grip, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.0F, 0.6F);
        this.storm.sound(level, grip, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F, 1.4F);
    }

    void snatch(ServerLevel level, Vec3 grip) {
        LivingEntity caught = null;
        double best = DRAG_REACH * SCALE;
        for (LivingEntity living : this.near(level, 12.0)) {
            double distance = living.getBoundingBox().getCenter().distanceTo(grip);
            if (distance < best && living.getBbWidth() <= GRAB_WIDE && living.getBbHeight() <= GRAB_TALL
                    && !HeldMobs.isHeldByAnyone(living) && !LightBubble.trapped(living)) {
                best = distance;
                caught = living;
            }
        }
        if (caught == null) {
            this.storm.sound(level, grip, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 0.6F);
            return;
        }
        this.held = caught;
        GRABBED.put(caught.getId(), this);
        if (caught instanceof Mob mob) {
            HeldMobs.hold(mob);
        }
        this.storm.sound(level, grip, SoundEvents.AMETHYST_BLOCK_PLACE, 2.0F, 0.6F);
        this.storm.sound(level, grip, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.6F, 0.7F);
    }

    // Held at the fingers, but never below the ground it is dragged over.
    void holdOnGround(ServerLevel level, Vec3 grip) {
        LivingEntity living = this.held;
        BlockHitResult under = LoadedWorld.clip(level, new ClipContext(grip.add(0.0, 1.0, 0.0),
                grip.subtract(0.0, 4.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                CollisionContext.empty()));
        double y = grip.y - living.getBbHeight() * 0.5;
        if (under.getType() != HitResult.Type.MISS) {
            y = Math.max(y, under.getLocation().y);
        }
        this.hold(new Vec3(grip.x, y + living.getBbHeight() * 0.5, grip.z));
    }

    private void scrape(ServerLevel level, LivingEntity living) {
        this.hit(level, living, this.storm.ability.getDamage() * SCRAPE_DAMAGE, Vec3.ZERO, 0.0, 0.0);
        Vec3 feet = living.position();
        this.dustAt(level, feet, 12, 0.5);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, feet.add(0.0, 0.2, 0.0), 6, 0.3, 0.1);
        this.storm.sound(level, feet, SoundEvents.GRINDSTONE_USE, 1.2F, 0.6F + 0.02F * this.t);
        this.storm.sound(level, feet, SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 0.8F);
    }

    private static Vec3 flat(Vec3 way) {
        Vec3 flat = new Vec3(way.x, 0.0, way.z);
        return flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    void hold(Vec3 grip) {
        hold(this.held, grip);
    }

    static void hold(LivingEntity living, Vec3 grip) {
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

    private static double distance(Vec3 a, Vec3 b, Vec3 point) {
        Vec3 ab = b.subtract(a);
        double length = ab.lengthSqr();
        double u = length < 1.0E-9 ? 0.0 : Mth.clamp(point.subtract(a).dot(ab) / length, 0.0, 1.0);
        return a.add(ab.scale(u)).distanceTo(point);
    }
}
