package nl.tivek.multiversepowers.schematic.client;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.content.ModItems;
import nl.tivek.multiversepowers.schematic.Schematic;
import nl.tivek.multiversepowers.schematic.SchematicLibrary;
import nl.tivek.multiversepowers.schematic.SchematicRules;
import nl.tivek.multiversepowers.schematic.client.blueprint.ClientBlueprints;
import nl.tivek.multiversepowers.schematic.client.hologram.Hologram;
import nl.tivek.multiversepowers.schematic.client.menu.LibraryPage;
import nl.tivek.multiversepowers.schematic.client.menu.PreviewModel;
import nl.tivek.multiversepowers.schematic.format.SchematicException;
import nl.tivek.multiversepowers.schematic.format.SchematicReader;
import nl.tivek.multiversepowers.schematic.net.SchematicAskPayload;
import nl.tivek.multiversepowers.schematic.net.SchematicFilePayload;
import nl.tivek.multiversepowers.schematic.net.SchematicListPayload;
import nl.tivek.multiversepowers.schematic.net.SchematicNoticePayload;
import nl.tivek.multiversepowers.schematic.net.SchematicStatusPayload;

// What the wand holder's own game knows: the library folder last listed, the schematic being looked at (read here from
// the file the server sent, away from the game's thread), the last paste's progress and the last notice.
public final class SchematicClient {
    public static final String PREFIX = "schematic." + MultiversePowers.MODID + ".";

    private static final class Incoming {
        final int purpose;
        final String path;
        final int parts;
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int next;

        Incoming(int purpose, String path, int parts) {
            this.purpose = purpose;
            this.path = path;
            this.parts = parts;
        }
    }

    public static String folder = "";
    public static String root = "";
    public static List<SchematicLibrary.Entry> entries = List.of();
    public static long listed;
    @Nullable
    public static Schematic viewed;
    @Nullable
    public static String viewedPath;
    @Nullable
    public static String loadingPath;
    // How far the file of the schematic asked for has come in, 0 to 1.
    public static float loading;
    @Nullable
    public static Component viewError;
    @Nullable
    static SchematicStatusPayload status;
    static long statusAt;
    @Nullable
    public static Component notice;
    public static boolean noticeBad;
    public static long noticeAt;
    @Nullable
    private static Incoming incoming;
    private static int asked;

    private SchematicClient() {
    }

    // The host, a listed owner or an operator, as the server checks again; and no character.
    public static boolean allowed(LocalPlayer player) {
        return trusted(player) && ClientCharacter.active() == null;
    }

    public static boolean trusted(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.hasSingleplayerServer() || PowerRules.isOwner(player.getGameProfile())
                || player.hasPermissions(2);
    }

    public static boolean holding(LocalPlayer player) {
        return player.getMainHandItem().is(ModItems.SCHEMATIC_WAND.get());
    }

    public static void ask(int action, String path, String text) {
        PacketDistributor.sendToServer(new SchematicAskPayload(action, path, text));
    }

    public static void list(String folder) {
        ask(SchematicAskPayload.LIST, folder, "");
    }

    // Asks for a schematic to look at; it is read once its file is in.
    public static void view(String path) {
        if (path.equals(viewedPath) && viewed != null || path.equals(loadingPath)) {
            return;
        }
        loadingPath = path;
        loading = 0.0F;
        viewError = null;
        asked++;
        ask(SchematicAskPayload.VIEW, path, "");
    }

    // A file or folder renamed from the menu: the chosen schematic and the hologram follow it.
    private static void renamed(String from, String to) {
        viewedPath = moved(viewedPath, from, to);
        Hologram.path = moved(Hologram.path, from, to);
        LibraryPage.selected = moved(LibraryPage.selected, from, to);
        Hologram.renamed(from, to);
    }

    @Nullable
    static String moved(@Nullable String path, String from, String to) {
        if (path == null) {
            return null;
        }
        return path.equals(from) ? to : path.startsWith(from + "/") ? to + path.substring(from.length()) : path;
    }

