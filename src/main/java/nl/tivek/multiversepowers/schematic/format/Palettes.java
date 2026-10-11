package nl.tivek.multiversepowers.schematic.format;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.DSL;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.tivek.multiversepowers.schematic.Schematic;

// Saved block states, block entities and creatures from any game version, made as this game has them (the game's own
// data fixers), and block states written as text read back.
final class Palettes {
    static final int CURRENT = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    // Before the flattening of 1.13: numeric block ids, and block entities with their old names.
    static final int FLATTENING = 1451;
    static final int OLD = 1343;
    // The oldest version the game's data fixers know: block entities and creatures named without a namespace.
    private static final int UNNAMED = 99;

    private Palettes() {
    }

    // MCEdit's files carry no version: a block entity or creature named "Chest" rather than "minecraft:chest" comes
    // from before 1.11, when the names changed.
    static int legacy(CompoundTag tag) {
        return tag.getString("id").indexOf(':') < 0 ? UNNAMED : OLD;
    }

    // A palette entry as this game has it: its state, or air and the block's name when the game has no such block.
    record Entry(BlockState state, @Nullable String missing) {
    }

    // The block names a schematic's palette entries stand in for as air, by palette entry.
    static final class Missing {
        private final Int2ObjectMap<String> names = new Int2ObjectOpenHashMap<>();

        void note(int entry, String name) {
            this.names.put(entry, name);
        }

        // Counts the cells each missing block would have filled, into the schematic.
        void count(Schematic schematic) {
            if (this.names.isEmpty()) {
                return;
            }
            int[] cells = new int[schematic.palette.size()];
            for (short cell : schematic.cells) {
                if (cell != Schematic.VOID) {
                    cells[cell]++;
                }
            }
            for (Int2ObjectMap.Entry<String> name : this.names.int2ObjectEntrySet()) {
                int count = name.getIntKey() < cells.length ? cells[name.getIntKey()] : 0;
                if (count > 0) {
                    schematic.missing.merge(name.getValue(), count, Integer::sum);
                }
            }
        }
    }

    // A state saved as {Name, Properties} by a game of `version`.
    static Entry entry(CompoundTag tag, int version) {
        CompoundTag fixed = fix(References.BLOCK_STATE, tag, version);
        String name = fixed.getString("Name");
        ResourceLocation id = ResourceLocation.tryParse(name);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return new Entry(Blocks.AIR.defaultBlockState(), id == null ? name : id.toString());
        }
        return new Entry(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), fixed), null);
    }

    // The state in palette entry `entry`; air (noted) when the game has no such block.
    static BlockState known(CompoundTag tag, int version, Missing missing, int entry) {
        Entry read = entry(tag, version);
        if (read.missing() != null) {
            missing.note(entry, read.missing());
        }
        return read.state();
    }

    // "minecraft:oak_stairs[facing=east,half=bottom]" as {Name, Properties}.
    static CompoundTag tag(String text) {
        CompoundTag tag = new CompoundTag();
        int open = text.indexOf('[');
        String name = open < 0 ? text : text.substring(0, open);
        tag.putString("Name", name.trim());
        if (open >= 0) {
            int close = text.lastIndexOf(']');
            String inside = text.substring(open + 1, close > open ? close : text.length());
            CompoundTag properties = new CompoundTag();
            for (String pair : inside.split(",")) {
                int equals = pair.indexOf('=');
                if (equals > 0) {
                    properties.putString(pair.substring(0, equals).trim(), pair.substring(equals + 1).trim());
                }
            }
            if (!properties.isEmpty()) {
                tag.put("Properties", properties);
            }
        }
        return tag;
    }

    static CompoundTag blockEntity(CompoundTag tag, int version) {
        return fix(References.BLOCK_ENTITY, tag, version);
    }

    static CompoundTag creature(CompoundTag tag, int version) {
        return fix(References.ENTITY_TREE, tag, version);
    }

    private static CompoundTag fix(DSL.TypeReference type, CompoundTag tag, int version) {
        if (version > CURRENT) {
            return newer(tag);
        }
        if (version == CURRENT) {
            return tag;
        }
        try {
            Tag fixed = DataFixers.getDataFixer().update(type, new Dynamic<>(NbtOps.INSTANCE, (Tag) tag), version,
                    CURRENT).getValue();
            return fixed instanceof CompoundTag compound ? compound : tag;
        } catch (RuntimeException e) {
            return tag;
        }
    }

    // Games from 1.21.5 on keep text as data where this one wants JSON in a string: a sign's lines and a name are
    // written back as JSON, or this game cannot read them.
    static CompoundTag newer(CompoundTag tag) {
        for (String side : new String[] { "front_text", "back_text" }) {
            if (!(tag.get(side) instanceof CompoundTag text)) {
                continue;
            }
            for (String lines : new String[] { "messages", "filtered_messages" }) {
                if (text.get(lines) instanceof ListTag list) {
                    ListTag json = new ListTag();
                    for (Tag line : list) {
                        json.add(StringTag.valueOf(json(line)));
                    }
                    text.put(lines, json);
                }
            }
        }
        Tag name = tag.get("CustomName");
        if (name != null) {
            tag.putString("CustomName", json(name));
        }
        return tag;
    }

    // Text as JSON: a string already in JSON stays as it is, plain words and text as data are made JSON.
    static String json(Tag text) {
        if (text instanceof StringTag string) {
            String words = string.getAsString();
            String start = words.stripLeading();
            // Gson reads leniently ("Room #3" parses as "Room"), so only what looks like JSON is tried as JSON.
            boolean looks = start.startsWith("{") || start.startsWith("[") || start.startsWith("\"");
            try {
                if (!looks) {
                    return JsonOps.INSTANCE.createString(words).toString();
                }
                JsonElement parsed = JsonParser.parseString(words);
                if ((parsed.isJsonObject() || parsed.isJsonArray() || parsed.isJsonPrimitive() && parsed
                        .getAsJsonPrimitive().isString()) && ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,
                                parsed).result().isPresent()) {
                    return words;
                }
            } catch (RuntimeException e) {
                // Not JSON: plain words.
            }
            return JsonOps.INSTANCE.createString(words).toString();
        }
        Component read = ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, text).result().orElse(Component.empty());
        return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, read).result().map(JsonElement::toString)
                .orElse("\"\"");
    }
}
