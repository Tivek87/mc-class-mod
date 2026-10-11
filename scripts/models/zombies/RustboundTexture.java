import java.nio.file.Path;

// Paints the Rustbound for the boxes of creature/rustbound/client/RustboundModel: rustbound.png (grey dead skin
// stained with rust, a bolted skull and a jaw wired shut, a torn coat open over the chest, rusted scrap plates riveted
// down its back, a fist of scrap on its right arm, copper wire round its thin left arm), rustbound_eyes.png (its eyes
// glowing a cold blue), rustbound_glow.png (the magnet core and the cracks running out of it, drawn over it as bright
// as it pulls) and, for creature/rustbound/client/ShardRenderer, rust_shard.png (a strip of rusted iron). Keep the
// boxes here as the models build them.
// From the repository root, with Java 22 or newer: java scripts/models/zombies/RustboundTexture.java
public final class RustboundTexture {
    private static final int[] SKIN = { 112, 118, 108 };
    private static final int[] DARK = { 84, 90, 82 };
    private static final int[] STAIN = { 130, 82, 52 };
    private static final int[] SOCKET = { 34, 36, 38 };
    private static final int[] GAP = { 36, 22, 20 };
    private static final int[] TOOTH = { 190, 180, 140 };
    private static final int[] NAIL = { 60, 56, 50 };
    private static final int[] RUST = { 142, 72, 36 };
    private static final int[] RUST_DARK = { 92, 46, 26 };
    private static final int[] RUST_LIGHT = { 182, 108, 54 };
    private static final int[] IRON = { 92, 94, 100 };
    private static final int[] IRON_DARK = { 56, 58, 64 };
    private static final int[] RIVET = { 160, 156, 146 };
    private static final int[] COAT = { 72, 60, 48 };
    private static final int[] COAT_DARK = { 48, 40, 32 };
    private static final int[] COPPER = { 176, 100, 58 };
    private static final int[] COPPER_DARK = { 112, 62, 38 };
    private static final int[] VERDIGRIS = { 92, 150, 126 };
    private static final int[] CORE = { 40, 62, 84 };
    private static final int[] CORE_LIGHT = { 150, 210, 240 };
    private static final int[] EYE = { 150, 220, 255 };
    private static final int[] GLOW = { 110, 200, 255 };
    private static final int[] GLOW_CORE = { 220, 245, 255 };

    private static final Paint.Box HEAD = new Paint.Box(0, 0, 7, 7, 7);
    private static final Paint.Box JAW = new Paint.Box(28, 0, 6, 2, 5);
    private static final Paint.Box SKULL_PLATE = new Paint.Box(50, 0, 1, 5, 5);
    private static final Paint.Box BOLT = new Paint.Box(64, 0, 1, 1, 3);
    private static final Paint.Box CHEST = new Paint.Box(0, 16, 8, 8, 5);
    private static final Paint.Box BELLY = new Paint.Box(26, 16, 7, 4, 4);
    private static final Paint.Box COAT_BOX = new Paint.Box(48, 16, 8, 11, 5);
    private static final Paint.Box CORE_BOX = new Paint.Box(76, 16, 3, 3, 1);
    private static final Paint.Box[] PLATES = { new Paint.Box(0, 96, 6, 5, 1), new Paint.Box(14, 96, 5, 4, 1),
            new Paint.Box(26, 96, 4, 6, 1), new Paint.Box(38, 96, 7, 2, 1) };
    private static final Paint.Box REBAR = new Paint.Box(56, 96, 1, 7, 1);
    private static final Paint.Box REBAR_SHORT = new Paint.Box(60, 96, 1, 4, 1);
    private static final Paint.Box UPPER = new Paint.Box(0, 32, 3, 6, 3);
    private static final Paint.Box GAUNTLET = new Paint.Box(12, 32, 5, 7, 5);
    private static final Paint.Box FIST = new Paint.Box(32, 32, 5, 4, 5);
    private static final Paint.Box PAULDRON = new Paint.Box(52, 32, 6, 3, 6);
    private static final Paint.Box SPIKE = new Paint.Box(76, 32, 1, 3, 1);
    private static final Paint.Box THIN_ARM = new Paint.Box(0, 48, 2, 13, 2);
    private static final Paint.Box HAND = new Paint.Box(8, 48, 2, 3, 2);
    private static final Paint.Box WIRE_HIGH = new Paint.Box(16, 48, 2, 4, 2);
    private static final Paint.Box WIRE_LOW = new Paint.Box(24, 48, 2, 3, 2);
    private static final Paint.Box LEG = new Paint.Box(0, 64, 3, 12, 3);
    private static final Paint.Box RAGS = new Paint.Box(12, 64, 3, 7, 3);
    private static final Paint.Box SHIN_PLATE = new Paint.Box(28, 64, 4, 4, 1);
    private static final Paint.Box STRIP = new Paint.Box(0, 0, 1, 5, 1);
    private static final Paint.Box BAR = new Paint.Box(4, 0, 3, 1, 1);
    private static final Paint.Box STUB = new Paint.Box(0, 6, 1, 2, 1);

