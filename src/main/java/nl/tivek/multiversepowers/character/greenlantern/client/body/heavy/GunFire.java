package nl.tivek.multiversepowers.character.greenlantern.client.body.heavy;

import nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyShots;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves.*;

// The revolvers', cannon's and minigun's shots as every game works them out from the move and its clock, on the same
// ticks the server fires them (HeavyShots): which hand each leaves from, how hard it kicks, how long since the last.
final class GunFire {
    // Ticks a shot's kick lasts.
    private static final double KICK = 5.0;

    private GunFire() {
    }

    // Whether this held weapon fires rounds of its own timeline.
    static boolean of(ClientHeavy.Held held) {
        return held.weapon >= REVOLVERS && held.brokeAt < 0.0;
    }

    // How many dead-eye marks the guns have shot so far, as the server's rounds left tell it.
    private static int marks(ClientHeavy.Held held) {
        return held.weapon == REVOLVERS && held.move == UNBRACE ? Math.max(0, held.startAmmo - held.ammo) : 0;
    }

    // Whether shot `index` of this move (0 the first) is really fired: the revolvers stop once their rounds run out.
    private static boolean real(ClientHeavy.Held held, int index) {
        return held.weapon != REVOLVERS || index < held.startAmmo;
    }

    // The hand shot `index` leaves from: 0 right, 1 left (the minigun and the cannon, always the right).
    static int side(ClientHeavy.Held held, int index) {
        return held.weapon == REVOLVERS ? HeavyShots.side(held.startAmmo - index) : 0;
    }

    // How many shots this move has fired by `age` ticks in.
    static int count(ClientHeavy.Held held, double age) {
        if (!of(held) || held.move < 0 || held.move >= MOVES) {
            return 0;
        }
        int marks = marks(held);
        int count = 0;
        for (int tick = 0; tick <= (int) Math.floor(age); tick++) {
            if (HeavyShots.fires(held.weapon, held.move, tick, held.spun(), marks) && real(held, count)) {
                count++;
            }
        }
        return count;
    }

    // How far `side`'s gun is thrown back now by its last shots, 0 at rest to about 1.
    static double kick(ClientHeavy.Held held, int side, double age) {
        return since(held, side, age, true);
    }

    // Ticks since `side`'s gun last fired in this move, or a large number.
    static double last(ClientHeavy.Held held, int side, double age) {
        return since(held, side, age, false);
    }

    private static double since(ClientHeavy.Held held, int side, double age, boolean kick) {
        if (!of(held) || held.move < 0 || held.move >= MOVES) {
            return kick ? 0.0 : 1.0E3;
        }
        int marks = marks(held);
        int index = 0;
        double sum = 0.0;
        double last = 1.0E3;
        for (int tick = 0; tick <= (int) Math.floor(age); tick++) {
            if (!HeavyShots.fires(held.weapon, held.move, tick, held.spun(), marks) || !real(held, index)) {
                continue;
            }
            if (side(held, index) == side) {
                double s = age - tick;
                last = s;
                sum += Ease.jolt(s / KICK);
            }
            index++;
        }
        return kick ? Math.min(1.4, sum) : last;
    }
}
