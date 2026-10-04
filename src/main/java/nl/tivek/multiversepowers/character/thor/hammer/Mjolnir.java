package nl.tivek.multiversepowers.character.thor.hammer;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.thor.ThorBlow;
import nl.tivek.multiversepowers.character.thor.ThorCharge;
import nl.tivek.multiversepowers.character.thor.ThorMoves;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.spell.SpellTargets;

// Thor's hammer: on his belt, in his right hand, or out of his hands as a ThrownHammer. Thrown it flies at what he
// aims at and comes back by itself (Hammer Throw), stays where it stops (Throw to Stay), is dashed after (Throw and
// Follow, HammerPull) or, hurled from the sky, strikes a ring of chained lightning (Storm Throw, StormThrow). Wherever
// it rests he can call it back or dash to it, and he flies only with it. Its uppercut is here too. Where it is on him
// and how far it reaches grow with his size.
public final class Mjolnir extends MjolnirCatch {
    private static final double UPPERCUT_REACH = 3.8;
    private static final double UPPERCUT_WIDTH = 0.45;
    private static final double LAUNCH = 1.6;
    private static final double STORM_REACH = 24.0;

    private Mjolnir(UUID owner) {
        super(owner);
    }

    private static Mjolnir of(ServerPlayer player) {
        return ALL.computeIfAbsent(player.getUUID(), Mjolnir::new);
    }

    @Nullable
    private static Mjolnir find(ServerPlayer player) {
        return ALL.get(player.getUUID());
    }

    // In his right hand.
    public static boolean inHand(ServerPlayer player) {
        Mjolnir hammer = find(player);
        return hammer != null && hammer.armed && hammer.state == State.HOME;
    }

    // On him: in a hand or on his belt.
    public static boolean home(ServerPlayer player) {
        Mjolnir hammer = find(player);
        return hammer == null || hammer.state == State.HOME;
    }

    public static boolean resting(ServerPlayer player) {
        Mjolnir hammer = find(player);
        return hammer != null && hammer.state == State.RESTING;
    }

    // Still on its way out to where it will rest, or in his hand about to be let go.
    public static boolean goingOut(ServerPlayer player) {
        Mjolnir hammer = find(player);
        return hammer != null && hammer.state == State.OUT;
    }

    public static int flags(ServerPlayer player) {
        Mjolnir hammer = find(player);
        if (hammer == null) {
            return 0;
        }
        int flags = hammer.armed ? ThorStatePayload.ARMED : 0;
        flags |= hammer.state != State.HOME ? ThorStatePayload.THROWN : 0;
        flags |= hammer.state == State.RESTING ? ThorStatePayload.RESTING : 0;
        flags |= hammer.state == State.BACK ? ThorStatePayload.CALLING : 0;
        flags |= hammer.cocked ? ThorStatePayload.COCKED : 0;
        return flags;
    }

    // Where the hammer's head is: out of his hands, held up over him as he flies, ahead of his right fist, or on his
    // left hip.
    public static Vec3 where(ServerPlayer player) {
        Mjolnir hammer = find(player);
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

    // Takes it from his belt into his hand, or hangs it back; never while it is out.
    public static boolean toggle(ServerPlayer player) {
        Mjolnir hammer = of(player);
        if (hammer.state != State.HOME) {
            return false;
        }
        hammer.armed = !hammer.armed;
        hammer.cocked = false;
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

    // Hammer Throw (it comes back by itself) or, crouched, Throw to Stay (it rests where it stops), at most `reach`
    // blocks.
    public static boolean fling(ServerPlayer player, float damage, boolean stay, double reach) {
        if (!inHand(player)) {
            return false;
        }
        Mjolnir hammer = of(player);
        hammer.launch(player, stay ? Throw.STAY : Throw.RETURN, damage, reach);
        hammer.backDamage = stay ? 0.0F : callDamage(player);
        ThorMoves.tell(player, ThorStatePayload.BLOW, ThorBlow.HAMMER_THROW.ordinal());
        return true;
    }

    // Throw and Follow's draw: he draws it back over his shoulder, sparks crawling over it.
    public static boolean draw(ServerPlayer player) {
        if (!inHand(player)) {
            return false;
        }
        Mjolnir hammer = of(player);
        hammer.cocked = true;
        Vec3 at = where(player);
        ServerLevel level = player.serverLevel();
        level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.5F,
                1.6F);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 8, 0.15, 0.1);
        ThorMoves.tell(player, ThorStatePayload.NONE, 0);
        return true;
    }

