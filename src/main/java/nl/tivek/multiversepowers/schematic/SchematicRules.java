package nl.tivek.multiversepowers.schematic;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.config.WorldSettings;

// The Schematic Wand's world settings, and who may use it: the host, a listed owner or an operator, while no character.
public final class SchematicRules {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue BLOCKS_PER_TICK;
    public static final ModConfigSpec.IntValue LARGEST;
    public static final ModConfigSpec.IntValue FILE_MEGABYTES;
    public static final ModConfigSpec.IntValue UNDO_BLOCKS;
    public static final ModConfigSpec.IntValue BLUEPRINTS;
    public static final ModConfigSpec.IntValue LARGEST_SAVE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(ModConfigs.WIP, "Schematics: the Schematic Wand's files and how it places them",
                "World settings: every world keeps its own copy of this file, in <world>/serverconfig/welcomescreen/.",
                "The schematic files (.schem, .schematic, .litematic, .nbt) are read from the folder 'schematics' in",
                "the game folder (on a server, the server's own folder); with the wand they can be added, sorted and",
                "downloaded from inside the game.").push("schematics");
        BLOCKS_PER_TICK = builder.comment("Blocks a paste places each tick (20 ticks a second): more is faster, fewer"
                + " keeps a busy server smooth (default: 6000)").defineInRange("blocksPerTick", 6000, 100, 200000);
        LARGEST = builder.comment("The most cells (width x height x length) a schematic may have, so a huge file"
                + " never fills the memory (default: 16777216 = 256 x 256 x 256)")
                .defineInRange("largestSchematic", 16777216, 1000, 67108864);
        FILE_MEGABYTES = builder.comment("The largest schematic file, in megabytes, that may be imported or opened"
                + " (default: 32)").defineInRange("largestFileMegabytes", 32, 1, 512);
        UNDO_BLOCKS = builder.comment("How many replaced blocks of a player's last paste are kept so it can be undone;"
                + " a larger paste cannot be undone (default: 2000000)")
                .defineInRange("undoBlocks", 2000000, 0, 20000000);
        BLUEPRINTS = builder.comment("The most blueprints (see-through plans to build by hand) a world may hold at"
                + " once (default: 32)").defineInRange("blueprints", 32, 1, 256);
        LARGEST_SAVE = builder.comment("The most cells (width x height x length) of an area the wand may save as a"
                + " schematic at once; saving reads every block, so a huge area holds the server up (default:"
                + " 2000000)").defineInRange("largestSave", 2000000, 1000, 16777216);
        builder.pop();
        SPEC = builder.build();
    }

    private SchematicRules() {
    }

    public static int blocksPerTick() {
        return get(BLOCKS_PER_TICK);
    }

    public static int largest() {
        return get(LARGEST);
    }

    public static long largestFile() {
        return get(FILE_MEGABYTES) * 1024L * 1024L;
    }

    public static int undoBlocks() {
        return get(UNDO_BLOCKS);
    }

    public static int blueprints() {
        return get(BLUEPRINTS);
    }

    public static int largestSave() {
        return get(LARGEST_SAVE);
    }

    private static int get(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    // The host, a listed owner or an operator: they may manage the files.
    public static boolean trusted(ServerPlayer player) {
        return WorldSettings.mayGrant(player) || player.hasPermissions(2);
    }

    // They may hold and use the wand: trusted, and no character.
    public static boolean allowed(ServerPlayer player) {
        return trusted(player) && Characters.of(player) == null;
    }
}
