package nl.tivek.multiversepowers.faction.mob.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.Tags;
import nl.tivek.multiversepowers.faction.mob.MobStats;

// What the Mobs page tells of a creature, in sections: its body, how it fights, how it moves and its place in the
// world, each number as this world has it (its own times or plus the page's) with the game's own beside it while it
// differs. Read from one of its kind made for the preview; numbers a kind has no attribute for are left out.
final class MobFacts {
    static final String PREFIX = MobRulesState.PREFIX + "mobs.";
    // A creature walking at its own pace gains its speed squared each tick and keeps 0.546 of what it had (ground
    // friction and drag): blocks a second, per speed squared.
    private static final double PACE = 20.0 / (1.0 - 0.546);

    enum Section {
        BODY(MobStats.Stat.HEALTH, MobStats.Stat.ARMOR, MobStats.Stat.TOUGHNESS, MobStats.Stat.SIZE,
                MobStats.Stat.REGEN),
        FIGHT(MobStats.Stat.DAMAGE, MobStats.Stat.PUSH, MobStats.Stat.STEADY, MobStats.Stat.FOLLOW),
        MOVING(MobStats.Stat.SPEED, MobStats.Stat.JUMP, MobStats.Stat.GRAVITY),
        WORLD(MobStats.Stat.SPAWN_CHANCE, MobStats.Stat.DROPS, MobStats.Stat.XP);

        final MobStats.Stat[] stats;

        Section(MobStats.Stat... stats) {
            this.stats = stats;
        }

        Component title() {
            return Component.translatable(PREFIX + "section." + this.name().toLowerCase(Locale.ROOT));
        }
    }

    // One line: its name, what it is now, whether this world changed it, a tip, and the number it is changed by.
    record Fact(Component name, Component value, boolean changed, @Nullable Component tip,
            @Nullable MobStats.Stat stat) {
    }

    private MobFacts() {
    }

    // Whether `living` has what the number changes, so the page offers it.
    static boolean applies(@Nullable LivingEntity living, MobStats.Stat stat) {
        if (stat.attribute == null || living == null) {
            return true;
        }
        return living.getAttributes().hasAttribute(stat.attribute);
    }

