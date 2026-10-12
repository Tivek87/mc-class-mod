package nl.tivek.multiversepowers.creature.wizard.field;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.WizardKnock;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// An icy blizzard the Wizard calls down round his foe: it bursts out of the ground where his staff strikes it and
// whirls there a while, a wall of snow turning round and round. Whoever stands in it is pushed round with the wind,
// slowed, frozen (a player only chilled, the frost thin at the edge of their sight) and bitten by the cold every half
// second. Seen from inside it, a blue-grey mist closes in (client/BlizzardRenderer).
public final class Blizzard extends Entity {
    public static final double RADIUS = 6.0;
    public static final double TALL = 5.0;
    public static final int GROWS = 14;
    public static final int LIFE = 150;
    public static final int FADES = 25;
    private static final int BITE = 10;
    // How far a player's frost goes, as a share of being frozen through.
    private static final float PLAYER_FROST = 0.4F;
    @Nullable
    private UUID owner;
    private float damage = 2.0F;

    public Blizzard(EntityType<? extends Blizzard> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void call(ServerLevel level, Wizard wizard, Vec3 ground, float damage) {
        Blizzard storm = new Blizzard(WizardEntities.BLIZZARD.get(), level);
        storm.setPos(ground.x, ground.y, ground.z);
        storm.owner = wizard.getUUID();
        storm.damage = damage;
        level.addFreshEntity(storm);
        ParticleFx.send(level, ParticleTypes.SNOWFLAKE, ground.x, ground.y + 0.5, ground.z, 120, 2.0, 0.4, 2.0, 0.6);
        ParticleFx.send(level, ParticleTypes.CLOUD, ground.x, ground.y + 0.3, ground.z, 40, 2.5, 0.2, 2.5, 0.15);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.6F, 0.5F);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.HOSTILE, 1.6F, 0.5F);
    }

    // How strong it whirls now: growing at first, dying away at the end.
    public float strength(float partialTick) {
        float age = this.tickCount + partialTick;
        return Mth.clamp(Math.min(age / GROWS, (LIFE - age) / FADES), 0.0F, 1.0F);
    }

    public double radius(float partialTick) {
        return RADIUS * (0.35 + 0.65 * Math.min(1.0, (this.tickCount + partialTick) / GROWS));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.snow();
            return;
        }
        if (this.tickCount >= LIFE) {
            this.discard();
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        Entity owner = this.owner == null ? null : level.getEntity(this.owner);
        if (this.tickCount % 20 == 0) {
            level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.BREEZE_IDLE_AIR,
                    SoundSource.HOSTILE, 1.4F, 0.4F + this.random.nextFloat() * 0.1F);
        }
        if (!(owner instanceof Mob mob)) {
            return;
        }
        double radius = this.radius(0.0F);
        float strength = this.strength(0.0F);
        for (LivingEntity living : MonsterBlows.foes(mob, new AABB(this.position(), this.position())
                .inflate(radius, 0.0, radius).expandTowards(0.0, TALL, 0.0))) {
            Vec3 off = living.position().subtract(this.position());
            double far = Math.sqrt(off.x * off.x + off.z * off.z);
            if (far > radius) {
                continue;
            }
            // Pushed round with the wind and a little in towards its eye.
            if (far > 0.3) {
                double push = 0.05 * strength * WizardKnock.KNOCK;
                living.push(-off.z / far * push - off.x / far * push * 0.3, 0.0,
                        off.x / far * push - off.z / far * push * 0.3);
                living.hurtMarked = true;
            }
            // A player is chilled, never frozen white: the frost at the screen's edge stays thin enough to see through.
            int most = living instanceof Player ? (int) (living.getTicksRequiredToFreeze() * PLAYER_FROST)
                    : living.getTicksRequiredToFreeze() + 40;
            living.setTicksFrozen(Math.min(most, living.getTicksFrozen() + 5));
            if (this.tickCount % BITE == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2), mob);
                if (living.hurt(Wizard.source(this, owner, WizardEntities.FROST), this.damage * strength)) {
                    level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.PLAYER_HURT_FREEZE,
                            SoundSource.HOSTILE, 0.6F, 1.0F);
                }
            }
        }
    }

    // A wall of snow whirling round, thicker at its edge, and the frost on the ground.
    private void snow() {
        float strength = this.strength(0.0F);
        if (strength <= 0.0F) {
            return;
        }
        double radius = this.radius(0.0F);
        int count = (int) (10 * strength);
        for (int k = 0; k < count; k++) {
            double a = this.random.nextDouble() * Mth.TWO_PI;
            double r = radius * (0.25 + 0.75 * Math.sqrt(this.random.nextDouble()));
            double y = this.random.nextDouble() * TALL;
            double x = Math.cos(a) * r;
            double z = Math.sin(a) * r;
            double spin = 0.35 + 0.25 * r / radius;
            this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX() + x, this.getY() + y, this.getZ() + z,
                    -Math.sin(a) * spin, -0.04, Math.cos(a) * spin);
        }
        if (this.random.nextInt(2) == 0) {
            double a = this.random.nextDouble() * Mth.TWO_PI;
            this.level().addParticle(ParticleTypes.CLOUD, this.getX() + Math.cos(a) * radius, this.getY() + 0.2,
                    this.getZ() + Math.sin(a) * radius, -Math.sin(a) * 0.2, 0.02, Math.cos(a) * 0.2);
        }
        if (this.random.nextInt(3) == 0) {
            double a = this.random.nextDouble() * Mth.TWO_PI;
            double r = Math.sqrt(this.random.nextDouble()) * radius;
            this.level().addParticle(Wizard.dust(Wizard.ICE, 1.2F), this.getX() + Math.cos(a) * r, this.getY() + 0.1,
                    this.getZ() + Math.sin(a) * r, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0 * 128.0;
    }
}
