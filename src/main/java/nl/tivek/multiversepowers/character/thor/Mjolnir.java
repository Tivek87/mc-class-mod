package nl.tivek.multiversepowers.character.thor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.spell.SpellTargets;

// Thor's hammer: at his belt, in his right hand, or thrown. Thrown, it flies at what he aims at, throws what it hits
// far away and flies home by itself. Thrown to fly after, it stops where it hits or at the end of its reach, hanging
// in the air, and his game flies him to it; there he catches it. In flight it is a ThrownHammer. Where it is on him
// and how far it reaches grow with his size.
final class Mjolnir {
    private static final double SPEED = 2.2;
    private static final double HOME_SPEED = 2.6;
    private static final double REACH = 40.0;
    private static final double CATCH = 1.8;
    private static final int WAITS = 100;
    private static final int LONGEST = 300;
    private static final double KNOCK = 3.2;
    private static final double KNOCK_UP = 0.7;
    private static final double UPPERCUT_REACH = 3.8;
    private static final double UPPERCUT_WIDTH = 0.45;
    private static final double LAUNCH = 1.6;
    private static final Map<UUID, Mjolnir> ALL = new HashMap<>();

    private enum State {
        HOME,
        OUT,
        WAITING,
        BACK
    }

    private final UUID owner;
    private boolean armed;
    private State state = State.HOME;
    @Nullable
    private ThrownHammer shown;
    private Vec3 at = Vec3.ZERO;
    private Vec3 way = Vec3.ZERO;
    private double flown;
    private double far;
    private boolean stay;
    private float damage;
    private int age;
    private int waited;
    private final Set<UUID> hit = new HashSet<>();

    private Mjolnir(UUID owner) {
        this.owner = owner;
    }

    private static Mjolnir of(ServerPlayer player) {
        return ALL.computeIfAbsent(player.getUUID(), Mjolnir::new);
    }

    // In his hand and not thrown.
    static boolean inHand(ServerPlayer player) {
        Mjolnir hammer = ALL.get(player.getUUID());
        return hammer != null && hammer.armed && hammer.state == State.HOME;
    }

    static int flags(ServerPlayer player) {
        Mjolnir hammer = ALL.get(player.getUUID());
        if (hammer == null) {
            return 0;
        }
        return (hammer.armed ? ThorStatePayload.ARMED : 0)
                | (hammer.state != State.HOME ? ThorStatePayload.THROWN : 0);
    }

    // Where the hammer's head is: thrown, held up over him as he flies, ahead of his right fist, or on his left hip.
    static Vec3 where(ServerPlayer player) {
        Mjolnir hammer = ALL.get(player.getUUID());
        if (hammer != null && hammer.state != State.HOME) {
            return hammer.at;
        }
        double size = player.getScale();
        Vec3 ahead = Vec3.directionFromRotation(0.0F, player.yBodyRot);
        Vec3 right = ahead.cross(Vectors.UP).normalize();
        if (ThorMoves.flying(player)) {
            return player.position().add(0.0, 2.3 * size, 0.0).add(ahead.scale(0.55 * size))
                    .add(right.scale(-0.3 * size));
        }
        if (hammer != null && hammer.armed) {
            return player.position().add(0.0, 0.75 * size, 0.0).add(ahead.scale(0.5 * size))
                    .add(right.scale(0.35 * size));
        }
        return player.position().add(0.0, 0.9 * size, 0.0).add(right.scale(-0.35 * size));
    }

