package nl.tivek.multiversepowers.testfight;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.entity.PlayerKnockdowns;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;

// A filmed test fight, to see how bodies move: a host or an operator who crouches and holds the scroll wheel on a
// creature built as a person squares up to it, and the two trade a set exchange in place (its hook ducked, a left to
// the body, an uppercut, its jab parried, a cross) that ends in a roundhouse kick throwing it limp. Here the creature is
// held where it stands, each blow lands on time and the kick throws it; the moves are drawn by every game near
// (client/FightPoses), on the same clock.
public final class TestFight {
    // How far apart they stand (blocks), how far off a creature may be asked for, and how far the player may be pushed
    // from where they began before the fight stops.
    public static final double GAP = 1.15;
    public static final double REACH = 6.0;
    private static final double ROAM = 2.0;
    public static final int LENGTH = 150;
    // When its hook passes over the player's head, and when each of their blows lands: the left to the body, the
    // uppercut, the right hand slapping its jab aside, the cross, and the kick that throws it.
    public static final int HOOK = 25;
    public static final int BODY = 32;
    public static final int UPPERCUT = 42;
    public static final int PARRY = 58;
    public static final int CROSS = 64;
    public static final int KICK = 94;

    private static final Map<UUID, TestFight> FIGHTS = new HashMap<>();

    private final UUID owner;
    private final Mob target;
    private final Vec3 start;
    private final Vec3 way;
    private final float facing;
    private Vec3 base;
    private Vec3 spot;
    private boolean held = true;
    private boolean done;

    private TestFight(UUID owner, Mob target, Vec3 start, Vec3 spot, Vec3 way) {
        this.owner = owner;
        this.target = target;
        this.start = start;
        this.base = start;
        this.spot = spot;
        this.way = way;
        this.facing = (float) Math.toDegrees(Math.atan2(way.x, -way.z));
    }

    // Who may start one: the host of a world played alone or opened to others, or an operator.
    public static boolean allowed(ServerPlayer player) {
        return player.server.isSingleplayerOwner(player.getGameProfile()) || player.hasPermissions(2);
    }

