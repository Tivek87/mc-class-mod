package nl.tivek.multiversepowers.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.command.ShortCommand.Arg;

// Makes every short command a command of the game (one another mod already has is left to it), and the world's own
// aliases (`CommandAliases`). Keeps where players were before a teleport or a death, for /back.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class ShortCommandRegistry {
    private static final DynamicCommandExceptionType NOT_A_SWITCH = new DynamicCommandExceptionType(
            typed -> Component.translatable("command." + MultiversePowers.MODID + ".switch", typed));

    private ShortCommandRegistry() {
    }

    // Last, so every other mod's commands are in and a name already taken is seen as taken.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        for (ShortCommand command : ShortCommands.all()) {
            if (dispatcher.getRoot().getChild(command.name()) != null) {
                MultiversePowers.LOGGER.info("/{} is already a command: the short command is left out", command.name());
                continue;
            }
            dispatcher.register(build(command, event.getBuildContext()));
        }
        CommandAliases.register(dispatcher);
    }

    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShortActions.left(player);
        }
    }

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShortActions.left(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShortActions.left(player);
        }
    }

    public static void clear() {
        ShortActions.clear();
        CommandAliases.clear();
    }

    // `/name`, then each argument under the one before, each place it may end running the command.
    private static LiteralArgumentBuilder<CommandSourceStack> build(ShortCommand command, CommandBuildContext context) {
        List<Arg> args = command.args();
        ArgumentBuilder<CommandSourceStack, ?> next = null;
        boolean restOptional = true;
        for (int i = args.size() - 1; i >= 0; i--) {
            RequiredArgumentBuilder<CommandSourceStack, ?> node = argument(args.get(i), context);
            if (restOptional) {
                node.executes(c -> run(c, command));
            }
            if (next != null) {
                node.then(next);
            }
            next = node;
            restOptional &= args.get(i).fallback() != null;
        }
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(command.name())
                .requires(source -> source.hasPermission(command.permission()));
        if (restOptional) {
            root.executes(c -> run(c, command));
        }
        if (next != null) {
            root.then(next);
        }
        return root;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> argument(Arg arg, CommandBuildContext context) {
        RequiredArgumentBuilder<CommandSourceStack, ?> node = Commands.argument(arg.name(), type(arg, context));
        return switch (arg.kind()) {
            case SWITCH -> node.suggests((c, builder) -> SharedSuggestionProvider.suggest(List.of("on", "off"),
                    builder));
            case ENTITY -> node.suggests(SuggestionProviders.SUMMONABLE_ENTITIES);
            default -> node;
        };
    }

    private static ArgumentType<?> type(Arg arg, CommandBuildContext context) {
        return switch (arg.kind()) {
            case NUMBER -> IntegerArgumentType.integer(0, 1_000_000);
            case SECONDS -> IntegerArgumentType.integer(1, 1_000_000);
            case LEVEL -> IntegerArgumentType.integer(1, 256);
            case SWITCH -> StringArgumentType.word();
            case PLAYERS -> EntityArgument.players();
            case ITEM -> ItemArgument.item(context);
            case EFFECT -> ResourceArgument.resource(context, Registries.MOB_EFFECT);
            case ENCHANTMENT -> ResourceArgument.resource(context, Registries.ENCHANTMENT);
            case ENTITY -> ResourceArgument.resource(context, Registries.ENTITY_TYPE);
            case TEXT -> StringArgumentType.greedyString();
        };
    }

    private static int run(CommandContext<CommandSourceStack> context, ShortCommand command)
            throws CommandSyntaxException {
        Map<String, String> args = args(context, command);
        if (command.action() != null) {
            return command.action().run(context.getSource(), args);
        }
        for (String line : ShortCommands.fill(command.template(), args)) {
            context.getSource().getServer().getCommands().performPrefixedCommand(context.getSource(), line);
        }
        return 1;
    }

    // Every argument as it was typed (so a selector or an item with its parts goes on as it is), or its fallback.
    private static Map<String, String> args(CommandContext<CommandSourceStack> context, ShortCommand command)
            throws CommandSyntaxException {
        Map<String, String> typed = new HashMap<>();
        for (ParsedCommandNode<CommandSourceStack> node : context.getNodes()) {
            if (node.getNode() instanceof ArgumentCommandNode<?, ?> argument) {
                typed.put(argument.getName(), node.getRange().get(context.getInput()));
            }
        }
        Map<String, String> args = new LinkedHashMap<>();
        for (Arg arg : command.args()) {
            String value = typed.getOrDefault(arg.name(), arg.fallback());
            args.put(arg.name(), switch (arg.kind()) {
                case LEVEL -> String.valueOf(Integer.parseInt(value) - 1);
                case SWITCH -> onOff(value);
                default -> value;
            });
        }
        return args;
    }

    private static String onOff(String typed) throws CommandSyntaxException {
        return switch (typed.toLowerCase(Locale.ROOT)) {
            case "on", "true", "yes", "1" -> "true";
            case "off", "false", "no", "0" -> "false";
            default -> throw NOT_A_SWITCH.create(typed);
        };
    }
}
