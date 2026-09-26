package nl.tivek.multiversepowers.config.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.config.Unit;

public record ConfigNumber(Component label, Component description, Unit unit, double min, double max, double step,
        boolean whole, double defaultValue, DoubleSupplier stored, DoubleConsumer store, String file,
        List<String> path) {

    public String format(double value) {
        if (this.whole) {
            return Long.toString(Math.round(value));
        }
        return BigDecimal.valueOf(Math.round(value * 1000.0) / 1000.0).stripTrailingZeros().toPlainString();
    }

    public double clamp(double value) {
        double kept = Math.max(this.min, Math.min(this.max, value));
        return this.whole ? Math.round(kept) : kept;
    }
}
