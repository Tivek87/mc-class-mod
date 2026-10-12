package nl.tivek.multiversepowers.creature.wizard.rift;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;

// A rift the Wizard opens to go through, to play with or to cast out of: a window into a deep sky (client/rift/
// PortalRenderer), standing upright and facing its yaw or lying flat facing up, ringed in the light of its kind:
// arcane violet, the void's black and magenta, a golden clockwork ring, frost, a starry night or embers. It swirls
// open, holds and closes again by itself; it harms no one.
public final class WizardPortal extends Entity {
    public static final int ARCANE = 0;
    public static final int VOID = 1;
    public static final int GOLDEN = 2;
    public static final int FROST = 3;
    public static final int STARRY = 4;
    public static final int EMBER = 5;
    public static final int STYLES = 6;
    public static final int OPENS = 8;
    private static final int[] RIM = { Wizard.VIOLET, Wizard.VOID, Wizard.TIME, Wizard.ICE, Wizard.STARLIGHT,
            Wizard.FIRE };
    private static final int[] DEEP = { 0x0B0420, 0x020004, 0x1A1004, 0x02101C, 0x050818, 0x1C0602 };
    private static final int[] GLOW = { 0xE0CCFF, 0xFF8AF0, 0xFFF2C0, 0xE8FAFF, 0xFFFFFF, 0xFFD08A };
    private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(WizardPortal.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(WizardPortal.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(WizardPortal.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> UPRIGHT = SynchedEntityData.defineId(WizardPortal.class,
            EntityDataSerializers.BOOLEAN);

    public WizardPortal(EntityType<? extends WizardPortal> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // A rift round `center` of `radius`, open `life` ticks: upright facing `yaw` (degrees, as a creature faces), or
    // flat on its back.
    public static WizardPortal open(ServerLevel level, Vec3 center, float yaw, float radius, int life, int style,
            boolean upright) {
        WizardPortal portal = new WizardPortal(WizardEntities.PORTAL.get(), level);
        portal.setPos(center.x, center.y, center.z);
        portal.setYRot(yaw);
        portal.entityData.set(STYLE, Mth.clamp(style, 0, STYLES - 1));
        portal.entityData.set(LIFE, life);
        portal.entityData.set(RADIUS, radius);
        portal.entityData.set(UPRIGHT, upright);
        level.addFreshEntity(portal);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.HOSTILE,
                1.2F, 0.6F + 0.15F * style);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ILLUSIONER_PREPARE_MIRROR,
                SoundSource.HOSTILE, 1.0F, 1.2F);
        return portal;
    }

    // The kind of rift his phase opens: violet, frost, embers, the void, gold.
    public static int styleOf(int phase) {
        return switch (phase) {
            case 2 -> FROST;
            case 3 -> EMBER;
            case 4 -> VOID;
            case 5 -> GOLDEN;
            default -> ARCANE;
        };
    }

    public static int rim(int style) {
        return RIM[Mth.clamp(style, 0, STYLES - 1)];
    }

    public static int deep(int style) {
        return DEEP[Mth.clamp(style, 0, STYLES - 1)];
    }

    public static int glow(int style) {
        return GLOW[Mth.clamp(style, 0, STYLES - 1)];
    }

    public int style() {
        return this.entityData.get(STYLE);
    }

    public int life() {
        return this.entityData.get(LIFE);
    }

    public float radius() {
        return this.entityData.get(RADIUS);
    }

    public boolean upright() {
        return this.entityData.get(UPRIGHT);
    }

    // The way it faces: along its yaw when upright, up when it lies flat.
    public Vec3 facing() {
        if (!this.upright()) {
            return new Vec3(0.0, 1.0, 0.0);
        }
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    // How far open it is: 0 shut, 1 wide open.
    public float openness(float partialTick) {
        float age = this.tickCount + partialTick;
        return Mth.clamp(Math.min(age / OPENS, (this.life() - age) / OPENS), 0.0F, 1.0F);
    }

    // Shuts it within a few ticks.
    public void close() {
        this.entityData.set(LIFE, Math.min(this.life(), this.tickCount + OPENS));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STYLE, ARCANE);
        builder.define(LIFE, 40);
        builder.define(RADIUS, 1.2F);
        builder.define(UPRIGHT, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.sparkle();
        } else if (this.tickCount >= this.life()) {
            this.discard();
        }
    }

    // Motes of its light drifting off its rim, and its kind's own: snow, embers, the void's ink, stars.
    private void sparkle() {
        float open = this.openness(0.0F);
        if (open <= 0.0F) {
            return;
        }
        int style = this.style();
        Vec3 facing = this.facing();
        Vec3 side = this.upright() ? new Vec3(facing.z, 0.0, -facing.x) : new Vec3(1.0, 0.0, 0.0);
        Vec3 up = this.upright() ? new Vec3(0.0, 1.0, 0.0) : new Vec3(0.0, 0.0, 1.0);
        double r = this.radius() * open;
        double a = this.random.nextDouble() * Mth.TWO_PI;
        Vec3 rim = this.position().add(side.scale(Math.cos(a) * r * 0.8)).add(up.scale(Math.sin(a) * r));
        this.level().addParticle(Wizard.dust(rim(style), 1.1F), rim.x, rim.y, rim.z, facing.x * 0.02,
                facing.y * 0.02, facing.z * 0.02);
        if (this.random.nextInt(3) == 0) {
            ParticleOptions own = switch (style) {
                case FROST -> ParticleTypes.SNOWFLAKE;
                case EMBER -> ParticleTypes.SMALL_FLAME;
                case VOID -> ParticleTypes.REVERSE_PORTAL;
                case GOLDEN -> ParticleTypes.WAX_ON;
                case STARRY -> ParticleTypes.END_ROD;
                default -> ParticleTypes.WITCH;
            };
            Vec3 out = facing.scale(0.04 + this.random.nextDouble() * 0.04);
            this.level().addParticle(own, rim.x, rim.y, rim.z, out.x, out.y, out.z);
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
