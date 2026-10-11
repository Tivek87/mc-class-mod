package nl.tivek.multiversepowers.creature.wizard;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// An image of the Wizard. Left behind as he blinks away (BLINK): himself to the life but ghostly, a shimmer of violet
// glass you can see through, turning to watch whoever is nearest and now and then flicking a spark it never lets go;
// from his second phase it bursts when struck. One of his sixteen in the circle of mirrors (IMAGE): solid as he is,
// moved by him (WizardMirrors), only its pale crystal and the shadow it does not cast telling it from him. It harms no
// one by itself; the first blow shatters it into shards of violet glass with a push.
public final class WizardDecoy extends MovingMonster {
    public static final int BLINK = 0;
    public static final int IMAGE = 1;
    private static final int BLINK_LIFE = 200;
    private static final int IMAGE_LIFE = 320;
    private static final double PUSH = 4.0;
    private static final double BURST = 2.6;
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(WizardDecoy.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LOOK = SynchedEntityData.defineId(WizardDecoy.class,
            EntityDataSerializers.INT);
    private static final EntityDimensions FLOATING = EntityDimensions.scalable(0.6F, 1.95F + (float) Wizard.HOVER)
            .withEyeHeight(1.62F + (float) Wizard.HOVER);
    @Nullable
    private UUID owner;

    public WizardDecoy(EntityType<? extends WizardDecoy> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.xpReward = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODE, BLINK);
        builder.define(LOOK, 1);
    }

    // A ghostly image of him left at `at`.
    static void leave(ServerLevel level, Wizard wizard, Vec3 at) {
        make(level, wizard, at, BLINK);
    }

    // One of his images in the circle of mirrors.
    @Nullable
    static WizardDecoy image(ServerLevel level, Wizard wizard, Vec3 at) {
        WizardDecoy decoy = make(level, wizard, at, IMAGE);
        if (decoy != null) {
            decoy.setNoGravity(true);
        }
        return decoy;
    }

    @Nullable
    private static WizardDecoy make(ServerLevel level, Wizard wizard, Vec3 at, int mode) {
        WizardDecoy decoy = WizardEntities.DECOY.get().create(level);
        if (decoy == null) {
            return null;
        }
        decoy.entityData.set(MODE, mode);
        decoy.entityData.set(LOOK, wizard.phase());
        decoy.owner = wizard.getUUID();
        decoy.moveTo(at.x, at.y, at.z, wizard.getYRot(), wizard.getXRot());
        decoy.yBodyRot = wizard.yBodyRot;
        decoy.yHeadRot = wizard.yHeadRot;
        if (wizard.hasCustomName()) {
            decoy.setCustomName(wizard.getCustomName());
        }
        level.addFreshEntity(decoy);
        return decoy;
    }

    // Where the crystal of an image's staff is (as Wizard.staffTip).
    static Vec3 tip(LivingEntity body, double hover) {
        float yaw = body.yBodyRot * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        return body.position().add(-cos * 0.45 - sin * 0.35, 2.25 + hover, -sin * 0.45 + cos * 0.35);
    }

    public int mode() {
        return this.entityData.get(MODE);
    }

    // The phase of the Wizard it shows.
    public int look() {
        return this.entityData.get(LOOK);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (LOOK.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.look() > 1 ? FLOATING : super.getDefaultDimensions(pose);
    }

    @Override
    protected int kinds() {
        return WizardMove.KINDS;
    }

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.mode() == IMAGE) {
                double hover = this.look() > 1 ? Wizard.HOVER : 0.0;
                WizardSparks.of(this, this.look(), tip(this, hover), this.position().add(0.0, 1.35 + hover, 0.0));
            }
            return;
        }
        boolean image = this.mode() == IMAGE;
        Entity wizard = this.owner == null ? null : ((ServerLevel) this.level()).getEntity(this.owner);
        if (image && (!(wizard instanceof Wizard) || !wizard.isAlive())) {
            this.shatter(null);
            return;
        }
        if (!image) {
            Player near = this.level().getNearestPlayer(this, 24.0);
            if (near != null) {
                this.lookAt(near, 20.0F, 20.0F);
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
            }
            if (this.tickCount % 50 == 25 && this.move() == 0) {
                this.start(WizardMove.SPARK);
            } else if (this.move() != 0 && this.moveAge(0.0F) >= WizardMove.length(WizardMove.SPARK)) {
                this.start(0);
            }
        }
        if (this.tickCount >= (image ? IMAGE_LIFE : BLINK_LIFE)) {
            this.shatter(null);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved() || source.getEntity() instanceof Wizard
                || source.getEntity() instanceof WizardDecoy) {
            return false;
        }
        this.shatter(source.getEntity() instanceof LivingEntity living ? living : null);
        return true;
    }

    void shatter(@Nullable LivingEntity by) {
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) {
            return;
        }
        Vec3 at = this.position().add(0.0, 1.0 + (this.look() > 1 ? Wizard.HOVER : 0.0), 0.0);
        ParticleFx.send(level, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.AMETHYST_SHARD)),
                at.x, at.y, at.z, 40, 0.3, 0.7, 0.3, 0.25);
        ParticleFx.send(level, Wizard.dust(Wizard.VIOLET, 1.6F), at.x, at.y, at.z, 30, 0.3, 0.8, 0.3, 0.0);
        ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y, at.z, 12, 0.2, 0.5, 0.2, 0.15);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.4F, 0.8F);
        Entity wizard = this.owner == null ? null : level.getEntity(this.owner);
        if (by != null && this.mode() == BLINK && this.look() > 1 && wizard instanceof Wizard owner) {
            // From his second phase a struck decoy bursts in the striker's face.
            for (LivingEntity living : MonsterBlows.foes(owner, new AABB(at, at).inflate(BURST))) {
                Vec3 away = MonsterBlows.toward(owner, at, living.position());
                MonsterBlows.hurt(living, Wizard.source(this, owner, WizardEntities.ARCANE),
                        owner.harm(WizardMove.SPARK), away.scale(0.9).add(0.0, 0.35, 0.0));
            }
            ParticleFx.at(level, ParticleTypes.FLASH, at);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.4F,
                    0.7F);
        } else if (by != null && by.distanceTo(this) < PUSH) {
            Vec3 away = by.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : away.normalize();
            by.push(away.x * 0.9, 0.35, away.z * 0.9);
            by.hurtMarked = true;
        }
        this.discard();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
