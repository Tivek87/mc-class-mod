package nl.tivek.multiversepowers.engine.client.pose;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

// How a body is walking now, for what it carries to move with its steps: where in its stride it is (`phase`, radians,
// a whole stride each 2π, the legs as the game swings them), how much it walks (`amount`, 0 standing to 1 at full
// walking speed) and how much of that is a sprint (`sprint`, 0 to 1), each eased so a start or stop never jumps.
public record Gait(float phase, float amount, float sprint) {
    public static final Gait STILL = new Gait(0.0F, 0.0F, 0.0F);
    // The game swings legs by this much of a turn per step of its walk animation.
    private static final float STRIDE = 0.6662F;

    private static final Map<Integer, float[]> EASED = new HashMap<>();

    public static Gait of(LivingEntity entity, float partialTick) {
        float speed = Mth.clamp(entity.walkAnimation.speed(partialTick) * 1.4F, 0.0F, 1.0F);
        float phase = entity.walkAnimation.position(partialTick) * STRIDE;
        float sprinting = entity.isSprinting() && speed > 0.3F ? 1.0F : 0.0F;
        // Sprinting eased in and out over a few frames per entity: the game flips it at once.
        float[] eased = EASED.computeIfAbsent(entity.getId(), id -> new float[] { sprinting, entity.tickCount });
        float now = entity.tickCount + partialTick;
        float step = Mth.clamp(now - eased[1], 0.0F, 4.0F);
        eased[1] = now;
        eased[0] = Mth.lerp(1.0F - (float) Math.pow(0.7, step), eased[0], sprinting);
        if (EASED.size() > 256) {
            EASED.clear();
        }
        return new Gait(phase, speed, eased[0]);
    }

    public Gait scaled(float by) {
        return new Gait(this.phase, this.amount * by, this.sprint * by);
    }

    // The bounce of each step: 0 at the top, 1 at the bottom, twice a stride.
    public float bounce() {
        return this.amount * (0.5F - 0.5F * Mth.cos(this.phase * 2.0F));
    }

    // The sway from side to side with the legs, -1 to 1 once a stride.
    public float sway() {
        return this.amount * Mth.sin(this.phase);
    }
}
