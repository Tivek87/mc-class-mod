package nl.tivek.welcomescreen.character.lantern;

import java.util.HashMap;
import java.util.Map;

/**
 * The creatures the ring's scan marked for you (see {@link RingScan}), by entity id, with the colour of their glow: the
 * game draws them with a glowing outline through walls, in that colour (see the hooks in
 * {@code nl.tivek.welcomescreen.mixin}). Only your own game fills it; on a server it stays empty. Kept here, apart from
 * the client's code, because the hook into every entity lives on both sides.
 */
public final class ScanGlow {
    private static final Map<Integer, Integer> GLOWING = new HashMap<>();

    private ScanGlow() {
    }

    /** The colour this entity glows in for you, or -1 when the scan did not mark it. */
    public static int colour(int entity) {
        Integer colour = GLOWING.get(entity);
        return colour == null ? -1 : colour;
    }

    /** From now on exactly these entities glow, each in its own colour. */
    public static void set(Map<Integer, Integer> glowing) {
        GLOWING.clear();
        GLOWING.putAll(glowing);
    }

    /** Nothing glows any more (you left the world). */
    public static void clear() {
        GLOWING.clear();
    }
}
