package nl.tivek.multiversepowers.character.greenlantern.ability;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.ChunkPreloader;

public final class Flight implements Effect {
    public static final int ARISE_TICKS = 24;
    // Parts of the top speed, so ramming and scraping keep working whatever the top speed is set to.
    private static final double RAM_PART = 0.49;
    public static final double SCRAPE_PART = 0.245;
    private static final double RAM_PUSH = 0.36;
    private static final int RAM_AGAIN = 12;
    private static final int DESCENT_MAX = 2400;
    private static final int DIVE_MAX = 300;
    private static final int STILL_TICKS = 2;
    private static final double PEAK_FADE = 0.85;
    private static final double SLAM_CHECK = 0.7;

    private static final Map<UUID, Flight> FLYING = new HashMap<>();

    private final ServerPlayer owner;
    private final CharacterAbility ability;
    private final float perTick;
    private int ticks;
    private boolean descending;
    private int descentTicks;
    private boolean dive;
    private int diveTicks;
    private Vec3 velocity = Vec3.ZERO;
    private double peak;
    private Vec3 lastPos;
    private int still;
    private final Map<Integer, Long> rammed = new HashMap<>();

    private Flight(ServerPlayer owner, CharacterAbility ability) {
        this.owner = owner;
        this.ability = ability;
        this.perTick = (float) (PowerRing.MAX_POWER / (ability.value("fullRingSeconds") * 20.0));
        this.lastPos = owner.position();
    }

    public static boolean toggle(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        Flight flight = FLYING.get(owner.getUUID());
        if (flight != null) {
            if (!flight.descending) {
                flight.land(level);
                return true;
            }
            return false;
        }
        if (Recharge.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        if (owner.isPassenger() || owner.isSleeping() || owner.isFallFlying() || HeldMobs.isHeldByAnyone(owner)) {
            return false;
        }
        if (PowerRing.power(owner) + 1.0E-4F < (float) ability.value("powerCost")) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        flight = new Flight(owner, ability);
        FLYING.put(owner.getUUID(), flight);
        Effects.start(level, flight);
        owner.resetFallDistance();
        flight.sound(level, SoundEvents.BEACON_POWER_SELECT, 0.9F, 1.5F);
        flight.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 0.8F);
        PowerRing.sync(owner);
        return false;
    }

    public static boolean flying(ServerPlayer player) {
        Flight flight = FLYING.get(player.getUUID());
        return flight != null && !flight.descending;
    }

    public static boolean descending(ServerPlayer player) {
        Flight flight = FLYING.get(player.getUUID());
        return flight != null && flight.descending;
    }

    static boolean dive(ServerPlayer owner, ServerLevel level) {
        Flight flight = FLYING.get(owner.getUUID());
        if (flight == null || flight.descending || flight.ticks < ARISE_TICKS || flight.dive) {
            return false;
        }
        flight.dive = true;
        flight.diveTicks = 0;
        flight.sound(level, SoundEvents.MACE_SMASH_AIR, 1.0F, 0.8F);
        flight.sound(level, SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.6F);
        PowerRing.sync(owner);
        return true;
    }

    public static boolean diving(ServerPlayer player) {
        Flight flight = FLYING.get(player.getUUID());
        return flight != null && flight.dive;
    }

    public static int ticks(ServerPlayer player) {
        Flight flight = FLYING.get(player.getUUID());
        return flight == null ? -1 : flight.ticks;
    }

    static Vec3 velocity(ServerPlayer player) {
        Flight flight = FLYING.get(player.getUUID());
        return flight == null || flight.descending ? Vec3.ZERO : flight.velocity;
    }

    static Vec3 heading(ServerPlayer player) {
        Flight flight = FLYING.get(player.getUUID());
        Vec3 moving = velocity(player);
        double slowest = flight == null ? 0.0 : RAM_PART * flight.top() * 0.5;
        return flight != null && moving.lengthSqr() > slowest * slowest ? moving.normalize() : player.getLookAngle();
    }

    private double top() {
        return this.ability.value("topSpeed") / 20.0;
    }

    static void recharged(ServerPlayer player, ServerLevel level) {
        Flight flight = FLYING.get(player.getUUID());
        if (flight == null || !flight.descending) {
            return;
        }
        flight.descending = false;
        flight.descentTicks = 0;
        flight.sound(level, SoundEvents.BEACON_POWER_SELECT, 0.9F, 1.5F);
        PowerRing.sync(player);
    }

