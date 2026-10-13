package nl.tivek.multiversepowers.faction.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.faction.FactionEdits;
import nl.tivek.multiversepowers.faction.Factions;
import nl.tivek.multiversepowers.faction.Reputation;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;

// `/faction`: what every player may do (see the factions and their own reputation, join, leave, invite) and the
// making, filling and deleting of factions; the rest of the operators' side is `FactionAdmin`.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class FactionCommand {
    static final String KEY = "faction." + MultiversePowers.MODID + ".";
    static final String RULES = KEY + "rule.";

    private FactionCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("faction").executes(FactionCommand::list);
        root.then(Commands.literal("list").executes(FactionCommand::list));
        root.then(Commands.literal("help").executes(FactionCommand::help));
        root.then(Commands.literal("info").then(factionArg("faction").executes(FactionCommand::info)));
        root.then(Commands.literal("rep").executes(FactionCommand::reputations)
                .then(Commands.literal("get").requires(FactionCommand::op)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> reputations(context, EntityArgument.getPlayer(context,
                                        "player")))))
                .then(Commands.literal("set").requires(FactionCommand::op)
                        .then(Commands.argument("players", EntityArgument.players()).then(factionArg("faction")
                                .then(Commands.argument("value", IntegerArgumentType.integer(Faction.LOWEST,
                                        Faction.HIGHEST)).executes(context -> reputation(context, true))))))
                .then(Commands.literal("add").requires(FactionCommand::op)
                        .then(Commands.argument("players", EntityArgument.players()).then(factionArg("faction")
                                .then(Commands.argument("value", IntegerArgumentType.integer(-200, 200))
                                        .executes(context -> reputation(context, false)))))));
        root.then(Commands.literal("create").requires(FactionCommand::op)
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> create(context, StringArgumentType.getString(context, "id")))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> create(context, StringArgumentType.getString(context,
                                        "name"))))));
        root.then(Commands.literal("delete").requires(FactionCommand::op)
                .then(factionArg("faction").executes(FactionCommand::delete)));
        root.then(Commands.literal("add").requires(FactionCommand::op)
                .then(factionArg("faction").then(Commands.argument("players", EntityArgument.players())
                        .executes(FactionCommand::add))));
        root.then(Commands.literal("remove").requires(FactionCommand::op)
                .then(Commands.argument("players", EntityArgument.players()).executes(FactionCommand::remove)));
        root.then(Commands.literal("invite")
                .then(Commands.argument("player", EntityArgument.player()).executes(FactionCommand::invite)));
        root.then(Commands.literal("join").then(factionArg("faction").executes(FactionCommand::join)));
        root.then(Commands.literal("leave").executes(FactionCommand::leave));
        root.then(pair("ally", Standing.FRIENDLY));
        root.then(pair("enemy", Standing.HOSTILE));
        root.then(pair("neutral", Standing.NEUTRAL));
        FactionAdmin.register(root, event.getBuildContext());
        event.getDispatcher().register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> pair(String name, Standing standing) {
        return Commands.literal(name).requires(FactionCommand::op).then(factionArg("faction")
                .then(factionArg("other").executes(context -> relate(context, standing, name))));
    }

    static boolean op(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    static MutableComponent text(String key, Object... args) {
        return Component.translatable(KEY + key, args);
    }

    static CommandSyntaxException fail(String key, Object... args) {
        return new SimpleCommandExceptionType(text(key, args)).create();
    }

    // How a faction stands, in its colour; null is each creature as its nature has it.
    static Component stand(@Nullable Standing standing) {
        return standing == null ? text("stand.natural").withColor(0xC8C8C8)
                : text("stand." + standing.name().toLowerCase(Locale.ROOT)).withColor(standing.rgb());
    }

    static RequiredArgumentBuilder<CommandSourceStack, String> factionArg(String name) {
        return Commands.argument(name, StringArgumentType.word()).suggests((context, builder) -> {
            String typed = builder.getRemainingLowerCase();
            for (Faction faction : FactionRules.all()) {
                if (faction.id().toLowerCase(Locale.ROOT).startsWith(typed)) {
                    builder.suggest(faction.id(), Component.literal(faction.name()));
                }
            }
            return builder.buildFuture();
        });
    }

    static Faction faction(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, name);
        Faction faction = FactionRules.get(id);
        if (faction == null) {
            throw fail("unknown", id);
        }
        return faction;
    }

    private static List<String> players(MinecraftServer server, Faction faction) {
        PlayerTeam team = server.getScoreboard().getPlayerTeam(faction.id());
        List<String> names = team == null ? new ArrayList<>() : new ArrayList<>(team.getPlayers());
        names.sort(null);
        return names;
    }

    private static Component names(List<String> names) {
        return names.isEmpty() ? text("list.nobody") : Component.literal(String.join(", ", names));
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Collection<Faction> factions = FactionRules.all();
        if (factions.isEmpty()) {
            source.sendSuccess(() -> text("list.none"), false);
            return 0;
        }
        ServerPlayer self = source.getPlayer();
        source.sendSuccess(() -> text("list.head", factions.size()), false);
        for (Faction faction : factions) {
            MutableComponent name = Reputation.name(faction).copy().withStyle(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/faction info " + faction.id()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text("list.hover"))));
            int kinds = FactionRules.members(faction.id()).size();
            int players = players(source.getServer(), faction).size();
            source.sendSuccess(() -> self == null ? text("list.line", name, faction.id(), kinds, players)
                    : text("list.line.you", name, faction.id(), kinds, players,
                            stand(Reputation.toward(faction, self)), Reputation.of(self, faction)), false);
        }
        return factions.size();
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> text("help"), false);
        if (op(context.getSource())) {
            context.getSource().sendSuccess(() -> text("help.op"), false);
        }
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Faction faction = faction(context, "faction");
        source.sendSuccess(() -> text("info.head", Reputation.name(faction), faction.id()), false);
        if (!faction.description().isEmpty()) {
            source.sendSuccess(() -> Component.literal(faction.description()), false);
        }
        if (!faction.lore().isEmpty()) {
            source.sendSuccess(() -> Component.literal(faction.lore()).withStyle(style -> style.withItalic(true)
                    .withColor(0xA9A9B8)), false);
        }
        source.sendSuccess(() -> text("info.attitude", stand(faction.attitude()),
                text(faction.reputation() && FactionRules.reputation() ? "info.reputation" : "info.reputation.off",
                        faction.hostileAt(), faction.friendlyAt()),
                text(faction.help() ? "info.help" : "info.help.off"),
                text(faction.open() ? "info.open" : "info.invite")), false);
        List<String> kinds = new ArrayList<>();
        FactionRules.members(faction.id()).forEach(kind -> kinds.add(kind.getDescription().getString()));
        kinds.sort(null);
        source.sendSuccess(() -> text("info.members", kinds.size(), names(kinds)), false);
        source.sendSuccess(() -> text("info.players", names(players(source.getServer(), faction))), false);
        List<Component> relations = new ArrayList<>();
        Standing own = FactionRules.relation(faction.id(), faction.id());
        if (own != null) {
            relations.add(text("info.relation.own", stand(own)));
        }
        for (Faction other : FactionRules.all()) {
            Standing standing = other == faction ? null : FactionRules.relation(faction.id(), other.id());
            if (standing != null) {
                relations.add(text("info.relation", Reputation.name(other), stand(standing)));
            }
        }
        for (Component relation : relations) {
            source.sendSuccess(() -> relation, false);
        }
        ServerPlayer self = source.getPlayer();
        if (self != null) {
            source.sendSuccess(() -> text("info.you", stand(Reputation.toward(faction, self)),
                    Reputation.of(self, faction)), false);
        }
        return 1;
    }

    private static int reputations(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return reputations(context, context.getSource().getPlayerOrException());
    }

    private static int reputations(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();
        if (!FactionRules.reputation()) {
            source.sendSuccess(() -> text("rep.off"), false);
            return 0;
        }
        source.sendSuccess(() -> text("rep.head", player.getDisplayName()), false);
        int shown = 0;
        for (Faction faction : FactionRules.all()) {
            if (faction.reputation()) {
                shown++;
                source.sendSuccess(() -> text("rep.line", Reputation.name(faction), Reputation.of(player, faction),
                        stand(Reputation.toward(faction, player)), faction.hostileAt(), faction.friendlyAt()), false);
            }
        }
        return shown;
    }

    private static int reputation(CommandContext<CommandSourceStack> context, boolean set)
            throws CommandSyntaxException {
        Faction faction = faction(context, "faction");
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        int value = IntegerArgumentType.getInteger(context, "value");
        for (ServerPlayer player : players) {
            if (set) {
                Reputation.set(player, faction, value);
            } else {
                Reputation.add(player, faction, value);
            }
            context.getSource().sendSuccess(() -> text("rep.set", player.getDisplayName(), Reputation.name(faction),
                    Reputation.of(player, faction), stand(Reputation.toward(faction, player))), true);
        }
        return players.size();
    }

    private static int create(CommandContext<CommandSourceStack> context, String name) {
        String id = StringArgumentType.getString(context, "id");
        String why = FactionEdits.create(context.getSource().getServer(), id, name);
        if (why != null) {
            context.getSource().sendFailure(text(why, id));
            return 0;
        }
        Faction faction = FactionRules.get(id);
        context.getSource().sendSuccess(() -> text("created", faction == null ? id : Reputation.name(faction)), true);
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Faction faction = faction(context, "faction");
        if (!FactionEdits.delete(context.getSource().getServer(), faction.id())) {
            return 0;
        }
        context.getSource().sendSuccess(() -> text("deleted", Reputation.name(faction)), true);
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Faction faction = faction(context, "faction");
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        for (ServerPlayer player : players) {
            FactionEdits.join(player, faction);
            context.getSource().sendSuccess(() -> text("joined", player.getDisplayName(), Reputation.name(faction)),
                    true);
        }
        return players.size();
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int done = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(context, "players")) {
            if (FactionEdits.leave(player)) {
                done++;
                context.getSource().sendSuccess(() -> text("left", player.getDisplayName()), true);
            } else {
                context.getSource().sendFailure(text("not_in_one", player.getDisplayName()));
            }
        }
        return done;
    }

    private static int invite(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer self = context.getSource().getPlayerOrException();
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Faction faction = Factions.of(self);
        if (faction == null) {
            context.getSource().sendFailure(text("no_faction"));
            return 0;
        }
        if (Factions.of(player) == faction) {
            context.getSource().sendFailure(text("already", player.getDisplayName(), Reputation.name(faction)));
            return 0;
        }
        Factions.invite(player, faction.id());
        String join = "/faction join " + faction.id();
        player.sendSystemMessage(text("invite", self.getDisplayName(), Reputation.name(faction), faction.id())
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, join))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(join)))));
        context.getSource().sendSuccess(() -> text("invited", player.getDisplayName(), Reputation.name(faction)),
                false);
        return 1;
    }

    private static int join(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer self = context.getSource().getPlayerOrException();
        Faction faction = faction(context, "faction");
        if (Factions.of(self) == faction) {
            context.getSource().sendFailure(text("already", self.getDisplayName(), Reputation.name(faction)));
            return 0;
        }
        if (!op(context.getSource()) && !faction.open() && !Factions.invited(self, faction.id())) {
            context.getSource().sendFailure(text("not_invited", Reputation.name(faction)));
            return 0;
        }
        if (!op(context.getSource()) && Reputation.toward(faction, self) == Standing.HOSTILE) {
            context.getSource().sendFailure(text("unwelcome", Reputation.name(faction)));
            return 0;
        }
        FactionEdits.join(self, faction);
        context.getSource().sendSuccess(() -> text("joined", self.getDisplayName(), Reputation.name(faction)), true);
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer self = context.getSource().getPlayerOrException();
        if (!FactionEdits.leave(self)) {
            context.getSource().sendFailure(text("no_faction"));
            return 0;
        }
        context.getSource().sendSuccess(() -> text("left", self.getDisplayName()), true);
        return 1;
    }

    // Both factions made to stand the same way to each other, in one go.
    private static int relate(CommandContext<CommandSourceStack> context, Standing standing, String name)
            throws CommandSyntaxException {
        Faction faction = faction(context, "faction");
        Faction other = faction(context, "other");
        if (faction == other) {
            context.getSource().sendFailure(text("same"));
            return 0;
        }
        MinecraftServer server = context.getSource().getServer();
        FactionEdits.relation(server, faction.id(), other.id(), standing);
        FactionEdits.relation(server, other.id(), faction.id(), standing);
        context.getSource().sendSuccess(() -> text("relation." + name, Reputation.name(faction),
                Reputation.name(other)), true);
        return 1;
    }
}
