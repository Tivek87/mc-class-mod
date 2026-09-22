package nl.tivek.welcomescreen.client.classes;

import javax.annotation.Nullable;
import nl.tivek.welcomescreen.classes.PlayerClass;

/**
 * The local player's class as the server last told us, for showing it on screen.
 */
public final class ClientClassData {
    @Nullable
    private static PlayerClass current;

    private ClientClassData() {
    }

    @Nullable
    public static PlayerClass get() {
        return current;
    }

    public static void set(@Nullable PlayerClass playerClass) {
        current = playerClass;
    }
}
