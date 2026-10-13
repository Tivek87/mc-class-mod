package nl.tivek.multiversepowers.faction.rules;

import java.util.ArrayList;
import java.util.List;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.faction.Standing;

// The factions every world starts with, as lines of its settings file: peoples of their own, each with its story,
// how it starts toward players and which creatures belong to it. Each stands toward players as its creatures would
// by nature (a hostile faction's creatures hunt, a neutral one's only answer a provocation), and toward the other
// factions where its story says so; elsewhere each creature does as its nature has it. The Ironfang Syndicate has no
// creatures: it is a faction for players to join.
public final class FactionDefaults {
    public static final String MONSTERS = "creeping";
    public static final String WILDLIFE = "wildkin";
    public static final String SEA = "tidefolk";
    private static final String OWN = MultiversePowers.MODID + ":";
    private static final Standing HOSTILE = Standing.HOSTILE;
    private static final Standing NEUTRAL = Standing.NEUTRAL;
    private static final Standing FRIENDLY = Standing.FRIENDLY;

    // `united`: its members never fight each other. `open`: players may join it without an invite.
    private record Start(String id, String name, int color, Standing attitude, boolean reputation, boolean help,
            boolean united, boolean open, String description, String lore, String... kinds) {
    }

    private static final Start[] STARTS = {
        new Start("hearthbound", "The Hearthbound Accord", 0x4CC9A6, NEUTRAL, true, true, true, true,
                "The free villages of the Overworld: farmers, smiths and traders, the wandering merchants who carry"
                        + " their goods, and the iron and snow golems they raise to keep the night out.",
                "Every village swears one oath at its bell: no door barred to an honest traveller, no mercy for a"
                        + " raider. Trade with them and you are kin; burn one hut and every golem in the land learns"
                        + " your face.",
                "villager", "wandering_trader", "trader_llama", "iron_golem", "snow_golem"),
        new Start("covenant", "The Exiled Covenant", 0x7A8494, HOSTILE, true, true, true, true,
                "Outcasts driven from the villages long ago: pillagers and vindicators, the evokers who lead them,"
                        + " the witches who brew for them and the ravagers they ride to war.",
                "They were villagers once, until they bargained with something under the dark oaks that paid in"
                        + " totems instead of emeralds. Now they raid the bells that cast them out.",
                "pillager", "vindicator", "evoker", "illusioner", "ravager", "vex", "witch"),
        new Start("legion", "The Hollow Legion", 0x8BD45A, HOSTILE, true, true, true, false,
                "The restless dead in their endless ranks: zombies and skeletons, the drowned and the husks, the"
                        + " phantoms of the night sky and the Wither that would lead them all.",
                "No one remembers the war that raised them, only that it never ended. Each dusk the Legion musters"
                        + " and marches on whatever still breathes; each dawn it burns, and each dusk it rises again.",
                "zombie", "husk", "drowned", "zombie_villager", "skeleton", "stray", "bogged", "phantom", "giant",
                "zombie_horse", "skeleton_horse", "wither"),
        new Start("blight", "The Rotborn Blight", 0xA8B85A, HOSTILE, true, true, true, false,
                "Not the honest dead but the sick: brutes swollen with rot, bloaters, lurkers and screamers, the"
                        + " rust-eaten Rustbound and the ferals their cries call up out of the ground.",
                "It began in a single mine, with rust in the water and a cough that would not leave. The Blight does"
                        + " not march like the Legion: it spreads, it waits in the dark, and it screams when it finds"
                        + " you.",
                OWN + "zombie_brute", OWN + "bloater", OWN + "lurker", OWN + "screamer", OWN + "feral",
                OWN + "rustbound"),
        new Start("horde", "The Gilded Horde", 0xF2C84B, HOSTILE, true, true, true, false,
                "The piglin clans of the crimson forests and the bastions: gold-hungry raiders and the brutes who"
                        + " guard their hoards.",
                "In the Horde a piglin is worth what it carries. Wear their gold and they may call you cousin; take"
                        + " it, and the whole bastion comes screaming down the ramparts.",
                "piglin", "piglin_brute"),
        new Start("cinder", "The Cinder Court", 0xE25C5C, HOSTILE, true, false, true, false,
                "The fortress lords of the Nether: blazes, ghasts and magma cubes, the wither skeletons who man the"
                        + " walls, the hoglins and zoglins that roam the crimson wastes, and the elemental blazes that"
                        + " strayed into the caves of the Overworld.",
                "The Court was old when the first portal opened. Its halls of nether brick still stand, and the"
                        + " blazes still keep their posts for a king who walked into the fire and never came out.",
                "blaze", "ghast", "magma_cube", "wither_skeleton", "hoglin", "zoglin", OWN + "elemental_blaze"),
        new Start("wanderers", "The Wanderers Between", 0xA57BEA, NEUTRAL, true, false, true, false,
                "Creatures caught between worlds: the endermen who step from place to place, the zombified piglins"
                        + " who stumbled out of the portals, and the striders of the lava seas.",
                "They belong nowhere and only want to be left to their wandering. Meet their eyes or raise a hand to"
                        + " one of their kind, and you learn how quickly a wanderer finds its way back to you.",
                "enderman", "zombified_piglin", "strider"),
        new Start("endwatch", "The Endwatch", 0xD8B4F8, HOSTILE, true, false, true, false,
                "The sentinels of the outer islands: shulkers in their purpur towers, the endermites that swarm from"
                        + " broken pearls, and the Ender Dragon they keep watch for.",
                "The dragon sleeps on its pillar and the shulkers never sleep at all. Whoever crosses the void is"
                        + " watched from every tower, and no trespasser has left the islands as they came.",
                "shulker", "endermite", "ender_dragon"),
        new Start(MONSTERS, "The Creeping Dark", 0xE27BC4, HOSTILE, true, false, true, false,
                "What hunts where the light does not reach: creepers, spiders, silverfish and slimes, the breezes of"
                        + " the trial vaults, the Warden in the deep and the Stalker in the trees.",
                "The Dark has no banner and no throne, and needs neither. It waits behind every torch you forgot to"
                        + " place, and it is always patient.",
                "creeper", "spider", "cave_spider", "silverfish", "slime", "breeze", "warden", OWN + "stalker"),
        new Start("abyssal", "The Abyssal Throne", 0x3FA7A0, HOSTILE, true, true, true, false,
                "The guardians of the drowned monuments and the elder guardians who rule them, holding the deep for"
                        + " a sea-king long gone.",
                "The king of the deep has not been seen for an age. His guardians do not ask where he went: they keep"
                        + " his halls, his gold, and every diver who comes looking for either.",
                "guardian", "elder_guardian"),
        new Start(WILDLIFE, "The Wild Kin", 0xC9A27A, NEUTRAL, true, false, false, false,
                "The beasts of forest, mountain and plain: wolves and foxes, bears and deer, bees and owls, hunters"
                        + " and hunted alike.",
                "The wild keeps its own law: eat or be eaten, and leave the young alone. Walk softly and the Kin let"
                        + " you pass; strike first and the whole forest remembers.",
                "wolf", "fox", "ocelot", "polar_bear", "panda", "parrot", "bat", "bee", "frog", "armadillo", "sniffer",
                "allay", "goat", "rabbit", OWN + "brown_bear", OWN + "deer", OWN + "raccoon", OWN + "hedgehog",
                OWN + "owl", OWN + "squirrel", OWN + "snake", OWN + "moose", OWN + "lynx", OWN + "monkey",
                OWN + "elephant", OWN + "giraffe", OWN + "duck", OWN + "crocodile"),
        new Start("meadowfolk", "The Meadowfolk", 0xF2994A, FRIENDLY, false, false, false, false,
                "The tame beasts of field and pen: cattle and sheep, pigs and chickens, horses, llamas and camels,"
                        + " and the cats that keep the barns.",
                "They gave up the wild long ago for grain, grass and a fence that holds. They have no quarrel with"
                        + " anyone, which is as well, since everyone has a quarrel with them at dinner time.",
                "cow", "mooshroom", "pig", "sheep", "chicken", "horse", "donkey", "mule", "llama", "camel", "cat"),
        new Start(SEA, "The Tidefolk", 0x5CB8E2, NEUTRAL, false, false, false, false,
                "Everything that swims the open sea and the rivers: fish, squid, dolphins, turtles, axolotls and"
                        + " crabs.",
                "The sea takes no sides and keeps no score. Its people trade only in currents and moonlight, and the"
                        + " dolphins still lead a lost sailor home.",
                "cod", "salmon", "tropical_fish", "pufferfish", "squid", "glow_squid", "dolphin", "turtle", "axolotl",
                "tadpole", OWN + "crab"),
        new Start("conclave", "The Arcane Conclave", 0x6F8CF0, HOSTILE, true, false, true, false,
                "The Wizard and everything that answers his call: his familiars, his sentries and the rifts he tears"
                        + " through the sky.",
                "The Conclave was once a circle of seven. Six stopped asking what lay beyond the stars; the seventh"
                        + " did not, and now he comes down out of the rift alone to see who dares stand in his way.",
                OWN + "wizard"),
        new Start("ironfang", "The Ironfang Syndicate", 0xB8C0CC, NEUTRAL, true, true, true, true,
                "A rugged frontier faction of rogue monster-hunters, sellswords and scavenging outlaws. Driven by"
                        + " profit rather than oaths, they hunt rare elemental entities and salvage relics from"
                        + " dungeons to sell to the highest bidder.",
                "Every Ironfang wears an iron fang on a cord: the first coin they ever earned, hammered into a tooth."
                        + " Lose it and you are out. Sell it and you are dead."),
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
            lines.add(id + "attitude=" + FactionTable.name(start.attitude()));
            lines.add(id + "reputation=" + start.reputation());
            lines.add(id + "help=" + start.help());
            lines.add(id + "open=" + start.open());
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
                lines.add(start.id() + ">" + start.id() + "=" + FactionTable.name(FRIENDLY));
            }
        }
        // The villages against all who raid them, and they against the villages.
        both(lines, "hearthbound", "covenant", HOSTILE);
        both(lines, "hearthbound", "legion", HOSTILE);
        both(lines, "hearthbound", "blight", HOSTILE);
        // The hunters: welcome in the villages, sworn against what preys on them.
        both(lines, "hearthbound", "ironfang", FRIENDLY);
        both(lines, "ironfang", "covenant", HOSTILE);
        for (String prey : new String[] {"legion", "blight", MONSTERS, "cinder", "endwatch", "abyssal"}) {
            lines.add("ironfang>" + prey + "=" + FactionTable.name(HOSTILE));
        }
        return lines;
    }

    private static void both(List<String> lines, String one, String other, Standing standing) {
        lines.add(one + ">" + other + "=" + FactionTable.name(standing));
        lines.add(other + ">" + one + "=" + FactionTable.name(standing));
    }

    public static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (Start start : STARTS) {
            ids.add(start.id());
        }
        return ids;
    }
}