    // Takes it from his belt into his hand, or hangs it back; never while it is thrown.
    static boolean toggle(ServerPlayer player) {
        Mjolnir hammer = of(player);
        if (hammer.state != State.HOME) {
            return false;
        }
        hammer.armed = !hammer.armed;
        ServerLevel level = player.serverLevel();
        double size = player.getScale();
        level.playSound(null, player.getX(), player.getY() + size, player.getZ(),
                hammer.armed ? SoundEvents.ARMOR_EQUIP_IRON.value() : SoundEvents.ARMOR_EQUIP_CHAIN.value(),
                SoundSource.PLAYERS, 1.0F, hammer.armed ? 0.8F : 1.1F);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, player.position().add(0.0, size, 0.0), 8, 0.3 * size,
                0.1);
        ThorMoves.tell(player, ThorStatePayload.NONE, 0);
        return true;
    }

    // Throws it along his look: at what he aims at, else as far as it reaches or up to the first block.
    static boolean fling(ServerPlayer player, float damage, boolean stay) {
        Mjolnir hammer = ALL.get(player.getUUID());
        if (hammer == null || !inHand(player)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        LivingEntity target = Targeting.aimLiving(player, level, REACH);
        double far = REACH;
        if (target != null) {
            far = Math.min(REACH, target.getBoundingBox().getCenter().distanceTo(eye) + 1.0);
        }
        ThrownHammer shown = spawn(level, player, eye.add(look.scale(0.6 * player.getScale())), look);
        hammer.shown = shown;
        hammer.state = State.OUT;
        hammer.at = shown.position();
        hammer.way = look;
        hammer.flown = 0.0;
        hammer.far = far;
        hammer.stay = stay;
        hammer.damage = damage;
        hammer.age = 0;
        hammer.waited = 0;
        hammer.hit.clear();
        level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.2F, 0.7F);
        level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4F, 1.8F);
        ThorMoves.tell(player, ThorStatePayload.NONE, 0);
        Effects.start(level, (lvl, age) -> hammer.tick(lvl));
        return true;
    }

    private static ThrownHammer spawn(ServerLevel level, ServerPlayer owner, Vec3 at, Vec3 way) {
        ThrownHammer shown = new ThrownHammer(ThrownHammer.TYPE.get(), level);
        shown.setSize(owner.getScale());
        shown.setCharged(ThorCharge.hammer(owner) > 1.0F);
        shown.moveTo(at.x, at.y, at.z, (float) Math.toDegrees(Math.atan2(-way.x, way.z)), headFirst(way));
        level.addFreshEntity(shown);
        return shown;
    }

    private boolean tick(ServerLevel level) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(this.owner);
        if (this.state == State.HOME || this.shown == null) {
            return false;
        }
        if (owner == null || owner.level() != level || !owner.isAlive() || this.shown.isRemoved()
                || ++this.age > LONGEST) {
            this.home(owner);
            return false;
        }
        this.shown.setCharged(ThorCharge.hammer(owner) > 1.0F);
        double size = owner.getScale();
        switch (this.state) {
            case OUT -> this.out(level, owner);
            case WAITING -> {
                ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, this.at, 2, 0.2, 0.1);
                if (owner.getBoundingBox().getCenter().distanceTo(this.at) < (CATCH + 0.8) * size) {
                    this.home(owner);
                    return false;
                }
                if (++this.waited > WAITS) {
                    this.state = State.BACK;
                }
            }
            case BACK -> {
                Vec3 hand = owner.getEyePosition().add(0.0, -0.5 * size, 0.0);
                Vec3 to = hand.subtract(this.at);
                double gap = to.length();
                if (gap < CATCH * size) {
                    this.home(owner);
                    return false;
                }
                this.way = to.scale(1.0 / gap);
                this.move(this.at.add(this.way.scale(Math.min(HOME_SPEED, gap))));
            }
            case HOME -> {
            }
        }
        this.trail(level, owner);
        return true;
    }

    // Flies on along its throw: stopped by the first block, hitting what it passes through.
    private void out(ServerLevel level, ServerPlayer owner) {
        double step = Math.min(SPEED, this.far - this.flown);
        Vec3 next = this.at.add(this.way.scale(step));
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(this.at, next, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, owner));
        boolean stopped = block.getType() != HitResult.Type.MISS;
        if (stopped) {
            next = block.getLocation().subtract(this.way.scale(0.3));
        }
        Vec3 from = this.at;
        Vec3 to = next;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.0),
                entity -> Targeting.mayStrike(owner, entity) && !this.hit.contains(entity.getUUID()))) {
            if (target.getBoundingBox().inflate(0.4).clip(from, to).isEmpty()
                    && !target.getBoundingBox().inflate(0.4).contains(to)) {
                continue;
            }
            this.hit.add(target.getUUID());
            this.strike(level, owner, target);
            stopped = true;
        }
        this.flown += step;
        this.move(next);
        if (stopped || this.flown >= this.far - 1.0E-3) {
            if (this.stay) {
                this.state = State.WAITING;
                ThorMoves.spare(owner, 120);
                ThorMoves.tell(owner, ThorStatePayload.PULL, this.shown == null ? 0 : this.shown.getId() + 1);
            } else {
                this.state = State.BACK;
            }
        }
    }

    // What it hits is thrown far the way it flew.
    private void strike(ServerLevel level, ServerPlayer owner, LivingEntity target) {
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().playerAttack(owner), this.damage);
        Vec3 flat = new Vec3(this.way.x, 0.0, this.way.z);
        Vec3 push = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        float more = ThorCharge.hammer(owner);
        SpellTargets.push(target, push, KNOCK * more, KNOCK_UP * more);
        Vec3 at = target.getBoundingBox().getCenter();
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 20, 0.4, 0.3);
        ParticleFx.cloud(level, ParticleTypes.CRIT, at, 12, 0.3, 0.3);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_AIR, SoundSource.PLAYERS, 1.2F, 0.8F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    private void move(Vec3 to) {
        this.at = to;
        if (this.shown != null) {
            float yaw = (float) Math.toDegrees(Math.atan2(-this.way.x, this.way.z));
            this.shown.moveTo(to.x, to.y, to.z, yaw, this.state == State.WAITING ? 0.0F : headFirst(this.way));
        }
    }

    // It never spins: its head leads the way it flies (0 stands it upright, 90 lays it level).
    private static float headFirst(Vec3 way) {
        return (float) (90.0 - Math.toDegrees(Math.atan2(way.y, Math.sqrt(way.x * way.x + way.z * way.z))));
    }

    private void trail(ServerLevel level, ServerPlayer owner) {
        if (this.state == State.WAITING) {
            return;
        }
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, this.at, 2, 0.15, 0.05);
        if (ThorCharge.hammer(owner) > 1.0F) {
            ParticleFx.at(level, ParticleFx.dust(ThorMoves.GLOW, 1.2F), this.at);
        }
    }

    // Back in his hand.
    private void home(@Nullable ServerPlayer owner) {
        if (this.shown != null) {
            this.shown.discard();
            this.shown = null;
        }
        this.state = State.HOME;
        if (owner != null) {
            ServerLevel level = owner.serverLevel();
            double size = owner.getScale();
            level.playSound(null, owner.getX(), owner.getY() + size, owner.getZ(), SoundEvents.TRIDENT_RETURN,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, owner.position().add(0.0, 1.2 * size, 0.0), 10,
                    0.3 * size, 0.1);
            ThorMoves.tell(owner, ThorStatePayload.NONE, 0);
        }
    }

    // The hammer's uppercut: it always finds what stands before him. A raised shield only takes the push; else it
    // flies high into the air.
    static boolean uppercut(ServerPlayer player, float damage) {
        if (!inHand(player)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        ThorMoves.tell(player, ThorStatePayload.BLOW, ThorBlow.HAMMER_UPPERCUT.ordinal());
        level.playSound(player, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.8F, 0.6F);
        UUID id = player.getUUID();
        int lands = ThorBlow.HAMMER_UPPERCUT.hit();
        Effects.start(level, (lvl, age) -> {
            ServerPlayer thor = lvl.getServer().getPlayerList().getPlayer(id);
            if (thor == null || thor.level() != lvl || !thor.isAlive()) {
                return false;
            }
            if (age < lands) {
                return true;
            }
            LivingEntity target = before(lvl, thor);
            if (target != null) {
                launch(lvl, thor, target, damage);
            }
            return false;
        });
        return true;
    }

    @Nullable
    private static LivingEntity before(ServerLevel level, ServerPlayer thor) {
        Vec3 eye = thor.getEyePosition();
        Vec3 ahead = new Vec3(thor.getLookAngle().x, 0.0, thor.getLookAngle().z).normalize();
        double reach = UPPERCUT_REACH * thor.getScale();
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                thor.getBoundingBox().inflate(reach), entity -> Targeting.mayStrike(thor, entity))) {
            Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            double far = to.length();
            if (far > reach + target.getBbWidth() * 0.5
                    || flat.lengthSqr() > 1.0E-4 && flat.normalize().dot(ahead) < UPPERCUT_WIDTH) {
                continue;
            }
            if (far < nearest) {
                nearest = far;
                best = target;
            }
        }
        return best;
    }

    private static void launch(ServerLevel level, ServerPlayer thor, LivingEntity target, float damage) {
        Vec3 away = target.position().subtract(thor.position());
        Vec3 way = away.horizontalDistanceSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0)
                : new Vec3(away.x, 0.0, away.z).normalize();
        Vec3 at = target.getBoundingBox().getCenter();
        float more = ThorCharge.hammer(thor);
        if (target.isBlocking()) {
            if (target instanceof Player blocker) {
                blocker.disableShield();
            }
            SpellTargets.push(target, way, 1.3 * more, 0.3);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
            return;
        }
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().playerAttack(thor), damage);
        target.setDeltaMovement(way.x * 0.25, LAUNCH * more, way.z * 0.25);
        target.hasImpulse = true;
        target.hurtMarked = true;
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 24, 0.4, 0.3);
        ParticleFx.cloud(level, ParticleTypes.CRIT, at, 14, 0.3, 0.4);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.0F, 1.1F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.5F);
    }

    static void leave(ServerPlayer player) {
        Mjolnir hammer = ALL.remove(player.getUUID());
        if (hammer != null && hammer.shown != null) {
            hammer.shown.discard();
            hammer.shown = null;
        }
        if (hammer != null) {
            hammer.state = State.HOME;
        }
    }

    static void clear() {
        for (Mjolnir hammer : ALL.values()) {
            if (hammer.shown != null) {
                hammer.shown.discard();
            }
        }
        ALL.clear();
    }
}
