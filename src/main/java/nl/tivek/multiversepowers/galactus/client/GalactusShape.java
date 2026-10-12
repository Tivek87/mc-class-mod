package nl.tivek.multiversepowers.galactus.client;

import nl.tivek.multiversepowers.galactus.client.GalactusHull.Look;
import nl.tivek.multiversepowers.galactus.client.GalactusHull.Paint;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Galactus's body in boxes, as the game's own creatures are built, a hull per bone of `GalactusRig`, in model pixels as
// he stands at rest. His helmet: a shell round a face with glowing eyes under a V brow, cheek guards, the crown with its
// ridge and prongs, discs at the ears and the two great plates stepping out from them and up like horns. Stepped
// pauldrons over a broad chest with a breastplate and the round emblem, a gorget, banded abdomen and a belt with a
// glowing buckle and hanging plates, armoured legs and boots; flared gauntlets and armoured hands with every finger in
// three bones. Dark purple plates (cracks of energy across the big ones) over a deep blue suit.
final class GalactusShape {
    static final Paint ARMOR = Paint.of(0x4A2C66, Look.PLATE, true);
    static final Paint ARMOR_DARK = Paint.of(0x2F1B40, Look.PLATE, true);
    static final Paint SUIT = Paint.of(0x2B3B66, Look.SUIT, false);
    static final Paint TRIM = Paint.of(0x3C3B4C, Look.TRIM, false);
    static final Paint SILVER = Paint.of(0x9497AE, Look.PLAIN, false);
    static final Paint SKIN = Paint.of(0x7E7088, Look.PLAIN, false);
    static final Paint DARK = Paint.of(0x0D0A13, Look.PLAIN, false);
    static final Paint EYE = Paint.of(0xD8B8FF, Look.GLOW, false);
    static final Paint ENERGY = Paint.of(0xA45CFF, Look.GLOW, false);
    static final Paint EMBLEM = Paint.of(0x4A2C66, Look.EMBLEM, false);

    final GalactusHull[] hulls = new GalactusHull[GalactusRig.COUNT];

    GalactusShape(GalactusRig rig) {
        for (int b = 0; b < GalactusRig.COUNT; b++) {
            this.hulls[b] = new GalactusHull(rig.pivot[b]);
        }
        this.pelvis();
        this.abdomen();
        this.chest();
        this.head();
        this.helmet();
        for (int side = 0; side < 2; side++) {
            this.leg(side);
            this.arm(side);
            this.hand(side, rig);
        }
        for (GalactusHull hull : this.hulls) {
            hull.moveBy(new Vector3f(hull.pivot).negate());
        }
    }

    private GalactusHull h(int bone) {
        return this.hulls[bone];
    }

    private void pelvis() {
        GalactusHull hull = h(GalactusRig.PELVIS);
        hull.box(SUIT, -7.4, 26.0, -4.8, 7.4, 35.5, 4.8);
        hull.box(TRIM, -8.0, 32.6, -5.4, 8.0, 35.6, 5.4);
        hull.box(ARMOR, -2.6, 32.0, 5.4, 2.6, 36.2, 6.2);
        hull.box(ENERGY, -1.2, 33.4, 6.2, 1.2, 34.8, 6.5);
        hull.box(ARMOR, -3.3, 22.5, 4.9, 3.3, 32.6, 5.9);
        hull.box(ARMOR, -2.2, 20.5, 4.6, 2.2, 22.5, 5.6);
        hull.box(ARMOR_DARK, -3.2, 25.0, -5.9, 3.2, 32.6, -4.9);
        for (int s = -1; s <= 1; s += 2) {
            hull.box(ARMOR, s * 7.4, 26.0, -3.0, s * 9.0, 33.6, 3.6);
        }
    }

    private void leg(int side) {
        double x = GalactusRig.sign(side) * 4.7;
        GalactusHull thigh = h(GalactusRig.THIGH + side);
        thigh.box(SUIT, x - 3.6, 17.0, -3.4, x + 3.6, 31.0, 3.8);
        thigh.box(ARMOR, x - 2.7, 19.5, 3.8, x + 2.7, 28.5, 4.8);
        GalactusHull shin = h(GalactusRig.SHIN + side);
        shin.box(ARMOR, x - 2.3, 15.2, 2.4, x + 2.3, 19.4, 4.6);
        shin.box(ARMOR, x - 3.3, 3.0, -2.9, x + 3.3, 17.0, 3.7);
        shin.box(TRIM, x - 3.5, 11.2, -3.1, x + 3.5, 11.9, 3.9);
        shin.box(TRIM, x - 3.5, 7.2, -3.1, x + 3.5, 7.9, 3.9);
        shin.box(ARMOR_DARK, x - 3.6, 0.0, -3.2, x + 3.6, 3.0, 4.4);
        shin.box(ARMOR, x - 2.7, 0.0, 4.4, x + 2.7, 2.2, 7.6);
        shin.box(TRIM, x - 2.0, 0.0, 7.6, x + 2.0, 1.4, 8.0);
    }

