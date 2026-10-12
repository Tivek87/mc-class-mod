package nl.tivek.multiversepowers.galactus.client;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Keyframes;
import nl.tivek.multiversepowers.galactus.Galactus;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// How Galactus moves at each moment of his timeline, the same on every game: floating, breathing, swaying, looking
// round and at the moon, his left hand opening and closing and offering itself, then the reach: a draw back, the arm
// rising in an arc with the elbow leading, the hand curling into a pointing finger, the jab with a recoil at contact,
// and the hand relaxing open as the arm comes down. Parts follow a little behind each other.
final class GalactusMotion {
    private static final float R = Galactus.RAISE;
    private static final float H = Galactus.REACHED;
    private static final float T = Galactus.TOUCH;
    private static final float L = Galactus.LET_GO;
    private static final float B = Galactus.BACK;
    private static final float W = Galactus.LOWERED;
    private static final float G = Galactus.GONE;

    // Hand shapes, per finger (index to little): three knuckle bends and the spread; the thumb's three bends and how
    // far it lies across the palm.
    private static final float[][] RELAX = { { .28F, .34F, .22F, .05F }, { .34F, .4F, .25F, 0F },
            { .38F, .44F, .27F, -.05F }, { .44F, .48F, .3F, -.1F }, { .2F, .15F, .2F, .15F } };
    private static final float[][] OPEN = { { .04F, .04F, .02F, .15F }, { .03F, .03F, .02F, .03F },
            { .04F, .04F, .02F, -.1F }, { .06F, .05F, .03F, -.22F }, { -.2F, .05F, .05F, -.25F } };
    private static final float[][] FIST = { { 1.45F, 1.65F, 1.0F, .02F }, { 1.5F, 1.65F, 1.0F, 0F },
            { 1.5F, 1.6F, 1.0F, -.03F }, { 1.45F, 1.55F, .95F, -.06F }, { .55F, .6F, .55F, .8F } };
    private static final float[][] POINT = { { .03F, .02F, .02F, .05F }, { 1.5F, 1.65F, 1.05F, 0F },
            { 1.55F, 1.62F, 1.0F, -.05F }, { 1.48F, 1.55F, .95F, -.1F }, { .5F, .55F, .5F, .75F } };
    private static final float[][] CLAW = { { .55F, .7F, .6F, .22F }, { .6F, .75F, .6F, .05F },
            { .6F, .75F, .6F, -.12F }, { .6F, .7F, .55F, -.28F }, { .1F, .3F, .3F, -.15F } };

    private static final Keyframes.Key[] R_ELBOW_UP = keys(0, .22F, R - 90, .22F, R + 5, .55F, R + 110, 1.75F,
            R + 210, 1.2F, H, .5F);
    private static final Keyframes.Key[] R_ELBOW_DOWN = keys(B, .3F, B + 40, 1.05F, W, .45F, W + 70, .22F);
    private static final Keyframes.Key[] R_WRIST = keys(0, .1F, R - 90, .14F, R + 5, .3F, R + 110, .55F, R + 230,
            .05F, H, -.22F, T, -.12F, B, -.05F, B + 30, .45F, W, .35F, W + 70, .12F);
    private static final Keyframes.Key[] R_TWIST = keys(0, 0F, R, 0F, R + 150, .7F, H, 1.15F, B, 1.15F, W, .3F,
            W + 60, 0F);
    private static final Keyframes.Key[] R_CLAW = keys(0, 0F, R - 90, 0F, R - 20, .55F, R + 40, .3F, R + 120, 0F);
    private static final Keyframes.Key[] R_POINT = keys(0, 0F, R + 20, 0F, R + 190, 1F, B + 15, 1F, W - 10, 0F);
    private static final Keyframes.Key[] R_OPEN = keys(0, 0F, B + 15, 0F, B + 60, 1F, W + 40, 1F, W + 130, 0F);
    private static final Keyframes.Key[] TWIST = keys(0, 0F, R - 90, 0F, R + 5, -.13F, R + 150, .05F, H, .14F, T,
            .2F, B, .16F, W, .03F, W + 80, 0F);
    private static final Keyframes.Key[] LEAN = keys(0, .03F, R - 90, .03F, R + 5, -.04F, H, .05F, T, .11F, B,
            .07F, W, .03F);
    private static final Keyframes.Key[] BEND = keys(0, 0F, R, 0F, H, -.05F, T, -.075F, B, -.05F, W, 0F);
    private static final Keyframes.Key[] TILT = keys(0, 0F, R, 0F, H, .1F, T, .15F, B, .1F, W, 0F);
    private static final Keyframes.Key[] LOOK = keys(0, .35F, R - 200, .55F, R, .85F, H, 1F, B, 1F, W + 80, .55F,
            G, .4F);
    private static final Keyframes.Key[] L_OFFER = keys(0, 0F, 950, 0F, 1150, 1F, 1360, 1F, 1470, 0F);
    private static final Keyframes.Key[] L_BRACE = keys(0, 0F, R - 60, 0F, R + 80, 1F, B, 1F, W + 60, 0F);
    private static final Keyframes.Key[] L_FIST = keys(0, 0F, R, 0F, R + 120, .45F, T - 20, .6F, T, 1F, L + 20, 1F,
            B + 50, 0F);
    private static final Keyframes.Key[] CHARGE = keys(0, 0F, R - 90, 0F, R, .35F, H, .8F, T, 1.6F, L, 1.2F, B, .8F,
            W, .2F, W + 100, 0F);

