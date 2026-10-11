package nl.tivek.multiversepowers.creature.screamer.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;
import nl.tivek.multiversepowers.creature.screamer.Screamer;
import nl.tivek.multiversepowers.creature.screamer.ScreamerMoves;

// How the Screamer holds itself this frame (ScreamerModel): hunched, knees bent, arms hanging forward, a limping,
// jerky shamble; fits of teeth clicking (Screamer.chatter); twitches that snap its head and a shoulder aside, laid on
// unsmoothed so they jerk. On top of that its moves on the server's timeline: a bite, a screech with its head thrown
// back and its jaw unhinged down to its chest, and a burst with its arms flung wide. Arms and legs turn forward with a
// negative angle; a positive lean bends the trunk forward.
final class ScreamerPoses {
    private static final PoseSprings SPRINGS = new PoseSprings(0.32, 0.75);

    float lean;
    float sink;
    float twist;
    float roll;
    float nod;
    float shake;
    float tilt;
    float jaw;
    float drop;
    final float[] arm = new float[2];
    final float[] armOut = new float[2];
    final float[] elbow = new float[2];
    final float[] leg = new float[2];
    final float[] knee = new float[2];

    void of(Screamer screamer, float limbSwing, float amount, float age) {
        this.lean = ScreamerModel.LEAN;
        this.sink = 0.6F;
        this.twist = 0.0F;
        this.roll = 0.0F;
        this.nod = -0.15F;
        this.shake = 0.0F;
        this.tilt = 0.0F;
        this.jaw = 0.12F;
        this.drop = 0.0F;
        for (int s = 0; s < 2; s++) {
            this.arm[s] = -0.2F;
            this.armOut[s] = 0.0F;
            this.elbow[s] = 0.35F;
            this.leg[s] = -0.15F;
            this.knee[s] = 0.3F;
        }
        int move = screamer.move();
        double t = screamer.moveAge(age - screamer.tickCount);
        int id = screamer.getId();
        this.walk(limbSwing, amount);
        switch (move) {
            case ScreamerMoves.BITE -> this.bite(t);
            case ScreamerMoves.SCREECH -> this.screech(t, age);
            case ScreamerMoves.BURST -> this.burst(t);
            default -> {
            }
        }
        float[] c = { this.lean, this.sink, this.twist, this.roll, this.nod, this.shake, this.tilt, this.jaw, this.drop,
                this.arm[0], this.arm[1], this.armOut[0], this.armOut[1], this.elbow[0], this.elbow[1], this.leg[0],
                this.leg[1], this.knee[0], this.knee[1] };
        SPRINGS.follow(id, c, age, screamer.level().getGameTime());
        this.lean = c[0];
        this.sink = c[1];
        this.twist = c[2];
        this.roll = c[3];
        this.nod = c[4];
        this.shake = c[5];
        this.tilt = c[6];
        this.jaw = Math.max(0.0F, c[7]);
        this.drop = Math.max(0.0F, c[8]);
        for (int s = 0; s < 2; s++) {
            this.arm[s] = c[9 + s];
            this.armOut[s] = c[11 + s];
            this.elbow[s] = Math.max(0.0F, c[13 + s]);
            this.leg[s] = c[15 + s];
            this.knee[s] = Math.max(0.0F, c[17 + s]);
        }
        this.chatter(Screamer.chatter(screamer, age));
        if (move == 0) {
            this.twitch(age, id);
        }
    }

    // A limping shamble: one leg drags, so its weight drops hard onto the other; the arms dangle and swing loose.
    private void walk(float limbSwing, float amount) {
        float phase = limbSwing * 0.62F;
        float w = Math.min(1.0F, amount * 1.5F);
        float limp = phase + 0.35F * Mth.sin(phase);
        float c = Mth.cos(limp);
        float s = Mth.sin(limp);
        this.leg[0] += 0.7F * w * c;
        this.leg[1] -= 0.5F * w * c;
        this.knee[0] += 0.6F * w * Math.max(0.0F, -s);
        this.knee[1] += 0.3F * w * Math.max(0.0F, s);
        this.arm[0] -= 0.35F * w * c;
        this.arm[1] += 0.3F * w * c;
        this.roll += 0.12F * w * Math.max(0.0F, s);
        this.sink += 1.2F * w * Math.max(0.0F, s);
        this.twist += 0.1F * w * c;
        this.tilt += 0.1F * w * s;
    }

    // Teeth clicking: the jaw snapping open and shut every other tick.
    private void chatter(float into) {
        if (into < 0.0F) {
            return;
        }
        float on = Trunk.in(into, 0.0, 2.0) * (1.0F - Trunk.in(into, 11.0, 14.0));
        this.jaw += on * 0.22F * (0.5F + 0.5F * Mth.cos(into * Mth.PI));
        this.shake += on * 0.04F * Mth.sin(into * 2.3F);
    }

