package nl.tivek.multiversepowers.faction.rules;

import java.util.ArrayList;
import java.util.List;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.faction.Standing;

// The factions every world starts with, as lines of its settings file. Each leaves its creatures as the game has
// them: no attitude of its own and no relations but where the game agrees: its own members at peace with each
// other, and the Illagers after the Villagers. Only a player's reputation moves them.
public final class FactionDefaults {
    public static final String MONSTERS = "monsters";
    public static final String WILDLIFE = "wildlife";
    public static final String SEA = "sea";
    private static final String OWN = MultiversePowers.MODID + ":";

    // `united`: its members never fight each other.
    private record Start(String id, String name, int color, boolean reputation, boolean help, boolean united,
            String description, String lore, String... kinds) {
    }

    private static final Start[] STARTS = {
        new Start("villagers", "Villagers", 0x4CC9A6, true, true, true,
                "The people of the villages, their traders and the golems that guard them.",
                "They trade with anyone who leaves them in peace, and remember who did not.",
                "villager", "wandering_trader", "trader_llama", "iron_golem", "snow_golem"),
        new Start("illagers", "Illagers", 0x7A8494, true, true, true,
                "Outcasts who raid villages: pillagers, vindicators, evokers and what they bring along.",
                "Driven out long ago, they came back with crossbows and a grudge.",
                "pillager", "vindicator", "evoker", "illusioner", "ravager", "vex"),
        // Not united: a skeleton's stray arrow sets a zombie on it, as in the game.
        new Start("undead", "Undead", 0x8BD45A, true, false, false,
                "Everything that died and got up again: zombies, skeletons, phantoms and the Wither.",
                "They do not rest, and they do not like the living.",
                "zombie", "husk", "drowned", "zombie_villager", "skeleton", "stray", "bogged", "wither_skeleton",
                "phantom", "zombie_horse", "skeleton_horse", "wither", "giant", OWN + "zombie_brute", OWN + "bloater",
                OWN + "lurker", OWN + "screamer", OWN + "feral", OWN + "rustbound"),
        new Start("piglins", "Piglins", 0xF2C84B, true, true, true,
                "The gold-hungry folk of the Nether, and those of them the Overworld turned.",
                "Wear their gold and they may let you pass. Touch their gold and they will not.",
                "piglin", "piglin_brute", "zombified_piglin"),
        new Start("nether", "Nether Dwellers", 0xE25C5C, true, false, false,
                "The beasts and spirits of the Nether: blazes, ghasts, hoglins, magma cubes and striders.",
                "Born of fire, in a place that was never meant for visitors.",
                "blaze", "ghast", "magma_cube", "hoglin", "zoglin", "strider"),
        new Start("end", "Endfolk", 0xA57BEA, true, false, false,
                "What lives in the End: endermen, shulkers and the dragon they circle.",
                "They were here before the stars went out, and they do not like to be looked at.",
                "enderman", "endermite", "shulker", "ender_dragon"),
        new Start(MONSTERS, "Monsters", 0xE27BC4, true, false, false,
                "The dangers of the dark that belong to no one: creepers, spiders, slimes, witches and worse.",
                "No banner, no leader. Only hunger.",
                "creeper", "spider", "cave_spider", "silverfish", "slime", "witch", "breeze", "warden", "guardian",
                "elder_guardian", OWN + "stalker"),
        new Start(WILDLIFE, "Wildlife", 0xC9A27A, true, false, false,
                "The wild animals of forest, mountain and plain, hunters and hunted alike.",
                "Leave them be and they leave you be. Mostly.",
                "wolf", "fox", "ocelot", "polar_bear", "panda", "parrot", "bat", "bee", "frog", "armadillo", "sniffer",
                "allay", "goat", "rabbit", OWN + "brown_bear", OWN + "deer", OWN + "raccoon", OWN + "hedgehog",
                OWN + "owl", OWN + "squirrel", OWN + "snake", OWN + "moose", OWN + "lynx", OWN + "monkey",
                OWN + "elephant", OWN + "giraffe", OWN + "duck", OWN + "crocodile"),
        new Start("farm", "Farm Animals", 0xF2994A, false, false, false,
                "The animals people keep: cattle, pigs, sheep, chickens, horses and cats.",
                "They ask for grass, grain and a fence that holds.",
                "cow", "mooshroom", "pig", "sheep", "chicken", "horse", "donkey", "mule", "llama", "camel", "cat"),
        new Start(SEA, "Sea Life", 0x5CB8E2, false, false, false,
                "What swims: fish, squid, dolphins, turtles and axolotls.",
                "The sea keeps its own, and does not keep score.",
                "cod", "salmon", "tropical_fish", "pufferfish", "squid", "glow_squid", "dolphin", "turtle", "axolotl",
                "tadpole", OWN + "crab"),
        new Start("arcane", "The Arcane", 0x6F8CF0, true, false, true,
                "The Wizard and whatever answers his call.",
                "He was studying the sky long before anyone thought to ask why.",
                OWN + "wizard"),
    };

    private FactionDefaults() {
    }

    public static List<String> factions() {
        List<String> lines = new ArrayList<>();
        for (Start start : STARTS) {
            String id = start.id() + "|";
            lines.add(id + "name=" + start.name());
            lines.add(id + "description=" + start.description());
            lines.add(id + "lore=" + start.lore());
            lines.add(id + "color=" + String.format("%06x", start.color()));
            lines.add(id + "attitude=" + FactionTable.GAME);
            lines.add(id + "reputation=" + start.reputation());
            lines.add(id + "help=" + start.help());
        }
        return lines;
    }

    public static List<String> members() {
        List<String> lines = new ArrayList<>();
        for (Start start : STARTS) {
            for (String kind : start.kinds()) {
                lines.add((kind.contains(":") ? kind : "minecraft:" + kind) + "=" + start.id());
            }
        }
        // Its pilot's when called, no one's when wild.
        lines.add(OWN + "mech_helper=");
        return lines;
    }

    public static List<String> relations() {
        List<String> lines = new ArrayList<>();
        for (Start start : STARTS) {
            if (start.united()) {
                lines.add(start.id() + ">" + start.id() + "=" + FactionTable.name(Standing.FRIENDLY));
            }
        }
        // As the game has it: every one of them comes for a village.
        lines.add("illagers>villagers=" + FactionTable.name(Standing.HOSTILE));
        return lines;
    }

    public static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (Start start : STARTS) {
            ids.add(start.id());
        }
        return ids;
    }
}
