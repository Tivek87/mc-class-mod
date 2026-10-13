package nl.tivek.multiversepowers.faction.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.faction.Standing;

// The world's factions: a world setting of its own file, the three lists `FactionTable` reads and the numbers of
// reputation. Every kind of creature is in one faction: the one its line names, else the one for its sort (a monster
// of a mod no list knows joins the Creeping Dark), else none. Both sides read it: the server sends every player its
// copy.
public final class FactionRules {
    public static final String FILE = ModConfigs.file("factions");
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FACTIONS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MEMBERS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RELATIONS;
    public static final ModConfigSpec.IntValue REPUTATION;
    public static final ModConfigSpec.IntValue KILL_LOSS;
    public static final ModConfigSpec.IntValue HIT_LOSS;
    public static final ModConfigSpec.IntValue ENEMY_GAIN;
    public static final ModConfigSpec.IntValue DEED_GAIN;
    public static final ModConfigSpec.IntValue ALLY_SHARE;
    public static final ModConfigSpec.IntValue DAILY_DRIFT;
    public static final ModConfigSpec.IntValue HELP_RANGE;

    // The numbers of reputation, as the Factions page and `/faction rule` name them.
    public enum Rule {
        REPUTATION("reputation", 1, 1),
        KILL_LOSS("killLoss", 4, 50),
        HIT_LOSS("hitLoss", 1, 20),
        ENEMY_GAIN("enemyGain", 2, 50),
        DEED_GAIN("deedGain", 1, 20),
        ALLY_SHARE("allyShare", 50, 100),
        DAILY_DRIFT("dailyDrift", 2, 50),
        HELP_RANGE("helpRange", 16, 48);

        public final String key;
        public final int normal;
        public final int most;

        Rule(String key, int normal, int most) {
            this.key = key;
            this.normal = normal;
            this.most = most;
        }

        // On or off rather than a number.
        public boolean toggle() {
            return this.most == 1;
        }

        public ModConfigSpec.IntValue value() {
            return switch (this) {
                case REPUTATION -> FactionRules.REPUTATION;
                case KILL_LOSS -> FactionRules.KILL_LOSS;
                case HIT_LOSS -> FactionRules.HIT_LOSS;
                case ENEMY_GAIN -> FactionRules.ENEMY_GAIN;
                case DEED_GAIN -> FactionRules.DEED_GAIN;
                case ALLY_SHARE -> FactionRules.ALLY_SHARE;
                case DAILY_DRIFT -> FactionRules.DAILY_DRIFT;
                case HELP_RANGE -> FactionRules.HELP_RANGE;
            };
        }

        public int get() {
            return SPEC.isLoaded() ? this.value().get() : this.normal;
        }

        @Nullable
        public static Rule of(String key) {
            for (Rule rule : values()) {
                if (rule.key.equals(key)) {
                    return rule;
                }
            }
            return null;
        }
    }

