package nl.tivek.multiversepowers.creature.animal;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

// A wild animal that leaves you be until it is given cause, then stays angry at whoever gave it for a while, as the
// game's own wolves and polar bears do.
public abstract class NeutralAnimal extends WildAnimal implements NeutralMob {
    private static final UniformInt ANGER = TimeUtil.rangeOfSeconds(20, 39);
    private int angerLeft;
    @Nullable
    private UUID angerAt;

    protected NeutralAnimal(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.readPersistentAngerSaveData(this.level(), tag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.addPersistentAngerSaveData(tag);
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(ANGER.sample(this.random));
    }

    @Override
    public void setRemainingPersistentAngerTime(int time) {
        this.angerLeft = time;
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return this.angerLeft;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID target) {
        this.angerAt = target;
    }

    @Nullable
    @Override
    public UUID getPersistentAngerTarget() {
        return this.angerAt;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            this.updatePersistentAnger((ServerLevel) this.level(), true);
        }
    }
}
