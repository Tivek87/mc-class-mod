package nl.tivek.multiversepowers.creature.stalker.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;
import nl.tivek.multiversepowers.creature.stalker.Stalker;
import nl.tivek.multiversepowers.creature.stalker.StalkerMoves;

// How the Stalker holds itself this frame (StalkerModel). Slow, it creeps: sunk low, leaning in, long careful steps
// with its claws held off the ground; fast, it runs bent double with its arms swept back. Pressed to a trunk it hugs it
// high with one arm, the other gripping its side, and now and then leans its head out round it to look; in a bush it is
// curled up tight on its haunches, only its head up. On top of that its moves on the server's timeline: the lunge
// (sinking onto its haunches, then flung forward with both claws), two swipes, the blink (shaking itself apart, then
// shaking itself together again elsewhere), throwing off a borrowed shape, folding down into a bush and unfolding out
// of it. Arms and legs turn forward with a negative angle; a positive lean bends the trunk forward.
final class StalkerPoses {
    private static final PoseSprings SPRINGS = new PoseSprings(0.3, 0.8);

    float lean;
    float sink;
    float ahead;
    float twist;
    float roll;
    float nod;
    float shake;
    float tilt;
    float jaw;
    float follow;
    final float[] arm = new float[2];
    final float[] armOut = new float[2];
    final float[] armIn = new float[2];
    final float[] elbow = new float[2];
    final float[] leg = new float[2];
    final float[] legOut = new float[2];
    final float[] knee = new float[2];

    void of(Stalker stalker, float limbSwing, float amount, float age) {
        this.lean = StalkerModel.LEAN;
        this.sink = 0.3F;
        this.ahead = 0.0F;
        this.twist = 0.0F;
        this.roll = 0.0F;
        this.nod = -0.1F;
        this.shake = 0.0F;
        this.tilt = 0.0F;
        this.jaw = 0.05F;
        this.follow = 1.0F;
        for (int s = 0; s < 2; s++) {
            this.arm[s] = -0.12F;
            this.armOut[s] = 0.02F;
            this.armIn[s] = 0.0F;
            this.elbow[s] = 0.25F;
            this.leg[s] = -0.1F;
            this.legOut[s] = 0.0F;
            this.knee[s] = 0.2F;
        }
        int move = stalker.move();
        double t = stalker.moveAge(age - stalker.tickCount);
        int id = stalker.getId();
        int hiding = stalker.hiding();
        if (hiding == Stalker.BUSH) {
            this.curl(1.0F, age, id);
        } else if (hiding == Stalker.TRUNK) {
            this.hug(age, id);
        } else {
            this.walk(limbSwing, amount);
        }
        switch (move) {
            case StalkerMoves.LUNGE -> this.lunge(t);
            case StalkerMoves.SWIPE -> this.swipe(t);
            case StalkerMoves.REVEAL -> this.reveal(t);
            case StalkerMoves.FOLD -> this.curl(Trunk.in(t, 0.0, StalkerMoves.FOLD_END), age, id);
            case StalkerMoves.RISE -> this.curl(1.0F - Trunk.in(t, 0.0, StalkerMoves.RISE_END), age, id);
            default -> {
            }
        }
        float[] c = { this.lean, this.sink, this.ahead, this.twist, this.roll, this.nod, this.shake, this.tilt,
                this.jaw, this.follow, this.arm[0], this.arm[1], this.armOut[0], this.armOut[1], this.armIn[0],
                this.armIn[1], this.elbow[0], this.elbow[1], this.leg[0], this.leg[1], this.legOut[0], this.legOut[1],
                this.knee[0], this.knee[1] };
        SPRINGS.follow(id, c, age, stalker.level().getGameTime());
        this.lean = c[0];
        this.sink = c[1];
        this.ahead = c[2];
        this.twist = c[3];
        this.roll = c[4];
        this.nod = c[5];
        this.shake = c[6];
        this.tilt = c[7];
        this.jaw = Math.max(0.0F, c[8]);
        this.follow = c[9];
        for (int s = 0; s < 2; s++) {
            this.arm[s] = c[10 + s];
            this.armOut[s] = c[12 + s];
            this.armIn[s] = c[14 + s];
            this.elbow[s] = Math.max(0.0F, c[16 + s]);
            this.leg[s] = c[18 + s];
            this.legOut[s] = c[20 + s];
            this.knee[s] = Math.max(0.0F, c[22 + s]);
        }
        if (move == StalkerMoves.BLINK) {
            this.blink(t, age);
        } else if (move == 0 && hiding == Stalker.OPEN) {
            this.twitch(age, id);
        }
    }

