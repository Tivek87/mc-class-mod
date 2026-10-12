package nl.tivek.multiversepowers.creature.animal.snake.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.snake.Snake;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the snake moves: bends run down its body from head to tail as it slithers, wider the faster it goes; lying
// still it keeps a loose curve of its own; its tongue flicks out now and then; and its acts: coiled with its neck
// raised in an S and its head level, the tail tip quivering; the strike, the neck thrown straight and the jaws wide;
// basking in lazy curves; and lifting its head high to look about.
final class SnakePoses {
    // How far the body bends at each joint (from the head back), to its left.
    static final int BEND = 0;
    // How far each joint before the middle lifts what lies ahead of it (the head's own first).
    static final int RISE = SnakeModel.JOINTS;
    static final int GAPE = RISE + SnakeModel.FRONT;
    static final int TONGUE = GAPE + 1;
    static final int LUNGE = GAPE + 2;
    static final int LOOK = GAPE + 3;
    private static final int COUNT = LOOK + 1;
    private static final float PACE = 1.5F;
    private static final float WAVE = 0.95F;
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);

    Channels of(Snake snake, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        int id = snake.getId();
        float a = Mth.clamp(amount * 1.6F, 0.0F, 1.0F);
        float phase = limbSwing * PACE;
        float seed = (id * 37 % 100) * 0.0628F;
        for (int j = 0; j < SnakeModel.JOINTS; j++) {
            float lying = 0.32F * Mth.cos(j * 0.9F + seed);
            float going = 0.55F * Mth.cos(phase - j * WAVE);
            c[BEND + j] = Mth.lerp(a, lying, going);
        }
        c[LOOK] = 1.0F;
        c[TONGUE] = Channels.twitch(age, id, 52, 9.0F);
        for (int kind = Snake.COIL; kind <= Snake.PERISCOPE; kind++) {
            float w = snake.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) snake.actAge(kind, partialTick), age, id), w);
            }
        }
        return p;
    }

    private Channels act(int kind, float t, float age, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        switch (kind) {
            case Snake.COIL -> {
                coil(c, age, id, 0.0F);
                c[TONGUE] = Channels.twitch(age, id, 15, 7.0F);
            }
            case Snake.STRIKE -> {
                float lunge = Trunk.in(t, 0.0, Snake.STRIKE_HIT) * (1.0F - Trunk.in(t, Snake.STRIKE_HIT + 1.0,
                        Snake.STRIKE_END));
                coil(c, age, id, lunge);
                c[GAPE] = Trunk.in(t, 0.0, 3.0) * (1.0F - Trunk.in(t, Snake.STRIKE_HIT + 1.0, 9.0));
                c[LUNGE] = -4.0F * lunge;
                c[TONGUE] = 0.0F;
            }
            case Snake.BASK -> {
                float seed = (id * 53 % 100) * 0.0628F;
                for (int j = 0; j < SnakeModel.JOINTS; j++) {
                    c[BEND + j] = 0.5F * Mth.cos(j * 0.75F + seed);
                }
                c[TONGUE] = Channels.twitch(age, id, 130, 9.0F);
                c[LOOK] = 0.3F;
            }
            case Snake.PERISCOPE -> {
                c[RISE + 3] = -0.6F;
                c[RISE + 2] = -0.5F;
                c[RISE + 1] = 0.35F;
                c[RISE] = 0.7F;
                c[BEND] = 0.5F * Mth.sin(t * 0.07F);
                c[TONGUE] = Channels.twitch(age, id, 24, 8.0F);
                c[LOOK] = 0.5F;
            }
            default -> {
            }
        }
        return p;
    }

    // Coiled: the body wound in a spiral behind, the neck raised in an S with the head level; `lunge` (0 to 1)
    // throws the neck out straight.
    private static void coil(float[] c, float age, int id, float lunge) {
        float side = id % 2 == 0 ? 1.0F : -1.0F;
        float[] wound = { 0.0F, 0.15F, 0.35F, 0.6F, 0.95F, 0.95F, 0.9F, 0.8F, 0.6F };
        for (int j = 0; j < SnakeModel.JOINTS; j++) {
            c[BEND + j] = side * wound[j] * (j < SnakeModel.FRONT ? 1.0F - lunge : 1.0F);
        }
        // The tail tip quivers.
        c[BEND + 8] += 0.45F * Mth.sin(age * 3.1F);
        float held = 1.0F - 0.85F * lunge;
        c[RISE + 3] = -0.75F * held;
        c[RISE + 2] = -0.55F * held;
        c[RISE + 1] = 0.35F * held;
        c[RISE] = 0.8F * held + 0.05F * Mth.sin(age * 0.2F);
        c[LOOK] = 1.0F;
    }
}