    private void abdomen() {
        GalactusHull hull = h(GalactusRig.ABDOMEN);
        hull.box(SUIT, -7.4, 33.5, -4.6, 7.4, 41.8, 4.8);
        for (int i = 0; i < 3; i++) {
            double y = 35.4 + i * 1.9;
            double a = 4.6 + 0.55 * i;
            hull.box(ARMOR, -a, y, 3.9, a, y + 1.45, 5.4 + 0.15 * i);
        }
        hull.box(ARMOR_DARK, -5.5, 35.0, -5.4, 5.5, 41.0, -4.6);
    }

    private void chest() {
        GalactusHull hull = h(GalactusRig.CHEST);
        hull.box(SUIT, -9.5, 40.0, -5.4, 9.5, 50.5, 5.4);
        hull.box(ARMOR, -9.2, 41.0, 1.0, 9.2, 49.6, 6.4);
        hull.box(ARMOR, -7.6, 40.2, 1.0, 7.6, 41.0, 5.6);
        hull.box(ARMOR, -6.6, 49.6, -2.0, 6.6, 50.6, 5.2);
        hull.box(EMBLEM, -3.2, 41.2, 6.4, 3.2, 47.6, 7.0);
        hull.box(ARMOR_DARK, -8.6, 41.0, -6.3, 8.6, 49.6, -5.4);
        hull.box(ARMOR, -5.6, 49.0, -3.6, 5.6, 52.0, 3.0);
        for (int s = -1; s <= 1; s += 2) {
            hull.box(SUIT, s * 3.0, 47.6, -2.8, s * 10.4, 51.6, 2.8);
        }
    }

    private void head() {
        GalactusHull hull = h(GalactusRig.HEAD);
        hull.box(SKIN, -2.5, 48.8, -2.6, 2.5, 53.0, 2.0);
        hull.box(SKIN, -3.2, 51.6, -2.4, 3.2, 60.4, 4.0);
        hull.box(SKIN, -2.2, 51.4, 2.2, 2.2, 53.6, 4.4);
        hull.box(SKIN, -0.5, 54.4, 4.0, 0.5, 56.4, 4.7);
        hull.box(DARK, -1.0, 53.0, 4.4, 1.0, 53.4, 4.5);
        for (int s = -1; s <= 1; s += 2) {
            hull.box(DARK, s * 0.3, 55.5, 3.8, s * 2.4, 56.9, 4.1);
            hull.box(EYE, s * 0.6, 55.8, 4.0, s * 2.1, 56.6, 4.25);
            // The brow: its outer ends higher, a V over the eyes.
            hull.box(ARMOR, new Vector3f(s * 1.55F, 57.4F, 4.2F), new Vector3f(1.75F, 0.45F, 0.5F),
                    new Quaternionf().rotationZ(s * 0.25F));
        }
    }

    private void helmet() {
        GalactusHull hull = h(GalactusRig.HEAD);
        hull.box(ARMOR, -4.4, 51.0, -5.8, 4.4, 61.0, -1.0);
        hull.box(ARMOR, -4.3, 58.6, -5.6, 4.3, 61.4, 4.3);
        hull.box(ARMOR, -3.6, 61.4, -4.4, 3.6, 66.2, 2.4);
        hull.box(TRIM, -4.5, 61.0, -5.9, 4.5, 61.8, 4.5);
        hull.box(ARMOR, -0.6, 58.6, 4.3, 0.6, 61.4, 4.9);
        hull.box(ARMOR, -0.6, 61.4, 2.4, 0.6, 66.6, 3.2);
        for (double x : new double[] { -2.25, -0.75, 0.75, 2.25 }) {
            double high = Math.abs(x) > 1.0 ? 2.2 : 1.6;
            hull.box(ARMOR, x - 0.4, 66.2, -3.4, x + 0.4, 66.2 + high, 1.4);
        }
        for (int s = -1; s <= 1; s += 2) {
            hull.box(ARMOR, s * 3.2, 51.4, -1.0, s * 4.3, 58.2, 3.6);
            hull.box(TRIM, s * 4.3, 53.2, -2.6, s * 5.3, 57.2, 1.4);
            hull.box(SILVER, s * 5.3, 54.6, -1.2, s * 5.7, 55.8, 0.0);
            // The great side plates: out from the ear, then stepping up and out like horns.
            hull.box(ARMOR, s * 5.3, 54.4, -2.4, s * 10.2, 56.2, 1.2);
            hull.box(TRIM, s * 5.3, 54.0, -2.0, s * 9.8, 54.4, 0.8);
            hull.box(ARMOR, s * 9.4, 56.2, -2.6, s * 11.4, 60.0, 1.4);
            hull.box(ARMOR, s * 10.6, 60.0, -2.8, s * 12.4, 64.2, 1.6);
            hull.box(ARMOR, s * 11.6, 64.2, -3.0, s * 13.4, 68.0, 1.8);
            hull.box(ARMOR, s * 12.4, 68.0, -2.6, s * 13.8, 70.0, 1.0);
        }
    }

