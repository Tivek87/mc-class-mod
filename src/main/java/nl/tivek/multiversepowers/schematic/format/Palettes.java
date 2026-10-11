package nl.tivek.multiversepowers.schematic.format;

import com.mojang.datafixers.DSL;
import com.mojang.serialization.Dynamic;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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

    // A state saved as {Name, Properties} by a game of `version`; null when this game has no such block.
    @Nullable
    static BlockState state(CompoundTag tag, int version) {
        CompoundTag fixed = fix(References.BLOCK_STATE, tag, version);
        ResourceLocation id = ResourceLocation.tryParse(fixed.getString("Name"));
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return null;
        }
        return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), fixed);
    }

    // The state in a palette entry; air (counted) when the game has no such block.
    static BlockState known(CompoundTag tag, int version, int[] unknown) {
        BlockState state = state(tag, version);
        if (state == null) {
            unknown[0]++;
            return Blocks.AIR.defaultBlockState();
        }
        return state;
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
        if (version >= CURRENT) {
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
}
