package nl.tivek.multiversepowers.creature.lurker.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;
import nl.tivek.multiversepowers.creature.lurker.Crawler;
import nl.tivek.multiversepowers.creature.lurker.LurkerMoves;

// How a crawler holds itself this frame (LurkerModel): on all fours, a quick gait moving each hand with the foot across
// from it, spine rolling and head steady as a hunter's; low and slow while it stalks, its head pushed forward; now and
// then a twitch. On top of that its moves on the server's timeline: a claw raised high and raked down, a crouch and a
// spring with both claws reaching, a hard landing, a dart aside, and climbing out of the ground. Arms and legs turn
// forward with a negative angle; a positive lean bends the trunk forward.
final class LurkerPoses {
    private static final PoseSprings SPRINGS = new PoseSprings(0.35, 0.75);

    float lean;
    float sink;
    float shift;
    float twist;
    float roll;
    float nod;
    float tilt;
    float jaw;
    float legOut;
    final float[] arm = new float[2];
    final float[] armOut = new float[2];
    final float[] elbow = new float[2];
    final float[] leg = new float[2];
    final float[] knee = new float[2];

    <T extends MovingMonster & Crawler> void of(T crawler, float limbSwing, float amount, float age) {
        this.lean = LurkerModel.LEAN;
        this.sink = 0.0F;
        this.shift = 0.0F;
        this.twist = 0.0F;
        this.roll = 0.0F;
        this.nod = -0.1F;
        this.tilt = 0.0F;
        this.jaw = 0.05F;
        this.legOut = 0.0F;
        for (int s = 0; s < 2; s++) {
            this.arm[s] = LurkerModel.ARM;
            this.armOut[s] = 0.0F;
            this.elbow[s] = LurkerModel.ELBOW;
            this.leg[s] = LurkerModel.LEG;
            this.knee[s] = LurkerModel.KNEE;
        }
        int move = crawler.move();
        double t = crawler.moveAge(age - crawler.tickCount);
        boolean stalking = crawler.stalking();
        this.walk(limbSwing, amount, stalking);
        this.idle(age, crawler.getId(), stalking);
        switch (move) {
            case LurkerMoves.CLAW_RIGHT -> this.claw(t, 0);
            case LurkerMoves.CLAW_LEFT -> this.claw(t, 1);
            case LurkerMoves.POUNCE -> this.pounce(t);
            case LurkerMoves.LANDED -> this.land(t);
            case LurkerMoves.DASH_RIGHT -> this.dash(t, -1.0F);
            case LurkerMoves.DASH_LEFT -> this.dash(t, 1.0F);
            default -> {
            }
        }
        float risen = crawler.risen(age - crawler.tickCount);
        if (risen < 1.0F) {
            this.rise(1.0F - Trunk.in(risen, 0.75, 1.0), age);
        }
        float[] c = { this.lean, this.sink, this.shift, this.twist, this.roll, this.nod, this.tilt, this.jaw,
                this.legOut, this.arm[0], this.arm[1], this.armOut[0], this.armOut[1], this.elbow[0], this.elbow[1],
                this.leg[0], this.leg[1], this.knee[0], this.knee[1] };
        SPRINGS.follow(crawler.getId(), c, age, crawler.level().getGameTime());
        this.lean = c[0];
        this.sink = c[1];
        this.shift = c[2];
        this.twist = c[3];
        this.roll = c[4];
        this.nod = c[5];
        this.tilt = c[6];
        this.jaw = Math.max(0.0F, c[7]);
        this.legOut = c[8];
        for (int s = 0; s < 2; s++) {
            this.arm[s] = c[9 + s];
            this.armOut[s] = c[11 + s];
            this.elbow[s] = Math.max(0.0F, c[13 + s]);
            this.leg[s] = c[15 + s];
            this.knee[s] = Math.max(0.0F, c[17 + s]);
        }
    }

    // A hand moves with the foot across from it; the one swinging forward lifts, its elbow or knee folding more. Stalking
    // it goes low, its hips sunk and its head pushed out, with long, slow, careful steps.
    private void walk(float limbSwing, float amount, boolean stalking) {
        float phase = limbSwing * (stalking ? 0.55F : 0.85F);
        float w = Math.min(1.0F, amount * (stalking ? 2.4F : 1.5F));
        float stride = stalking ? 0.45F : 0.6F;
        for (int s = 0; s < 2; s++) {
            float sw = Mth.sin(phase + s * Mth.PI);
            float lift = Math.max(0.0F, Mth.cos(phase + s * Mth.PI));
            this.arm[s] += -stride * w * sw;
            this.elbow[s] += 0.45F * w * lift;
            float lw = Mth.sin(phase + (1 - s) * Mth.PI);
            float legLift = Math.max(0.0F, Mth.cos(phase + (1 - s) * Mth.PI));
            this.leg[s] += -stride * 0.8F * w * lw;
            this.knee[s] += 0.35F * w * legLift;
        }
        this.roll += 0.08F * w * Mth.sin(phase);
        this.twist += 0.1F * w * Mth.sin(phase);
        this.sink += 0.7F * w * Math.abs(Mth.cos(phase));
        if (stalking) {
            this.sink += 3.0F;
            this.lean += 0.12F;
            this.nod += 0.15F;
            for (int s = 0; s < 2; s++) {
                this.elbow[s] += 0.35F;
                this.knee[s] += 0.25F;
                this.armOut[s] += 0.15F;
            }
            this.legOut += 0.15F;
        }
    }

