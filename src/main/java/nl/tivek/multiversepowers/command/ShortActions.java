package nl.tivek.multiversepowers.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// The short commands the game has no command for, each telling who used it what it did.
final class ShortActions {
    private static final String KEY = "command." + MultiversePowers.MODID + ".done.";
    private static final double REACH = 256.0;
    private static final int MOST_SUMMONED = 100;
    private static final ResourceLocation FLY = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "fly_command");

    // Where each player was before a teleport or a death, for /back.
    private record Spot(ResourceKey<Level> level, Vec3 at, float yaw, float pitch) {
    }

    private static final Map<UUID, Spot> BEFORE = new HashMap<>();

    private ShortActions() {
    }

    static void left(ServerPlayer player) {
        BEFORE.put(player.getUUID(), new Spot(player.level().dimension(), player.position(), player.getYRot(),
                player.getXRot()));
    }

    static void clear() {
        BEFORE.clear();
    }

    private static Collection<ServerPlayer> players(CommandSourceStack source, String who)
            throws CommandSyntaxException {
        return EntityArgument.players().parse(new StringReader(who)).findPlayers(source);
    }

    private static void tell(CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> Component.translatable(KEY + key, args), true);
    }

    static int heal(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        Collection<ServerPlayer> players = players(source, args.get("player"));
        for (ServerPlayer player : players) {
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);
            player.clearFire();
        }
        tell(source, "heal", players.size());
        return players.size();
    }

    static int feed(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        Collection<ServerPlayer> players = players(source, args.get("player"));
        for (ServerPlayer player : players) {
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);
        }
        tell(source, "feed", players.size());
        return players.size();
    }

    static int fly(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        Collection<ServerPlayer> players = players(source, args.get("player"));
        for (ServerPlayer player : players) {
            AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
            if (flight == null) {
                continue;
            }
            boolean on = !flight.hasModifier(FLY);
            if (on) {
                flight.addPermanentModifier(new AttributeModifier(FLY, 1.0, AttributeModifier.Operation.ADD_VALUE));
            } else {
                flight.removeModifier(FLY);
                if (!player.mayFly() && player.getAbilities().flying) {
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
            tell(source, on ? "fly.on" : "fly.off", player.getDisplayName());
        }
        return players.size();
    }

    static int god(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        Collection<ServerPlayer> players = players(source, args.get("player"));
        for (ServerPlayer player : players) {
            boolean on = !player.getAbilities().invulnerable;
            player.getAbilities().invulnerable = on;
            player.onUpdateAbilities();
            tell(source, on ? "god.on" : "god.off", player.getDisplayName());
        }
        return players.size();
    }

    static int extinguish(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        Collection<ServerPlayer> players = players(source, args.get("player"));
        players.forEach(ServerPlayer::clearFire);
        tell(source, "ext", players.size());
        return players.size();
    }

    static int repair(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        ItemStack held = source.getPlayerOrException().getMainHandItem();
        if (!held.isDamageableItem()) {
            source.sendFailure(Component.translatable(KEY + "repair.none"));
            return 0;
        }
        held.setDamageValue(0);
        tell(source, "repair", held.getHoverName());
        return 1;
    }

    static int repairAll(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        int fixed = 0;
        for (ServerPlayer player : players(source, args.get("player"))) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isDamageableItem() && stack.isDamaged()) {
                    stack.setDamageValue(0);
                    fixed++;
                }
            }
        }
        tell(source, "repairall", fixed);
        return fixed;
    }

    static int more(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        ItemStack held = source.getPlayerOrException().getMainHandItem();
        if (held.isEmpty()) {
            source.sendFailure(Component.translatable(KEY + "hand.empty"));
            return 0;
        }
        held.setCount(held.getMaxStackSize());
        tell(source, "more", held.getCount(), held.getHoverName());
        return 1;
    }

    static int hat(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            source.sendFailure(Component.translatable(KEY + "hand.empty"));
            return 0;
        }
        ItemStack worn = player.getItemBySlot(EquipmentSlot.HEAD);
        player.setItemSlot(EquipmentSlot.HEAD, held.split(1));
        if (!worn.isEmpty() && !player.getInventory().add(worn)) {
            player.drop(worn, false);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        tell(source, "hat");
        return 1;
    }

    static int pos(CommandSourceStack source, Map<String, String> args) {
        Vec3 at = source.getPosition();
        tell(source, "pos", String.format(Locale.ROOT, "%.1f", at.x), String.format(Locale.ROOT, "%.1f", at.y),
                String.format(Locale.ROOT, "%.1f", at.z),
                source.getLevel().dimension().location().toString());
        return 1;
    }

    private static void teleport(ServerPlayer player, ServerLevel level, Vec3 to) {
        left(player);
        player.teleportTo(level, to.x, to.y, to.z, player.getYRot(), player.getXRot());
    }

    static int top(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos at = player.blockPosition();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ());
        teleport(player, level, new Vec3(player.getX(), y, player.getZ()));
        tell(source, "top", y);
        return 1;
    }

    static int spawn(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getServer().overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
        teleport(player, level, new Vec3(spawn.getX() + 0.5, Math.max(y, spawn.getY()), spawn.getZ() + 0.5));
        tell(source, "spawn");
        return 1;
    }

    static int back(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Spot spot = BEFORE.get(player.getUUID());
        ServerLevel level = spot == null ? null : source.getServer().getLevel(spot.level());
        if (level == null) {
            source.sendFailure(Component.translatable(KEY + "back.none"));
            return 0;
        }
        left(player);
        player.teleportTo(level, spot.at().x, spot.at().y, spot.at().z, spot.yaw(), spot.pitch());
        tell(source, "back");
        return 1;
    }

    static int up(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        int blocks = Math.max(1, Math.min(256, Integer.parseInt(args.get("blocks"))));
        BlockPos floor = player.blockPosition().above(blocks - 1);
        if (!level.isInWorldBounds(floor)) {
            source.sendFailure(Component.translatable(KEY + "up.none"));
            return 0;
        }
        if (level.getBlockState(floor).isAir()) {
            level.setBlockAndUpdate(floor, Blocks.GLASS.defaultBlockState());
        }
        teleport(player, level, new Vec3(player.getX(), floor.getY() + 1, player.getZ()));
        tell(source, "up", blocks);
        return 1;
    }

    private static BlockHitResult look(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(REACH));
        return LoadedWorld.clip(player.level(), new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
    }

    static int jump(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockHitResult hit = look(player);
        if (hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.translatable(KEY + "look.none"));
            return 0;
        }
        BlockPos onto = hit.getBlockPos();
        ServerLevel level = player.serverLevel();
        while (level.isInWorldBounds(onto.above()) && !level.noCollision(player, player.getBoundingBox()
                .move(Vec3.atBottomCenterOf(onto.above()).subtract(player.position())))) {
            onto = onto.above();
        }
        teleport(player, level, Vec3.atBottomCenterOf(onto.above()));
        tell(source, "jump");
        return 1;
    }

    static int summon(CommandSourceStack source, Map<String, String> args) {
        int count = Math.max(1, Math.min(MOST_SUMMONED, Integer.parseInt(args.get("count"))));
        Vec3 at = source.getPosition();
        if (source.getEntity() instanceof ServerPlayer player) {
            BlockHitResult hit = look(player);
            if (hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceTo(at) < 48.0) {
                at = Vec3.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection()));
            }
        }
        String line = String.format(Locale.ROOT, "summon %s %.2f %.2f %.2f", args.get("creature"), at.x, at.y, at.z);
        for (int i = 0; i < count; i++) {
            source.getServer().getCommands().performPrefixedCommand(source, line);
        }
        return count;
    }

    static int butcher(CommandSourceStack source, Map<String, String> args) {
        return kill(source, args, true);
    }

    static int killMobs(CommandSourceStack source, Map<String, String> args) {
        return kill(source, args, false);
    }

    private static int kill(CommandSourceStack source, Map<String, String> args, boolean hostileOnly) {
        double radius = Math.max(1, Math.min(512, Integer.parseInt(args.get("radius"))));
        Vec3 at = source.getPosition();
        List<Mob> mobs = source.getLevel().getEntitiesOfClass(Mob.class,
                new AABB(at, at).inflate(radius),
                mob -> (!hostileOnly || mob instanceof Enemy) && mob.distanceToSqr(at) <= radius * radius);
        for (Mob mob : mobs) {
            mob.kill();
        }
        tell(source, hostileOnly ? "butcher" : "kmobs", mobs.size());
        return mobs.size();
    }

    static int smite(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        String who = args.get("player");
        ServerLevel level = source.getLevel();
        if (!who.isEmpty()) {
            Collection<ServerPlayer> players = players(source, who);
            for (ServerPlayer player : players) {
                strike(player.serverLevel(), player.position());
            }
            tell(source, "smite", players.size());
            return players.size();
        }
        BlockHitResult hit = look(source.getPlayerOrException());
        if (hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.translatable(KEY + "look.none"));
            return 0;
        }
        strike(level, hit.getLocation());
        tell(source, "smite", 1);
        return 1;
    }

    private static void strike(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at);
            level.addFreshEntity(bolt);
        }
    }

    static int boom(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException {
        BlockHitResult hit = look(source.getPlayerOrException());
        if (hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.translatable(KEY + "look.none"));
            return 0;
        }
        float power = Math.max(1, Math.min(20, Integer.parseInt(args.get("power"))));
        Vec3 at = hit.getLocation();
        source.getLevel().explode(null, at.x, at.y, at.z, power, Level.ExplosionInteraction.TNT);
        tell(source, "boom", (int) power);
        return 1;
    }
}
