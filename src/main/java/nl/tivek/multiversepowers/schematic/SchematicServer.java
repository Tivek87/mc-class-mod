package nl.tivek.multiversepowers.schematic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.content.ModItems;
import nl.tivek.multiversepowers.schematic.format.SchematicException;
import nl.tivek.multiversepowers.schematic.format.SchematicReader;
import nl.tivek.multiversepowers.schematic.net.SchematicAskPayload;
import nl.tivek.multiversepowers.schematic.net.SchematicFilePayload;
import nl.tivek.multiversepowers.schematic.net.SchematicListPayload;
import nl.tivek.multiversepowers.schematic.net.SchematicNoticePayload;
import nl.tivek.multiversepowers.schematic.net.SchematicPlacePayload;
import nl.tivek.multiversepowers.schematic.paste.SchematicPastes;

// The server's side of the Schematic Wand: the menu's requests on the library, files either way, placing and undoing.
// Files are read and written away from the server thread, so a large one never holds the game up.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class SchematicServer {
    private static final class Upload {
        final String path;
        final int parts;
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int next;

        Upload(String path, int parts) {
            this.path = path;
            this.parts = parts;
        }
    }

    private record Kept(Path path, long modified, Schematic schematic) {
    }

    private static final Map<UUID, Upload> UPLOADS = new HashMap<>();
    private static final Set<UUID> READING = new HashSet<>();
    // The last schematic read for a paste, for the next paste of the same file.
    @Nullable
    private static volatile Kept kept;

    private SchematicServer() {
    }

    public static void onAsk(ServerPlayer player, SchematicAskPayload ask) {
        if (!SchematicRules.trusted(player)) {
            notice(player, "error.not_allowed", "", "", true);
            return;
        }
        Path path = SchematicLibrary.resolve(ask.path());
        if (path == null) {
            notice(player, "error.path", ask.path(), "", true);
            return;
        }
        switch (ask.action()) {
            case SchematicAskPayload.LIST -> list(player, path);
            case SchematicAskPayload.FOLDER -> {
                try {
                    Path made = SchematicLibrary.makeFolder(path, ask.text());
                    notice(player, "notice.folder", made.getFileName().toString(), "", false);
                } catch (IOException e) {
                    notice(player, "error.folder", ask.text(), "", true);
                }
                list(player, path);
            }
            case SchematicAskPayload.DELETE -> {
                try {
                    SchematicLibrary.delete(path);
                    notice(player, "notice.deleted", path.getFileName().toString(), "", false);
                } catch (IOException e) {
                    notice(player, "error.delete", ask.path(), "", true);
                }
                list(player, path.getParent());
            }
            case SchematicAskPayload.VIEW -> send(player, path, ask.path(), SchematicFilePayload.VIEW);
            case SchematicAskPayload.DOWNLOAD -> send(player, path, ask.path(), SchematicFilePayload.DOWNLOAD);
            case SchematicAskPayload.UNDO -> SchematicPastes.undo(player);
            case SchematicAskPayload.CANCEL -> SchematicPastes.cancel(player);
            default -> {
            }
        }
    }

    private static void list(ServerPlayer player, Path folder) {
        try {
            String root = player.getServer() != null && player.getServer().isDedicatedServer()
                    ? SchematicLibrary.FOLDER + "/" : SchematicLibrary.root().toString();
            PacketDistributor.sendToPlayer(player, new SchematicListPayload(SchematicLibrary.relative(folder), root,
                    SchematicLibrary.list(folder)));
        } catch (IOException e) {
            notice(player, "error.list", "", "", true);
        }
    }

    // A file to the player in parts, read away from the server thread.
    private static void send(ServerPlayer player, Path path, String relative, int purpose) {
        MinecraftServer server = player.getServer();
        UUID id = player.getUUID();
        long largest = SchematicRules.largestFile();
        Util.ioPool().execute(() -> {
            byte[] bytes;
            try {
                if (!Files.isRegularFile(path) || Files.size(path) > largest) {
                    throw new IOException("size");
                }
                bytes = Files.readAllBytes(path);
            } catch (IOException e) {
                server.execute(() -> notice(id, "error.read", relative, "", true));
                return;
            }
            server.execute(() -> {
                ServerPlayer now = server.getPlayerList().getPlayer(id);
                if (now == null) {
                    return;
                }
                int parts = Math.max(1, (bytes.length + SchematicFilePayload.DOWN - 1) / SchematicFilePayload.DOWN);
                for (int part = 0; part < parts; part++) {
                    int from = part * SchematicFilePayload.DOWN;
                    PacketDistributor.sendToPlayer(now, new SchematicFilePayload(purpose, relative, part, parts,
                            Arrays.copyOfRange(bytes, from, Math.min(bytes.length, from + SchematicFilePayload.DOWN))));
                }
            });
        });
    }

    // A part of a file a player imports; the last one is written next to any of the same name.
    public static void onFile(ServerPlayer player, SchematicFilePayload payload) {
        if (payload.purpose() != SchematicFilePayload.UPLOAD || !SchematicRules.trusted(player)) {
            return;
        }
        UUID id = player.getUUID();
        if (payload.part() == 0) {
            if ((long) payload.parts() * SchematicFilePayload.UP > SchematicRules.largestFile() + SchematicFilePayload.UP
                    || payload.bytes().length > SchematicFilePayload.UP) {
                notice(player, "error.too_large", payload.path(), "", true);
                UPLOADS.remove(id);
                return;
            }
            UPLOADS.put(id, new Upload(payload.path(), payload.parts()));
        }
        Upload upload = UPLOADS.get(id);
        if (upload == null || payload.part() != upload.next || !upload.path.equals(payload.path())
                || payload.bytes().length > SchematicFilePayload.UP) {
            UPLOADS.remove(id);
            return;
        }
        upload.bytes.writeBytes(payload.bytes());
        upload.next++;
        if (upload.next < upload.parts) {
            return;
        }
        UPLOADS.remove(id);
        int slash = upload.path.lastIndexOf('/');
        Path folder = SchematicLibrary.resolve(slash < 0 ? "" : upload.path.substring(0, slash));
        String name = upload.path.substring(slash + 1);
        if (folder == null || !SchematicReader.readable(name)) {
            notice(player, "error.import", name, "", true);
            return;
        }
        byte[] bytes = upload.bytes.toByteArray();
        MinecraftServer server = player.getServer();
        Util.ioPool().execute(() -> {
            try {
                Path saved = SchematicLibrary.save(folder, name, bytes);
                server.execute(() -> {
                    notice(id, "notice.imported", saved.getFileName().toString(), "", false);
                    ServerPlayer now = server.getPlayerList().getPlayer(id);
                    if (now != null) {
                        list(now, folder);
                    }
                });
            } catch (IOException e) {
                server.execute(() -> notice(id, "error.import", name, "", true));
            }
        });
    }

    // Reads the schematic away from the server thread (or takes the one kept), then starts its paste.
    public static void onPlace(ServerPlayer player, SchematicPlacePayload place) {
        if (!SchematicRules.allowed(player)) {
            notice(player, "error.not_allowed", "", "", true);
            return;
        }
        Path path = SchematicLibrary.resolve(place.path());
        UUID id = player.getUUID();
        if (path == null || !Files.isRegularFile(path)) {
            notice(player, "error.read", place.path(), "", true);
            return;
        }
        if (SchematicPastes.busy(player) || !READING.add(id)) {
            notice(player, "error.busy", "", "", true);
            return;
        }
        MinecraftServer server = player.getServer();
        int largest = SchematicRules.largest();
        long largestFile = SchematicRules.largestFile();
        Util.backgroundExecutor().execute(() -> {
            Schematic schematic = null;
            String error = null;
            String[] detail = { place.path(), "" };
            try {
                long modified = Files.getLastModifiedTime(path).toMillis();
                Kept last = kept;
                if (last != null && last.path().equals(path) && last.modified() == modified) {
                    schematic = last.schematic();
                } else {
                    if (Files.size(path) > largestFile) {
                        throw new IOException("size");
                    }
                    schematic = SchematicReader.read(Files.readAllBytes(path), path.getFileName().toString(),
                            largest);
                    kept = new Kept(path, modified, schematic);
                }
            } catch (SchematicException e) {
                error = "error." + e.key();
                if (!e.arg(0).isEmpty()) {
                    detail[0] = e.arg(0);
                    detail[1] = e.arg(1);
                }
            } catch (IOException | RuntimeException e) {
                error = "error.read";
            }
            Schematic read = schematic;
            String failed = error;
            server.execute(() -> {
                READING.remove(id);
                ServerPlayer now = server.getPlayerList().getPlayer(id);
                if (now == null) {
                    return;
                }
                if (failed != null || read == null) {
                    notice(now, failed == null ? "error.read" : failed, detail[0], detail[1], true);
                    return;
                }
                SchematicPlacement placement = new SchematicPlacement(place.origin(),
                        Rotation.values()[Math.floorMod(place.rotation(), 4)],
                        Mirror.values()[Math.floorMod(place.mirror(), 3)]);
                SchematicPastes.start(now, read, placement, place.mode(), place.flags());
            });
        });
    }

    // The wand stays out of the hands of anyone who may not use it, or who has become a character.
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0
                || !holds(player) || SchematicRules.allowed(player)) {
            return;
        }
        Inventory inventory = player.getInventory();
        boolean moved = false;
        for (int hand = 0; hand < 2; hand++) {
            ItemStack stack = hand == 0 ? inventory.getSelected() : inventory.offhand.get(0);
            if (!stack.is(ModItems.SCHEMATIC_WAND.get())) {
                continue;
            }
            for (int slot = Inventory.getSelectionSize(); slot < inventory.items.size(); slot++) {
                if (inventory.items.get(slot).isEmpty()) {
                    inventory.items.set(slot, stack.copy());
                    if (hand == 0) {
                        inventory.items.set(inventory.selected, ItemStack.EMPTY);
                    } else {
                        inventory.offhand.set(0, ItemStack.EMPTY);
                    }
                    moved = true;
                    break;
                }
            }
        }
        if (moved) {
            notice(player, SchematicRules.trusted(player) ? "error.character" : "error.not_allowed", "", "", true);
        }
    }

    private static boolean holds(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.SCHEMATIC_WAND.get())
                || player.getOffhandItem().is(ModItems.SCHEMATIC_WAND.get());
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UPLOADS.remove(event.getEntity().getUUID());
    }

    public static void notice(ServerPlayer player, String key, String first, String second, boolean bad) {
        PacketDistributor.sendToPlayer(player, new SchematicNoticePayload(key, first, second, bad));
    }

    public static void notice(UUID id, String key, String first, String second, boolean bad) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(id);
        if (player != null) {
            notice(player, key, first, second, bad);
        }
    }

    public static void clear() {
        UPLOADS.clear();
        READING.clear();
        kept = null;
    }
}
