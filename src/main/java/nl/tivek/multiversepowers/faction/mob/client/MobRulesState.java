package nl.tivek.multiversepowers.faction.mob.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.SettingsPages;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.ScrollArea;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.mob.MobBulkEditPayload;
import nl.tivek.multiversepowers.faction.mob.MobPageActionPayload;
import nl.tivek.multiversepowers.faction.mob.MobRuleEditPayload;
import nl.tivek.multiversepowers.faction.mob.MobRules;
import nl.tivek.multiversepowers.faction.mob.MobSpawnEditPayload;
import nl.tivek.multiversepowers.faction.mob.MobStatEditPayload;
import nl.tivek.multiversepowers.faction.mob.MobStats;

// The Mobs page's state under `MobRulesScreen`: its creatures in rows and groups, what was sent and not back yet,
// a standing for many at once, each creature's numbers, and how each stands now.
abstract class MobRulesState extends NavScreen {
    static final String PREFIX = "config." + MultiversePowers.MODID + ".";
    static final int COLOR = 0x7FD8FF;
    static final int ROW = 18;
    static final int HEADER = 16;
    static final Standing[] ORDER = {Standing.FRIENDLY, Standing.NEUTRAL, Standing.HOSTILE};

    static final String VANILLA = "minecraft";
    // The groups folded in, by mod id, for as long as the game runs.
    static final Set<String> FOLDED = new HashSet<>();

    // A creature (`index` in the table's order) or the player (-1), and the mod it comes from.
    record Row(int index, EntityType<?> type, Component name, ItemStack icon, String group) {
    }

    static final int CREATURE = 0;
    static final int GROUP = 1;
    static final int ALL = 2;
    static final int NUMBERS = 3;
    static final int NUMBER = 4;

    // A line of the list: a creature, the head of a group, all shown, the way to the picked creature's numbers, or one
    // of them. `members`: the creatures a group or all shown holds.
    record Line(int kind, @Nullable Row row, @Nullable String group, List<Row> members,
            @Nullable MobStats.Stat stat) {
        static Line of(Row row) {
            return new Line(CREATURE, row, null, List.of(), null);
        }
    }

    final ScrollArea area = new ScrollArea();
    // Sent to the server, maybe not back yet, by "<creature>><target>": null for the game's own.
    final Map<String, Standing> sent = new HashMap<>();
    // Spawning turned on or off, maybe not back yet, by creature.
    final Map<String, Boolean> sentSpawn = new HashMap<>();
    // A creature's standing toward every creature, maybe not back yet.
    final Map<String, Standing> sentAny = new HashMap<>();
    // A number, maybe not back yet, by "<creature>|<number>".
    final Map<String, Double> sentStat = new HashMap<>();
    int stamp;
    // The picked creature's numbers shown instead of who it attacks.
    boolean numbers;
    List<Row> creatures = List.of();
    @Nullable
    Row player;
    @Nullable
    Row picked;
    String query = "";

    MobRulesState(Component title, @Nullable Screen root, String page) {
        super(title, root, page);
    }

    boolean editable() {
        return SettingsPages.worldEditable(MobRules.SPEC);
    }

    void build() {
        List<Row> rows = new ArrayList<>();
        List<EntityType<?>> types = ClientMobTable.types();
        for (int i = 0; i < types.size(); i++) {
            EntityType<?> type = types.get(i);
            if (type != null) {
                SpawnEggItem egg = SpawnEggItem.byId(type);
                rows.add(new Row(i, type, type.getDescription(),
                        egg != null ? new ItemStack(egg) : new ItemStack(Items.NAME_TAG),
                        MobRules.id(type).substring(0, MobRules.id(type).indexOf(':'))));
            }
        }
        rows.sort(Comparator.comparing(row -> row.name().getString()));
        this.creatures = rows;
        this.player = new Row(-1, EntityType.PLAYER, Component.translatable(PREFIX + "mobs.player"),
                new ItemStack(Items.PLAYER_HEAD), VANILLA);
    }

