package nl.tivek.multiversepowers.bugreport.client.form;

import java.util.Map;

// A form's answers: what was typed or picked (`raw`), and as the form reads it (`get`: defaults, bounds and locks
// applied), which is what decides what shows.
public final class Values {
    private final Form form;
    private final Map<String, String> raw;

    public Values(Form form, Map<String, String> raw) {
        this.form = form;
        this.raw = raw;
    }

    public String raw(String id) {
        return this.raw.getOrDefault(id, "");
    }

    public boolean has(String id) {
        return this.raw.containsKey(id);
    }

    public String get(String id) {
        Field field = this.form.field(id);
        return field == null ? this.raw(id) : field.value(this);
    }

    public boolean is(String id, String... any) {
        String value = this.get(id);
        for (String option : any) {
            if (option.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean picked(String id, String option) {
        for (String part : this.get(id).split(",")) {
            if (part.equals(option)) {
                return true;
            }
        }
        return false;
    }
}
