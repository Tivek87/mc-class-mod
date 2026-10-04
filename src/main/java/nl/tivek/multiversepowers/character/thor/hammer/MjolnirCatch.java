package nl.tivek.multiversepowers.character.thor.hammer;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.thor.ThorCharge;
import nl.tivek.multiversepowers.character.thor.ThorGrab;
import nl.tivek.multiversepowers.character.thor.ThorMoves;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.spell.SpellTargets;

// The hammer flying back to Thor, called from wherever it is or by itself after a throw: it jerks free, turns its grip
// to him and speeds up, flies straight through blocks, hits each creature in its way once (knocking it aside, never
// into him) and slams grip first into his hand: his right, his left in flight or when he called it to fly, or onto
// his belt while his right hand holds a creature.
abstract class MjolnirCatch extends MjolnirRest {
    private static final double CALLED_SPEED = 3.2;
    private static final double HOME_SPEED = 2.6;
    private static final int SPEED_UP = 4;
    private static final double ASIDE = 1.0;
    private static final double ASIDE_UP = 0.3;

    private boolean called;
    private boolean toFly;
    private int backAge;
    float backDamage;

    MjolnirCatch(UUID owner) {
        super(owner);
    }

    // Called back: it comes at once from wherever it is, wrenched out of a block it is stuck in; still in his hand
    // (thrown but not yet let go) the throw is simply undone.
    final void callBack(ServerLevel level, ServerPlayer owner, float damage, boolean toFly) {
        this.backDamage = damage;
        if (this.state == State.BACK) {
            this.called = true;
            this.toFly |= toFly;
            return;
        }
        if (this.inThrow()) {
            this.toFly = toFly;
            this.home(owner, toFly ? Hand.LEFT : Hand.RIGHT, false);
            return;
        }
        if (this.stuck()) {
            BlockPos in = BlockPos.containing(this.at.subtract(Vec3.atLowerCornerOf(this.restFace.getNormal())
                    .scale(0.05)));
            BlockState block = level.isLoaded(in) ? level.getBlockState(in) : null;
            if (block != null && !block.isAir()) {
                level.playSound(null, this.at.x, this.at.y, this.at.z,
                        block.getSoundType(level, in, owner).getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.6F);
            }
            this.dust(level, 24);
            level.playSound(null, this.at.x, this.at.y, this.at.z, SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.PLAYERS, 0.5F, 1.6F);
        } else if (this.state == State.RESTING && this.rest == ThrownHammer.LYING) {
            this.dust(level, 12);
        }
        this.hit.clear();
        this.toFly = toFly;
        this.turnBack(owner, true);
    }

    @Override
    final void turnBack(ServerPlayer owner, boolean called) {
        this.state = State.BACK;
        this.called = called;
        this.backAge = 0;
        this.age = 0;
        if (this.shown != null) {
            this.shown.setRest(ThrownHammer.BACK);
        }
        owner.serverLevel().playSound(null, this.at.x, this.at.y, this.at.z, SoundEvents.TRIDENT_RETURN,
                SoundSource.PLAYERS, 1.0F, 1.2F);
        ThorMoves.tell(owner, ThorStatePayload.NONE, 0);
    }

    @Override
    final void back(ServerLevel level, ServerPlayer owner) {
        double size = owner.getScale();
        double top = this.called ? CALLED_SPEED : HOME_SPEED;
        double speed = Math.min(top, 1.0 + (top - 1.0) * ++this.backAge / SPEED_UP);
        Hand hand = this.catching(owner);
        Vec3 to = this.catchPoint(owner, hand).subtract(this.at);
        double gap = to.length();
        if (gap < CATCH * size) {
            this.home(owner, hand, true);
            return;
        }
        this.way = to.scale(1.0 / gap);
        Vec3 next = this.at.add(this.way.scale(Math.min(speed, gap)));
        this.passing(level, owner, this.at, next);
        this.move(next);
        this.trail(level, owner);
    }

    // Each creature on its way back is hit once and knocked aside, out of its line.
    private void passing(ServerLevel level, ServerPlayer owner, Vec3 from, Vec3 to) {
        if (this.backDamage <= 0.0F) {
            return;
        }
        float more = ThorCharge.hammer(owner);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.0),
                entity -> Targeting.mayStrike(owner, entity) && !this.hit.contains(entity.getUUID()))) {
            AABB box = target.getBoundingBox().inflate(0.5);
            if (box.clip(from, to).isEmpty() && !box.contains(to) && !box.contains(from)) {
                continue;
            }
            this.hit.add(target.getUUID());
            Knockdowns.brief(target);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(owner), this.backDamage);
            Vec3 middle = target.getBoundingBox().getCenter();
            Vec3 line = to.subtract(from);
            double along = line.lengthSqr() < 1.0E-6 ? 0.0
                    : Math.max(0.0, Math.min(1.0, middle.subtract(from).dot(line) / line.lengthSqr()));
            Vec3 off = middle.subtract(from.add(line.scale(along)));
            Vec3 side = new Vec3(off.x, 0.0, off.z);
            if (side.lengthSqr() < 1.0E-4) {
                side = line.cross(Vectors.UP);
            }
            if (side.lengthSqr() > 1.0E-6) {
                SpellTargets.push(target, side.normalize(), ASIDE * more, ASIDE_UP * more);
            }
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, middle, 14, 0.3, 0.25);
            level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.MACE_SMASH_AIR, SoundSource.PLAYERS, 0.9F,
                    1.1F);
        }
    }

    // Which hand it comes to: his left flying (or called to fly), else his right, unless that holds a creature.
    private Hand catching(ServerPlayer owner) {
        if (this.toFly || ThorMoves.flying(owner)) {
            return Hand.LEFT;
        }
        return ThorGrab.carrying(owner) ? Hand.BELT : Hand.RIGHT;
    }

    private Vec3 catchPoint(ServerPlayer owner, Hand hand) {
        return hand == Hand.LEFT ? hand(owner, true) : owner.getEyePosition().add(0.0, -0.5 * owner.getScale(), 0.0);
    }

    @Override
    final void homed(ServerPlayer owner, Hand hand, boolean caught) {
        boolean fly = this.toFly;
        this.toFly = false;
        this.called = false;
        // Called to fly, he takes off the moment it is in his hand.
        if (fly) {
            ThorMoves.takeOff(owner);
        }
    }
}
