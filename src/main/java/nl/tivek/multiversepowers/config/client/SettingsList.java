package nl.tivek.multiversepowers.config.client;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.Unit;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;

// The settings window's list: each part under a title in its page's colour (click it to fold it shut), and a row for
// every setting: a number with its steps and what it means in that colour, or one button for a switch or a choice.
final class SettingsList extends ContainerObjectSelectionList<SettingsList.Row> {
    static final int ROW_HEIGHT = 20;
    private static final String PREFIX = "config." + MultiversePowers.MODID + ".";
    private static final Pattern NUMBER = Pattern.compile("-?[0-9]*\\.?[0-9]*");
    private static final int TEXT = 0xFFFFFF;
    private static final int CHANGED = NavScreen.WARN;
    private static final int ON = 0x7CF29C;
    private static final int OFF = 0xA0A0A0;
    private static final int WRONG = 0xFF6464;
    private static final int GROUP = 0x9C9C9C;
    private static final int LINE = 0x1CFFFFFF;
    private static final int HOVER = 0x14FFFFFF;

    record Block(String key, Component title, @Nullable Component hint, @Nullable Component about, int color,
            boolean collapsible, boolean collapsed, List<SettingsPages.Group> groups) {
        int count() {
            int count = 0;
            for (SettingsPages.Group group : this.groups) {
                count += group.numbers().size();
            }
            return count;
        }
    }

    private final SettingsScreen screen;
    private final List<ConfigNumber> numbers = new ArrayList<>();

    SettingsList(Minecraft minecraft, SettingsScreen screen, List<Block> blocks, int x, int y, int width,
            int height) {
        super(minecraft, width, height, y, ROW_HEIGHT);
        this.screen = screen;
        this.setX(x);
        for (Block block : blocks) {
            this.addEntry(new TitleRow(block));
            if (block.collapsed()) {
                continue;
            }
            for (SettingsPages.Group group : block.groups()) {
                if (group.title() != null) {
                    this.addEntry(new GroupRow(group.title(), GuiShapes.mix(block.color(), GROUP, 0.5F)));
                }
                for (ConfigNumber number : group.numbers()) {
                    this.numbers.add(number);
                    this.addEntry(new NumberRow(number, block.color()));
                }
            }
        }
    }

    List<ConfigNumber> numbers() {
        return this.numbers;
    }

    @Override
    public int getRowWidth() {
        return this.width - 18;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.width - 6;
    }

    @Override
    protected void renderListBackground(GuiGraphics graphics) {
        NavScreen.card(graphics, this.getX(), this.getY(), this.width, this.height, 0);
    }

    @Override
    protected void renderListSeparators(GuiGraphics graphics) {
    }

    private static FormattedCharSequence fit(Font font, Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        String cut = font.plainSubstrByWidth(text.getString(), Math.max(0, width - font.width("...")));
        return Component.literal(cut + "...").withStyle(text.getStyle()).getVisualOrderText();
    }

    abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
    }

    private final class TitleRow extends Row {
        private final Block block;
        private final Component title;
        private final Component hint;

        TitleRow(Block block) {
            this.block = block;
            String arrow = !block.collapsible() ? "" : block.collapsed() ? "▶ " : "▼ ";
            this.title = Component.literal(arrow).append(block.title().copy().withStyle(ChatFormatting.BOLD))
                    .append(Component.literal("  (" + block.count() + ")").withStyle(ChatFormatting.GRAY));
            this.hint = block.hint() == null ? Component.empty() : Component.translatable(PREFIX + "key",
                    block.hint());
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX,
                int mouseY, boolean hovering, float partialTick) {
            Font font = SettingsList.this.minecraft.font;
            if (hovering && this.block.collapsible()) {
                graphics.fill(left - 2, top, left + width + 2, top + height, HOVER);
            }
            graphics.fill(left - 2, top + 4, left, top + height - 4, 0xFF000000 | this.block.color());
            int y = top + (height - 8) / 2;
            int hintWidth = font.width(this.hint);
            graphics.drawString(font, fit(font, this.title, width - hintWidth - 16), left + 4, y,
                    0xFF000000 | this.block.color());
            graphics.drawString(font, this.hint, left + width - hintWidth - 4, y, 0xFF8E8E8E);
            graphics.fill(left, top + height - 1, left + width, top + height, LINE);
            if (hovering && this.block.about() != null) {
                List<FormattedCharSequence> lines = new ArrayList<>();
                lines.add(this.block.title().copy().withStyle(ChatFormatting.WHITE).getVisualOrderText());
                lines.addAll(font.split(this.block.about().copy().withStyle(ChatFormatting.GRAY), 220));
                SettingsList.this.screen.setTooltipForNextRenderPass(lines);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.block.collapsible() || button != 0) {
                return false;
            }
            SettingsList.this.screen.toggle(this.block.key());
            return true;
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

    private final class GroupRow extends Row {
        private final Component title;
        private final int color;

        GroupRow(Component title, int color) {
            this.title = title;
            this.color = color;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX,
                int mouseY, boolean hovering, float partialTick) {
            Font font = SettingsList.this.minecraft.font;
            int y = top + height - 9;
            FormattedCharSequence text = fit(font, this.title, width - 12);
            graphics.drawString(font, text, left + 6, y, 0xFF000000 | this.color);
            int end = left + 10 + font.width(text);
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

    private final class NumberRow extends Row {
        private final ConfigNumber number;
        // What a number means, in its page's colour.
        private final int color;
        // A switch or a choice: one button that goes on to the next value.
        private final boolean pick;
        private final Button minus;
        private final Button plus;
        private final Button next;
        private final Button reset;
        private final EditBox box;
        private double value;
        private boolean valid = true;

        NumberRow(ConfigNumber number, int color) {
            this.number = number;
            this.color = color;
            this.pick = number.unit() == Unit.SWITCH || number.choices() != null;
            this.value = SettingsList.this.screen.value(number);
            Font font = SettingsList.this.minecraft.font;
            Component steps = Component.translatable(PREFIX + "steps");
            this.minus = Button.builder(Component.literal("-"), button -> this.nudge(-1)).size(14, 16)
                    .tooltip(Tooltip.create(steps)).build();
            this.plus = Button.builder(Component.literal("+"), button -> this.nudge(1)).size(14, 16)
                    .tooltip(Tooltip.create(steps)).build();
            this.reset = Button.builder(Component.literal("↺"), button -> this.set(number.defaultValue()))
                    .size(14, 16)
                    .tooltip(Tooltip.create(Component.translatable(PREFIX + "reset",
                            number.format(number.defaultValue()), number.meaning(number.defaultValue()))))
                    .build();
            this.box = new EditBox(font, 0, 0, 44, 16, number.label());
            this.box.setMaxLength(12);
            this.box.setFilter(text -> NUMBER.matcher(text).matches());
            this.box.setValue(number.format(this.value));
            this.box.setResponder(this::typed);
            this.next = Button.builder(this.shown(), button -> this.set(this.following())).size(76, 16).build();
            boolean editable = SettingsList.this.screen.editable(number);
            this.minus.active = editable;
            this.plus.active = editable;
            this.next.active = editable;
            this.box.setEditable(editable);
        }

        // A switch flips; a choice goes on to the next, after the last back to the first.
        private double following() {
            double next = this.value + 1.0;
            return next > this.number.max() + 1.0E-9 ? this.number.min() : next;
        }

        // What the button of a switch or a choice says: on in green, off in grey, a choice by its name.
        private Component shown() {
            Component meaning = this.number.meaning(this.value);
            return this.number.unit() != Unit.SWITCH ? meaning
                    : meaning.copy().withColor(this.value >= 0.5 ? ON : OFF);
        }

        private void typed(String text) {
            try {
                double typed = Double.parseDouble(text);
                this.valid = typed >= this.number.min() - 1.0E-9 && typed <= this.number.max() + 1.0E-9;
                if (this.valid) {
                    this.value = this.number.clamp(typed);
                    SettingsList.this.screen.set(this.number, this.value);
                }
            } catch (NumberFormatException wrong) {
                this.valid = false;
            }
            this.box.setTextColor(this.valid ? 0xE0E0E0 : WRONG);
        }

        void set(double value) {
            this.value = this.number.clamp(value);
            this.box.setValue(this.number.format(this.value));
            this.next.setMessage(this.shown());
            SettingsList.this.screen.set(this.number, this.value);
        }

        private void nudge(int way) {
            int steps = Screen.hasShiftDown() ? 10 : 1;
            double next = Math.round((this.value + way * steps * this.number.step()) * 1000.0) / 1000.0;
            this.set(next);
        }

        private boolean changed() {
            return Math.abs(this.value - this.number.defaultValue()) > 1.0E-9;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX,
                int mouseY, boolean hovering, float partialTick) {
            Font font = SettingsList.this.minecraft.font;
            if (hovering) {
                graphics.fill(left - 2, top, left + width + 2, top + height, HOVER);
            }
            if (SettingsList.this.screen.waiting(this.number)) {
                graphics.fill(left - 2, top + 4, left, top + height - 4, 0xFF000000 | CHANGED);
            }
            int labelWidth = Math.max(80, (int) (width * 0.42));
            int x = left + labelWidth + 4;
            int textY = top + (height - 8) / 2;
            this.reset.visible = this.changed() && SettingsList.this.screen.editable(this.number);
            this.reset.setPosition(x + 78, top + 2);
            if (this.pick) {
                this.next.setPosition(x, top + 2);
                this.next.render(graphics, mouseX, mouseY, partialTick);
            } else {
                this.minus.setPosition(x, top + 2);
                this.box.setPosition(x + 16, top + 2);
                this.plus.setPosition(x + 62, top + 2);
                this.minus.render(graphics, mouseX, mouseY, partialTick);
                this.box.render(graphics, mouseX, mouseY, partialTick);
                this.plus.render(graphics, mouseX, mouseY, partialTick);
                int meaningX = x + 96;
                Component meaning = this.valid ? this.number.meaning(this.value)
                        : Component.translatable(PREFIX + "range", this.number.format(this.number.min()),
                                this.number.format(this.number.max()));
                graphics.drawString(font, fit(font, meaning, left + width - meaningX), meaningX, textY,
                        0xFF000000 | (this.valid ? this.color : WRONG));
            }
            this.reset.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawString(font, fit(font, this.number.label(), labelWidth - 6), left + 4, textY,
                    0xFF000000 | (this.changed() ? CHANGED : TEXT));

            if (mouseX >= left && mouseX < left + labelWidth && mouseY >= top && mouseY < top + height) {
                SettingsList.this.screen.setTooltipForNextRenderPass(this.tooltip(font));
            }
        }

        private List<FormattedCharSequence> tooltip(Font font) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(this.number.label().copy().withStyle(ChatFormatting.WHITE).getVisualOrderText());
            if (!this.number.description().getString().isEmpty()) {
                lines.addAll(font.split(this.number.description().copy().withStyle(ChatFormatting.GRAY), 220));
            }
            double standard = this.number.defaultValue();
            if (this.pick) {
                lines.add(Component.translatable(PREFIX + "default.plain", this.number.meaning(standard))
                        .withStyle(ChatFormatting.GREEN).getVisualOrderText());
                return lines;
            }
            lines.add(Component.translatable(PREFIX + "default", this.number.format(standard),
                    this.number.meaning(standard)).withStyle(ChatFormatting.GREEN).getVisualOrderText());
            lines.add(Component.translatable(PREFIX + "range", this.number.format(this.number.min()),
                    this.number.format(this.number.max())).withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            return lines;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.pick ? List.<GuiEventListener>of(this.next, this.reset)
                    : List.<GuiEventListener>of(this.minus, this.box, this.plus, this.reset);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.pick ? List.<NarratableEntry>of(this.next, this.reset)
                    : List.<NarratableEntry>of(this.minus, this.box, this.plus, this.reset);
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
