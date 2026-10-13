package nl.tivek.multiversepowers.bugreport.client.form;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;

// How a form's own controls look: option chips, section headings, small marks, the scrollbar and the tooltips.
final class FormDraw {
    static final int CHIP = 14;
    static final int LINE = 16;
    static final int GAP = 3;
    static final int TIP_WIDTH = 220;
    static final int REQUIRED = 0xFF7B7B;
    static final int LOCKED = 0xF2C84B;

    private FormDraw() {
    }

    // Each option's place at `x` within `width`, a row of `LINE` per line: a few short ones share the row evenly.
    static List<int[]> chips(Font font, Field field, int x, int width) {
        List<int[]> places = new ArrayList<>();
        int count = field.options().size();
        int widest = 0;
        for (String option : field.options()) {
            widest = Math.max(widest, chipWidth(font, field, option));
        }
        int even = count == 0 ? 0 : (width - GAP * (count - 1)) / count;
        if (count <= 4 && widest <= even) {
            for (int i = 0; i < count; i++) {
                int left = x + i * (even + GAP);
                places.add(new int[] {left, 0, i == count - 1 ? x + width - left : even});
            }
            return places;
        }
        int left = x;
        int line = 0;
        for (String option : field.options()) {
            int own = Math.min(width, chipWidth(font, field, option));
            if (left > x && left + own > x + width) {
                left = x;
                line++;
            }
            places.add(new int[] {left, line, own});
            left += own + GAP;
        }
        return places;
    }

    private static int chipWidth(Font font, Field field, String option) {
        return font.width(field.option(option)) + 12 + (field.isShut(option) ? 8 : 0);
    }

    static int lines(List<int[]> chips) {
        int lines = 0;
        for (int[] chip : chips) {
            lines = Math.max(lines, chip[1] + 1);
        }
        return lines;
    }

    // `state`: 0 plain, 1 under the pointer, 2 picked; `color` the picked fill (0 for the accent).
    static void chip(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int width, int state,
            int color, boolean dim, boolean lock) {
        int fill = state == 2 ? (dim ? 0xFF5A5A5A : 0xFF000000 | (color == 0 ? NavScreen.ACCENT : color))
                : state == 1 ? 0xFF3C3C3C : 0xFF262626;
        graphics.fill(x, y + 1, x + width, y + CHIP - 1, 0xFF000000);
        graphics.fill(x + 1, y, x + width - 1, y + CHIP, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + CHIP - 1, fill);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, state == 2 ? 0x60FFFFFF : 0x18FFFFFF);
        int textColor = state == 2 ? 0xFF101010 : dim ? 0xFF6A6A6A : color != 0 ? 0xFF000000 | color : 0xFFD2D2D2;
        int left = x + 6;
        if (lock) {
            lock(graphics, left, y + 4, 0xFF8A8A8A);
            left += 8;
        }
        graphics.drawString(font, text, left, y + 3, textColor, false);
    }

    // A small padlock, 5 wide and 7 high.
    static void lock(GuiGraphics graphics, int x, int y, int argb) {
        graphics.fill(x + 1, y, x + 4, y + 1, argb);
        graphics.fill(x + 1, y, x + 2, y + 3, argb);
        graphics.fill(x + 3, y, x + 4, y + 3, argb);
        graphics.fill(x, y + 3, x + 5, y + 7, argb);
    }

    // A small triangle pointing right (shut) or down (open).
    static void arrow(GuiGraphics graphics, int x, int y, boolean open, int argb) {
        for (int i = 0; i < 4; i++) {
            if (open) {
                graphics.fill(x + i, y + 1 + i, x + 7 - i, y + 2 + i, argb);
            } else {
                graphics.fill(x + 1 + i, y + i, x + 2 + i, y + 7 - i, argb);
            }
        }
    }

    static void scrollbar(GuiGraphics graphics, int x, int y, int height, double scroll, int total) {
        if (total <= height) {
            return;
        }
        graphics.fill(x, y, x + 3, y + height, 0x40000000);
        int thumb = Math.max(14, height * height / total);
        int top = y + (int) Math.round((height - thumb) * scroll / (total - height));
        graphics.fill(x, top, x + 3, top + thumb, 0xFF6A6A6A);
        graphics.fill(x, top, x + 2, top + thumb - 1, 0xFF9A9A9A);
    }

    // A tooltip: a bold name, then each part wrapped in its own colour.
    static List<FormattedCharSequence> tip(Font font, Component name, List<Component> parts) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(name.copy().withStyle(ChatFormatting.BOLD).getVisualOrderText());
        for (Component part : parts) {
            lines.addAll(font.split(part, TIP_WIDTH));
        }
        return lines;
    }

    // A field's name, marked * while it must be filled or locked while the other answers decide it.
    static Component label(Field field, Values values) {
        Component label = field.label();
        if (field.forced(values) != null) {
            return Component.empty().append(label).append(Component.literal(" "))
                    .append(Form.words("form.locked_mark").withColor(LOCKED));
        }
        return field.required(values) ? Component.empty().append(label)
                .append(Component.literal(" *").withColor(REQUIRED)) : label;
    }

    // A field's tooltip under its name: what it asks, whether it must be filled, and its limits.
    static List<Component> parts(Field field, Values values) {
        List<Component> parts = new ArrayList<>();
        parts.add(field.tip().copy().withColor(0xC8C8C8));
        if (field.forced(values) != null) {
            parts.add(Form.words("form.locked", field.why()).withColor(LOCKED));
        } else {
            parts.add(field.required(values) ? Form.words("form.required").withColor(REQUIRED)
                    : Form.words("form.optional.tip").withColor(0x8E8E8E));
        }
        switch (field.type()) {
            case TEXT, AREA -> parts.add(Form.words("form.max", field.max()).withColor(0x8E8E8E));
            case NUMBER -> {
                double[] bounds = field.bounds(values);
                parts.add(Form.words("form.range", Field.amount(field.unit(), bounds[0]),
                        Field.amount(field.unit(), bounds[1])).withColor(0x8E8E8E));
                parts.add(Form.words("form.plusminus").withColor(0x8E8E8E));
            }
            case MULTI -> parts.add(Form.words("form.multi").withColor(0x8E8E8E));
            case LIST -> parts.add(Form.words("form.list", field.nameMax(), field.max()).withColor(0x8E8E8E));
            case MOBS -> {
                parts.add(Form.words("form.mobs").withColor(0x8E8E8E));
                String shown = field.display(values);
                if (!shown.isEmpty()) {
                    parts.add(Component.literal(shown).withColor(0xC8C8C8));
                }
            }
            default -> {
            }
        }
        return parts;
    }

    // "12 / 300", yellow near the limit and red at it, its right edge at `right`.
    static void counter(GuiGraphics graphics, Font font, int length, int max, int right, int y) {
        Component count = Component.literal(length + " / " + max);
        int color = length >= max ? 0xFFFF7B7B : length > max * 9 / 10 ? 0xFFF2C84B : 0xFF5E5E5E;
        graphics.drawString(font, count, right - font.width(count), y, color, false);
    }

    // `text` cut to `width` with "..." at its end.
    static FormattedCharSequence fit(Font font, Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        FormattedText cut = font.substrByWidth(text, Math.max(0, width - font.width("...")));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of("...",
                text.getStyle())));
    }
}
