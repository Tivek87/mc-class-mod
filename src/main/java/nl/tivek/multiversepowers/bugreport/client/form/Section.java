package nl.tivek.multiversepowers.bugreport.client.form;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

// A part of a form that folds shut under its heading (`section.<id>` in en_us.json, `.tip` its tooltip); shown while
// any of its fields is. An optional one is named so on its heading.
public record Section(String id, boolean optional, List<Field> fields) {
    public static Section of(String id, Field... fields) {
        return new Section(id, false, List.of(fields));
    }

    public static Section optional(String id, Field... fields) {
        return new Section(id, true, List.of(fields));
    }

    public MutableComponent title() {
        return Form.words("section." + this.id);
    }

    public Component tip() {
        return Form.words("section." + this.id + ".tip");
    }

    public boolean shown(Values values) {
        for (Field field : this.fields) {
            if (field.shown(values)) {
                return true;
            }
        }
        return false;
    }

    public int missing(Values values) {
        int missing = 0;
        for (Field field : this.fields) {
            if (field.missing(values)) {
                missing++;
            }
        }
        return missing;
    }
}
