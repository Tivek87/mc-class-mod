package nl.tivek.welcomescreen.character.lantern;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.network.ConstructPayload;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;
import nl.tivek.welcomescreen.spell.SpellFx;

/**
 * The beam: what the ring does when Green Lantern holds the button of the hand that attacks for a while. A
 * steady beam of hard light pours out of the ring, straight where the crosshair points, and keeps hurting
 * everything in it, all the way through a row of creatures, up to the first wall. It costs ring power every
 * second and stops when he lets go or the ring runs dry. While he flies he can sweep it over the ground below
 * him.
 */
final class LightBeam implements SpellEffect {
    private static final double VIEW_RANGE = 128.0;
    // How far off its middle line a creature can be and still be in the beam, in blocks.
    private static final double REACH = 0.35;
    // How long it takes to die down once he lets go, in ticks.
    private static final int FADE_TICKS = 3;

    private static final Map<UUID, LightBeam> FIRING = new HashMap<>();

    private final int id;
    private final ServerPlayer owner;
    private final float damage;
    private final int every;
    private final float perTick;
    private final double range;
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
        this.facing = owner.getLookAngle();
        this.end = owner.getEyePosition();
    }

    /**
     * The button has been held long enough: the beam starts pouring out of the ring.
     *
     * @return true when it started
     */
    static boolean start(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (FIRING.containsKey(owner.getUUID()) || Lantern.busy(owner) || GiantFist.holding(owner)
                || Flight.descending(owner)) {
            return false;
        }
        if (PowerRing.power(owner) <= 0.0F) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        LightBeam beam = new LightBeam(owner, ability);
        FIRING.put(owner.getUUID(), beam);
        SpellCasting.start(level, beam);
        owner.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, owner.getX(), owner.getEyeY(), owner.getZ(), SoundEvents.GUARDIAN_ATTACK,
                SoundSource.PLAYERS, 0.8F, 1.9F);
        level.playSound(null, owner.getX(), owner.getEyeY(), owner.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 1.8F);
        PowerRing.sync(owner);
        return true;
    }

    /**
     * The button comes up, or something else takes the ring hand: the beam dies down.
     *
     * @return true when there was a beam
     */
    static boolean stop(ServerPlayer owner) {
        LightBeam beam = FIRING.remove(owner.getUUID());
        if (beam == null) {
            return false;
        }
        beam.fade = 0;
        PowerRing.sync(owner);
        return true;
    }

    /** True while the beam pours out of this player's ring. */
    static boolean firing(ServerPlayer player) {
        return FIRING.containsKey(player.getUUID());
    }

    /** The server stops: no beam is left. */
    static void clear() {
        FIRING.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (this.fade >= 0) {
            this.fade++;
            if (this.fade >= FADE_TICKS) {
                PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
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
        if (!PowerRing.fuels(this.owner, level) || Lantern.busy(this.owner) || GiantFist.holding(this.owner)
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
        BlockHitResult block = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER,
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
                Vec3 at = target.getBoundingBox().getCenter();
                SpellFx.cloud(level, SpellFx.dust(PowerRing.BRIGHT, 1.1F), at, 5, 0.25, 0.0);
            }
        }
        // Where it strikes a wall the light splashes off it.
        if (wall && this.age % 2 == 0) {
            SpellFx.cloud(level, SpellFx.dust(PowerRing.GREEN, 1.2F), this.end, 3, 0.15, 0.0);
            SpellFx.cloud(level, SpellFx.dust(PowerRing.BRIGHT, 0.7F), this.end, 2, 0.1, 0.0);
        }
        if (this.age % 12 == 1) {
            level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.9F, 1.9F);
        }
    }

    private void send(ServerLevel level) {
        float solid = this.fade < 0 ? 1.0F : 1.0F - (float) this.fade / FADE_TICKS;
        PacketDistributor.sendToPlayersNear(level, null, this.owner.getX(), this.owner.getEyeY(), this.owner.getZ(),
                VIEW_RANGE, new ConstructPayload(this.id, this.owner.getId(), this.end, this.facing,
                        (float) this.length, solid, 0.0F, true, ConstructPayload.BEAM));
    }
}
