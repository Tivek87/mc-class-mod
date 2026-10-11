package nl.tivek.multiversepowers.galactus.client;

import java.util.ArrayList;
import java.util.List;

// Galactus as boxes in model pixels, as the game's own creatures are built: feet on y 0, facing +z, his left on +x,
// some 68 pixels tall. His helmet with its tall ribbed crown, the discs at his ears and the two plates rising from
// them, his face under a visor with glowing eyes, a steel breastplate with a glowing round emblem, layered shoulder
// plates, ribbed bracers and boots, a belt and pointed skirt plates. The right arm is two parts of its own, so it can
// reach: the upper arm hanging from the shoulder (SHOULDER) and the forearm with the pointing hand from the elbow
// (ELBOW); its index finger is FINGER wide, its tip at TIP.
final class GalactusShape {
    enum Paint {
        HELM(0x6E3A78), HELM_LIGHT(0x9256A0), HELM_DARK(0x4A2552),
        STEEL(0x45557E), STEEL_LIGHT(0x5E73A6), STEEL_DARK(0x2C3755),
        SUIT(0x4A2738), SUIT_DARK(0x331A27),
        SKIN(0x9E8578), SKIN_DARK(0x7C665C),
        GLOW(0xBFF0FF), GLOW_DEEP(0x5FD0FF);

        final int rgb;

        Paint(int rgb) {
            this.rgb = rgb;
        }

        boolean glows() {
            return this == GLOW || this == GLOW_DEEP;
        }
    }

    record Box(float x0, float y0, float z0, float x1, float y1, float z1, Paint paint) {
    }

    // Where the right arm turns, in the body's pixels, and where its elbow is below that.
    static final float[] SHOULDER = { -11.2F, 43.5F, 0.0F };
    static final float ELBOW = -12.0F;
    // The index fingertip from the elbow, and how wide the finger is.
    static final float[] TIP = { -2.0F, -22.0F, 0.0F };
    static final float FINGER = 2.0F;

    final List<Box> body = new ArrayList<>();
    final List<Box> upper = new ArrayList<>();
    final List<Box> lower = new ArrayList<>();

    GalactusShape() {
        this.legs();
        this.waist();
        this.chest();
        this.leftArm();
        this.head();
        this.rightArm();
    }

    private static void box(List<Box> out, double x0, double y0, double z0, double x1, double y1, double z1,
            Paint paint) {
        out.add(new Box((float) Math.min(x0, x1), (float) Math.min(y0, y1), (float) Math.min(z0, z1),
                (float) Math.max(x0, x1), (float) Math.max(y0, y1), (float) Math.max(z0, z1), paint));
    }

    private void legs() {
        for (int s = -1; s <= 1; s += 2) {
            double c = 4.6 * s;
            box(this.body, c - 3.6, 0.0, -3.4, c + 3.6, 3.0, 5.4, Paint.HELM_DARK);
            box(this.body, c - 3.4, 2.4, 4.6, c + 3.4, 3.4, 5.6, Paint.HELM_LIGHT);
            box(this.body, c - 3.2, 3.0, -3.2, c + 3.2, 14.0, 3.2, Paint.HELM);
            for (double y : new double[] { 4.5, 7.5, 10.5 }) {
                box(this.body, c - 3.5, y, -3.5, c + 3.5, y + 1.2, 3.5, Paint.HELM_LIGHT);
            }
            box(this.body, c - 3.7, 13.0, -3.7, c + 3.7, 15.2, 3.7, Paint.HELM_LIGHT);
            box(this.body, c - 2.2, 14.0, 3.4, c + 2.2, 17.5, 4.3, Paint.STEEL);
            box(this.body, c - 2.9, 15.0, -2.9, c + 2.9, 27.0, 2.9, Paint.SUIT);
            box(this.body, c - 1.6, 17.5, 2.8, c + 1.6, 24.0, 3.3, Paint.SUIT_DARK);
        }
    }