    public static void clear() {
        FLYING.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (FLYING.get(this.owner.getUUID()) != this) {
            return false;
        }
        if (!PowerRing.fuels(this.owner, level) || this.owner.isPassenger() || this.owner.isSleeping()
                || this.owner.isSpectator() || HeldMobs.isHeldByAnyone(this.owner)) {
            this.end();
            return false;
        }
        this.ticks++;
        this.track();
        // The server's own motion of a flyer piles up gravity; a hit sends it to his game, which then drops him.
        if (!this.owner.hurtMarked) {
            this.owner.setDeltaMovement(this.velocity);
        }
        // He flies faster than the game makes new land by itself: the world ahead is made ready before he gets there.
        if (this.ticks % ChunkPreloader.EVERY_TICKS == 1) {
            ChunkPreloader.keep(this.owner, this.velocity, this.ability.value("chunkRadiusBlocks"),
                    this.ability.value("chunkAheadSeconds"));
        }
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
                ParticleFx.cloud(level, ParticleFx.dust(PowerRing.GREEN, 0.8F),
                        this.owner.position().add(0.0, 1.0, 0.0), 2, 0.3, 0.0);
            }
            return true;
        }
        float power = PowerRing.power(this.owner);
        if (power <= 1.0E-4F) {
            this.descend(level);
            return true;
        }
        if (this.dive && ++this.diveTicks > DIVE_MAX) {
            this.dive = false;
            PowerRing.sync(this.owner);
        }
        PowerRing.setPower(this.owner, power - this.perTick);
        this.ram(level);
        this.scrape(level);
        return true;
    }

    public static boolean scraping(Entity flyer, double height) {
        AABB below = flyer.getBoundingBox().expandTowards(0.0, -height, 0.0);
        return flyer.level().getBlockCollisions(flyer, below).iterator().hasNext();
    }

    private void scrape(ServerLevel level) {
        CharacterAbility shield = GameCharacter.GREEN_LANTERN.byName("light_shield");
        if (shield == null || this.velocity.length() < SCRAPE_PART * this.top() || !LightShield.up(this.owner)
                || !scraping(this.owner, shield.value("ramGroundBlocks"))) {
            return;
        }
        float cost = (float) (shield.value("ramGroundPowerPerSecond") / 20.0);
        PowerRing.setPower(this.owner, Math.max(0.0F, PowerRing.power(this.owner) - cost));
        if (this.ticks % 2 == 0) {
            Vec3 at = this.owner.position();
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), at, 4, 0.4, 0.05);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.1, at.z, 6, 0.4, 0.05, 0.4, 0.2);
        }
        if (this.ticks % 6 == 0) {
            this.sound(level, SoundEvents.GRINDSTONE_USE, 0.5F, 1.4F);
        }
    }

    private void track() {
        Vec3 now = this.owner.position();
        if (now.distanceToSqr(this.lastPos) < 1.0E-8) {
            this.still++;
        } else {
            this.still = 0;
        }
        this.lastPos = now;
        this.velocity = this.still >= STILL_TICKS ? Vec3.ZERO : this.owner.getKnownMovement();
        this.peak = Math.max(this.velocity.length(), this.peak * PEAK_FADE);
    }

    public static boolean slam(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        Flight flight = FLYING.get(owner.getUUID());
        if (flight == null || flight.descending || flight.ticks < ARISE_TICKS) {
            return false;
        }
        boolean fast = flight.dive || flight.peak >= flight.top() * SLAM_CHECK;
        flight.end();
        owner.setDeltaMovement(Vec3.ZERO);
        owner.hurtMarked = true;
        CharacterAbility shockwave = GameCharacter.GREEN_LANTERN.byName("shockwave");
        if (!fast || shockwave == null || !LandingSlam.start(owner, level, shockwave)) {
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.MACE_SMASH_GROUND,
                    SoundSource.PLAYERS, 1.0F, 1.1F);
        }
        PowerRing.sync(owner);
        return true;
    }

    private void ram(ServerLevel level) {
        double speed = this.velocity.length();
        CharacterAbility shield = GameCharacter.GREEN_LANTERN.byName("light_shield");
        if (speed < RAM_PART * this.top() || shield == null || !LightShield.up(this.owner)) {
            return;
        }
        Vec3 way = this.velocity.scale(1.0 / speed);
        AABB body = this.owner.getBoundingBox();
        // Reaches out ahead of him, and back over where he came from this tick, so fast flight never tunnels past a hit.
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
            double push = shield.value("ramKnockback") + RAM_PUSH * speed / this.top();
            target.setDeltaMovement(new Vec3(way.x * push, Math.max(0.35, way.y * push + 0.35), way.z * push)
                    .scale(1.0 - resist));
            target.hasImpulse = true;
            target.hurtMarked = true;
            Vec3 at = target.getBoundingBox().getCenter();
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), at, 20, 0.3);
            ParticleFx.cloud(level, ParticleTypes.CRIT, at, 12, 0.3, 0.35);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, 1.2F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F,
                    0.8F);
            LightShield.flash(this.owner);
        }
        this.rammed.values().removeIf(time -> time < now);
    }

    private void descend(ServerLevel level) {
        this.descending = true;
        this.dive = false;
        PowerRing.tell(this.owner, "flight_empty");
        this.sound(level, SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.3F);
        LightShield.stop(this.owner);
        PowerRing.sync(this.owner);
    }

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
