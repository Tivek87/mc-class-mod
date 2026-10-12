package nl.tivek.multiversepowers.creature.animal.crocodile.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.crocodile.Crocodile;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the crocodile moves: at rest on land it lies on its belly, legs splayed; walking it lifts itself onto its legs,
// a forefoot and the hind foot across from it together, its body and tail bending from side to side; afloat its legs
// are laid back along its body and its tail drives it with slow sweeps; and its acts: the warning (head up, jaws
// wide, tail curled to one side), the bite (a lunge, the jaws snapped shut, the head shaken), basking flat with its
// jaws agape, and raised on its forelegs to look about.
final class CrocodilePoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int BOB = 2;
    static final int SINK = 3;
    static final int LUNGE = 4;
    static final int SWAY = 5;
    static final int HEAD = 6;
    static final int HEAD_YAW = 7;
    static final int JAW = 8;
    // Each tail piece's turn to the side, from the body out.
    static final int TAIL = 9;
    static final int TAIL_PIECES = 4;
    // How far the tail slopes down to lie on the ground: 1 standing on land, 0 afloat.
    static final int DROOP = 13;
    static final int LOOK = 14;
    // Per leg (right front, left front, right hind, left hind): swung back, and lifted.
    static final int LEGS = 15;
    static final int PER_LEG = 2;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final float PACE = 1.1F;
    // How far a leg is lifted for the belly to lie on the ground.
    private static final float SPLAY = 0.9F;
    private static final float BELLY = 2.5F;
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private float swim;

    Channels of(Crocodile crocodile, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        this.swim = Mth.lerp(partialTick, crocodile.oSwim, crocodile.swim);
        float land = 1.0F - this.swim;
        // It walks slowly: any walk at all has it up on its legs.
        float a = Mth.clamp(amount * 4.0F, 0.0F, 1.0F);
        float walk = a * land;
        float rest = (1.0F - a) * land;
        float phase = limbSwing * PACE;
        c[LOOK] = 1.0F;
        c[SINK] = BELLY * rest;
        c[DROOP] = land * (1.0F - 0.55F * rest);
        float[] at = { 0.0F, Mth.PI, Mth.PI, 0.0F };
        for (int k = 0; k < 4; k++) {
            int leg = LEGS + k * PER_LEG;
            float w = phase + at[k];
            c[leg] = 0.55F * walk * Channels.swing(w) + 1.25F * this.swim;
            c[leg + 1] = 0.4F * walk * Channels.lift(w) + SPLAY * rest + 0.25F * this.swim;
        }
        c[SWAY] = 0.1F * walk * Mth.sin(phase);
        c[BOB] = 0.3F * walk * Mth.cos(2.0F * phase) + 0.2F * this.swim * Mth.sin(age * 0.07F);
        // The tail: a wave running down it, small on land, wide and slow in the water.
        float beat = age * 0.16F + limbSwing * 0.9F;
        for (int k = 0; k < TAIL_PIECES; k++) {
            c[TAIL + k] = -0.14F * walk * Mth.sin(phase - 0.7F * (k + 1))
                    + this.swim * (0.1F + 0.06F * k) * (1.0F + 2.0F * a) * Mth.sin(beat - 0.8F * k)
                    + 0.05F * rest * Mth.sin(age * 0.03F + k);
        }
        c[HEAD_YAW] = -0.08F * walk * Mth.sin(phase);
        for (int kind = Crocodile.GAPE; kind <= Crocodile.RISE; kind++) {
            float w = crocodile.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) crocodile.actAge(kind, partialTick), crocodile.getId()), w);
            }
        }
        return p;
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        float land = 1.0F - this.swim;
        float side = id % 2 == 0 ? 1.0F : -1.0F;
        switch (kind) {
            case Crocodile.GAPE -> {
                // Up on its legs, head lifted and jaws wide; the tail drawn round to one side, its tip twitching.
                stand(c, land);
                c[PITCH] = -0.08F;
                c[HEAD] = -0.4F + 0.03F * Mth.sin(t * 0.5F);
                c[JAW] = 0.85F + 0.05F * Mth.sin(t * 0.5F);
                for (int k = 0; k < TAIL_PIECES; k++) {
                    c[TAIL + k] = side * (0.28F + 0.08F * (k == TAIL_PIECES - 1 ? Mth.sin(t * 0.9F) : 0.0F));
                }
                c[LOOK] = 1.0F;
            }
            case Crocodile.BITE -> {
                float open = Trunk.in(t, 0.0, 3.0) * (1.0F - Trunk.in(t, Crocodile.BITE_HIT - 1.0, Crocodile.BITE_HIT));
                float out = Trunk.in(t, 1.0, Crocodile.BITE_HIT) * (1.0F - Trunk.in(t, 8.0, Crocodile.BITE_END));
                float shake = Trunk.in(t, Crocodile.BITE_HIT, 7.0) * (1.0F - Trunk.in(t, 11.0, 15.0));
                stand(c, land);
                c[LUNGE] = 5.0F * out;
                c[HEAD] = -0.3F * open;
                c[JAW] = 0.95F * open;
                c[HEAD_YAW] = 0.45F * shake * Mth.sin(t * 1.7F);
                c[SWAY] = 0.12F * shake * Mth.sin(t * 1.7F);
                for (int k = 0; k < TAIL_PIECES; k++) {
                    c[TAIL + k] = -0.2F * shake * Mth.sin(t * 1.7F - 0.6F * k);
                }
                c[LOOK] = 1.0F;
            }
            case Crocodile.BASK -> {
                // Flat on its belly in the sun, jaws held open to cool.
                c[SINK] = BELLY * land;
                c[DROOP] = 0.45F * land;
                for (int k = 0; k < 4; k++) {
                    c[LEGS + k * PER_LEG] = k < 2 ? -0.3F : 0.5F;
                    c[LEGS + k * PER_LEG + 1] = SPLAY * land;
                }
                c[HEAD] = -0.22F;
                c[JAW] = 0.5F + 0.02F * Mth.sin(t * 0.05F);
                c[HEAD_YAW] = 0.0F;
                for (int k = 0; k < TAIL_PIECES; k++) {
                    c[TAIL + k] = side * 0.16F;
                }
                c[LOOK] = 0.0F;
            }
            case Crocodile.RISE -> {
                // Front pushed up on straight forelegs, head high, turning slowly to look.
                stand(c, land);
                c[PITCH] = -0.24F;
                c[BOB] = -0.6F;
                c[HEAD] = 0.1F;
                c[HEAD_YAW] = 0.5F * Mth.sin(t * 0.07F);
                c[LOOK] = 0.3F;
            }
            default -> {
            }
        }
        return p;
    }

    // Up off its belly on all four legs.
    private static void stand(float[] c, float land) {
        c[SINK] = 0.0F;
        c[DROOP] = land;
        for (int k = 0; k < 4; k++) {
            c[LEGS + k * PER_LEG] *= 1.0F - land;
            c[LEGS + k * PER_LEG + 1] *= 1.0F - land;
        }
    }
}