    // A pose: every bone's turn, where the pelvis is, how the chest swells; the right shoulder runs a curve from A
    // through B to C (a null being the aim at the moon) at U; how far the head looks at the moon, the energy over his
    // armour and the charge in his reaching arm.
    static final class Pose {
        final Quaternionf[] turn = new Quaternionf[GalactusRig.COUNT];
        final Matrix4f root = new Matrix4f();
        final Vector3f swell = new Vector3f(1.0F);
        @Nullable
        Quaternionf armA;
        @Nullable
        Quaternionf armB;
        @Nullable
        Quaternionf armC;
        float armU;
        float look;
        float energy;
        float charge;

        Pose() {
            for (int b = 0; b < this.turn.length; b++) {
                this.turn[b] = new Quaternionf();
            }
        }
    }

    private GalactusMotion() {
    }

    static Pose at(double time) {
        float t = (float) time;
        Pose pose = new Pose();
        Quaternionf[] q = pose.turn;
        float breath = wave(t, 150.0F, 0.0F);
        // Floating: a slow bob and sway about the pelvis.
        pose.root.translate(0.0F, 0.35F * wave(t, 210.0F, 0.5F), 0.0F)
                .rotateY(0.03F * wave(t, 600.0F, 1.0F))
                .rotateZ(0.018F * wave(t, 290.0F, 0.2F))
                .rotateX(0.012F * wave(t, 350.0F, 1.0F));
        float twist = k(TWIST, t) + 0.02F * wave(t, 420.0F, 2.0F);
        float lean = k(LEAN, t);
        float bend = k(BEND, t);
        q[GalactusRig.ABDOMEN].rotationY(twist * 0.45F).rotateX(lean * 0.45F + 0.008F * breath)
                .rotateZ(bend * 0.45F);
        q[GalactusRig.CHEST].rotationY(twist * 0.55F).rotateX(lean * 0.55F - 0.016F * breath).rotateZ(bend * 0.55F);
        pose.swell.set(1.0F + 0.012F * breath, 1.0F + 0.006F * breath, 1.0F + 0.02F * breath);
        q[GalactusRig.HEAD].rotationX(0.12F + 0.012F * breath + 0.03F * wave(t, 330.0F, 0.7F))
                .rotateY(0.05F * wave(t, 400.0F, 0.0F)).rotateZ(k(TILT, t) + 0.015F * wave(t, 270.0F, 1.3F));
        pose.look = k(LOOK, t);
        legs(q, t);
        rightArm(pose, t);
        leftArm(q, t);
        pose.charge = k(CHARGE, t);
        pose.energy = 0.4F + 0.15F * wave(t, 96.0F, 0.0F) + 0.3F * pose.charge;
        return pose;
    }

