package nl.tivek.welcomescreen.character.lantern;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * The creatures of the dark fear the ring's light. When it bursts out around its bearer (the moment the ring slides
 * onto his finger), every one of them close by is thrown back and runs from him for a while: it forgets whoever it was
 * after and cannot pick anyone new, until the fear wears off.
 *
 * <p>Which creatures count is a tag of their own, {@code welcomescreen:fears_the_light}: the undead (zombies,
 * skeletons and the like), the warden, vexes and endermen. A data pack can change it.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID)
public final class Fear {
    /** The creatures that run from the ring's light. */
    public static final TagKey<EntityType<?>> FEARS_THE_LIGHT = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "fears_the_light"));
    // How fast a frightened creature runs, as a part of its own speed, and how far it looks for somewhere to run to.
    private static final double RUN = 1.45;
    private static final int RUN_BLOCKS = 16;
    private static final int RUN_HEIGHT = 7;
    // How often a creature that runs on its own brain (a warden) is sent somewhere new, in ticks.
    private static final int RETHINK = 15;

    // Every frightened creature, by its id: what it runs from and until when.
    private static final Map<UUID, Afraid> AFRAID = new HashMap<>();
    // The creatures that were given the goals below already; a creature keeps them for as long as it lives.
    private static final Set<Integer> TAUGHT = new HashSet<>();

    private Fear() {
    }

    private record Afraid(Mob mob, Vec3 from, long until) {
    }

    /**
     * The light bursts out around {@code owner}: every creature of the dark within {@code radius} blocks is thrown
     * back, harder the closer it stood, and runs from him for {@code ticks}.
     *
     * @return how many creatures it frightened
     */
    static int strike(ServerPlayer owner, ServerLevel level, double radius, int ticks, double push) {
        Vec3 at = owner.position();
        AABB area = owner.getBoundingBox().inflate(radius, radius * 0.5, radius);
        int count = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area,
                mob -> mob.isAlive() && mob.getType().is(FEARS_THE_LIGHT))) {
            Vec3 away = new Vec3(mob.getX() - at.x, 0.0, mob.getZ() - at.z);
            double distance = away.length();
            if (distance > radius) {
                continue;
            }
            Vec3 way = distance < 1.0E-3 ? owner.getLookAngle().multiply(1.0, 0.0, 1.0).normalize()
                    : away.scale(1.0 / distance);
            double strength = push * (1.0 - 0.6 * distance / radius)
                    * (1.0 - Mth.clamp(mob.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0));
            mob.setDeltaMovement(mob.getDeltaMovement().add(way.x * strength, 0.25 + 0.2 * strength,
                    way.z * strength));
            mob.hasImpulse = true;
            frighten(mob, at, level.getGameTime() + ticks);
            count++;
        }
        return count;
    }

    /** This creature runs from {@code from} until the level's clock reaches {@code until}. */
    static void frighten(Mob mob, Vec3 from, long until) {
        AFRAID.put(mob.getUUID(), new Afraid(mob, from, until));
        forget(mob);
        // A creature that thinks with goals is taught to run, and to leave everyone alone, ahead of anything else.
        if (mob instanceof PathfinderMob runner && TAUGHT.add(mob.getId())) {
            runner.goalSelector.addGoal(-1, new Run(runner));
            runner.targetSelector.addGoal(-1, new LeaveAlone(runner));
        }
    }

    /** True while this creature runs from the ring's light. */
    static boolean afraid(Mob mob) {
        Afraid afraid = AFRAID.get(mob.getUUID());
        return afraid != null && afraid.mob() == mob && mob.level().getGameTime() < afraid.until();
    }

    @Nullable
    private static Vec3 from(Mob mob) {
        Afraid afraid = AFRAID.get(mob.getUUID());
        return afraid == null ? null : afraid.from();
    }

    /** It lets go of whoever it was after, and stops being angry at anyone. */
    private static void forget(Mob mob) {
        mob.setTarget(null);
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        if (mob instanceof Warden warden) {
            warden.getEntityAngryAt().ifPresent(warden::clearAnger);
        }
    }

    /** Somewhere away from {@code from}, for this creature to run to; null when there is nowhere. */
    @Nullable
    private static Vec3 away(PathfinderMob mob, Vec3 from) {
        return DefaultRandomPos.getPosAway(mob, RUN_BLOCKS, RUN_HEIGHT, from);
    }

    /** The server stops: nobody is afraid any more. */
    static void clear() {
        AFRAID.clear();
        TAUGHT.clear();
    }

    /**
     * Every tick: fear wears off, and creatures that think with a brain instead of goals (the warden) are told again
     * to forget whoever they were after and to run.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (AFRAID.isEmpty()) {
            return;
        }
        Iterator<Afraid> all = AFRAID.values().iterator();
        while (all.hasNext()) {
            Afraid afraid = all.next();
            Mob mob = afraid.mob();
            if (!mob.isAlive() || mob.isRemoved() || mob.level().getGameTime() >= afraid.until()) {
                all.remove();
                continue;
            }
            forget(mob);
            if (!mob.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET) || mob.tickCount % RETHINK == 0) {
                if (mob instanceof PathfinderMob runner
                        && runner.getBrain().checkMemory(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED)) {
                    Vec3 to = away(runner, afraid.from());
                    if (to != null) {
                        runner.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                                new WalkTarget(to, (float) RUN, 0));
                    }
                }
            }
        }
    }

    /** A creature that thinks with goals: while afraid, it runs, ahead of anything else it would do. */
    private static final class Run extends Goal {
        private final PathfinderMob mob;

        Run(PathfinderMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return afraid(this.mob);
        }

        @Override
        public void start() {
            this.run();
        }

        @Override
        public void tick() {
            if (this.mob.getNavigation().isDone() || this.mob.tickCount % 20 == 0) {
                this.run();
            }
        }

        @Override
        public void stop() {
            this.mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private void run() {
            Vec3 from = from(this.mob);
            Vec3 to = from == null ? null : away(this.mob, from);
            if (to != null) {
                this.mob.getNavigation().moveTo(to.x, to.y, to.z, RUN);
            }
        }
    }

    /** A creature that thinks with goals: while afraid, it picks nobody to go after. */
    private static final class LeaveAlone extends Goal {
        private final PathfinderMob mob;

        LeaveAlone(PathfinderMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return afraid(this.mob);
        }

        @Override
        public void start() {
            this.mob.setTarget(null);
        }

        @Override
        public void tick() {
            this.mob.setTarget(null);
        }
    }
}
