package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;

public final class ScanGlow {
    private static final Map<Integer, Integer> GLOWING = new HashMap<>();

    private ScanGlow() {
    }

    public static int colour(int entity) {
        Integer colour = GLOWING.get(entity);
        return colour == null ? -1 : colour;
    }

    public static void set(Map<Integer, Integer> glowing) {
        GLOWING.clear();
        GLOWING.putAll(glowing);
    }

    public static void clear() {
        GLOWING.clear();
    }
}
