package nl.tivek.multiversepowers.schematic.blueprint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.schematic.Schematic;
import nl.tivek.multiversepowers.schematic.SchematicPlacement;
import nl.tivek.multiversepowers.schematic.SchematicRules;
import nl.tivek.multiversepowers.schematic.SchematicServer;
import nl.tivek.multiversepowers.schematic.format.SchematicReader;
import nl.tivek.multiversepowers.schematic.net.BlueprintAskPayload;
import nl.tivek.multiversepowers.schematic.net.BlueprintListPayload;
import nl.tivek.multiversepowers.schematic.net.SchematicFilePayload;
import nl.tivek.multiversepowers.schematic.paste.SchematicPastes;

// The server's side of blueprints: a wand holder sets one down (its file copied into the world's own folder, so it
// stays whatever happens to the library), takes it away or has it built for real; every player is sent the list and,
// asked, a blueprint's file, so everyone sees it and can build along.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Blueprints {
    private static final int RESEND = 200;
    private static final Map<String, Long> SENT = new HashMap<>();

    private Blueprints() {
    }

    public static void clear() {
        SENT.clear();
    }

    private static Path folder(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(MultiversePowers.MODID + "_blueprints");
    }

    // Sets down `schematic`, read from `source`, where `placement` puts it, in the player's own world.
    public static void add(ServerPlayer player, Schematic schematic, Path source, SchematicPlacement placement) {
        MinecraftServer server = player.server;
        BlueprintData data = BlueprintData.get(server);
        String name = source.getFileName().toString();
        if (data.all().size() >= SchematicRules.blueprints()) {
            SchematicServer.notice(player, "error.blueprints_full", String.valueOf(SchematicRules.blueprints()), "",
                    true);
            return;
        }
        int dot = name.lastIndexOf('.');
        int id = data.takeId();
        String file = id + (dot < 0 ? "" : name.substring(dot));
        Blueprint blueprint = new Blueprint(id, schematic.name, player.getGameProfile().getName(),
                player.level().dimension().location(), placement.origin(), placement.rotation().ordinal(),
                placement.mirror().ordinal(), schematic.sizeX, schematic.sizeY, schematic.sizeZ, file);
        UUID owner = player.getUUID();
        Path folder = folder(server);
        Util.ioPool().execute(() -> {
            try {
                Files.createDirectories(folder);
                Files.copy(source, folder.resolve(file), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                server.execute(() -> SchematicServer.notice(owner, "error.blueprint", name, "", true));
                return;
            }
            server.execute(() -> {
                // Others may have set theirs down while this one was copied.
                if (BlueprintData.get(server).all().size() >= SchematicRules.blueprints()) {
                    deleteFile(folder.resolve(file));
                    SchematicServer.notice(owner, "error.blueprints_full", String.valueOf(SchematicRules.blueprints()),
                            "", true);
                    return;
                }
                BlueprintData.get(server).add(blueprint);
                sync(server);
                SchematicServer.notice(owner, "notice.blueprint", schematic.name, "", false);
            });
        });
    }

    public static void onAsk(ServerPlayer player, BlueprintAskPayload ask) {
        MinecraftServer server = player.server;
        BlueprintData data = BlueprintData.get(server);
        Blueprint blueprint = data.get(ask.id());
        if (blueprint == null) {
            sync(player);
            return;
        }
        Path path = folder(server).resolve(blueprint.file());
        switch (ask.action()) {
            case BlueprintAskPayload.FILE -> {
                if (sentLately(player, blueprint)) {
                    return;
                }
                SchematicServer.send(player, path, blueprint.file(), SchematicFilePayload.BLUEPRINT);
            }
            case BlueprintAskPayload.REMOVE -> {
                if (!SchematicRules.trusted(player)) {
                    SchematicServer.notice(player, "error.not_allowed", "", "", true);
                    return;
                }
                remove(server, blueprint);
                SchematicServer.notice(player, "notice.blueprint_removed", blueprint.name(), "", false);
            }
            case BlueprintAskPayload.BUILD -> build(player, blueprint, path);
            default -> {
            }
        }
    }

    // A player's game asks each blueprint's file once; asked again within RESEND ticks it is not sent again.
    private static boolean sentLately(ServerPlayer player, Blueprint blueprint) {
        long now = player.server.getTickCount();
        if (SENT.size() > 1024) {
            SENT.values().removeIf(at -> now - at > RESEND);
        }
        String key = player.getUUID() + ":" + blueprint.id();
        Long at = SENT.get(key);
        if (at != null && now - at < RESEND) {
            return true;
        }
        SENT.put(key, now);
        return false;
    }

    private static void deleteFile(Path path) {
        Util.ioPool().execute(() -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                MultiversePowers.LOGGER.warn("Could not delete blueprint file {}", path);
            }
        });
    }

    private static void remove(MinecraftServer server, Blueprint blueprint) {
        BlueprintData.get(server).remove(blueprint.id());
        deleteFile(folder(server).resolve(blueprint.file()));
        sync(server);
    }

    // Builds it for real as the wand would, in its own world, and takes the blueprint away.
    private static void build(ServerPlayer player, Blueprint blueprint, Path path) {
        if (!SchematicRules.allowed(player)) {
            SchematicServer.notice(player, "error.not_allowed", "", "", true);
            return;
        }
        if (!player.level().dimension().location().equals(blueprint.level())) {
            SchematicServer.notice(player, "error.blueprint_world", blueprint.name(), "", true);
            return;
        }
        if (SchematicPastes.busy(player)) {
            SchematicServer.notice(player, "error.busy", "", "", true);
            return;
        }
        MinecraftServer server = player.server;
        UUID id = player.getUUID();
        int largest = SchematicRules.largest();
        Util.backgroundExecutor().execute(() -> {
            Schematic read;
            try {
                read = SchematicReader.read(Files.readAllBytes(path), blueprint.file(), largest);
            } catch (Exception e) {
                server.execute(() -> SchematicServer.notice(id, "error.read", blueprint.name(), "", true));
                return;
            }
            server.execute(() -> {
                ServerPlayer now = server.getPlayerList().getPlayer(id);
                if (now == null || BlueprintData.get(server).get(blueprint.id()) == null) {
                    return;
                }
                SchematicPastes.start(now, read, blueprint.placement(), SchematicPastes.SOLID,
                        SchematicPastes.CREATURES | SchematicPastes.CONTENTS);
                if (SchematicPastes.busy(now)) {
                    remove(server, blueprint);
                }
            });
        });
    }

    public static void sync(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(new BlueprintListPayload(BlueprintData.get(server).all()));
    }

    private static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new BlueprintListPayload(BlueprintData.get(player.server).all()));
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }
}
