package nl.tivek.multiversepowers.config.transfer;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.command.CommandAliases;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.config.WorldSettings;
import nl.tivek.multiversepowers.faction.mob.MobStats;

// A world's settings carried to another world in one go. Export: every world settings file (the rules, stamina, mobs,
// commands and every character) packed in one zip and sent to a player who may change them, who keeps it on their own
// computer. Import: such a zip sent back, each value in it set as the settings pages would (only what its setting
// takes, the owners list never), then saved and sent to everyone. A zip goes in parts (PART bytes), so any size fits.
public final class SettingsTransfer {
    public static final int PART = 30000;
    // The most a zip and a file in it may be, and parts in one import.
    private static final int MOST = 4 * 1024 * 1024;
    private static final int MOST_PARTS = MOST / PART + 1;
    private static final Map<UUID, ByteArrayOutputStream> INCOMING = new HashMap<>();

    private SettingsTransfer() {
    }

    public static void export(ServerPlayer player, String name) {
        if (!WorldSettings.mayEdit(player)) {
            player.displayClientMessage(Component.translatable("config." + MultiversePowers.MODID + ".denied"), false);
            return;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (String file : ModConfigs.worldFiles().keySet()) {
                ModConfig config = net.neoforged.fml.config.ModConfigs.getFileMap().get(file);
                if (config == null) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry(file));
                zip.write(Files.readAllBytes(config.getFullPath()));
                zip.closeEntry();
            }
        } catch (IOException | IllegalStateException e) {
            MultiversePowers.LOGGER.warn("Could not pack the world settings for {}", player.getGameProfile().getName(),
                    e);
            tell(player, "failed", 0);
            return;
        }
        byte[] all = bytes.toByteArray();
        int parts = Math.max(1, (all.length + PART - 1) / PART);
        for (int part = 0; part < parts; part++) {
            int from = part * PART;
            byte[] piece = Arrays.copyOfRange(all, from, Math.min(all.length, from + PART));
            PacketDistributor.sendToPlayer(player, new SettingsExportPayload(name, part, parts, piece));
        }
    }

    // One part of a zip a player imports; the last one sets it.
    public static void receive(ServerPlayer player, SettingsImportPayload payload) {
        if (!WorldSettings.mayEdit(player)) {
            INCOMING.remove(player.getUUID());
            if (payload.part() == 0) {
                player.displayClientMessage(Component.translatable("config." + MultiversePowers.MODID + ".denied"),
                        false);
            }
            return;
        }
        if (payload.part() == 0) {
            INCOMING.put(player.getUUID(), new ByteArrayOutputStream());
        }
        ByteArrayOutputStream bytes = INCOMING.get(player.getUUID());
        if (bytes == null || payload.parts() > MOST_PARTS || bytes.size() + payload.bytes().length > MOST) {
            INCOMING.remove(player.getUUID());
            return;
        }
        bytes.writeBytes(payload.bytes());
        if (payload.part() == payload.parts() - 1) {
            INCOMING.remove(player.getUUID());
            apply(player, bytes.toByteArray());
        }
    }

    public static void clear() {
        INCOMING.clear();
    }

    private static void apply(ServerPlayer player, byte[] zip) {
        Map<String, byte[]> files;
        try {
            files = unzip(zip);
        } catch (IOException | RuntimeException e) {
            tell(player, "bad_file", 0);
            return;
        }
        if (files.isEmpty()) {
            tell(player, "bad_file", 0);
            return;
        }
        Set<String> changed = new LinkedHashSet<>();
        int set = 0;
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            ModConfigSpec spec = ModConfigs.worldFiles().get(entry.getKey());
            if (spec == null || !spec.isLoaded()) {
                continue;
            }
            CommentedConfig read;
            try {
                read = TomlFormat.instance().createParser().parse(new String(entry.getValue(), StandardCharsets.UTF_8));
            } catch (RuntimeException e) {
                continue;
            }
            int here = take(player, spec, spec.getSpec(), read, new ArrayList<>());
            if (here > 0) {
                set += here;
                changed.add(entry.getKey());
            }
        }
        for (String file : changed) {
            WorldSettings.store(file);
        }
        if (changed.contains(ModConfigs.file("commands"))) {
            CommandAliases.sync(player.server);
        }
        if (changed.contains(ModConfigs.file("mobs"))) {
            MobStats.applyAll(player.server, null);
        }
        MultiversePowers.LOGGER.info("{} imported {} world settings into {}", player.getGameProfile().getName(), set,
                changed);
        tell(player, "imported", set);
    }

    private static void tell(ServerPlayer player, String result, int count) {
        PacketDistributor.sendToPlayer(player, new SettingsResultPayload(result, count));
    }

    // Every value under `section` of `spec` the imported file holds and the setting takes; how many changed.
    @SuppressWarnings("unchecked")
    private static int take(ServerPlayer player, ModConfigSpec spec, UnmodifiableConfig section, CommentedConfig read,
            List<String> path) {
        int set = 0;
        for (UnmodifiableConfig.Entry entry : section.entrySet()) {
            List<String> here = new ArrayList<>(path);
            here.add(entry.getKey());
            Object rule = entry.getRawValue();
            if (rule instanceof UnmodifiableConfig inner) {
                set += take(player, spec, inner, read, here);
                continue;
            }
            if (!(rule instanceof ModConfigSpec.ValueSpec valueSpec) || here.equals(PowerRules.OWNERS.getPath())
                    || here.equals(PowerRules.OPERATORS.getPath()) && !WorldSettings.mayGrant(player)) {
                continue;
            }
            Object raw = read.get(here);
            Object value = spec.getValues().get(here);
            if (raw == null || !(value instanceof ModConfigSpec.ConfigValue<?> setting)) {
                continue;
            }
            Object taken = as(raw, setting.getDefault());
            if (taken != null && valueSpec.test(taken) && !taken.equals(setting.get())) {
                ((ModConfigSpec.ConfigValue<Object>) setting).set(taken);
                set++;
            }
        }
        return set;
    }

    // `raw` as the kind of value the setting holds (its default's), or null when it is not one.
    private static Object as(Object raw, Object like) {
        if (like instanceof Integer) {
            return raw instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue()) ? n.intValue() : null;
        }
        if (like instanceof Long) {
            return raw instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue()) ? n.longValue() : null;
        }
        if (like instanceof Double) {
            return raw instanceof Number n && Double.isFinite(n.doubleValue()) ? n.doubleValue() : null;
        }
        if (like instanceof Boolean || like instanceof String) {
            return like.getClass().isInstance(raw) ? raw : null;
        }
        if (like instanceof List<?>) {
            return raw instanceof List<?> list ? new ArrayList<>(list) : null;
        }
        if (like instanceof Enum<?> e && raw instanceof String name) {
            for (Object constant : e.getDeclaringClass().getEnumConstants()) {
                if (((Enum<?>) constant).name().equalsIgnoreCase(name)) {
                    return constant;
                }
            }
        }
        return null;
    }

    // The world settings files in a zip, each no bigger than MOST.
    private static Map<String, byte[]> unzip(byte[] zip) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || !ModConfigs.worldFiles().containsKey(name)) {
                    continue;
                }
                byte[] contents = in.readNBytes(MOST + 1);
                if (contents.length > MOST) {
                    throw new IOException("Too big: " + name);
                }
                files.put(name, contents);
            }
        }
        return files;
    }
}
