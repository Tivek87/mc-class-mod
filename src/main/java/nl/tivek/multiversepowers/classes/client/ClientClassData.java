package nl.tivek.multiversepowers.classes.client;

import javax.annotation.Nullable;
import nl.tivek.multiversepowers.classes.PlayerClass;

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