    @Nullable
    private static List<? extends String> factionsFrom;
    @Nullable
    private static List<? extends String> membersFrom;
    @Nullable
    private static List<? extends String> relationsFrom;
    private static FactionTable table = FactionTable.EMPTY;
    private static final Map<EntityType<?>, Optional<Faction>> OF = new HashMap<>();
    @Nullable
    private static List<EntityType<?>> kinds;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(ModConfigs.WIP, "The factions of this world: who belongs together, who they like and what a"
                + " player's reputation with them does.",
                "World settings: every world keeps its own copy of this file, in <world>/serverconfig/welcomescreen/.",
                "Easiest changed in the game: settings window > Factions, or the command /faction.").push("factions");
        FACTIONS = builder.comment("One line per thing a faction has: \"<faction>|<key>=<value>\". Keys: name,"
                + " description, lore, color (six hex digits), attitude (how it starts toward players: hostile,"
                + " neutral or friendly; neutral creatures only answer a provocation, as a wolf or an enderman does),"
                + " reputation (true or false: whether a player's deeds change how it stands to them), hostileAt and"
                + " friendlyAt (the reputation, from -100 to 100, at which it turns hostile or friendly), help (true"
                + " or false: its creatures come to the aid of members and friends) and open (true or false: players"
                + " may join without an invite).")
                .defineListAllowEmpty("factions", FactionDefaults.factions(),
                        () -> "ironfang|name=The Ironfang Syndicate", FactionTable::validFaction);
        MEMBERS = builder.comment("Which faction each kind of creature is in: \"<creature>=<faction>\", nothing after"
                + " the = for none. A creature with no line joins the faction of its sort (creeping, wildkin,"
                + " tidefolk).")
                .defineListAllowEmpty("members", FactionDefaults.members(), () -> "minecraft:zombie=legion",
                        FactionTable::validMember);
        RELATIONS = builder.comment("How one faction stands toward another:"
                + " \"<faction>><other>=friendly|neutral|hostile\". Friendly never attacks, neutral only hits back,"
                + " hostile attacks on sight. A faction toward itself is how its members stand to each other. With no"
                + " line each creature does as its nature has it.")
                .defineListAllowEmpty("relations", FactionDefaults.relations(), () -> "covenant>hearthbound=hostile",
                        FactionTable::validRelation);
        builder.pop();
        builder.comment("How players earn and lose reputation with a faction.").push("reputation");
        REPUTATION = builder.comment("Whether reputation counts at all (1) or every faction stays as it starts (0).")
                .defineInRange("reputation", Rule.REPUTATION.normal, 0, Rule.REPUTATION.most);
        KILL_LOSS = builder.comment("Reputation lost with a faction for killing one of its members who had not"
                + " attacked you.").defineInRange("killLoss", Rule.KILL_LOSS.normal, 0, Rule.KILL_LOSS.most);
        HIT_LOSS = builder.comment("Reputation lost for striking a member who had not attacked you.")
                .defineInRange("hitLoss", Rule.HIT_LOSS.normal, 0, Rule.HIT_LOSS.most);
        ENEMY_GAIN = builder.comment("Reputation gained with a faction for killing something that hunts its members.")
                .defineInRange("enemyGain", Rule.ENEMY_GAIN.normal, 0, Rule.ENEMY_GAIN.most);
        DEED_GAIN = builder.comment("Reputation gained for a good turn: a trade, breeding its animals (taming gives"
                + " twice this).").defineInRange("deedGain", Rule.DEED_GAIN.normal, 0, Rule.DEED_GAIN.most);
        ALLY_SHARE = builder.comment("The share (percent) of a loss that factions friendly to the wronged one feel"
                + " too.").defineInRange("allyShare", Rule.ALLY_SHARE.normal, 0, Rule.ALLY_SHARE.most);
        DAILY_DRIFT = builder.comment("How far reputation drifts back to where it started, each day in the game.")
                .defineInRange("dailyDrift", Rule.DAILY_DRIFT.normal, 0, Rule.DAILY_DRIFT.most);
        HELP_RANGE = builder.comment("From how many blocks away a faction's creatures come to help a member or a"
                + " friend under attack; 0 for never.")
                .defineInRange("helpRange", Rule.HELP_RANGE.normal, 0, Rule.HELP_RANGE.most);
        builder.pop();
        SPEC = builder.build();
    }

    private FactionRules() {
    }

    public static synchronized FactionTable table() {
        if (!SPEC.isLoaded()) {
            return FactionTable.EMPTY;
        }
        List<? extends String> factions = FACTIONS.get();
        List<? extends String> members = MEMBERS.get();
        List<? extends String> relations = RELATIONS.get();
        if (factions != factionsFrom || members != membersFrom || relations != relationsFrom) {
            factionsFrom = factions;
            membersFrom = members;
            relationsFrom = relations;
            table = FactionTable.parse(factions, members, relations);
            OF.clear();
        }
        return table;
    }

    // Changes whenever the table does (a copy from the server came in).
    public static int stamp() {
        return SPEC.isLoaded() ? 31 * (31 * System.identityHashCode(FACTIONS.get())
                + System.identityHashCode(MEMBERS.get())) + System.identityHashCode(RELATIONS.get()) : 0;
    }

    @Nullable
    public static Faction get(@Nullable String id) {
        return table().get(id);
    }

    public static Collection<Faction> all() {
        return table().all();
    }

    @Nullable
    public static Standing relation(String from, String to) {
        return table().relation(from, to);
    }

    public static boolean reputation() {
        return Rule.REPUTATION.get() > 0;
    }

    // The faction a kind of creature is in, or null.
    @Nullable
    public static synchronized Faction of(EntityType<?> type) {
        FactionTable now = table();
        Optional<Faction> known = OF.get(type);
        if (known == null) {
            String member = now.member(id(type));
            known = Optional.ofNullable(now.get(member != null ? member : sort(type)));
            OF.put(type, known);
        }
        return known.orElse(null);
    }

    // Whether a line of its own puts `type` where it is, rather than its sort.
    public static boolean listed(EntityType<?> type) {
        return table().member(id(type)) != null;
    }

    @Nullable
    private static String sort(EntityType<?> type) {
        MobCategory category = type.getCategory();
        if (category == MobCategory.MONSTER) {
            return FactionDefaults.MONSTERS;
        }
        if (category == MobCategory.CREATURE || category == MobCategory.AMBIENT) {
            return FactionDefaults.WILDLIFE;
        }
        return category == MobCategory.MISC ? null : FactionDefaults.SEA;
    }

    public static String id(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    @Nullable
    public static EntityType<?> type(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(key).orElse(null);
    }

    // Every kind of creature a faction can hold: what lives, but for players and what only stands there.
    public static synchronized List<EntityType<?>> kinds() {
        if (kinds == null) {
            List<EntityType<?>> found = new ArrayList<>();
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                if (type != EntityType.PLAYER && type != EntityType.ARMOR_STAND && living(type)) {
                    found.add(type);
                }
            }
            kinds = List.copyOf(found);
        }
        return kinds;
    }

    @SuppressWarnings("unchecked")
    private static boolean living(EntityType<?> type) {
        return DefaultAttributes.hasSupplier((EntityType<? extends LivingEntity>) type);
    }

    // The kinds in `faction` now, by a line of their own or by their sort.
    public static List<EntityType<?>> members(String faction) {
        List<EntityType<?>> members = new ArrayList<>();
        for (EntityType<?> type : kinds()) {
            Faction own = of(type);
            if (own != null && own.id().equals(faction)) {
                members.add(type);
            }
        }
        return members;
    }
}
