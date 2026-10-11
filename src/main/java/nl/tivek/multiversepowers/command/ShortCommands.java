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
            {"mobgrief", "mobGriefing"}, {"mobspawn", "doMobSpawning"}};
    private static final String[][] EFFECTS = {{"speed", "speed"}, {"haste", "haste"}, {"nv", "night_vision"}};
    private static final String[][] GIVES = {{"food", "cooked_beef", "64"}};

    static {
        run("gs", Group.MODE, "gamemode survival $player", player());
        run("gc", Group.MODE, "gamemode creative $player", player());
        run("ga", Group.MODE, "gamemode adventure $player", player());
        run("gsp", Group.MODE, "gamemode spectator $player", player());

        run("day", Group.TIME, "time set day");
        run("night", Group.TIME, "time set night");

        run("sun", Group.WEATHER, "weather clear");
        run("rain", Group.WEATHER, "weather rain");
        run("storm", Group.WEATHER, "weather thunder");

        run("peace", Group.WORLD, "difficulty peaceful");
        run("normal", Group.WORLD, "difficulty normal");
        run("hard", Group.WORLD, "difficulty hard");
        run("loc", Group.WORLD, "locate structure $structure", new Arg("structure", Kind.TEXT, null));

        for (String[] rule : RULES) {
            run(rule[0], Group.RULES, "gamerule " + rule[1] + " $on", new Arg("on", Kind.SWITCH, "on"));
        }

        for (String[] effect : EFFECTS) {
            run(effect[0], Group.EFFECTS, "effect give $player minecraft:" + effect[1] + " $seconds $level",
                    new Arg("level", Kind.LEVEL, "1"), new Arg("seconds", Kind.SECONDS, "infinite"), player());
        }
        run("e", Group.EFFECTS, "effect give $player $effect $seconds $level", new Arg("effect", Kind.EFFECT, null),
                new Arg("level", Kind.LEVEL, "1"), new Arg("seconds", Kind.SECONDS, "infinite"), player());
        run("ec", Group.EFFECTS, "effect clear $player", player());

        run("i", Group.ITEMS, "give $player $item $count", new Arg("item", Kind.ITEM, null),
                new Arg("count", Kind.NUMBER, "1"), player());
        run("ci", Group.ITEMS, "clear $player", player());
        run("kit", Group.ITEMS, kit("diamond"), player());
        run("nkit", Group.ITEMS, kit("netherite"), player());
        for (String[] give : GIVES) {
            run(give[0], Group.ITEMS, "give $player minecraft:" + give[1] + " $count",
                    new Arg("count", Kind.NUMBER, give[2]), player());
        }

        act("heal", Group.PLAYER, 2, ShortActions::heal, player());
        act("feed", Group.PLAYER, 2, ShortActions::feed, player());
        act("fly", Group.PLAYER, 2, ShortActions::fly, player());
        act("god", Group.PLAYER, 2, ShortActions::god, player());
        act("repair", Group.PLAYER, 2, ShortActions::repair);
        act("more", Group.PLAYER, 2, ShortActions::more);
        run("lvl", Group.PLAYER, "xp add $player $amount levels", new Arg("amount", Kind.NUMBER, null), player());
        run("ench", Group.PLAYER, "enchant $player $enchantment $level",
                new Arg("enchantment", Kind.ENCHANTMENT, null), new Arg("level", Kind.NUMBER, "1"), player());
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
        run("kitems", Group.MOBS, "kill @e[type=item]");
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
