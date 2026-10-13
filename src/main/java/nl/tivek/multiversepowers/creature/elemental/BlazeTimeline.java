package nl.tivek.multiversepowers.creature.elemental;

import javax.annotation.Nullable;

// The Elemental Blaze's moves on one timeline for the server and every game that draws it. A cast first SHIFTs: the
// rod of its element swings round to the front and glows, so its foe sees what comes; then the element's own moment
// (CAST ticks after the shift) and its way back (END). A quick cast (the second of an Elemental Surge) shifts in half
// the time. AWAKEN is the flare into its second phase: rods flung wide, a burst of every element at BURST.
public final class BlazeTimeline {
    public static final int SHIFT = 16;
    public static final int QUICK = 8;
    public static final int AWAKEN = 11;
    public static final int KINDS = 11;
    public static final int BURST = 24;
    public static final int AWAKEN_END = 40;
    // The frost ray burns from the end of the shift for BEAM ticks; who stays in it FREEZE ticks is frozen.
    public static final int BEAM = 30;
    public static final int FREEZE = 12;
    // The earth rod tears its stone out of the ground for LIFT ticks before the throw.
    public static final int LIFT = 14;
    // The rod swings back into its ring over the last RETURN ticks of a move.
    public static final int RETURN = 8;
    private static final int[] CAST = { 14, 8, 0, 8, 6 };
    private static final int[] END = { 26, 18, 38, 20, 18 };

    private BlazeTimeline() {
    }

    public static int kind(Element element, boolean quick) {
        return element.ordinal() + 1 + (quick ? Element.ALL.length : 0);
    }

    // The element a move casts, or null (none, or the awakening).
    @Nullable
    public static Element element(int kind) {
        return kind >= 1 && kind <= 2 * Element.ALL.length ? Element.of(kind - 1) : null;
    }

    public static int shift(int kind) {
        return kind > Element.ALL.length ? QUICK : SHIFT;
    }

    // Ticks after the shift.
    public static int cast(Element element) {
        return CAST[element.ordinal()];
    }

    public static int end(Element element) {
        return END[element.ordinal()];
    }

    // How long the whole move lasts.
    public static int length(int kind) {
        Element element = element(kind);
        return kind == AWAKEN ? AWAKEN_END : element == null ? 0 : shift(kind) + end(element);
    }
}
