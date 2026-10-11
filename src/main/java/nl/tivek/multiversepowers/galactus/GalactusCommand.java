package nl.tivek.multiversepowers.galactus;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.effect.Effects;

// `/galactus [quick]|stop`: Galactus comes into the overworld's sky for every player (`Galactus`), quick six times as
// fast. Night is set first so the moon stands high as he pokes it, the time running on or not. For the host or an
// operator.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class GalactusCommand {
    private static final String KEY = "command." + MultiversePowers.MODID + ".galactus.";
    private static final float QUICK = 6.0F;
    // The time of day of the poke: the moon some 55 degrees up, so he stands clear of the ground.
    private static final long POKE_TIME = 15700L;
    private static long start = -1L;
    private static float speed = 1.0F;

    private GalactusCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("galactus").requires(GalactusCommand::allowed)
                .executes(context -> come(context, 1.0F))
                .then(Commands.literal("quick").executes(context -> come(context, QUICK)))
                .then(Commands.literal("stop").executes(GalactusCommand::stop)));
    }

    // A player joining while he is in the sky sees him too.
    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (start >= 0L && event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new GalactusPayload(start, speed));
        }
    }

    public static void clear() {
        start = -1L;
    }

    private static boolean allowed(CommandSourceStack source) {
        return source.hasPermission(2) || source.getPlayer() != null
                && source.getServer().isSingleplayerOwner(source.getPlayer().getGameProfile());
    }

    private static int come(CommandContext<CommandSourceStack> context, float pace) {
        MinecraftServer server = context.getSource().getServer();
        ServerLevel overworld = server.overworld();
        long day = overworld.getDayTime();
        long time = Math.floorMod(day, 24000L);
        boolean turning = overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        long setOut = POKE_TIME - (turning ? (long) (Galactus.TOUCH / pace) : 0L);
        overworld.setDayTime(day - time + setOut + (time > setOut ? 24000L : 0L));
        long now = overworld.getGameTime();
        start = now;
        speed = pace;
        PacketDistributor.sendToAllPlayers(new GalactusPayload(now, pace));
        int ends = (int) Math.ceil(Galactus.GONE / pace) + 20;
        Effects.start(overworld, (level, age) -> {
            if (start != now) {
                return false;
            }
            if (age < ends) {
                return true;
            }
            start = -1L;
            return false;
        });
        context.getSource().sendSuccess(() -> Component.translatable(KEY + "coming"), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        start = -1L;
        PacketDistributor.sendToAllPlayers(GalactusPayload.GONE);
        context.getSource().sendSuccess(() -> Component.translatable(KEY + "gone"), true);
        return 1;
    }
}
