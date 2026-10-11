package nl.tivek.multiversepowers.creature.wizard.field;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.spell.FallingStar;

// A rift into the night sky the Wizard tears open high over his foe: it swirls open, out of it falls star after star
// onto the ground below (most of them aimed at whoever stands under it), and it closes again. Drawn as a window into
// a deep starry sky ringed in violet light (client/RiftRenderer).
public final class StarRift extends Entity {
    public static final int OPENS = 16;
    public static final int RAINS = 96;
    public static final int LIFE = 116;
    public static final double RADIUS = 4.2;
    public static final double HEIGHT = 11.0;
    private static final double SPREAD = 6.0;
    @Nullable
    private UUID owner;
    private float damage = 7.0F;
    private int every = 5;
    // Where the stars fall: the ground under the rift.
    private double groundY;

    public StarRift(EntityType<? extends StarRift> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void open(ServerLevel level, Wizard wizard, Vec3 ground, float damage, int every) {
        // Under a roof it opens just below it.
        double height = HEIGHT;
        for (int up = 2; up <= HEIGHT; up++) {
            if (!level.getBlockState(BlockPos.containing(ground.x, ground.y + up, ground.z)).isAir()) {
                height = Math.max(3.0, up - 1.5);
                break;
            }
        }
        make(level, wizard, ground, height, damage, every);
    }

    // A rift that rains nothing, `height` over `ground`: where the great meteor comes out.
    public static void portal(ServerLevel level, Wizard wizard, Vec3 ground, double height) {
        make(level, wizard, ground, height, 0.0F, 0);
    }

    private static void make(ServerLevel level, Wizard wizard, Vec3 ground, double height, float damage, int every) {
        StarRift rift = new StarRift(WizardEntities.RIFT.get(), level);
        rift.setPos(ground.x, ground.y + height, ground.z);
        rift.groundY = ground.y;
        rift.owner = wizard.getUUID();
        rift.damage = damage;
        rift.every = every;
        level.addFreshEntity(rift);
        level.playSound(null, ground.x, ground.y + height, ground.z, SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE, 1.0F, 1.6F);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F,
                0.5F);
    }

    // How far open it is: 0 shut, 1 wide open.
    public float openness(float partialTick) {
        float age = this.tickCount + partialTick;
        return Mth.clamp(Math.min(age / OPENS, (LIFE - age) / (LIFE - RAINS)), 0.0F, 1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.sparkle();
            return;
        }
        if (this.tickCount >= LIFE) {
            this.discard();
            return;
        }
        if (this.every > 0 && this.tickCount >= OPENS && this.tickCount < RAINS
                && (this.tickCount - OPENS) % this.every == 0
                && this.level() instanceof ServerLevel level) {
            this.drop(level);
        }
        if (this.tickCount == RAINS && this.level() instanceof ServerLevel level) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.END_PORTAL_FRAME_FILL,
                    SoundSource.HOSTILE, 1.2F, 0.5F);
        }
    }

    // One star out of somewhere in the rift: two in three at someone standing under it, the rest anywhere below.
    private void drop(ServerLevel level) {
        Entity owner = this.owner == null ? null : level.getEntity(this.owner);
        double angle = this.random.nextDouble() * Mth.TWO_PI;
        double r = Math.sqrt(this.random.nextDouble()) * RADIUS * 0.85;
        Vec3 from = this.position().add(Math.cos(angle) * r, -0.3, Math.sin(angle) * r);
        Vec3 ground = new Vec3(this.getX(), this.groundY, this.getZ());
        Vec3 to = null;
        if (owner instanceof Mob mob && this.random.nextInt(3) != 0) {
            List<LivingEntity> under = MonsterBlows.foes(mob, new AABB(ground, ground).inflate(SPREAD + 2.0, 4.0,
                    SPREAD + 2.0));
            if (!under.isEmpty()) {
                LivingEntity mark = under.get(this.random.nextInt(under.size()));
                Vec3 lead = mark.getDeltaMovement().multiply(8.0, 0.0, 8.0);
                to = mark.position().add(lead);
            }
        }
        if (to == null) {
            double a = this.random.nextDouble() * Mth.TWO_PI;
            double d = Math.sqrt(this.random.nextDouble()) * SPREAD;
            to = ground.add(Math.cos(a) * d, 0.0, Math.sin(a) * d);
        }
        FallingStar.fall(level, owner, from, to, this.damage);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 1.2F,
                0.5F);
    }

    // Sparks drifting down out of it and a ring of motes turning at its rim.
    private void sparkle() {
        float open = this.openness(0.0F);
        if (open <= 0.0F) {
            return;
        }
        double radius = RADIUS * open;
        for (int k = 0; k < 3; k++) {
            double a = this.random.nextDouble() * Mth.TWO_PI;
            Vec3 at = this.position().add(Math.cos(a) * radius, 0.0, Math.sin(a) * radius);
            this.level().addParticle(Wizard.dust(Wizard.VIOLET, 1.6F), at.x, at.y, at.z, 0.0, -0.02, 0.0);
        }
        double a = this.random.nextDouble() * Mth.TWO_PI;
        double d = Math.sqrt(this.random.nextDouble()) * radius;
        this.level().addParticle(ParticleTypes.END_ROD, this.getX() + Math.cos(a) * d, this.getY() - 0.2,
                this.getZ() + Math.sin(a) * d, 0.0, -0.08, 0.0);
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
