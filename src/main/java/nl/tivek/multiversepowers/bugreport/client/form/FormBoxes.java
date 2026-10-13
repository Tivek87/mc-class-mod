package nl.tivek.multiversepowers.bugreport.client.form;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

// A form's answers and the game's text boxes it types them in, one per text, number and list entry, made at once
// and kept out of sight until `FormView` places them.
abstract class FormBoxes {
    static final int ROW = 20;
    static final int BAR = 6;

    final Form form;
    final Map<String, String> raw;
    final Values values;
    final Font font;
    final FormView.Host host;
    final int x;
    final int y;
    final int width;
    final int height;
    final int labelWidth;
    final Map<String, EditBox> boxes = new LinkedHashMap<>();
    final Map<String, MultiLineEditBox> areas = new LinkedHashMap<>();
    boolean muted;

    FormBoxes(Form form, Map<String, String> raw, Font font, FormView.Host host, int x, int y, int width,
              int height) {
        this.form = form;
        this.raw = raw;
        this.values = new Values(form, raw);
        this.font = font;
        this.host = host;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.labelWidth = Math.max(72, Math.min(124, width * 36 / 100));
        for (Section section : form.sections()) {
            for (Field field : section.fields()) {
                this.widgets(field);
            }
        }
    }

    public Values values() {
        return this.values;
    }

    int controlX() {
        return this.x + this.labelWidth;
    }

    int controlWidth() {
        return this.width - BAR - this.labelWidth;
    }

    static int areaHeight(int lines) {
        return lines * 9 + 8;
    }

    void widgets(Field field) {
        switch (field.type()) {
            case TEXT -> this.box(field.id(), this.controlWidth(), field.max(), field.label(), field.hint());
            case NUMBER -> {
                EditBox box = this.box(field.id(), 46, 9, field.label(), null);
                box.setFilter(text -> text.matches(field.hasDecimals() ? "[0-9]*\\.?[0-9]*" : "[0-9]*"));
            }
            case AREA -> this.area(field.id(), this.width - BAR, field.lines(), field.max(), field.label(),
                    field.hint());
            case LIST -> {
                int most = (int) this.form.field(field.count).high;
                for (int i = 0; i < most; i++) {
                    Component entry = Form.words("field." + field.id() + ".entry", i + 1);
                    this.box(Field.entry(field.id(), i, "name"), this.controlWidth(), field.nameMax(), entry,
                            Form.words("field." + field.id() + ".name.hint"));
                    this.area(Field.entry(field.id(), i, "desc"), this.width - BAR - 12, 3, field.max(), entry,
                            Form.words("field." + field.id() + ".desc.hint"));
                }
            }
            default -> {
            }
        }
    }

    EditBox box(String key, int width, int max, Component name, @Nullable Component hint) {
        EditBox box = new EditBox(this.font, 0, 0, width, 16, name);
        box.setMaxLength(max);
        box.setValue(this.raw.getOrDefault(key, ""));
        if (hint != null) {
            box.setHint(hint);
        }
        box.setResponder(value -> this.set(key, value));
        box.visible = false;
        this.boxes.put(key, box);
        this.host.add(box);
        return box;
    }

    void area(String key, int width, int lines, int max, Component name, @Nullable Component hint) {
        MultiLineEditBox area = new MultiLineEditBox(this.font, 0, 0, width, areaHeight(lines),
                hint == null ? Component.empty() : hint, name);
        // The box's own limit draws a counter under it, over the next row: the limit is kept here instead.
        area.setValue(cap(this.raw.getOrDefault(key, ""), max));
        area.setValueListener(value -> {
            if (value.length() > max) {
                area.setValue(cap(value, max));
                return;
            }
            this.set(key, value);
        });
        area.visible = false;
        this.areas.put(key, area);
        this.host.add(area);
    }

    static String cap(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

    void set(String key, String value) {
        if (this.muted || value.equals(this.raw.getOrDefault(key, ""))) {
            return;
        }
        this.raw.put(key, value);
        this.host.changed();
    }

    void pick(Field field, String option) {
        if (field.type() == Field.Type.MULTI) {
            List<String> picked = new ArrayList<>(List.of(this.values.get(field.id()).split(",")));
            picked.removeIf(String::isEmpty);
            if (!picked.remove(option)) {
                picked.add(option);
            }
            picked.sort((a, b) -> field.options().indexOf(a) - field.options().indexOf(b));
            this.set(field.id(), String.join(",", picked));
        } else if (option.equals(this.values.get(field.id())) && !field.required(this.values)
                && field.fallback.isEmpty()) {
            this.set(field.id(), "");
        } else {
            this.set(field.id(), option);
        }
    }

    // A number box out of focus shows the number as the form reads it (its default, kept in its bounds).
    void sync() {
        GuiEventListener focused = this.host.focused();
        this.muted = true;
        for (Field field : this.form.fields()) {
            EditBox box = this.boxes.get(field.id());
            if (field.type() == Field.Type.NUMBER && box != null && box != focused) {
                String shown = this.values.get(field.id());
                if (!shown.equals(box.getValue())) {
                    box.setValue(shown);
                }
            }
        }
        this.muted = false;
    }

}
