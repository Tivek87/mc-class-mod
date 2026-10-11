package nl.tivek.multiversepowers.character.greenlantern.ability.hands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.Tags;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.duo.RevolverGun;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandAbyss;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandGroup;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandVictimPayload;
import nl.tivek.multiversepowers.engine.entity.DeathStyles;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.GRABBED;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.fair;
import static nl.tivek.multiversepowers.engine.math.Vectors.flat;

// What the abyss, the beam eye and the gunslinger do: a pit torn open whose tentacles drag everything in it down into
// the stars, a thick beam of light out of an eye, three creatures shot one after another.
abstract class GiantHandShots extends GiantHandTears {
    // 25 at the hands' usual damage of 12: a quarter of an iron golem.
    private static final double ABYSS_DAMAGE = 25.0 / 12.0;
    private static final double SPIT_OUT = 0.5;
    private static final double SPIT_UP = 0.55;
    // How far above the ground and below the floor a creature is still in the pit.
    private static final double PIT_ABOVE = 3.0;
    private static final double PIT_BELOW = 1.5;
    private static final int SNARE_EVERY = 4;
    private static final double BEAM_DAMAGE = 0.5;
    private static final double BEAM_RANGE = 28.0;
    private static final double BEAM_WIDE = 1.5;
    private static final int BEAM_EVERY = 3;
    private static final double SHOT_DAMAGE = 4.0;
    private static final double SHOT_RANGE = 30.0;
    private static final double SHOT_WIDE = 0.6;
    // How far the gunslinger looks round for the next creature to shoot, in blocks.
    private static final double SLING_LOOKS = HandPose.SLING_HOMING;

    private record Taken(LivingEntity living, Vec3 caught, int at) {
    }

    private final List<Taken> taken = new ArrayList<>();
    private final Set<LivingEntity> shot = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<LivingEntity> freed = Collections.newSetFromMap(new IdentityHashMap<>());
    @Nullable
    private AbyssPit pit;

    GiantHandShots(GiantHands storm, int variant, Vec3 base, LivingEntity target) {
        super(storm, variant, base, target);
    }

    GiantHandShots(GiantHands storm, int variant, Vec3 base, LivingEntity target, Vec3 facing) {
        super(storm, variant, base, target, facing);
    }

    @Override
    void feat(ServerLevel level) {
        switch (this.move) {
            case HandPose.ABYSS -> this.abyss(level);
            case HandPose.BEAMEYE -> this.beamEye(level);
            case HandPose.GUNSLINGER -> this.gunslinger(level);
            default -> super.feat(level);
        }
    }

    @Override
    void letGo() {
        super.letGo();
        for (Taken taken : this.taken) {
            GRABBED.remove(taken.living().getId(), this);
            if (taken.living() instanceof Mob mob) {
                HeldMobs.release(mob);
            }
        }
        this.taken.clear();
    }

    @Override
    void end(ServerLevel level) {
        super.end(level);
        if (this.pit != null) {
            this.pit.close();
            this.pit = null;
        }
    }

    private double share(double share) {
        return this.storm.ability.getDamage() * share;
    }

