package nl.tivek.multiversepowers.character;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBubble;
import nl.tivek.multiversepowers.character.thor.ThorGrab;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.Captives;

// `/escapegame cage|grab|stop`: plays an escape game without being caught, to try it. For the host or an operator.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class EscapeCommand {
    private static final String KEY = "command." + MultiversePowers.MODID + ".escape.";
    // How long a tried game may last before it lets go by itself: the grab's second and a moment, the cage a minute.
    private static final int GRAB_TICKS = 40;
    private static final int CAGE_TICKS = 1200;

    private EscapeCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("escapegame").requires(EscapeCommand::allowed)
                .then(Commands.literal("cage").executes(context -> start(context, LightBubble.ESCAPE,
                        LightBubble.ESCAPE_FASTEST, CAGE_TICKS)))
                .then(Commands.literal("grab").executes(context -> start(context, ThorGrab.ESCAPE,
                        ThorGrab.ESCAPE_FASTEST, GRAB_TICKS)))
                .then(Commands.literal("stop").executes(EscapeCommand::stop)));
    }

    private static boolean allowed(CommandSourceStack source) {
        return source.hasPermission(2) || source.getPlayer() != null
                && source.getServer().isSingleplayerOwner(source.getPlayer().getGameProfile());
    }

    private static int start(CommandContext<CommandSourceStack> context, int game, int fastest, int ticks)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (Captives.held(player)) {
            context.getSource().sendFailure(Component.translatable(KEY + "busy"));
            return 0;
        }
        Captives.hold(player, game, fastest,
                freed -> freed.displayClientMessage(Component.translatable(KEY + "won"), true));
        Effects.start(player.serverLevel(), (level, age) -> {
            if (!Captives.held(player)) {
                return false;
            }
            if (age < ticks && player.isAlive() && !player.hasDisconnected()) {
                return true;
            }
            Captives.release(player);
            player.displayClientMessage(Component.translatable(KEY + "ended"), true);
            return false;
        });
        context.getSource().sendSuccess(() -> Component.translatable(KEY + "started"), false);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!Captives.held(player)) {
            context.getSource().sendFailure(Component.translatable(KEY + "none"));
            return 0;
        }
        Captives.release(player);
        context.getSource().sendSuccess(() -> Component.translatable(KEY + "ended"), false);
        return 1;
    }
}
