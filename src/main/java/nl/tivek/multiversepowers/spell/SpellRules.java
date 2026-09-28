package nl.tivek.multiversepowers.spell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.config.Unit;

// The spells' numbers in this world (spells.toml, a world setting like the characters' own).
public final class SpellRules {
    public static final ModConfigSpec SPEC;
    private static final Map<String, List<Rule>> RULES = new LinkedHashMap<>();

    // One number of a spell: its key in the file, its value, how it reads and how far one step moves it.
    public record Rule(String key, ModConfigSpec.ConfigValue<? extends Number> value, Unit unit, double step) {
        double get() {
            return (SPEC.isLoaded() ? this.value.get() : this.value.getDefault()).doubleValue();
        }
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(ModConfigs.WIP, "The spells in this world.",
                "World settings: every world keeps its own copy of this file, in <world>/serverconfig/welcomescreen/.",
                "The same numbers can be changed in the game: Mods > this mod > Config > Server.");
        Sheet fireball = new Sheet(builder, Spell.FIREBALL);
        fireball.number("speed", "How fast the fireball flies, in blocks a tick", 1.25, 0.3, 4.0,
                Unit.BLOCKS_PER_TICK, 0.05);
        fireball.number("blastDamage", "Damage of the blast, in half hearts", 4.0, 0.0, 200.0, Unit.HALF_HEARTS, 1.0);
        fireball.whole("burnTicks", "How long what it hits burns", 60, 0, 400, Unit.TICKS, 5.0);
        fireball.done();
        Sheet lightning = new Sheet(builder, Spell.LIGHTNING_STRIKE);
        lightning.number("rangeBlocks", "How far away the strike can land, in blocks", 40.0, 4.0, 96.0, Unit.BLOCKS,
                1.0);
        lightning.whole("shockedTicks", "How long what it hits stays slowed", 30, 0, 200, Unit.TICKS, 5.0);
        lightning.number("chainReachBlocks", "How far the lightning jumps on to the next creature, in blocks", 6.0, 1.0,
                24.0, Unit.BLOCKS, 0.5);
        lightning.number("chainDamage", "Damage of each jump, in half hearts", 4.0, 0.0, 200.0, Unit.HALF_HEARTS, 1.0);
        lightning.whole("chains", "How many creatures the lightning jumps on to", 3, 0, 12, Unit.COUNT, 1.0);
        lightning.done();
        Sheet poison = new Sheet(builder, Spell.POISON_AREA);
        poison.number("rangeBlocks", "How far away the vial can land, in blocks", 24.0, 4.0, 64.0, Unit.BLOCKS, 1.0);
        poison.done();
        Sheet voidWalk = new Sheet(builder, Spell.VOID_WALK);
        voidWalk.number("ambush", "How much harder the first blow out of the void hits", 1.5, 1.0, 5.0,
                Unit.STRENGTH, 0.1);
        voidWalk.whole("dazedTicks", "How long what that blow hits stays dazed", 40, 0, 200, Unit.TICKS, 5.0);
        voidWalk.number("speedBonus", "How much faster you walk in the void", 0.5, 0.0, 3.0, Unit.STRENGTH, 0.1);
        voidWalk.done();
        Sheet gust = new Sheet(builder, Spell.WIND_GUST);
        gust.number("strength", "How hard the gust throws back what it hits", 2.2, 0.0, 8.0, Unit.STRENGTH, 0.1);
        gust.number("lift", "How high the gust throws what it hits", 0.55, 0.0, 3.0, Unit.STRENGTH, 0.05);
        gust.done();
        SPEC = builder.build();
    }

    private SpellRules() {
    }

    public static List<Rule> rules(Spell spell) {
        return Collections.unmodifiableList(RULES.getOrDefault(spell.getId(), List.of()));
    }

    // A spell's number as this world's power rules make it: damage times the damage multiplier.
    public static double value(Spell spell, String key) {
        for (Rule rule : RULES.getOrDefault(spell.getId(), List.of())) {
            if (rule.key().equals(key)) {
                double value = rule.get();
                return rule.unit() == Unit.HALF_HEARTS ? value * PowerRules.damage() : value;
            }
        }
        throw new IllegalArgumentException(spell.getId() + " has no setting named " + key);
    }

    private static final class Sheet {
        private final ModConfigSpec.Builder builder;
        private final List<Rule> rules = new ArrayList<>();
        private final String spell;

        Sheet(ModConfigSpec.Builder builder, Spell spell) {
            this.builder = builder;
            this.spell = spell.getId();
            builder.push(this.spell);
            this.whole("cooldownTicks", "How long before it can be cast again, in ticks (20 a second)",
                    spell.defaultCooldown(), 0,
                    72000, Unit.TICKS, 5.0);
        }

        void number(String key, String comment, double value, double min, double max, Unit unit, double step) {
            this.rules.add(new Rule(key, this.builder.comment(comment).defineInRange(key, value, min, max), unit,
                    step));
        }

        void whole(String key, String comment, int value, int min, int max, Unit unit, double step) {
            this.rules.add(new Rule(key, this.builder.comment(comment).defineInRange(key, value, min, max), unit,
                    step));
        }

        void done() {
            this.builder.pop();
            RULES.put(this.spell, this.rules);
        }
    }
}