    static List<Fact> of(Section section, EntityType<?> type, @Nullable LivingEntity living, double[] stats,
            boolean spawns) {
        List<Fact> facts = new ArrayList<>();
        switch (section) {
            case BODY -> {
                double health = base(living, Attributes.MAX_HEALTH);
                if (!Double.isNaN(health)) {
                    double now = changed(health, MobStats.Stat.HEALTH, stats);
                    facts.add(stat(MobStats.Stat.HEALTH, hearts(now), health, now, hearts(health)));
                }
                addPlain(facts, living, Attributes.ARMOR, MobStats.Stat.ARMOR, stats);
                addPlain(facts, living, Attributes.ARMOR_TOUGHNESS, MobStats.Stat.TOUGHNESS, stats);
                double size = stats[MobStats.Stat.SIZE.ordinal()];
                float wide = type.getDimensions().width();
                float tall = type.getDimensions().height();
                facts.add(stat(MobStats.Stat.SIZE, Component.translatable(PREFIX + "fact.size", number(wide * size),
                        number(tall * size)), 1.0, size, Component.translatable(PREFIX + "fact.size",
                        number(wide), number(tall))));
                double regen = stats[MobStats.Stat.REGEN.ordinal()];
                if (regen > 0.0) {
                    facts.add(stat(MobStats.Stat.REGEN, Component.translatable(PREFIX + "per_second",
                            number(regen)), 0.0, regen, Component.literal("0")));
                }
            }
            case FIGHT -> {
                double damage = base(living, Attributes.ATTACK_DAMAGE);
                if (Double.isNaN(damage)) {
                    facts.add(new Fact(name(MobStats.Stat.DAMAGE), Component.translatable(PREFIX + "fact.no_melee"),
                            false, Component.translatable(PREFIX + "fact.no_melee.tip"), null));
                } else {
                    double now = changed(damage, MobStats.Stat.DAMAGE, stats);
                    facts.add(stat(MobStats.Stat.DAMAGE, hearts(now), damage, now, hearts(damage)));
                }
                addPlain(facts, living, Attributes.ATTACK_KNOCKBACK, MobStats.Stat.PUSH, stats);
                double steady = base(living, Attributes.KNOCKBACK_RESISTANCE);
                if (!Double.isNaN(steady)) {
                    double now = Math.min(1.0, changed(steady, MobStats.Stat.STEADY, stats));
                    facts.add(stat(MobStats.Stat.STEADY, percent(now), steady, now, percent(steady)));
                }
                double follow = base(living, Attributes.FOLLOW_RANGE);
                if (!Double.isNaN(follow)) {
                    double now = changed(follow, MobStats.Stat.FOLLOW, stats);
                    facts.add(stat(MobStats.Stat.FOLLOW, blocks(now), follow, now, blocks(follow)));
                }
            }
            case MOVING -> {
                double speed = base(living, Attributes.MOVEMENT_SPEED);
                if (!Double.isNaN(speed)) {
                    double now = changed(speed, MobStats.Stat.SPEED, stats);
                    facts.add(stat(MobStats.Stat.SPEED, pace(now), speed, now, pace(speed)));
                }
                double flying = base(living, Attributes.FLYING_SPEED);
                if (!Double.isNaN(flying) && type != EntityType.PLAYER) {
                    facts.add(new Fact(Component.translatable(PREFIX + "fact.fly"),
                            Component.literal(number(flying)), false,
                            Component.translatable(PREFIX + "fact.fly.tip"), null));
                }
                double jump = base(living, Attributes.JUMP_STRENGTH);
                if (!Double.isNaN(jump)) {
                    double now = changed(jump, MobStats.Stat.JUMP, stats);
                    facts.add(stat(MobStats.Stat.JUMP, height(now), jump, now, height(jump)));
                }
                double step = base(living, Attributes.STEP_HEIGHT);
                if (!Double.isNaN(step)) {
                    facts.add(new Fact(Component.translatable(PREFIX + "fact.step"), blocks(step), false,
                            Component.translatable(PREFIX + "fact.step.tip"), null));
                }
                double gravity = stats[MobStats.Stat.GRAVITY.ordinal()];
                if (gravity != MobStats.Stat.GRAVITY.normal) {
                    facts.add(stat(MobStats.Stat.GRAVITY, Component.literal("×" + number(gravity)), 1.0, gravity,
                            Component.literal("×1")));
                }
            }
            case WORLD -> {
                facts.add(new Fact(Component.translatable(PREFIX + "fact.kind"),
                        Component.translatableWithFallback(PREFIX + "kind." + type.getCategory().getName(),
                                type.getCategory().getName()), false, null, null));
                Component traits = traits(type, living);
                if (traits != null) {
                    facts.add(new Fact(Component.translatable(PREFIX + "fact.traits"), traits, false, null, null));
                }
                facts.add(new Fact(Component.translatable(PREFIX + "fact.spawns"),
                        Component.translatable(PREFIX + (spawns ? "on" : "off")), !spawns,
                        Component.translatable(PREFIX + "fact.spawns.tip"), null));
                for (MobStats.Stat stat : new MobStats.Stat[] { MobStats.Stat.SPAWN_CHANCE, MobStats.Stat.DROPS,
                        MobStats.Stat.XP }) {
                    double now = stats[stat.ordinal()];
                    facts.add(stat(stat, MobRulesState.value(stat, now), stat.normal, now,
                            MobRulesState.value(stat, stat.normal)));
                }
            }
        }
        return facts;
    }

    // A plain number, left out while the creature has none of it and the world adds none.
    private static void addPlain(List<Fact> facts, @Nullable LivingEntity living, Holder<Attribute> attribute,
            MobStats.Stat stat, double[] stats) {
        double base = base(living, attribute);
        if (Double.isNaN(base)) {
            return;
        }
        double now = changed(base, stat, stats);
        if (base > 0.0 || now != base) {
            facts.add(stat(stat, Component.literal(number(now)), base, now, Component.literal(number(base))));
        }
    }

