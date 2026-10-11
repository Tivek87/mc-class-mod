package nl.tivek.multiversepowers.creature.wizard.field;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import nl.tivek.multiversepowers.creature.wizard.Wizard;

// A spell of the Wizard's laid on a place for a few seconds: it knows who cast it and how hard it strikes, and is never
// saved with the world.
abstract class FieldSpell extends Entity {
    @Nullable
    private UUID owner;
    protected float damage;

    FieldSpell(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    void castBy(Wizard wizard, float damage) {
        this.owner = wizard.getUUID();
        this.damage = damage;
    }

    // Its caster, while he is alive.
    @Nullable
    Wizard owner(ServerLevel level) {
        Entity owner = this.owner == null ? null : level.getEntity(this.owner);
        return owner instanceof Wizard wizard && wizard.isAlive() ? wizard : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
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
