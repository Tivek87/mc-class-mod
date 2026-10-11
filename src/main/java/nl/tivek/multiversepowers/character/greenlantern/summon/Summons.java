package nl.tivek.multiversepowers.character.greenlantern.summon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.ring.Recharge;
import nl.tivek.multiversepowers.engine.ability.Cooldowns;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.faction.Factions;

// The construct wheel's summon: one to a few hard-light copies of real creatures, picked at random (the stronger, the
// rarer), formed round Green Lantern out of his ring's light. Each is the real creature with its own moves, but fights
// for him: it strikes back at what hurts it, turns on what hurts him or what he strikes, and goes after what is red to
// him; it never hurts him or his own, wrecks nothing, drops nothing and breaks up when its time runs out. Who owns it
// and until when is kept in the creature itself, so it lasts through a save.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Summons {
    private static final String OWNER = MultiversePowers.MODID + ":summon_owner";
    private static final String UNTIL = MultiversePowers.MODID + ":summon_until";
    private static final Cooldowns<String> WAIT = new Cooldowns<>(1);
    private static final String KEY = "summon";
    // The creatures it can make, with the setting that weighs each.
    private static final EntityType<?>[] KINDS = { EntityType.SKELETON, EntityType.ZOMBIFIED_PIGLIN,
            EntityType.CREEPER, EntityType.BLAZE, EntityType.VINDICATOR, EntityType.ENDERMAN, EntityType.IRON_GOLEM };
    private static final String[] WEIGHTS = { "summonSkeleton", "summonPiglin", "summonCreeper", "summonBlaze",
            "summonVindicator", "summonEnderman", "summonGolem" };
    private static final int CHECK = 20;

    private Summons() {
    }

    private static CharacterAbility wheel() {
        return GameCharacter.GREEN_LANTERN.byName("construct_wheel");
    }

    private static double value(String key) {
        return wheel().value(key);
    }

    // Picked in the wheel: pays, then forms the creatures in a ring before him; past the most he may have at once, his
    // oldest break up.
    public static void cast(ServerPlayer player) {
        if (Characters.of(player) != GameCharacter.GREEN_LANTERN || !player.isAlive() || Arrival.busy(player)
                || Recharge.busy(player)) {
            return;
        }
        if (WAIT.left(player, KEY, 0) > 0) {
            PowerRing.tell(player, "summon_wait");
            return;
        }
        if (pick(player.getRandom()) == null || !PowerRing.pay(player, value("summonPowerCost"))) {
            return;
        }
        WAIT.start(player, KEY, 0, (int) Math.round(value("summonCooldown") * 20.0));
        ServerLevel level = player.serverLevel();
        RandomSource random = player.getRandom();
        int most = Math.max(1, (int) Math.round(value("summonMost")));
        int count = 1 + random.nextInt(most);
        long until = level.getGameTime() + Math.round(value("summonMinutes") * 1200.0);
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        for (int i = 0; i < count; i++) {
            EntityType<?> kind = pick(random);
            double angle = yaw + (count == 1 ? 0.0 : (i - (count - 1) * 0.5) * 0.9);
            Vec3 want = player.position().add(-Mth.sin((float) angle) * 2.6, 0.0, Mth.cos((float) angle) * 2.6);
            BlockPos at = ground(level, kind, BlockPos.containing(want));
            Entity made = kind.create(level, null, at == null ? player.blockPosition() : at, MobSpawnType.MOB_SUMMONED,
                    false, false);
            if (!(made instanceof Mob mob)) {
                continue;
            }
            mob.getPersistentData().putUUID(OWNER, player.getUUID());
            mob.getPersistentData().putLong(UNTIL, until);
            mob.setPersistenceRequired();
            mob.setYRot(player.getYRot());
            mob.setYHeadRot(player.getYRot());
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                mob.setDropChance(slot, 0.0F);
            }
            level.addFreshEntityWithPassengers(mob);
            Vec3 middle = mob.getBoundingBox().getCenter();
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), middle, 24, mob.getBbWidth() * 0.5,
                    0.04);
            ParticleFx.cloud(level, ParticleTypes.END_ROD, middle, 8, mob.getBbWidth() * 0.4, 0.05);
        }
        List<Mob> his = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Mob mob && mob.isAlive() && player.getUUID().equals(ownerId(mob))) {
                his.add(mob);
            }
        }
        his.sort(Comparator.comparingLong(mob -> mob.getPersistentData().getLong(UNTIL)));
        for (int k = 0; k < his.size() - most; k++) {
            breakUp(level, his.get(k));
            his.get(k).discard();
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.9F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    @Nullable
    private static EntityType<?> pick(RandomSource random) {
        double total = 0.0;
        for (String weight : WEIGHTS) {
            total += Math.max(0.0, value(weight));
        }
        if (total <= 0.0) {
            return null;
        }
        double roll = random.nextDouble() * total;
        for (int k = 0; k < KINDS.length; k++) {
            roll -= Math.max(0.0, value(WEIGHTS[k]));
            if (roll < 0.0) {
                return KINDS[k];
            }
        }
        return KINDS[KINDS.length - 1];
    }

    // A free spot standing on something near `around`, a few blocks up or down.
    @Nullable
    private static BlockPos ground(ServerLevel level, EntityType<?> kind, BlockPos around) {
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos at = around.above(dy);
            if (!level.isLoaded(at)) {
                return null;
            }
            if (level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)
                    && level.noCollision(kind.getSpawnAABB(at.getX() + 0.5, at.getY(), at.getZ() + 0.5))) {
                return at;
            }
        }
        return null;
    }

    public static boolean owned(Entity entity) {
        return entity instanceof Mob && entity.getPersistentData().hasUUID(OWNER);
    }

    @Nullable
    static UUID ownerId(Entity entity) {
        return owned(entity) ? entity.getPersistentData().getUUID(OWNER) : null;
    }

    // The player a summon fights for, while they are in its world.
    @Nullable
    static ServerPlayer owner(Entity entity) {
        UUID id = ownerId(entity);
        if (id == null || entity.level().getServer() == null) {
            return null;
        }
        ServerPlayer player = entity.level().getServer().getPlayerList().getPlayer(id);
        return player != null && player.level() == entity.level() ? player : null;
    }

    // Whether `who` is `owner` or on his side: himself, a player of his team, or a summon of either.
    private static boolean ally(ServerPlayer owner, Entity who) {
        if (who == owner || who instanceof Player player && !Factions.mayHit(owner, player)) {
            return true;
        }
        UUID id = ownerId(who);
        if (id == null) {
            return false;
        }
        ServerPlayer other = owner.server.getPlayerList().getPlayer(id);
        return id.equals(owner.getUUID()) || other != null && !Factions.mayHit(owner, other);
    }

    // Whether a summon of `owner` may fight `target`.
    static boolean fair(ServerPlayer owner, LivingEntity target) {
        return target.isAlive() && !ally(owner, target);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && owned(mob)) {
            SummonGoals.take(mob);
        }
    }

    // Out of time it breaks up; it never burns (a construct of light).
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !owned(entity)) {
            return;
        }
        entity.clearFire();
        if (entity.tickCount % CHECK == 0 && entity.level().getGameTime() >= entity.getPersistentData().getLong(UNTIL)
                && entity.level() instanceof ServerLevel level) {
            breakUp(level, entity);
            entity.discard();
        }
    }

    private static void breakUp(ServerLevel level, Entity entity) {
        Vec3 middle = entity.getBoundingBox().getCenter();
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), middle, 30, entity.getBbWidth() * 0.6,
                0.08);
        level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL,
                1.0F, 1.1F);
    }

    // A summon never hurts its owner or his side, and they never hurt it.
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();
        if (source == null || victim.level().isClientSide()) {
            return;
        }
        ServerPlayer owner = owner(source);
        if (owner != null && ally(owner, victim)) {
            event.setCanceled(true);
            return;
        }
        ServerPlayer mine = owner(victim);
        if (mine != null && ally(mine, source)) {
            event.setCanceled(true);
        }
    }

    // Whatever turns a summon on its owner or his side (a hurt piglin calling the others in) is not heeded.
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null || event.getEntity().level().isClientSide()) {
            return;
        }
        ServerPlayer owner = owner(event.getEntity());
        if (owner != null && !fair(owner, target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (owned(event.getEntity()) && event.getEntity().level() instanceof ServerLevel level) {
            breakUp(level, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (owned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        if (owned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // A creeper's blast, a blaze's fire, an enderman's lifted block: a summon leaves the world as it was.
    @SubscribeEvent
    public static void onGrief(EntityMobGriefingEvent event) {
        if (owned(event.getEntity())) {
            event.setCanGrief(false);
        }
    }

    @SubscribeEvent
    public static void onTrack(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (event.getEntity() instanceof ServerPlayer viewer && owned(target)) {
            PacketDistributor.sendToPlayer(viewer, new SummonPayload(target.getId(), target.tickCount));
        }
    }

    public static void clear() {
        WAIT.clear();
    }
}
