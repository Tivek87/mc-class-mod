package nl.tivek.multiversepowers.faction.mob;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.WorldSettings;

// Each kind of creature's own numbers in this world, set on the Mobs page: a world setting beside the relations, one
// line per number that differs from the game's own, "<creature>|<number>=<value>". Its attributes take them as a
// modifier of this mod's own, put on as the creature comes into the world and again whenever they change.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class MobStats {
    private static final ResourceLocation MODIFIER = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "mob_stats");
    private static final Pattern LINE = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+\\|[a-zA-Z]+=-?[0-9.]+");
    private static final int MOST = 20000;

    // How a number is shown and what it does: times the creature's own, added to it, a share, on or off, or a rate.
    public enum Kind {
        TIMES, PLUS, PERCENT, SWITCH, PER_SECOND
    }

    public enum Stat {
        HEALTH("health", Attributes.MAX_HEALTH, Kind.TIMES, 1.0, 0.05, 50.0, 0.1),
        DAMAGE("damage", Attributes.ATTACK_DAMAGE, Kind.TIMES, 1.0, 0.0, 50.0, 0.1),
        SPEED("speed", Attributes.MOVEMENT_SPEED, Kind.TIMES, 1.0, 0.0, 5.0, 0.1),
        FOLLOW("follow", Attributes.FOLLOW_RANGE, Kind.TIMES, 1.0, 0.0, 8.0, 0.1),
        ARMOR("armor", Attributes.ARMOR, Kind.PLUS, 0.0, 0.0, 30.0, 1.0),
        TOUGHNESS("toughness", Attributes.ARMOR_TOUGHNESS, Kind.PLUS, 0.0, 0.0, 20.0, 1.0),
        STEADY("steady", Attributes.KNOCKBACK_RESISTANCE, Kind.PLUS, 0.0, 0.0, 1.0, 0.1),
        PUSH("push", Attributes.ATTACK_KNOCKBACK, Kind.PLUS, 0.0, 0.0, 5.0, 0.5),
        SIZE("size", Attributes.SCALE, Kind.TIMES, 1.0, 0.1, 8.0, 0.1),
        JUMP("jump", Attributes.JUMP_STRENGTH, Kind.TIMES, 1.0, 0.0, 5.0, 0.1),
        GRAVITY("gravity", Attributes.GRAVITY, Kind.TIMES, 1.0, 0.0, 5.0, 0.1),
        REGEN("regen", null, Kind.PER_SECOND, 0.0, 0.0, 20.0, 0.5),
        XP("xp", null, Kind.TIMES, 1.0, 0.0, 20.0, 0.5),
        DROPS("drops", null, Kind.SWITCH, 1.0, 0.0, 1.0, 1.0),
        SPAWN_CHANCE("spawnChance", null, Kind.PERCENT, 100.0, 0.0, 100.0, 5.0);

        public final String key;
        @Nullable
        public final Holder<Attribute> attribute;
        public final Kind kind;
        public final double normal;
        public final double least;
        public final double most;
        public final double step;

        Stat(String key, @Nullable Holder<Attribute> attribute, Kind kind, double normal, double least, double most,
                double step) {
            this.key = key;
            this.attribute = attribute;
            this.kind = kind;
            this.normal = normal;
            this.least = least;
            this.most = most;
            this.step = step;
        }

        @Nullable
        public static Stat of(String key) {
            for (Stat stat : values()) {
                if (stat.key.equals(key)) {
                    return stat;
                }
            }
            return null;
        }

        public double clamp(double value) {
            return Mth.clamp(Math.round(value / this.step) * this.step, this.least, this.most);
        }
    }

    @Nullable
    private static List<? extends String> parsedFrom;
    private static Map<EntityType<?>, EnumMap<Stat, Double>> values = Map.of();

    private MobStats() {
    }

    static boolean valid(Object line) {
        return line instanceof String text && text.length() <= 300 && LINE.matcher(text).matches();
    }

    public static double value(EntityType<?> type, Stat stat) {
        EnumMap<Stat, Double> own = values().get(type);
        Double value = own == null ? null : own.get(stat);
        return value == null ? stat.normal : value;
    }

    // How many of `type`'s numbers this world sets apart from the game's own.
    public static int changed(EntityType<?> type) {
        EnumMap<Stat, Double> own = values().get(type);
        return own == null ? 0 : own.size();
    }

    private static synchronized Map<EntityType<?>, EnumMap<Stat, Double>> values() {
        if (!MobRules.SPEC.isLoaded()) {
            return Map.of();
        }
        List<? extends String> lines = MobRules.STATS.get();
        if (lines != parsedFrom) {
            parsedFrom = lines;
            Map<EntityType<?>, EnumMap<Stat, Double>> parsed = new HashMap<>();
            for (String line : lines) {
                int bar = line.indexOf('|');
                int is = line.indexOf('=');
                if (bar < 0 || is < bar) {
                    continue;
                }
                EntityType<?> type = MobRules.type(line.substring(0, bar));
                Stat stat = Stat.of(line.substring(bar + 1, is));
                if (type == null || stat == null) {
                    continue;
                }
                try {
                    parsed.computeIfAbsent(type, key -> new EnumMap<>(Stat.class)).put(stat,
                            stat.clamp(Double.parseDouble(line.substring(is + 1))));
                } catch (NumberFormatException ignored) {
                }
            }
            values = parsed;
        }
        return values;
    }

    // A host's change from the Mobs page; the game's own value takes the line away, and the number * puts every number
    // of the creature back.
    public static void edit(ServerPlayer player, String creatureId, String statKey, double value) {
        if (!WorldSettings.mayEdit(player)) {
            player.displayClientMessage(Component.translatable("config." + MultiversePowers.MODID + ".denied"), false);
            return;
        }
        EntityType<?> type = MobRules.type(creatureId);
        Stat stat = Stat.of(statKey);
        boolean all = statKey.equals(MobRules.ANY);
        if (type == null || stat == null && !all || !Double.isFinite(value) || !MobRules.SPEC.isLoaded()) {
            return;
        }
        String prefix = MobRules.id(type) + "|" + (all ? "" : stat.key + "=");
        List<String> lines = new ArrayList<>();
        for (String line : MobRules.STATS.get()) {
            if (!line.startsWith(prefix)) {
                lines.add(line);
            }
        }
        if (!all && stat.clamp(value) != stat.normal) {
            lines.add(prefix + String.format(Locale.ROOT, "%.3f", stat.clamp(value)));
        }
        if (lines.size() > MOST || lines.equals(MobRules.STATS.get())) {
            return;
        }
        MobRules.remember(player, prefix);
        MobRules.STATS.set(lines);
        MobRules.saved(player.server);
        applyAll(player.server, type);
    }

    // Puts the numbers of `only` (every kind for null) on every creature in the worlds now.
    public static void applyAll(MinecraftServer server, @Nullable EntityType<?> only) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && !(living instanceof Player)
                        && (only == null || living.getType() == only)) {
                    apply(living);
                }
            }
        }
    }

    // Its kind's numbers on its attributes; a creature at full health stays so, one hurt keeps its share of it.
    private static void apply(LivingEntity living) {
        float before = living.getMaxHealth();
        for (Stat stat : Stat.values()) {
            AttributeInstance instance = stat.attribute == null ? null : living.getAttribute(stat.attribute);
            if (instance == null) {
                continue;
            }
            double value = value(living.getType(), stat);
            if (value == stat.normal) {
                instance.removeModifier(MODIFIER);
            } else {
                instance.addOrReplacePermanentModifier(new AttributeModifier(MODIFIER,
                        stat.kind == Kind.TIMES ? value - 1.0 : value,
                        stat.kind == Kind.TIMES ? AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                                : AttributeModifier.Operation.ADD_VALUE));
            }
        }
        float after = living.getMaxHealth();
        if (before > 0.0F && after != before && living.isAlive()) {
            living.setHealth(living.getHealth() * after / before);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living
                && !(living instanceof Player)) {
            apply(living);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity living = event.getEntity();
        if (!(living instanceof Player) && value(living.getType(), Stat.DROPS) < 0.5) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        LivingEntity living = event.getEntity();
        double times = living instanceof Player ? 1.0 : value(living.getType(), Stat.XP);
        if (times != 1.0) {
            event.setDroppedExperience((int) Math.round(event.getDroppedExperience() * times));
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity().tickCount % 20 != 0 || !(event.getEntity() instanceof LivingEntity living)
                || living instanceof Player || living.level().isClientSide() || !living.isAlive()) {
            return;
        }
        double regen = value(living.getType(), Stat.REGEN);
        if (regen > 0.0 && living.getHealth() < living.getMaxHealth()) {
            living.heal((float) regen);
        }
    }
}
