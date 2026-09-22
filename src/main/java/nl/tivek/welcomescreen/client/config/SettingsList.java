package nl.tivek.welcomescreen.client.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * The scrolling list of a settings page: a title for every ability, a smaller title for every part of it (the
 * beam, the dome), and a row for every number. A row has the number's name, - and + around a box you can type
 * in, a button that puts it back to the mod's own number once you changed it, and what the number means in plain
 * words. Point at a name to read what it does.
 */
final class SettingsList extends ContainerObjectSelectionList<SettingsList.Row> {
    static final int ROW_HEIGHT = 20;
    private static final String PREFIX = "config." + WelcomeScreenMod.MODID + ".";
    // What may be typed in a box: a number, possibly negative, with a point.
    private static final Pattern NUMBER = Pattern.compile("-?[0-9]*\\.?[0-9]*");
    private static final int TEXT = 0xFFFFFF;
    private static final int CHANGED = 0xF2C84B;
    private static final int MEANING = 0x7CF29C;
    private static final int WRONG = 0xFF6464;
    private static final int GROUP = 0x9CC8A8;
    private static final int LINE = 0x40FFFFFF;

    private final SettingsScreen screen;
    private final int titleColor;
    private final List<NumberRow> numbers = new ArrayList<>();
    // The row every section starts at, to scroll straight to it.
    private final List<Integer> starts = new ArrayList<>();

    SettingsList(Minecraft minecraft, SettingsScreen screen, SettingsPages.Page page, int x, int y, int width,
            int height) {
        super(minecraft, width, height, y, ROW_HEIGHT);
        this.screen = screen;
        this.titleColor = page.color();
        this.setX(x);
        // A page with one part needs no title for it: the page's own title says it.
        boolean titled = page.sections().size() > 1;
        for (SettingsPages.Section section : page.sections()) {
            this.starts.add(this.children().size());
            if (titled) {
                this.addEntry(new TitleRow(section));
            }
            for (SettingsPages.Group group : section.groups()) {
                if (group.title() != null) {
                    this.addEntry(new GroupRow(group.title()));
                }
                for (ConfigNumber number : group.numbers()) {
                    NumberRow row = new NumberRow(number);
                    this.numbers.add(row);
                    this.addEntry(row);
                }
            }
        }
    }

    /** Scrolls so that this section's title is at the top. */
    void scrollTo(int section) {
        this.setClampedScrollAmount(this.starts.get(section) * (double) ROW_HEIGHT);
    }

    /** The section whose rows are at the top of the list right now. */
    int shownSection() {
        int top = (int) (this.getScrollAmount() / ROW_HEIGHT + 0.5);
        int shown = 0;
        for (int i = 0; i < this.starts.size(); i++) {
            if (this.starts.get(i) <= top) {
                shown = i;
            }
        }
        return shown;
    }

    /** Every number back to what the mod itself has; nothing is saved yet. */
    void defaults() {
        for (NumberRow row : this.numbers) {
            row.set(row.number.defaultValue());
        }
    }

    /** Puts every number that differs from the file into it. */
    void store() {
        for (NumberRow row : this.numbers) {
            if (Math.abs(row.value - row.number.stored().getAsDouble()) > 1.0E-9) {
                row.number.store().accept(row.value);
            }
        }
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.width - 7;
    }

