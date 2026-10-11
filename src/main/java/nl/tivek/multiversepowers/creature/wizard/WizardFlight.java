package nl.tivek.multiversepowers.creature.wizard;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.spell.SpellBlast;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// The Wizard in the air, from his second phase: he circles his foe a few blocks up at a caster's distance, sweeping
// round them now this way, now that, closer when he loses sight of them; while he casts he hangs where he is. He blinks
// from one spot in the air to the next (AIRSTEP, and his blink), only to a spot that is free, loaded and in sight of
// them. The comet: he soars up, hangs aiming, and dives on them in flames; the phantom strike: he is gone and over
// them, hangs a moment and drops onto them with his staff. Dying, he sinks to the ground.
final class WizardFlight {
    private static final double COMET_SPEED = 1.7;
    private static final double PHANTOM_SPEED = 1.9;
    private static final double COMET_REACH = 4.6;
    private static final double PHANTOM_REACH = 3.8;
    // How high over the ground the supernova lifts him.
    private static final double NOVA_HIGH = 9.0;
    private final Wizard wizard;
    // Where a dive comes down (null until he has aimed it), and whether it has struck.
    @Nullable
    private Vec3 dive;
    private boolean struck;

    WizardFlight(Wizard wizard) {
        this.wizard = wizard;
    }

    // How high over his foe's feet he flies in each phase.
    private static double height(int phase) {
        return 2.6 + 0.5 * (phase - 2);
    }

    void reset() {
        this.struck = false;
        this.dive = null;
    }

    // Circles them in the air: further off when they come close, nearer when he cannot see them, swinging round.
    void steer(LivingEntity target, double far, boolean sees, int side, int now) {
        Vec3 me = this.wizard.position();
        Vec3 flat = me.subtract(target.position()).multiply(1.0, 0.0, 1.0);
        Vec3 out = flat.lengthSqr() < 1.0E-3 ? MonsterBlows.ahead(this.wizard).reverse() : flat.normalize();
        double swing = 0.45 * side;
        Vec3 round = new Vec3(out.x * Math.cos(swing) - out.z * Math.sin(swing), 0.0,
                out.x * Math.sin(swing) + out.z * Math.cos(swing));
        double want = !sees ? 3.5 : far < WizardMoves.NEAR ? 11.5 : 9.5;
        double up = height(this.wizard.phase()) + 0.9 * Math.sin(now * 0.05 + this.wizard.getId());
        Vec3 goal = target.position().add(round.scale(want)).add(0.0, up, 0.0);
        this.toward(goal, 0.3 + 0.04 * this.wizard.phase());
    }

    // Flies toward `goal`, slowing as he comes near, no faster than `speed`.
    private void toward(Vec3 goal, double speed) {
        Vec3 to = goal.subtract(this.wizard.position());
        double d = to.length();
        Vec3 want = d < 1.0E-3 ? Vec3.ZERO : to.scale(Math.min(speed, d * 0.12) / d);
        Vec3 v = this.wizard.getDeltaMovement();
        Vec3 next = v.add(want.subtract(v).scale(0.2));
        if (this.wizard.horizontalCollision) {
            next = next.add(0.0, 0.08, 0.0);
        }
        this.wizard.setDeltaMovement(next);
    }

    // Hangs where he is while he casts.
    void hover() {
        this.wizard.setDeltaMovement(this.wizard.getDeltaMovement().scale(0.7));
    }

    // A spot in the air `near` to `far` from `middle` and `low` to `high` over it: free, loaded, at least `least` from
    // where he is now and in sight of `target`; null when none is found.
    @Nullable
    Vec3 airSpot(ServerLevel level, Vec3 middle, double near, double far, double low, double high,
            @Nullable LivingEntity target, double least) {
        Vec3 from = this.wizard.position();
        for (int tries = 0; tries < 24; tries++) {
            double angle = this.wizard.getRandom().nextDouble() * Mth.TWO_PI;
            double out = Mth.lerp(this.wizard.getRandom().nextDouble(), near, far);
            double up = Mth.lerp(this.wizard.getRandom().nextDouble(), low, high);
            Vec3 at = middle.add(Math.cos(angle) * out, up, Math.sin(angle) * out);
            if (this.free(level, at, from, least, target)) {
                return at;
            }
        }
        return null;
    }

