package nl.tivek.multiversepowers.bugreport.client;

import static nl.tivek.multiversepowers.bugreport.client.form.Field.area;
import static nl.tivek.multiversepowers.bugreport.client.form.Field.choice;
import static nl.tivek.multiversepowers.bugreport.client.form.Field.text;

import java.util.List;
import nl.tivek.multiversepowers.bugreport.client.form.Field;
import nl.tivek.multiversepowers.bugreport.client.form.Form;
import nl.tivek.multiversepowers.bugreport.client.form.Section;

// The forms of the feedback page: the bug report, and an idea's, which depends on its category (only New mob, New
// faction and No category have a form yet; the others are shut until theirs is built). Each form's words are in
// en_us.json under `screen.welcomescreen.bugreport.field.` and `.section.`.
final class FeedbackForms {
    static final String CATEGORY = "idea.category";
    static final String MOB = "mob";
    static final String FACTION = "faction";
    static final String GLOBAL = "global";
    private static final String[] CATEGORIES = {MOB, FACTION, "magic", "character", "ability", "biome", "dimension",
        "item", "block", "structure", "event", GLOBAL};

    // Only the category to pick, while none is picked.
    static final Form PICK = new Form("idea.pick", "idea.name", "idea.priority", List.of(category()));

    static final Form GLOBAL_FORM = new Form(GLOBAL, "idea.name", "idea.priority", List.of(
            category(),
            Section.of("idea.basics",
                    text("idea.name", 80).required(),
                    priority("idea.priority")),
            Section.of("idea.idea",
                    area("idea.description", 2000, 8).required(),
                    area("idea.why", 500, 3)),
            Section.optional("idea.extra",
                    choice("idea.part", "characters", "mobs", "world", "items", "menus", "other"),
                    text("idea.like", 80))));

    static final Form BUG = new Form("bug", "bug.name", "bug.priority", List.of(
            Section.of("bug.basics",
                    text("bug.name", 80).required(),
                    choice("bug.type", "crash", "visual", "gameplay", "sound", "menus", "lag", "multiplayer", "other")
                            .required(),
                    choice("bug.crash_when", "startup", "joining", "playing", "leaving")
                            .when(v -> v.is("bug.type", "crash")),
                    choice("bug.often", "always", "sometimes", "once"),
                    priority("bug.priority")),
            Section.of("bug.what",
                    area("bug.description", 1500, 6).required(),
                    area("bug.steps", 1000, 4),
                    area("bug.expected", 400, 3)),
            Section.optional("bug.where",
                    text("bug.character", 40),
                    choice("bug.game", "single", "server", "lan"),
                    Field.yesNo("bug.mods", ""),
                    text("bug.mods_list", 200).when(v -> v.is("bug.mods", "yes")))));

    private FeedbackForms() {
    }

    static Section category() {
        return Section.of("idea.category",
                choice(CATEGORY, CATEGORIES).required().unreported()
                        .shut("magic", "character", "ability", "biome", "dimension", "item", "block", "structure",
                                "event"));
    }

    static Field priority(String id) {
        return choice(id, "low", "medium", "high").fallback("medium").required()
                .colors(BugReporter.Priority.LOW.color, BugReporter.Priority.MEDIUM.color,
                        BugReporter.Priority.HIGH.color);
    }

    static Form idea(String category) {
        return switch (category) {
            case MOB -> MobIdea.FORM;
            case FACTION -> FactionIdea.FORM;
            case GLOBAL -> GLOBAL_FORM;
            default -> PICK;
        };
    }
}
