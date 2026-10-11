package nl.tivek.multiversepowers.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import nl.tivek.multiversepowers.command.ShortCommand.Action;
import nl.tivek.multiversepowers.command.ShortCommand.Arg;
import nl.tivek.multiversepowers.command.ShortCommand.Group;
import nl.tivek.multiversepowers.command.ShortCommand.Kind;

// Every short command, in the order the Commands page lists them: what each does is
// `command.<mod>.<name>` in en_us.json.
public final class ShortCommands {
    private static final List<ShortCommand> ALL = new ArrayList<>();
    private static final String[][] RULES = {
            {"keepinv", "keepInventory"}, {"daycycle", "doDaylightCycle"}, {"weathercycle", "doWeatherCycle"},
            {"mobgrief", "mobGriefing"}, {"firetick", "doFireTick"}, {"mobspawn", "doMobSpawning"},
            {"insomnia", "doInsomnia"}, {"falldmg", "fallDamage"}, {"firedmg", "fireDamage"},
            {"drowndmg", "drowningDamage"}, {"freezedmg", "freezeDamage"}, {"mobloot", "doMobLoot"},
            {"blockdrops", "doTileDrops"}, {"instarespawn", "doImmediateRespawn"}, {"natregen", "naturalRegeneration"},
            {"patrols", "doPatrolSpawning"}, {"traders", "doTraderSpawning"}, {"wardens", "doWardenSpawning"},
            {"deathmsg", "showDeathMessages"}, {"advmsg", "announceAdvancements"},
            {"feedback", "sendCommandFeedback"}, {"limitcraft", "doLimitedCrafting"}};
    private static final String[][] EFFECTS = {
            {"speed", "speed"}, {"slow", "slowness"}, {"haste", "haste"}, {"fatigue", "mining_fatigue"},
            {"strength", "strength"}, {"jump", "jump_boost"}, {"nausea", "nausea"}, {"regen", "regeneration"},
            {"res", "resistance"}, {"fireres", "fire_resistance"}, {"breath", "water_breathing"},
            {"invis", "invisibility"}, {"blind", "blindness"}, {"nv", "night_vision"}, {"hunger", "hunger"},
            {"weak", "weakness"}, {"poison", "poison"}, {"wither", "wither"}, {"hpboost", "health_boost"},
            {"absorb", "absorption"}, {"sat", "saturation"}, {"glow", "glowing"}, {"levitate", "levitation"},
            {"luck", "luck"}, {"badluck", "unluck"}, {"slowfall", "slow_falling"}, {"conduit", "conduit_power"},
            {"dolphin", "dolphins_grace"}, {"badomen", "bad_omen"}, {"hero", "hero_of_the_village"},
            {"dark", "darkness"}, {"windy", "wind_charged"}, {"weaving", "weaving"}, {"oozing", "oozing"},
            {"infested", "infested"}};
    private static final String[][] GIVES = {
            {"food", "cooked_beef", "64"}, {"torches", "torch", "64"}, {"stone", "stone", "64"},
            {"logs", "oak_log", "64"}, {"glass", "glass", "64"}, {"tnt", "tnt", "16"}, {"pearls", "ender_pearl", "16"},
            {"rockets", "firework_rocket", "64"}, {"gapples", "golden_apple", "16"}, {"totem", "totem_of_undying", "1"},
            {"elytra", "elytra", "1"}, {"water", "water_bucket", "1"}, {"lava", "lava_bucket", "1"},
            {"bottles", "experience_bottle", "64"}, {"beacon", "beacon", "1"}};