    private static Fact stat(MobStats.Stat stat, Component value, double base, double now, Component own) {
        boolean changed = Math.abs(now - base) > 1.0E-6;
        MutableComponent tip = Component.translatable(PREFIX + "stat." + stat.key + ".desc");
        if (changed) {
            tip.append("\n").append(Component.translatable(PREFIX + "fact.own", own));
        }
        return new Fact(name(stat), value, changed, tip, stat);
    }

    private static Component name(MobStats.Stat stat) {
        return Component.translatable(PREFIX + "stat." + stat.key);
    }

    // The attribute's base value as the creature is made, NaN if it has none.
    static double base(@Nullable LivingEntity living, Holder<Attribute> attribute) {
        if (living == null || !living.getAttributes().hasAttribute(attribute)) {
            return Double.NaN;
        }
        return living.getAttributeBaseValue(attribute);
    }

    // `base` with the world's number put on it, as `MobStats` does.
    static double changed(double base, MobStats.Stat stat, double[] stats) {
        double value = stats[stat.ordinal()];
        return stat.kind == MobStats.Kind.TIMES ? base * value : base + value;
    }

    // What the number makes of the creature, for the Numbers tab: its health in hearts, its pace, and so on.
    @Nullable
    static Component result(MobStats.Stat stat, @Nullable LivingEntity living, double[] stats) {
        Holder<Attribute> attribute = stat.attribute;
        if (attribute == null || stat == MobStats.Stat.SIZE || stat == MobStats.Stat.GRAVITY) {
            return null;
        }
        double base = base(living, attribute);
        if (Double.isNaN(base)) {
            return null;
        }
        double now = changed(base, stat, stats);
        return switch (stat) {
            case HEALTH, DAMAGE -> hearts(now);
            case SPEED -> pace(now);
            case FOLLOW -> blocks(now);
            case STEADY -> percent(Math.min(1.0, now));
            case JUMP -> height(now);
            default -> Component.literal(number(now));
        };
    }

    @Nullable
    private static Component traits(EntityType<?> type, @Nullable LivingEntity living) {
        List<String> traits = new ArrayList<>();
        if (type.is(EntityTypeTags.UNDEAD)) {
            traits.add("undead");
        }
        if (type.is(EntityTypeTags.ARTHROPOD)) {
            traits.add("arthropod");
        }
        if (type.is(EntityTypeTags.AQUATIC)) {
            traits.add("aquatic");
        }
        if (type.is(EntityTypeTags.ILLAGER)) {
            traits.add("illager");
        }
        if (type.is(Tags.EntityTypes.BOSSES)) {
            traits.add("boss");
        }
        if (type.fireImmune()) {
            traits.add("fire");
        }
        if (living != null && living.getAttributes().hasAttribute(Attributes.FLYING_SPEED)) {
            traits.add("flies");
        }
        if (traits.isEmpty()) {
            return null;
        }
        MutableComponent text = Component.empty();
        for (int i = 0; i < traits.size(); i++) {
            if (i > 0) {
                text.append(" · ");
            }
            text.append(Component.translatable(PREFIX + "trait." + traits.get(i)));
        }
        return text;
    }

    static Component hearts(double health) {
        return Component.translatable(PREFIX + "fact.hearts", number(health), number(health / 2.0));
    }

    private static Component pace(double speed) {
        return Component.translatable(PREFIX + "fact.pace", number(speed * speed * PACE));
    }

    private static Component blocks(double blocks) {
        return Component.translatable(PREFIX + "fact.blocks", number(blocks));
    }

    private static Component percent(double share) {
        return Component.literal(number(share * 100.0) + "%");
    }

    // How high a jump of this strength rises, stepped as the game steps it.
    private static Component height(double strength) {
        double rise = 0.0;
        double up = strength;
        for (int tick = 0; tick < 200 && up > 0.0; tick++) {
            rise += up;
            up = (up - 0.08) * 0.98;
        }
        return Component.translatable(PREFIX + "fact.blocks", number(rise));
    }

    static String number(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.005) {
            return String.valueOf((long) Math.rint(value));
        }
        String text = String.format(Locale.ROOT, Math.abs(value) < 1.0 ? "%.2f" : "%.1f", value);
        return text.contains(".") ? text.replaceAll("0+$", "").replaceAll("\\.$", "") : text;
    }
}