    // How `actor` stands to `target` now: what was just sent, else what the world sets, else the game's own.
    Standing shown(Row actor, Row target) {
        Standing own = ClientMobTable.of(actor.index(), target.index());
        String key = this.sent.isEmpty() ? null : key(actor, target);
        if (key != null && this.sent.containsKey(key)) {
            Standing chosen = this.sent.get(key);
            return chosen != null ? chosen : own;
        }
        Standing set = MobRules.set(actor.type(), target.type());
        return set != null ? set : own;
    }

    static String key(Row actor, Row target) {
        return MobRules.id(actor.type()) + ">" + MobRules.id(target.type());
    }

    int changes(Row actor) {
        int changed = MobStats.changed(actor.type());
        changed += this.shown(actor, this.player) != ClientMobTable.of(actor.index(), -1) ? 1 : 0;
        for (Row target : this.creatures) {
            changed += this.shown(actor, target) != ClientMobTable.of(actor.index(), target.index()) ? 1 : 0;
        }
        return changed;
    }

    void choose(Row actor, Row target, Standing standing) {
        boolean own = standing == ClientMobTable.of(actor.index(), target.index());
        this.sent.put(key(actor, target), own ? null : standing);
        PacketDistributor.sendToServer(new MobRuleEditPayload(MobRules.id(actor.type()), MobRules.id(target.type()),
                own ? -1 : standing.ordinal()));
    }

    void reset(Row actor) {
        this.sent.put(key(actor, this.player), null);
        for (Row target : this.creatures) {
            this.sent.put(key(actor, target), null);
        }
        PacketDistributor.sendToServer(new MobRuleEditPayload(MobRules.id(actor.type()), "", -1));
        if (MobStats.changed(actor.type()) > 0) {
            this.resetNumbers(actor);
        }
    }

    // How every creature in `rows` stands now (toward every creature, as a group in the list; or what the picked one
    // is toward each), or null where they differ.
    @Nullable
    Standing common(List<Row> rows) {
        Standing same = null;
        for (Row row : rows) {
            if (row == this.picked) {
                continue;
            }
            Standing now = this.picked != null ? this.shown(this.picked, row) : this.any(row);
            if (now == null || same != null && now != same) {
                return null;
            }
            same = now;
        }
        return same;
    }

    @Nullable
    Standing any(Row actor) {
        String id = MobRules.id(actor.type());
        if (this.sentAny.containsKey(id)) {
            return this.sentAny.get(id);
        }
        Standing any = MobRules.any(actor.type());
        return any != null ? any : MobRules.everyoneHostile() ? Standing.HOSTILE : null;
    }

    // Every one of `rows` at once: what the picked creature is toward each, or in the list each toward every creature.
    void all(List<Row> rows, Standing standing) {
        List<String> ids = new ArrayList<>();
        for (Row row : rows) {
            if (row != this.picked) {
                ids.add(MobRules.id(row.type()));
                if (this.picked != null) {
                    this.sent.put(key(this.picked, row), standing);
                } else {
                    this.sentAny.put(MobRules.id(row.type()), standing);
                }
            }
        }
        for (int from = 0; from < ids.size(); from += MobBulkEditPayload.MOST) {
            List<String> part = ids.subList(from, Math.min(ids.size(), from + MobBulkEditPayload.MOST));
            PacketDistributor.sendToServer(this.picked != null
                    ? new MobBulkEditPayload(List.of(MobRules.id(this.picked.type())), part, standing.ordinal())
                    : new MobBulkEditPayload(part, List.of(MobRules.ANY), standing.ordinal()));
        }
    }

    double stat(Row creature, MobStats.Stat stat) {
        return this.sentStat.getOrDefault(MobRules.id(creature.type()) + "|" + stat.key,
                MobStats.value(creature.type(), stat));
    }

    void stat(Row creature, MobStats.Stat stat, double value) {
        double clamped = stat.clamp(value);
        this.sentStat.put(MobRules.id(creature.type()) + "|" + stat.key, clamped);
        PacketDistributor.sendToServer(new MobStatEditPayload(MobRules.id(creature.type()), stat.key, clamped));
    }

