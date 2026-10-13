package nl.tivek.multiversepowers.bugreport.client;

import static nl.tivek.multiversepowers.bugreport.client.form.Field.area;
import static nl.tivek.multiversepowers.bugreport.client.form.Field.choice;
import static nl.tivek.multiversepowers.bugreport.client.form.Field.mobs;
import static nl.tivek.multiversepowers.bugreport.client.form.Field.multi;
import static nl.tivek.multiversepowers.bugreport.client.form.Field.text;
import static nl.tivek.multiversepowers.bugreport.client.form.Field.yesNo;

import java.util.ArrayList;
import java.util.List;
import nl.tivek.multiversepowers.bugreport.client.form.Field;
import nl.tivek.multiversepowers.bugreport.client.form.Form;
import nl.tivek.multiversepowers.bugreport.client.form.Section;
import nl.tivek.multiversepowers.bugreport.client.form.Values;
import nl.tivek.multiversepowers.faction.rules.FactionDefaults;

// The idea form for a new faction: what it is, how it starts toward players and what reputation does, which
// creatures are in it (picked from the game's own, or new ones described), how it stands to each faction the game
// starts with, and where it lives.
final class FactionIdea {
    // The factions every world starts with, as `faction/rules/FactionDefaults` has them.
    static final String[] FACTIONS = FactionDefaults.ids().toArray(String[]::new);

    static final Form FORM = new Form("faction", "faction.name", "faction.priority", List.of(
            FeedbackForms.category(),
            Section.of("faction.basics",
                    text("faction.name", 40).required(),
                    area("faction.description", 2500, 5).required(),
                    area("faction.lore", 2500, 4),
                    FeedbackForms.priority("faction.priority")),
            Section.of("faction.standing",
                    stand("faction.attitude").fallback("neutral").required(),
                    yesNo("faction.reputation", "yes"),
                    choice("faction.turns_hostile", "quickly", "normal", "slowly", "never").fallback("normal")
                            .when(FactionIdea::reputation),
                    choice("faction.turns_friendly", "quickly", "normal", "slowly", "never").fallback("normal")
                            .when(FactionIdea::reputation),
                    multi("faction.loses", "kill_member", "hit_member", "kill_friend", "steal", "trespass")
                            .fallback("kill_member,hit_member").when(FactionIdea::reputation),
                    multi("faction.gains", "kill_enemy", "trade", "gift", "protect", "quest")
                            .fallback("kill_enemy").when(FactionIdea::reputation),
                    text("faction.reputation_other", 150).when(FactionIdea::reputation)),
            Section.of("faction.members",
                    mobs("faction.mobs"),
                    area("faction.new_mobs", 2500, 3).requiredWhen(v -> v.get("faction.mobs").isBlank()),
                    stand("faction.among").fallback("friendly"),
                    yesNo("faction.help", "yes"),
                    yesNo("faction.joinable", "yes"),
                    choice("faction.join_how", "open", "invite", "reputation").fallback("open")
                            .when(v -> v.is("faction.joinable", "yes"))),
            relations(),
            Section.optional("faction.home",
                    multi("faction.biomes", "plains", "forest", "taiga", "snowy", "desert", "savanna", "jungle",
                            "swamp", "badlands", "mountains", "ocean", "river", "beach", "caves", "deep_dark",
                            "mushroom", "nether", "end", "anywhere"),
                    text("faction.base", 150),
                    text("faction.leader", 60)),
            Section.optional("faction.extra",
                    text("faction.color", 30),
                    area("faction.looks", 2500, 3),
                    text("faction.like", 60),
                    area("faction.notes", 2500, 4))));

    private FactionIdea() {
    }

    private static Field stand(String id) {
        return choice(id, "friendly", "neutral", "hostile").colors(0x6EE7A0, 0xFFA040, 0xFF6B6B);
    }

    private static Section relations() {
        List<Field> fields = new ArrayList<>();
        for (String faction : FACTIONS) {
            fields.add(stand("faction.to." + faction));
        }
        fields.add(text("faction.to_other", 200));
        return Section.of("faction.relations", fields.toArray(Field[]::new));
    }

    private static boolean reputation(Values values) {
        return values.is("faction.reputation", "yes");
    }
}
