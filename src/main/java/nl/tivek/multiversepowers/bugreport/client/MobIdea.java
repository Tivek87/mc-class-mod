package nl.tivek.multiversepowers.bugreport.client;

import static nl.tivek.multiversepowers.bugreport.client.form.Field.area;
import static nl.tivek.multiversepowers.bugreport.client.form.Field.choice;
import static nl.tivek.multiversepowers.bugreport.client.form.Field.list;
import static nl.tivek.multiversepowers.bugreport.client.form.Field.multi;
import static nl.tivek.multiversepowers.bugreport.client.form.Field.number;
import static nl.tivek.multiversepowers.bugreport.client.form.Field.text;
import static nl.tivek.multiversepowers.bugreport.client.form.Field.yesNo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import nl.tivek.multiversepowers.bugreport.client.form.Field.Unit;
import nl.tivek.multiversepowers.bugreport.client.form.Form;
import nl.tivek.multiversepowers.bugreport.client.form.Section;
import nl.tivek.multiversepowers.bugreport.client.form.Values;

// The idea form for a new mob: what it is, how it fights, how tough it is, where it spawns, what it drops, its tier
// with that many abilities, and for a boss its fight.
final class MobIdea {
    // The faction it belongs to: none, one every world starts with, or a new one. Above the form, which reads it.
    private static final String[] FACTIONS;

    static {
        List<String> factions = new ArrayList<>(List.of("none"));
        factions.addAll(List.of(FactionIdea.FACTIONS));
        factions.add("new");
        FACTIONS = factions.toArray(String[]::new);
    }

    // How many abilities each tier has, fewest and most.
    private static final Map<String, int[]> ABILITIES = Map.of("entity", new int[] {2, 7}, "elite", new int[] {5, 9},
            "mini_boss", new int[] {8, 14}, "boss", new int[] {10, 20}, "world_boss", new int[] {14, 28});