    // The shoulder curve resolved (the aim from the rig), the pauldrons riding part of each arm's turn, the head
    // turned towards `lookAt` (model pixels) as far as the pose says; then the rig posed.
    static void apply(GalactusRig rig, Pose pose, Vector3f reach, Vector3f lookAt) {
        Quaternionf aim = rig.aim(pose.root, pose.turn, pose.swell, reach, new Vector3f(0.35F, 1.0F, 0.15F));
        Quaternionf a = pose.armA == null ? aim : pose.armA;
        Quaternionf b = pose.armB == null ? aim : pose.armB;
        Quaternionf c = pose.armC == null ? aim : pose.armC;
        float u = pose.armU;
        Quaternionf ab = new Quaternionf(a).slerp(b, u);
        Quaternionf bc = new Quaternionf(b).slerp(c, u);
        pose.turn[GalactusRig.UPPER + GalactusRig.RIGHT].set(ab.slerp(bc, u));
        for (int side = 0; side < 2; side++) {
            pose.turn[GalactusRig.PAULDRON + side].set(new Quaternionf().slerp(pose.turn[GalactusRig.UPPER + side],
                    0.15F));
        }
        rig.pose(pose.root, pose.turn, pose.swell);
        Vector3f eye = rig.at(GalactusRig.HEAD, new Vector3f(0.0F, 5.0F, 2.0F));
        Vector3f way = new Vector3f(lookAt).sub(eye);
        Quaternionf chest = rig.world[GalactusRig.CHEST].getNormalizedRotation(new Quaternionf());
        chest.conjugate().transform(way);
        if (way.lengthSquared() > 1.0E-6F && pose.look > 0.0F) {
            way.normalize();
            float yaw = Mth.clamp((float) Math.atan2(way.x, way.z), -0.9F, 0.9F);
            float pitch = Mth.clamp((float) Math.asin(Mth.clamp(way.y, -1.0F, 1.0F)), -0.5F, 0.7F);
            Quaternionf look = new Quaternionf().rotationY(yaw * pose.look).rotateX(-pitch * pose.look);
            pose.turn[GalactusRig.HEAD].premul(look);
            rig.pose(pose.root, pose.turn, pose.swell);
        }
    }

    private static void legs(Quaternionf[] q, float t) {
        float drift = wave(t, 330.0F, 0.0F);
        q[GalactusRig.THIGH + GalactusRig.RIGHT].rotationX(-0.25F + 0.05F * drift).rotateZ(-0.05F);
        q[GalactusRig.THIGH + GalactusRig.LEFT].rotationX(0.1F - 0.05F * drift).rotateZ(0.06F);
        q[GalactusRig.SHIN + GalactusRig.RIGHT].rotationX(0.45F + 0.06F * wave(t, 330.0F, 0.8F));
        q[GalactusRig.SHIN + GalactusRig.LEFT].rotationX(0.24F + 0.05F * wave(t, 330.0F, 2.6F));
    }

    private static void rightArm(Pose pose, float t) {
        Quaternionf[] q = pose.turn;
        int side = GalactusRig.RIGHT;
        float s = GalactusRig.sign(side);
        Quaternionf hang = new Quaternionf().rotationX(0.04F * wave(t, 260.0F, 0.4F))
                .rotateZ(s * (0.13F + 0.02F * wave(t, 300.0F, 1.1F)));
        Quaternionf back = new Quaternionf().rotationX(0.32F).rotateZ(s * 0.22F);
        Quaternionf mid = new Quaternionf().rotationX(-1.35F).rotateZ(s * 0.5F);
        Quaternionf low = new Quaternionf().rotationX(-0.9F).rotateZ(s * 0.45F);
        if (t < R - 90.0F) {
            pose.armA = pose.armB = pose.armC = hang;
        } else if (t < R + 5.0F) {
            Quaternionf drawn = new Quaternionf(hang).slerp(back, (float) Ease.smooth((t - R + 90.0F) / 95.0F));
            pose.armA = pose.armB = pose.armC = drawn;
        } else if (t < H) {
            pose.armA = back;
            pose.armB = mid;
            pose.armC = null;
            pose.armU = (float) Ease.smooth((t - R - 5.0F) / (H - R - 5.0F));
        } else if (t < B) {
            pose.armA = pose.armB = pose.armC = null;
        } else if (t < W + 40.0F) {
            pose.armA = null;
            pose.armB = low;
            pose.armC = hang;
            pose.armU = (float) Ease.smooth((t - B) / (W + 40.0F - B));
        } else {
            pose.armA = pose.armB = pose.armC = hang;
        }
        float elbow;
        if (t < H) {
            elbow = k(R_ELBOW_UP, t);
        } else if (t < T) {
            float u = (t - H) / (T - H);
            elbow = Mth.lerp(u * u, 0.5F, 0.06F);
        } else if (t < L) {
            elbow = 0.06F + 0.2F * (float) Ease.jolt((t - T) / 14.0F);
        } else if (t < B) {
            elbow = Mth.lerp((float) Ease.smooth((t - L) / (B - L)), 0.06F, 0.3F);
        } else {
            elbow = k(R_ELBOW_DOWN, t);
        }
        // The forearm and hand lag the upper arm a little, the wrist most.
        q[GalactusRig.FORE + side].rotationX(-elbow).rotateY(-s * k(R_TWIST, t - 6.0F));
        float wrist = k(R_WRIST, t - 10.0F) + 0.03F * wave(t, 140.0F, 0.3F);
        q[GalactusRig.HAND + side].rotationAxis(wrist, 0.0F, 0.0F, -s);
        float[] weights = new float[5];
        for (int f = 0; f < 5; f++) {
            float lag = t - (f == GalactusRig.THUMB ? 8.0F : f * 7.0F);
            weights[0] = k(R_CLAW, lag);
            weights[1] = k(R_POINT, lag);
            weights[2] = k(R_OPEN, lag + (f == GalactusRig.THUMB ? 0.0F : 2.0F * f));
            weights[3] = 0.0F;
            fingers(q, side, f, t, weights);
        }
    }

