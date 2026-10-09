package nl.tivek.multiversepowers.character.thor.hammer;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.thor.ThorCharge;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.spell.SpellTargets;

// The hammer flying out: it leaves his hand as his arm comes through, from there towards what he aims at, and flies
// until a block, a creature or the end of its reach stops it, throwing far what it hits. Thrown to come back it then
// flies back; to stay or to be followed it rests there; hurled down by a Storm Throw its lightning strikes first.
abstract class MjolnirFlight extends MjolnirCore {
    static final double SPEED = 2.2;
    // It leaves his hand this many ticks after the button.
    static final int RELEASE = 3;
    private static final double KNOCK = 3.2;
    private static final double KNOCK_UP = 0.7;
    // A creature it is thrown at is aimed into this far, a block just past its face.
    private static final double INTO_CREATURE = 1.0;
    static final double INTO_BLOCK = 0.6;

    private Vec3 aim = Vec3.ZERO;
    private double reach;
    private double past;
    private double flown;
    private double far;
    private int release;
    @Nullable
    Direction face;
    @Nullable
    LivingEntity struck;

    MjolnirFlight(UUID owner) {
        super(owner);
    }

    abstract void turnBack(ServerPlayer owner, boolean called);

    abstract void settle(ServerLevel level, ServerPlayer owner, @Nullable Direction face);

    // Where his throwing hand lets go of it: his right, over his shoulder; in flight his left, ahead of him.
    static Vec3 hand(ServerPlayer player, boolean left) {
        double size = player.getScale();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        Vec3 right = flat.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : flat.normalize().cross(Vectors.UP);
        return player.getEyePosition().add(look.scale(0.6 * size)).add(right.scale((left ? -0.35 : 0.35) * size))
                .add(0.0, -0.15 * size, 0.0);
    }