    private void waist() {
        box(this.body, -7.6, 25.0, -3.6, 7.6, 30.0, 3.6, Paint.SUIT);
        box(this.body, -8.2, 28.0, -4.2, 8.2, 31.0, 4.2, Paint.HELM_DARK);
        box(this.body, -1.8, 28.3, 4.2, 1.8, 30.7, 4.9, Paint.STEEL_LIGHT);
        box(this.body, -0.6, 29.0, 4.9, 0.6, 30.0, 5.2, Paint.GLOW_DEEP);
        for (int s = -1; s <= 1; s += 2) {
            box(this.body, s * 0.6, 20.0, 4.2, s * 6.4, 28.0, 5.0, Paint.HELM);
            box(this.body, s * 1.4, 18.0, 4.2, s * 5.6, 20.0, 5.0, Paint.HELM);
            box(this.body, s * 2.4, 16.5, 4.2, s * 4.6, 18.0, 5.0, Paint.HELM);
            box(this.body, s * 0.9, 21.0, 5.0, s * 1.5, 27.5, 5.3, Paint.HELM_LIGHT);
            box(this.body, s * 7.8, 20.0, -3.2, s * 8.6, 28.0, 3.2, Paint.HELM_DARK);
        }
        box(this.body, -6.5, 18.0, -5.0, 6.5, 28.0, -4.2, Paint.HELM_DARK);
        box(this.body, -6.6, 30.0, -3.6, 6.6, 37.5, 3.6, Paint.SUIT);
        for (double y : new double[] { 31.5, 33.5, 35.5 }) {
            box(this.body, -4.2, y, 3.5, 4.2, y + 1.1, 4.0, Paint.SUIT_DARK);
        }
    }

    private void chest() {
        box(this.body, -8.2, 37.0, -4.6, 8.2, 47.0, 4.6, Paint.STEEL);
        box(this.body, -7.4, 38.5, 4.6, 7.4, 46.2, 5.4, Paint.STEEL_LIGHT);
        box(this.body, -8.2, 37.0, 4.4, 8.2, 38.6, 5.0, Paint.STEEL_DARK);
        box(this.body, -3.2, 40.3, 5.4, 3.2, 45.7, 5.9, Paint.STEEL_DARK);
        box(this.body, -2.4, 41.1, 5.9, 2.4, 44.9, 6.2, Paint.HELM);
        box(this.body, -1.2, 42.2, 6.2, 1.2, 43.8, 6.5, Paint.GLOW_DEEP);
        box(this.body, -4.8, 46.5, -3.8, 4.8, 48.6, 3.8, Paint.STEEL_DARK);
        box(this.body, -2.2, 47.5, -2.2, 2.2, 49.5, 2.2, Paint.SKIN_DARK);
        for (int s = -1; s <= 1; s += 2) {
            box(this.body, s * 7.2, 42.5, -4.8, s * 13.4, 48.0, 4.8, Paint.STEEL);
            box(this.body, s * 7.0, 47.6, -4.4, s * 12.8, 49.0, 4.4, Paint.STEEL_LIGHT);
            box(this.body, s * 9.0, 41.0, -4.2, s * 13.9, 43.0, 4.2, Paint.STEEL_DARK);
        }
    }

    // His left arm hangs at his side, the fist closed.
    private void leftArm() {
        box(this.body, 9.2, 31.0, -2.1, 13.2, 43.0, 2.1, Paint.SUIT);
        box(this.body, 8.6, 20.0, -2.6, 13.8, 31.0, 2.6, Paint.HELM);
        for (double y : new double[] { 21.5, 24.0, 26.5 }) {
            box(this.body, 8.3, y, -2.9, 14.1, y + 1.1, 2.9, Paint.HELM_LIGHT);
        }
        box(this.body, 8.2, 29.0, -3.0, 14.2, 31.4, 3.0, Paint.HELM_LIGHT);
        box(this.body, 8.4, 14.5, -1.9, 14.0, 20.0, 1.9, Paint.HELM_DARK);
        box(this.body, 8.8, 12.8, -1.4, 13.6, 14.5, 1.4, Paint.HELM_DARK);
    }

