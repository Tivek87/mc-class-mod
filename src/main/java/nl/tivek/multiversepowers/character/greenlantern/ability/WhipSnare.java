package nl.tivek.multiversepowers.character.greenlantern.ability;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// The lasso's hold on a creature: the lash reaches it, winds round it, and the haul throws it down at the owner's feet.
public final class WhipSnare {
    public static final int REACHING = 0;
    public static final int BOUND = 1;
    public static final int HAULING = 2;
    public static final int LANDED = 3;
    // Ticks it still tells of the creature once it has landed, while the lash unwinds from it.
    private static final int UNWIND = 6;
    private static final double WIDEST = 3.0;
    private static final double TALLEST = 4.0;
    private static final double STRONGEST = 150.0;
    private static final double LOST = 8.0;
    private static final double LANDS_AHEAD = 1.7;
    private static final double VIEW_RANGE = 96.0;

    final int id = PowerRing.newId();
    private final ServerPlayer owner;
    final LivingEntity target;
    private int phase = REACHING;
    private Vec3 bound = Vec3.ZERO;
    private Vec3 from = Vec3.ZERO;
    private Vec3 landing = Vec3.ZERO;
    private boolean holding;

    private WhipSnare(ServerPlayer owner, LivingEntity target) {
        this.owner = owner;
        this.target = target;
    }

    @Nullable
    static WhipSnare aim(ServerPlayer owner, ServerLevel level, double range) {
        LivingEntity target = Targeting.aimLiving(owner, level, range);
        if (target == null) {
            return null;
        }
        if (target.getBbWidth() > WIDEST || target.getBbHeight() > TALLEST || target.getMaxHealth() > STRONGEST
                || HeldMobs.isHeldByAnyone(target)) {
            PowerRing.tell(owner, "lasso_too_big");
            return null;
        }
        return new WhipSnare(owner, target);
    }

    boolean lost(ServerLevel level, double range) {
        return !this.target.isAlive() || this.target.isRemoved() || this.target.level() != level
                || this.target.distanceTo(this.owner) > range + LOST;
    }

    // Moves the creature for the lasso's own tick t; false once the lash has let go of it.
    boolean tick(ServerLevel level, int t, Hurt values) {
        if (t < WhipMove.LASSO_REACH) {
            return true;
        }
        if (this.phase == LANDED) {
            return t < WhipMove.LASSO_LAND + UNWIND;
        }
        if (this.phase == REACHING) {
            this.phase = BOUND;
            this.bound = this.target.position();
            if (this.target instanceof Mob mob) {
                this.holding = HeldMobs.hold(mob);
            }
            this.sound(level, SoundEvents.LEASH_KNOT_PLACE, 1.0F, 1.2F);
            this.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F, 1.6F);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), this.target.getBoundingBox().getCenter(),
                    8, 0.35, 0.05);
        }
        if (t < WhipMove.LASSO_HAUL) {
            this.place(this.bound);
            return true;
        }
        if (this.phase != HAULING) {
            this.phase = HAULING;
            this.from = this.target.position();
            this.landing = this.landing(level);
            this.sound(level, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.8F);
            this.sound(level, SoundEvents.LEASH_KNOT_BREAK, 0.8F, 0.7F);
        }
        double u = Math.min(1.0, (t - WhipMove.LASSO_HAUL) / (double) (WhipMove.LASSO_LAND - WhipMove.LASSO_HAUL));
        if (u < 1.0) {
            double far = this.from.subtract(this.landing).horizontalDistance();
            double arc = (0.6 + 0.12 * far) * 4.0 * u * (1.0 - u);
            this.place(this.from.lerp(this.landing, Ease.smooth(Math.pow(u, 0.8))).add(0.0, arc, 0.0));
            return true;
        }
        this.place(this.landing);
        this.phase = LANDED;
        this.land(level, values);
        return true;
    }

    // In front of the owner on the ground, or short of a wall in the way.
    private Vec3 landing(ServerLevel level) {
        Vec3 ahead = WhipHits.flat(this.owner.getLookAngle());
        Vec3 spot = this.owner.position().add(ahead.scale(LANDS_AHEAD + this.target.getBbWidth() * 0.5));
        Vec3 lift = new Vec3(0.0, this.target.getBbHeight() * 0.5, 0.0);
        BlockHitResult wall = LoadedWorld.clip(level, new ClipContext(this.target.position().add(lift), spot.add(lift),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.target));
        if (wall.getType() != HitResult.Type.MISS) {
            Vec3 back = this.target.position().subtract(spot);
            Vec3 stop = wall.getLocation().subtract(lift);
            spot = back.lengthSqr() < 1.0E-6 ? stop : stop.add(back.normalize().scale(0.4 + this.target.getBbWidth()
                    * 0.5));
        }
        double floor = Targeting.floorBelow(level, BlockPos.containing(spot.x, spot.y + 1.5, spot.z));
        return new Vec3(spot.x, floor, spot.z);
    }

    private void land(ServerLevel level, Hurt values) {
        this.release();
        this.target.invulnerableTime = 0;
        this.target.hurt(level.damageSources().playerAttack(this.owner), (float) values.damage());
        int slow = (int) Math.round(values.slowSeconds() * 20.0);
        if (slow > 0) {
            this.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 2));
        }
        this.target.setDeltaMovement(0.0, 0.18, 0.0);
        this.target.hurtMarked = true;
        Vec3 at = this.landing;
        BlockState ground = level.getBlockState(BlockPos.containing(at.subtract(0.0, 0.2, 0.0)));
        if (!ground.isAir()) {
            ParticleFx.cloud(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.add(0.0, 0.1, 0.0), 30,
                    0.5, 0.25);
        }
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.GREEN, 1.4F), at.add(0.0, 0.15, 0.0), 32, 0.35);
        ParticleFx.cloud(level, ParticleTypes.CRIT, this.target.getBoundingBox().getCenter(), 10, 0.3, 0.3);
        this.sound(level, SoundEvents.MACE_SMASH_GROUND, 0.9F, 1.15F);
        this.sound(level, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.9F);
        this.sound(level, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.6F, 1.4F);
    }

    private void place(Vec3 at) {
        this.target.setDeltaMovement(Vec3.ZERO);
        this.target.resetFallDistance();
        if (this.target instanceof ServerPlayer player) {
            // A server that does not allow flying must not think he hangs in the air on his own.
            player.teleportTo(at.x, at.y, at.z);
            player.connection.aboveGroundTickCount = 0;
        } else {
            this.target.setPos(at.x, at.y, at.z);
        }
    }

    void release() {
        if (this.holding && this.target instanceof Mob mob) {
            HeldMobs.release(mob);
        }
        this.holding = false;
    }

    void send(ServerLevel level, int t) {
        Vec3 at = this.target.getBoundingBox().getCenter();
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), at, this.owner.getLookAngle(), 0.0F, 1.0F,
                        LightBubble.caught(this.target.getId()), true, ConstructPayload.WHIP_SNARE, this.phase, t,
                        null));
    }

    void gone(ServerLevel level) {
        this.release();
        ConstructPayload.sendRemove(level, this.id, this.target.position());
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, this.target.getX(), this.target.getY() + 0.5, this.target.getZ(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }

    record Hurt(double damage, double slowSeconds) {
    }
}