    public static void onList(SchematicListPayload payload) {
        folder = payload.folder();
        root = payload.root();
        entries = payload.entries();
        listed = Util.getMillis();
    }

    public static void onFile(SchematicFilePayload payload) {
        if (payload.part() == 0) {
            incoming = new Incoming(payload.purpose(), payload.path(), payload.parts());
        }
        Incoming file = incoming;
        if (file == null || payload.part() != file.next || !file.path.equals(payload.path())) {
            incoming = null;
            return;
        }
        file.bytes.writeBytes(payload.bytes());
        file.next++;
        if (file.purpose == SchematicFilePayload.VIEW && file.path.equals(loadingPath)) {
            loading = file.next / (float) file.parts;
        }
        if (file.next < file.parts) {
            return;
        }
        incoming = null;
        byte[] bytes = file.bytes.toByteArray();
        String name = file.path.substring(file.path.lastIndexOf('/') + 1);
        if (file.purpose == SchematicFilePayload.DOWNLOAD) {
            FileDialogs.save(name, bytes);
        } else if (file.purpose == SchematicFilePayload.BLUEPRINT) {
            ClientBlueprints.onFile(file.path, bytes);
        } else if (file.path.equals(loadingPath)) {
            read(file.path, name, bytes);
        }
    }

    private static void read(String path, String name, byte[] bytes) {
        int ask = asked;
        int largest = SchematicRules.largest();
        CompletableFuture.supplyAsync(() -> {
            try {
                return SchematicReader.read(bytes, name, largest);
            } catch (SchematicException e) {
                throw new IllegalStateException(e);
            }
        }, Util.backgroundExecutor()).whenComplete((schematic, error) -> Minecraft.getInstance().execute(() -> {
            if (ask != asked) {
                return;
            }
            loadingPath = null;
            if (schematic == null) {
                Throwable cause = error;
                while (cause != null && !(cause instanceof SchematicException)) {
                    cause = cause.getCause();
                }
                viewError = cause instanceof SchematicException bad ? bad.text()
                        : Component.translatable(PREFIX + "error.unreadable", name);
                viewed = null;
                viewedPath = null;
                return;
            }
            viewed = schematic;
            viewedPath = path;
            PreviewModel.LIBRARY.build(schematic);
        }));
    }

    public static void onStatus(SchematicStatusPayload payload) {
        status = payload;
        statusAt = Util.getMillis();
    }

    public static void onNotice(SchematicNoticePayload payload) {
        if (payload.key().equals("error.not_allowed") || payload.key().equals("error.character")) {
            Hologram.hide();
        }
        if (payload.key().startsWith("error.") && payload.first().equals(loadingPath)) {
            loadingPath = null;
            viewError = payload.text();
            viewed = null;
            viewedPath = null;
        }
        if (payload.key().equals("notice.renamed")) {
            renamed(payload.first(), payload.second());
        }
        say(payload.text(), payload.bad());
    }

    public static void say(Component text, boolean bad) {
        notice = text;
        noticeBad = bad;
        noticeAt = Util.getMillis();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Minecraft.getInstance().screen == null) {
            player.displayClientMessage(text, true);
        }
    }

    // A paste under way, or just ended.
    @Nullable
    public static SchematicStatusPayload status() {
        SchematicStatusPayload now = status;
        if (now == null) {
            return null;
        }
        return now.over() && Util.getMillis() - statusAt > 4000L ? null : now;
    }

    public static void clear() {
        folder = "";
        root = "";
        entries = List.of();
        listed = 0L;
        viewed = null;
        viewedPath = null;
        loadingPath = null;
        viewError = null;
        status = null;
        notice = null;
        incoming = null;
        Hologram.hide();
        Hologram.forgetSpots();
        AreaSelection.stop();
        AreaSelection.clear();
        PreviewModel.LIBRARY.release();
        PreviewModel.BLUEPRINT.release();
    }
}
