package nl.tivek.multiversepowers.bugreport.client.form;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

// A part of a sent report under its own heading: a table (`columns` and `rows`) or a text. The relay lays each out as
// Markdown on the GitHub issue.
public record Block(String title, List<String> columns, List<List<String>> rows, String text) {
    static Block table(String title, List<String> columns, List<List<String>> rows) {
        return new Block(title, columns, rows, "");
    }

    static Block text(String title, String text) {
        return new Block(title, List.of(), List.of(), text);
    }

    public boolean isTable() {
        return !this.columns.isEmpty();
    }

    public JsonObject json() {
        JsonObject json = new JsonObject();
        json.addProperty("title", this.title);
        if (this.isTable()) {
            JsonArray columns = new JsonArray();
            this.columns.forEach(columns::add);
            JsonArray rows = new JsonArray();
            for (List<String> row : this.rows) {
                JsonArray cells = new JsonArray();
                row.forEach(cells::add);
                rows.add(cells);
            }
            json.add("columns", columns);
            json.add("rows", rows);
        } else {
            json.addProperty("text", this.text);
        }
        return json;
    }
}
