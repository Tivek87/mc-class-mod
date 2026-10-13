package nl.tivek.multiversepowers.bugreport.client.form;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

// One question of a form: its kind, limits, default, and when it shows, must be filled or is locked to an answer by
// the others. Its words are `field.<id>` in en_us.json (`.tip`, `.hint`, each option's `.o.<option>` and its `.tip`).
public final class Field {
    public enum Type { TEXT, AREA, CHOICE, MULTI, NUMBER, LIST }

    public enum Unit { COUNT, HEALTH, BLOCKS, PER_SECOND }

    final String id;
    final Type type;
    int max;
    int lines = 3;
    List<String> options = List.of();
    int[] colors = new int[0];
    Set<String> shut = Set.of();
    String fallback = "";
    double low;
    double high;
    boolean decimals;
    boolean reported = true;
    Unit unit = Unit.COUNT;
    @Nullable
    Function<Values, int[]> range;
    Predicate<Values> required = values -> false;
    Predicate<Values> shown = values -> true;
    Function<Values, String> forced = values -> null;
    String forcedWhy = "";
    String count = "";
    int nameMax;

    private Field(String id, Type type) {
        this.id = id;
        this.type = type;
    }

    public static Field text(String id, int max) {
        Field field = new Field(id, Type.TEXT);
        field.max = max;
        return field;
    }

    public static Field area(String id, int max, int lines) {
        Field field = new Field(id, Type.AREA);
        field.max = max;
        field.lines = lines;
        return field;
    }

    public static Field choice(String id, String... options) {
        Field field = new Field(id, Type.CHOICE);
        field.options = List.of(options);
        return field;
    }

    public static Field yesNo(String id, String fallback) {
        return choice(id, "yes", "no").fallback(fallback);
    }

    public static Field multi(String id, String... options) {
        Field field = new Field(id, Type.MULTI);
        field.options = List.of(options);
        return field;
    }

    public static Field number(String id, double low, double high, Unit unit, double fallback) {
        Field field = new Field(id, Type.NUMBER);
        field.low = low;
        field.high = high;
        field.unit = unit;
        field.fallback = format(fallback);
        return field;
    }

    // Entries of a name and a description, as many as the number field `count` says.
    public static Field list(String id, String count, int nameMax, int descriptionMax) {
        Field field = new Field(id, Type.LIST);
        field.count = count;
        field.nameMax = nameMax;
        field.max = descriptionMax;
        return field;
    }

    public Field required() {
        this.required = values -> true;
        return this;
    }

    public Field requiredWhen(Predicate<Values> when) {
        this.required = when;
        return this;
    }

    public Field when(Predicate<Values> when) {
        this.shown = when;
        return this;
    }

    public Field fallback(String value) {
        this.fallback = value;
        return this;
    }

    public Field colors(int... colors) {
        this.colors = colors;
        return this;
    }

    // Left out of the report's blocks: said elsewhere (its header, a list's heading).
    public Field unreported() {
        this.reported = false;
        return this;
    }

    public Field decimals() {
        this.decimals = true;
        return this;
    }

    // Options that cannot be picked yet: drawn with a lock, their tooltip saying `field.<id>.locked`.
    public Field shut(String... options) {
        this.shut = new LinkedHashSet<>(Arrays.asList(options));
        return this;
    }

    // The bounds of a number as the other answers set them, within `low` and `high`.
    public Field range(Function<Values, int[]> range) {
        this.range = range;
        return this;
    }

    // An answer the others decide: shown, but not changed, with why as `field.<id>.<why>`.
    public Field lockedTo(Function<Values, String> forced, String why) {
        this.forced = forced;
        this.forcedWhy = why;
        return this;
    }

    public String id() {
        return this.id;
    }

    public Type type() {
        return this.type;
    }

    public int max() {
        return this.max;
    }

    public int lines() {
        return this.lines;
    }

    public int nameMax() {
        return this.nameMax;
    }

    public List<String> options() {
        return this.options;
    }

    public boolean isShut(String option) {
        return this.shut.contains(option);
    }

    public int color(String option) {
        int index = this.options.indexOf(option);
        return index >= 0 && index < this.colors.length ? this.colors[index] : 0;
    }

    public boolean shown(Values values) {
        return this.shown.test(values);
    }

    public boolean required(Values values) {
        return this.required.test(values);
    }

