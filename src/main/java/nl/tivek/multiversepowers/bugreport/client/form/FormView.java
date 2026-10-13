package nl.tivek.multiversepowers.bugreport.client.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

// A form drawn in a scrolling box: each section under a heading that folds it, each field a row with its name on the
// left, fields shown only while the answers so far call for them. Text goes in the game's own text boxes, kept in the
// box (hidden outside it); every row and option has a tooltip.
public final class FormView extends FormBoxes {
    private static final int LEGEND = 14;
    private static final int HEADER = 16;
    private static final int SECTION_GAP = 5;
    private static final int SCROLL_STEP = 18;
    private static final int COUNTER = 10;
    // Folded sections and how far each form was scrolled, while the game runs.
    private static final Map<String, Set<String>> FOLDED = new HashMap<>();
    private static final Map<String, Double> SCROLLED = new HashMap<>();

    public interface Host {
        void add(AbstractWidget widget);

        @Nullable
        GuiEventListener focused();

        void focus(@Nullable GuiEventListener widget);

        void tooltip(List<FormattedCharSequence> lines);

        void changed();
    }

    private enum Kind { LEGEND, HEADER, FIELD, ENTRY }

    private record Item(Kind kind, @Nullable Section section, @Nullable Field field, int index, int y, int height) {
    }

    private record Hit(int x, int y, int width, int height, Runnable action) {
    }

    private final Map<String, Integer> places = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private List<Item> items = List.of();
    private int total;
    private double scroll;
    @Nullable
    private GuiEventListener followed;
    @Nullable
    private List<FormattedCharSequence> tip;

    public FormView(Form form, Map<String, String> raw, Font font, Host host, int x, int y, int width, int height) {
        super(form, raw, font, host, x, y, width, height);
        this.scroll = SCROLLED.getOrDefault(form.id(), 0.0);
    }

    // A form's folded sections: at first its optional ones.
    private static Set<String> folded(Form form) {
        return FOLDED.computeIfAbsent(form.id(), id -> {
            Set<String> folded = new HashSet<>();
            form.sections().stream().filter(Section::optional).forEach(section -> folded.add(section.id()));
            return folded;
        });
    }

    public static void fold(Form form, String section, boolean folded) {
        if (folded) {
            folded(form).add(section);
        } else {
            folded(form).remove(section);
        }
    }

    private boolean folded(Section section) {
        return folded(this.form).contains(section.id());
    }

    public boolean allOpen() {
        return this.form.sections().stream().noneMatch(this::folded);
    }

    public void foldAll(boolean folded) {
        this.form.sections().forEach(section -> fold(this.form, section.id(), folded));
    }

    private void layout() {
        List<Item> list = new ArrayList<>();
        this.places.clear();
        list.add(new Item(Kind.LEGEND, null, null, 0, 0, LEGEND));
        int at = LEGEND;
        boolean first = true;
        for (Section section : this.form.sections()) {
            if (!section.shown(this.values)) {
                continue;
            }
            at += first ? 0 : SECTION_GAP;
            first = false;
            list.add(new Item(Kind.HEADER, section, null, 0, at, HEADER));
            at += HEADER + 3;
            if (this.folded(section)) {
                continue;
            }
            for (Field field : section.fields()) {
                if (!field.shown(this.values)) {
                    continue;
                }
                int own = this.rowHeight(field);
                list.add(new Item(Kind.FIELD, section, field, 0, at, own));
                this.places.put(field.id(), at);
                at += own;
                if (field.type() == Field.Type.LIST) {
                    int entry = ROW + areaHeight(3) + COUNTER + 4;
                    for (int i = 0; i < field.entries(this.values); i++) {
                        list.add(new Item(Kind.ENTRY, section, field, i, at, entry));
                        this.places.put(Field.entry(field.id(), i, "name"), at);
                        this.places.put(Field.entry(field.id(), i, "desc"), at + ROW);
                        at += entry;
                    }
                }
            }
            at += 2;
        }
        this.items = list;
        this.total = at;
    }

    private int rowHeight(Field field) {
        return switch (field.type()) {
            case CHOICE, MULTI -> Math.max(ROW,
                    FormDraw.lines(FormDraw.chips(this.font, field, this.controlX(), this.controlWidth()))
                            * FormDraw.LINE + 4);
            case AREA -> 11 + areaHeight(field.lines()) + 5;
            default -> ROW;
        };
    }

    private double maxScroll() {
        return Math.max(0, this.total - this.height);
    }

    public boolean inside(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
    }