    private RustboundTexture() {
    }

    public static void main(String[] args) throws Exception {
        Path folder = Paint.textures(args, "entity");
        Paint body = new Paint(128, 128, 7331);
        body(body);
        body.save(folder, "rustbound");
        Paint eyes = new Paint(128, 128, 1);
        int[] f = HEAD.face(Paint.Face.FRONT);
        eyes.dot(f, 1, 3, EYE, 170);
        eyes.dot(f, 2, 3, EYE);
        eyes.dot(f, 4, 3, EYE);
        eyes.dot(f, 5, 3, EYE, 170);
        eyes.save(folder, "rustbound_eyes");
        Paint glow = new Paint(128, 128, 91);
        glow(glow);
        glow.save(folder, "rustbound_glow");
        Paint shard = new Paint(16, 16, 23);
        for (Paint.Box box : new Paint.Box[] { STRIP, BAR, STUB }) {
            metal(shard, box);
        }
        shard.save(folder, "rust_shard");
        System.out.println("painted");
    }

    private static void body(Paint p) {
        for (Paint.Box box : new Paint.Box[] { HEAD, JAW, CHEST, BELLY, UPPER, THIN_ARM, HAND }) {
            p.skin(box, SKIN, DARK, STAIN);
        }
        // Its face: deep sockets, a nose rotted to two holes, rust weeping down from the bolt and the plate.
        int[] f = HEAD.face(Paint.Face.FRONT);
        p.row(f, 2, DARK);
        for (int x : new int[] { 1, 2, 4, 5 }) {
            p.dot(f, x, 3, SOCKET);
        }
        p.dot(f, 3, 4, DARK);
        p.dot(f, 2, 5, SOCKET);
        p.dot(f, 4, 5, SOCKET);
        p.row(f, 6, DARK);
        for (int y = 0; y < 5; y++) {
            p.dot(f, 1, y, y < 2 ? RUST : STAIN);
        }
        p.dot(f, 6, 4, STAIN);
        p.dot(f, 6, 5, STAIN);
        int[] top = HEAD.face(Paint.Face.TOP);
        for (int k = 0; k < 5; k++) {
            p.dot(top, p.random.nextInt(7), p.random.nextInt(7), STAIN);
        }
        for (Paint.Face face : new Paint.Face[] { Paint.Face.LEFT, Paint.Face.RIGHT }) {
            int[] side = HEAD.face(face);
            p.veins(side, 2, STAIN);
        }
        // The jaw wired shut: broken teeth behind loops of copper wire.
        int[] lip = JAW.face(Paint.Face.FRONT);
        for (int x = 0; x < 6; x++) {
            p.dot(lip, x, 0, x % 2 == 0 ? TOOTH : GAP);
        }
        for (Paint.Face face : new Paint.Face[] { Paint.Face.FRONT, Paint.Face.LEFT, Paint.Face.RIGHT }) {
            int[] side = JAW.face(face);
            for (int x = 1; x < side[2]; x += 2) {
                p.column(side, x, COPPER);
            }
        }
        p.fill(JAW.face(Paint.Face.TOP), GAP, 6);
        rusted(p, SKULL_PLATE);
        rivets(p, SKULL_PLATE.face(Paint.Face.LEFT));
        rivets(p, SKULL_PLATE.face(Paint.Face.RIGHT));
        p.all(BOLT, IRON, 8);
        p.dot(BOLT.face(Paint.Face.FRONT), 0, 0, RIVET);
        // Chest and belly: grey and sunken, ribs showing, rust bleeding out round where the core sits.
        int[] chest = CHEST.face(Paint.Face.FRONT);
        for (int y = 1; y < 7; y += 2) {
            for (int x = 0; x < 8; x++) {
                if (x < 2 || x > 5) {
                    p.dot(chest, x, y, DARK);
                }
            }
        }
        for (int k = 0; k < 10; k++) {
            int x = 2 + p.random.nextInt(5);
            int y = 1 + p.random.nextInt(6);
            p.dot(chest, x, y, p.shade(k % 3 == 0 ? RUST : STAIN, 8));
        }
        p.veins(BELLY.face(Paint.Face.FRONT), 2, STAIN);
        // The coat: worn leather, torn open down the front, ragged at the hem and holed.
        for (Paint.Face face : Paint.Face.values()) {
            int[] c = COAT_BOX.face(face);
            p.fill(c, COAT, 7);
            p.row(c, 0, COAT_DARK);
            if (face != Paint.Face.TOP && face != Paint.Face.BOTTOM) {
                p.hem(c, 3);
                p.tear(c, face == Paint.Face.BACK ? 4 : 2);
                for (int k = 0; k < 3; k++) {
                    p.dot(c, p.random.nextInt(c[2]), p.random.nextInt(c[3]), STAIN);
                }
            }
        }
        int[] front = COAT_BOX.face(Paint.Face.FRONT);
        for (int y = 0; y < front[3]; y++) {
            int l = y < 1 ? 3 : 2 - (p.random.nextInt(3) == 0 ? 1 : 0);
            int r = y < 1 ? 4 : 5 + (p.random.nextInt(3) == 0 ? 1 : 0);
            for (int x = l; x <= r; x++) {
                p.clear(front, x, y);
            }
            p.dot(front, l - 1, y, COAT_DARK);
            p.dot(front, r + 1, y, COAT_DARK);
        }
        p.fill(COAT_BOX.face(Paint.Face.BOTTOM), COAT_DARK, 4);
        p.tear(COAT_BOX.face(Paint.Face.BOTTOM), 12);
        // The core: a dark disc of iron round a pale centre, wound with copper.
        for (Paint.Face face : Paint.Face.values()) {
            p.fill(CORE_BOX.face(face), CORE, 6);
        }
        int[] core = CORE_BOX.face(Paint.Face.FRONT);
        p.dot(core, 1, 1, CORE_LIGHT);
        p.dot(core, 0, 0, COPPER);
        p.dot(core, 2, 2, COPPER);
        p.dot(core, 2, 0, COPPER_DARK);
        p.dot(core, 0, 2, COPPER_DARK);
        // The scrap down its back, riveted on, and the rebar sticking out of it.
        for (Paint.Box plate : PLATES) {
            rusted(p, plate);
            rivets(p, plate.face(Paint.Face.BACK));
        }
        for (Paint.Box bar : new Paint.Box[] { REBAR, REBAR_SHORT }) {
            for (Paint.Face face : Paint.Face.values()) {
                int[] b = bar.face(face);
                p.fill(b, IRON_DARK, 6);
                for (int y = 0; y < b[3]; y += 2) {
                    p.row(b, y, RUST_DARK);
                }
            }
            p.fill(bar.face(Paint.Face.TOP), RUST_LIGHT, 8);
        }
        // The right arm: the shoulder grey and stained under the pauldron, then scrap riveted round the forearm and a
        // fist of dark iron, its knuckles worn bright.
        p.veins(UPPER.face(Paint.Face.FRONT), 1, STAIN);
        rusted(p, GAUNTLET);
        for (Paint.Face face : new Paint.Face[] { Paint.Face.FRONT, Paint.Face.RIGHT, Paint.Face.LEFT,
                Paint.Face.BACK }) {
            int[] g = GAUNTLET.face(face);
            p.row(g, 2, RUST_DARK);
            p.row(g, 5, RUST_DARK);
            p.dot(g, 0, 1, RIVET);
            p.dot(g, g[2] - 1, 4, RIVET);
            p.dot(g, 0, 4, RIVET);
            p.dot(g, g[2] - 1, 1, RIVET);
        }
        for (Paint.Face face : Paint.Face.values()) {
            int[] fist = FIST.face(face);
            p.fill(fist, IRON, 8);
            for (int k = 0; k < 4; k++) {
                p.dot(fist, p.random.nextInt(fist[2]), p.random.nextInt(fist[3]), RUST);
            }
        }
        int[] knuckles = FIST.face(Paint.Face.FRONT);
        for (int x = 0; x < 5; x += 2) {
            p.dot(knuckles, x, 1, RIVET);
        }
        p.row(knuckles, 2, IRON_DARK);
        p.fill(FIST.face(Paint.Face.BOTTOM), IRON_DARK, 6);
        rusted(p, PAULDRON);
        int[] shoulder = PAULDRON.face(Paint.Face.TOP);
        rivets(p, shoulder);
        p.row(shoulder, 2, RUST_DARK);
        p.row(shoulder, 3, RUST_LIGHT);
        for (Paint.Face face : Paint.Face.values()) {
            int[] s = SPIKE.face(face);
            p.fill(s, IRON_DARK, 6);
            p.dot(s, 0, s[3] - 1, RIVET);
        }
        // The thin left arm: dark nails, and copper wire wound tight round it, gone green in places.
        int[] hand = HAND.face(Paint.Face.FRONT);
        p.dot(hand, 0, 2, NAIL);
        p.dot(hand, 1, 2, NAIL);
        p.fill(HAND.face(Paint.Face.BOTTOM), DARK, 6);
        for (Paint.Box wire : new Paint.Box[] { WIRE_HIGH, WIRE_LOW }) {
            for (Paint.Face face : Paint.Face.values()) {
                int[] w = wire.face(face);
                for (int y = 0; y < w[3]; y++) {
                    for (int x = 0; x < w[2]; x++) {
                        if ((x + y) % 2 == 0) {
                            p.dot(w, x, y, p.shade(p.random.nextInt(5) == 0 ? VERDIGRIS : COPPER, 10));
                        } else {
                            p.dot(w, x, y, COPPER_DARK, p.random.nextInt(3) == 0 ? 255 : 0);
                        }
                    }
                }
            }
        }
        // Trousers worn through, rags tied round the thighs, a plate strapped to the right shin.
        for (Paint.Face face : Paint.Face.values()) {
            p.fill(LEG.face(face), COAT_DARK, 7);
        }
        for (Paint.Face face : new Paint.Face[] { Paint.Face.FRONT, Paint.Face.RIGHT, Paint.Face.LEFT,
                Paint.Face.BACK }) {
            int[] leg = LEG.face(face);
            p.row(leg, 11, DARK);
            p.dot(leg, p.random.nextInt(3), 7 + p.random.nextInt(3), SKIN);
            p.dot(leg, p.random.nextInt(3), 3 + p.random.nextInt(3), STAIN);
            int[] rags = RAGS.face(face);
            p.fill(rags, COAT, 9);
            p.row(rags, 1, COAT_DARK);
            p.hem(rags, 3);
            p.tear(rags, 2);
        }
        p.fill(RAGS.face(Paint.Face.TOP), COAT, 9);
        rusted(p, SHIN_PLATE);
        rivets(p, SHIN_PLATE.face(Paint.Face.FRONT));
    }