    public static void request(ServerPlayer player, int id) {
        ServerLevel level = player.serverLevel();
        if (!allowed(player) || FIGHTS.containsKey(player.getUUID()) || !player.isAlive() || player.isSpectator()
                || PlayerKnockdowns.isDown(player) || player.isPassenger() || player.getAbilities().flying
                || player.isFallFlying() || player.isInWater()) {
            return;
        }
        if (!(level.getEntity(id) instanceof Mob mob) || mob.isBaby() || mob.isPassenger() || mob.isVehicle()
                || !Targeting.isTargetable(player, mob) || mob.distanceTo(player) > REACH + 1.0) {
            return;
        }
        Vec3 from = player.position();
        Vec3 flat = new Vec3(mob.getX() - from.x, 0.0, mob.getZ() - from.z);
        Vec3 way = flat.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, player.getYRot())
                : flat.normalize();
        Vec3 spot = place(level, mob, from.add(way.scale(GAP)));
        if (spot == null || !HeldMobs.hold(mob)) {
            return;
        }
        TestFight fight = new TestFight(player.getUUID(), mob, from, spot, way);
        FIGHTS.put(player.getUUID(), fight);
        fight.stand();
        fight.tell(player, TestFightPayload.START);
        Effects.start(level, fight::tick);
    }

    // Where it stands to fight: level with the player, or on the ground right under or over that spot; null when there
    // is no room for it there.
    @Nullable
    private static Vec3 place(ServerLevel level, Mob mob, Vec3 spot) {
        for (double rise : new double[] { 0.0, 0.5, -0.5, 1.0, -1.0 }) {
            Vec3 at = spot.add(0.0, rise, 0.0);
            if (level.noCollision(mob, mob.getDimensions(mob.getPose()).makeBoundingBox(at))
                    && !level.noCollision(mob, mob.getDimensions(mob.getPose()).makeBoundingBox(at.add(0.0, -0.1,
                            0.0)))) {
                return at;
            }
        }
        return null;
    }

    private boolean tick(ServerLevel level, int age) {
        if (this.done) {
            return false;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.owner);
        if (player == null || !player.isAlive() || player.level() != level || PlayerKnockdowns.isDown(player)
                || !this.target.isAlive() || this.target.isRemoved() || this.target.level() != level
                || player.position().distanceToSqr(this.start) > ROAM * ROAM) {
            this.end(level, player);
            return false;
        }
        if (this.held) {
            // Nudged by a blow from elsewhere, the player keeps the creature where their blows land.
            if (player.position().distanceToSqr(this.base) > 1.0E-4) {
                Vec3 next = place(level, this.target, player.position().add(this.way.scale(GAP)));
                if (next == null) {
                    this.end(level, player);
                    return false;
                }
                this.base = player.position();
                this.spot = next;
            }
            this.stand();
        }
        Vec3 face = this.spot.add(0.0, this.target.getBbHeight() * 0.85, 0.0).subtract(this.way.scale(0.3));
        Vec3 ribs = this.spot.add(0.0, this.target.getBbHeight() * 0.55, 0.0).subtract(this.way.scale(0.3));
        switch (age) {
            case HOOK -> sound(level, player.position().add(0.0, 1.6, 0.0), SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F,
                    1.4F);
            case BODY -> this.blow(level, player, ribs, 2.0F, SoundEvents.PLAYER_ATTACK_STRONG, 0.9F, 6);
            case UPPERCUT -> this.blow(level, player, face, 3.0F, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 12);
            case PARRY -> sound(level, face.add(this.way.scale(-0.6)), SoundEvents.PLAYER_ATTACK_WEAK, 1.0F, 1.5F);
            case CROSS -> this.blow(level, player, face, 2.0F, SoundEvents.PLAYER_ATTACK_STRONG, 1.1F, 8);
            case KICK -> this.kick(level, player, ribs);
            default -> {
            }
        }
        if (age >= LENGTH) {
            this.end(level, player);
            return false;
        }
        return true;
    }

    // Held where it fights, facing the player.
    private void stand() {
        this.target.setPos(this.spot.x, this.spot.y, this.spot.z);
        this.target.setDeltaMovement(Vec3.ZERO);
        this.target.setYRot(this.facing);
        this.target.setYHeadRot(this.facing);
        this.target.setYBodyRot(this.facing);
        this.target.setXRot(0.0F);
    }

    private void blow(ServerLevel level, ServerPlayer player, Vec3 at, float damage, SoundEvent sound, float pitch,
            int sparks) {
        this.hurt(level, player, damage);
        ParticleFx.cloud(level, ParticleTypes.CRIT, at, sparks, 0.15, 0.25);
        sound(level, at, sound, 1.0F, pitch);
    }

    // The kick: let go, struck and thrown away from the player, limp.
    private void kick(ServerLevel level, ServerPlayer player, Vec3 ribs) {
        this.tell(player, TestFightPayload.LET_GO);
        this.let();
        this.hurt(level, player, 4.0F);
        this.target.setDeltaMovement(this.way.x * 0.7, 0.45, this.way.z * 0.7);
        this.target.hasImpulse = true;
        this.target.hurtMarked = true;
        ParticleFx.cloud(level, ParticleTypes.SWEEP_ATTACK, ribs, 1, 0.0, 0.0);
        ParticleFx.cloud(level, ParticleTypes.CRIT, ribs, 18, 0.2, 0.35);
        ParticleFx.cloud(level, ParticleTypes.DAMAGE_INDICATOR, ribs, 4, 0.2, 0.1);
        sound(level, ribs, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.8F);
        sound(level, ribs, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.7F);
    }

    private void hurt(ServerLevel level, ServerPlayer player, float damage) {
        this.target.invulnerableTime = 0;
        this.target.hurt(level.damageSources().playerAttack(player), damage);
        if (this.held) {
            // Held, it takes the blow where it stands; the hit's own knockback would push it off.
            this.target.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void let() {
        if (this.held) {
            this.held = false;
            HeldMobs.release(this.target);
        }
    }

    private void end(ServerLevel level, @Nullable ServerPlayer player) {
        if (this.done) {
            return;
        }
        this.done = true;
        FIGHTS.remove(this.owner, this);
        this.let();
        this.tell(player, TestFightPayload.END);
    }

    private void tell(@Nullable ServerPlayer player, int phase) {
        if (player == null) {
            PacketDistributor.sendToPlayersTrackingEntity(this.target, new TestFightPayload(-1, this.target.getId(),
                    phase));
        } else {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new TestFightPayload(player.getId(),
                    this.target.getId(), phase));
        }
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void clear() {
        FIGHTS.clear();
    }
}
