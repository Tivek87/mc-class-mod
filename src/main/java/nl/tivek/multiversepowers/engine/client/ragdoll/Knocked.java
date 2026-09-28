package nl.tivek.multiversepowers.engine.client.ragdoll;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

// What the server says of the creatures a throw has down (Knockdowns): still flying, or the ticks left before it may
// move again. A limp body lies until just before then and gets up in time to stand before its creature moves on; one
// the server never held down gets up at once, as it may walk on already.
public final class Knocked {
    private static final int FLYING = Integer.MAX_VALUE;
    // Standing this many ticks before its creature may move, so it is seen up before it walks on.
    private static final int MARGIN = 5;
    // A body the server has said nothing of waits this long for word before it gets up.
    private static final int GRACE = 6;
    // Lying while the server still has it flying, it gets up after this long all the same.
    private static final int LONGEST = 200;

    private static final Int2IntOpenHashMap LEFT = new Int2IntOpenHashMap();

    private Knocked() {
    }

    // Flying (below 0), lying `ticks` more, or free again (0).
    public static void told(int entity, int ticks) {
        if (ticks == 0) {
            LEFT.remove(entity);
        } else {
            LEFT.put(entity, ticks < 0 ? FLYING : ticks);
        }
    }

    static void tick() {
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
        return left == FLYING || left > GetUp.PERSON_TICKS + MARGIN;
    }

    // Whether a body that has lain `lain` ticks gets up now.
    static boolean getsUp(int entity, int lain, boolean person) {
        if (!LEFT.containsKey(entity)) {
            return lain >= GRACE;
        }
        int left = LEFT.get(entity);
        return left == FLYING ? lain >= LONGEST : left <= GetUp.ticks(person) + MARGIN;
    }

    static void forget(int entity) {
        LEFT.remove(entity);
    }

    static void clear() {
        LEFT.clear();
    }
}
