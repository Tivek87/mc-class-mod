package nl.tivek.multiversepowers.schematic.paste;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.schematic.Schematic;
import nl.tivek.multiversepowers.schematic.SchematicPlacement;
import nl.tivek.multiversepowers.schematic.SchematicServer;
import nl.tivek.multiversepowers.schematic.net.SchematicStatusPayload;

// Every player's paste under way and their last one, kept to be undone; one at a time each.
public final class SchematicPastes {
    public static final int ALL = SchematicPaste.ALL;
    public static final int SOLID = SchematicPaste.SOLID;
    public static final int EMPTY = SchematicPaste.EMPTY;
    public static final int CREATURES = SchematicPaste.CREATURES;
    public static final int CONTENTS = SchematicPaste.CONTENTS;
    public static final int PHYSICS = SchematicPaste.PHYSICS;

    private static final Map<UUID, SchematicPaste> RUNNING = new HashMap<>();
    private static final Map<UUID, SchematicUndo> UNDOING = new HashMap<>();
    private static final Map<UUID, PasteUndo> LAST = new HashMap<>();

    private SchematicPastes() {
    }

    public static boolean busy(ServerPlayer player) {
        return RUNNING.containsKey(player.getUUID()) || UNDOING.containsKey(player.getUUID());
    }

    public static void start(ServerPlayer player, Schematic schematic, SchematicPlacement placement, int mode,
            int flags) {
        if (busy(player)) {
            SchematicServer.notice(player, "error.busy", "", "", true);
            return;
        }
        SchematicPaste paste = new SchematicPaste(player, schematic, placement, Math.floorMod(mode, 3), flags);
        RUNNING.put(player.getUUID(), paste);
        LAST.remove(player.getUUID());
        paste.report(player, SchematicStatusPayload.RUNNING);
        Effects.start(player.serverLevel(), paste);
    }

    static void ended(SchematicPaste paste) {
        RUNNING.remove(paste.owner);
        if (paste.undo.count > 0 || !paste.undo.creatures.isEmpty()) {
            if (paste.undo.full) {
                SchematicServer.notice(paste.owner, "notice.no_undo", paste.schematic.name, "", false);
            } else {
                LAST.put(paste.owner, paste.undo);
            }
        }
    }

    static void undone(SchematicUndo undo) {
        UNDOING.remove(undo.owner);
    }

    public static void cancel(ServerPlayer player) {
        SchematicPaste paste = RUNNING.get(player.getUUID());
        SchematicUndo undo = UNDOING.get(player.getUUID());
        if (paste != null) {
            paste.cancelled = true;
        } else if (undo != null) {
            undo.cancelled = true;
        } else {
            SchematicServer.notice(player, "error.nothing", "", "", true);
        }
    }

    public static void undo(ServerPlayer player) {
        if (busy(player)) {
            SchematicServer.notice(player, "error.busy", "", "", true);
            return;
        }
        PasteUndo last = LAST.remove(player.getUUID());
        if (last == null) {
            SchematicServer.notice(player, "error.no_undo", "", "", true);
            return;
        }
        ServerLevel level = player.server.getLevel(last.level);
        if (level == null) {
            SchematicServer.notice(player, "error.no_undo", "", "", true);
            return;
        }
        SchematicUndo undo = new SchematicUndo(player, last);
        UNDOING.put(player.getUUID(), undo);
        Effects.start(level, undo);
    }

    public static boolean canUndo(ServerPlayer player) {
        return LAST.containsKey(player.getUUID());
    }

    public static void clear() {
        RUNNING.clear();
        UNDOING.clear();
        LAST.clear();
    }
}
