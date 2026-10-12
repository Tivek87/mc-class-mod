package nl.tivek.multiversepowers.creature.wizard.field;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.WizardKnock;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// Glacial spikes, his second phase: his staff driven into the ground, a line of ice spikes bursts up out of it from his
// feet towards his foe, running fast along the ground, and at its end a crown of great spikes. Whoever it runs under
// is struck, frozen and slowed and tossed up; it stops at a wall or an edge. The spikes stand a moment and sink. Drawn
// by client/field/SpikesRenderer, which reads its way from its turn and how far it ran from LENGTH.
public final class GlacialSpikes extends FieldSpell {
    public static final double RANGE = 22.0;
    public static final double SPEED = 0.95;
    // Ticks the crown stands once the line has run, and its reach.
    public static final int STAND = 22;
    public static final double CROWN = 2.8;
    private static final double WIDE = 1.3;
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(GlacialSpikes.class,
            EntityDataSerializers.FLOAT);
    private final Set<Integer> struck = new HashSet<>();
    private boolean crowned;

    public GlacialSpikes(EntityType<? extends GlacialSpikes> type, Level level) {
        super(type, level);
    }

    // A line from `ground` (under him) towards `to`.
    public static void run(ServerLevel level, Wizard wizard, Vec3 ground, Vec3 to, float damage) {
        GlacialSpikes spikes = new GlacialSpikes(WizardEntities.GLACIAL.get(), level);
        Vec3 way = to.subtract(ground).multiply(1.0, 0.0, 1.0);
        float yaw = way.lengthSqr() < 1.0E-4 ? wizard.getYRot() : (float) (Mth.atan2(-way.x, way.z) * Mth.RAD_TO_DEG);
        spikes.moveTo(ground.x, ground.y, ground.z, yaw, 0.0F);
        spikes.castBy(wizard, damage);
        spikes.entityData.set(LENGTH, (float) RANGE);
        level.addFreshEntity(spikes);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.6F, 0.6F);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.POWDER_SNOW_BREAK, SoundSource.HOSTILE, 2.0F,
                0.5F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LENGTH, (float) RANGE);
    }

    // How far the line runs before it stops.
    public float length() {
        return this.entityData.get(LENGTH);
    }

    // The way it runs, flat.
    public Vec3 way() {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    // How far its head has run `age` ticks in.
    public double head(float age) {
        return Math.min(this.length(), age * SPEED);
    }

    // Ticks until the line has run its whole length, and until it is gone.
    public int ran() {
        return Mth.ceil(this.length() / SPEED);
    }

    public int life() {
        return this.ran() + STAND;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Wizard wizard = this.owner(level);
        if (wizard == null || this.tickCount > this.life()) {
            this.discard();
            return;
        }
        Vec3 way = this.way();
        double was = this.head(this.tickCount - 1);
        double head = this.head(this.tickCount);
        if (head > was && this.length() >= RANGE - 1.0E-3 && this.blocked(level, this.position().add(way.scale(head)))) {
            this.entityData.set(LENGTH, (float) Math.max(1.0, was));
            head = was;
        }
        if (head > was) {
            Vec3 at = this.position().add(way.scale(head));
            for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at).inflate(WIDE + 1.0, 2.0,
                    WIDE + 1.0))) {
                this.strike(level, wizard, living, at, WIDE, 1.0F);
            }
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                    at.x, at.y + 0.3, at.z, 8, 0.4, 0.2, 0.4, 0.1);
            ParticleFx.send(level, ParticleTypes.SNOWFLAKE, at.x, at.y + 0.6, at.z, 4, 0.3, 0.4, 0.3, 0.03);
            if (this.tickCount % 3 == 0) {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.8F,
                        0.8F + 0.4F * this.random.nextFloat());
            }
        }
        if (!this.crowned && this.tickCount >= this.ran()) {
            this.crowned = true;
            Vec3 end = this.position().add(way.scale(this.length()));
            this.struck.clear();
            for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(end, end).inflate(CROWN + 1.0, 3.0,
                    CROWN + 1.0))) {
                this.strike(level, wizard, living, end, CROWN, 1.3F);
            }
            ParticleFx.sphereOut(level, Wizard.dust(Wizard.ICE, 1.8F), end.add(0.0, 1.0, 0.0), 40, 0.4);
            ParticleFx.shockwave(level, ParticleTypes.SNOWFLAKE, end.add(0.0, 0.3, 0.0), 40, 0.5);
            level.playSound(null, end.x, end.y, end.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.0F,
                    0.5F);
            level.playSound(null, end.x, end.y, end.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.5F,
                    0.6F);
        }
    }

    private void strike(ServerLevel level, Wizard wizard, LivingEntity living, Vec3 at, double reach, float harm) {
        double far = Math.hypot(living.getX() - at.x, living.getZ() - at.z) - living.getBbWidth() * 0.5;
        double over = living.getY() - at.y;
        if (far > reach || over > 1.6 || over < -2.0 || !this.struck.add(living.getId())) {
            return;
        }
        if (MonsterBlows.hurt(living, Wizard.source(this, wizard, WizardEntities.FROST), this.damage * harm,
                WizardKnock.knock(new Vec3(0.0, 0.85, 0.0)).add(this.way().scale(0.15)))) {
            living.setTicksFrozen(Math.max(living.getTicksFrozen(), 220));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2), wizard);
            Vec3 hit = living.getBoundingBox().getCenter();
            ParticleFx.send(level, ParticleTypes.SNOWFLAKE, hit.x, hit.y, hit.z, 16, 0.3, 0.5, 0.3, 0.05);
        }
    }

    // A wall before its head, or no ground under it: the line stops there.
    private boolean blocked(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at.x, at.y + 0.5, at.z);
        if (!level.isLoaded(pos)) {
            return true;
        }
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
            return true;
        }
        for (int down = 1; down <= 3; down++) {
            BlockPos below = pos.below(down);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