    // Let go of the draw: thrown `far` blocks (as far as it was drawn) to rest where it stops, and he dashes after it
    // at `pace` blocks a tick.
    public static boolean leap(ServerPlayer player, float damage, double far, double pace) {
        Mjolnir hammer = find(player);
        if (hammer == null || !hammer.cocked || !inHand(player)) {
            uncock(player);
            return false;
        }
        hammer.launch(player, Throw.FOLLOW, damage, far);
        hammer.backDamage = 0.0F;
        ThorMoves.tell(player, ThorStatePayload.BLOW, ThorBlow.HAMMER_THROW.ordinal());
        HammerPull.start(player, pace);
        return true;
    }

    // A draw that ends without a throw (knocked down, the hammer put away).
    public static void uncock(ServerPlayer player) {
        Mjolnir hammer = find(player);
        if (hammer != null && hammer.cocked) {
            hammer.cocked = false;
            ThorMoves.tell(player, ThorStatePayload.NONE, 0);
        }
    }

    // Storm Throw: hurled from his left hand in flight at what he aims at; it strikes its ring and comes back.
    public static boolean storm(ServerPlayer player, float damage) {
        Mjolnir hammer = of(player);
        if (hammer.state != State.HOME || !ThorMoves.flying(player)) {
            return false;
        }
        hammer.launch(player, Throw.STORM, damage, STORM_REACH);
        hammer.backDamage = callDamage(player);
        ThorMoves.tell(player, ThorStatePayload.BLOW, ThorBlow.STORM_THROW.ordinal());
        return true;
    }

    // Call the Hammer back from wherever it is, hitting what is in its way; `toFly`: into his raised left hand, taking
    // off as he catches it.
    public static boolean call(ServerPlayer player, float damage, boolean toFly) {
        Mjolnir hammer = find(player);
        if (hammer == null || hammer.state == State.HOME || HammerPull.pulling(player)) {
            return false;
        }
        hammer.callBack(player.serverLevel(), player, damage, toFly);
        return true;
    }

    // Follow the Hammer: he dashes to it where it rests, if it is within `reach` and not in lava.
    public static boolean follow(ServerPlayer player, double reach, double pace) {
        Mjolnir hammer = find(player);
        if (hammer == null || hammer.state != State.RESTING || player.position().distanceTo(hammer.at) > reach
                || player.level().getFluidState(BlockPos.containing(hammer.at)).is(FluidTags.LAVA)) {
            return false;
        }
        return HammerPull.start(player, pace);
    }

    // Where it rests or flies, for the pull; null while it is on him.
    @Nullable
    static Vec3 out(ServerPlayer player) {
        Mjolnir hammer = find(player);
        return hammer == null || hammer.state == State.HOME ? null : hammer.at;
    }

    // The pull got him there: on the ground by it he takes it in his right hand, in the air in his left and flies on.
    static void reached(ServerPlayer player, boolean land) {
        Mjolnir hammer = find(player);
        if (hammer == null || hammer.state != State.RESTING) {
            return;
        }
        if (hammer.stuck()) {
            hammer.dust(player.serverLevel(), 16);
        }
        hammer.home(player, land ? Hand.RIGHT : Hand.LEFT, true);
    }

    @Override
    double stays() {
        return setting("hammer_throw", "stayBlocks", 128.0);
    }

    private static double setting(String ability, String key, double fallback) {
        CharacterAbility found = GameCharacter.THOR.byName(ability);
        return found == null ? fallback : found.value(key);
    }

    // What the hammer flying back by itself does to each creature it passes: Call the Hammer's damage, charged or not.
    private static float callDamage(ServerPlayer player) {
        CharacterAbility call = GameCharacter.THOR.byName("hammer_call");
        return call == null ? 0.0F : call.getDamage() * ThorCharge.hammer(player);
    }

    // The hammer's uppercut: it always finds what stands before him. A raised shield only takes the push; else it
    // flies high into the air.
    public static boolean uppercut(ServerPlayer player, float damage) {
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

    // He is no longer Thor: wherever the hammer is, it is gone; the next time he has it on him again.
    public static void leave(ServerPlayer player) {
        Mjolnir hammer = ALL.remove(player.getUUID());
        if (hammer != null && hammer.shown != null) {
            hammer.shown.discard();
            hammer.shown = null;
        }
        if (hammer != null) {
            hammer.state = State.HOME;
        }
    }

    public static void clear() {
        for (Mjolnir hammer : ALL.values()) {
            if (hammer.shown != null) {
                hammer.shown.discard();
            }
        }
        ALL.clear();
    }
}