    private static void leftArm(Quaternionf[] q, float t) {
        int side = GalactusRig.LEFT;
        float s = GalactusRig.sign(side);
        float offer = k(L_OFFER, t);
        float brace = k(L_BRACE, t);
        Quaternionf hang = new Quaternionf().rotationX(0.05F * wave(t, 280.0F, 2.0F))
                .rotateZ(s * (0.14F + 0.025F * wave(t, 320.0F, 0.0F)));
        Quaternionf offering = new Quaternionf().rotationX(-0.5F + 0.05F * wave(t, 200.0F, 0.0F)).rotateZ(s * 0.2F);
        Quaternionf bracing = new Quaternionf().rotationX(0.25F).rotateZ(s * 0.38F);
        q[GalactusRig.UPPER + side].set(hang.slerp(offering, offer).slerp(bracing, brace));
        float elbow = Mth.lerp(brace, Mth.lerp(offer, 0.2F + 0.05F * wave(t, 240.0F, 1.0F), 1.35F), 0.6F);
        q[GalactusRig.FORE + side].rotationX(-elbow).rotateY(s * 1.4F * offer * (1.0F - brace));
        q[GalactusRig.HAND + side].rotationAxis(0.1F - 0.25F * offer + 0.1F * brace, 0.0F, 0.0F, -s);
        float breathe = 0.5F + 0.5F * wave(t, 190.0F, 0.0F);
        float[] weights = new float[5];
        for (int f = 0; f < 5; f++) {
            float lag = t - f * 5.0F;
            weights[0] = 0.0F;
            weights[1] = 0.0F;
            weights[2] = Mth.lerp(offer, 0.35F * breathe, 0.55F + 0.45F * wave(lag, 160.0F, 0.0F));
            weights[3] = k(L_FIST, lag);
            fingers(q, side, f, t, weights);
        }
    }

    // One finger posed from the relaxed hand towards a claw, a point, an open hand and a fist by `weights`.
    private static void fingers(Quaternionf[] q, int side, int f, float t, float[] weights) {
        float s = GalactusRig.sign(side);
        float[] bend = RELAX[f].clone();
        float[][][] shapes = { CLAW, POINT, OPEN, FIST };
        for (int w = 0; w < 4; w++) {
            float weight = Mth.clamp(weights[w], 0.0F, 1.0F);
            for (int c = 0; c < 4; c++) {
                bend[c] = Mth.lerp(weight, bend[c], shapes[w][f][c]);
            }
        }
        float flutter = 0.05F * wave(t, 170.0F, f * 1.3F + side);
        if (f == GalactusRig.THUMB) {
            Vector3f way = GalactusRig.thumbWay(side);
            Vector3f flex = new Vector3f(way).cross(-s, 0.0F, 0.0F).normalize();
            Vector3f across = new Vector3f(way).cross(0.0F, 0.0F, -1.0F).normalize();
            q[GalactusRig.digit(side, f, 0)].rotationAxis(bend[3], across.x, across.y, across.z)
                    .rotateAxis(bend[0] + flutter, flex.x, flex.y, flex.z);
            for (int k = 1; k < 3; k++) {
                q[GalactusRig.digit(side, f, k)].rotationAxis(bend[k], flex.x, flex.y, flex.z);
            }
            return;
        }
        q[GalactusRig.digit(side, f, 0)].rotationAxis(bend[3], -1.0F, 0.0F, 0.0F)
                .rotateAxis(bend[0] + flutter, 0.0F, 0.0F, -s);
        q[GalactusRig.digit(side, f, 1)].rotationAxis(bend[1] + flutter * 0.6F, 0.0F, 0.0F, -s);
        q[GalactusRig.digit(side, f, 2)].rotationAxis(bend[2] + flutter * 0.4F, 0.0F, 0.0F, -s);
    }

    private static float wave(float t, float period, float phase) {
        return (float) Math.sin(Math.PI * 2.0 * t / period + phase);
    }

    private static float k(Keyframes.Key[] keys, float t) {
        return Keyframes.at(keys, t)[0];
    }

    private static Keyframes.Key[] keys(float... pairs) {
        Keyframes.Key[] out = new Keyframes.Key[pairs.length / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = new Keyframes.Key(pairs[2 * i], false, new float[] { pairs[2 * i + 1] });
        }
        return out;
    }
}