    static final Form FORM = new Form("mob", "mob.name", "mob.priority", List.of(
            FeedbackForms.category(),
            Section.of("mob.basics",
                    text("mob.name", 40).required(),
                    area("mob.description", 750, 5).required(),
                    area("mob.lore", 500, 4),
                    FeedbackForms.priority("mob.priority")),
            Section.of("mob.nature",
                    choice("mob.tier", "entity", "elite", "mini_boss", "boss", "world_boss").fallback("entity")
                            .required(),
                    choice("mob.attitude", "hostile", "neutral", "friendly").required()
                            .colors(0xFF6B6B, 0xFFA040, 0x6EE7A0),
                    choice("mob.faction", FACTIONS).fallback("none"),
                    text("mob.faction_new", 40).required().when(v -> v.is("mob.faction", "new")),
                    choice("mob.kind", "undead", "humanoid", "animal", "monster", "arthropod", "aquatic", "magical",
                            "construct", "elemental", "nether", "ender", "plant", "other").required(),
                    multi("mob.movement", "ground", "swims", "flies", "climbs", "burrows").fallback("ground")
                            .required(),
                    choice("mob.size", "tiny", "small", "normal", "large", "huge", "giant").fallback("normal")),
            Section.of("mob.combat",
                    yesNo("mob.attacks", "yes").lockedTo(v -> v.is("mob.attitude", "hostile") ? "yes" : null,
                            "hostile"),
                    choice("mob.style", "melee", "ranged", "both").fallback("melee").required()
                            .when(MobIdea::attacks),
                    number("mob.damage", 1, 200, Unit.HEALTH, 4).when(MobIdea::attacks),
                    number("mob.reach", 2, 64, Unit.BLOCKS, 16)
                            .when(v -> attacks(v) && v.is("mob.style", "ranged", "both")),
                    number("mob.aggro", 1, 128, Unit.BLOCKS, 16)),
            Section.of("mob.health",
                    number("mob.hp", 1, 2048, Unit.HEALTH, 20),
                    yesNo("mob.regen", "no"),
                    number("mob.regen_rate", 0.1, 100, Unit.PER_SECOND, 0.5).decimals()
                            .when(v -> v.is("mob.regen", "yes")),
                    yesNo("mob.armor", "no"),
                    number("mob.armor_points", 1, 30, Unit.COUNT, 4).when(v -> v.is("mob.armor", "yes")),
                    yesNo("mob.toughness", "no"),
                    number("mob.toughness_points", 1, 20, Unit.COUNT, 2).when(v -> v.is("mob.toughness", "yes")),
                    yesNo("mob.sunburn", "yes").when(v -> v.is("mob.kind", "undead")),
                    multi("mob.immune", "fire", "fall", "drowning", "knockback", "arrows", "magic", "poison")),
            Section.of("mob.spawning",
                    yesNo("mob.spawns", "yes").lockedTo(v -> v.is("mob.tier", "boss", "world_boss") ? "no" : null,
                            "boss"),
                    multi("mob.biomes", "plains", "forest", "taiga", "snowy", "desert", "savanna", "jungle",
                            "swamp", "badlands", "mountains", "ocean", "river", "beach", "caves", "deep_dark",
                            "mushroom", "nether", "end", "anywhere").required().when(MobIdea::spawns),
                    choice("mob.time", "day", "night", "both").fallback("both").when(MobIdea::spawns),
                    choice("mob.rarity", "common", "uncommon", "rare", "very_rare").fallback("common")
                            .when(MobIdea::spawns),
                    number("mob.group", 1, 12, Unit.COUNT, 1).when(MobIdea::spawns),
                    area("mob.appears", 300, 3).when(v -> !spawns(v))
                            .requiredWhen(v -> v.is("mob.tier", "boss", "world_boss"))),
            Section.of("mob.drops",
                    yesNo("mob.drops", "yes"),
                    text("mob.drops_what", 100).required().when(v -> v.is("mob.drops", "yes")),
                    number("mob.xp", 0, 1000, Unit.COUNT, 5)),
            Section.of("mob.abilities",
                    number("mob.ability_count", 2, 28, Unit.COUNT, 2).range(MobIdea::abilityRange).unreported(),
                    list("mob.abilities", "mob.ability_count", 40, 300).required()),
            Section.of("mob.boss",
                    area("mob.arena", 300, 3).when(MobIdea::boss).requiredWhen(v -> v.is("mob.tier", "boss",
                            "world_boss")),
                    number("mob.phases", 1, 10, Unit.COUNT, 1).when(MobIdea::boss),
                    yesNo("mob.voice", "no").when(v -> v.is("mob.tier", "boss", "world_boss")),
                    area("mob.voice_lines", 300, 3).when(v -> v.is("mob.tier", "boss", "world_boss")
                            && v.is("mob.voice", "yes")),
                    area("mob.loot", 300, 3).when(MobIdea::boss)
                            .requiredWhen(v -> v.is("mob.tier", "world_boss")),
                    choice("mob.rematch", "never", "special").fallback("never")
                            .when(v -> v.is("mob.tier", "world_boss")),
                    text("mob.rematch_how", 200).required()
                            .when(v -> v.is("mob.tier", "world_boss") && v.is("mob.rematch", "special")),
                    area("mob.lore_impact", 300, 3).when(v -> v.is("mob.tier", "world_boss"))),
            Section.optional("mob.extra",
                    area("mob.looks", 300, 3),
                    text("mob.sounds", 150),
                    text("mob.like", 60),
                    area("mob.notes", 500, 4))));

    private MobIdea() {
    }

    private static boolean attacks(Values values) {
        return values.is("mob.attacks", "yes");
    }

    private static boolean spawns(Values values) {
        return values.is("mob.spawns", "yes");
    }

    private static boolean boss(Values values) {
        return values.is("mob.tier", "mini_boss", "boss", "world_boss");
    }

    private static int[] abilityRange(Values values) {
        return ABILITIES.getOrDefault(values.get("mob.tier"), ABILITIES.get("entity"));
    }
}