    // Breathing in short shallow pants; its head cocks and now and then the whole body twitches.
    private void idle(float age, int id, boolean stalking) {
        float breath = Mth.sin(age * 0.18F + id);
        this.lean += 0.015F * breath;
        this.jaw += 0.04F * (1.0F + breath);
        this.tilt += 0.18F * Mth.sin(age * 0.031F + id * 0.7F) * Mth.sin(age * 0.013F + id);
        float spasm = Mth.sin(age * 0.37F + id * 2.1F);
        if (!stalking && spasm > 0.97F) {
            float jerk = (spasm - 0.97F) * 33.0F;
            this.tilt += 0.35F * jerk * Mth.sin(age * 5.0F);
            this.roll += 0.06F * jerk;
            this.arm[id & 1] -= 0.25F * jerk;
        }
    }

    // It rears onto its hind legs a little, raises one claw high and back, then rakes it down across in front of it.
    private void claw(double t, int s) {
        float up = Trunk.in(t, 0.0, LurkerMoves.CLAW_HIT - 1) * (1.0F - Trunk.in(t, LurkerMoves.CLAW_HIT - 1,
                LurkerMoves.CLAW_HIT + 1));
        float down = Trunk.in(t, LurkerMoves.CLAW_HIT - 1, LurkerMoves.CLAW_HIT + 1) * (1.0F - Trunk.in(t,
                LurkerMoves.CLAW_HIT + 2, LurkerMoves.CLAW_END));
        float rear = up + 0.6F * down;
        int o = 1 - s;
        this.lean -= 0.45F * rear;
        this.sink += 1.5F * rear;
        this.shift += 1.5F * rear;
        this.twist += (s == 0 ? 1.0F : -1.0F) * (-0.3F * up + 0.35F * down);
        this.arm[s] = Mth.lerp(up, this.arm[s], -2.6F);
        this.arm[s] = Mth.lerp(down, this.arm[s], -0.35F);
        this.armOut[s] += 0.4F * up - 0.35F * down;
        this.elbow[s] = Mth.lerp(up, this.elbow[s], 1.4F);
        this.elbow[s] = Mth.lerp(down, this.elbow[s], 0.1F);
        this.arm[o] += 0.15F * rear;
        this.jaw += 0.5F * up + 0.7F * down;
        this.nod -= 0.15F * up;
    }

    // Crouching low, hips sunk and claws drawn back, it springs: stretched out long, both claws reaching for the head,
    // its legs trailing and its jaw wide.
    private void pounce(double t) {
        float crouch = Trunk.in(t, 0.0, LurkerMoves.SPRING - 1) * (1.0F - Trunk.in(t, LurkerMoves.SPRING - 1,
                LurkerMoves.SPRING + 1));
        float fly = Trunk.in(t, LurkerMoves.SPRING - 1, LurkerMoves.SPRING + 2);
        this.sink += 4.5F * crouch;
        this.lean += 0.15F * crouch - 0.25F * fly;
        this.nod += 0.2F * crouch - 0.2F * fly;
        this.jaw += 0.2F * crouch + 0.9F * fly;
        for (int s = 0; s < 2; s++) {
            this.elbow[s] += 0.7F * crouch;
            this.knee[s] += 0.5F * crouch;
            this.arm[s] = Mth.lerp(fly, this.arm[s] + 0.3F * crouch, -2.3F);
            this.elbow[s] = Mth.lerp(fly, this.elbow[s], 0.2F);
            this.armOut[s] += 0.25F * fly;
            this.leg[s] = Mth.lerp(fly, this.leg[s], 0.7F);
            this.knee[s] = Mth.lerp(fly, this.knee[s], 0.35F);
        }
    }

    // It lands heavily on its claws, sinking deep, and pulls itself together.
    private void land(double t) {
        float hit = 1.0F - Trunk.in(t, 0.0, LurkerMoves.LAND_END);
        this.sink += 4.0F * hit;
        this.nod += 0.25F * hit;
        for (int s = 0; s < 2; s++) {
            this.elbow[s] += 0.9F * hit;
            this.knee[s] += 0.4F * hit;
            this.armOut[s] += 0.3F * hit;
        }
        this.legOut += 0.25F * hit;
    }

    // Thrown aside low, its body rolled toward where it goes and its limbs tucked in.
    private void dash(double t, float way) {
        float on = Trunk.in(t, 0.0, 2.0) * (1.0F - Trunk.in(t, LurkerMoves.DASH_END - 3, LurkerMoves.DASH_END));
        this.roll += way * 0.55F * on;
        this.twist -= way * 0.3F * on;
        this.sink += 2.5F * on;
        for (int s = 0; s < 2; s++) {
            this.elbow[s] += 0.8F * on;
            this.knee[s] += 0.4F * on;
            this.arm[s] += 0.35F * on;
        }
    }

    // Climbing out of the ground: trunk upright, head thrown back, claws reaching up by turns and dragging it up.
    private void rise(float on, float age) {
        this.lean = Mth.lerp(on, this.lean, 0.35F);
        this.nod -= 0.5F * on;
        this.jaw += 0.6F * on;
        for (int s = 0; s < 2; s++) {
            float reach = Mth.sin(age * 0.45F + s * Mth.PI);
            this.arm[s] = Mth.lerp(on, this.arm[s], -2.4F - 0.5F * reach);
            this.elbow[s] = Mth.lerp(on, this.elbow[s], 0.6F + 0.6F * Math.max(0.0F, -reach));
            this.armOut[s] += 0.2F * on;
            this.leg[s] = Mth.lerp(on, this.leg[s], -0.2F);
            this.knee[s] = Mth.lerp(on, this.knee[s], 0.5F);
        }
    }
}
