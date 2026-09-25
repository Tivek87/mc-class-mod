package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Fear {
    public static final TagKey<EntityType<?>> FEARS_THE_LIGHT = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "fears_the_light"));
    private static final double RUN = 1.45;
    private static final int RUN_BLOCKS = 16;
    private static final int RUN_HEIGHT = 7;
    private static final int RETHINK = 15;

    private static final Map<UUID, Afraid> AFRAID = new HashMap<>();
    private static final Set<Mob> TAUGHT = Collections.newSetFromMap(new WeakHashMap<>());
    private static final int RETRY = 10;

    private Fear() {
    }

    private record Afraid(Mob mob, Vec3 from, long until) {
    }

    public static int strike(ServerPlayer owner, ServerLevel level, double radius, int ticks, double push) {
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

    static void frighten(Mob mob, Vec3 from, long until) {
        AFRAID.put(mob.getUUID(), new Afraid(mob, from, until));
        forget(mob);
        if (mob instanceof PathfinderMob runner && TAUGHT.add(mob)) {
            runner.goalSelector.addGoal(-1, new Run(runner));
            runner.targetSelector.addGoal(-1, new LeaveAlone(runner));
        }
    }

    static boolean afraid(Mob mob) {
        Afraid afraid = AFRAID.get(mob.getUUID());
        return afraid != null && afraid.mob() == mob && mob.level().getGameTime() < afraid.until();
    }

    @Nullable
    private static Vec3 from(Mob mob) {
        Afraid afraid = AFRAID.get(mob.getUUID());
        return afraid == null ? null : afraid.from();
    }

    private static void forget(Mob mob) {
        mob.setTarget(null);
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        if (mob instanceof Warden warden) {
            warden.getEntityAngryAt().ifPresent(warden::clearAnger);
        }
    }

    @Nullable
    private static Vec3 away(PathfinderMob mob, Vec3 from) {
        return DefaultRandomPos.getPosAway(mob, RUN_BLOCKS, RUN_HEIGHT, from);
    }

    public static void clear() {
        AFRAID.clear();
        TAUGHT.clear();
    }

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
            // The warden thinks by itself, not with goals, so it must be told to forget every tick.
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

    private static final class Run extends Goal {
        private final PathfinderMob mob;
        private int stuckAt = Integer.MIN_VALUE / 2;

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
            boolean arrived = this.mob.getNavigation().isDone() && this.mob.tickCount - this.stuckAt >= RETRY;
            if (arrived || this.mob.tickCount % 20 == 0) {
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
            if (to == null || !this.mob.getNavigation().moveTo(to.x, to.y, to.z, RUN)) {
                this.stuckAt = this.mob.tickCount;
            }
        }
    }

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