    // The ground cracks round the creature, tears a jerk at a time and gives way; tentacles rise out of the stars at the
    // pit's floor, take everything in it, drag it down and kill it; the hands push the ground shut again.
    private void abyss(ServerLevel level) {
        Vec3 facing = this.aim.subtract(this.base);
        Vec3 floor = HandAbyss.floor(this.variant, this.base);
        if (this.t == 2 || this.t == HandAbyss.LEAVES + 6) {
            boolean opening = this.t == 2;
            for (HandGroup.Sub sub : HandGroup.at(this.variant, this.base, facing, this.t)) {
                if (opening) {
                    this.opens(level, sub.portal(), sub.left() ? 1.2F : 1.4F);
                } else {
                    this.shuts(level, sub.portal(), sub.left() ? 1.5F : 1.7F);
                }
            }
        }
        if (this.t == HandAbyss.DIGS) {
            this.rim(level, 10);
            this.storm.sound(level, this.base, SoundEvents.ANVIL_LAND, 1.6F, 0.5F);
            this.storm.sound(level, this.base, SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.5F);
        }
        for (int k = 0; k < HandAbyss.TEARS.length; k++) {
            if (this.t == HandAbyss.TEARS[k]) {
                this.rim(level, 8 + 4 * k);
                ParticleFx.cloud(level, ParticleTypes.END_ROD, this.base.add(0.0, 0.2, 0.0), 6 + 4 * k, 2.0, 0.06);
                this.storm.sound(level, this.base, SoundEvents.DEEPSLATE_BREAK, 2.2F, 0.5F + 0.1F * k);
                this.storm.sound(level, this.base, SoundEvents.ROOTED_DIRT_BREAK, 2.2F, 0.6F + 0.2F * k);
            }
        }
        if (this.t == HandAbyss.OPENS) {
            this.rim(level, 40);
            this.pit = AbyssPit.open(level, this.storm.owner, this.base, HandAbyss.depth(this.variant));
            ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.6F), this.base.add(0.0, 0.2, 0.0), 40, 0.7);
            ParticleFx.cloud(level, ParticleTypes.END_ROD, floor.add(0.0, 0.5, 0.0), 30, 2.0, 0.2);
            this.storm.sound(level, this.base, SoundEvents.GENERIC_EXPLODE.value(), 2.4F, 0.5F);
            this.storm.sound(level, this.base, SoundEvents.DEEPSLATE_BREAK, 3.0F, 0.4F);
            this.storm.sound(level, this.base, SoundEvents.END_PORTAL_SPAWN, 1.0F, 0.7F);
        }
        if (this.t == HandAbyss.RISES) {
            this.storm.sound(level, floor, SoundEvents.WARDEN_EMERGE, 2.0F, 1.2F);
            this.storm.sound(level, floor, SoundEvents.SLIME_ATTACK, 2.0F, 0.5F);
        }
        if (this.t >= HandAbyss.SNARES && this.t <= HandAbyss.SNARES_UNTIL
                && (this.t - HandAbyss.SNARES) % SNARE_EVERY == 0) {
            this.snareAll(level, floor);
        }
        this.drag(level);
        if (this.t == HandAbyss.CLOSES) {
            this.storm.sound(level, this.base, SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.5F);
            this.storm.sound(level, this.base, SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.5F);
        }
        if (this.t == HandAbyss.SHUTS) {
            this.letGo();
            if (this.pit != null) {
                this.pit.close();
                this.pit = null;
            }
            this.rim(level, 40);
            ParticleFx.send(level, ParticleTypes.FLASH, this.base.x, this.base.y + 0.5, this.base.z, 1, 0.0, 0.0, 0.0,
                    0.0);
            ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), this.base.add(0.0, 0.15, 0.0), 40,
                    0.8);
            this.storm.sound(level, this.base, SoundEvents.ANVIL_LAND, 2.4F, 0.4F);
            this.storm.sound(level, this.base, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.7F);
            this.storm.sound(level, this.base, SoundEvents.AMETHYST_CLUSTER_BREAK, 2.0F, 0.5F);
        }
    }

    // The ground's own dust thrown up all round the pit's rim.
    private void rim(ServerLevel level, int count) {
        for (int k = 0; k < 8; k++) {
            double angle = Math.PI * 2.0 * k / 8.0;
            Vec3 at = this.base.add(Math.sin(angle) * HandAbyss.RADIUS, 0.0, Math.cos(angle) * HandAbyss.RADIUS);
            this.dustAt(level, at, Math.max(2, count / 4), 0.6);
        }
        BlockPos under = BlockPos.containing(this.base.x, this.base.y - 0.5, this.base.z);
        BlockState ground = level.isLoaded(under) ? level.getBlockState(under) : null;
        if (ground != null && !ground.isAir()) {
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), this.base.x, this.base.y + 0.2,
                    this.base.z, count, HandAbyss.RADIUS * 0.5, 0.3, HandAbyss.RADIUS * 0.5, 0.3);
        }
    }

    // A tentacle winds round everything in the pit, on the ground over it or falling in, that the caster may hurt.
    private void snareAll(ServerLevel level, Vec3 floor) {
        double wide = HandAbyss.RADIUS + 0.5;
        for (LivingEntity living : this.near(level, HandAbyss.RADIUS + HandAbyss.DEEPEST + 4.0)) {
            double dx = living.getX() - this.base.x;
            double dz = living.getZ() - this.base.z;
            if (dx * dx + dz * dz > wide * wide || living.getY() < floor.y - PIT_BELOW
                    || living.getY() > this.base.y + PIT_ABOVE || this.holds(living) || this.freed.contains(living)
                    || living.getType().is(Tags.EntityTypes.BOSSES) || HeldMobs.isHeldByAnyone(living)
                    || LightBubble.trapped(living) || GRABBED.containsKey(living.getId())) {
                continue;
            }
            GRABBED.put(living.getId(), this);
            if (living instanceof Mob mob) {
                HeldMobs.hold(mob);
            }
            Vec3 middle = living.getBoundingBox().getCenter();
            this.taken.add(new Taken(living, middle, this.t));
            HandVictimPayload.send(living, this.id(), HandVictimPayload.ABYSS);
            this.storm.sound(level, middle, SoundEvents.SLIME_SQUISH, 1.8F, 0.5F);
            this.storm.sound(level, middle, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.6F, 0.6F);
        }
    }

    private boolean holds(LivingEntity living) {
        for (Taken taken : this.taken) {
            if (taken.living() == living) {
                return true;
            }
        }
        return false;
    }

    // Each taken creature is dragged down to the floor, sunk into the stars below it and dies there; one that lives
    // through it rises out of the ground again and is thrown out over the pit's rim.
    private void drag(ServerLevel level) {
        this.taken.removeIf(taken -> {
            LivingEntity living = taken.living();
            if (living.isRemoved() || living.level() != level) {
                GRABBED.remove(living.getId(), this);
                return true;
            }
            if (this.t - taken.at() >= HandAbyss.SPITS && living.isAlive()) {
                this.spit(level, taken);
                return true;
            }
            return false;
        });
        for (Taken taken : this.taken) {
            LivingEntity living = taken.living();
            int since = this.t - taken.at();
            hold(living, since > HandAbyss.SINKS && living.isAlive()
                    ? HandAbyss.rising(this.variant, this.base, taken.caught(), living.getBbHeight(), since)
                    : HandAbyss.held(this.variant, this.base, taken.caught(), living.getBbHeight(), since));
            if (since == HandAbyss.SINKS && living.isAlive()) {
                Vec3 at = HandAbyss.floor(this.variant, this.base).add(0.0, 0.3, 0.0);
                this.hit(level, living, this.share(ABYSS_DAMAGE), Vec3.ZERO, 0.0, 0.0);
                ParticleFx.cloud(level, ParticleTypes.END_ROD, at, 12, 0.8, 0.12);
                this.storm.sound(level, at, SoundEvents.ENDERMAN_TELEPORT, 1.8F, 0.5F);
                this.storm.sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
            }
        }
    }

    private void spit(ServerLevel level, Taken taken) {
        LivingEntity living = taken.living();
        GRABBED.remove(living.getId(), this);
        if (living instanceof Mob mob) {
            HeldMobs.release(mob);
        }
        HandVictimPayload.send(living, this.id(), HandVictimPayload.ABYSS_FREED);
        this.freed.add(living);
        this.push(living, flat(living.position().subtract(this.base)), SPIT_OUT, SPIT_UP);
        Vec3 middle = living.getBoundingBox().getCenter();
        this.dustAt(level, middle, 12, 0.8);
        ParticleFx.cloud(level, ParticleTypes.END_ROD, middle, 14, 0.6, 0.2);
        this.storm.sound(level, middle, SoundEvents.SLIME_ATTACK, 1.8F, 0.6F);
        this.storm.sound(level, middle, SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.7F);
    }

    // The eye opens, gathers light and fires: every few ticks the beam burns everything along it to its end.
    private void beamEye(ServerLevel level) {
        HandPose.Place place = this.place();
        Vec3 eye = place.at(HandPose.EYE_POINT);
        if (this.t == HandPose.BEAMEYE_OPENS) {
            this.storm.sound(level, eye, SoundEvents.ENDER_EYE_DEATH, 2.0F, 0.5F);
            this.storm.sound(level, eye, SoundEvents.BEACON_ACTIVATE, 1.4F, 0.7F);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), eye, 20, 0.4);
        }
        if (this.t == HandPose.BEAMEYE_CHARGES) {
            this.storm.sound(level, eye, SoundEvents.WARDEN_SONIC_CHARGE, 2.0F, 1.1F);
            this.storm.sound(level, eye, SoundEvents.BEACON_POWER_SELECT, 1.6F, 0.6F);
        }
        if (this.t == HandPose.BEAMEYE_FIRES) {
            this.storm.sound(level, eye, SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 1.3F);
            this.storm.sound(level, eye, SoundEvents.BEACON_ACTIVATE, 2.0F, 1.6F);
            this.storm.sound(level, eye, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.4F, 1.4F);
        }
        if (this.t < HandPose.BEAMEYE_FIRES || this.t > HandPose.BEAMEYE_STOPS
                || (this.t - HandPose.BEAMEYE_FIRES) % BEAM_EVERY != 0) {
            return;
        }
        Vec3 way = HandPose.aimedAlong(place.forward(), eye, this.base, this.aim.subtract(this.base));
        BlockHitResult wall = LoadedWorld.clip(level, new ClipContext(eye, eye.add(way.scale(BEAM_RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? eye.add(way.scale(BEAM_RANGE)) : wall.getLocation();
        double length = end.distanceTo(eye);
        for (LivingEntity living : this.near(level, BEAM_RANGE + 6.0)) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(eye);
            double along = Mth.clamp(to.dot(way), 0.0, length);
            if (to.subtract(way.scale(along)).length() > BEAM_WIDE + living.getBbWidth() * 0.5) {
                continue;
            }
            DeathStyles.mark(living, DeathStyles.Style.ASH);
            this.hit(level, living, this.share(BEAM_DAMAGE), flat(way), 0.0, 0.0);
            // Held in the beam: a slow push along it, not the hop of a blow.
            living.setDeltaMovement(way.x * 0.2, Math.min(0.0, living.getDeltaMovement().y), way.z * 0.2);
            living.hurtMarked = true;
        }
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), end, 14, 0.35);
        if (wall.getType() != HitResult.Type.MISS) {
            this.dustAt(level, end.add(0.0, 0.5, 0.0), 6, 0.4);
        }
        if ((this.t - HandPose.BEAMEYE_FIRES) % (BEAM_EVERY * 3) == 0) {
            this.storm.sound(level, eye, SoundEvents.BEACON_AMBIENT, 2.0F, 1.4F);
            this.storm.sound(level, end, SoundEvents.FIRE_EXTINGUISH, 1.0F, 1.6F);
        }
    }

    // A revolver grows out of the ring's light into the fist, and three shots go off, each at the creature the hand
    // has turned to, the next one picked after each shot; then the revolver breaks apart.
    private void gunslinger(ServerLevel level) {
        HandPose.Place place = this.place();
        RevolverGun.Gun gun = RevolverGun.inFist(place);
        Vec3 muzzle = gun.at(RevolverGun.MUZZLE);
        if (this.t == HandPose.ARRIVES + 4) {
            this.storm.sound(level, muzzle, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.9F);
            this.storm.sound(level, muzzle, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.6F);
        }
        for (int k = 0; k < HandPose.SLING_SHOTS.length; k++) {
            int at = HandPose.SLING_SHOTS[k];
            if (this.t == at - 5) {
                this.storm.sound(level, muzzle, SoundEvents.TRIPWIRE_CLICK_ON, 1.6F, 0.6F);
                this.storm.sound(level, muzzle, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.2F, 1.6F);
            }
            if (this.t == at) {
                this.fire(level, gun, muzzle);
            }
        }
        if (this.t == HandPose.SLING_BLOWS) {
            this.storm.sound(level, muzzle, SoundEvents.CANDLE_EXTINGUISH, 1.6F, 0.8F);
        }
        if (this.t == HandPose.GUN_BREAKS) {
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), gun.at(RevolverGun.CENTER), 24, 0.3);
            this.storm.sound(level, muzzle, SoundEvents.GLASS_BREAK, 1.6F, 0.8F);
            this.storm.sound(level, muzzle, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6F, 0.9F);
        }
    }

    private void fire(ServerLevel level, RevolverGun.Gun gun, Vec3 muzzle) {
        Vec3 own = gun.forward();
        Vec3 way = own;
        if (this.target.isAlive() && this.target.level() == level) {
            Vec3 to = this.target.getBoundingBox().getCenter().subtract(muzzle).normalize();
            way = to.dot(own) < 0.5 ? own : to;
        }
        BlockHitResult wall = LoadedWorld.clip(level, new ClipContext(muzzle, muzzle.add(way.scale(SHOT_RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        double far = wall.getType() == HitResult.Type.MISS ? SHOT_RANGE : wall.getLocation().distanceTo(muzzle);
        LivingEntity first = null;
        double nearest = far;
        for (LivingEntity living : this.near(level, SHOT_RANGE + 6.0)) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(muzzle);
            double along = to.dot(way);
            if (along < 0.0 || along > nearest) {
                continue;
            }
            if (to.subtract(way.scale(along)).length() <= SHOT_WIDE + living.getBbWidth() * 0.5) {
                nearest = along;
                first = living;
            }
        }
        if (first != null) {
            this.hit(level, first, this.share(SHOT_DAMAGE), flat(way), 0.6, 0.2);
            this.shot.add(first);
        }
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), muzzle, 12, 0.25);
        ParticleFx.send(level, ParticleTypes.FLASH, muzzle.x, muzzle.y, muzzle.z, 1, 0.0, 0.0, 0.0, 0.0);
        this.storm.sound(level, muzzle, SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 1.8F);
        this.storm.sound(level, muzzle, SoundEvents.FIREWORK_ROCKET_BLAST, 2.0F, 0.6F);
        this.storm.sound(level, muzzle, SoundEvents.AMETHYST_BLOCK_HIT, 1.6F, 1.6F);
        this.next(level);
    }

    // The nearest creature it has not shot yet, else the one it shot that still stands nearest.
    private void next(ServerLevel level) {
        LivingEntity fresh = null;
        LivingEntity again = null;
        double freshBest = SLING_LOOKS;
        double againBest = SLING_LOOKS;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(this.base, this.base).inflate(SLING_LOOKS), entity -> fair(this.storm.owner, entity))) {
            double dx = living.getX() - this.base.x;
            double dz = living.getZ() - this.base.z;
            double far = Math.sqrt(dx * dx + dz * dz);
            if (!living.isAlive() || far > SLING_LOOKS || far < HandPose.SLING_CLEAR * GiantHands.SCALE) {
                continue;
            }
            if (!this.shot.contains(living) && far < freshBest) {
                freshBest = far;
                fresh = living;
            } else if (this.shot.contains(living) && far < againBest) {
                againBest = far;
                again = living;
            }
        }
        LivingEntity next = fresh != null ? fresh : again;
        if (next != null) {
            this.target = next;
        }
    }
}
