package nl.tivek.multiversepowers.schematic.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.schematic.SchematicRules;
import nl.tivek.multiversepowers.schematic.format.SchematicReader;
import nl.tivek.multiversepowers.schematic.net.SchematicFilePayload;

// A schematic from the player's own computer going to the server's library, a few parts a tick so the connection
// never chokes on a big one.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class SchematicUpload {
    private static final int PARTS_A_TICK = 8;

    @Nullable
    private static byte[] bytes;
    private static String target = "";
    private static int next;
    private static int parts;

    private SchematicUpload() {
    }

    static boolean busy() {
        return bytes != null;
    }

    static float progress() {
        return parts == 0 ? 0.0F : next / (float) parts;
    }

    // Sends a file into `folder` of the library, under its own name; it is read away from the game's thread.
    static void send(Path file, String folder) {
        String name = file.getFileName().toString();
        if (!SchematicReader.readable(name)) {
            SchematicClient.say(Component.translatable(SchematicClient.PREFIX + "error.kind", name), true);
            return;
        }
        if (busy()) {
            return;
        }
        long largest = SchematicRules.largestFile();
        Util.ioPool().execute(() -> {
            byte[] read = null;
            String error = null;
            try {
                if (Files.size(file) > largest) {
                    error = "error.too_large";
                } else {
                    read = Files.readAllBytes(file);
                }
            } catch (IOException e) {
                error = "error.read";
            }
            byte[] got = read;
            String failed = error;
            Minecraft.getInstance().execute(() -> {
                if (failed != null || got == null) {
                    SchematicClient.say(Component.translatable(SchematicClient.PREFIX
                            + (failed == null ? "error.read" : failed), name), true);
                    return;
                }
                bytes = got;
                target = folder.isEmpty() ? name : folder + "/" + name;
                next = 0;
                parts = Math.max(1, (got.length + SchematicFilePayload.UP - 1) / SchematicFilePayload.UP);
                SchematicClient.say(Component.translatable(SchematicClient.PREFIX + "notice.importing", name), false);
            });
        });
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        byte[] file = bytes;
        if (file == null) {
            return;
        }
        if (Minecraft.getInstance().getConnection() == null) {
            bytes = null;
            return;
        }
        for (int k = 0; k < PARTS_A_TICK && next < parts; k++, next++) {
            int from = next * SchematicFilePayload.UP;
            PacketDistributor.sendToServer(new SchematicFilePayload(SchematicFilePayload.UPLOAD, target, next, parts,
                    Arrays.copyOfRange(file, from, Math.min(file.length, from + SchematicFilePayload.UP))));
        }
        if (next >= parts) {
            bytes = null;
        }
    }
}