    // Slow it creeps, sunk low and leaning in with long careful steps, claws held up off the ground; fast it runs bent
    // double, arms swept back and its head thrust out.
    private void walk(float limbSwing, float amount) {
        float w = Math.min(1.0F, amount * 1.6F);
        float run = Trunk.in(amount, 0.55, 0.85);
        float creep = w * (1.0F - run);
        float phase = limbSwing * (0.45F + 0.2F * run);
        float c = Mth.cos(phase);
        float s = Mth.sin(phase);
        this.lean += 0.4F * creep + 0.75F * run;
        this.sink += 1.6F * creep + 1.0F * run;
        this.nod -= 0.35F * creep + 0.65F * run;
        for (int k = 0; k < 2; k++) {
            float way = k == 0 ? 1.0F : -1.0F;
            float swing = way * c;
            float lift = Math.max(0.0F, way * s);
            this.leg[k] += -0.45F * creep - 0.5F * run + (0.55F * creep + 0.95F * run) * swing;
            this.knee[k] += 0.75F * creep + 0.5F * run + (0.7F * creep + 1.2F * run) * lift;
            this.arm[k] += -0.55F * creep - (0.35F * creep) * swing + run * (0.6F - 0.9F * swing);
            this.elbow[k] += 0.9F * creep + 0.4F * run;
            this.armOut[k] += 0.12F * creep + 0.08F * run;
        }
        this.twist += (0.08F * creep + 0.14F * run) * c;
        this.roll += 0.04F * w * s;
        this.sink += 0.5F * run * Math.abs(s);
    }

    // Pressed to a trunk: one arm reaching up round it, the other gripping its side, and every few seconds its head
    // leaning out round it, slowly, to one side, to look.
    private void hug(float age, int id) {
        this.lean = 0.05F;
        this.sink += 0.4F;
        int slot = Mth.floor(age / 50.0F);
        int hash = slot * 73_856_093 ^ id * 19_349_663;
        float into = (age - slot * 50.0F) / 50.0F;
        float peek = (hash & 1) == 0 ? 0.0F : Mth.sin(Mth.clamp(into, 0.0F, 1.0F) * Mth.PI);
        peek = peek * peek * (3.0F - 2.0F * peek);
        float way = (hash >> 1 & 1) == 0 ? 1.0F : -1.0F;
        int high = way > 0 ? 1 : 0;
        int low = 1 - high;
        this.arm[high] = -2.5F;
        this.armOut[high] = -0.25F;
        this.armIn[high] = 0.35F;
        this.elbow[high] = 0.7F;
        this.arm[low] = -1.25F;
        this.armOut[low] = -0.35F;
        this.armIn[low] = 0.5F;
        this.elbow[low] = 1.1F;
        this.roll += way * 0.22F * peek;
        this.tilt += way * 0.45F * peek;
        this.twist += way * 0.12F * peek;
        this.follow = 0.25F + 0.75F * peek;
        this.nod += 0.1F;
    }

    // Curled up tight on its haunches, knees to its chest, arms wrapped round its shins, only its head up and
    // watching; it breathes slow and shallow.
    private void curl(float into, float age, int id) {
        float k = into * into * (3.0F - 2.0F * into);
        float breath = 0.03F * Mth.sin(age * 0.09F + id);
        this.lean += (1.05F + breath - this.lean) * k;
        this.sink += (9.5F - this.sink) * k;
        this.ahead += (-1.5F - this.ahead) * k;
        this.nod += (-1.0F - this.nod) * k;
        this.follow += (0.5F - this.follow) * k;
        for (int s = 0; s < 2; s++) {
            this.leg[s] += (-1.35F - this.leg[s]) * k;
            this.knee[s] += (2.45F - this.knee[s]) * k;
            this.legOut[s] += (0.18F - this.legOut[s]) * k;
            this.arm[s] += (-0.5F - this.arm[s]) * k;
            this.elbow[s] += (1.55F - this.elbow[s]) * k;
            this.armOut[s] += (-0.15F - this.armOut[s]) * k;
            this.armIn[s] += (0.3F - this.armIn[s]) * k;
        }
    }

    // Now and then its head snaps aside and back, a claw twitches; the rest of it never moves.
    private void twitch(float age, int id) {
        int slot = Mth.floor(age / 13.0F);
        int hash = slot * 83_492_791 ^ id * 19_349_663;
        if ((hash & 7) > 1) {
            return;
        }
        float into = age - slot * 13.0F;
        float jerk = into < 1.0F ? into : Math.max(0.0F, 1.0F - (into - 1.0F) / 7.0F);
        float way = (hash >> 3 & 1) == 0 ? 1.0F : -1.0F;
        this.tilt += way * 0.6F * jerk;
        this.shake += way * 0.25F * jerk;
        this.elbow[hash >> 4 & 1] += 0.4F * jerk;
    }

