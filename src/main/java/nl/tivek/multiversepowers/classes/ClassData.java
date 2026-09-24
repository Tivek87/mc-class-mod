package nl.tivek.multiversepowers.classes;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * Stores the chosen class on the player itself, so it survives relogging and respawning.
 */
public final class ClassData {
    private static final String KEY = MultiversePowers.MODID + ":class";

    private ClassData() {
    }

    public static boolean hasClass(Player player) {
        return !player.getPersistentData().getString(KEY).isEmpty();
    }

    /** The chosen class, or null when the player has not picked one yet. */
    @Nullable
    public static PlayerClass getClass(Player player) {
        return PlayerClass.byId(player.getPersistentData().getString(KEY));
    }

    public static void setClass(Player player, PlayerClass playerClass) {
        player.getPersistentData().putString(KEY, playerClass.getId());
    }

    public static void copy(Player from, Player to) {
        String id = from.getPersistentData().getString(KEY);
        if (!id.isEmpty()) {
            to.getPersistentData().putString(KEY, id);
        }
    }
}