    // Takes every text box out of reach until the next frame: a click outside the form must not land in one.
    public void hideWidgets() {
        this.boxes.values().forEach(box -> box.visible = false);
        this.areas.values().forEach(area -> area.visible = false);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.hits.clear();
        this.tip = null;
        this.layout();
        this.follow();
        this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll));
        SCROLLED.put(this.form.id(), this.scroll);
        boolean inside = this.inside(mouseX, mouseY);
        int mx = inside ? mouseX : -10000;
        int my = inside ? mouseY : -10000;
        this.hideWidgets();
        this.sync();
        graphics.enableScissor(this.x, this.y, this.x + this.width, this.y + this.height);
        for (Item item : this.items) {
            int top = this.y + item.y() - (int) this.scroll;
            if (top + item.height() < this.y || top > this.y + this.height) {
                continue;
            }
            switch (item.kind()) {
                case LEGEND -> graphics.drawString(this.font, Form.words("form.legend"), this.x, top + 2, 0xFF8E8E8E,
                        false);
                case HEADER -> this.header(graphics, item.section(), top, mx, my);
                case FIELD -> this.field(graphics, item.field(), top, item.height(), mx, my);
                case ENTRY -> this.entry(graphics, item.field(), item.index(), top, mx, my);
            }
        }
        for (EditBox box : this.boxes.values()) {
            if (box.visible) {
                box.render(graphics, mx, my, partialTick);
            }
        }
        for (MultiLineEditBox area : this.areas.values()) {
            if (area.visible) {
                area.render(graphics, mx, my, partialTick);
            }
        }
        graphics.disableScissor();
        FormDraw.scrollbar(graphics, this.x + this.width - 3, this.y, this.height, this.scroll, this.total);
        if (this.tip != null) {
            this.host.tooltip(this.tip);
        }
    }

    // Scrolls a box that just took the keys (Tab) into view.
    private void follow() {
        GuiEventListener focused = this.host.focused();
        if (focused == this.followed) {
            return;
        }
        this.followed = focused;
        for (Map.Entry<String, EditBox> entry : this.boxes.entrySet()) {
            if (entry.getValue() == focused) {
                this.show(entry.getKey(), ROW);
            }
        }
        for (Map.Entry<String, MultiLineEditBox> entry : this.areas.entrySet()) {
            if (entry.getValue() == focused) {
                this.show(entry.getKey(), entry.getValue().getHeight() + 12);
            }
        }
    }

    private void show(String key, int room) {
        Integer at = this.places.get(key);
        if (at == null) {
            return;
        }
        if (at < this.scroll) {
            this.scroll = Math.max(0, at - 4);
        } else if (at + room > this.scroll + this.height) {
            this.scroll = at + room - this.height + 4;
        }
    }

    private void header(GuiGraphics graphics, Section section, int top, int mouseX, int mouseY) {
        int right = this.x + this.width - BAR;
        boolean over = mouseX >= this.x && mouseX < right && mouseY >= top && mouseY < top + HEADER;
        boolean open = !this.folded(section);
        graphics.fill(this.x, top, right, top + HEADER, over ? 0x40FFFFFF : 0x24FFFFFF);
        graphics.fill(this.x, top + HEADER - 1, right, top + HEADER, 0x30000000);
        FormDraw.arrow(graphics, this.x + 4, top + 5, open, 0xFFD2D2D2);
        int missing = section.missing(this.values);
        Component status = missing > 0 ? Form.words("form.missing", missing).withColor(FormDraw.REQUIRED)
                : section.optional() ? Form.words("form.optional").withColor(0x8E8E8E)
                : Form.words("form.done").withColor(0x6EE7A0);
        int statusWidth = this.font.width(status);
        graphics.drawString(this.font, status, right - 5 - statusWidth, top + 4, 0xFFFFFFFF, false);
        Component title = section.title();
        Component summary = this.summary(section);
        if (summary != null) {
            title = Component.empty().append(title).append(Component.literal(":  ").withColor(0x8E8E8E))
                    .append(summary.copy().withColor(0x6EE7A0));
        }
        int room = right - 5 - statusWidth - 8 - (this.x + 15);
        graphics.drawString(this.font, FormDraw.fit(this.font, title, room), this.x + 15, top + 4, 0xFFFFFFFF, true);
        this.hits.add(new Hit(this.x, top, right - this.x, HEADER, () -> fold(this.form, section.id(), open)));
        if (over) {
            List<Component> parts = new ArrayList<>();
            parts.add(section.tip().copy().withColor(0xC8C8C8));
            if (missing > 0) {
                parts.add(Form.words("form.missing.tip", missing).withColor(FormDraw.REQUIRED));
            }
            parts.add(Form.words(open ? "form.fold.tip" : "form.unfold.tip").withColor(0x8E8E8E));
            this.tip = FormDraw.tip(this.font, section.title(), parts);
        }
    }

    // A one-question section's answer, shown on its heading.
    @Nullable
    private Component summary(Section section) {
        if (section.fields().size() != 1 || section.fields().get(0).type() != Field.Type.CHOICE) {
            return null;
        }
        String shown = section.fields().get(0).display(this.values);
        return shown.isEmpty() ? null : Component.literal(shown);
    }

    private void field(GuiGraphics graphics, Field field, int top, int rowHeight, int mouseX, int mouseY) {
        boolean area = field.type() == Field.Type.AREA;
        int labelY = area ? top + 1 : top + 6;
        Component label = FormDraw.label(field, this.values);
        int labelRoom = area ? this.width - BAR - 60 : this.labelWidth - 8;
        graphics.drawString(this.font, FormDraw.fit(this.font, label, labelRoom), this.x, labelY, 0xFFD2D2D2, false);
        boolean overLabel = mouseX >= this.x && mouseX < this.x + labelRoom && mouseY >= labelY - 2
                && mouseY < labelY + 9;
        boolean overRow = mouseX >= this.x && mouseX < this.x + this.width - BAR && mouseY >= top
                && mouseY < top + rowHeight;
        if (overLabel || overRow && field.type() != Field.Type.CHOICE && field.type() != Field.Type.MULTI) {
            this.tip = FormDraw.tip(this.font, field.label(), FormDraw.parts(field, this.values));
        }
        int cx = this.controlX();
        switch (field.type()) {
            case TEXT -> this.place(this.boxes.get(field.id()), cx, top + 2);
            case NUMBER -> this.number(graphics, field, cx, top + 2, mouseX, mouseY);
            case CHOICE, MULTI -> this.options(graphics, field, cx, top + 2, mouseX, mouseY);
            case AREA -> {
                MultiLineEditBox box = this.areas.get(field.id());
                FormDraw.counter(graphics, this.font, box.getValue().length(), field.max(), this.x + this.width - BAR,
                        labelY);
                this.place(box, this.x, top + 11);
            }
            case LIST -> {
                Component note = Form.words("form.entries", field.entries(this.values),
                        this.form.field(field.count).label());
                graphics.drawString(this.font, FormDraw.fit(this.font, note, this.controlWidth()), cx, labelY,
                        0xFF8E8E8E, false);
            }
        }
    }

    private void place(AbstractWidget widget, int x, int y) {
        widget.setX(x);
        widget.setY(y);
        widget.visible = true;
    }

    private void number(GuiGraphics graphics, Field field, int cx, int top, int mouseX, int mouseY) {
        EditBox box = this.boxes.get(field.id());
        this.step(graphics, field, cx, top, -1, mouseX, mouseY);
        this.place(box, cx + 16, top);
        this.step(graphics, field, cx + 64, top, 1, mouseX, mouseY);
        String typed = box.getValue();
        String kept = this.values.get(field.id());
        double[] bounds = field.bounds(this.values);
        // A plain count would only repeat the box: its range shows instead.
        Component readout = field.unit() == Field.Unit.COUNT
                ? Form.words("form.between", Field.format(bounds[0]), Field.format(bounds[1]))
                : Field.amount(field.unit(), Field.parse(kept, 0));
        boolean clamped = !typed.isEmpty() && Field.parse(typed, Double.NaN) != Field.parse(kept, 0);
        if (clamped) {
            readout = Form.words("form.kept", readout);
        }
        graphics.drawString(this.font, FormDraw.fit(this.font, readout, this.controlWidth() - 84), cx + 84, top + 4,
                clamped ? 0xFFF2C84B : 0xFF8E8E8E, false);
    }

    private void step(GuiGraphics graphics, Field field, int left, int top, int sign, int mouseX, int mouseY) {
        boolean over = mouseX >= left && mouseX < left + 14 && mouseY >= top && mouseY < top + 16;
        graphics.fill(left, top, left + 14, top + 16, 0xFF000000);
        graphics.fill(left + 1, top + 1, left + 13, top + 15, over ? 0xFF4A4A4A : 0xFF2C2C2C);
        graphics.fill(left + 4, top + 7, left + 10, top + 9, 0xFFE0E0E0);
        if (sign > 0) {
            graphics.fill(left + 6, top + 5, left + 8, top + 11, 0xFFE0E0E0);
        }
        this.hits.add(new Hit(left, top, 14, 16, () -> {
            double stepSize = field.hasDecimals() ? 0.5 : 1.0;
            double step = stepSize * (Screen.hasShiftDown() ? 10 : 1);
            double next = Field.parse(this.values.get(field.id()), 0) + sign * step;
            double[] bounds = field.bounds(this.values);
            this.boxes.get(field.id()).setValue(Field.format(Math.max(bounds[0], Math.min(bounds[1], next))));
        }));
    }

    private void options(GuiGraphics graphics, Field field, int cx, int top, int mouseX, int mouseY) {
        String forced = field.forced(this.values);
        List<int[]> chips = FormDraw.chips(this.font, field, cx, this.controlWidth());
        for (int i = 0; i < chips.size(); i++) {
            String option = field.options().get(i);
            int[] chip = chips.get(i);
            int chipY = top + chip[1] * FormDraw.LINE;
            boolean shut = field.isShut(option);
            boolean picked = field.type() == Field.Type.MULTI ? this.values.picked(field.id(), option)
                    : option.equals(this.values.get(field.id()));
            boolean over = mouseX >= chip[0] && mouseX < chip[0] + chip[2] && mouseY >= chipY
                    && mouseY < chipY + FormDraw.CHIP;
            boolean usable = forced == null && !shut;
            int state = picked ? 2 : over && usable ? 1 : 0;
            FormattedCharSequence text = FormDraw.fit(this.font, field.option(option), chip[2] - 12 - (shut ? 8 : 0));
            FormDraw.chip(graphics, this.font, text, chip[0], chipY, chip[2], state, field.color(option),
                    forced != null || shut, shut);
            if (usable) {
                this.hits.add(new Hit(chip[0], chipY, chip[2], FormDraw.CHIP, () -> this.pick(field, option)));
            }
            if (over) {
                List<Component> parts = new ArrayList<>();
                Component own = field.optionTip(option);
                parts.add((own != null ? own : field.tip()).copy().withColor(0xC8C8C8));
                if (shut) {
                    parts.add(field.shutWhy().copy().withColor(FormDraw.LOCKED));
                } else if (forced != null) {
                    parts.add(Form.words("form.locked", field.why()).withColor(FormDraw.LOCKED));
                }
                this.tip = FormDraw.tip(this.font, field.option(option), parts);
            }
        }
    }

    private void entry(GuiGraphics graphics, Field field, int index, int top, int mouseX, int mouseY) {
        Component number = Form.words("field." + field.id() + ".entry", index + 1);
        if (field.required(this.values)) {
            number = Component.empty().append(number).append(Component.literal(" *").withColor(FormDraw.REQUIRED));
        }
        graphics.fill(this.x + 3, top + 2, this.x + 4, top + ROW + areaHeight(3), 0x40FFFFFF);
        graphics.drawString(this.font, FormDraw.fit(this.font, number, this.labelWidth - 16), this.x + 10, top + 6,
                0xFFB8B8B8, false);
        EditBox name = this.boxes.get(Field.entry(field.id(), index, "name"));
        MultiLineEditBox does = this.areas.get(Field.entry(field.id(), index, "desc"));
        this.place(name, this.controlX(), top + 2);
        this.place(does, this.x + 12, top + ROW);
        FormDraw.counter(graphics, this.font, does.getValue().length(), field.max(), this.x + this.width - BAR,
                top + ROW + areaHeight(3) + 2);
        if (mouseX >= this.x && mouseX < this.controlX() && mouseY >= top && mouseY < top + ROW) {
            this.tip = FormDraw.tip(this.font, number, List.of(
                    Form.words("field." + field.id() + ".entry.tip").withColor(0xC8C8C8),
                    Form.words("form.list", field.nameMax(), field.max()).withColor(0x8E8E8E)));
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !this.inside(mouseX, mouseY)) {
            return false;
        }
        for (Hit hit : List.copyOf(this.hits)) {
            if (mouseX >= hit.x() && mouseX < hit.x() + hit.width() && mouseY >= hit.y()
                    && mouseY < hit.y() + hit.height()) {
                this.host.focus(null);
                hit.action().run();
                return true;
            }
        }
        return false;
    }

    // Scrolls the form, unless a text box with the keys lies under the pointer: that one scrolls its own text.
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!this.inside(mouseX, mouseY)) {
            return false;
        }
        GuiEventListener focused = this.host.focused();
        if (focused instanceof MultiLineEditBox area && area.visible && area.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll - scrollY * SCROLL_STEP));
        return true;
    }

    // Opens a field's section, scrolls to it and gives its box (or its first unnamed entry) the keys.
    public void reveal(Field field) {
        for (Section section : this.form.sections()) {
            if (section.fields().contains(field)) {
                fold(this.form, section.id(), false);
            }
        }
        this.layout();
        String key = field.id();
        if (field.type() == Field.Type.LIST) {
            for (int i = 0; i < field.entries(this.values); i++) {
                if (this.values.raw(Field.entry(field.id(), i, "name")).isBlank()) {
                    key = Field.entry(field.id(), i, "name");
                    break;
                }
            }
        }
        Integer at = this.places.get(key);
        if (at != null) {
            this.scroll = Math.max(0, Math.min(this.maxScroll(), at - 22));
        }
        GuiEventListener widget = this.boxes.containsKey(key) ? this.boxes.get(key) : this.areas.get(key);
        if (widget != null) {
            this.followed = widget;
            this.host.focus(widget);
        }
    }
}
