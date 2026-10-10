package nl.tivek.multiversepowers.character.greenlantern.client.body.heavy;

import static nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves.*;

// The heavy weapons' moves as keyframes, ticks from a move's start. A key puts the weapon in the chest's frame (pixels:
// +x his left, y down, -z ahead): the middle between his hands, the way its length points from there (turned `az` to
// his right from straight ahead, raised `el`), and a hint for its own up (the axe's blades lie along it, the chainsaw's
// top handle faces it, its teeth the other way). The trunk turns (+ to his right), leans (+ forward), rolls (+ to his
// left) and sinks (pixels).
final class HeavyKeys {
    record Key(float t, float gx, float gy, float gz, float az, float el, float ux, float uy, float uz, float twist,
            float pitch, float roll, float drop) {
    }

    private static final Key[][][] SCRIPTS = new Key[WEAPONS][MOVES][];
    private static final Key[] REST = new Key[WEAPONS];

    static {
        REST[AXE] = k(0, 0, 6, -6, -0.5F, 0.95F, -1, -1, 0, 0, 0, 0, 0);
        REST[SAW] = k(0, -1, 8, -6, 0.15F, -0.3F, 0, -1, 0, 0, 0, 0, 0);

        SCRIPTS[AXE][FORM] = keys(k(0, -3, 4, -7, 0.3F, 1.2F, -1, 0, 0, 0.15F, 0, 0, 0.4F), rest(AXE, 8),
                rest(AXE, 12));
        SCRIPTS[AXE][CHOP] = keys(rest(AXE, 0),
                k(3, -6, -1, -1, 2.5F, 0.9F, 1, 1, -1, 0.55F, -0.05F, 0.05F, 0.6F),
                k(6, 1, 8, -9, -0.5F, -0.35F, 1, 1, 0.3F, -0.55F, 0.25F, -0.08F, 1.6F),
                k(9, 4, 10, -5, -1.5F, -0.8F, 1, 1, 1, -0.75F, 0.3F, -0.05F, 1.8F),
                rest(AXE, 14));
        SCRIPTS[AXE][CHOP_BACK] = keys(rest(AXE, 0),
                k(3, 6, 5, -3, -2.2F, 0.15F, 0, 0, -1, -0.6F, 0, 0.05F, 1.0F),
                k(6, -1, 5, -9, 0.1F, 0.05F, -1, 0, 0, 0.2F, 0.12F, 0, 1.4F),
                k(9, -6, 5, -4, 1.7F, 0.15F, 0, 0, 1, 0.7F, 0.05F, -0.04F, 1.2F),
                rest(AXE, 14));
        SCRIPTS[AXE][CLEAVE] = keys(rest(AXE, 0),
                k(5, 0, -8, 1, 0, 2.5F, 0, -1, -0.3F, 0, -0.2F, 0, -0.3F),
                k(9, 0, 8, -9, 0, -0.7F, 0, 1, 0.5F, 0, 0.5F, 0, 3.0F),
                k(12, 0, 9, -9, 0, -0.8F, 0, 1, 0.5F, 0, 0.5F, 0, 3.0F),
                rest(AXE, 18));
        SCRIPTS[AXE][LEAP] = keys(rest(AXE, 0),
                k(5, -4, 2, 1, 1.8F, 1.6F, 0, -1, 0, 0.3F, 0.25F, 0, 4.0F),
                k(8, 0, -9, 1, 0, 2.6F, 0, -1, -1, 0, -0.15F, 0, -1.0F),
                k(15, 0, -10, 2, 0, 2.8F, 0, -1, -1, 0, -0.25F, 0, -1.0F),
                k(18, 0, 9, -10, 0, -1.0F, 0, 1, 0.3F, 0, 0.6F, 0, 5.0F),
                k(24, 0, 9, -10, 0, -1.0F, 0, 1, 0.3F, 0, 0.55F, 0, 4.6F),
                rest(AXE, 30));
        SCRIPTS[AXE][HOOK] = keys(rest(AXE, 0),
                k(4, -2, 3, -10, 0.15F, 0.35F, -1, 0, 0, -0.2F, 0.25F, 0, 1.0F),
                k(6, -1, 4, -12, 0.05F, 0.15F, -1, 0, 0, -0.1F, 0.35F, 0, 1.5F),
                k(10, -4, 6, -3, 0.9F, 0.5F, -1, 0, 1, 0.45F, -0.2F, 0.04F, 1.5F),
                rest(AXE, 16));
        Key out = k(4, -8, 8, -3, 1.45F, 0.05F, 0, 0, -1, 0.3F, 0.05F, 0, 1.5F);
        SCRIPTS[AXE][WHIRL] = keys(rest(AXE, 0), out, at(out, 14));
        SCRIPTS[AXE][WHIRL_OUT] = keys(at(out, 0),
                k(4, 2, 7, -9, -0.6F, 0, 0, 0, -1, -0.5F, 0.2F, 0, 1.8F),
                k(8, 5, 9, -5, -1.4F, -0.3F, 0, 0, 1, -0.7F, 0.15F, 0, 1.4F),
                rest(AXE, 14));

        SCRIPTS[SAW][FORM] = keys(k(0, -3, 6, -4, 0.3F, 0.2F, 0, -1, 0, 0.1F, 0, 0, 0.4F), rest(SAW, 8),
                rest(SAW, 12));
        SCRIPTS[SAW][REV] = keys(rest(SAW, 0),
                k(2, -6, 0, -4, 0.9F, 0.75F, -1, -1, 1, 0.45F, 0, 0.04F, 0.6F),
                k(5, 1, 8, -9, -0.4F, -0.3F, -1, -1, 0, -0.45F, 0.25F, -0.05F, 1.6F),
                k(8, 4, 10, -5, -1.0F, -0.6F, -1, -1, -0.5F, -0.6F, 0.2F, 0, 1.6F),
                rest(SAW, 12));
        SCRIPTS[SAW][REV_BACK] = keys(rest(SAW, 0),
                k(2, 5, 10, -4, -1.0F, -0.6F, 1, 1, 0, -0.4F, 0.15F, 0, 1.2F),
                k(5, -1, 4, -9, 0.3F, 0.3F, 1, 1, 0.5F, 0.35F, 0.1F, 0.04F, 1.0F),
                k(8, -6, -1, -5, 1.0F, 0.8F, 1, 1, 1, 0.6F, -0.05F, 0.04F, 0.6F),
                rest(SAW, 12));
        Key rend = k(4, 0, 6, -11, 0, -0.1F, 0, -1, 0, 0, 0.3F, 0, 2.0F);
        SCRIPTS[SAW][REND] = keys(rest(SAW, 0), rend, at(rend, 10));
        SCRIPTS[SAW][REND_OUT] = keys(at(rend, 0), rest(SAW, 8));
        SCRIPTS[SAW][IMPALE] = keys(rest(SAW, 0),
                k(3, -2, 7, -1, 0.05F, 0, 0, -1, 0, 0.35F, 0, 0, 1.2F),
                k(5, 0, 6, -13, 0, 0, 0, -1, 0, -0.25F, 0.35F, 0, 1.8F),
                k(11, 0, 6, -12, 0, 0.05F, 0, -1, 0, -0.25F, 0.35F, 0, 1.8F),
                k(13, 0, 2, -11, 0, 0.6F, 0, -1, 0.5F, -0.1F, 0.1F, 0, 0.8F),
                rest(SAW, 20));
        Key guard = k(4, 0, 1, -8, -1.1F, 0.65F, 0, 0, 1, 0.15F, 0.05F, 0, 1.0F);
        SCRIPTS[SAW][GUARD] = keys(rest(SAW, 0), guard, at(guard, 8));
        SCRIPTS[SAW][GUARD_DOWN] = keys(at(guard, 0), rest(SAW, 7));
    }

    private HeavyKeys() {
    }

    static Key[] of(int weapon, int move) {
        return SCRIPTS[weapon][move];
    }

    static Key rest(int weapon) {
        return REST[weapon];
    }

    private static Key rest(int weapon, float t) {
        return at(REST[weapon], t);
    }

    private static Key at(Key key, float t) {
        return new Key(t, key.gx(), key.gy(), key.gz(), key.az(), key.el(), key.ux(), key.uy(), key.uz(),
                key.twist(), key.pitch(), key.roll(), key.drop());
    }

    private static Key k(float t, float gx, float gy, float gz, float az, float el, float ux, float uy, float uz,
            float twist, float pitch, float roll, float drop) {
        return new Key(t, gx, gy, gz, az, el, ux, uy, uz, twist, pitch, roll, drop);
    }

    private static Key[] keys(Key... keys) {
        return keys;
    }
}
