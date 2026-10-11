package nl.tivek.multiversepowers.stage.show;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.stage.LampKind;
import nl.tivek.multiversepowers.stage.LampLooks;
import nl.tivek.multiversepowers.stage.LampSettings;
import nl.tivek.multiversepowers.stage.StageLightBlockEntity;
import nl.tivek.multiversepowers.stage.show.LightShowRig.Group;

// `/lightshow`: builds a stage of every kind of stage light in front of you and runs a show on it, scene after scene,
// round and round until `/lightshow stop` takes it all down again. For the host or an operator, one show each.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class LightShow {
    private static final String KEY = "command." + MultiversePowers.MODID + ".lightshow.";
    private static final Map<UUID, LightShow> RUNNING = new HashMap<>();

    // One scene: how long it lasts, in seconds, and the look of each group (null: dark).
    private record Scene(int seconds, Map<Group, Consumer<LampSettings>> looks) {
    }

    private static final List<Scene> SCENES = List.of(
            scene(10, Group.SEARCH, look(LampKind.SEARCHLIGHT, "premiere"), Group.DISCO,
                    look(LampKind.DISCO_BALL, "classic")),
            scene(12, Group.HEADS, look(LampKind.MOVING_HEAD, "sweep"), Group.PARS, wash("warm"), Group.DISCO,
                    look(LampKind.DISCO_BALL, "classic"), Group.SEARCH, look(LampKind.SEARCHLIGHT, "circle")),
            scene(12, Group.LASERS, look(LampKind.LASER, "fan"), Group.HEADS, look(LampKind.MOVING_HEAD, "sweep"),
                    Group.PARS, wash("fade")),
            scene(8, Group.HEADS, look(LampKind.MOVING_HEAD, "rave"), Group.LASERS, look(LampKind.LASER, "rainbow"),
                    Group.STROBES, look(LampKind.STROBE, "fast"), Group.PARS, wash("pulse"), Group.DISCO,
                    look(LampKind.DISCO_BALL, "party"), Group.SEARCH, look(LampKind.SEARCHLIGHT, "premiere")),
            scene(14, Group.HEADS, look(LampKind.MOVING_HEAD, "party"), Group.LASERS, look(LampKind.LASER, "tunnel"),
                    Group.PARS, wash("fade"), Group.DISCO, look(LampKind.DISCO_BALL, "fast")),
            scene(10, Group.HEADS, look(LampKind.MOVING_HEAD, "spotlight"), Group.PARS, wash("warm"),
                    Group.DISCO, look(LampKind.DISCO_BALL, "stars"), Group.SEARCH,
                    look(LampKind.SEARCHLIGHT, "still")),
            scene(10, Group.HEADS, look(LampKind.MOVING_HEAD, "rave"), Group.LASERS, look(LampKind.LASER, "fan"),
                    Group.STROBES, look(LampKind.STROBE, "burst"), Group.PARS, wash("pulse"), Group.DISCO,
                    look(LampKind.DISCO_BALL, "party"), Group.SEARCH, look(LampKind.SEARCHLIGHT, "circle")),
            scene(8, Group.DISCO, look(LampKind.DISCO_BALL, "stars"), Group.SEARCH,
                    look(LampKind.SEARCHLIGHT, "premiere")));

    private final LightShowRig rig;
    private int scene = -1;
    private int sceneEnds;
    private boolean stopped;

    private LightShow(LightShowRig rig) {
        this.rig = rig;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("lightshow").requires(LightShow::allowed)
                .executes(LightShow::start)
                .then(Commands.literal("start").executes(LightShow::start))
                .then(Commands.literal("stop").executes(LightShow::stop)));
    }

    private static boolean allowed(CommandSourceStack source) {
        return source.hasPermission(2) || source.getPlayer() != null
                && source.getServer().isSingleplayerOwner(source.getPlayer().getGameProfile());
    }

    private static int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        LightShow old = RUNNING.remove(player.getUUID());
        if (old != null) {
            old.end();
        }
        LightShowRig rig = new LightShowRig(player.serverLevel(), player.blockPosition(), player.getDirection());
        if (!rig.build()) {
            context.getSource().sendFailure(Component.translatable(KEY + "no_room"));
            return 0;
        }
        LightShow show = new LightShow(rig);
        RUNNING.put(player.getUUID(), show);
        Effects.start(player.serverLevel(), show::tick);
        context.getSource().sendSuccess(() -> Component.translatable(KEY + "started", rig.lamps.size()), false);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        LightShow show = RUNNING.remove(player.getUUID());
        if (show == null) {
            context.getSource().sendFailure(Component.translatable(KEY + "none"));
            return 0;
        }
        show.end();
        context.getSource().sendSuccess(() -> Component.translatable(KEY + "stopped"), false);
        return 1;
    }

    // Every show taken down, as the server stops, so no stage is saved half-built.
    public static void stopAll(MinecraftServer server) {
        for (LightShow show : RUNNING.values()) {
            show.end();
        }
        RUNNING.clear();
    }

    private void end() {
        this.stopped = true;
        this.rig.remove();
    }

    private boolean tick(ServerLevel level, int age) {
        if (this.stopped) {
            return false;
        }
        if (age >= this.sceneEnds) {
            this.scene = (this.scene + 1) % SCENES.size();
            Scene now = SCENES.get(this.scene);
            this.sceneEnds = age + now.seconds() * 20;
            this.play(now);
        }
        return true;
    }

    // Every light of the stage set to the scene's look of its group, or dark; heads and lasers each a little behind
    // the one before, so they move as a wave.
    private void play(Scene scene) {
        for (LightShowRig.Lamp lamp : this.rig.lamps) {
            BlockPos pos = lamp.pos();
            if (!this.rig.level.isLoaded(pos)
                    || !(this.rig.level.getBlockEntity(pos) instanceof StageLightBlockEntity entity)) {
                continue;
            }
            LampSettings settings = LampSettings.of(entity.kind());
            Consumer<LampSettings> look = scene.looks().get(lamp.group());
            if (look == null) {
                settings.on = false;
            } else {
                look.accept(settings);
                if (settings.aim == LampSettings.Aim.SPOT) {
                    settings.spot = this.rig.stage;
                }
            }
            int count = Math.max(1, this.rig.count(lamp.group()));
            settings.phase = switch (lamp.group()) {
                case HEADS, PARS -> (double) lamp.index() / count;
                case LASERS, SEARCH, STROBES -> lamp.index() % 2 * 0.5;
                case DISCO -> 0.0;
            };
            entity.edit(settings.write(), false);
        }
    }

    private static Consumer<LampSettings> look(LampKind kind, String id) {
        for (LampLooks.Look look : LampLooks.of(kind)) {
            if (look.id().equals(id)) {
                return look.apply();
            }
        }
        throw new IllegalArgumentException(id);
    }

    // A wash look that also turns the floor lights to light the stage.
    private static Consumer<LampSettings> wash(String id) {
        return look(LampKind.PAR, id).andThen(settings -> settings.aim = LampSettings.Aim.SPOT);
    }

    private static Scene scene(int seconds, Object... pairs) {
        Map<Group, Consumer<LampSettings>> looks = new EnumMap<>(Group.class);
        for (int i = 0; i < pairs.length; i += 2) {
            @SuppressWarnings("unchecked")
            Consumer<LampSettings> look = (Consumer<LampSettings>) pairs[i + 1];
            looks.put((Group) pairs[i], look);
        }
        return new Scene(seconds, looks);
    }
}
