package nl.tivek.multiversepowers.galactus.client;

import nl.tivek.multiversepowers.galactus.client.GalactusHull.Paint;

// The pixels on Galactus's boxes, as a texture painted for the game would hold them: a plate's edges bevelled (lit
// above and left, shaded below and right) and speckled, a suit ribbed, trim riveted, the emblem's rings of silver and
// energy round its core, and cracks of energy wandering across a plate pixel by pixel.
final class GalactusPixels {
    private static final float[] SILVER = rgb(0x9497AE);
    private static final float[] DARK = rgb(0x0D0A13);
    private static final float[] ENERGY = rgb(0xA45CFF);
    private static final float[] CORE = rgb(0xE2C8FF);
    private static final float[] CRACK = rgb(0x2A1240);

    private GalactusPixels() {
    }

    // Red, green, blue, the pixel's own number, crack, glow.
    static float[] pixel(Paint paint, int face, int i, int j, int cols, int rows, int box, boolean crack) {
        float noise = hash(box, face, i, j);
        float[] base = { paint.r(), paint.g(), paint.b() };
        float light = 1.0F;
        float glow = 0.0F;
        boolean edge = cols > 2 && rows > 2;
        switch (paint.look()) {
            case PLATE -> {
                light = 0.9F + 0.12F * noise;
                if (edge && (i == 0 || j == 0)) {
                    light *= 1.18F;
                } else if (edge && (i == cols - 1 || j == rows - 1)) {
                    light *= 0.72F;
                }
            }
            case SUIT -> light = (i % 3 == 0 ? 0.78F : 0.95F) * (0.92F + 0.08F * noise);
            case TRIM -> {
                light = 0.9F + 0.1F * noise;
                if (i % 4 == 1 && j % 4 == 1 && cols > 3) {
                    base = SILVER;
                }
            }
            case PLAIN -> light = 0.93F + 0.07F * noise;
            case GLOW -> glow = 1.0F;
            case EMBLEM -> {
                if (face == 0) {
                    float x = (i + 0.5F) / cols * 2.0F - 1.0F;
                    float y = (j + 0.5F) / rows * 2.0F - 1.0F;
                    float r = (float) Math.sqrt(x * x + y * y);
                    if (r < 0.28F) {
                        base = CORE;
                        glow = 1.0F;
                    } else if (r < 0.52F) {
                        base = ENERGY;
                        glow = 1.0F;
                    } else if (r < 0.66F) {
                        base = DARK;
                    } else if (r < 0.94F) {
                        base = SILVER;
                        light = 0.88F + 0.12F * noise;
                    } else {
                        light = 0.9F + 0.1F * noise;
                    }
                } else {
                    light = 0.9F + 0.1F * noise;
                }
            }
        }
        if (crack) {
            return new float[] { CRACK[0], CRACK[1], CRACK[2], noise, 1.0F, 0.0F };
        }
        return new float[] { base[0] * light, base[1] * light, base[2] * light, noise, 0.0F, glow };
    }

    // One or two cracks wandering across a face from one side to the other, a pixel up or down at a time.
    static boolean[] cracks(int cols, int rows, int seed) {
        boolean[] out = new boolean[cols * rows];
        if (cols < 6 || rows < 4) {
            return out;
        }
        int count = 1 + (hash(seed, 1, 0, 0) > 0.6F ? 1 : 0);
        for (int c = 0; c < count; c++) {
            int j = 1 + (int) (hash(seed, 2, c, 0) * (rows - 2));
            int length = (int) (cols * (0.4F + 0.5F * hash(seed, 3, c, 0)));
            int start = (int) (hash(seed, 4, c, 0) * (cols - length));
            for (int i = start; i < start + length; i++) {
                out[j * cols + i] = true;
                float step = hash(seed, 5 + c, i, j);
                if (step < 0.25F && j > 1) {
                    j--;
                    out[j * cols + i] = true;
                } else if (step > 0.75F && j < rows - 2) {
                    j++;
                    out[j * cols + i] = true;
                }
            }
        }
        return out;
    }

    private static float hash(int a, int b, int c, int d) {
        int h = a * 0x27D4EB2D ^ b * 0x165667B1 ^ c * 0x9E3779B1 ^ d * 0x85EBCA77;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return (h >>> 8) / (float) (1 << 24);
    }

    private static float[] rgb(int rgb) {
        return new float[] { (rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F };
    }
}