    // Thrown: at what he aims at within `reach`, else where his look meets a block, else as far as it reaches. It
    // leaves his hand RELEASE ticks from now.
    final void launch(ServerPlayer player, Throw kind, float damage, double reach) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        LivingEntity target = Targeting.aimLiving(player, level, reach);
        if (target != null) {
            this.launchAt(player, kind, damage, reach, target.getBoundingBox().getCenter(), INTO_CREATURE, RELEASE);
            return;
        }
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(eye, eye.add(look.scale(reach)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean wall = block.getType() != HitResult.Type.MISS;
        this.launchAt(player, kind, damage, reach, wall ? block.getLocation() : eye.add(look.scale(reach)),
                wall ? INTO_BLOCK : 0.0, RELEASE);
    }

    // Thrown at a point of his own choosing and `past` blocks on beyond it, at most `reach` blocks; it leaves his hand
    // `release` ticks from now.
    final void launchAt(ServerPlayer player, Throw kind, float damage, double reach, Vec3 aim, double past,
            int release) {
        this.aim = aim;
        this.past = past;
        this.state = State.OUT;
        this.kind = kind;
        this.damage = damage;
        this.reach = reach;
        this.flown = 0.0;
        this.release = release;
        this.age = 0;
        this.cocked = false;
        this.face = null;
        this.struck = null;
        this.hit.clear();
        this.way = player.getLookAngle();
        this.at = hand(player, kind == Throw.STORM);
        this.run(player.serverLevel());
    }

    // Still in his hand: when it is let go, it flies at `aim` instead.
    final void aimAt(Vec3 aim) {
        if (this.inThrow()) {
            this.aim = aim;
        }
    }

    @Override
    final void out(ServerLevel level, ServerPlayer owner) {
        if (this.shown == null) {
            Vec3 from = hand(owner, this.kind == Throw.STORM);
            this.at = from;
            if (--this.release > 0) {
                return;
            }
            Vec3 to = this.aim.subtract(from);
            this.way = to.lengthSqr() < 1.0E-6 ? owner.getLookAngle() : to.normalize();
            this.far = Math.min(this.reach, to.length() + this.past);
            this.shown = this.show(level, owner, from, ThrownHammer.OUT);
            level.playSound(null, from.x, from.y, from.z, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.2F,
                    0.7F);
            level.playSound(null, from.x, from.y, from.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4F,
                    1.8F);
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, from, 8, 0.15, 0.1);
            if (this.kind == Throw.STORM) {
                StormStrike.hurled(level, owner, from, this.way, this.far);
            }
        }
        double step = Math.min(this.kind == Throw.STORM ? HammerRules.STORM_SPEED : SPEED, this.far - this.flown);
        Vec3 next = this.at.add(this.way.scale(step));
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(this.at, next, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, owner));
        boolean stopped = block.getType() != HitResult.Type.MISS;
        if (stopped) {
            next = block.getLocation();
            this.face = block.getDirection();
        }
        Vec3 from = this.at;
        Vec3 to = next;
        LivingEntity first = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.0),
                entity -> Targeting.mayStrike(owner, entity) && !this.hit.contains(entity.getUUID()))) {
            AABB box = target.getBoundingBox().inflate(0.4);
            Optional<Vec3> enters = box.clip(from, to);
            if (enters.isEmpty() && !box.contains(to) && !box.contains(from)) {
                continue;
            }
            this.hit.add(target.getUUID());
            this.strike(level, owner, target);
            Vec3 there = enters.orElse(box.contains(from) ? from : to);
            if (there.distanceToSqr(from) < nearest) {
                nearest = there.distanceToSqr(from);
                first = target;
                next = there;
            }
        }
        // The first creature in its way stops it where it hit.
        if (first != null) {
            this.face = null;
            this.struck = first;
            stopped = true;
        }
        this.flown += step;
        this.move(next);
        this.trail(level, owner);
        if (stopped && this.struck == null) {
            this.clang(level, BlockPos.containing(next.subtract(Vec3.atLowerCornerOf(this.face.getNormal())
                    .scale(0.05))));
        }
        if (!stopped && this.flown < this.far - 1.0E-3) {
            return;
        }
        switch (this.kind) {
            case RETURN -> this.turnBack(owner, false);
            // Run out in the air, a Storm Throw strikes nothing and only flies back.
            case STORM -> {
                if (stopped) {
                    StormStrike.strike(level, owner, this.at, this.struck, this.damage);
                }
                this.turnBack(owner, false);
            }
            case STAY, FOLLOW -> this.settle(level, owner, this.face);
        }
    }

    // What it hits is thrown far the way it flew (less so by a Storm Throw, whose lightning does the rest).
    private void strike(ServerLevel level, ServerPlayer owner, LivingEntity target) {
        Knockdowns.brief(target);
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().playerAttack(owner), this.damage);
        Vec3 flat = new Vec3(this.way.x, 0.0, this.way.z);
        Vec3 push = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        float more = ThorCharge.hammer(owner) * (this.kind == Throw.STORM ? 0.4F : 1.0F);
        SpellTargets.push(target, push, KNOCK * more, KNOCK_UP * more);
        Vec3 at = target.getBoundingBox().getCenter();
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 20, 0.4, 0.3);
        ParticleFx.cloud(level, ParticleTypes.CRIT, at, 12, 0.3, 0.3);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_AIR, SoundSource.PLAYERS, 1.2F, 0.8F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    // A block stops it with a clang and a puff of its dust.
    private void clang(ServerLevel level, BlockPos pos) {
        BlockState state = level.isLoaded(pos) ? level.getBlockState(pos) : null;
        if (state != null && !state.isAir()) {
            ParticleFx.cloud(level, new BlockParticleOption(ParticleTypes.BLOCK, state), this.at, 16, 0.2, 0.15);
            level.playSound(null, this.at.x, this.at.y, this.at.z,
                    state.getSoundType(level, pos, null).getBreakSound(), SoundSource.BLOCKS, 0.8F, 0.7F);
        }
        level.playSound(null, this.at.x, this.at.y, this.at.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS,
                0.8F, 1.2F);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, this.at, 10, 0.2, 0.15);
    }

    // Still in his hand: its throw under way, not yet let go.
    final boolean inThrow() {
        return this.state == State.OUT && this.shown == null;
    }
}
