package nl.tivek.welcomescreen.character.lantern;

import java.util.HashMap;
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
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;
import nl.tivek.welcomescreen.spell.SpellFx;

/**
 * Flight: Green Lantern brings his fists to his chest, throws his arms down along his sides and rises into the
 * air, and from there flies wherever he looks. His own game moves him (every client moves its own player); the
 * server makes the ring pay for every tick of it, keeps him from being hurt by a fall he never makes, and lets
 * his shield ram whatever he flies into.
 *
 * <p>A full ring keeps him up for a set number of seconds; what he shoots or holds up meanwhile costs on top.
 * If the ring runs dry up there, its last light lets him sink down gently, and he can fly again once he has
 * recharged it on the ground.
 */
public final class Flight implements SpellEffect {
    /** Ticks the take-off lasts: fists to the chest, arms down along the sides, and up. Clients play it too. */
    public static final int ARISE_TICKS = 24;
    // Below this speed, in blocks per tick, flying into a creature with the shield up does not ram it.
    private static final double RAM_SPEED = 0.4;
    // Ticks before the same creature can be rammed again.
    private static final int RAM_AGAIN = 12;
    // A descent that somehow never touches ground ends by itself after this many ticks.
    private static final int DESCENT_MAX = 2400;
    // Ticks without any movement from his game before he counts as standing still in the air.
    private static final int STILL_TICKS = 2;

    private static final Map<UUID, Flight> FLYING = new HashMap<>();

    private final ServerPlayer owner;
    private final float perTick;
    private int ticks;
    private boolean descending;
    private int descentTicks;
    // How far he moved on the last tick, as his own game told it: the speed his shots take along.
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 lastPos;
    private int still;
    // Entity id -> the game time it may be rammed again.
    private final Map<Integer, Long> rammed = new HashMap<>();

    private Flight(ServerPlayer owner, CharacterAbility ability) {
        this.owner = owner;
        this.perTick = (float) (PowerRing.MAX_POWER / (ability.value("fullRingSeconds") * 20.0));
        this.lastPos = owner.position();
    }

    /**
     * The flight key: takes off, or lands again.
     *
     * @return true when he landed, so the key's cooldown starts; taking off starts none
     */
    static boolean toggle(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        Flight flight = FLYING.get(owner.getUUID());
        if (flight != null) {
            // An empty ring is already letting him down; the key cannot hurry that.
            if (!flight.descending) {
                flight.land(level);
                return true;
            }
            return false;
        }
        if (Lantern.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        if (owner.isPassenger() || owner.isSleeping() || owner.isFallFlying()) {
            return false;
        }
        if (PowerRing.power(owner) + 1.0E-4F < (float) ability.value("powerCost")) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        flight = new Flight(owner, ability);
        FLYING.put(owner.getUUID(), flight);
        SpellCasting.start(level, flight);
        owner.resetFallDistance();
        flight.sound(level, SoundEvents.BEACON_POWER_SELECT, 0.9F, 1.5F);
        flight.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 0.8F);
        PowerRing.sync(owner);
        return false;
    }

    /** True while this player flies (taking off included), and not while an empty ring lets him down. */
    public static boolean flying(ServerPlayer player) {
        Flight flight = FLYING.get(player.getUUID());
        return flight != null && !flight.descending;
    }

    /** True while an empty ring lets this player sink down to the ground. */
    static boolean descending(ServerPlayer player) {
        Flight flight = FLYING.get(player.getUUID());
        return flight != null && flight.descending;
    }

    /** How many ticks ago this player took off, or -1 when he is not in the air on his ring. */
    static int ticks(ServerPlayer player) {
        Flight flight = FLYING.get(player.getUUID());
        return flight == null ? -1 : flight.ticks;
    }

    /**
     * How fast this player flies, in blocks per tick, as his own game moved him on the last tick; zero when he
     * is not flying. His shots and his fist take this along, so he never overtakes them.
     */
    static Vec3 velocity(ServerPlayer player) {
        Flight flight = FLYING.get(player.getUUID());
        return flight == null || flight.descending ? Vec3.ZERO : flight.velocity;
    }

    /** The way the ram cone points: the way he flies, or the way he looks while he hardly moves. */
    static Vec3 heading(ServerPlayer player) {
        Vec3 moving = velocity(player);
        return moving.lengthSqr() > RAM_SPEED * RAM_SPEED * 0.25 ? moving.normalize() : player.getLookAngle();
    }

