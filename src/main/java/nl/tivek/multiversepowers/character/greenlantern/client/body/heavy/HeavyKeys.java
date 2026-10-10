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
    // How each gun is held up to fire.
    private static final Key[] AIMED = new Key[WEAPONS];

    static {
        REST[AXE] = k(0, 0, 6, -6, -0.5F, 0.95F, -1, -1, 0, 0, 0, 0, 0);
        REST[SAW] = k(0, -1, 8, -6, 0.15F, -0.3F, 0, -1, 0, 0, 0, 0, 0);
        // The launcher carried low at his right side, its back end clear of his body; the shotgun held out before him.
        REST[RPG] = k(0, -5.5F, 6, -5, 0.15F, -0.15F, 0, -1, 0, 0, 0, 0, 0);
        REST[SHOTGUN] = k(0, -3, 6, -9, 0.05F, -0.15F, 0, -1, 0, 0, 0, 0, 0);

        // Grown hanging from his right hand, swung up in an arc over his right shoulder and set down on it.
        SCRIPTS[AXE][FORM] = keys(k(0, -4, 9, -4, 0.4F, -1.0F, -1, 0, 0, 0.15F, 0.1F, 0, 0.8F),
                k(7, -6, 2, -6, 0.6F, 0.6F, -1, -0.5F, 0, 0.35F, -0.05F, 0.03F, 0.4F),
                k(11, -3, -4, 0, 0.1F, 1.7F, -1, -1, 0, 0.15F, -0.15F, 0, 0.0F),
                k(15, 0, 7, -5, -0.5F, 0.85F, -1, -1, 0, 0, 0.12F, 0, 1.2F),
                rest(AXE, 20));
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
        // Drawn back over his right shoulder, flung out low and far so the beard bites down behind the target, then
        // torn back across his body to his left hip as he leans away, and brought round to rest.
        SCRIPTS[AXE][HOOK] = keys(rest(AXE, 0),
                k(3, -5, 0, -2, 0.55F, 1.35F, -1, 0, 0, 0.45F, -0.12F, 0.05F, 0.5F),
                k(6, -1, 5, -13, 0.05F, 0.1F, 0, 1, 0.3F, -0.25F, 0.45F, 0, 2.0F),
                k(8, -1, 6, -12, 0.0F, 0.0F, 0, 1, 0.4F, -0.2F, 0.4F, 0, 2.2F),
                k(11, 4, 8, -3, -0.6F, 0.2F, 0, 1, 0.8F, -0.6F, -0.2F, -0.06F, 1.6F),
                k(14, 3, 6, -5, -0.7F, 0.6F, -1, -0.5F, 0.5F, -0.3F, 0.0F, 0, 0.8F),
                rest(AXE, 20));
        Key out = k(4, -8, 8, -3, 1.45F, 0.05F, 0, 0, -1, 0.3F, 0.05F, 0, 1.5F);
        SCRIPTS[AXE][WHIRL] = keys(rest(AXE, 0), out, at(out, 14));
        SCRIPTS[AXE][WHIRL_OUT] = keys(at(out, 0),
                k(4, 2, 7, -9, -0.6F, 0, 0, 0, -1, -0.5F, 0.2F, 0, 1.8F),
                k(8, 5, 9, -5, -1.4F, -0.3F, 0, 0, 1, -0.7F, 0.15F, 0, 1.4F),
                rest(AXE, 14));

        // Grown hanging low and jerked up twice as its cord is pulled (with the sound's two pulls), thrust out as the
        // engine catches.
        SCRIPTS[SAW][FORM] = keys(k(0, -3, 9, -3, 0.2F, -0.9F, 0, -1, 0, 0.1F, 0.05F, 0, 0.6F),
                k(3, -3, 7, -3, 0.25F, -0.6F, 0, -1, 0, 0.25F, -0.05F, 0.02F, 0.4F),
                k(7, -2, 8, -5, 0.3F, -0.5F, 0, -1, 0, 0.25F, 0.15F, 0, 1.2F),
                k(11, -4, 5, -3, 0.25F, -0.1F, 0, -1, 0, 0.35F, -0.1F, 0.03F, 0.3F),
                k(14, -1, 7, -7, 0.1F, -0.2F, 0, -1, 0, 0.05F, 0.12F, 0, 1.0F),
                rest(SAW, 20));
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

        // The launcher on his right shoulder, aimed level; a shot kicks it up and it comes back to rest. Empty, it
        // reloads: lowered across him, the next rocket grows into its mouth, is pushed home and locks with a jolt.
        Key shoulder = k(0, -4, 1, -3, 0, 0, 0, -1, 0, 0.15F, 0, 0, 0.3F);
        AIMED[RPG] = shoulder;
        SCRIPTS[RPG][FORM] = keys(k(0, -2, 9, -4, 0.2F, -0.8F, 0, -1, 0, 0.1F, 0.05F, 0, 0.6F), at(shoulder, 8),
                at(shoulder, 13), rest(RPG, 20));
        SCRIPTS[RPG][SHOOT] = keys(rest(RPG, 0), at(shoulder, 2), at(shoulder, 3),
                k(5, -4, -1, 1, 0, 0.25F, 0, -1, 0, 0.2F, -0.2F, 0, 0.0F),
                k(9, -4, 1, -3, 0, 0.05F, 0, -1, 0, 0.15F, 0, 0, 0.3F), rest(RPG, 14));
        SCRIPTS[RPG][AIM] = keys(rest(RPG, 0), at(shoulder, 4),
                k(14, -4, 1.3F, -3, 0.01F, 0.015F, 0, -1, 0, 0.15F, 0, 0, 0.32F), at(shoulder, 24));
        SCRIPTS[RPG][LOOSE] = keys(at(shoulder, 0), at(shoulder, 2),
                k(4, -4, -1.5F, 1.5F, 0, 0.3F, 0, -1, 0, 0.22F, -0.25F, 0, -0.2F),
                k(8, -4, 1, -3, 0, 0.05F, 0, -1, 0, 0.15F, 0, 0, 0.3F), rest(RPG, 14));
        // Pointed down at his feet as he crouches, fired, and swung back up as the blast throws him.
        Key down = k(3, -3, 6, -3, 0, -1.35F, 0, 0, -1, 0.1F, 0.3F, 0, 1.5F);
        SCRIPTS[RPG][KICK] = keys(rest(RPG, 0), down, at(down, 4),
                k(7, -3, 3, -4, 0, -0.6F, 0, -1, -0.5F, 0, -0.2F, 0, -1.0F),
                k(12, -3, 4, -6, 0.1F, -0.3F, 0, -1, 0, 0, 0, 0, 0), rest(RPG, 18));
        // The guided rocket: shouldered, fired with a kick, the launcher held steady on the crosshair while he steers;
        // let go, it comes down to rest (and reloads).
        // A held move goes round from tick 4 (HeavyMoves.LOOP_FROM): the kick is over by then.
        SCRIPTS[RPG][BRACE] = keys(rest(RPG, 0), at(shoulder, 2),
                k(3, -4, -0.5F, 0, 0, 0.2F, 0, -1, 0, 0.18F, -0.15F, 0, 0.1F), at(shoulder, 4), at(shoulder, 12));
        SCRIPTS[RPG][UNBRACE] = keys(at(shoulder, 0), rest(RPG, 8));
        SCRIPTS[RPG][RELOAD] = keys(rest(RPG, 0),
                k(5, -1, 6, -7, -0.3F, -0.5F, 0.3F, -1, 0, 0, 0.2F, 0, 0.8F),
                k(10, -0.5F, 6.3F, -7, -0.33F, -0.55F, 0.3F, -1, 0, -0.03F, 0.22F, 0, 0.9F),
                k(15, 0, 6.6F, -7.5F, -0.35F, -0.55F, 0.3F, -1, 0, -0.05F, 0.25F, 0, 1.0F),
                k(18, -1, 5.4F, -6.5F, -0.3F, -0.45F, 0.3F, -1, 0, 0, 0.18F, 0, 0.7F),
                k(21, -1, 6, -7, -0.3F, -0.5F, 0.3F, -1, 0, 0, 0.2F, 0, 0.8F), rest(RPG, 26));

        // The sawed-off held low before him: each shot flips its barrels up; both barrels throw him back. Empty, it
        // reloads: tipped toward him as the barrels break open, jerked up to fling the spent shells out, two new ones
        // pressed in, and flicked up to snap it shut (the barrels and shells move in HeavyPainter).
        Key aim = k(0, -2, 2, -9, 0, 0.02F, 0, -1, 0, 0.12F, 0.08F, 0, 0.8F);
        AIMED[SHOTGUN] = aim;
        SCRIPTS[SHOTGUN][FORM] = keys(k(0, -2, 7, -6, 0.3F, -0.9F, 0, -1, 0, 0.1F, 0, 0, 0.4F),
                k(7, -2, 6, -7, 0.25F, -1.0F, 0, -1, 0, 0.1F, 0, 0, 0.4F),
                k(10, -3, 3, -8, 0.05F, 0.1F, 0, -1, 0, 0.1F, 0, 0, 0.2F),
                k(14, -3, 4, -8, 0.05F, 0, 0, -1, 0, 0.05F, 0, 0, 0.2F), rest(SHOTGUN, 20));
        SCRIPTS[SHOTGUN][SHOOT] = keys(rest(SHOTGUN, 0), k(2, -2, 3, -9, 0.02F, 0.02F, 0, -1, 0, 0.1F, 0.05F, 0,
                0.4F), k(4, -2, 1, -6, 0, 0.5F, 0, -1, 0.3F, 0.15F, -0.15F, 0.02F, 0.2F),
                k(8, -2, 3, -8, 0.02F, 0.08F, 0, -1, 0, 0.1F, 0, 0, 0.3F), rest(SHOTGUN, 16));
        SCRIPTS[SHOTGUN][AIM] = keys(rest(SHOTGUN, 0), at(aim, 4),
                k(10, -2, 2.3F, -9, 0.01F, 0.03F, 0, -1, 0, 0.12F, 0.08F, 0, 0.85F), at(aim, 16));
        SCRIPTS[SHOTGUN][LOOSE] = keys(at(aim, 0), at(aim, 2),
                k(4, -2, -1, -4, 0, 0.85F, 0, -1, 0.5F, 0.2F, -0.3F, 0.03F, -0.2F),
                k(8, -2, 3, -8, 0.02F, 0.08F, 0, -1, 0, 0.1F, 0.05F, 0, 0.4F), rest(SHOTGUN, 14));
        // Turned stock first and driven forward into the creature ahead.
        SCRIPTS[SHOTGUN][KICK] = keys(rest(SHOTGUN, 0), k(3, -1, 2, -4, 0.2F, 1.2F, 0, 0, 1, 0.25F, -0.1F, 0, 0.4F),
                k(5, 0, 3, -12, 0, 1.0F, 0, 0, 1, -0.2F, 0.3F, 0, 1.2F),
                k(8, -1, 3, -10, 0.1F, 0.9F, 0, 0, 1, -0.1F, 0.2F, 0, 1.0F), rest(SHOTGUN, 12));
        // Barrels raised across his face; let go, swung down and fired.
        Key deflect = k(4, 0, -2, -8, -0.9F, 0.9F, 0, 0, 1, 0.1F, 0.05F, 0, 0.8F);
        SCRIPTS[SHOTGUN][BRACE] = keys(rest(SHOTGUN, 0), deflect,
                k(7, 0, -1.8F, -8, -0.92F, 0.92F, 0, 0, 1, 0.1F, 0.05F, 0, 0.85F), at(deflect, 10));
        SCRIPTS[SHOTGUN][UNBRACE] = keys(at(deflect, 0), k(2, -2, 3, -9, 0, 0.05F, 0, -1, 0, 0.1F, 0.1F, 0, 0.6F),
                k(4, -2, 1, -6, 0, 0.5F, 0, -1, 0.3F, 0.15F, -0.15F, 0, 0.3F), rest(SHOTGUN, 12));
        Key open = k(4, -2, 4.5F, -7, 0, -0.3F, 0, -1, 0.45F, 0.1F, 0.15F, 0, 0.6F);
        SCRIPTS[SHOTGUN][RELOAD] = keys(rest(SHOTGUN, 0), open, at(open, 6),
                k(8, -2, 3, -7, 0, 0.15F, 0, -1, 0.2F, 0.1F, 0.05F, 0, 0.4F), at(open, 11), at(open, 12.5F),
                k(14, -2, 4.9F, -7, 0, -0.32F, 0, -1, 0.45F, 0.1F, 0.17F, 0, 0.7F), at(open, 16), at(open, 17.5F),
                k(19, -2, 4.9F, -7, 0, -0.32F, 0, -1, 0.45F, 0.1F, 0.17F, 0, 0.7F), at(open, 21),
                k(23, -2, 5, -7.5F, 0, -0.4F, 0, -1, 0.3F, 0.1F, 0.18F, 0, 0.7F),
                k(25, -2, 3, -8.5F, 0.02F, 0.1F, 0, -1, 0, 0.12F, 0.05F, 0, 0.5F), rest(SHOTGUN, 32));
    }

    private HeavyKeys() {
    }

    static Key[] of(int weapon, int move) {
        return SCRIPTS[weapon][move];
    }

    static Key rest(int weapon) {
        return REST[weapon];
    }

    static Key aimed(int weapon) {
        return AIMED[weapon];
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
