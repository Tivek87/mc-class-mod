package nl.tivek.multiversepowers.character.greenlantern.ability.fist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.flight.Flight;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBeam;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBolt;
import nl.tivek.multiversepowers.character.greenlantern.ability.ring.Recharge;
import nl.tivek.multiversepowers.character.greenlantern.ability.ring.RingHands;
import nl.tivek.multiversepowers.character.greenlantern.fist.FistMoves;
import nl.tivek.multiversepowers.character.greenlantern.fist.FistPayload;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// Left click with empty hands: the ring wraps both fists in gloves of hard light and they strike. Each click throws
// the next of the combo's ten blows (FistMoves), reaching reachBlocks; a pause of comboTicks starts it over. Held,
// the button throws one of four heavy blows at random, each with a giant fist of its own.
public final class LightFists implements Effect {
    private static final double VIEW_RANGE = 96.0;
    // A blow catches what its fist passes within this of, besides the reach.
    private static final double SLACK = 0.55;
    // A wide blow sweeps through this much of a turn either side of the look (cosine).
    private static final double WIDE_ARC = 0.35;
    private static final double LUNGE = 0.55;

    private static final Map<UUID, LightFists> ACTIVE = new HashMap<>();

    private final ServerPlayer owner;
    private final CharacterAbility ability;
    private int move = -1;
    private int age;
    private int combo = -1;
    private int idle;
    private int lastHeavy = -1;
    private boolean queued;
    private Vec3 at = Vec3.ZERO;
    private Vec3 ahead = new Vec3(0.0, 0.0, 1.0);

    private LightFists(ServerPlayer owner, CharacterAbility ability) {
        this.owner = owner;
        this.ability = ability;
    }

