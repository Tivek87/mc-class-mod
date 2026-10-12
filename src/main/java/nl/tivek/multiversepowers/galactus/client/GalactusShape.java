package nl.tivek.multiversepowers.galactus.client;

import nl.tivek.multiversepowers.galactus.client.GalactusHull.Look;
import nl.tivek.multiversepowers.galactus.client.GalactusHull.Paint;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Galactus's body in boxes, as the game's own creatures are built, a hull per bone of `GalactusRig`, in model pixels as
// he stands at rest. His helmet: a shell round a face of bare skin, of which the mask leaves only the nose and mouth to
// see (its eyes two lenses of blue-violet light under a V brow), the crown a tall tower stepping in to its peak, discs
// at the ears and the two great plates slanting out from them and standing tall. Stepped pauldrons over a broad
// chest with a breastplate, a steel-blue yoke and the round emblem, a gorget, banded abdomen and a belt with a
// glowing buckle and hanging plates, armoured legs and boots; flared gauntlets and armoured hands with every finger in
// three bones. Dark purple plates (cracks of energy across the big ones) over a deep blue suit.
final class GalactusShape {
    static final Paint ARMOR = Paint.of(0x4A2C66, Look.PLATE, true);
    static final Paint ARMOR_DARK = Paint.of(0x2F1B40, Look.PLATE, true);
    static final Paint SUIT = Paint.of(0x2B3B66, Look.SUIT, false);
    static final Paint TRIM = Paint.of(0x3C3B4C, Look.TRIM, false);
    static final Paint SILVER = Paint.of(0x9497AE, Look.PLAIN, false);
    static final Paint YOKE = Paint.of(0x4F6294, Look.PLATE, false);
    static final Paint SKIN = Paint.of(0xE3AE8C, Look.PLAIN, false);
    static final Paint LIP = Paint.of(0x9A5B4C, Look.PLAIN, false);
    static final Paint DARK = Paint.of(0x0D0A13, Look.PLAIN, false);
    // Light is drawn brighter than its paint: this deep, his eyes stay blue-violet instead of burning out to white.
    static final Paint EYE = Paint.of(0x6B66FF, Look.GLOW, false);
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
        // A steel-blue yoke across the shoulders, the emblem set in its point.
        hull.box(YOKE, -9.4, 45.8, 0.6, 9.4, 50.2, 6.9);
        hull.box(YOKE, -4.6, 42.6, 6.4, 4.6, 45.8, 6.9);
        hull.box(EMBLEM, -3.2, 42.9, 6.9, 3.2, 49.3, 7.5);
        hull.box(ARMOR_DARK, -8.6, 41.0, -6.3, 8.6, 49.6, -5.4);
        hull.box(ARMOR, -5.6, 49.0, -3.6, 5.6, 52.0, 3.0);
        for (int s = -1; s <= 1; s += 2) {
            hull.box(SUIT, s * 3.0, 47.6, -2.8, s * 10.4, 51.6, 2.8);
        }
    }

    private void head() {
        GalactusHull hull = h(GalactusRig.HEAD);
        hull.box(ARMOR_DARK, -2.5, 48.8, -2.6, 2.5, 53.0, 2.0);
        hull.box(SKIN, -3.2, 51.6, -2.4, 3.2, 60.4, 4.0);
        hull.box(SKIN, -2.2, 51.4, 2.2, 2.2, 53.6, 4.4);
        hull.box(SKIN, -0.55, 53.9, 4.0, 0.55, 55.6, 4.9);
        hull.box(SKIN, -0.85, 53.9, 4.0, 0.85, 54.5, 4.7);
        hull.box(LIP, -1.1, 52.7, 4.4, 1.1, 53.1, 4.55);
        // The mask: over his eyes and his cheeks and under his chin, open only round his nose and mouth.
        hull.box(ARMOR, -3.4, 55.4, 3.6, 3.4, 58.6, 4.6);
        hull.box(ARMOR, -1.9, 51.0, 4.0, 1.9, 51.9, 4.9);
        for (int s = -1; s <= 1; s += 2) {
            hull.box(ARMOR, s * 1.9, 51.4, 3.4, s * 3.4, 55.4, 4.5);
            hull.box(TRIM, s * 1.9, 51.9, 4.5, s * 2.2, 55.4, 4.7);
            // Each eye a lens of light in the mask, slanting up to its outer end, under a brow that does the same.
            hull.box(DARK, s * 0.45, 55.9, 4.6, s * 2.9, 57.5, 4.7);
            hull.box(EYE, new Vector3f(s * 1.65F, 56.7F, 4.78F), new Vector3f(1.0F, 0.5F, 0.08F),
                    new Quaternionf().rotationZ(s * 0.14F));
            hull.box(ARMOR, new Vector3f(s * 1.7F, 58.0F, 4.75F), new Vector3f(1.8F, 0.4F, 0.45F),
                    new Quaternionf().rotationZ(s * 0.22F));
        }
    }

    private void helmet() {
        GalactusHull hull = h(GalactusRig.HEAD);
        hull.box(ARMOR, -4.4, 51.0, -5.8, 4.4, 61.0, -1.0);
        hull.box(ARMOR, -4.3, 58.6, -5.6, 4.3, 61.4, 4.3);
        hull.box(TRIM, -4.5, 61.0, -5.9, 4.5, 61.8, 4.7);
        // The crown: a tall tower stepping in to its peak, a ridge up its front.
        hull.box(ARMOR, -3.6, 61.8, -4.4, 3.6, 66.0, 2.6);
        hull.box(ARMOR, -2.9, 66.0, -3.8, 2.9, 67.8, 2.0);
        hull.box(ARMOR, -2.0, 67.8, -3.0, 2.0, 69.2, 1.2);
        hull.box(ARMOR_DARK, -1.0, 69.2, -2.2, 1.0, 70.0, 0.4);
        hull.box(ARMOR, -0.6, 58.6, 4.3, 0.6, 61.4, 4.9);
        hull.box(ARMOR, -0.6, 61.8, 2.6, 0.6, 67.4, 3.3);
        for (int s = -1; s <= 1; s += 2) {
            hull.box(SILVER, s * 2.0, 59.5, 4.3, s * 3.0, 60.5, 4.5);
            hull.box(ARMOR, s * 3.2, 51.4, -1.0, s * 4.3, 58.2, 3.6);
            hull.box(TRIM, s * 4.3, 52.9, -2.9, s * 5.4, 57.5, 1.7);
            hull.box(SILVER, s * 5.4, 54.6, -1.2, s * 5.8, 55.8, 0.0);
            // The two great plates: each a broad flat arm slanting out and up from the ear to its elbow, then
            // standing tall from there, leaning in a little at its tip.
            hull.box(ARMOR, new Vector3f(s * 8.7F, 56.8F, -0.6F), new Vector3f(3.9F, 1.6F, 0.9F),
                    new Quaternionf().rotationZ(s * 0.42F));
            hull.box(ARMOR, new Vector3f(s * 12.2F, 58.6F, -0.6F), new Vector3f(1.7F, 1.7F, 0.9F), null);
            hull.box(ARMOR, new Vector3f(s * 11.9F, 65.0F, -0.6F), new Vector3f(1.7F, 6.2F, 0.9F),
                    new Quaternionf().rotationZ(s * 0.08F));
            hull.box(TRIM, new Vector3f(s * 11.9F, 65.0F, 0.4F), new Vector3f(0.8F, 5.2F, 0.1F),
                    new Quaternionf().rotationZ(s * 0.08F));
            hull.box(ARMOR_DARK, new Vector3f(s * 11.2F, 71.6F, -0.6F), new Vector3f(1.2F, 0.8F, 0.8F),
                    new Quaternionf().rotationZ(s * 0.08F));
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
