package nl.tivek.multiversepowers.bugreport.client.form;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

// A report's questions in sections, and the report they make: its title and priority from two of its fields, every
// other answer given laid out in blocks, a table of the short ones per section and a block per text.
public final class Form {
    public static final String KEY = "screen.welcomescreen.bugreport.";

    private final String id;
    private final String titleField;
    private final String priorityField;
    private final List<Section> sections;
    private final Map<String, Field> fields = new LinkedHashMap<>();

    public Form(String id, String titleField, String priorityField, List<Section> sections) {
        this.id = id;
        this.titleField = titleField;
        this.priorityField = priorityField;
        this.sections = List.copyOf(sections);
        for (Section section : this.sections) {
            for (Field field : section.fields()) {
                this.fields.put(field.id, field);
            }
        }
    }

    public static MutableComponent words(String key, Object... args) {
        return Component.translatable(KEY + key, args);
    }

    public String id() {
        return this.id;
    }

    public List<Section> sections() {
        return this.sections;
    }

    public Field field(String id) {
        return this.fields.get(id);
    }

    public Iterable<Field> fields() {
        return this.fields.values();
    }

    public String title(Values values) {
        return values.get(this.titleField).strip();
    }

    public String priority(Values values) {
        return values.get(this.priorityField);
    }

    public List<Field> missing(Values values) {
        List<Field> missing = new ArrayList<>();
        for (Section section : this.sections) {
            for (Field field : section.fields()) {
                if (field.missing(values)) {
                    missing.add(field);
                }
            }
        }
        return missing;
    }

    // Whether `key` holds an answer of this form, a list entry's too: what Clear empties.
    public boolean owns(String key) {
        for (Field field : this.fields.values()) {
            if (key.equals(field.id) || field.type == Field.Type.LIST && key.startsWith(field.id + ".")) {
                return true;
            }
        }
        return false;
    }

    public List<Block> blocks(Values values) {
        List<Block> blocks = new ArrayList<>();
        for (Section section : this.sections) {
            if (!section.shown(values)) {
                continue;
            }
            List<List<String>> facts = new ArrayList<>();
            List<Block> texts = new ArrayList<>();
            for (Field field : section.fields()) {
                if (!field.shown(values) || !field.reported || field.id.equals(this.titleField)
                        || field.id.equals(this.priorityField)) {
                    continue;
                }
                String label = field.label().getString();
                switch (field.type) {
                    case AREA -> {
                        String text = values.get(field.id).strip();
                        if (!text.isEmpty()) {
                            texts.add(Block.text(label, text));
                        }
                    }
                    case LIST -> {
                        List<List<String>> rows = new ArrayList<>();
                        for (int i = 0; i < field.entries(values); i++) {
                            String name = values.raw(Field.entry(field.id, i, "name")).strip();
                            String does = values.raw(Field.entry(field.id, i, "desc")).strip();
                            if (!name.isEmpty() || !does.isEmpty()) {
                                rows.add(List.of(String.valueOf(i + 1), name, does));
                            }
                        }
                        if (!rows.isEmpty()) {
                            texts.add(Block.table(words("report.list", label, rows.size()).getString(),
                                    List.of("#", words("report.name").getString(), words("report.does").getString()),
                                    rows));
                        }
                    }
                    default -> {
                        String shown = field.display(values);
                        if (!shown.isEmpty()) {
                            facts.add(List.of(label, shown));
                        }
                    }
                }
            }
            if (!facts.isEmpty()) {
                blocks.add(Block.table(section.title().getString(),
                        List.of(words("report.question").getString(), words("report.answer").getString()), facts));
            }
            blocks.addAll(texts);
        }
        return blocks;
    }

    // The blocks as plain text, at most `max` long: what a relay that knows no blocks gets as the description.
    public static String plain(List<Block> blocks, int max) {
        StringBuilder text = new StringBuilder();
        for (Block block : blocks) {
            text.append(text.isEmpty() ? "" : "\n\n").append("== ").append(block.title()).append(" ==");
            if (block.isTable()) {
                for (List<String> row : block.rows()) {
                    text.append('\n').append(String.join(" | ", row));
                }
            } else {
                text.append('\n').append(block.text());
            }
        }
        return text.length() <= max ? text.toString() : text.substring(0, max - 3) + "...";
    }
}
