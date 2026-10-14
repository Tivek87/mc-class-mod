package nl.tivek.multiversepowers.creature.animal.snake.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.snake.Snake;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the snake moves: bends run down its body from head to tail as it slithers, as fast as it goes forward, so each
// curve stays where it is on the ground while the body slides through it, wider toward the tail and the faster it
// goes, its head held a little up and facing ahead; lying still it keeps a loose curve of its own; its tongue flicks
// out twice at a time now and then; and its acts: coiled with its neck raised in an S and its head level, the tail tip
// quivering; the strike, the neck thrown straight and the jaws wide; basking in lazy curves; and lifting its head high
// to look about. The whole pose follows on springs.
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
    // The bend turns this much from one joint to the next (each piece two pixels long): one curve every 13 pixels.
    private static final float WAVE = 0.95F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 0.6, 0.6, 1.0);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);

    Channels of(Snake snake, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        int id = snake.getId();
        float a = Mth.clamp(amount * 1.6F, 0.0F, 1.0F);
        // The wave runs back along the body as fast as the body goes forward: one curve (13 pixels) a stride.
        float phase = this.strides.phase(id, age, snake.level().getGameTime(),
                Strides.rate(Mth.clamp(amount, 0.0F, 1.0F), 1.0F, 2.0F * Mth.TWO_PI / WAVE));
        float seed = (id * 37 % 100) * 0.0628F;
        for (int j = 0; j < SnakeModel.JOINTS; j++) {
            float lying = 0.25F * Mth.cos(j * 0.8F + seed);
            float wide = 0.4F + 0.25F * j / (SnakeModel.JOINTS - 1.0F);
            float going = wide * Mth.cos(phase - j * WAVE);
            c[BEND + j] = Mth.lerp(a, lying, going);
        }
        c[RISE] = 0.12F * a;
        c[RISE + 1] = -0.1F * a;
        c[LOOK] = 1.0F;
        // Two quick flicks, then a rest.
        c[TONGUE] = Math.max(Channels.twitch(age, id, 52, 5.0F), Channels.twitch(age - 7.0F, id, 52, 5.0F));
        for (int kind = Snake.COIL; kind <= Snake.PERISCOPE; kind++) {
            float w = snake.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) snake.actAge(kind, partialTick), age, id), w);
            }
        }
        float tongue = c[TONGUE];
        float lunge = c[LUNGE];
        float gape = c[GAPE];
        this.springs.follow(id, c, age, snake.level().getGameTime());
        c[TONGUE] = tongue;
        c[LUNGE] = lunge;
        c[GAPE] = gape;
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
                c[RISE + 4] = -0.5F;
                c[RISE + 3] = -0.45F;
                c[RISE + 2] = -0.1F;
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
        float[] wound = { 0.0F, 0.1F, 0.22F, 0.4F, 0.6F, 0.75F, 0.75F, 0.72F, 0.7F, 0.66F, 0.6F, 0.5F, 0.4F };
        for (int j = 0; j < SnakeModel.JOINTS; j++) {
            c[BEND + j] = side * wound[j] * (j < SnakeModel.FRONT ? 1.0F - lunge : 1.0F);
        }
        // The tail tip quivers.
        c[BEND + SnakeModel.JOINTS - 1] += 0.45F * Mth.sin(age * 3.1F);
        float held = 1.0F - 0.85F * lunge;
        c[RISE + 4] = -0.55F * held;
        c[RISE + 3] = -0.5F * held;
        c[RISE + 2] = -0.25F * held;
        c[RISE + 1] = 0.45F * held;
        c[RISE] = 0.85F * held + 0.05F * Mth.sin(age * 0.2F);
        c[LOOK] = 1.0F;
    }
}
