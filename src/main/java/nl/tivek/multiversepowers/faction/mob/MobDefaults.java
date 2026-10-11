package nl.tivek.multiversepowers.faction.mob;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.faction.Standing;

// How every creature of the game, modded ones too, stands to every other and to the player by the game's own rules,
// read once per server from the target goals of one of each, made but never added to the world: one that looks for a
// kind to attack is hostile to it, one that only hits back neutral, any other friendly. Creatures run by a brain have
// no such goals: a monster among them that is not neutral (as an enderman is) is hostile to players.
public final class MobDefaults {
    @Nullable
    private static final Field TARGET_TYPE = field(NearestAttackableTargetGoal.class, "targetType");
    @Nullable
    private static final Field CONDITIONS = field(NearestAttackableTargetGoal.class, "targetConditions");
    @Nullable
    private static final Field SELECTOR = field(TargetingConditions.class, "selector");

    @Nullable
    private static MobTablePayload table;
    private static final Map<EntityType<?>, Integer> INDEX = new HashMap<>();

    private MobDefaults() {
    }

    public static MobTablePayload table(ServerLevel level) {
        if (table == null) {
            table = build(level);
            INDEX.clear();
            for (int i = 0; i < table.types().size(); i++) {
                INDEX.put(MobRules.type(table.types().get(i)), i);
            }
        }
        return table;
    }

    // The game's own standing of `actor` toward `target`; anything not a creature of the table is friendly.
    public static Standing of(ServerLevel level, EntityType<?> actor, EntityType<?> target) {
        MobTablePayload table = table(level);
        Integer row = INDEX.get(actor);
        Integer column = target == EntityType.PLAYER ? Integer.valueOf(-1) : INDEX.get(target);
        if (row == null || column == null) {
            return Standing.FRIENDLY;
        }
        return Standing.values()[table.standings()[table.at(row, column + 1)]];
    }

    public static void clear() {
        table = null;
        INDEX.clear();
    }

    private static MobTablePayload build(ServerLevel level) {
        List<Mob> mobs = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.PLAYER) {
                continue;
            }
            try {
                Entity made = type.create(level);
                if (made instanceof Mob mob) {
                    mobs.add(mob);
                }
            } catch (RuntimeException | LinkageError e) {
                MultiversePowers.LOGGER.debug("Could not make a {} to read its targets", type, e);
            }
        }
        mobs.sort(Comparator.comparing(mob -> MobRules.id(mob.getType())));
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(FakePlayerFactory.getMinecraft(level));
        targets.addAll(mobs);
        byte[] standings = new byte[mobs.size() * targets.size()];
        List<String> types = new ArrayList<>();
        for (int i = 0; i < mobs.size(); i++) {
            Mob actor = mobs.get(i);
            types.add(MobRules.id(actor.getType()));
            for (int j = 0; j < targets.size(); j++) {
                standings[i * targets.size() + j] = (byte) standing(actor, targets.get(j)).ordinal();
            }
        }
        return new MobTablePayload(types, standings);
    }

    private static Standing standing(Mob actor, LivingEntity target) {
        if (actor.getType() == target.getType()) {
            return Standing.FRIENDLY;
        }
        boolean answers = actor instanceof NeutralMob;
        for (WrappedGoal wrapped : actor.targetSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof NearestAttackableTargetGoal<?> nearest && aims(nearest, target)) {
                return Standing.HOSTILE;
            }
            answers |= wrapped.getGoal() instanceof HurtByTargetGoal;
        }
        if (actor instanceof Enemy && !(actor instanceof NeutralMob) && target instanceof Player) {
            return Standing.HOSTILE;
        }
        return answers ? Standing.NEUTRAL : Standing.FRIENDLY;
    }

    // Whether the goal would pick `target`: its kind, and its own test (one that needs the world it is not in fails).
    @SuppressWarnings("unchecked")
    private static boolean aims(NearestAttackableTargetGoal<?> goal, LivingEntity target) {
        if (TARGET_TYPE == null || CONDITIONS == null || SELECTOR == null) {
            return false;
        }
        try {
            if (!((Class<?>) TARGET_TYPE.get(goal)).isInstance(target)) {
                return false;
            }
            Predicate<LivingEntity> test = (Predicate<LivingEntity>) SELECTOR.get(CONDITIONS.get(goal));
            return test == null || test.test(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    @Nullable
    private static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            MultiversePowers.LOGGER.warn("Mob standings cannot read {}.{}", owner.getSimpleName(), name, e);
            return null;
        }
    }
}
