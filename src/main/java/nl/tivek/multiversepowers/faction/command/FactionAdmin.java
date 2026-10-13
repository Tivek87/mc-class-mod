package nl.tivek.multiversepowers.faction.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import nl.tivek.multiversepowers.faction.FactionData;
import nl.tivek.multiversepowers.faction.FactionEdits;
import nl.tivek.multiversepowers.faction.Factions;
import nl.tivek.multiversepowers.faction.MobFactions;
import nl.tivek.multiversepowers.faction.Reputation;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;
import nl.tivek.multiversepowers.faction.rules.FactionRules.Rule;
import nl.tivek.multiversepowers.faction.rules.FactionTable;

// The operators' side of `/faction`: a faction's words, numbers, members and relations, one creature's own faction
// and behaviour, players' reputations and pinned standings, the numbers of reputation, a step back and a reset.
final class FactionAdmin {
    private static final String[] STANCES = {"friendly", "neutral", "hostile"};

    private FactionAdmin() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, CommandBuildContext build) {
        root.then(set());
        root.then(Commands.literal("relation").requires(FactionCommand::op)
                .then(FactionCommand.factionArg("faction").then(stances(FactionCommand.factionArg("other"), true,
                        (context, standing) -> relation(context, standing)))));
        root.then(Commands.literal("member").requires(FactionCommand::op)
                .then(Commands.literal("add").then(FactionCommand.factionArg("faction")
                        .then(Commands.argument("creature", ResourceArgument.resource(build, Registries.ENTITY_TYPE))
                                .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .executes(context -> member(context, true)))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("creature", ResourceArgument.resource(build, Registries.ENTITY_TYPE))
                                .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .executes(context -> member(context, false))))
                .then(Commands.literal("list").then(FactionCommand.factionArg("faction")
                        .executes(FactionAdmin::members))));
        root.then(Commands.literal("mob").requires(FactionCommand::op)
                .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.literal("info").executes(FactionAdmin::mobInfo))
                        .then(Commands.literal("faction")
                                .then(Commands.literal("none").executes(context -> mobFaction(context,
                                        MobFactions.NONE)))
                                .then(Commands.literal("kind").executes(context -> mobFaction(context, "")))
                                .then(FactionCommand.factionArg("faction").executes(context -> mobFaction(context,
                                        FactionCommand.faction(context, "faction").id()))))
                        .then(stances(Commands.literal("stance"), false, FactionAdmin::mobStance)
                                .then(Commands.literal("faction").executes(context -> mobStance(context, null))))));
        root.then(Commands.literal("stance").requires(FactionCommand::op)
                .then(Commands.argument("players", EntityArgument.players())
                        .then(stances(FactionCommand.factionArg("faction"), false, FactionAdmin::pin)
                                .then(Commands.literal("clear").executes(context -> pin(context, null))))));
        root.then(Commands.literal("rule").requires(FactionCommand::op)
                .executes(FactionAdmin::rules)
                .then(Commands.argument("rule", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (Rule rule : Rule.values()) {
                                builder.suggest(rule.key);
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(FactionAdmin::rule))));
        root.then(Commands.literal("undo").requires(FactionCommand::op).executes(FactionAdmin::undo));
        root.then(Commands.literal("reset").requires(FactionCommand::op)
                .then(Commands.literal("confirm").executes(FactionAdmin::reset)));
    }

    @FunctionalInterface
    private interface Stand {
        int run(CommandContext<CommandSourceStack> context, @Nullable Standing standing) throws CommandSyntaxException;
    }

    // `node` with friendly, neutral and hostile under it, and `natural` for as each creature's nature has it.
    private static <T extends com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, T>> T stances(T node,
            boolean natural, Stand run) {
        for (String name : STANCES) {
            Standing standing = FactionTable.standing(name);
            node.then(Commands.literal(name).executes(context -> run.run(context, standing)));
        }
        if (natural) {
            node.then(Commands.literal(FactionTable.NATURAL).executes(context -> run.run(context, null)));
        }
        return node;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> set() {
        var faction = FactionCommand.factionArg("faction");
        for (String key : new String[] {"name", "description", "lore"}) {
            faction.then(Commands.literal(key).then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(context -> word(context, key, StringArgumentType.getString(context, "text")))));
        }
        faction.then(Commands.literal("color").then(Commands.argument("hex", StringArgumentType.word())
                .executes(context -> word(context, "color", StringArgumentType.getString(context, "hex")))));
        faction.then(stances(Commands.literal("attitude"), false, (context, standing) -> word(context, "attitude",
                FactionTable.name(standing))));
        for (String key : new String[] {"reputation", "help", "open"}) {
            faction.then(Commands.literal(key)
                    .then(Commands.literal("true").executes(context -> word(context, key, "true")))
                    .then(Commands.literal("false").executes(context -> word(context, key, "false"))));
        }
        for (String key : new String[] {"hostileAt", "friendlyAt"}) {
            faction.then(Commands.literal(key).then(Commands.argument("reputation",
                    IntegerArgumentType.integer(Faction.LOWEST, Faction.HIGHEST)).executes(context -> word(context,
                            key, String.valueOf(IntegerArgumentType.getInteger(context, "reputation"))))));
        }
        return Commands.literal("set").requires(FactionCommand::op).then(faction);
    }

    private static int word(CommandContext<CommandSourceStack> context, String key, String value)
            throws CommandSyntaxException {
        Faction faction = FactionCommand.faction(context, "faction");
        if (!FactionEdits.word(context.getSource().getServer(), faction.id(), key, value)) {
            context.getSource().sendFailure(FactionCommand.text("set.bad", key, value));
            return 0;
        }
        Faction now = FactionRules.get(faction.id());
        context.getSource().sendSuccess(() -> FactionCommand.text("set.done", Reputation.name(now), key, value), true);
        return 1;
    }

    private static int relation(CommandContext<CommandSourceStack> context, @Nullable Standing standing)
            throws CommandSyntaxException {
        Faction faction = FactionCommand.faction(context, "faction");
        Faction other = FactionCommand.faction(context, "other");
        FactionEdits.relation(context.getSource().getServer(), faction.id(), other.id(), standing);
        String key = (faction == other ? "relation.own" : "relation.set") + (standing == null ? ".natural" : "");
        context.getSource().sendSuccess(() -> FactionCommand.text(key, Reputation.name(faction),
                FactionCommand.stand(standing), Reputation.name(other)), true);
        return 1;
    }

    private static int member(CommandContext<CommandSourceStack> context, boolean add) throws CommandSyntaxException {
        EntityType<?> type = ResourceArgument.getResource(context, "creature", Registries.ENTITY_TYPE).value();
        Faction faction = add ? FactionCommand.faction(context, "faction") : null;
        if (FactionEdits.members(context.getSource().getServer(), add ? faction.id() : "",
                List.of(FactionRules.id(type))) == 0) {
            context.getSource().sendFailure(FactionCommand.text("member.bad", type.getDescription()));
            return 0;
        }
        context.getSource().sendSuccess(() -> add
                ? FactionCommand.text("member.added", type.getDescription(), Reputation.name(faction))
                : FactionCommand.text("member.removed", type.getDescription()), true);
        return 1;
    }

    private static int members(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Faction faction = FactionCommand.faction(context, "faction");
        List<EntityType<?>> kinds = FactionRules.members(faction.id());
        List<String> names = new ArrayList<>();
        for (EntityType<?> kind : kinds) {
            names.add(kind.getDescription().getString());
        }
        names.sort(null);
        context.getSource().sendSuccess(() -> FactionCommand.text("member.list", Reputation.name(faction),
                names.size(), names.isEmpty() ? FactionCommand.text("list.nobody")
                        : Component.literal(String.join(", ", names))), false);
        return kinds.size();
    }

    private static List<Mob> mobs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        List<Mob> mobs = new ArrayList<>();
        for (Entity entity : EntityArgument.getEntities(context, "targets")) {
            if (entity instanceof Mob mob && MobFactions.free(mob)) {
                mobs.add(mob);
            }
        }
        if (mobs.isEmpty()) {
            throw FactionCommand.fail("mob.none");
        }
        return mobs;
    }

    private static int mobFaction(CommandContext<CommandSourceStack> context, String faction)
            throws CommandSyntaxException {
        List<Mob> mobs = mobs(context);
        mobs.forEach(mob -> MobFactions.faction(mob, faction));
        Faction now = FactionRules.get(faction);
        context.getSource().sendSuccess(() -> FactionCommand.text(faction.isEmpty() ? "mob.faction.kind"
                : now == null ? "mob.faction.none" : "mob.faction", mobs.size(),
                now == null ? Component.empty() : Reputation.name(now)), true);
        return mobs.size();
    }

    private static int mobStance(CommandContext<CommandSourceStack> context, @Nullable Standing standing)
            throws CommandSyntaxException {
        List<Mob> mobs = mobs(context);
        mobs.forEach(mob -> MobFactions.stance(mob, standing));
        context.getSource().sendSuccess(() -> FactionCommand.text(standing == null ? "mob.stance.faction"
                : "mob.stance", mobs.size(), FactionCommand.stand(standing)), true);
        return mobs.size();
    }

    private static int mobInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        List<Mob> mobs = mobs(context);
        for (Mob mob : mobs) {
            Faction faction = MobFactions.of(mob);
            MobFactions.Own own = MobFactions.own(mob);
            Standing stance = MobFactions.stance(mob);
            context.getSource().sendSuccess(() -> FactionCommand.text("mob.info", mob.getDisplayName(),
                    faction == null ? FactionCommand.text("none") : Reputation.name(faction),
                    FactionCommand.text(own != null && !own.faction().isEmpty() ? "mob.info.own" : "mob.info.kind"),
                    stance == null ? FactionCommand.text("mob.info.faction") : FactionCommand.stand(stance)), false);
        }
        return mobs.size();
    }

    private static int pin(CommandContext<CommandSourceStack> context, @Nullable Standing standing)
            throws CommandSyntaxException {
        Faction faction = FactionCommand.faction(context, "faction");
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        MinecraftServer server = context.getSource().getServer();
        for (ServerPlayer player : players) {
            FactionData.get(server).pin(player.getUUID(), faction.id(), standing);
            context.getSource().sendSuccess(() -> standing == null
                    ? FactionCommand.text("pin.cleared", player.getDisplayName(), Reputation.name(faction))
                    : FactionCommand.text("pin.set", Reputation.name(faction), FactionCommand.stand(standing),
                            player.getDisplayName()), true);
        }
        Factions.changed(server);
        return players.size();
    }

    private static int rules(CommandContext<CommandSourceStack> context) {
        for (Rule rule : Rule.values()) {
            context.getSource().sendSuccess(() -> FactionCommand.text("rule.line", rule.key, rule.get(), rule.most,
                    Component.translatable(FactionCommand.RULES + rule.key + ".desc")), false);
        }
        return Rule.values().length;
    }

    private static int rule(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "rule");
        Rule rule = Rule.of(key);
        if (rule == null) {
            context.getSource().sendFailure(FactionCommand.text("rule.unknown", key));
            return 0;
        }
        FactionEdits.rule(context.getSource().getServer(), rule, IntegerArgumentType.getInteger(context, "value"));
        context.getSource().sendSuccess(() -> FactionCommand.text("rule.set", rule.key, rule.get()), true);
        return 1;
    }

    private static int undo(CommandContext<CommandSourceStack> context) {
        if (!FactionEdits.undo(context.getSource().getServer())) {
            context.getSource().sendFailure(FactionCommand.text("undo.none"));
            return 0;
        }
        context.getSource().sendSuccess(() -> FactionCommand.text("undo.done", FactionEdits.steps()), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context) {
        FactionEdits.reset(context.getSource().getServer());
        context.getSource().sendSuccess(() -> FactionCommand.text("reset.done"), true);
        return 1;
    }
}
