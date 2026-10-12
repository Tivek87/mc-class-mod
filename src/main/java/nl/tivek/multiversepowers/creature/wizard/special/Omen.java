package nl.tivek.multiversepowers.creature.wizard.special;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;

// A sign in the sky over the Wizard while a phase's special lasts, kept over his head by every game: the Voidborn's
// black sun (ECLIPSE), a disc of darkness in a burning magenta ring, and the Eternal's golden clock (CLOCK), its hands
// stopped as time stands still and running again before it shatters. Drawn by client/special/OmenRenderer.
public final class Omen extends Entity {
    public static final int ECLIPSE = 0;
    public static final int CLOCK = 1;
    // How high over him it hangs.
    public static final double HIGH = 9.0;
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(Omen.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(Omen.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(Omen.class,
            EntityDataSerializers.INT);

    public Omen(EntityType<? extends Omen> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void raise(ServerLevel level, Wizard wizard, int kind, int life) {
        Omen omen = new Omen(WizardEntities.OMEN.get(), level);
        omen.entityData.set(OWNER, wizard.getId());
        omen.entityData.set(KIND, kind);
        omen.entityData.set(LIFE, life);
        Vec3 at = over(wizard);
        omen.setPos(at.x, at.y, at.z);
        level.addFreshEntity(omen);
    }

    private static Vec3 over(Entity wizard) {
        return wizard.position().add(0.0, HIGH + (wizard instanceof Wizard caster ? caster.hover() : 0.0), 0.0);
    }

    public int kind() {
        return Mth.clamp(this.entityData.get(KIND), ECLIPSE, CLOCK);
    }

    public int life() {
        return this.entityData.get(LIFE);
    }

    @Nullable
    private Entity owner() {
        return this.level().getEntity(this.entityData.get(OWNER));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, -1);
        builder.define(KIND, ECLIPSE);
        builder.define(LIFE, 60);
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = this.owner();
        if (owner != null) {
            Vec3 at = over(owner);
            this.setPos(at.x, at.y, at.z);
        }
        if (!this.level().isClientSide && (owner == null || !owner.isAlive() || this.tickCount > this.life())) {
            this.discard();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160.0 * 160.0;
    }
}
