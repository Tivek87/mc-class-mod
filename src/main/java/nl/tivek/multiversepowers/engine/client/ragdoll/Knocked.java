package nl.tivek.multiversepowers.engine.client.ragdoll;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import nl.tivek.multiversepowers.engine.client.ragdoll.getup.GetUp;

// What the server says of the creatures a throw has down (Knockdowns): still flying, or the ticks left before it may
// move again. A limp body lies until just before then, never less than LIES on the ground, and gets up in time to
// stand before its creature moves on; one the server never held down gets up at once, as it may walk on already.
public final class Knocked {
    // A body down on the ground lies this long before it may get up: 3 seconds.
    static final int LIES = 60;
    private static final int FLYING = Integer.MAX_VALUE;
    // Standing this many ticks before its creature may move, so it is seen up before it walks on.
    private static final int MARGIN = 5;
    // A body the server has said nothing of waits this long for word before it gets up.
    private static final int GRACE = 6;
    // Lying while the server still has it flying, it gets up after this long all the same.
    private static final int LONGEST = 200;

    private static final Int2IntOpenHashMap LEFT = new Int2IntOpenHashMap();
    // Creatures the server has just sent flying (thrown, or hit as they got up), and on which tick word came.
    private static final Int2IntOpenHashMap AGAIN = new Int2IntOpenHashMap();
    private static int now;

    private Knocked() {
    }

    // Flying (below 0), lying `ticks` more, or free again (0).
    public static void told(int entity, int ticks) {
        if (ticks == 0) {
            LEFT.remove(entity);
        } else {
            LEFT.put(entity, ticks < 0 ? FLYING : ticks);
        }
        if (ticks < 0) {
            AGAIN.put(entity, now);
        }
    }

    // Whether word came this tick that the server sent the creature flying; asked once.
    static boolean again(int entity) {
        return AGAIN.containsKey(entity) && AGAIN.remove(entity) >= now - 1;
    }

    static void tick() {
        now++;
        AGAIN.int2IntEntrySet().removeIf(entry -> entry.getIntValue() < now - 1);
        ObjectIterator<Int2IntMap.Entry> all = LEFT.int2IntEntrySet().fastIterator();
        while (all.hasNext()) {
            Int2IntMap.Entry entry = all.next();
            int left = entry.getIntValue();
            if (left == FLYING) {
                continue;
            }
            if (left <= 1) {
                all.remove();
            } else {
                entry.setValue(left - 1);
            }
        }
    }

    // Whether the server holds the creature down with time enough left to lie before it gets up.
    static boolean down(int entity) {
        int left = LEFT.get(entity);
        return left == FLYING || left > LIES + GetUp.PERSON_TICKS + MARGIN;
    }

    // Whether a body down `down` ticks, `lain` of them on the ground, gets up now.
    static boolean getsUp(int entity, int down, int lain, boolean person) {
        if (!LEFT.containsKey(entity)) {
            return down >= GRACE;
        }
        int left = LEFT.get(entity);
        return left == FLYING ? down >= LONGEST : lain >= LIES && left <= GetUp.ticks(person) + MARGIN;
    }

    static void forget(int entity) {
        LEFT.remove(entity);
        AGAIN.remove(entity);
    }

    static void clear() {
        LEFT.clear();
        AGAIN.clear();
    }
}
