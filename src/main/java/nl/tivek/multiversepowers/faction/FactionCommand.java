package nl.tivek.multiversepowers.faction;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.tivek.multiversepowers.MultiversePowers;

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class FactionCommand {
    private static final String KEY = "faction." + MultiversePowers.MODID + ".";

    private FactionCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("faction")
                .then(Commands.literal("create").requires(FactionCommand::op)
                        .then(Commands.argument("name", StringArgumentType.word()).executes(FactionCommand::create)))
                .then(Commands.literal("delete").requires(FactionCommand::op)
                        .then(Commands.argument("faction", TeamArgument.team()).executes(FactionCommand::delete)))
                .then(Commands.literal("add").requires(FactionCommand::op)
                        .then(Commands.argument("faction", TeamArgument.team())
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(FactionCommand::add))))
                .then(Commands.literal("remove").requires(FactionCommand::op)
                        .then(Commands.argument("players", EntityArgument.players()).executes(FactionCommand::remove)))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player()).executes(FactionCommand::invite)))
                .then(Commands.literal("join")
                        .then(Commands.argument("faction", TeamArgument.team()).executes(FactionCommand::join)))
                .then(Commands.literal("leave").executes(FactionCommand::leave))
                .then(relation("enemy", Standings.Relation.ENEMY))
                .then(relation("ally", Standings.Relation.ALLY))
                .then(relation("neutral", Standings.Relation.NONE))
                .then(Commands.literal("list").executes(FactionCommand::list)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> relation(String name, Standings.Relation relation) {
        return Commands.literal(name).requires(FactionCommand::op)
                .then(Commands.argument("faction", TeamArgument.team())
                        .then(Commands.argument("other", TeamArgument.team())
                                .executes(context -> relate(context, relation, name))));
    }

    private static boolean op(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    private static Component text(String key, Object... args) {
        return Component.translatable(KEY + key, args);
    }

    private static int create(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        ServerScoreboard scoreboard = context.getSource().getServer().getScoreboard();
        if (scoreboard.getPlayerTeam(name) != null) {
            context.getSource().sendFailure(text("exists", name));
            return 0;
        }
        PlayerTeam team = scoreboard.addPlayerTeam(name);
        team.setAllowFriendlyFire(false);
        team.setSeeFriendlyInvisibles(true);
        context.getSource().sendSuccess(() -> text("created", name), true);
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PlayerTeam team = TeamArgument.getTeam(context, "faction");
        MinecraftServer server = context.getSource().getServer();
        String name = team.getName();
        FactionData.get(server).forget(name);
        Factions.forget(name);
        server.getScoreboard().removePlayerTeam(team);
        Factions.sync(server);
        context.getSource().sendSuccess(() -> text("deleted", name), true);
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PlayerTeam team = TeamArgument.getTeam(context, "faction");
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        MinecraftServer server = context.getSource().getServer();
        for (ServerPlayer player : players) {
            server.getScoreboard().addPlayerToTeam(player.getScoreboardName(), team);
            context.getSource().sendSuccess(() -> text("joined", player.getDisplayName(), team.getName()), true);
        }
        Factions.sync(server);
        return players.size();
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        MinecraftServer server = context.getSource().getServer();
        int done = 0;
        for (ServerPlayer player : players) {
            if (server.getScoreboard().removePlayerFromTeam(player.getScoreboardName())) {
                done++;
                context.getSource().sendSuccess(() -> text("left", player.getDisplayName()), true);
            } else {
                context.getSource().sendFailure(text("not_in_one", player.getDisplayName()));
            }
        }
        Factions.sync(server);
        return done;
    }

    private static int invite(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer self = context.getSource().getPlayerOrException();
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        PlayerTeam team = self.getTeam();
        if (team == null) {
            context.getSource().sendFailure(text("no_faction"));
            return 0;
        }
        if (player.getTeam() == team) {
            context.getSource().sendFailure(text("already", player.getDisplayName(), team.getName()));
            return 0;
        }
        Factions.invite(player, team.getName());
        player.sendSystemMessage(text("invite", self.getDisplayName(), team.getName(), team.getName()));
        context.getSource().sendSuccess(() -> text("invited", player.getDisplayName(), team.getName()), false);
        return 1;
    }

    private static int join(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer self = context.getSource().getPlayerOrException();
        PlayerTeam team = TeamArgument.getTeam(context, "faction");
        if (self.getTeam() == team) {
            context.getSource().sendFailure(text("already", self.getDisplayName(), team.getName()));
            return 0;
        }
        if (!op(context.getSource()) && !Factions.invited(self, team.getName())) {
            context.getSource().sendFailure(text("not_invited", team.getName()));
            return 0;
        }
        MinecraftServer server = context.getSource().getServer();
        server.getScoreboard().addPlayerToTeam(self.getScoreboardName(), team);
        Factions.sync(server);
        context.getSource().sendSuccess(() -> text("joined", self.getDisplayName(), team.getName()), true);
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer self = context.getSource().getPlayerOrException();
        MinecraftServer server = context.getSource().getServer();
        if (!server.getScoreboard().removePlayerFromTeam(self.getScoreboardName())) {
            context.getSource().sendFailure(text("no_faction"));
            return 0;
        }
        Factions.sync(server);
        context.getSource().sendSuccess(() -> text("left", self.getDisplayName()), true);
        return 1;
    }

    private static int relate(CommandContext<CommandSourceStack> context, Standings.Relation relation, String name)
            throws CommandSyntaxException {
        PlayerTeam team = TeamArgument.getTeam(context, "faction");
        PlayerTeam other = TeamArgument.getTeam(context, "other");
        if (team == other) {
            context.getSource().sendFailure(text("same"));
            return 0;
        }
        MinecraftServer server = context.getSource().getServer();
        FactionData.get(server).set(team.getName(), other.getName(), relation);
        Factions.sync(server);
        context.getSource().sendSuccess(() -> text("relation." + name, team.getName(), other.getName()), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        Collection<PlayerTeam> teams = server.getScoreboard().getPlayerTeams();
        if (teams.isEmpty()) {
            context.getSource().sendSuccess(() -> text("list.none"), false);
            return 0;
        }
        FactionData data = FactionData.get(server);
        for (PlayerTeam team : teams) {
            List<String> allies = new ArrayList<>();
            List<String> enemies = new ArrayList<>();
            for (PlayerTeam other : teams) {
                Standings.Relation relation = team == other ? Standings.Relation.NONE
                        : data.relation(team.getName(), other.getName());
                if (relation == Standings.Relation.ALLY) {
                    allies.add(other.getName());
                } else if (relation == Standings.Relation.ENEMY) {
                    enemies.add(other.getName());
                }
            }
            Component members = team.getPlayers().isEmpty() ? text("list.nobody")
                    : Component.literal(String.join(", ", team.getPlayers()));
            Component friends = allies.isEmpty() ? text("list.nobody") : Component.literal(String.join(", ", allies));
            Component foes = enemies.isEmpty() ? text("list.nobody") : Component.literal(String.join(", ", enemies));
            context.getSource().sendSuccess(() -> text("list.team", team.getName(), members, friends, foes), false);
        }
        return teams.size();
    }
}
