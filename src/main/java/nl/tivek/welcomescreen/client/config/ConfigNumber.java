package nl.tivek.welcomescreen.client.config;

import java.math.BigDecimal;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.config.Unit;

/**
 * One number on a settings page: what it is called and what it does, what it counts, how far it may go, what
 * the mod itself would have, and where it is kept.
 *
 * @param label        its name on the screen
 * @param description  what it does, shown when you point at its name
 * @param unit         what it counts, so the screen can say what the number means
 * @param step         how much the - and + buttons change it
 * @param whole        true for a count without decimals
 * @param defaultValue what the mod itself has
 * @param stored       what the settings file holds now
 * @param store        puts a new number in the settings file (written to disk when the page is saved)
 */
public record ConfigNumber(Component label, Component description, Unit unit, double min, double max, double step,
        boolean whole, double defaultValue, DoubleSupplier stored, DoubleConsumer store) {

    /** The number as it is typed in its box: whole, or with at most three decimals and no trailing zeros. */
    public String format(double value) {
        if (this.whole) {
            return Long.toString(Math.round(value));
        }
        return BigDecimal.valueOf(Math.round(value * 1000.0) / 1000.0).stripTrailingZeros().toPlainString();
    }

    /** {@code value} kept within the limits, and whole when it has to be. */
    public double clamp(double value) {
        double kept = Math.max(this.min, Math.min(this.max, value));
        return this.whole ? Math.round(kept) : kept;
    }
}
