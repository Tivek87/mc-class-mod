package nl.tivek.multiversepowers.creature;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

// A zombie of the mod's own that makes its moves on one timeline in every game: the server starts a move (start) and
// every game that sees it learns which by an entity event and draws it by how long it has been going (moveAge). Out
// in daylight with nothing on its head it catches fire, as a zombie does.
public abstract class MovingMonster extends Monster {
    // The entity events that tell every game a move starts (kinds after FIRST_EVENT).
    private static final byte FIRST_EVENT = 100;

    private int move;
    private int moveAt;

    protected MovingMonster(EntityType<? extends MovingMonster> type, Level level) {
        super(type, level);
    }

    // How many kinds of move it has (1 to kinds; 0 is none).
    protected abstract int kinds();

    // The move it makes now in this game (0 none) and how many ticks it is into it.
    public int move() {
        return this.move;
    }

    public double moveAge(float partialTick) {
        return this.tickCount - this.moveAt + partialTick;
    }

    // Starts a move here and in every game that sees it (0 ends it).
    public void start(int kind) {
        this.move = kind;
        this.moveAt = this.tickCount;
        this.level().broadcastEntityEvent(this, (byte) (FIRST_EVENT + kind));
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= FIRST_EVENT && id <= FIRST_EVENT + this.kinds()) {
            this.move = id - FIRST_EVENT;
            this.moveAt = this.tickCount;
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        if (this.isAlive() && this.isSunBurnTick() && this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            this.igniteForSeconds(8.0F);
        }
        super.aiStep();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return super.causeFallDamage(distance - 2.0F, multiplier, source);
    }
}