    // It sinks onto its haunches with its arms drawn back, springs, flies at them arms out, and tears down with both
    // claws as it lands.
    private void lunge(double t) {
        float coil = Trunk.in(t, 0.0, StalkerMoves.LEAP) * (1.0F - Trunk.in(t, StalkerMoves.LEAP,
                StalkerMoves.LEAP + 1.5));
        float fly = Trunk.in(t, StalkerMoves.LEAP, StalkerMoves.LEAP + 1.5) * (1.0F - Trunk.in(t,
                StalkerMoves.LUNGE_HIT, StalkerMoves.LUNGE_HIT + 1.5));
        float tear = Trunk.in(t, StalkerMoves.LUNGE_HIT, StalkerMoves.LUNGE_HIT + 1.5) * (1.0F - Trunk.in(t,
                StalkerMoves.LUNGE_HIT + 6, StalkerMoves.LUNGE_END));
        this.lean += 0.55F * coil + 0.35F * fly + 0.7F * tear;
        this.sink += 4.0F * coil + 2.5F * tear;
        this.nod += -0.4F * coil - 0.6F * fly - 0.2F * tear;
        this.jaw += 0.2F * coil + 0.9F * fly + 0.4F * tear;
        for (int s = 0; s < 2; s++) {
            this.arm[s] += 0.9F * coil - 2.6F * fly - 0.7F * tear;
            this.armOut[s] += 0.35F * coil + 0.4F * fly - 0.1F * tear;
            this.elbow[s] += 0.8F * coil - 0.1F * fly + 0.3F * tear;
            this.leg[s] += -0.8F * coil + 0.6F * fly - 0.6F * tear;
            this.knee[s] += 1.3F * coil + 0.2F * fly + 0.9F * tear;
        }
    }

    // A rake of the right claw from high across and down, then the left, its whole trunk swinging with each.
    private void swipe(double t) {
        float[] at = { StalkerMoves.SWIPE_FIRST, StalkerMoves.SWIPE_SECOND };
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            float raise = Trunk.in(t, at[s] - 5, at[s] - 1) * (1.0F - Trunk.in(t, at[s] - 1, at[s]));
            float rake = Trunk.in(t, at[s] - 1, at[s]) * (1.0F - Trunk.in(t, at[s] + 2, at[s] + 5));
            this.arm[s] += -2.4F * raise - 0.4F * rake;
            this.armOut[s] += 0.6F * raise - 0.45F * rake;
            this.armIn[s] += 0.6F * rake;
            this.elbow[s] += 0.9F * raise - 0.2F * rake;
            this.twist += side * (0.3F * raise - 0.45F * rake);
            this.lean += 0.35F * rake - 0.1F * raise;
            this.jaw += 0.35F * rake;
        }
        this.sink += 1.2F;
        for (int s = 0; s < 2; s++) {
            this.knee[s] += 0.5F;
            this.leg[s] -= 0.25F;
        }
    }

    // Thrown off its borrowed shape it rises out of a crouch, arms flung wide, head back with its jaw open.
    private void reveal(double t) {
        float rise = Trunk.in(t, 0.0, StalkerMoves.REVEAL_END * 0.6);
        float hold = 1.0F - Trunk.in(t, StalkerMoves.REVEAL_END * 0.6, StalkerMoves.REVEAL_END + 4);
        float crouch = 1.0F - rise;
        this.sink += 6.0F * crouch;
        this.lean += 0.9F * crouch - 0.3F * rise * hold;
        this.nod += -0.7F * rise * hold;
        this.jaw += 1.0F * rise * hold;
        for (int s = 0; s < 2; s++) {
            this.knee[s] += 2.0F * crouch;
            this.leg[s] -= 1.0F * crouch;
            this.arm[s] += -0.6F * crouch - 0.4F * rise * hold;
            this.armOut[s] += 1.1F * rise * hold;
            this.elbow[s] += 1.0F * crouch;
        }
    }

    // Shaking itself apart: every part of it jitters harder and harder until it is gone, and again from the hardest as
    // it comes back together elsewhere. Laid on unsmoothed so it judders.
    private void blink(double t, float age) {
        double gone = StalkerMoves.VANISH;
        float hard = (float) (t < gone ? t / gone : Math.max(0.0, 1.0 - (t - gone) / (StalkerMoves.BLINK_END - gone)));
        float a = Mth.sin(age * 9.1F);
        float b = Mth.sin(age * 13.7F + 1.3F);
        this.shake += 0.5F * hard * a;
        this.tilt += 0.4F * hard * b;
        this.roll += 0.12F * hard * a;
        this.twist += 0.2F * hard * b;
        this.jaw += 0.6F * hard;
        for (int s = 0; s < 2; s++) {
            this.arm[s] += 0.6F * hard * (s == 0 ? a : b);
            this.armOut[s] += 0.5F * hard;
        }
    }
}
