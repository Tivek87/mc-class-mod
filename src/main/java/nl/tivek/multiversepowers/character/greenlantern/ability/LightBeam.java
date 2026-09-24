package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

/**
 * The beam: what the ring does when Green Lantern holds the button of the hand that attacks for a while. A
 * steady beam of hard light pours out of the ring, straight where the crosshair points, and keeps hurting
 * everything in it, all the way through a row of creatures, up to the first wall. It costs ring power every
 * second and stops when he lets go or the ring runs dry. While he flies he can sweep it over the ground below
 * him.
 */
public final class LightBeam implements Effect {
    private static final double VIEW_RANGE = 128.0;
    // How far off its middle line a creature can be and still be in the beam, in blocks.
    private static final double REACH = 0.35;
    // How long it takes to die down once he lets go, in ticks.
    private static final int FADE_TICKS = 3;

    // A charge nobody finished or let go of is forgotten after this many ticks.
    private static final int CHARGE_FORGET = 100;
    // How much of the whole gathering the server must have seen before the beam may start: half, since a server
    // that falls behind counts fewer ticks than his own game did, and the network never brings two presses exactly as
    // far apart as they were made.
    private static final double GATHER_SEEN = 0.5;

    private static final Map<UUID, LightBeam> FIRING = new HashMap<>();
    // When each player's ring started gathering its light for the beam, as a server tick: he holds the attack button
    // on its way to the beam, and everyone sees the light run down his arm into the ring.
    private static final Map<UUID, Integer> CHARGING = new HashMap<>();

    private final int id;
    private final ServerPlayer owner;
    private final float damage;
    private final int every;
    private final float perTick;
    private final double range;
    private final double push;
    private int age;
    private int fade = -1;
    private Vec3 facing;
    private double length;
    private Vec3 end;

    private LightBeam(ServerPlayer owner, CharacterAbility ability) {
        this.id = PowerRing.newId();
        this.owner = owner;
        this.damage = (float) ability.value("beamDamage");
        this.every = Math.max(1, ability.intValue("beamTicks"));
        this.perTick = (float) (ability.value("beamPowerPerSecond") / 20.0);
        this.range = ability.value("beamRangeBlocks");
        this.push = ability.value("beamKnockback");
        this.facing = owner.getLookAngle();
        this.end = owner.getEyePosition();
    }

