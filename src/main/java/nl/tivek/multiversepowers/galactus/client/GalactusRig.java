package nl.tivek.multiversepowers.galactus.client;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Galactus's bones, in model pixels as he stands at rest: feet on y 0, facing +z, his left on +x, arms hanging with the
// palms in and the fingers down. Each bone turns about its pivot within its parent; the hands have every finger on
// three knuckles and the thumb on three joints. Side 0 is his right (x below 0), side 1 his left.
final class GalactusRig {
    static final int PELVIS = 0;
    static final int ABDOMEN = 1;
    static final int CHEST = 2;
    static final int HEAD = 3;
    static final int THIGH = 4;
    static final int SHIN = 6;
    static final int PAULDRON = 8;
    static final int UPPER = 10;
    static final int FORE = 12;
    static final int HAND = 14;
    static final int DIGITS = 16;
    static final int THUMB = 4;
    static final int COUNT = DIGITS + 2 * 15;
    static final int RIGHT = 0;
    static final int LEFT = 1;

    // Where each finger's first knuckle sits on the hand (y, z) and how long and thick its three bones are.
    static final float[][] KNUCKLE = { { 23.0F, 1.6F }, { 22.85F, 0.52F }, { 23.0F, -0.56F }, { 23.4F, -1.6F } };
    static final float[][] LENGTH = { { 2.2F, 1.45F, 1.15F }, { 2.4F, 1.6F, 1.25F }, { 2.25F, 1.5F, 1.15F },
            { 1.8F, 1.2F, 1.0F }, { 1.7F, 1.4F, 1.2F } };
    static final float[] RADIUS = { 0.58F, 0.6F, 0.56F, 0.5F, 0.66F };
    static final float TAPER = 0.9F;
    // The thumb's root on the palm, and the way it points at rest (for its x, times the side's sign).
    static final float[] THUMB_ROOT = { -0.45F, 26.6F, 1.75F };
    static final float[] THUMB_WAY = { -0.35F, -0.72F, 0.6F };
    static final float HAND_X = 11.7F;
    // The head and helmet drawn this much bigger than modelled, for the great helmet's weight.
    static final float HEAD_SIZE = 1.2F;

    final int[] parent = new int[COUNT];
    final Vector3f[] pivot = new Vector3f[COUNT];
    final Matrix4f[] world = new Matrix4f[COUNT];

    GalactusRig() {
        this.bone(PELVIS, -1, 0.0F, 31.0F, 0.0F);
        this.bone(ABDOMEN, PELVIS, 0.0F, 35.0F, 0.0F);
        this.bone(CHEST, ABDOMEN, 0.0F, 40.5F, 0.0F);
        this.bone(HEAD, CHEST, 0.0F, 51.0F, -0.3F);
        for (int side = 0; side < 2; side++) {
            float s = sign(side);
            this.bone(THIGH + side, PELVIS, s * 4.5F, 30.0F, 0.2F);
            this.bone(SHIN + side, THIGH + side, s * 4.7F, 17.0F, 0.5F);
            this.bone(PAULDRON + side, CHEST, s * 11.0F, 48.5F, 0.0F);
            this.bone(UPPER + side, CHEST, s * 11.0F, 48.5F, 0.0F);
            this.bone(FORE + side, UPPER + side, s * 11.5F, 37.5F, 0.3F);
            this.bone(HAND + side, FORE + side, s * HAND_X, 27.5F, 0.3F);
            for (int f = 0; f < 4; f++) {
                float y = KNUCKLE[f][0];
                for (int k = 0; k < 3; k++) {
                    this.bone(digit(side, f, k), k == 0 ? HAND + side : digit(side, f, k - 1), s * HAND_X, y,
                            KNUCKLE[f][1]);
                    y -= LENGTH[f][k];
                }
            }
            Vector3f way = thumbWay(side);
            Vector3f at = new Vector3f(s * (HAND_X + THUMB_ROOT[0]), THUMB_ROOT[1], THUMB_ROOT[2]);
            for (int k = 0; k < 3; k++) {
                this.bone(digit(side, THUMB, k), k == 0 ? HAND + side : digit(side, THUMB, k - 1), at.x, at.y, at.z);
                at.fma(LENGTH[THUMB][k], way);
            }
        }
        for (int b = 0; b < COUNT; b++) {
            this.world[b] = new Matrix4f();
        }
    }