    static {
        run("gs", Group.MODE, "gamemode survival $player", player());
        run("gc", Group.MODE, "gamemode creative $player", player());
        run("ga", Group.MODE, "gamemode adventure $player", player());
        run("gsp", Group.MODE, "gamemode spectator $player", player());

        run("day", Group.TIME, "time set day");
        run("night", Group.TIME, "time set night");
        run("noon", Group.TIME, "time set noon");
        run("midnight", Group.TIME, "time set midnight");
        run("sunrise", Group.TIME, "time set 23000");
        run("sunset", Group.TIME, "time set 12000");

        run("sun", Group.WEATHER, "weather clear");
        run("rain", Group.WEATHER, "weather rain");
        run("storm", Group.WEATHER, "weather thunder");

        run("peace", Group.WORLD, "difficulty peaceful");
        run("easy", Group.WORLD, "difficulty easy");
        run("normal", Group.WORLD, "difficulty normal");
        run("hard", Group.WORLD, "difficulty hard");
        run("wspawn", Group.WORLD, "setworldspawn");
        run("loc", Group.WORLD, "locate structure $structure", new Arg("structure", Kind.TEXT, null));
        run("biome", Group.WORLD, "locate biome $biome", new Arg("biome", Kind.TEXT, null));

        for (String[] rule : RULES) {
            run(rule[0], Group.RULES, "gamerule " + rule[1] + " $on", new Arg("on", Kind.SWITCH, "on"));
        }
        run("sleep", Group.RULES, "gamerule playersSleepingPercentage $percent", new Arg("percent", Kind.NUMBER,
                "100"));
        run("tickspeed", Group.RULES, "gamerule randomTickSpeed $speed", new Arg("speed", Kind.NUMBER, "3"));

        for (String[] effect : EFFECTS) {
            run(effect[0], Group.EFFECTS, "effect give $player minecraft:" + effect[1] + " $seconds $level",
                    new Arg("level", Kind.LEVEL, "1"), new Arg("seconds", Kind.SECONDS, "infinite"), player());
        }
        run("e", Group.EFFECTS, "effect give $player $effect $seconds $level", new Arg("effect", Kind.EFFECT, null),
                new Arg("level", Kind.LEVEL, "1"), new Arg("seconds", Kind.SECONDS, "infinite"), player());
        run("ec", Group.EFFECTS, "effect clear $player", player());
        run("econe", Group.EFFECTS, "effect clear $player $effect", new Arg("effect", Kind.EFFECT, null), player());

        run("i", Group.ITEMS, "give $player $item $count", new Arg("item", Kind.ITEM, null),
                new Arg("count", Kind.NUMBER, "1"), player());
        run("ic", Group.ITEMS, "clear $player $item", new Arg("item", Kind.ITEM, null), player());
        run("ci", Group.ITEMS, "clear $player", player());
        run("kit", Group.ITEMS, kit("diamond"), player());
        run("nkit", Group.ITEMS, kit("netherite"), player());
        run("bow", Group.ITEMS, "give $player bow;give $player arrow $count", new Arg("count", Kind.NUMBER, "64"),
                player());
        for (String[] give : GIVES) {
            run(give[0], Group.ITEMS, "give $player minecraft:" + give[1] + " $count",
                    new Arg("count", Kind.NUMBER, give[2]), player());
        }

        act("heal", Group.PLAYER, 2, ShortActions::heal, player());
        act("feed", Group.PLAYER, 2, ShortActions::feed, player());
        act("fly", Group.PLAYER, 2, ShortActions::fly, player());
        act("god", Group.PLAYER, 2, ShortActions::god, player());
        act("ext", Group.PLAYER, 2, ShortActions::extinguish, player());
        act("repair", Group.PLAYER, 2, ShortActions::repair);
        act("repairall", Group.PLAYER, 2, ShortActions::repairAll, player());
        act("more", Group.PLAYER, 2, ShortActions::more);
        act("hat", Group.PLAYER, 2, ShortActions::hat);
        run("xpp", Group.PLAYER, "xp add $player $amount points", new Arg("amount", Kind.NUMBER, null), player());
        run("lvl", Group.PLAYER, "xp add $player $amount levels", new Arg("amount", Kind.NUMBER, null), player());
        run("xpclear", Group.PLAYER, "xp set $player 0 levels;xp set $player 0 points", player());
        run("ench", Group.PLAYER, "enchant $player $enchantment $level",
                new Arg("enchantment", Kind.ENCHANTMENT, null), new Arg("level", Kind.NUMBER, "1"), player());
        run("suicide", Group.PLAYER, "kill @s");
        run("setspawn", Group.PLAYER, "spawnpoint $player", player());
        act("pos", Group.PLAYER, 0, ShortActions::pos);

        act("top", Group.MOVE, 2, ShortActions::top);
        act("spawn", Group.MOVE, 2, ShortActions::spawn);
        act("back", Group.MOVE, 2, ShortActions::back);
        act("up", Group.MOVE, 2, ShortActions::up, new Arg("blocks", Kind.NUMBER, "1"));
        act("j", Group.MOVE, 2, ShortActions::jump);
        run("tpp", Group.MOVE, "tp @s $player", new Arg("player", Kind.PLAYERS, null));
        run("tph", Group.MOVE, "tp $player @s", new Arg("player", Kind.PLAYERS, null));

        act("sm", Group.MOBS, 2, ShortActions::summon, new Arg("creature", Kind.ENTITY, null),
                new Arg("count", Kind.NUMBER, "1"));
        act("butcher", Group.MOBS, 2, ShortActions::butcher, new Arg("radius", Kind.NUMBER, "64"));
        act("kmobs", Group.MOBS, 2, ShortActions::killMobs, new Arg("radius", Kind.NUMBER, "64"));
        run("kitems", Group.MOBS, "kill @e[type=item]");
        run("kxp", Group.MOBS, "kill @e[type=experience_orb]");
        act("smite", Group.MOBS, 2, ShortActions::smite, new Arg("player", Kind.PLAYERS, ""));
        act("boom", Group.MOBS, 2, ShortActions::boom, new Arg("power", Kind.NUMBER, "4"));
    }

    private ShortCommands() {
    }

    public static List<ShortCommand> all() {
        return Collections.unmodifiableList(ALL);
    }

    @Nullable
    public static ShortCommand find(String name) {
        for (ShortCommand command : ALL) {
            if (command.name().equals(name)) {
                return command;
            }
        }
        return null;
    }

    private static String kit(String metal) {
        StringBuilder kit = new StringBuilder();
        for (String piece : new String[] {"sword", "pickaxe", "axe", "shovel", "helmet", "chestplate", "leggings",
                "boots"}) {
            kit.append("give $player minecraft:").append(metal).append('_').append(piece).append(';');
        }
        return kit.append("give $player minecraft:shield;give $player minecraft:cooked_beef 32").toString();
    }

    private static Arg player() {
        return new Arg("player", Kind.PLAYERS, "@s");
    }

    private static void run(String name, Group group, String template, Arg... args) {
        ALL.add(new ShortCommand(name, group, 2, List.of(args), template, null));
    }

    private static void act(String name, Group group, int permission, Action action, Arg... args) {
        ALL.add(new ShortCommand(name, group, permission, List.of(args), null, action));
    }

    // `template` with every argument filled in, one of the game's commands per entry.
    static List<String> fill(String template, Map<String, String> args) {
        List<String> lines = new ArrayList<>();
        for (String line : template.split(";")) {
            String filled = line;
            // Longest names first, so $player is never taken for a shorter one.
            List<String> names = new ArrayList<>(args.keySet());
            names.sort((a, b) -> b.length() - a.length());
            for (String name : names) {
                filled = filled.replace("$" + name, args.get(name));
            }
            lines.add(filled.replaceAll(" +", " ").trim());
        }
        return lines;
    }
}
