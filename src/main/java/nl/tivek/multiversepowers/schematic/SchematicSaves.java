package nl.tivek.multiversepowers.schematic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.tivek.multiversepowers.schematic.blueprint.Blueprints;
import nl.tivek.multiversepowers.schematic.format.SchematicReader;
import nl.tivek.multiversepowers.schematic.net.SchematicSavePayload;

// An area of the world saved as a schematic: the blocks between two corners (and the creatures in it, if asked), as
// the game's own structure file (.nbt) in a folder of the library, and, if asked, set down as a blueprint where it
// stands. The blocks are read on the server's thread (an area is held to the world setting largestSave), the file
// written away from it.
public final class SchematicSaves {
    private SchematicSaves() {
    }

    public static void onSave(ServerPlayer player, SchematicSavePayload ask) {
        if (!SchematicRules.allowed(player)) {
            SchematicServer.notice(player, SchematicRules.trusted(player) ? "error.character" : "error.not_allowed",
                    "", "", true);
            return;
        }
        Path folder = SchematicLibrary.resolve(ask.folder());
        if (folder == null || Files.exists(folder) && !Files.isDirectory(folder)) {
            SchematicServer.notice(player, "error.path", ask.folder(), "", true);
            return;
        }
        String name = SchematicLibrary.clean(ask.name());
        if (name.toLowerCase(Locale.ROOT).endsWith(".nbt")) {
            name = name.substring(0, name.length() - 4).trim();
        }
        if (name.isEmpty()) {
            SchematicServer.notice(player, "error.save_name", "", "", true);
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos min = BlockPos.min(ask.from(), ask.to());
        BlockPos max = BlockPos.max(ask.from(), ask.to());
        if (min.getY() < level.getMinBuildHeight() || max.getY() >= level.getMaxBuildHeight()) {
            SchematicServer.notice(player, "error.save_height", "", "", true);
            return;
        }
        Vec3i size = new Vec3i(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ()
                + 1);
        long most = SchematicRules.largestSave();
        // Each side first, so the product of three far corners cannot run past a long.
        long volume = size.getX() > most || size.getZ() > most ? Long.MAX_VALUE
                : (long) size.getX() * size.getY() * size.getZ();
        if (volume > most) {
            SchematicServer.notice(player, "error.save_big", String.format("%,.0f", (double) size.getX() * size.getY()
                    * size.getZ()), String.format("%,d", most), true);
            return;
        }
        for (int cx = min.getX() >> 4; cx <= max.getX() >> 4; cx++) {
            for (int cz = min.getZ() >> 4; cz <= max.getZ() >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    SchematicServer.notice(player, "error.save_unloaded", "", "", true);
                    return;
                }
            }
        }
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, min, size, (ask.flags() & SchematicSavePayload.CREATURES) != 0,
                Blocks.STRUCTURE_VOID);
        template.setAuthor(player.getGameProfile().getName());
        CompoundTag tag = template.save(new CompoundTag());
        MinecraftServer server = player.server;
        UUID id = player.getUUID();
        String fileName = name + ".nbt";
        boolean blueprint = (ask.flags() & SchematicSavePayload.BLUEPRINT) != 0;
        long largestFile = SchematicRules.largestFile();
        int largest = SchematicRules.largest();
        Util.ioPool().execute(() -> {
            Path saved;
            byte[] bytes;
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                NbtIo.writeCompressed(tag, out);
                bytes = out.toByteArray();
                if (bytes.length > largestFile) {
                    throw new IOException("size");
                }
                saved = SchematicLibrary.save(folder, fileName, bytes);
            } catch (IOException e) {
                server.execute(() -> SchematicServer.notice(id, "error.save", fileName, "", true));
                return;
            }
            Schematic read = null;
            if (blueprint) {
                try {
                    read = SchematicReader.read(bytes, saved.getFileName().toString(), largest);
                } catch (Exception e) {
                    read = null;
                }
            }
            Schematic plan = read;
            server.execute(() -> {
                ServerPlayer now = server.getPlayerList().getPlayer(id);
                if (now == null) {
                    return;
                }
                SchematicServer.notice(now, "notice.area_saved", SchematicLibrary.relative(saved),
                        String.format("%,d", volume), false);
                SchematicServer.list(now, folder);
                if (plan != null) {
                    Blueprints.add(now, plan, saved, new SchematicPlacement(min, Rotation.NONE, Mirror.NONE));
                } else if (blueprint) {
                    SchematicServer.notice(now, "error.blueprint", saved.getFileName().toString(), "", true);
                }
            });
        });
    }
}