    private void head() {
        box(this.body, -3.0, 48.6, 0.8, 3.0, 56.2, 4.3, Paint.SKIN);
        box(this.body, -0.5, 51.3, 4.3, 0.5, 54.0, 4.9, Paint.SKIN);
        box(this.body, -1.4, 50.2, 4.3, 1.4, 50.8, 4.5, Paint.SKIN_DARK);
        box(this.body, -2.0, 48.4, 3.6, 2.0, 49.8, 4.5, Paint.HELM_DARK);
        box(this.body, -4.6, 48.6, -4.2, 4.6, 58.5, 1.2, Paint.HELM);
        for (int s = -1; s <= 1; s += 2) {
            box(this.body, s * 3.0, 48.6, 0.8, s * 4.6, 55.5, 4.6, Paint.HELM);
        }
        box(this.body, -4.7, 54.0, 3.6, 4.7, 56.2, 4.9, Paint.HELM_LIGHT);
        box(this.body, -2.6, 54.4, 4.9, -0.8, 55.2, 5.2, Paint.GLOW);
        box(this.body, 0.8, 54.4, 4.9, 2.6, 55.2, 5.2, Paint.GLOW);
        box(this.body, -4.4, 56.2, -3.9, 4.4, 58.5, 4.5, Paint.HELM);
        // The crown, narrowing as it climbs, ribbed up its front.
        double[][] tiers = { { 58.5, 61.5, 4.3, 3.9 }, { 61.5, 64.5, 3.9, 3.5 }, { 64.5, 67.2, 3.5, 3.1 } };
        for (double[] tier : tiers) {
            box(this.body, -tier[2], tier[0], -tier[3], tier[2], tier[1], tier[3], Paint.HELM);
            for (double x : new double[] { -2.9, -1.8, 1.8, 2.9 }) {
                box(this.body, x - 0.25, tier[0], tier[3], x + 0.25, tier[1], tier[3] + 0.4, Paint.HELM_DARK);
            }
        }
        box(this.body, -3.0, 67.2, -2.6, 3.0, 68.2, 2.6, Paint.HELM_LIGHT);
        box(this.body, -1.6, 57.5, 4.4, 1.6, 62.5, 5.0, Paint.HELM_LIGHT);
        box(this.body, -0.5, 60.5, 5.0, 0.5, 62.5, 5.2, Paint.HELM_DARK);
        // The discs at his ears and the plates rising from them, leaning out as they climb.
        for (int s = -1; s <= 1; s += 2) {
            box(this.body, s * 4.5, 50.8, -1.6, s * 5.6, 55.2, 2.6, Paint.STEEL_DARK);
            box(this.body, s * 5.6, 51.8, -0.6, s * 6.2, 54.2, 1.6, Paint.HELM_LIGHT);
            box(this.body, s * 6.2, 52.2, -0.9, s * 9.6, 54.0, 1.0, Paint.HELM);
            double[][] steps = { { 52.0, 55.0, 9.0 }, { 55.0, 58.0, 9.5 }, { 58.0, 61.0, 10.1 }, { 61.0, 64.0, 10.7 },
                    { 64.0, 66.8, 11.3 } };
            for (double[] step : steps) {
                box(this.body, s * step[2], step[0], -1.4, s * (step[2] + 2.2), step[1], 1.4, Paint.HELM);
                box(this.body, s * (step[2] - 0.3), step[0] + 0.6, -0.8, s * step[2], step[1] - 0.6, 0.8,
                        Paint.HELM_DARK);
            }
            box(this.body, s * 11.0, 66.8, -1.2, s * 13.2, 67.6, 1.2, Paint.HELM_LIGHT);
        }
    }

    // The right arm on its own two parts, each about its joint: the upper arm, then the forearm and the pointing hand.
    private void rightArm() {
        box(this.upper, -2.0, ELBOW, -2.1, 2.0, 0.5, 2.1, Paint.SUIT);
        box(this.lower, -2.6, -11.0, -2.6, 2.6, 0.0, 2.6, Paint.HELM);
        for (double y : new double[] { -9.5, -7.0, -4.5 }) {
            box(this.lower, -2.9, y, -2.9, 2.9, y + 1.1, 2.9, Paint.HELM_LIGHT);
        }
        box(this.lower, -3.0, -2.4, -3.0, 3.0, 0.4, 3.0, Paint.HELM_LIGHT);
        box(this.lower, -3.0, -16.0, -1.8, 3.0, -11.0, 1.8, Paint.HELM_DARK);
        double tipX = TIP[0];
        double half = FINGER / 2.0;
        box(this.lower, tipX - half, -18.2, -half, tipX + half, -16.0, half, Paint.HELM_DARK);
        box(this.lower, tipX - half - 0.1, -18.6, -half - 0.1, tipX + half + 0.1, -18.2, half + 0.1, Paint.HELM);
        box(this.lower, tipX - half, TIP[1], -half, tipX + half, -18.6, half, Paint.HELM_DARK);
        for (int i = 0; i < 3; i++) {
            double x0 = -1.0 + i * 1.33;
            box(this.lower, x0, -17.4, -1.0, x0 + 1.25, -16.0, 2.6, Paint.HELM_DARK);
        }
        box(this.lower, 1.2, -15.0, 1.8, 3.2, -13.0, 3.2, Paint.HELM_DARK);
    }
}