    // Rusted iron: a mottled orange-brown with darker pits and streaks running down where water stood on it.
    private static void rusted(Paint p, Paint.Box box) {
        for (Paint.Face face : Paint.Face.values()) {
            int[] f = box.face(face);
            p.skin(f, RUST, RUST_DARK, IRON_DARK);
            for (int k = 0; k < Math.max(1, f[2] * f[3] / 12); k++) {
                p.dot(f, p.random.nextInt(f[2]), p.random.nextInt(f[3]), p.shade(RUST_LIGHT, 8));
            }
            if (f[3] > 2) {
                int x = p.random.nextInt(f[2]);
                for (int y = p.random.nextInt(2); y < f[3]; y++) {
                    p.dot(f, x, y, RUST_DARK);
                }
            }
        }
    }

    private static void rivets(Paint p, int[] f) {
        p.dot(f, 0, 0, RIVET);
        p.dot(f, f[2] - 1, 0, RIVET);
        p.dot(f, 0, f[3] - 1, RIVET);
        p.dot(f, f[2] - 1, f[3] - 1, RIVET);
    }

    private static void metal(Paint p, Paint.Box box) {
        for (Paint.Face face : Paint.Face.values()) {
            int[] f = box.face(face);
            p.fill(f, RUST, 14);
            p.dot(f, p.random.nextInt(f[2]), p.random.nextInt(f[3]), IRON);
            p.dot(f, p.random.nextInt(f[2]), p.random.nextInt(f[3]), RUST_LIGHT);
            p.dot(f, 0, 0, RUST_DARK);
        }
    }