    /** The server stops: nobody flies any more. */
    static void clear() {
        FLYING.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (FLYING.get(this.owner.getUUID()) != this) {
            return false;
        }
        // No longer Green Lantern, gone, or somewhere a ring cannot carry him: the flight is over at once.
        if (!PowerRing.fuels(this.owner, level) || this.owner.isPassenger() || this.owner.isSleeping()
                || this.owner.isSpectator()) {
            this.end();
            return false;
        }
        this.ticks++;
        this.track();
        // His own game keeps him up; the server must neither count a fall nor think he hangs in the air unlawfully.
        this.owner.resetFallDistance();
        this.owner.connection.aboveGroundTickCount = 0;
        if (this.descending) {
            this.descentTicks++;
            if (this.owner.onGround() || this.owner.isInWater() || this.descentTicks > DESCENT_MAX) {
                this.end();
                return false;
            }
            if (this.descentTicks % 4 == 0) {
                SpellFx.cloud(level, SpellFx.dust(PowerRing.GREEN, 0.8F), this.owner.position().add(0.0, 1.0, 0.0),
                        2, 0.3, 0.0);
            }
            return true;
        }
        float power = PowerRing.power(this.owner);
        if (power <= 1.0E-4F) {
            this.descend(level);
            return true;
        }
        PowerRing.setPower(this.owner, power - this.perTick);
        this.ram(level);
        return true;
    }

    /** How far he moved on this tick: what his own game sent, or nothing once he has stood still a moment. */
    private void track() {
        Vec3 now = this.owner.position();
        if (now.distanceToSqr(this.lastPos) < 1.0E-8) {
            this.still++;
        } else {
            this.still = 0;
        }
        this.lastPos = now;
        this.velocity = this.still >= STILL_TICKS ? Vec3.ZERO : this.owner.getKnownMovement();
    }

    /**
     * With the shield up, the shield becomes a pointed cone in front of him, and whatever he flies into is rammed
     * away: a heavy hit and a long throw, both bigger the faster he flies.
     */
    private void ram(ServerLevel level) {
        double speed = this.velocity.length();
        CharacterAbility shield = GameCharacter.GREEN_LANTERN.byName("light_shield");
        if (speed < RAM_SPEED || shield == null || !LightShield.up(this.owner)) {
            return;
        }
        Vec3 way = this.velocity.scale(1.0 / speed);
        AABB body = this.owner.getBoundingBox();
        // The cone reaches out ahead of him, and this tick he also went through everything behind its tip.
        AABB reach = body.expandTowards(way.scale(1.8)).expandTowards(this.velocity.scale(-1.0)).inflate(0.35);
        long now = level.getGameTime();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, reach,
                entity -> PowerRing.canHit(this.owner, entity))) {
            Vec3 to = target.getBoundingBox().getCenter().subtract(body.getCenter());
            Long again = this.rammed.get(target.getId());
            if (to.dot(way) < -0.4 || again != null && now < again) {
                continue;
            }
            this.rammed.put(target.getId(), now + RAM_AGAIN);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner),
                    (float) (shield.value("ramDamage") + shield.value("ramDamagePerSpeed") * speed));
            double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            double push = shield.value("ramKnockback") + speed * 0.8;
            target.setDeltaMovement(new Vec3(way.x * push, Math.max(0.35, way.y * push + 0.35), way.z * push)
                    .scale(1.0 - resist));
            target.hasImpulse = true;
            target.hurtMarked = true;
            Vec3 at = target.getBoundingBox().getCenter();
            SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 1.3F), at, 20, 0.3);
            SpellFx.cloud(level, ParticleTypes.CRIT, at, 12, 0.3, 0.35);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, 1.2F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F,
                    0.8F);
            LightShield.flash(this.owner);
        }
        this.rammed.values().removeIf(time -> time < now);
    }

    /** The ring has nothing left: its last light lets him sink down gently, and he steers no more. */
    private void descend(ServerLevel level) {
        this.descending = true;
        PowerRing.tell(this.owner, "flight_empty");
        this.sound(level, SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.3F);
        LightShield.stop(this.owner);
        PowerRing.sync(this.owner);
    }

    /** He lands, or turns off his flight in the air and falls from there. */
    private void land(ServerLevel level) {
        this.sound(level, SoundEvents.BEACON_DEACTIVATE, 0.6F, 1.6F);
        this.end();
    }

    private void end() {
        FLYING.remove(this.owner.getUUID(), this);
        this.owner.resetFallDistance();
        PowerRing.sync(this.owner);
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, this.owner.getX(), this.owner.getY() + 1.0, this.owner.getZ(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }
}