    private boolean free(ServerLevel level, Vec3 at, Vec3 from, double least, @Nullable LivingEntity target) {
        if (at.distanceTo(from) < least || !level.isLoaded(BlockPos.containing(at))
                || at.y < level.getMinBuildHeight() + 2 || at.y > level.getMaxBuildHeight() - 4) {
            return false;
        }
        AABB box = this.wizard.getDimensions(this.wizard.getPose()).makeBoundingBox(at);
        if (!level.noCollision(this.wizard, box) || level.containsAnyLiquid(box)) {
            return false;
        }
        return target == null || sees(level, at.add(0.0, 1.6 + this.wizard.hover(), 0.0), target.getEyePosition());
    }

    private boolean sees(ServerLevel level, Vec3 from, Vec3 to) {
        return LoadedWorld.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                this.wizard)).getType() == HitResult.Type.MISS;
    }

    // Gone in a fold of the dark and there again at `to`.
    void blinkTo(ServerLevel level, Vec3 to) {
        Vec3 from = this.wizard.position();
        this.wizard.teleportTo(to.x, to.y, to.z);
        this.wizard.setDeltaMovement(Vec3.ZERO);
        WizardSparks.blink(level, from, to);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.2F,
                1.2F);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.4F);
    }

    // A blink to another spot in the air round them, if he finds one.
    void airstep(ServerLevel level, @Nullable LivingEntity target) {
        Vec3 middle = target == null ? this.wizard.position() : target.position();
        double up = height(this.wizard.phase());
        Vec3 to = this.airSpot(level, middle, 7.0, 12.0, up - 1.0, up + 3.0, target, 5.0);
        if (to != null) {
            this.blinkTo(level, to);
        }
    }

    // Somewhere in the air 9 to 14 blocks from `middle`, in sight of `target`; whether he found one.
    boolean blinkNear(ServerLevel level, Vec3 middle, @Nullable LivingEntity target) {
        double up = height(this.wizard.phase());
        Vec3 to = this.airSpot(level, middle, 9.0, 14.0, up, up + 4.0, target, 7.0);
        if (to == null) {
            return false;
        }
        this.wizard.teleportTo(to.x, to.y, to.z);
        this.wizard.setDeltaMovement(Vec3.ZERO);
        return true;
    }

    // The comet: up, hanging and aiming, then down onto them in flames.
    void comet(ServerLevel level, int t, @Nullable LivingEntity target) {
        if (t == 1) {
            this.struck = false;
        }
        if (t <= WizardMove.COMET_UP) {
            this.wizard.setDeltaMovement(this.wizard.getDeltaMovement().multiply(0.6, 0.0, 0.6).add(0.0, 0.62, 0.0));
            return;
        }
        if (t < WizardMove.COMET_DIVE) {
            this.hover();
            if (target != null && t < WizardMove.COMET_DIVE - 4) {
                this.dive = WizardCasts.ground(level, target.position());
            }
            this.mark(level, t, Wizard.FIRE, COMET_REACH);
            return;
        }
        if (this.struck) {
            this.hover();
            return;
        }
        this.fall(level, t, COMET_SPEED, ParticleTypes.FLAME, Wizard.FIRE, COMET_REACH, WizardMove.COMET,
                WizardEntities.INFERNO);
    }

    // The phantom strike: gone and over them, hanging a moment, then dropped onto them with his staff.
    void phantom(ServerLevel level, int t, @Nullable LivingEntity target) {
        if (t == WizardMove.release(WizardMove.PHANTOM)) {
            this.struck = false;
            if (target != null) {
                this.over(level, target);
            }
            return;
        }
        if (t < WizardMove.release(WizardMove.PHANTOM)) {
            return;
        }
        if (t < WizardMove.PHANTOM_SLAM) {
            this.wizard.setDeltaMovement(Vec3.ZERO);
            if (target != null && t < WizardMove.PHANTOM_SLAM - 4) {
                this.dive = WizardCasts.ground(level, target.position());
            }
            this.mark(level, t, Wizard.VIOLET, PHANTOM_REACH);
            return;
        }
        if (this.struck) {
            this.hover();
            return;
        }
        this.fall(level, t, PHANTOM_SPEED, ParticleTypes.REVERSE_PORTAL, Wizard.VIOLET, PHANTOM_REACH,
                WizardMove.PHANTOM, WizardEntities.ARCANE);
    }

    // Behind them and over them (or anywhere close over them), an image of him left where he was.
    private void over(ServerLevel level, LivingEntity target) {
        Vec3 look = target.getLookAngle().multiply(1.0, 0.0, 1.0);
        look = look.lengthSqr() < 1.0E-3 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
        Vec3 behind = target.position().subtract(look.scale(2.4)).add(0.0, 4.2, 0.0);
        Vec3 from = this.wizard.position();
        Vec3 to = this.free(level, behind, from, 0.0, null) ? behind
                : this.airSpot(level, target.position(), 0.5, 3.0, 3.0, 5.5, null, 0.0);
        if (to == null) {
            return;
        }
        WizardDecoy.leave(level, this.wizard, from);
        this.blinkTo(level, to);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    // Where the dive will come down: a ring on the ground closing in.
    private void mark(ServerLevel level, int t, int colour, double reach) {
        if (t % 2 == 0 && this.dive != null) {
            ParticleFx.ring(level, Wizard.dust(colour, 1.5F), this.dive.add(0.0, 0.15, 0.0), reach * (1.2 - 0.02 * t),
                    24, t * 0.2);
        }
    }

    // The dive itself, and the blow where it lands.
    private void fall(ServerLevel level, int t, double speed, ParticleOptions trail, int colour, double reach, int kind,
            ResourceKey<DamageType> type) {
        Vec3 me = this.wizard.position();
        if (this.dive == null) {
            // Lost them before he could aim: straight down.
            this.dive = WizardCasts.ground(level, me);
        }
        Vec3 to = this.dive.subtract(me);
        boolean there = to.length() < 1.4 || this.wizard.verticalCollision || this.wizard.horizontalCollision
                || this.wizard.onGround() || t > WizardMove.length(kind) - 8;
        if (!there) {
            this.wizard.setDeltaMovement(to.normalize().scale(speed));
            ParticleFx.send(level, trail, me.x, me.y + 1.0, me.z, 6, 0.3, 0.5, 0.3, 0.02);
            ParticleFx.send(level, Wizard.dust(colour, 1.8F), me.x, me.y + 1.0, me.z, 4, 0.3, 0.5, 0.3, 0.0);
            return;
        }
        this.struck = true;
        this.wizard.setDeltaMovement(0.0, 0.55, 0.0);
        Vec3 at = me.add(0.0, 0.3, 0.0);
        for (LivingEntity living : SpellBlast.burst(level, this.wizard, this.wizard, at, 2.4F, false, reach,
                this.wizard.harm(kind), type)) {
            if (kind == WizardMove.COMET) {
                living.igniteForSeconds(4.0F);
            }
            living.setDeltaMovement(living.getDeltaMovement().add(0.0, 0.5, 0.0));
            living.hurtMarked = true;
        }
        ParticleFx.shockwave(level, Wizard.dust(colour, 2.4F), at, 64, 0.9);
        ParticleFx.shockwave(level, kind == WizardMove.COMET ? ParticleTypes.FLAME : ParticleTypes.END_ROD, at, 40,
                0.6);
        ParticleFx.groundStar(level, Wizard.dust(colour, 1.6F), at, reach, 5, 2, t * 0.3, 0.3);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at.add(0.0, 1.0, 0.0), 30, 0.6);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 0.8F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    // The supernova lifts him high over the ground.
    void rise(ServerLevel level, int t) {
        if (t < WizardMove.NOVA_RISE && this.overGround(level) < NOVA_HIGH) {
            this.wizard.setDeltaMovement(this.wizard.getDeltaMovement().multiply(0.6, 0.0, 0.6).add(0.0, 0.42, 0.0));
        } else {
            this.wizard.setDeltaMovement(Vec3.ZERO);
        }
    }

    // How far over the ground under him he is (as far as it looks, 24 blocks).
    private double overGround(ServerLevel level) {
        Vec3 me = this.wizard.position();
        BlockHitResult down = LoadedWorld.clip(level, new ClipContext(me, me.add(0.0, -24.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this.wizard));
        return down.getType() == HitResult.Type.MISS ? 24.0 : me.y - down.getLocation().y;
    }

    // Dying in the air, he sinks to the ground, and falls as anything does once he is there.
    static void sink(Wizard wizard) {
        if (!wizard.isNoGravity()) {
            return;
        }
        if (wizard.onGround() || wizard.verticalCollision) {
            wizard.setNoGravity(false);
            return;
        }
        wizard.setDeltaMovement(0.0, -0.3, 0.0);
    }
}