    static float sign(int side) {
        return side == RIGHT ? -1.0F : 1.0F;
    }

    static int digit(int side, int finger, int segment) {
        return DIGITS + side * 15 + finger * 3 + segment;
    }

    static Vector3f thumbWay(int side) {
        return new Vector3f(sign(side) * THUMB_WAY[0], THUMB_WAY[1], THUMB_WAY[2]).normalize();
    }

    // How wide the index finger is at its tip, and the tip in its last bone.
    static float fingerWidth() {
        return 2.0F * RADIUS[0] * TAPER * TAPER * TAPER;
    }

    static Vector3f tipLocal() {
        return new Vector3f(0.0F, -LENGTH[0][2] - fingerWidth() * 0.5F, 0.0F);
    }

    // The end of a finger (or the thumb) in its last bone.
    static Vector3f endLocal(int side, int finger) {
        Vector3f way = finger == THUMB ? thumbWay(side) : new Vector3f(0.0F, -1.0F, 0.0F);
        return way.mul(LENGTH[finger][2] + RADIUS[finger] * TAPER * TAPER * TAPER);
    }

    private void bone(int b, int parent, float x, float y, float z) {
        this.parent[b] = parent;
        this.pivot[b] = new Vector3f(x, y, z);
    }

    // Every bone's frame in model pixels: `root` places the pelvis, then each bone turns by `turn` about its pivot.
    void pose(Matrix4f root, Quaternionf[] turn, Vector3f chestSwell) {
        for (int b = 0; b < COUNT; b++) {
            int up = this.parent[b];
            Vector3f at = this.pivot[b];
            Matrix4f m = this.world[b];
            if (up < 0) {
                m.set(root).translate(at);
            } else {
                Vector3f from = this.pivot[up];
                m.set(this.world[up]).translate(at.x - from.x, at.y - from.y, at.z - from.z);
            }
            m.rotate(turn[b]);
            if (b == CHEST) {
                m.scale(chestSwell);
            } else if (b == HEAD) {
                m.scale(HEAD_SIZE);
            }
        }
    }

    Vector3f at(int bone, Vector3f local) {
        return this.world[bone].transformPosition(new Vector3f(local));
    }

    Vector3f tip() {
        return this.at(digit(RIGHT, 0, 2), tipLocal());
    }

    Vector3f joint(int bone) {
        return this.at(bone, new Vector3f());
    }

    // The way from the right shoulder to the index tip in the chest's frame, the arm turned by nothing at the shoulder.
    Vector3f reachInChest(Matrix4f root, Quaternionf[] turn, Vector3f swell) {
        Quaternionf kept = new Quaternionf(turn[UPPER + RIGHT]);
        turn[UPPER + RIGHT].identity();
        this.pose(root, turn, swell);
        Vector3f d = this.tip().sub(this.joint(UPPER + RIGHT));
        turn[UPPER + RIGHT].set(kept);
        Quaternionf chest = this.world[CHEST].getNormalizedRotation(new Quaternionf());
        return chest.conjugate().transform(d);
    }

    // The turn at the right shoulder that lays the whole arm along `reach` (body frame), its elbow bending towards
    // `bendTo` (chest frame) as an arm reaching up does.
    Quaternionf aim(Matrix4f root, Quaternionf[] turn, Vector3f swell, Vector3f reach, Vector3f bendTo) {
        Vector3f d = this.reachInChest(root, turn, swell);
        Quaternionf chest = this.world[CHEST].getNormalizedRotation(new Quaternionf());
        Vector3f want = chest.conjugate(new Quaternionf()).transform(new Vector3f(reach)).normalize();
        Quaternionf q = new Quaternionf().rotationTo(new Vector3f(d).normalize(), want);
        Vector3f dn = new Vector3f(d).normalize();
        Vector3f bend = new Vector3f(0.0F, 0.0F, 1.0F);
        bend.sub(new Vector3f(dn).mul(bend.dot(dn)));
        q.transform(bend);
        Vector3f goal = new Vector3f(bendTo).sub(new Vector3f(want).mul(bendTo.dot(want)));
        if (bend.lengthSquared() > 1.0E-6F && goal.lengthSquared() > 1.0E-6F) {
            bend.normalize();
            goal.normalize();
            float angle = (float) Math.atan2(new Vector3f(bend).cross(goal).dot(want), bend.dot(goal));
            q.premul(new Quaternionf().rotationAxis(angle, want));
        }
        return q;
    }
}