    @Override
    protected void renderListBackground(GuiGraphics graphics) {
        graphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), 0x50000000);
    }

    @Override
    protected void renderListSeparators(GuiGraphics graphics) {
        graphics.fill(this.getX(), this.getY() - 1, this.getRight(), this.getY(), LINE);
        graphics.fill(this.getX(), this.getBottom(), this.getRight(), this.getBottom() + 1, LINE);
    }

    /** {@code text} cut down to {@code width} pixels, with dots where it was cut. */
    private static FormattedCharSequence fit(Font font, Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        String cut = font.plainSubstrByWidth(text.getString(), Math.max(0, width - font.width("...")));
        return Component.literal(cut + "...").withStyle(text.getStyle()).getVisualOrderText();
    }

    abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
    }

    /** The title of an ability, with the key it sits on at the right. */
    private final class TitleRow extends Row {
        private final Component title;
        private final Component hint;

        TitleRow(SettingsPages.Section section) {
            this.title = section.title().copy().withStyle(ChatFormatting.BOLD);
            this.hint = section.hint() == null ? Component.empty()
                    : Component.translatable(PREFIX + "key", section.hint());
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX,
                int mouseY, boolean hovering, float partialTick) {
            Font font = SettingsList.this.minecraft.font;
            int y = top + height - 9;
            int hintWidth = font.width(this.hint);
            graphics.drawString(font, fit(font, this.title, width - hintWidth - 8), left, y,
                    0xFF000000 | SettingsList.this.titleColor);
            graphics.drawString(font, this.hint, left + width - hintWidth, y, 0xFFA8A090);
            graphics.fill(left, top + height + 1, left + width, top + height + 2, 0x80000000 | (SettingsList.this.titleColor & 0xFFFFFF));
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(narration(this.title));
        }
    }

    /** The title of one part of an ability, like "Light Beam (hold 2 s)", with a thin line after it. */
    private final class GroupRow extends Row {
        private final Component title;

        GroupRow(Component title) {
            this.title = title;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX,
                int mouseY, boolean hovering, float partialTick) {
            Font font = SettingsList.this.minecraft.font;
            int y = top + height - 9;
            FormattedCharSequence text = fit(font, this.title, width - 12);
            graphics.drawString(font, text, left + 4, y, 0xFF000000 | GROUP);
            int end = left + 8 + font.width(text);
            if (end < left + width) {
                graphics.fill(end, y + 4, left + width, y + 5, LINE);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(narration(this.title));
        }
    }

    /** One number: its name, - [box] +, the button to put it back, and what it means. */
    private final class NumberRow extends Row {
        private final ConfigNumber number;
        private final Button minus;
        private final Button plus;
        private final Button reset;
        private final EditBox box;
        private double value;
        private boolean valid = true;

        NumberRow(ConfigNumber number) {
            this.number = number;
            this.value = number.clamp(number.stored().getAsDouble());
            Font font = SettingsList.this.minecraft.font;
            this.minus = Button.builder(Component.literal("-"), button -> this.nudge(-1)).size(14, 16).build();
            this.plus = Button.builder(Component.literal("+"), button -> this.nudge(1)).size(14, 16).build();
            this.reset = Button.builder(Component.literal("↺"), button -> this.set(number.defaultValue()))
                    .size(14, 16)
                    .tooltip(Tooltip.create(Component.translatable(PREFIX + "reset",
                            number.format(number.defaultValue()), number.unit().describe(number.defaultValue()))))
                    .build();
            this.box = new EditBox(font, 0, 0, 44, 16, number.label());
            this.box.setMaxLength(12);
            this.box.setFilter(text -> NUMBER.matcher(text).matches());
            this.box.setValue(number.format(this.value));
            this.box.setResponder(this::typed);
        }

        private void typed(String text) {
            try {
                double typed = Double.parseDouble(text);
                this.valid = typed >= this.number.min() - 1.0E-9 && typed <= this.number.max() + 1.0E-9;
                if (this.valid) {
                    this.value = this.number.clamp(typed);
                }
            } catch (NumberFormatException wrong) {
                this.valid = false;
            }
            this.box.setTextColor(this.valid ? 0xE0E0E0 : WRONG);
        }

        void set(double value) {
            this.value = this.number.clamp(value);
            this.box.setValue(this.number.format(this.value));
        }

        private void nudge(int way) {
            double next = Math.round((this.value + way * this.number.step()) * 1000.0) / 1000.0;
            this.set(next);
        }

        private boolean changed() {
            return Math.abs(this.value - this.number.defaultValue()) > 1.0E-9;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX,
                int mouseY, boolean hovering, float partialTick) {
            Font font = SettingsList.this.minecraft.font;
            int labelWidth = Math.max(80, (int) (width * 0.42));
            int x = left + labelWidth + 4;
            this.minus.setPosition(x, top);
            this.box.setPosition(x + 16, top);
            this.plus.setPosition(x + 62, top);
            this.reset.setPosition(x + 78, top);
            this.reset.visible = this.changed();
            this.minus.render(graphics, mouseX, mouseY, partialTick);
            this.box.render(graphics, mouseX, mouseY, partialTick);
            this.plus.render(graphics, mouseX, mouseY, partialTick);
            this.reset.render(graphics, mouseX, mouseY, partialTick);

            int textY = top + (height - 8) / 2;
            graphics.drawString(font, fit(font, this.number.label(), labelWidth - 4), left, textY,
                    0xFF000000 | (this.changed() ? CHANGED : TEXT));
            int meaningX = x + 96;
            Component meaning = this.valid ? this.number.unit().describe(this.value)
                    : Component.translatable(PREFIX + "range", this.number.format(this.number.min()),
                            this.number.format(this.number.max()));
            graphics.drawString(font, fit(font, meaning, left + width - meaningX), meaningX, textY,
                    0xFF000000 | (this.valid ? MEANING : WRONG));

            if (mouseX >= left && mouseX < left + labelWidth && mouseY >= top && mouseY < top + height) {
                SettingsList.this.screen.setTooltipForNextRenderPass(this.tooltip(font));
            }
        }

        /** What the number does, what the mod itself has, and how far it may go. */
        private List<FormattedCharSequence> tooltip(Font font) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(this.number.label().copy().withStyle(ChatFormatting.WHITE).getVisualOrderText());
            if (!this.number.description().getString().isEmpty()) {
                lines.addAll(font.split(this.number.description().copy().withStyle(ChatFormatting.GRAY), 220));
            }
            double standard = this.number.defaultValue();
            lines.add(Component.translatable(PREFIX + "default", this.number.format(standard),
                    this.number.unit().describe(standard)).withStyle(ChatFormatting.GREEN).getVisualOrderText());
            lines.add(Component.translatable(PREFIX + "range", this.number.format(this.number.min()),
                    this.number.format(this.number.max())).withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            return lines;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.minus, this.box, this.plus, this.reset);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.minus, this.box, this.plus, this.reset);
        }
    }

    private static NarratableEntry narration(Component title) {
        return new NarratableEntry() {
            @Override
            public NarratableEntry.NarrationPriority narrationPriority() {
                return NarratableEntry.NarrationPriority.HOVERED;
            }

            @Override
            public void updateNarration(NarrationElementOutput output) {
                output.add(NarratedElementType.TITLE, title);
            }
        };
    }
}