    void resetNumbers(Row creature) {
        for (MobStats.Stat stat : MobStats.Stat.values()) {
            this.sentStat.put(MobRules.id(creature.type()) + "|" + stat.key, stat.normal);
        }
        PacketDistributor.sendToServer(new MobStatEditPayload(MobRules.id(creature.type()), MobRules.ANY, 0.0));
    }

    static Component value(MobStats.Stat stat, double value) {
        String number = value == Math.rint(value) ? String.valueOf((long) value)
                : String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "");
        return switch (stat.kind) {
            case TIMES -> Component.literal("×" + number);
            case PLUS -> Component.literal("+" + number);
            case PERCENT -> Component.literal(number + "%");
            case PER_SECOND -> Component.translatable(PREFIX + "mobs.per_second", number);
            case SWITCH -> Component.translatable(PREFIX + "mobs." + (value >= 0.5 ? "on" : "off"));
        };
    }

    boolean spawns(Row creature) {
        return this.sentSpawn.getOrDefault(MobRules.id(creature.type()), MobRules.spawns(creature.type()));
    }

    void spawning(Row creature, boolean spawns) {
        this.sentSpawn.put(MobRules.id(creature.type()), spawns);
        PacketDistributor.sendToServer(new MobSpawnEditPayload(MobRules.id(creature.type()), spawns));
    }

    static Component groupName(String group) {
        if (group.equals(VANILLA)) {
            return Component.translatable(PREFIX + "mobs.vanilla");
        }
        return Component.literal(ModList.get().getModContainerById(group).map(mod -> mod.getModInfo().getDisplayName())
                .orElse(group));
    }

    static boolean matches(Row row, String wanted) {
        return wanted.isEmpty() || row.name().getString().toLowerCase(Locale.ROOT).contains(wanted)
                || MobRules.id(row.type()).contains(wanted);
    }

    // The picked creature's numbers alone, or: the way to them, all shown, the player while a creature is picked, then a
    // group per mod, the game's own first; while searching every group shows what it found, folded or not.
    List<Line> shownLines() {
        String wanted = this.query.trim().toLowerCase(Locale.ROOT);
        List<Line> lines = new ArrayList<>();
        if (this.picked != null && this.numbers) {
            for (MobStats.Stat stat : MobStats.Stat.values()) {
                lines.add(new Line(NUMBER, null, null, List.of(), stat));
            }
            return lines;
        }
        if (this.picked != null && wanted.isEmpty()) {
            lines.add(new Line(NUMBERS, null, null, List.of(), null));
        }
        List<Row> everyone = new ArrayList<>();
        Line all = new Line(ALL, null, null, everyone, null);
        lines.add(all);
        if (this.picked != null && matches(this.player, wanted)) {
            lines.add(Line.of(this.player));
            everyone.add(this.player);
        }
        Map<String, List<Row>> groups = new LinkedHashMap<>();
        for (Row row : this.creatures) {
            if (matches(row, wanted)) {
                groups.computeIfAbsent(row.group(), key -> new ArrayList<>()).add(row);
            }
        }
        List<String> order = new ArrayList<>(groups.keySet());
        order.sort(Comparator.comparing((String group) -> !group.equals(VANILLA))
                .thenComparing(group -> groupName(group).getString().toLowerCase(Locale.ROOT)));
        for (String group : order) {
            List<Row> rows = groups.get(group);
            everyone.addAll(rows);
            lines.add(new Line(GROUP, null, group, rows, null));
            if (!wanted.isEmpty() || !FOLDED.contains(group)) {
                for (Row row : rows) {
                    lines.add(Line.of(row));
                }
            }
        }
        if (everyone.isEmpty()) {
            lines.remove(all);
        }
        return lines;
    }

    void page(int action) {
        PacketDistributor.sendToServer(new MobPageActionPayload(action));
    }

    static Component label(Standing standing) {
        return Component.translatable(PREFIX + "mobs." + standing.name().toLowerCase(Locale.ROOT));
    }

    static int dim(int rgb) {
        int r = (rgb >> 16 & 0xFF) * 3 / 5;
        int g = (rgb >> 8 & 0xFF) * 3 / 5;
        int b = (rgb & 0xFF) * 3 / 5;
        return r << 16 | g << 8 | b;
    }
}
