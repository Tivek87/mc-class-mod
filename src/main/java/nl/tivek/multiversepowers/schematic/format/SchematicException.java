package nl.tivek.multiversepowers.schematic.format;

import java.io.IOException;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;

// A schematic that cannot be read or used, with the line that tells the player why
// (`schematic.welcomescreen.error.<key>`).
public final class SchematicException extends IOException {
    private final String key;
    private final transient Object[] args;

    public SchematicException(String key, Object... args) {
        super(key);
        this.key = key;
        this.args = args;
    }

    public String key() {
        return this.key;
    }

    // One of the line's words as text, for a notice sent over the network; "" past the last.
    public String arg(int index) {
        return index < this.args.length ? String.valueOf(this.args[index]) : "";
    }

    public Component text() {
        return Component.translatable("schematic." + MultiversePowers.MODID + ".error." + this.key, this.args);
    }
}