    // Only what glows: the core, white hot at its centre, and the cracks of blue light running out of it across the
    // chest, under the open coat.
    private static void glow(Paint p) {
        for (Paint.Face face : Paint.Face.values()) {
            int[] c = CORE_BOX.face(face);
            for (int y = 0; y < c[3]; y++) {
                for (int x = 0; x < c[2]; x++) {
                    p.dot(c, x, y, p.shade(GLOW, 12), 210);
                }
            }
        }
        p.dot(CORE_BOX.face(Paint.Face.FRONT), 1, 1, GLOW_CORE, 255);
        int[] chest = CHEST.face(Paint.Face.FRONT);
        int[][] cracks = { { 3, 3, -1, -1 }, { 5, 3, 1, -1 }, { 3, 5, -1, 1 }, { 5, 5, 1, 1 }, { 4, 6, 0, 1 } };
        for (int[] crack : cracks) {
            int x = crack[0];
            int y = crack[1];
            for (int step = 0; step < 3; step++) {
                p.dot(chest, x, y, GLOW, 190 - step * 50);
                x += crack[2] != 0 ? crack[2] : p.random.nextInt(3) - 1;
                y += crack[3] != 0 && p.random.nextBoolean() ? crack[3] : 0;
            }
        }
        for (int x = 3; x <= 5; x++) {
            for (int y = 3; y <= 5; y++) {
                p.dot(chest, x, y, GLOW, 150);
            }
        }
    }
}