    /**
     * The button has been held long enough: the beam starts pouring out of the ring.
     *
     * @return true when it started
     */
    static boolean start(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        // Whatever happens now, the gathering is over.
        Integer since = CHARGING.remove(owner.getUUID());
        boolean charged = since != null;
        // Only a ring that really gathered its light a good while pours it out: a game that says so at once is not
        // believed.
        boolean ready = charged && owner.server.getTickCount() - since >= ability.holdTicks() * GATHER_SEEN;
        if (!ready || FIRING.containsKey(owner.getUUID()) || Recharge.busy(owner) || GiantFist.holding(owner)
                || Flight.descending(owner) || LightFlare.up(owner) || AirStrike.calling(owner)) {
            if (charged) {
                PowerRing.sync(owner);
            }
            return false;
        }
        if (PowerRing.power(owner) <= 0.0F) {
            PowerRing.tell(owner, "no_power");
            if (charged) {
                PowerRing.sync(owner);
            }
            return false;
        }
        LightBeam beam = new LightBeam(owner, ability);
        FIRING.put(owner.getUUID(), beam);
        Effects.start(level, beam);
        owner.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, owner.getX(), owner.getEyeY(), owner.getZ(), SoundEvents.GUARDIAN_ATTACK,
                SoundSource.PLAYERS, 0.8F, 1.9F);
        level.playSound(null, owner.getX(), owner.getEyeY(), owner.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 1.8F);
        // All the light it gathered breaks loose at once.
        level.playSound(null, owner.getX(), owner.getEyeY(), owner.getZ(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, 0.45F, 1.7F);
        level.playSound(null, owner.getX(), owner.getEyeY(), owner.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.PLAYERS, 0.8F, 0.6F);
        Vec3 ring = owner.getEyePosition().add(owner.getLookAngle().scale(0.9));
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), ring, 24, 0.35);
        PowerRing.sync(owner);
        return true;
    }

    /**
     * He pressed the attack button: the ring starts gathering its light, in case he holds the button for the beam.
     * Letting go before that ({@link #stop}) or the beam starting ({@link #start}) ends it.
     */
    static void charge(ServerPlayer owner) {
        if (FIRING.containsKey(owner.getUUID()) || CHARGING.containsKey(owner.getUUID())) {
            return;
        }
        CHARGING.put(owner.getUUID(), owner.server.getTickCount());
        PowerRing.sync(owner);
    }

    /**
     * How many ticks ago this player's ring started gathering its light for the beam, or -1 when it gathers none. One
     * that has gone on far longer than the button needs is forgotten.
     */
    public static int charging(ServerPlayer player) {
        Integer since = CHARGING.get(player.getUUID());
        if (since == null) {
            return -1;
        }
        int ticks = player.server.getTickCount() - since;
        if (ticks > CHARGE_FORGET) {
            CHARGING.remove(player.getUUID());
            return -1;
        }
        return Math.max(0, ticks);
    }

    /**
     * The button comes up, or something else takes the ring hand: the beam dies down.
     *
     * @return true when there was a beam
     */
    static boolean stop(ServerPlayer owner) {
        LightBeam beam = FIRING.remove(owner.getUUID());
        if (beam == null) {
            // Let go before the beam came: the light it gathered dies away again.
            if (CHARGING.remove(owner.getUUID()) != null) {
                PowerRing.sync(owner);
            }
            return false;
        }
        beam.fade = 0;
        PowerRing.sync(owner);
        return true;
    }

    /** True while the beam pours out of this player's ring. */
    public static boolean firing(ServerPlayer player) {
        return FIRING.containsKey(player.getUUID());
    }

    /** The server stops: no beam is left. */
    public static void clear() {
        FIRING.clear();
        CHARGING.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (this.fade >= 0) {
            this.fade++;
            if (this.fade >= FADE_TICKS) {
                ConstructPayload.sendRemove(level, this.id, this.owner.position());
                return false;
            }
            this.send(level);
            return true;
        }
        if (FIRING.get(this.owner.getUUID()) != this) {
            this.fade = 0;
            return true;
        }
        float power = PowerRing.power(this.owner);
        if (!PowerRing.fuels(this.owner, level) || Recharge.busy(this.owner) || GiantFist.holding(this.owner)
                || Flight.descending(this.owner) || power <= 0.0F) {
            if (power <= 0.0F) {
                PowerRing.tell(this.owner, "no_power");
            }
            stop(this.owner);
            this.send(level);
            return true;
        }
        PowerRing.setPower(this.owner, power - this.perTick);
        this.age++;
        this.shine(level);
        this.send(level);
        return true;
    }

    /** Straight out along the crosshair, up to the first wall: everything in it is hurt every few ticks. */
    private void shine(ServerLevel level) {
        Vec3 eye = this.owner.getEyePosition();
        this.facing = this.owner.getLookAngle();
        Vec3 far = eye.add(this.facing.scale(this.range));
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(eye, far, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this.owner));
        boolean wall = block.getType() != HitResult.Type.MISS;
        this.end = wall ? block.getLocation() : far;
        this.length = eye.distanceTo(this.end);
        if (this.age % this.every == 1 || this.every == 1) {
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, this.end)
                    .inflate(REACH + 1.0), entity -> PowerRing.canHit(this.owner, entity))) {
                if (target.getBoundingBox().inflate(REACH).clip(eye, this.end).isEmpty()) {
                    continue;
                }
                // Every hit of the beam lands, however quickly they follow each other.
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().playerAttack(this.owner), this.damage);
                // The light drives what it hits back, a little with every hit.
                double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
                Vec3 shove = new Vec3(this.facing.x, 0.0, this.facing.z).scale(this.push * (1.0 - resist));
                target.setDeltaMovement(target.getDeltaMovement().add(shove.x, 0.04 * this.push, shove.z));
                target.hasImpulse = true;
                // Players move themselves on their own client, so they have to be told about the push.
                target.hurtMarked = true;
                Vec3 at = target.getBoundingBox().getCenter();
                ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.1F), at, 5, 0.25, 0.0);
                ParticleFx.cloud(level, ParticleTypes.CRIT, at, 4, 0.3, 0.2);
            }
        }
        // Where it strikes a wall the light splashes off it.
        if (wall && this.age % 2 == 0) {
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.GREEN, 1.6F), this.end, 5, 0.25, 0.0);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 0.9F), this.end, 3, 0.15, 0.0);
            // Sparks spray back off the wall.
            Vec3 back = this.facing.scale(-1.0);
            for (int i = 0; i < 2; i++) {
                Vec3 way = back.add(ParticleFx.spread(0.9), ParticleFx.spread(0.9) + 0.3,
                        ParticleFx.spread(0.9)).normalize();
                ParticleFx.fly(level, ParticleTypes.END_ROD, this.end.add(back.scale(0.1)), way, 0.25);
            }
        }
        if (this.age % 12 == 1) {
            level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.9F, 1.9F);
        }
    }

    private void send(ServerLevel level) {
        float solid = this.fade < 0 ? 1.0F : 1.0F - (float) this.fade / FADE_TICKS;
        PacketDistributor.sendToPlayersNear(level, null, this.owner.getX(), this.owner.getEyeY(), this.owner.getZ(),
                VIEW_RANGE, new ConstructPayload(this.id, this.owner.getId(), this.end, this.facing,
                        (float) this.length, solid, 0.0F, true, ConstructPayload.BEAM, 0, this.age, null));
    }
}