    // Now and then a jerk snaps its head over and a shoulder up, and lets go again.
    private void twitch(float age, int id) {
        int slot = Mth.floor(age / 9.0F);
        int hash = slot * 73_856_093 ^ id * 19_349_663;
        if ((hash & 3) != 0) {
            return;
        }
        float into = age - slot * 9.0F;
        float jerk = into < 1.0F ? into : Math.max(0.0F, 1.0F - (into - 1.0F) / 5.0F);
        float way = (hash >> 2 & 1) == 0 ? 1.0F : -1.0F;
        this.tilt += way * 0.55F * jerk;
        this.shake += way * 0.3F * jerk;
        int s = hash >> 3 & 1;
        this.armOut[s] += 0.35F * jerk;
        this.arm[s] -= 0.3F * jerk;
        this.roll += way * 0.06F * jerk;
    }

    // Its head goes back and its jaw opens as it lunges, then it snaps its teeth shut on what is in front of it.
    private void bite(double t) {
        float draw = Trunk.in(t, 0.0, ScreamerMoves.BITE_HIT - 1) * (1.0F - Trunk.in(t, ScreamerMoves.BITE_HIT - 1,
                ScreamerMoves.BITE_HIT));
        float lunge = Trunk.in(t, ScreamerMoves.BITE_HIT - 1, ScreamerMoves.BITE_HIT) * (1.0F - Trunk.in(t,
                ScreamerMoves.BITE_HIT + 2, ScreamerMoves.BITE_END));
        float open = Trunk.in(t, 0.0, ScreamerMoves.BITE_HIT - 1) * (1.0F - Trunk.in(t, ScreamerMoves.BITE_HIT - 0.5,
                ScreamerMoves.BITE_HIT));
        this.lean += -0.15F * draw + 0.45F * lunge;
        this.nod += -0.4F * draw + 0.35F * lunge;
        this.jaw += 0.8F * open;
        this.drop += 1.0F * open;
        for (int s = 0; s < 2; s++) {
            this.arm[s] += -0.9F * draw - 0.5F * lunge;
            this.armOut[s] += 0.25F * draw - 0.2F * lunge;
            this.elbow[s] += 0.3F * draw;
        }
    }

    // Arched back with its head thrown up, it draws breath as its jaw slides open; then it thrusts its head out and
    // screams, jaw unhinged down to its chest, arms flung back, every bit of it shaking.
    private void screech(double t, float age) {
        float draw = Trunk.in(t, 0.0, ScreamerMoves.SCREAM - 2) * (1.0F - Trunk.in(t, ScreamerMoves.SCREAM - 2,
                ScreamerMoves.SCREAM));
        float scream = Trunk.in(t, ScreamerMoves.SCREAM - 2, ScreamerMoves.SCREAM) * (1.0F - Trunk.in(t,
                ScreamerMoves.SCREAM_END, ScreamerMoves.SCREECH_END));
        float shake = scream * Mth.sin(age * 4.7F);
        this.lean += -0.35F * draw + 0.25F * scream;
        this.nod += -0.75F * draw + 0.05F * scream;
        this.jaw += 0.55F * draw + 1.2F * scream;
        this.drop += 2.0F * draw + 5.0F * scream;
        this.shake += 0.07F * shake;
        this.roll += 0.03F * shake;
        this.sink += 0.8F * scream;
        for (int s = 0; s < 2; s++) {
            this.arm[s] += 0.55F * draw + 0.75F * scream;
            this.armOut[s] += 0.45F * draw + 0.8F * scream;
            this.elbow[s] += 0.4F * draw - 0.2F * scream;
            this.knee[s] += 0.35F * scream;
        }
    }

    // Hunched in on itself with its arms drawn across its chest, it throws itself open: arms flung wide, head back,
    // mouth wide.
    private void burst(double t) {
        float gather = Trunk.in(t, 0.0, ScreamerMoves.BLAST - 1) * (1.0F - Trunk.in(t, ScreamerMoves.BLAST - 1,
                ScreamerMoves.BLAST));
        float open = Trunk.in(t, ScreamerMoves.BLAST - 1, ScreamerMoves.BLAST) * (1.0F - Trunk.in(t,
                ScreamerMoves.BLAST + 3, ScreamerMoves.BURST_END));
        this.lean += 0.4F * gather - 0.45F * open;
        this.nod += 0.3F * gather - 0.6F * open;
        this.jaw += 1.1F * open;
        this.drop += 3.5F * open;
        this.sink += 1.5F * gather;
        for (int s = 0; s < 2; s++) {
            this.arm[s] += -0.8F * gather + 0.4F * open;
            this.armOut[s] += -0.5F * gather + 1.3F * open;
            this.elbow[s] += 1.2F * gather - 0.3F * open;
            this.knee[s] += 0.6F * gather;
        }
    }
}
