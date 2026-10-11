package nl.tivek.multiversepowers.stage.client;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

// A slider over a lamp's number, `least` to `most` in steps of `step`, showing the number as `shown` reads it; a
// `curved` one gives small numbers as much room as big ones (a beam 0.01 or 4 wide).
final class LampSlider extends AbstractSliderButton {
    private final double least;
    private final double most;
    private final double step;
    private final boolean curved;
    private final DoubleFunction<Component> shown;
    private final DoubleConsumer set;

    LampSlider(int x, int y, int width, int height, double value, double least, double most, double step,
            boolean curved, DoubleFunction<Component> shown, DoubleConsumer set) {
        super(x, y, width, height, Component.empty(), curved
                ? Math.log(Mth.clamp(value, least, most) / least) / Math.log(most / least)
                : (Mth.clamp(value, least, most) - least) / (most - least));
        this.least = least;
        this.most = most;
        this.step = step;
        this.curved = curved;
        this.shown = shown;
        this.set = set;
        this.updateMessage();
    }

    double number() {
        double raw = this.curved ? this.least * Math.pow(this.most / this.least, this.value)
                : this.least + this.value * (this.most - this.least);
        return Mth.clamp(Math.round(raw / this.step) * this.step, this.least, this.most);
    }

    @Override
    protected void updateMessage() {
        this.setMessage(this.shown.apply(this.number()));
    }

    @Override
    protected void applyValue() {
        this.set.accept(this.number());
    }
}