    private void arm(int side) {
        double s = GalactusRig.sign(side);
        GalactusHull pauldron = h(GalactusRig.PAULDRON + side);
        pauldron.box(ARMOR, s * 6.6, 46.4, -5.4, s * 16.0, 52.6, 5.4);
        pauldron.box(ARMOR, s * 8.0, 52.6, -4.2, s * 14.6, 54.0, 4.2);
        pauldron.box(ARMOR, s * 9.6, 54.0, -2.6, s * 13.0, 54.8, 2.6);
        pauldron.box(TRIM, s * 6.4, 45.8, -5.6, s * 16.2, 46.6, 5.6);
        double x = s * 11.2;
        GalactusHull upper = h(GalactusRig.UPPER + side);
        upper.box(SUIT, x - 3.6, 36.8, -3.4, x + 3.6, 50.0, 3.8);
        upper.box(ARMOR, x - 4.2, 45.0, -4.0, x + 4.2, 46.2, 4.4);
        upper.box(ARMOR, x - 4.0, 43.6, -3.8, x + 4.0, 44.8, 4.2);
        upper.box(TRIM, x - 3.8, 41.0, -3.6, x + 3.8, 41.8, 4.0);
        x = s * 11.55;
        GalactusHull fore = h(GalactusRig.FORE + side);
        fore.box(ARMOR, x - 2.3, 35.4, -3.8, x + 2.3, 40.0, -0.6);
        fore.box(SUIT, x - 3.1, 33.8, -2.8, x + 3.1, 38.6, 3.4);
        fore.box(ARMOR, x - 3.6, 31.4, -3.3, x + 3.6, 34.6, 3.9);
        fore.box(ARMOR, x - 4.1, 29.4, -3.8, x + 4.1, 31.4, 4.4);
        fore.box(ARMOR, x - 4.6, 27.8, -4.3, x + 4.6, 29.4, 4.9);
        fore.box(TRIM, x - 3.8, 32.9, -3.5, x + 3.8, 33.5, 4.1);
        fore.box(TRIM, x - 4.3, 30.6, -4.0, x + 4.3, 31.2, 4.6);
        fore.box(ENERGY, x - 4.75, 28.2, -4.45, x + 4.75, 28.6, 5.05);
    }

    private void hand(int side, GalactusRig rig) {
        double s = GalactusRig.sign(side);
        double x = s * GalactusRig.HAND_X;
        GalactusHull hand = h(GalactusRig.HAND + side);
        hand.box(ARMOR_DARK, x - 1.0, 22.7, -2.2, x + 1.0, 28.0, 2.6);
        hand.box(ARMOR, x + s * 1.0, 23.4, -1.9, x + s * 1.5, 27.2, 2.3);
        hand.box(TRIM, x - 1.1, 22.6, -2.4, x + 1.1, 23.3, 2.6);
        hand.box(ARMOR_DARK, x - s * 1.2, 24.6, 0.8, x - s * 0.2, 27.2, 2.6);
        Vector3f down = new Vector3f(0.0F, -1.0F, 0.0F);
        for (int f = 0; f < 5; f++) {
            Vector3f way = f == GalactusRig.THUMB ? GalactusRig.thumbWay(side) : down;
            Quaternionf turn = f == GalactusRig.THUMB ? new Quaternionf().rotationTo(down, way) : null;
            for (int k = 0; k < 3; k++) {
                int bone = GalactusRig.digit(side, f, k);
                Vector3f from = rig.pivot[bone];
                float r = GalactusRig.RADIUS[f] * (float) Math.pow(GalactusRig.TAPER, k);
                // The last bone reaches on by its half width to the fingertip.
                float length = GalactusRig.LENGTH[f][k] + (k == 2 ? r * GalactusRig.TAPER : 0.0F);
                Vector3f middle = new Vector3f(from).fma(length * 0.5F, way);
                h(bone).box(ARMOR, middle, new Vector3f(r, length * 0.5F, r), turn);
                if (f != GalactusRig.THUMB) {
                    // A plate over the back of each finger bone.
                    h(bone).box(TRIM, new Vector3f(middle).add((float) s * (r + 0.1F), 0.0F, 0.0F),
                            new Vector3f(0.15F, length * 0.4F, r * 0.7F), null);
                }
            }
        }
    }
}