    public static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability, boolean on, int data) {
        // One move of the hands at a time (RingHands), and no punch while a bolt still leaves the ring.
        if (!on || Recharge.busy(owner) || Flight.descending(owner) || RingHands.busy(owner, RingHands.Move.FISTS)
                || LightBolt.cooling(owner)) {
            return false;
        }
        boolean hold = (data & Characters.HOLD) != 0;
        if (!hold && (data & Characters.TAP) == 0) {
            return false;
        }
        LightFists fists = ACTIVE.get(owner.getUUID());
        if (fists == null || fists.owner != owner) {
            fists = new LightFists(owner, ability);
            ACTIVE.put(owner.getUUID(), fists);
            Effects.start(level, fists);
        }
        return hold ? fists.heavy(level) : fists.click(level);
    }

    public static boolean busy(ServerPlayer player) {
        LightFists fists = ACTIVE.get(player.getUUID());
        // A blow from before a death or a log-out (another player object) never holds the hands.
        return fists != null && fists.owner == player && fists.move >= 0;
    }

    public static void stop(ServerPlayer player) {
        LightFists fists = ACTIVE.remove(player.getUUID());
        if (fists != null) {
            fists.move = -1;
        }
    }

    // A weapon from the wheel takes the hands: the gloves break up at once for everyone near.
    public static void putAway(ServerPlayer player, ServerLevel level) {
        stop(player);
        PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), VIEW_RANGE,
                new FistPayload(player.getId(), FistPayload.AWAY, 0.0F));
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private boolean click(ServerLevel level) {
        if (this.move >= 0 && (FistMoves.heavy(this.move) || this.age < FistMoves.open(this.move))) {
            this.queued = !FistMoves.heavy(this.move);
            return true;
        }
        return this.next(level);
    }

    private boolean next(ServerLevel level) {
        if (!PowerRing.pay(this.owner, this.ability.value("powerCost"))) {
            return false;
        }
        this.combo = this.idle > this.ability.intValue("comboTicks") || this.combo < 0 ? 0
                : (this.combo + 1) % FistMoves.COMBO;
        this.begin(level, this.combo);
        return true;
    }

    private boolean heavy(ServerLevel level) {
        if (this.move >= 0 && FistMoves.heavy(this.move)) {
            return false;
        }
        if (!PowerRing.pay(this.owner, this.ability.value("heavyPowerCost"))) {
            return false;
        }
        int pick;
        do {
            pick = FistMoves.COMBO + this.owner.getRandom().nextInt(FistMoves.KINDS - FistMoves.COMBO);
        } while (pick == this.lastHeavy);
        this.lastHeavy = pick;
        this.combo = -1;
        this.begin(level, pick);
        return true;
    }

    private void begin(ServerLevel level, int kind) {
        this.move = kind;
        this.age = 0;
        this.queued = false;
        this.idle = 0;
        Vec3 look = this.owner.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        this.ahead = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        this.at = this.owner.position();
        PacketDistributor.sendToPlayersNear(level, null, this.owner.getX(), this.owner.getY(), this.owner.getZ(),
                VIEW_RANGE, new FistPayload(this.owner.getId(), kind, this.owner.getYRot()));
        this.sound(level, this.at.add(0.0, 1.2, 0.0), SoundEvents.PLAYER_ATTACK_SWEEP, 0.4F,
                1.5F + this.owner.getRandom().nextFloat() * 0.3F);
        if (kind == FistMoves.FINISHER) {
            this.owner.setDeltaMovement(this.owner.getDeltaMovement().add(this.ahead.scale(LUNGE)));
            this.owner.hurtMarked = true;
        }
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (ACTIVE.get(this.owner.getUUID()) != this) {
            return false;
        }
        if (!this.owner.isAlive() || this.owner.isRemoved()) {
            ACTIVE.remove(this.owner.getUUID(), this);
            this.move = -1;
            return false;
        }
        if (this.move < 0) {
            this.idle++;
            // Long done and idle, it lets go; the next click makes it again.
            if (this.idle > FistMoves.GUARD + this.ability.intValue("comboTicks")) {
                ACTIVE.remove(this.owner.getUUID(), this);
                return false;
            }
            return true;
        }
        this.age++;
        if (this.age == FistMoves.hit(this.move)) {
            this.land(level);
        }
        if (this.queued && this.age >= FistMoves.open(this.move)) {
            this.move = -1;
            this.next(level);
            return true;
        }
        if (this.age >= FistMoves.length(this.move)) {
            this.move = -1;
            this.idle = 0;
        }
        return true;
    }

    private void land(ServerLevel level) {
        switch (this.move) {
            case FistMoves.HAMMER -> this.blast(level, this.ground(FistMoves.AHEAD), 0.9, 0.7,
                    SoundEvents.MACE_SMASH_GROUND_HEAVY);
            case FistMoves.RISING -> this.blast(level, this.ground(FistMoves.AHEAD), 0.25, 1.25,
                    SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR);
            case FistMoves.SPIN -> this.blast(level, this.owner.position(), 1.2, 0.35, SoundEvents.PLAYER_ATTACK_SWEEP);
            case FistMoves.PISTON -> this.piston(level);
            default -> this.punch(level);
        }
    }

    // A combo blow: the creature its fist meets ahead within reach, and for a sweeping one those beside it as well.
    private void punch(ServerLevel level) {
        double reach = this.ability.value("reachBlocks");
        Vec3 eye = this.owner.getEyePosition();
        Vec3 look = this.owner.getLookAngle();
        Vec3 end = eye.add(look.scale(reach));
        List<LivingEntity> hit = new ArrayList<>();
        LivingEntity best = null;
        double bestFar = Double.MAX_VALUE;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(eye, end).inflate(SLACK + 1.0), living -> living != this.owner && living.isAlive()
                        && PowerRing.canHit(this.owner, living))) {
            AABB box = living.getBoundingBox().inflate(SLACK);
            Vec3 middle = living.getBoundingBox().getCenter();
            boolean ahead = box.contains(eye) || box.clip(eye, end).isPresent();
            if (ahead) {
                double far = middle.distanceToSqr(eye);
                if (far < bestFar) {
                    bestFar = far;
                    best = living;
                }
            } else if (FistMoves.wide(this.move)) {
                Vec3 to = middle.subtract(eye);
                Vec3 flat = new Vec3(to.x, 0.0, to.z);
                if (to.length() <= reach + SLACK && flat.lengthSqr() > 1.0E-6
                        && flat.normalize().dot(this.ahead) >= WIDE_ARC) {
                    hit.add(living);
                }
            }
        }
        if (best != null) {
            hit.add(0, best);
        }
        if (hit.isEmpty()) {
            return;
        }
        double damage = this.ability.getDamage() * FistMoves.strength(this.move);
        if (this.move == FistMoves.FINISHER) {
            damage *= this.ability.value("finisherTimes");
        }
        double lift = this.move == FistMoves.UPPERCUT ? 0.55 : this.move == FistMoves.FINISHER ? 0.4 : 0.15;
        for (LivingEntity living : hit) {
            this.strike(level, living, damage, this.ahead.scale(FistMoves.push(this.move)).add(0.0, lift, 0.0));
        }
        Vec3 spot = hit.get(0).getBoundingBox().getCenter();
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.1F), spot, 8, 0.2);
        this.sound(level, spot, this.move == FistMoves.FINISHER ? SoundEvents.MACE_SMASH_AIR
                : SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, 1.2F + this.owner.getRandom().nextFloat() * 0.2F);
        this.sound(level, spot, SoundEvents.AMETHYST_BLOCK_HIT, 0.7F, 0.8F);
    }

    // A heavy's giant fist striking `middle`: everything within heavyRadius, thrown out from it by `out` and up by
    // `up`.
    private void blast(ServerLevel level, Vec3 middle, double out, double up, SoundEvent sound) {
        double radius = this.ability.value("heavyRadius");
        double damage = this.ability.value("heavyDamage");
        double knock = this.ability.value("heavyKnockback");
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(middle, middle).inflate(radius, 2.5, radius), living -> living != this.owner
                        && living.isAlive() && PowerRing.canHit(this.owner, living))) {
            Vec3 from = living.position().subtract(middle);
            Vec3 flat = new Vec3(from.x, 0.0, from.z);
            if (flat.length() > radius + living.getBbWidth() * 0.5) {
                continue;
            }
            Vec3 way = flat.lengthSqr() < 1.0E-4 ? this.ahead : flat.normalize();
            this.strike(level, living, damage, way.scale(out * knock).add(0.0, up * knock, 0.0));
        }
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), middle.add(0.0, 0.15, 0.0), 28, 0.6);
        ParticleFx.cloud(level, ParticleTypes.CLOUD, middle.add(0.0, 0.3, 0.0), 14, radius * 0.4, 0.1);
        this.sound(level, middle, sound, 1.0F, 1.0F);
        this.sound(level, middle, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 0.6F);
    }

    // The piston: a straight line out ahead at chest height, everything on it thrown hard away.
    private void piston(ServerLevel level) {
        Vec3 from = this.owner.position().add(0.0, this.owner.getBbHeight() * 0.6, 0.0);
        Vec3 to = from.add(this.ahead.scale(FistMoves.PISTON_REACH));
        double damage = this.ability.value("heavyDamage");
        double knock = this.ability.value("heavyKnockback");
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.0),
                living -> living != this.owner && living.isAlive() && PowerRing.canHit(this.owner, living))) {
            if (living.getBoundingBox().inflate(0.9).clip(from, to).isEmpty()
                    && !living.getBoundingBox().inflate(0.9).contains(from)) {
                continue;
            }
            this.strike(level, living, damage, this.ahead.scale(FistMoves.push(FistMoves.PISTON) * knock)
                    .add(0.0, 0.35, 0.0));
        }
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), to, 16, 0.5, 0.15);
        this.sound(level, to, SoundEvents.PISTON_EXTEND, 1.0F, 0.6F);
        this.sound(level, to, SoundEvents.MACE_SMASH_AIR, 1.0F, 0.8F);
    }

    // A hit as the owner's own (which knocks back by itself): its own push is set after it.
    private void strike(ServerLevel level, LivingEntity living, double damage, Vec3 push) {
        living.invulnerableTime = 0;
        if (!living.hurt(level.damageSources().playerAttack(this.owner), (float) damage)) {
            return;
        }
        double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        living.setDeltaMovement(push.scale(1.0 - resist));
        living.hasImpulse = true;
        living.hurtMarked = true;
    }

    // Where a heavy's giant fist meets the ground ahead of where the blow began.
    private Vec3 ground(double far) {
        return this.at.add(this.ahead.scale(far));
    }

    private void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
