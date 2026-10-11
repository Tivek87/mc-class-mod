package nl.tivek.multiversepowers.creature.wizard.spell;

import javax.annotation.Nullable;
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
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// A bolt of arcane light from the Wizard's staff: it flies fast and bends after its mark a little each tick, so it
// can be stepped aside from late but not outrun; it strikes with the harm of pure magic and bursts in violet sparks.
// Unspent, it fizzles out after a few seconds. A spark (WizardEntities.SPARK) is its small quick cousin, flicked
// between his big spells: faster, straight, weaker. An orb (ORB, his arcane orbit) circles him first and is let go at
// them one after another; a star shard (SHARD, his star spiral) flies straight out in the white light of the stars.
public final class ArcaneBolt extends ThrowableProjectile {
    private static final double SPEED = 0.95;
    private static final double SPARK_SPEED = 1.4;
    private static final double TURN = 0.09;
    private static final int LIFE = 70;
    private static final int SPARK_LIFE = 35;
    private static final double SHARD_SPEED = 0.75;
    private static final int SHARD_LIFE = 60;
    private static final double ORBIT_RADIUS = 1.7;
    private float damage = 6.0F;
    @Nullable
    private LivingEntity mark;
    // An orb circling him: who, its place on the circle and the tick it is let go.
    @Nullable
    private Wizard circles;
    private double slot;
    private int launch;

    public ArcaneBolt(EntityType<? extends ArcaneBolt> type, Level level) {
        super(type, level);
    }

    public static void cast(ServerLevel level, Wizard wizard, LivingEntity target, float damage) {
        ArcaneBolt bolt = new ArcaneBolt(WizardEntities.BOLT.get(), level);
        bolt.setOwner(wizard);
        Vec3 from = wizard.staffTip();
        bolt.setPos(from.x, from.y, from.z);
        Vec3 way = target.getBoundingBox().getCenter().subtract(from).normalize();
        bolt.setDeltaMovement(way.scale(SPEED));
        bolt.damage = damage;
        bolt.mark = target;
        level.addFreshEntity(bolt);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.0F,
                1.6F);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5F,
                0.7F);
    }

    // A quick spark from `from` at where `target` will be, led a little.
    public static void spark(ServerLevel level, Wizard wizard, Vec3 from, LivingEntity target, float damage) {
        ArcaneBolt spark = new ArcaneBolt(WizardEntities.SPARK.get(), level);
        spark.setOwner(wizard);
        spark.setPos(from.x, from.y, from.z);
        Vec3 to = target.getBoundingBox().getCenter();
        Vec3 lead = target.getDeltaMovement().multiply(1.0, 0.0, 1.0).scale(from.distanceTo(to) / SPARK_SPEED);
        spark.setDeltaMovement(to.add(lead).subtract(from).normalize().scale(SPARK_SPEED));
        spark.damage = damage;
        level.addFreshEntity(spark);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.2F,
                1.8F);
    }

    // `count` orbs round him, let go at `target` one after another from `first` ticks on, `every` apart.
    public static void orbit(ServerLevel level, Wizard wizard, LivingEntity target, float damage, int count,
            int first, int every) {
        for (int k = 0; k < count; k++) {
            ArcaneBolt orb = new ArcaneBolt(WizardEntities.ORB.get(), level);
            orb.setOwner(wizard);
            orb.circles = wizard;
            orb.slot = k * Mth.TWO_PI / count;
            orb.launch = first + k * every;
            orb.damage = damage;
            orb.mark = target;
            Vec3 at = orb.circling(wizard);
            orb.setPos(at.x, at.y, at.z);
            level.addFreshEntity(orb);
        }
        level.playSound(null, wizard.getX(), wizard.getY() + 1.5, wizard.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.HOSTILE, 1.6F, 1.4F);
    }

    // A star shard from `from` straight along `way`.
    public static void shard(ServerLevel level, Wizard wizard, Vec3 from, Vec3 way, float damage) {
        ArcaneBolt shard = new ArcaneBolt(WizardEntities.SHARD.get(), level);
        shard.setOwner(wizard);
        shard.setPos(from.x, from.y, from.z);
        shard.setDeltaMovement(way.normalize().scale(SHARD_SPEED));
        shard.damage = damage;
        level.addFreshEntity(shard);
    }

    private Vec3 circling(Wizard wizard) {
        double angle = this.tickCount * 0.22 + this.slot;
        return wizard.position().add(Math.cos(angle) * ORBIT_RADIUS, 1.6 + wizard.hover() + 0.2 * Math.sin(angle * 2.0),
                Math.sin(angle) * ORBIT_RADIUS);
    }

    private boolean isSpark() {
        return this.getType() == WizardEntities.SPARK.get();
    }

    private boolean isShard() {
        return this.getType() == WizardEntities.SHARD.get();
    }

    private int colour() {
        return this.isShard() ? Wizard.STARLIGHT : Wizard.VIOLET;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            Vec3 at = this.position();
            Vec3 v = this.getDeltaMovement();
            for (int k = 0; k < 2; k++) {
                double s = k * 0.5;
                this.level().addParticle(Wizard.dust(this.colour(), 1.1F), at.x - v.x * s, at.y + 0.2 - v.y * s,
                        at.z - v.z * s, 0.0, 0.0, 0.0);
            }
            if (this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.END_ROD, at.x, at.y + 0.2, at.z, -v.x * 0.05, -v.y * 0.05,
                        -v.z * 0.05);
            }
            return;
        }
        if (this.circles != null) {
            if (!this.circles.isAlive()) {
                this.burst(this.position());
                return;
            }
            if (this.tickCount < this.launch) {
                this.setDeltaMovement(this.circling(this.circles).subtract(this.position()));
                return;
            }
            this.circles = null;
            if (this.mark != null) {
                Vec3 way = this.mark.getBoundingBox().getCenter().subtract(this.position()).normalize();
                this.setDeltaMovement(way.scale(SPEED));
            }
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL,
                    SoundSource.HOSTILE, 0.8F, 1.8F);
            this.launch = this.tickCount;
        }
        LivingEntity target = this.mark;
        if (target != null && target.isAlive()) {
            Vec3 want = target.getBoundingBox().getCenter().subtract(this.position()).normalize();
            Vec3 v = this.getDeltaMovement().normalize();
            this.setDeltaMovement(v.add(want.subtract(v).scale(TURN)).normalize().scale(SPEED));
        }
        int life = this.isSpark() ? SPARK_LIFE : this.isShard() ? SHARD_LIFE : LIFE + this.launch;
        if (this.tickCount > life) {
            this.burst(this.position());
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !Wizard.ally(entity) && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity living) {
            Vec3 push = this.getDeltaMovement().normalize().scale(0.4);
            if (living.hurt(Wizard.source(this, this.getOwner(), WizardEntities.ARCANE), this.damage)) {
                living.push(push.x, 0.15, push.z);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.burst(result.getLocation());
        }
    }

    private void burst(Vec3 at) {
        if (this.level() instanceof ServerLevel level) {
            ParticleFx.send(level, Wizard.dust(this.colour(), 1.3F), at.x, at.y + 0.2, at.z, 16, 0.25, 0.25, 0.25,
                    0.0);
            ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y + 0.2, at.z, 8, 0.05, 0.05, 0.05, 0.12);
            ParticleFx.send(level, ParticleTypes.WITCH, at.x, at.y + 0.2, at.z, 6, 0.2, 0.2, 0.2, 0.05);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 0.9F,
                    1.4F);
        }
        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) {
            this.damage = tag.getFloat("Damage");
        }
    }
}