    @Nullable
    public String forced(Values values) {
        return this.forced.apply(values);
    }

    public Component why() {
        return Form.words("field." + this.id + "." + this.forcedWhy);
    }

    // What is stored, or the default; a number kept in its bounds, a choice one of its options.
    String value(Values values) {
        String forced = this.forced(values);
        if (forced != null) {
            return forced;
        }
        String raw = values.raw(this.id).strip();
        return switch (this.type) {
            case NUMBER -> {
                double[] bounds = this.bounds(values);
                double number = parse(raw.isEmpty() ? this.fallback : raw, bounds[0]);
                yield format(Math.max(bounds[0], Math.min(bounds[1], this.decimals ? number : Math.round(number))));
            }
            case CHOICE -> this.options.contains(raw) && !this.shut.contains(raw) ? raw : this.fallback;
            case MULTI -> {
                StringBuilder kept = new StringBuilder();
                for (String option : (values.has(this.id) ? raw : this.fallback).split(",")) {
                    if (this.options.contains(option)) {
                        kept.append(kept.isEmpty() ? "" : ",").append(option);
                    }
                }
                yield kept.toString();
            }
            default -> raw;
        };
    }

    public double[] bounds(Values values) {
        if (this.range != null) {
            int[] range = this.range.apply(values);
            return new double[] {Math.max(this.low, range[0]), Math.min(this.high, range[1])};
        }
        return new double[] {this.low, this.high};
    }

    public int entries(Values values) {
        return (int) parse(values.get(this.count), 0);
    }

    public static String entry(String list, int index, String part) {
        return list + "." + index + "." + part;
    }

    // Shown, needed and still empty: a list's entries need their names.
    boolean missing(Values values) {
        if (!this.shown(values) || !this.required(values)) {
            return false;
        }
        if (this.type == Type.LIST) {
            for (int i = 0; i < this.entries(values); i++) {
                if (values.raw(entry(this.id, i, "name")).isBlank()) {
                    return true;
                }
            }
            return false;
        }
        return this.value(values).isBlank();
    }

    // The answer in words, as the report and its preview show it.
    String display(Values values) {
        String value = this.value(values);
        return switch (this.type) {
            case CHOICE -> value.isEmpty() ? "" : this.option(value).getString();
            case MULTI -> {
                StringBuilder text = new StringBuilder();
                for (String option : value.isEmpty() ? new String[0] : value.split(",")) {
                    text.append(text.isEmpty() ? "" : ", ").append(this.option(option).getString());
                }
                yield text.toString();
            }
            case NUMBER -> amount(this.unit, parse(value, 0)).getString();
            default -> value.strip();
        };
    }

    public static Component amount(Unit unit, double number) {
        String n = format(number);
        return switch (unit) {
            case COUNT -> Component.literal(n);
            case HEALTH -> Form.words("unit.health", n, format(number / 2.0));
            case BLOCKS -> Form.words(number == 1 ? "unit.block" : "unit.blocks", n);
            case PER_SECOND -> Form.words("unit.per_second", n, format(number / 2.0));
        };
    }

    public Unit unit() {
        return this.unit;
    }

    public boolean hasDecimals() {
        return this.decimals;
    }

    public MutableComponent label() {
        return Form.words("field." + this.id);
    }

    public MutableComponent tip() {
        return Form.words("field." + this.id + ".tip");
    }

    @Nullable
    public Component hint() {
        String key = Form.KEY + "field." + this.id + ".hint";
        return I18n.exists(key) ? Component.translatable(key) : null;
    }

    public MutableComponent option(String option) {
        return Form.words("field." + this.id + ".o." + option);
    }

    @Nullable
    public Component optionTip(String option) {
        String key = Form.KEY + "field." + this.id + ".o." + option + ".tip";
        return I18n.exists(key) ? Component.translatable(key) : null;
    }

    public Component shutWhy() {
        return Form.words("field." + this.id + ".locked");
    }

    public static double parse(String text, double fallback) {
        try {
            double number = Double.parseDouble(text.strip());
            return Double.isFinite(number) ? number : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static String format(double number) {
        if (number == Math.rint(number)) {
            return Long.toString((long) number);
        }
        return String.format(Locale.ROOT, "%.2f", number).replaceAll("0+$", "");
    }
}
