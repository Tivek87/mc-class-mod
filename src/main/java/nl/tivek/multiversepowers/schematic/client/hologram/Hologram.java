package nl.tivek.multiversepowers.schematic.client.hologram;

import javax.annotation.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.schematic.Schematic;
import nl.tivek.multiversepowers.schematic.SchematicPlacement;
import nl.tivek.multiversepowers.schematic.net.SchematicPlacePayload;
import nl.tivek.multiversepowers.schematic.paste.SchematicPastes;

// The schematic as a hologram in the world, full size, before it is placed: where it stands (following the look, set
// on the first block the look falls on or floating through blocks at a distance, or pinned where it is), turned and
// mirrored, and what placing it replaces and brings along.
public final class Hologram {
    public static final int ON_BLOCKS = 0;
    public static final int THROUGH = 1;
    private static final double REACH = 160.0;

    @Nullable
    public static Schematic schematic;
    @Nullable
    public static String path;
    public static Rotation rotation = Rotation.NONE;
    public static Mirror mirror = Mirror.NONE;
    public static int mode = SchematicPastes.SOLID;
    public static int flags = SchematicPastes.CREATURES | SchematicPastes.CONTENTS;
    public static int follow = ON_BLOCKS;
    public static boolean pinned;
    // Placed as a blueprint: a see-through plan to build by hand, never built itself.
    public static boolean blueprint;
    // The grid it snaps to across the ground, in blocks; one of GRIDS by index.
    public static final int[] GRIDS = { 1, 2, 3, 4, 6, 8, 10, 16, 32, 64, 96, 128 };
    public static int grid;
    static double distance = 8.0;
    // On blocks: how many blocks it stands past (or short of) the block looked at, the way the player faces.
    static int ahead;
    private static BlockPos pinnedAt = BlockPos.ZERO;
    private static BlockPos origin = BlockPos.ZERO;

    private Hologram() {
    }

    public static boolean shown() {
        return schematic != null;
    }

    public static void show(String file, Schematic shown) {
        schematic = shown;
        path = file;
        pinned = false;
        ahead = 0;
        distance = Math.max(6.0, Math.max(shown.sizeX, shown.sizeZ) * 0.8 + 3.0);
        HologramRenderer.load(shown);
    }

    public static void hide() {
        schematic = null;
        path = null;
        pinned = false;
        HologramRenderer.release();
    }

    static int sizeX() {
        Schematic now = schematic;
        return now == null ? 0 : placement().sizeX(now);
    }

    static int sizeZ() {
        Schematic now = schematic;
        return now == null ? 0 : placement().sizeZ(now);
    }

    public static SchematicPlacement placement() {
        return new SchematicPlacement(origin, rotation, mirror);
    }

    public static int gridSize() {
        return GRIDS[Math.floorMod(grid, GRIDS.length)];
    }

    // The next grid size, from 128 back to 1; a pinned hologram moves onto it.
    public static void nextGrid() {
        grid = Math.floorMod(grid + 1, GRIDS.length);
        int most = Math.max(1, 64 / gridSize());
        ahead = Math.max(-most, Math.min(most, ahead));
        if (pinned) {
            pinnedAt = snap(pinnedAt, false);
            origin = pinnedAt;
        }
    }

    // Its corner on the grid across the ground (and up and down too when it floats), the nearest grid line.
    private static BlockPos snap(BlockPos at, boolean upright) {
        int size = gridSize();
        if (size == 1) {
            return at;
        }
        return new BlockPos(Math.round(at.getX() / (float) size) * size, upright ? Math.round(at.getY()
                / (float) size) * size : at.getY(), Math.round(at.getZ() / (float) size) * size);
    }

    // Where it stands now: worked out every frame before it is drawn.
    public static void update(LocalPlayer player, float partialTick) {
        Schematic now = schematic;
        if (now == null) {
            return;
        }
        if (pinned) {
            origin = pinnedAt;
            return;
        }
        int sx = sizeX();
        int sz = sizeZ();
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        if (follow == ON_BLOCKS) {
            BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(look.scale(REACH)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                origin = snap(against(hit.getBlockPos().relative(hit.getDirection()), hit.getDirection(), sx,
                        now.sizeY, sz).relative(player.getDirection(), ahead * gridSize()), false);
                return;
            }
        }
        Vec3 at = eye.add(look.scale(distance));
        origin = snap(BlockPos.containing(at.x - sx / 2.0, at.y - now.sizeY / 2.0, at.z - sz / 2.0), true);
    }

    // The box set against the block face the look fell on: on top of a floor, under a ceiling, out from a wall.
    private static BlockPos against(BlockPos target, Direction face, int sx, int sy, int sz) {
        int x = target.getX() - sx / 2;
        int y = target.getY();
        int z = target.getZ() - sz / 2;
        switch (face) {
            case EAST -> x = target.getX();
            case WEST -> x = target.getX() - sx + 1;
            case SOUTH -> z = target.getZ();
            case NORTH -> z = target.getZ() - sz + 1;
            case DOWN -> y = target.getY() - sy + 1;
            default -> {
            }
        }
        return new BlockPos(x, y, z);
    }

    public static void pin() {
        pinned = !pinned;
        pinnedAt = origin;
    }

    // Turns it a quarter clockwise about its middle.
    public static void rotate() {
        int sx = sizeX();
        int sz = sizeZ();
        rotation = rotation.getRotated(Rotation.CLOCKWISE_90);
        if (pinned) {
            pinnedAt = pinnedAt.offset(sx / 2 - sizeX() / 2, 0, sz / 2 - sizeZ() / 2);
        }
    }

    public static void mirror() {
        mirror = Mirror.values()[(mirror.ordinal() + 1) % Mirror.values().length];
    }

    // Moves it a block, or a grid step across the ground (it stays pinned there).
    public static void nudge(int dx, int dy, int dz) {
        if (!pinned) {
            pinned = true;
            pinnedAt = snap(origin, false);
        }
        int size = gridSize();
        pinnedAt = pinnedAt.offset(dx * size, dy, dz * size);
        origin = pinnedAt;
    }

    // The scroll wheel: farther or closer (pinned or on blocks: a block or grid step at a time along the look).
    public static void scroll(LocalPlayer player, int steps) {
        if (pinned) {
            Direction facing = Direction.getNearest(player.getLookAngle());
            nudge(facing.getStepX() * steps, facing.getStepY() * steps, facing.getStepZ() * steps);
        } else if (follow == THROUGH || !onBlock(player)) {
            Schematic now = schematic;
            double step = now == null ? 1.0 : Math.max(1.0, Math.max(now.sizeX, now.sizeZ) / 16.0);
            distance = Mth.clamp(distance + steps * step, 2.0, 256.0);
        } else {
            int most = Math.max(1, 64 / gridSize());
            ahead = Mth.clamp(ahead + steps, -most, most);
        }
    }

    // Whether the look falls on a block within reach, so it stands on it.
    public static boolean onBlock(LocalPlayer player) {
        if (follow != ON_BLOCKS || pinned) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        return player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(REACH)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.BLOCK;
    }

    // Sends it to be built (or set down as a blueprint) where it stands; the hologram is gone until shown again.
    public static void place() {
        String file = path;
        if (file == null) {
            return;
        }
        PacketDistributor.sendToServer(new SchematicPlacePayload(file, origin, rotation.ordinal(), mirror.ordinal(),
                mode, blueprint ? flags | SchematicPlacePayload.BLUEPRINT : flags));
        hide();
    }
}
